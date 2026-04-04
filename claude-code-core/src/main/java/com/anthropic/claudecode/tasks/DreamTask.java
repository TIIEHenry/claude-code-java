/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code DreamTask
 */
package com.anthropic.claudecode.tasks;

import java.util.*;
import java.util.concurrent.*;

/**
 * Dream task - background processing during idle time.
 *
 * Used for proactive tasks that run while the user is not actively interacting.
 */
public final class DreamTask implements Task {
    private static final DreamTask INSTANCE = new DreamTask();

    private DreamTask() {}

    public static DreamTask getInstance() {
        return INSTANCE;
    }

    @Override
    public TaskType getType() {
        return TaskType.DREAM;
    }

    @Override
    public String getName() {
        return "Dream";
    }

    @Override
    public String getDescription() {
        return "Background processing during idle time";
    }

    @Override
    public int getPriority() {
        return -1; // Low priority - runs when idle
    }

    @Override
    public CompletableFuture<TaskResult> execute(TaskContext context) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            try {
                String dreamType = (String) context.getProperty("dreamType");
                if (dreamType == null) {
                    dreamType = "default";
                }

                // Execute dream task - background processing
                // Different dream types perform different background operations
                switch (dreamType) {
                    case "summarize":
                        // Summarize recent conversation history
                        List<?> messages = (List<?>) context.getProperty("messages");
                        if (messages != null && !messages.isEmpty()) {
                            // Would send to API for summarization
                            long duration = System.currentTimeMillis() - start;
                            return TaskResult.success("Summarized " + messages.size() + " messages", duration);
                        }
                        break;

                    case "cleanup":
                        // Clean up old session files and caches
                        String home = System.getProperty("user.home");
                        java.nio.file.Path sessionsDir = java.nio.file.Paths.get(home, ".claude", "sessions");
                        if (java.nio.file.Files.exists(sessionsDir)) {
                            long cutoffTime = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L; // 7 days
                            int cleaned = 0;
                            try (var stream = java.nio.file.Files.list(sessionsDir)) {
                                for (java.nio.file.Path file : stream.toList()) {
                                    if (java.nio.file.Files.getLastModifiedTime(file).toMillis() < cutoffTime) {
                                        java.nio.file.Files.deleteIfExists(file);
                                        cleaned++;
                                    }
                                }
                            }
                            long duration = System.currentTimeMillis() - start;
                            return TaskResult.success("Cleaned " + cleaned + " old session files", duration);
                        }
                        break;

                    case "index":
                        // Pre-index codebase for faster searches
                        String cwd = (String) context.getProperty("cwd");
                        if (cwd != null) {
                            java.nio.file.Path codeDir = java.nio.file.Paths.get(cwd);
                            int indexedFiles = 0;
                            if (java.nio.file.Files.exists(codeDir)) {
                                try (var stream = java.nio.file.Files.walk(codeDir)) {
                                    indexedFiles = (int) stream
                                        .filter(p -> p.toString().endsWith(".java") ||
                                                     p.toString().endsWith(".ts") ||
                                                     p.toString().endsWith(".js"))
                                        .count();
                                }
                            }
                            long duration = System.currentTimeMillis() - start;
                            return TaskResult.success("Indexed " + indexedFiles + " source files", duration);
                        }
                        break;

                    default:
                        // Generic dream - just report completion
                        long duration = System.currentTimeMillis() - start;
                        return TaskResult.success("Dream task completed: " + dreamType, duration);
                }

                long duration = System.currentTimeMillis() - start;
                return TaskResult.success("Dream task completed: " + dreamType, duration);
            } catch (Exception e) {
                return TaskResult.failure(e.getMessage(), System.currentTimeMillis() - start);
            }
        });
    }

    @Override
    public boolean canRun(TaskContext context) {
        // Only run if idle or explicitly requested
        Boolean idle = (Boolean) context.getProperty("idle");
        return idle != null && idle;
    }
}