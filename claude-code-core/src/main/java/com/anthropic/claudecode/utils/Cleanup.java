/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code cleanup utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

/**
 * Cleanup utilities for old files and directories.
 */
public final class Cleanup {
    private Cleanup() {}

    private static final int DEFAULT_CLEANUP_PERIOD_DAYS = 30;
    private static final long ONE_DAY_MS = 24 * 60 * 60 * 1000L;

    /**
     * Cleanup result record.
     */
    public record CleanupResult(int messages, int errors) {
        public CleanupResult add(CleanupResult other) {
            return new CleanupResult(messages + other.messages, errors + other.errors);
        }
    }

    /**
     * Get the cutoff date for cleanup based on settings.
     */
    public static Instant getCutoffDate() {
        int cleanupPeriodDays = DEFAULT_CLEANUP_PERIOD_DAYS;
        String envValue = System.getenv("CLAUDE_CLEANUP_PERIOD_DAYS");
        if (envValue != null && !envValue.isEmpty()) {
            try {
                cleanupPeriodDays = Integer.parseInt(envValue);
            } catch (NumberFormatException e) {
                // Use default
            }
        }
        return Instant.now().minus(cleanupPeriodDays, ChronoUnit.DAYS);
    }

    /**
     * Convert filename to date.
     */
    public static Instant convertFileNameToDate(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        String isoStr = filename.split("\\.")[0]
                .replaceFirst("T(\\d{2})-(\\d{2})-(\\d{2})-(\\d{3})Z", "T$1:$2:$3.$4Z");
        try {
            return Instant.parse(isoStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Clean up old files in a directory.
     */
    public static CleanupResult cleanupOldFilesInDirectory(Path dirPath, Instant cutoffDate) {
        int[] counts = new int[]{0, 0}; // [messages, errors]

        if (!Files.exists(dirPath)) {
            return new CleanupResult(0, 0);
        }

        try (var stream = Files.list(dirPath)) {
            stream.forEach(file -> {
                try {
                    if (Files.isRegularFile(file)) {
                        String name = file.getFileName().toString();
                        Instant timestamp = convertFileNameToDate(name);
                        if (timestamp != null && timestamp.isBefore(cutoffDate)) {
                            Files.delete(file);
                            counts[0]++;
                        }
                    }
                } catch (IOException e) {
                    counts[1]++;
                }
            });
        } catch (IOException e) {
            // Directory doesn't exist or can't be read
        }

        return new CleanupResult(counts[0], counts[1]);
    }

    /**
     * Delete a file if it's older than the cutoff date.
     */
    public static boolean deleteIfOld(Path filePath, Instant cutoffDate) throws IOException {
        if (!Files.exists(filePath)) {
            return false;
        }
        FileTime mtime = Files.getLastModifiedTime(filePath);
        if (mtime.toInstant().isBefore(cutoffDate)) {
            Files.delete(filePath);
            return true;
        }
        return false;
    }

    /**
     * Try to remove an empty directory.
     */
    public static boolean tryRmdir(Path dirPath) {
        try {
            Files.deleteIfExists(dirPath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Clean up old session files.
     */
    public static CleanupResult cleanupOldSessionFiles(Path projectsDir) {
        int[] counts = new int[]{0, 0}; // [messages, errors]
        Instant cutoffDate = getCutoffDate();

        if (!Files.exists(projectsDir)) {
            return new CleanupResult(0, 0);
        }

        try (var projectStream = Files.list(projectsDir)) {
            projectStream.filter(Files::isDirectory).forEach(projectDir -> {
                try (var entryStream = Files.list(projectDir)) {
                    entryStream.forEach(entry -> {
                        try {
                            if (Files.isRegularFile(entry)) {
                                String name = entry.getFileName().toString();
                                if (name.endsWith(".jsonl") || name.endsWith(".cast")) {
                                    if (deleteIfOld(entry, cutoffDate)) {
                                        counts[0]++;
                                    }
                                }
                            } else if (Files.isDirectory(entry)) {
                                // Clean up session directory
                                cleanupSessionDirectory(entry, cutoffDate);
                                tryRmdir(entry);
                            }
                        } catch (IOException e) {
                            counts[1]++;
                        }
                    });
                } catch (IOException e) {
                    counts[1]++;
                }
                tryRmdir(projectDir);
            });
        } catch (IOException e) {
            // Ignore
        }

        return new CleanupResult(counts[0], counts[1]);
    }

    /**
     * Clean up a session directory.
     */
    private static void cleanupSessionDirectory(Path sessionDir, Instant cutoffDate) {
        Path toolResultsDir = sessionDir.resolve("tool-results");
        if (!Files.exists(toolResultsDir)) {
            return;
        }

        try (var toolStream = Files.list(toolResultsDir)) {
            toolStream.forEach(toolDir -> {
                try {
                    if (Files.isDirectory(toolDir)) {
                        try (var fileStream = Files.list(toolDir)) {
                            fileStream.filter(Files::isRegularFile).forEach(file -> {
                                try {
                                    deleteIfOld(file, cutoffDate);
                                } catch (IOException e) {
                                    // Ignore
                                }
                            });
                        }
                        tryRmdir(toolDir);
                    } else if (Files.isRegularFile(toolDir)) {
                        deleteIfOld(toolDir, cutoffDate);
                    }
                } catch (IOException e) {
                    // Ignore
                }
            });
        } catch (IOException e) {
            // Ignore
        }

        tryRmdir(toolResultsDir);
    }

    /**
     * Clean up old debug logs.
     */
    public static CleanupResult cleanupOldDebugLogs(Path debugDir) {
        int[] counts = new int[]{0, 0}; // [messages, errors]
        Instant cutoffDate = getCutoffDate();

        if (!Files.exists(debugDir)) {
            return new CleanupResult(0, 0);
        }

        try (var stream = Files.list(debugDir)) {
            stream.filter(Files::isRegularFile)
                  .filter(f -> f.getFileName().toString().endsWith(".txt"))
                  .filter(f -> !f.getFileName().toString().equals("latest"))
                  .forEach(file -> {
                      try {
                          if (deleteIfOld(file, cutoffDate)) {
                              counts[0]++;
                          }
                      } catch (IOException e) {
                          counts[1]++;
                      }
                  });
        } catch (IOException e) {
            // Ignore
        }

        return new CleanupResult(counts[0], counts[1]);
    }

    /**
     * Clean up old plan files.
     */
    public static CleanupResult cleanupOldPlanFiles(Path plansDir) {
        return cleanupFilesWithExtension(plansDir, ".md");
    }

    /**
     * Clean up files with a specific extension.
     */
    public static CleanupResult cleanupFilesWithExtension(Path dir, String extension) {
        int[] counts = new int[]{0, 0}; // [messages, errors]
        Instant cutoffDate = getCutoffDate();

        if (!Files.exists(dir)) {
            return new CleanupResult(0, 0);
        }

        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                  .filter(f -> f.getFileName().toString().endsWith(extension))
                  .forEach(file -> {
                      try {
                          if (deleteIfOld(file, cutoffDate)) {
                              counts[0]++;
                          }
                      } catch (IOException e) {
                          counts[1]++;
                      }
                  });
        } catch (IOException e) {
            // Ignore
        }

        tryRmdir(dir);
        return new CleanupResult(counts[0], counts[1]);
    }

    /**
     * Run all background cleanup tasks.
     */
    public static CompletableFuture<CleanupResult> runBackgroundCleanup() {
        return CompletableFuture.supplyAsync(() -> {
            CleanupResult total = new CleanupResult(0, 0);
            String home = System.getProperty("user.home");
            Path configDir = Paths.get(home, ".claude");

            // Clean up message files
            total = total.add(cleanupOldFilesInDirectory(configDir.resolve("logs"), getCutoffDate()));

            // Clean up debug logs
            total = total.add(cleanupOldDebugLogs(configDir.resolve("debug")));

            // Clean up plan files
            total = total.add(cleanupOldPlanFiles(configDir.resolve("plans")));

            // Clean up session files
            total = total.add(cleanupOldSessionFiles(configDir.resolve("projects")));

            return total;
        });
    }
}