/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/platform.ts
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Platform detection utilities.
 */
public final class Platform {
    private Platform() {}

    /**
     * Supported platform types.
     */
    public enum Type {
        MACOS,
        WINDOWS,
        WSL,
        LINUX,
        UNKNOWN
    }

    private static volatile Type cachedPlatform = null;

    /**
     * Get the current platform.
     */
    public static Type getPlatform() {
        if (cachedPlatform != null) {
            return cachedPlatform;
        }

        String osName = System.getProperty("os.name", "").toLowerCase();

        if (osName.contains("mac")) {
            cachedPlatform = Type.MACOS;
        } else if (osName.contains("win")) {
            cachedPlatform = Type.WINDOWS;
        } else if (osName.contains("nux")) {
            // Check if running in WSL
            if (isWSL()) {
                cachedPlatform = Type.WSL;
            } else {
                cachedPlatform = Type.LINUX;
            }
        } else {
            cachedPlatform = Type.UNKNOWN;
        }

        return cachedPlatform;
    }

    /**
     * Check if running in Windows Subsystem for Linux.
     */
    public static boolean isWSL() {
        try {
            Path procVersion = Paths.get("/proc/version");
            if (Files.exists(procVersion)) {
                String content = Files.readString(procVersion).toLowerCase();
                return content.contains("microsoft") || content.contains("wsl");
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    /**
     * Get WSL version.
     */
    public static String getWslVersion() {
        if (getPlatform() != Type.WSL && getPlatform() != Type.LINUX) {
            return null;
        }

        try {
            Path procVersion = Paths.get("/proc/version");
            if (Files.exists(procVersion)) {
                String content = Files.readString(procVersion);

                // Check for explicit WSL version markers
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("WSL(\\d+)", Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1);
                }

                // If contains Microsoft but no version, assume WSL1
                if (content.toLowerCase().contains("microsoft")) {
                    return "1";
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Linux distribution info.
     */
    public record LinuxDistroInfo(
        String linuxDistroId,
        String linuxDistroVersion,
        String linuxKernel
    ) {}

    /**
     * Get Linux distribution info.
     */
    public static LinuxDistroInfo getLinuxDistroInfo() {
        if (getPlatform() != Type.LINUX && getPlatform() != Type.WSL) {
            return null;
        }

        String kernel = System.getProperty("os.version");
        String distroId = null;
        String distroVersion = null;

        try {
            Path osRelease = Paths.get("/etc/os-release");
            if (Files.exists(osRelease)) {
                String content = Files.readString(osRelease);
                for (String line : content.split("\n")) {
                    java.util.regex.Matcher matcher =
                        java.util.regex.Pattern.compile("^(ID|VERSION_ID)=(.*)$").matcher(line);
                    if (matcher.matches()) {
                        String key = matcher.group(1);
                        String value = matcher.group(2).replaceFirst("^\"|\"$", "");
                        if ("ID".equals(key)) {
                            distroId = value;
                        } else if ("VERSION_ID".equals(key)) {
                            distroVersion = value;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        return new LinuxDistroInfo(distroId, distroVersion, kernel);
    }

    /**
     * Detect VCS systems in a directory.
     */
    public static List<String> detectVcs(String dir) {
        Set<String> detected = new HashSet<>();

        // Check for Perforce via env var
        if (System.getenv("P4PORT") != null) {
            detected.add("perforce");
        }

        // VCS markers
        Map<String, String> vcsMarkers = Map.of(
            ".git", "git",
            ".hg", "mercurial",
            ".svn", "svn",
            ".p4config", "perforce",
            "$tf", "tfs",
            ".tfvc", "tfs",
            ".jj", "jujutsu",
            ".sl", "sapling"
        );

        try {
            Path targetDir = dir != null ? Paths.get(dir) : Paths.get(System.getProperty("user.dir"));
            if (Files.isDirectory(targetDir)) {
                try (var stream = Files.list(targetDir)) {
                    stream.filter(Files::isDirectory)
                        .map(p -> p.getFileName().toString())
                        .forEach(name -> {
                            String vcs = vcsMarkers.get(name);
                            if (vcs != null) {
                                detected.add(vcs);
                            }
                        });
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        return new ArrayList<>(detected);
    }

    /**
     * Check if platform is supported.
     */
    public static boolean isSupported() {
        Type platform = getPlatform();
        return platform == Type.MACOS || platform == Type.WSL;
    }

    /**
     * Check if running on Windows.
     */
    public static boolean isWindows() {
        return getPlatform() == Type.WINDOWS;
    }

    /**
     * Check if running on macOS.
     */
    public static boolean isMacOS() {
        return getPlatform() == Type.MACOS;
    }

    /**
     * Check if running on macOS (alias).
     */
    public static boolean isMac() {
        return isMacOS();
    }

    /**
     * Get OS name.
     */
    public static String getOsName() {
        return System.getProperty("os.name");
    }

    /**
     * Check if running on Linux.
     */
    public static boolean isLinux() {
        Type platform = getPlatform();
        return platform == Type.LINUX || platform == Type.WSL;
    }
}