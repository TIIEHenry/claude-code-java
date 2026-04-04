/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/autoDream/autoDream
 */
package com.anthropic.claudecode.services.autodream;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.time.*;

/**
 * Auto dream service - Background memory consolidation.
 */
public final class AutoDreamService {
    private final AutoDreamConfig config;
    private final ExecutorService executor;
    private final Path lockPath;
    private final Path sessionPath;
    private volatile boolean running;
    private Instant lastConsolidatedAt;

    public AutoDreamService(AutoDreamConfig config, Path projectPath) {
        this.config = config;
        this.executor = Executors.newSingleThreadExecutor();
        this.lockPath = projectPath.resolve(".claude").resolve("dream.lock");
        this.sessionPath = projectPath.resolve(".claude").resolve("sessions");
        this.running = false;
        this.lastConsolidatedAt = null;
    }

    /**
     * Check if the gate is open for consolidation.
     */
    public boolean isGateOpen() {
        if (!config.isEnabled()) {
            return false;
        }

        // Check time gate
        if (lastConsolidatedAt != null) {
            Duration sinceLast = Duration.between(lastConsolidatedAt, Instant.now());
            if (sinceLast.toHours() < config.getMinHours()) {
                return false;
            }
        }

        // Check session gate
        int sessionCount = countSessionsSince(lastConsolidatedAt);
        if (sessionCount < config.getMinSessions()) {
            return false;
        }

        // Check lock
        if (isConsolidationInProgress()) {
            return false;
        }

        return true;
    }

    /**
     * Try to acquire consolidation lock.
     */
    public boolean tryAcquireLock() {
        try {
            if (Files.exists(lockPath)) {
                // Check if lock is stale (older than 1 hour)
                long lockAge = System.currentTimeMillis() -
                    Files.getLastModifiedTime(lockPath).toMillis();
                if (lockAge < 3600000) {
                    return false;
                }
            }

            Files.createDirectories(lockPath.getParent());
            Files.writeString(lockPath, Instant.now().toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Release consolidation lock.
     */
    public void releaseLock() {
        try {
            Files.deleteIfExists(lockPath);
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Rollback consolidation lock (on error).
     */
    public void rollbackLock() {
        releaseLock();
    }

    /**
     * Perform consolidation.
     */
    public CompletableFuture<ConsolidationResult> performConsolidation() {
        if (!isGateOpen()) {
            return CompletableFuture.completedFuture(
                new ConsolidationResult(false, "Gate not open", 0)
            );
        }

        if (!tryAcquireLock()) {
            return CompletableFuture.completedFuture(
                new ConsolidationResult(false, "Could not acquire lock", 0)
            );
        }

        running = true;

        return CompletableFuture.supplyAsync(() -> {
            try {
                // List sessions to consolidate
                List<Path> sessions = listSessionsToConsolidate();

                // Build consolidation prompt
                String prompt = buildConsolidationPrompt(sessions);

                // Run consolidation agent
                int memoriesExtracted = runConsolidationAgent(prompt);

                // Update last consolidated time
                lastConsolidatedAt = Instant.now();
                writeLastConsolidatedAt();

                return new ConsolidationResult(true, null, memoriesExtracted);
            } catch (Exception e) {
                rollbackLock();
                return new ConsolidationResult(false, e.getMessage(), 0);
            } finally {
                running = false;
                releaseLock();
            }
        }, executor);
    }

    /**
     * Count sessions since a given time.
     */
    private int countSessionsSince(Instant since) {
        if (!Files.exists(sessionPath)) {
            return 0;
        }

        try {
            long sinceMillis = since != null ? since.toEpochMilli() : 0;
            return (int) Files.list(sessionPath)
                .filter(Files::isRegularFile)
                .filter(p -> {
                    try {
                        return Files.getLastModifiedTime(p).toMillis() > sinceMillis;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Check if consolidation is in progress.
     */
    private boolean isConsolidationInProgress() {
        if (!Files.exists(lockPath)) {
            return false;
        }

        try {
            long lockAge = System.currentTimeMillis() -
                Files.getLastModifiedTime(lockPath).toMillis();
            return lockAge < 3600000; // 1 hour
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * List sessions to consolidate.
     */
    private List<Path> listSessionsToConsolidate() {
        List<Path> sessions = new ArrayList<>();
        if (!Files.exists(sessionPath)) {
            return sessions;
        }

        try {
            long sinceMillis = lastConsolidatedAt != null ?
                lastConsolidatedAt.toEpochMilli() : 0;

            Files.list(sessionPath)
                .filter(Files::isRegularFile)
                .filter(p -> {
                    try {
                        return Files.getLastModifiedTime(p).toMillis() > sinceMillis;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .forEach(sessions::add);
        } catch (Exception e) {
            // Return empty list
        }

        return sessions;
    }

    /**
     * Build consolidation prompt.
     */
    private String buildConsolidationPrompt(List<Path> sessions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Consolidate memories from the following sessions:\n\n");

        for (Path session : sessions) {
            sb.append("Session: ").append(session.getFileName()).append("\n");
            // Implementation would read session content
        }

        return sb.toString();
    }

    /**
     * Run consolidation agent.
     */
    private int runConsolidationAgent(String prompt) {
        // Implementation would run a forked agent
        return 0;
    }

    /**
     * Write last consolidated time.
     */
    private void writeLastConsolidatedAt() {
        try {
            Path statePath = lockPath.resolveSibling("dream.state");
            Files.writeString(statePath, lastConsolidatedAt.toString());
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Read last consolidated time.
     */
    public void readLastConsolidatedAt() {
        try {
            Path statePath = lockPath.resolveSibling("dream.state");
            if (Files.exists(statePath)) {
                String content = Files.readString(statePath).trim();
                lastConsolidatedAt = Instant.parse(content);
            }
        } catch (Exception e) {
            lastConsolidatedAt = null;
        }
    }

    /**
     * Shutdown the service.
     */
    public void shutdown() {
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    /**
     * Check if consolidation is running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Get last consolidated time.
     */
    public Instant getLastConsolidatedAt() {
        return lastConsolidatedAt;
    }

    /**
     * Consolidation result record.
     */
    public record ConsolidationResult(
        boolean success,
        String error,
        int memoriesExtracted
    ) {}
}