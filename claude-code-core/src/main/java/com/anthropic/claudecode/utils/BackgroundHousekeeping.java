/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code background housekeeping utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Background housekeeping operations.
 * Runs cleanup and maintenance tasks in the background.
 */
public final class BackgroundHousekeeping {
    private BackgroundHousekeeping() {}

    // 24 hours in milliseconds
    private static final long RECURRING_CLEANUP_INTERVAL_MS = 24 * 60 * 60 * 1000;

    // 10 minutes after start
    private static final long DELAY_VERY_SLOW_OPERATIONS_MS = 10 * 60 * 1000;

    private static volatile boolean started = false;
    private static volatile boolean needsCleanup = true;
    private static volatile ScheduledExecutorService scheduler;

    /**
     * Start background housekeeping tasks.
     */
    public static void startBackgroundHousekeeping() {
        if (started) {
            return;
        }
        started = true;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "housekeeping");
            t.setDaemon(true);
            return t;
        });

        // Schedule very slow operations after 10 minutes
        scheduler.schedule(() -> runVerySlowOps(), DELAY_VERY_SLOW_OPERATIONS_MS, TimeUnit.MILLISECONDS);

        // Schedule recurring cleanup every 24 hours for long-running sessions
        String userType = System.getenv("USER_TYPE");
        if ("ant".equals(userType)) {
            scheduler.scheduleAtFixedRate(() -> {
                CleanupUtils.cleanupNpmCacheForAnthropicPackages();
                CleanupUtils.cleanupOldVersionsThrottled();
            }, RECURRING_CLEANUP_INTERVAL_MS, RECURRING_CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stop background housekeeping.
     */
    public static void stopBackgroundHousekeeping() {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
        started = false;
    }

    /**
     * Run very slow operations.
     * Skips if user was active in last minute.
     */
    private static void runVerySlowOps() {
        // Check if user was active recently
        if (isInteractive() && wasUserActiveRecently()) {
            // Reschedule for later
            scheduler.schedule(() -> runVerySlowOps(),
                    DELAY_VERY_SLOW_OPERATIONS_MS, TimeUnit.MILLISECONDS);
            return;
        }

        if (needsCleanup) {
            needsCleanup = false;
            CleanupUtils.cleanupOldMessageFilesInBackground();
        }

        // Check again before running next slow op
        if (isInteractive() && wasUserActiveRecently()) {
            scheduler.schedule(() -> runVerySlowOps(),
                    DELAY_VERY_SLOW_OPERATIONS_MS, TimeUnit.MILLISECONDS);
            return;
        }

        CleanupUtils.cleanupOldVersions();
    }

    /**
     * Check if running in interactive mode.
     */
    private static boolean isInteractive() {
        return Boolean.parseBoolean(System.getenv("CLAUDE_CODE_INTERACTIVE"));
    }

    /**
     * Check if user was active in last minute.
     */
    private static boolean wasUserActiveRecently() {
        long lastInteraction = ActivityManager.getInstance().getLastInteractionTime();
        return lastInteraction > System.currentTimeMillis() - 60000;
    }

    /**
     * Initialize services.
     */
    public static void initServices() {
        // Initialize MagicDocs service (placeholder)
        // Initialize SkillImprovement (placeholder)
        // Initialize ExtractMemories (placeholder)
        // Initialize AutoDream (placeholder)
    }

    /**
     * Auto-update marketplaces and plugins.
     */
    public static void autoUpdateMarketplacesAndPlugins() {
        CompletableFuture.runAsync(() -> {
            try {
                String home = System.getProperty("user.home");
                java.nio.file.Path pluginsDir = java.nio.file.Paths.get(home, ".claude", "plugins");

                if (!java.nio.file.Files.exists(pluginsDir)) {
                    return;
                }

                // Check each plugin for updates
                try (var stream = java.nio.file.Files.list(pluginsDir)) {
                    for (java.nio.file.Path pluginDir : stream.toList()) {
                        if (java.nio.file.Files.isDirectory(pluginDir)) {
                            checkPluginUpdate(pluginDir);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore errors during auto-update
            }
        });
    }

    private static void checkPluginUpdate(java.nio.file.Path pluginDir) {
        try {
            java.nio.file.Path manifestPath = pluginDir.resolve("manifest.json");
            if (!java.nio.file.Files.exists(manifestPath)) {
                return;
            }

            String content = java.nio.file.Files.readString(manifestPath);

            // Extract plugin info
            String name = extractJsonValueString(content, "name");
            String version = extractJsonValueString(content, "version");
            String updateUrl = extractJsonValueString(content, "updateUrl");

            if (name == null || updateUrl == null) {
                return;
            }

            // Check for updates
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(updateUrl))
                .header("Accept", "application/json")
                .GET()
                .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String latestVersion = extractJsonValueString(response.body(), "version");

                if (latestVersion != null && !latestVersion.equals(version)) {
                    // New version available - would download and install
                    String downloadUrl = extractJsonValueString(response.body(), "downloadUrl");

                    if (downloadUrl != null) {
                        downloadAndUpdatePlugin(pluginDir, downloadUrl);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors for individual plugins
        }
    }

    private static void downloadAndUpdatePlugin(java.nio.file.Path pluginDir, String downloadUrl) {
        try {
            // Download new version
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(downloadUrl))
                .GET()
                .build();

            java.net.http.HttpResponse<byte[]> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                byte[] bytes = response.body();

                // Write to temp file and extract
                java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("plugin-", ".zip");
                java.nio.file.Files.write(tempFile, bytes);

                // Extract zip to plugin directory
                try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                        java.nio.file.Files.newInputStream(tempFile))) {
                    java.util.zip.ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        java.nio.file.Path targetPath = pluginDir.resolve(entry.getName());
                        if (entry.isDirectory()) {
                            java.nio.file.Files.createDirectories(targetPath);
                        } else {
                            java.nio.file.Files.copy(zis, targetPath,
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }

                java.nio.file.Files.delete(tempFile);
            }
        } catch (Exception e) {
            // Ignore errors during download/update
        }
    }

    private static String extractJsonValueString(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int valStart = json.indexOf("\"", idx + key.length() + 3) + 1;
        if (valStart < 1) return null;
        int valEnd = json.indexOf("\"", valStart);
        return json.substring(valStart, valEnd);
    }
}