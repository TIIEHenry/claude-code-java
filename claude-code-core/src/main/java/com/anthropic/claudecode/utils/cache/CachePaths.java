/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/cachePaths
 */
package com.anthropic.claudecode.utils.cache;

import java.util.*;
import java.nio.file.*;

/**
 * Cache paths - Cache directory management.
 */
public final class CachePaths {
    private static final String CACHE_DIR_NAME = ".claude";
    private static volatile Path baseCachePath = null;

    /**
     * Get base cache path.
     */
    public static Path getBaseCachePath() {
        if (baseCachePath == null) {
            String home = System.getProperty("user.home");
            baseCachePath = Paths.get(home, CACHE_DIR_NAME);
        }
        return baseCachePath;
    }

    /**
     * Set custom cache path.
     */
    public static void setBaseCachePath(Path path) {
        baseCachePath = path;
    }

    /**
     * Get cache subdirectory.
     */
    public static Path getCachePath(String subdirectory) {
        return getBaseCachePath().resolve(subdirectory);
    }

    /**
     * Get tasks cache path.
     */
    public static Path getTasksPath() {
        return getCachePath("tasks");
    }

    /**
     * Get history cache path.
     */
    public static Path getHistoryPath() {
        return getCachePath("history");
    }

    /**
     * Get sessions cache path.
     */
    public static Path getSessionsPath() {
        return getCachePath("sessions");
    }

    /**
     * Get memory cache path.
     */
    public static Path getMemoryPath() {
        return getCachePath("memory");
    }

    /**
     * Get plugins cache path.
     */
    public static Path getPluginsPath() {
        return getCachePath("plugins");
    }

    /**
     * Get settings cache path.
     */
    public static Path getSettingsPath() {
        return getCachePath("settings");
    }

    /**
     * Get logs cache path.
     */
    public static Path getLogsPath() {
        return getCachePath("logs");
    }

    /**
     * Get temp cache path.
     */
    public static Path getTempPath() {
        return getCachePath("temp");
    }

    /**
     * Get MCP cache path.
     */
    public static Path getMcpPath() {
        return getCachePath("mcp");
    }

    /**
     * Get project-specific cache path.
     */
    public static Path getProjectCachePath(String projectPath) {
        String projectName = Paths.get(projectPath).getFileName().toString();
        return getCachePath("projects").resolve(projectName);
    }

    /**
     * Ensure cache directory exists.
     */
    public static void ensureCacheDirectory() {
        try {
            Files.createDirectories(getBaseCachePath());
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Ensure subdirectory exists.
     */
    public static void ensureDirectory(String subdirectory) {
        try {
            Files.createDirectories(getCachePath(subdirectory));
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Clear cache directory.
     */
    public static void clearCache() {
        try {
            Files.walk(getBaseCachePath())
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception e) {
                        // Ignore
                    }
                });
            ensureCacheDirectory();
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Get cache size in bytes.
     */
    public static long getCacheSize() {
        try {
            return Files.walk(getBaseCachePath())
                .filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try {
                        return Files.size(p);
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get cache size formatted.
     */
    public static String getCacheSizeFormatted() {
        long size = getCacheSize();
        return formatSize(size);
    }

    /**
     * Format size.
     */
    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)) + " MB";
        return (bytes / (1024 * 1024 * 1024)) + " GB";
    }

    /**
     * Cache entry record.
     */
    public record CacheEntry(
        String key,
        Path path,
        long size,
        long createdAt,
        long lastAccessed,
        String type
    ) {
        public boolean isExpired(long maxAgeMs) {
            return System.currentTimeMillis() - lastAccessed > maxAgeMs;
        }
    }
}