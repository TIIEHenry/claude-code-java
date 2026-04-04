/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/platform.ts
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Platform detection utilities.
 */
public final class PlatformUtil {
    private PlatformUtil() {}

    /**
     * Platform type.
     */
    public enum Platform {
        MACOS,
        WINDOWS,
        WSL,
        LINUX,
        UNKNOWN
    }

    /**
     * Supported platforms.
     */
    public static final List<Platform> SUPPORTED_PLATFORMS = List.of(Platform.MACOS, Platform.WSL);

    private static volatile Platform cachedPlatform = null;

    /**
     * Get current platform.
     */
    public static Platform getPlatform() {
        if (cachedPlatform != null) {
            return cachedPlatform;
        }

        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("mac")) {
            cachedPlatform = Platform.MACOS;
        } else if (osName.contains("win")) {
            cachedPlatform = Platform.WINDOWS;
        } else if (osName.contains("nux")) {
            // Check if running in WSL
            if (isWSL()) {
                cachedPlatform = Platform.WSL;
            } else {
                cachedPlatform = Platform.LINUX;
            }
        } else {
            cachedPlatform = Platform.UNKNOWN;
        }

        return cachedPlatform;
    }

    /**
     * Check if running in WSL.
     */
    private static boolean isWSL() {
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
        if (getPlatform() != Platform.LINUX && getPlatform() != Platform.WSL) {
            return null;
        }

        try {
            Path procVersion = Paths.get("/proc/version");
            if (Files.exists(procVersion)) {
                String content = Files.readString(procVersion);

                // Check for explicit WSL version markers
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("WSL(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1);
                }

                // If no explicit version but contains Microsoft, assume WSL1
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
        if (getPlatform() != Platform.LINUX && getPlatform() != Platform.WSL) {
            return null;
        }

        String linuxKernel = System.getProperty("os.version");
        String distroId = null;
        String distroVersion = null;

        try {
            Path osRelease = Paths.get("/etc/os-release");
            if (Files.exists(osRelease)) {
                List<String> lines = Files.readAllLines(osRelease);
                for (String line : lines) {
                    if (line.startsWith("ID=")) {
                        distroId = line.substring(3).replace("\"", "");
                    } else if (line.startsWith("VERSION_ID=")) {
                        distroVersion = line.substring(11).replace("\"", "");
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        return new LinuxDistroInfo(distroId, distroVersion, linuxKernel);
    }

    /**
     * VCS markers.
     */
    private static final List<Map.Entry<String, String>> VCS_MARKERS = List.of(
        Map.entry(".git", "git"),
        Map.entry(".hg", "mercurial"),
        Map.entry(".svn", "svn"),
        Map.entry(".p4config", "perforce"),
        Map.entry("$tf", "tfs"),
        Map.entry(".tfvc", "tfs"),
        Map.entry(".jj", "jujutsu"),
        Map.entry(".sl", "sapling")
    );

    /**
     * Detect VCS in directory.
     */
    public static List<String> detectVcs(String dir) {
        Set<String> detected = new HashSet<>();

        // Check for Perforce via env var
        if (System.getenv("P4PORT") != null) {
            detected.add("perforce");
        }

        try {
            String targetDir = dir != null ? dir : System.getProperty("user.dir");
            Path targetPath = Paths.get(targetDir);

            if (Files.isDirectory(targetPath)) {
                Set<String> entries = new HashSet<>();
                try (var stream = Files.list(targetPath)) {
                    stream.forEach(p -> entries.add(p.getFileName().toString()));
                }

                for (Map.Entry<String, String> marker : VCS_MARKERS) {
                    if (entries.contains(marker.getKey())) {
                        detected.add(marker.getValue());
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        return new ArrayList<>(detected);
    }

    /**
     * Detect VCS in current directory.
     */
    public static List<String> detectVcs() {
        return detectVcs(null);
    }

    /**
     * Check if platform is macOS.
     */
    public static boolean isMacOS() {
        return getPlatform() == Platform.MACOS;
    }

    /**
     * Check if platform is Windows.
     */
    public static boolean isWindows() {
        return getPlatform() == Platform.WINDOWS;
    }

    /**
     * Check if platform is WSL.
     */
    public static boolean isWSLPlatform() {
        return getPlatform() == Platform.WSL;
    }

    /**
     * Check if platform is Linux (not WSL).
     */
    public static boolean isLinux() {
        return getPlatform() == Platform.LINUX;
    }

    /**
     * Reset cached platform (for testing).
     */
    public static void resetPlatform() {
        cachedPlatform = null;
    }

    /**
     * Get OS name.
     */
    public static String getOsName() {
        return System.getProperty("os.name");
    }

    /**
     * Get OS version.
     */
    public static String getOsVersion() {
        return System.getProperty("os.version");
    }

    /**
     * Get OS architecture.
     */
    public static String getOsArch() {
        return System.getProperty("os.arch");
    }

    /**
     * Check if running on ARM architecture.
     */
    public static boolean isArm() {
        String arch = getOsArch().toLowerCase();
        return arch.contains("aarch64") || arch.contains("arm");
    }

    /**
     * Check if running on x86 architecture.
     */
    public static boolean isX86() {
        String arch = getOsArch().toLowerCase();
        return arch.contains("x86") || arch.contains("amd64");
    }
}