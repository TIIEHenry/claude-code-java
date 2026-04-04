/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/cachePaths
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.util.*;

/**
 * Cache paths - Standard cache directory management.
 */
public final class CachePaths {
    private static final Path CACHE_ROOT;

    static {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");

        if (os.contains("mac")) {
            CACHE_ROOT = Paths.get(userHome, "Library", "Caches", "claude-code");
        } else if (os.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                CACHE_ROOT = Paths.get(localAppData, "claude-code", "cache");
            } else {
                CACHE_ROOT = Paths.get(userHome, ".claude-code", "cache");
            }
        } else {
            String xdgCacheHome = System.getenv("XDG_CACHE_HOME");
            if (xdgCacheHome != null) {
                CACHE_ROOT = Paths.get(xdgCacheHome, "claude-code");
            } else {
                CACHE_ROOT = Paths.get(userHome, ".cache", "claude-code");
            }
        }
    }

    /**
     * Get the cache root directory.
     */
    public static Path getCacheRoot() {
        return CACHE_ROOT;
    }

    /**
     * Get a cache directory for a specific purpose.
     */
    public static Path getCacheDir(String name) {
        return CACHE_ROOT.resolve(name);
    }

    /**
     * Get the prompt cache directory.
     */
    public static Path getPromptCacheDir() {
        return getCacheDir("prompts");
    }

    /**
     * Get the tool cache directory.
     */
    public static Path getToolCacheDir() {
        return getCacheDir("tools");
    }

    /**
     * Get the session cache directory.
     */
    public static Path getSessionCacheDir() {
        return getCacheDir("sessions");
    }

    /**
     * Get the HTTP cache directory.
     */
    public static Path getHttpCacheDir() {
        return getCacheDir("http");
    }

    /**
     * Get the plugin cache directory.
     */
    public static Path getPluginCacheDir() {
        return getCacheDir("plugins");
    }

    /**
     * Get the LSP cache directory.
     */
    public static Path getLspCacheDir() {
        return getCacheDir("lsp");
    }

    /**
     * Get the models cache directory.
     */
    public static Path getModelsCacheDir() {
        return getCacheDir("models");
    }

    /**
     * Get the temp cache directory.
     */
    public static Path getTempCacheDir() {
        return getCacheDir("temp");
    }

    /**
     * Get the errors log directory.
     */
    public static Path errors() {
        return getCacheDir("errors");
    }

    /**
     * Get the MCP logs directory for a specific server.
     */
    public static Path mcpLogs(String serverName) {
        return getCacheDir("mcp").resolve(serverName);
    }

    /**
     * Ensure a cache directory exists.
     */
    public static Path ensureCacheDir(String name) {
        Path dir = getCacheDir(name);
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            // Ignore
        }
        return dir;
    }

    /**
     * Clear all caches.
     */
    public static void clearAllCaches() {
        try {
            if (Files.exists(CACHE_ROOT)) {
                Files.walk(CACHE_ROOT)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception e) {
                            // Ignore
                        }
                    });
            }
            Files.createDirectories(CACHE_ROOT);
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Get cache size in bytes.
     */
    public static long getCacheSize() {
        try {
            if (!Files.exists(CACHE_ROOT)) {
                return 0;
            }

            return Files.walk(CACHE_ROOT)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
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
     * Format cache size as human-readable string.
     */
    public static String formatCacheSize() {
        long bytes = getCacheSize();

        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}