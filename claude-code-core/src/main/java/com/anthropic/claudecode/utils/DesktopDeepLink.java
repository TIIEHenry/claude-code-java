/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code desktop deep link utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Utilities for building and opening deep links to Claude Desktop.
 */
public final class DesktopDeepLink {
    private DesktopDeepLink() {}

    private static final String MIN_DESKTOP_VERSION = "1.1.2396";

    /**
     * Desktop install status.
     */
    public sealed interface DesktopInstallStatus permits
            DesktopInstallStatus.NotInstalled,
            DesktopInstallStatus.VersionTooOld,
            DesktopInstallStatus.Ready {

        String status();

        public static final class NotInstalled implements DesktopInstallStatus {
            @Override public String status() { return "not-installed"; }
        }

        public static final class VersionTooOld implements DesktopInstallStatus {
            private final String version;

            public VersionTooOld(String version) {
                this.version = version;
            }

            public String version() { return version; }
            @Override public String status() { return "version-too-old"; }
        }

        public static final class Ready implements DesktopInstallStatus {
            private final String version;

            public Ready(String version) {
                this.version = version;
            }

            public String version() { return version; }
            @Override public String status() { return "ready"; }
        }
    }

    /**
     * Result of opening session in desktop.
     */
    public record DesktopOpenResult(
            boolean success,
            String error,
            String deepLinkUrl
    ) {}

