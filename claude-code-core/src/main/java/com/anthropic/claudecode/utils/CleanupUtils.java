/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/cleanup
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.util.stream.*;

/**
 * Cleanup utilities - Resource cleanup management.
 */
public final class CleanupUtils {
    private static final List<CleanupTask> cleanupTasks = new CopyOnWriteArrayList<>();
    private static volatile boolean shutdownHookRegistered = false;

    /**
     * Register a cleanup task.
     */
    public static void register(Runnable task, String description, int priority) {
        registerShutdownHook();
        cleanupTasks.add(new CleanupTask(task, description, priority));
        cleanupTasks.sort(Comparator.comparingInt(CleanupTask::priority).reversed());
    }

    /**
     * Register a cleanup task with default priority.
     */
    public static void register(Runnable task, String description) {
        register(task, description, 0);
    }

    /**
     * Register a cleanup task with high priority.
     */
    public static void registerHighPriority(Runnable task, String description) {
        register(task, description, 100);
    }

    /**
     * Unregister a cleanup task.
     */
    public static void unregister(Runnable task) {
        cleanupTasks.removeIf(t -> t.task() == task);
    }

    /**
     * Run all cleanup tasks.
     */
    public static void runCleanup() {
        List<Exception> errors = new ArrayList<>();

        for (CleanupTask task : cleanupTasks) {
            try {
                task.task().run();
            } catch (Exception e) {
                errors.add(e);
            }
        }

        cleanupTasks.clear();

        if (!errors.isEmpty()) {
            // Log errors but don't throw
            for (Exception e : errors) {
                System.err.println("Cleanup error: " + e.getMessage());
            }
        }
    }

    /**
     * Run cleanup and exit.
     */
    public static void runCleanupAndExit(int exitCode) {
        runCleanup();
        System.exit(exitCode);
    }

    /**
     * Get pending cleanup tasks.
     */
    public static List<String> getPendingCleanupTasks() {
        return cleanupTasks.stream()
            .map(CleanupTask::description)
            .toList();
    }

    /**
     * Clear all pending cleanup tasks without running them.
     */
    public static void clearPendingTasks() {
        cleanupTasks.clear();
    }

    // Additional cleanup methods for BackgroundHousekeeping
    public static void cleanupNpmCacheForAnthropicPackages() {
        try {
            // Find npm cache directory
            String npmCache = System.getenv("NPM_CONFIG_CACHE");
            if (npmCache == null) {
                String os = System.getProperty("os.name").toLowerCase();
                String home = System.getProperty("user.home");

                if (os.contains("win")) {
                    npmCache = System.getenv("APPDATA") + "\\npm-cache";
                } else if (os.contains("mac")) {
                    npmCache = home + "/.npm";
                } else {
                    npmCache = home + "/.npm";
                }
            }

            Path cachePath = Path.of(npmCache);
            if (!Files.exists(cachePath)) return;

            // Clean up @anthropic packages from cache
            Files.walk(cachePath)
                .filter(p -> p.toString().contains("@anthropic"))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception e) {
                        // Ignore delete errors
                    }
                });
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    public static void cleanupOldVersionsThrottled() {
        // Only cleanup if last cleanup was more than 24 hours ago
        Path markerFile = Path.of(System.getProperty("user.home"))
            .resolve(".claude")
            .resolve(".last-cleanup");

        try {
            if (Files.exists(markerFile)) {
                long lastCleanup = Files.getLastModifiedTime(markerFile).toMillis();
                if (System.currentTimeMillis() - lastCleanup < 24 * 60 * 60 * 1000) {
                    return; // Skip - too soon
                }
            }

            cleanupOldVersions();

            // Update marker
            Files.createDirectories(markerFile.getParent());
            Files.writeString(markerFile, String.valueOf(System.currentTimeMillis()));
        } catch (Exception e) {
            // Ignore errors
        }
    }

    public static void cleanupOldMessageFilesInBackground() {
        CompletableFuture.runAsync(() -> {
            try {
                Path messagesDir = Path.of(System.getProperty("user.home"))
                    .resolve(".claude")
                    .resolve("messages");

                if (!Files.exists(messagesDir)) return;

                long cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000); // 30 days

                Files.walk(messagesDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis() < cutoffTime;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception e) {
                            // Ignore
                        }
                    });
            } catch (Exception e) {
                // Ignore errors
            }
        });
    }

    public static void cleanupOldVersions() {
        try {
            Path versionsDir = Path.of(System.getProperty("user.home"))
                .resolve(".claude")
                .resolve("versions");

            if (!Files.exists(versionsDir)) return;

            // Keep only the 5 most recent versions
            List<Path> versions = Files.list(versionsDir)
                .filter(Files::isDirectory)
                .sorted((a, b) -> {
                    try {
                        return Long.compare(
                            Files.getLastModifiedTime(b).toMillis(),
                            Files.getLastModifiedTime(a).toMillis()
                        );
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .toList();

            // Delete old versions
            for (int i = 5; i < versions.size(); i++) {
                Path oldVersion = versions.get(i);
                deleteDirectory(oldVersion);
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Delete directory recursively.
     */
    private static void deleteDirectory(Path path) {
        try {
            if (Files.isDirectory(path)) {
                Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception e) {
                            // Ignore
                        }
                    });
            } else {
                Files.deleteIfExists(path);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private static synchronized void registerShutdownHook() {
        if (!shutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(CleanupUtils::runCleanup));
            shutdownHookRegistered = true;
        }
    }

    /**
     * Cleanup task record.
     */
    private record CleanupTask(Runnable task, String description, int priority) {}

    /**
     * Cleanup scope - Auto-closeable for try-with-resources.
     */
    public static class CleanupScope implements AutoCloseable {
        private final List<Runnable> tasks = new ArrayList<>();

        public CleanupScope register(Runnable task) {
            tasks.add(task);
            return this;
        }

        @Override
        public void close() {
            List<Exception> errors = new ArrayList<>();

            // Run in reverse order
            for (int i = tasks.size() - 1; i >= 0; i--) {
                try {
                    tasks.get(i).run();
                } catch (Exception e) {
                    errors.add(e);
                }
            }

            if (!errors.isEmpty()) {
                throw new RuntimeException("Cleanup errors: " + errors.size());
            }
        }
    }
}