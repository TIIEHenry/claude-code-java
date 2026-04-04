/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code system directories utilities
 */
package com.anthropic.claudecode.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Cross-platform system directories utilities.
 * Handles differences between Windows, macOS, Linux, and WSL.
 */
public final class SystemDirectories {
    private SystemDirectories() {}

    /**
     * System directories record.
     */
    public record Directories(
            String home,
            String desktop,
            String documents,
            String downloads
    ) {
        /**
         * Get directory by name.
         */
        public String get(String name) {
            return switch (name.toLowerCase()) {
                case "home" -> home;
                case "desktop" -> desktop;
                case "documents" -> documents;
                case "downloads" -> downloads;
                default -> null;
            };
        }

        /**
         * Get as map.
         */
        public Map<String, String> asMap() {
            Map<String, String> map = new HashMap<>();
            map.put("HOME", home);
            map.put("DESKTOP", desktop);
            map.put("DOCUMENTS", documents);
            map.put("DOWNLOADS", downloads);
            return map;
        }
    }

    /**
     * Get the user home directory.
     */
    private static String getHome() {
        String home = System.getenv("HOME");
        if (home != null && !home.isEmpty()) {
            return home;
        }
        // Windows
        home = System.getenv("USERPROFILE");
        if (home != null && !home.isEmpty()) {
            return home;
        }
        return System.getProperty("user.home", "");
    }

    /**
     * Get system directories for current platform.
     */
    public static Directories getSystemDirectories() {
        return getSystemDirectories(Platform.getOsName());
    }

    /**
     * Get system directories for a specific platform.
     */
    public static Directories getSystemDirectories(String platform) {
        String homeDir = getHome();

        // Default paths
        String desktop = Paths.get(homeDir, "Desktop").toString();
        String documents = Paths.get(homeDir, "Documents").toString();
        String downloads = Paths.get(homeDir, "Downloads").toString();

        if (platform == null) {
            return new Directories(homeDir, desktop, documents, downloads);
        }

        switch (platform.toLowerCase()) {
            case "windows":
                // Windows: Use USERPROFILE if available
                String userProfile = System.getenv("USERPROFILE");
                if (userProfile != null && !userProfile.isEmpty()) {
                    return new Directories(
                            homeDir,
                            Paths.get(userProfile, "Desktop").toString(),
                            Paths.get(userProfile, "Documents").toString(),
                            Paths.get(userProfile, "Downloads").toString()
                    );
                }
                return new Directories(homeDir, desktop, documents, downloads);

            case "linux":
            case "wsl":
                // Linux/WSL: Check XDG Base Directory specification
                String xdgDesktop = System.getenv("XDG_DESKTOP_DIR");
                String xdgDocuments = System.getenv("XDG_DOCUMENTS_DIR");
                String xdgDownloads = System.getenv("XDG_DOWNLOAD_DIR");

                return new Directories(
                        homeDir,
                        xdgDesktop != null ? xdgDesktop : desktop,
                        xdgDocuments != null ? xdgDocuments : documents,
                        xdgDownloads != null ? xdgDownloads : downloads
                );

            case "macos":
            case "darwin":
            default:
                // macOS and unknown platforms use standard paths
                return new Directories(homeDir, desktop, documents, downloads);
        }
    }

    /**
     * Get home directory.
     */
    public static String getHomeDir() {
        return getHome();
    }

    /**
     * Get desktop directory.
     */
    public static String getDesktopDir() {
        return getSystemDirectories().desktop();
    }

    /**
     * Get documents directory.
     */
    public static String getDocumentsDir() {
        return getSystemDirectories().documents();
    }

    /**
     * Get downloads directory.
     */
    public static String getDownloadsDir() {
        return getSystemDirectories().downloads();
    }

    /**
     * Get path in home directory.
     */
    public static Path getHomePath(String... components) {
        Path base = Paths.get(getHome());
        for (String component : components) {
            base = base.resolve(component);
        }
        return base;
    }

    /**
     * Resolve system directories with custom home and platform.
     */
    public static Directories resolve(String home, String platform) {
        return getSystemDirectories(platform);
    }
}