    /**
     * Check if running in dev mode.
     */
    private static boolean isDevMode() {
        String nodeEnv = System.getenv("NODE_ENV");
        if ("development".equals(nodeEnv)) {
            return true;
        }

        // Check for build directories in paths
        String[] pathsToCheck = {
                System.getProperty("user.dir"),
                System.getProperty("java.home")
        };
        String[] buildDirs = {
                "/build-ant/",
                "/build-ant-native/",
                "/build-external/",
                "/build-external-native/"
        };

        for (String path : pathsToCheck) {
            if (path != null) {
                for (String dir : buildDirs) {
                    if (path.contains(dir)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Build a deep link URL for Claude Desktop to resume a CLI session.
     */
    public static String buildDesktopDeepLink(String sessionId, String cwd) {
        String protocol = isDevMode() ? "claude-dev" : "claude";
        return String.format("%s://resume?session=%s&cwd=%s",
                protocol,
                sessionId,
                encodeUrl(cwd));
    }

    /**
     * URL encode a string.
     */
    private static String encodeUrl(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }

    /**
     * Check if Claude Desktop app is installed.
     */
    public static CompletableFuture<Boolean> isDesktopInstalled() {
        return CompletableFuture.supplyAsync(() -> {
            // In dev mode, assume the dev Desktop app is running
            if (isDevMode()) {
                return true;
            }

            String os = System.getProperty("os.name", "").toLowerCase();

            if (os.contains("mac")) {
                return Files.exists(Paths.get("/Applications/Claude.app"));
            } else if (os.contains("linux")) {
                // Check if xdg-mime can find a handler for claude://
                try {
                    ProcessBuilder pb = new ProcessBuilder("xdg-mime", "query", "default", "x-scheme-handler/claude");
                    Process p = pb.start();
                    p.waitFor(5, TimeUnit.SECONDS);
                    return p.exitValue() == 0 && !readStream(p.getInputStream()).trim().isEmpty();
                } catch (Exception e) {
                    return false;
                }
            } else if (os.contains("win")) {
                // On Windows, try to query the registry for the protocol handler
                try {
                    ProcessBuilder pb = new ProcessBuilder("reg", "query", "HKEY_CLASSES_ROOT\\claude", "/ve");
                    Process p = pb.start();
                    p.waitFor(5, TimeUnit.SECONDS);
                    return p.exitValue() == 0;
                } catch (Exception e) {
                    return false;
                }
            }

            return false;
        });
    }

    /**
     * Get the installed Claude Desktop version.
     */
    public static CompletableFuture<String> getDesktopVersion() {
        return CompletableFuture.supplyAsync(() -> {
            String os = System.getProperty("os.name", "").toLowerCase();

            if (os.contains("mac")) {
                try {
                    ProcessBuilder pb = new ProcessBuilder("defaults", "read",
                            "/Applications/Claude.app/Contents/Info.plist",
                            "CFBundleShortVersionString");
                    Process p = pb.start();
                    p.waitFor(5, TimeUnit.SECONDS);
                    if (p.exitValue() == 0) {
                        String version = readStream(p.getInputStream()).trim();
                        return version.isEmpty() ? null : version;
                    }
                } catch (Exception e) {
                    // Ignore
                }
            } else if (os.contains("win")) {
                String localAppData = System.getenv("LOCALAPPDATA");
                if (localAppData != null) {
                    Path installDir = Paths.get(localAppData, "AnthropicClaude");
                    try {
                        List<String> versions = new ArrayList<>();
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(installDir)) {
                            for (Path entry : stream) {
                                String name = entry.getFileName().toString();
                                if (name.startsWith("app-")) {
                                    versions.add(name.substring(4));
                                }
                            }
                        }
                        if (!versions.isEmpty()) {
                            Collections.sort(versions);
                            return versions.get(versions.size() - 1);
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }

            return null;
        });
    }

    /**
     * Check Desktop install status including version compatibility.
     */
    public static CompletableFuture<DesktopInstallStatus> getDesktopInstallStatus() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean installed = isDesktopInstalled().get();
                if (!installed) {
                    return new DesktopInstallStatus.NotInstalled();
                }

                String version = getDesktopVersion().get();
                if (version == null) {
                    return new DesktopInstallStatus.Ready("unknown");
                }

                if (!isVersionAtLeast(version, MIN_DESKTOP_VERSION)) {
                    return new DesktopInstallStatus.VersionTooOld(version);
                }

                return new DesktopInstallStatus.Ready(version);
            } catch (Exception e) {
                return new DesktopInstallStatus.Ready("unknown");
            }
        });
    }

    /**
     * Open a deep link URL.
     */
    private static CompletableFuture<Boolean> openDeepLink(String deepLinkUrl) {
        return CompletableFuture.supplyAsync(() -> {
            String os = System.getProperty("os.name", "").toLowerCase();

            try {
                if (os.contains("mac")) {
                    ProcessBuilder pb;
                    if (isDevMode()) {
                        pb = new ProcessBuilder("osascript", "-e",
                                "tell application \"Electron\" to open location \"" + deepLinkUrl + "\"");
                    } else {
                        pb = new ProcessBuilder("open", deepLinkUrl);
                    }
                    Process p = pb.start();
                    p.waitFor(5, TimeUnit.SECONDS);
                    return p.exitValue() == 0;
                } else if (os.contains("linux")) {
                    ProcessBuilder pb = new ProcessBuilder("xdg-open", deepLinkUrl);
                    Process p = pb.start();
                    p.waitFor(5, TimeUnit.SECONDS);
                    return p.exitValue() == 0;
                } else if (os.contains("win")) {
                    ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "", deepLinkUrl);
                    Process p = pb.start();
                    p.waitFor(5, TimeUnit.SECONDS);
                    return p.exitValue() == 0;
                }
            } catch (Exception e) {
                // Ignore
            }

            return false;
        });
    }

    /**
     * Open current session in Claude Desktop.
     */
    public static CompletableFuture<DesktopOpenResult> openCurrentSessionInDesktop(
            String sessionId, String cwd) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean installed = isDesktopInstalled().get();
                if (!installed) {
                    return new DesktopOpenResult(false,
                            "Claude Desktop is not installed. Install it from https://claude.ai/download",
                            null);
                }

                String deepLinkUrl = buildDesktopDeepLink(sessionId, cwd);
                boolean opened = openDeepLink(deepLinkUrl).get();

                if (!opened) {
                    return new DesktopOpenResult(false,
                            "Failed to open Claude Desktop. Please try opening it manually.",
                            deepLinkUrl);
                }

                return new DesktopOpenResult(true, null, deepLinkUrl);
            } catch (Exception e) {
                return new DesktopOpenResult(false, e.getMessage(), null);
            }
        });
    }

    /**
     * Read stream to string.
     */
    private static String readStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Check if version is at least minVersion.
     */
    private static boolean isVersionAtLeast(String version, String minVersion) {
        // Simple version comparison - split by dots and compare
        String[] v = version.split("\\.");
        String[] min = minVersion.split("\\.");

        for (int i = 0; i < Math.max(v.length, min.length); i++) {
            int vPart = i < v.length ? parseVersionPart(v[i]) : 0;
            int minPart = i < min.length ? parseVersionPart(min[i]) : 0;

            if (vPart > minPart) return true;
            if (vPart < minPart) return false;
        }

        return true;
    }

    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}