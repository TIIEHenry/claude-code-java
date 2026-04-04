/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code cron tasks lock
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Scheduler lease lock for scheduled_tasks.json.
 * When multiple sessions run in the same project, only one drives the scheduler.
 */
public final class CronTasksLock {
    private CronTasksLock() {}

    private static final String LOCK_FILE_REL = ".claude/scheduled_tasks.lock";

    // Lock file path
    private static Path lockPath = null;

    /**
     * Scheduler lock data.
     */
    private record SchedulerLock(String sessionId, int pid, long acquiredAt) {}

    /**
     * Options for lock operations.
     */
    public record SchedulerLockOptions(Path dir, String lockIdentity) {}

    /**
     * Get the lock file path.
     */
    private static Path getLockPath(Path dir) {
        if (dir != null) {
            return dir.resolve(LOCK_FILE_REL);
        }
        return Paths.get(System.getProperty("user.dir")).resolve(LOCK_FILE_REL);
    }

    /**
     * Read the lock file.
     */
    private static SchedulerLock readLock(Path dir) {
        try {
            Path path = getLockPath(dir);
            if (!Files.exists(path)) {
                return null;
            }

            String raw = Files.readString(path);
            Map<String, Object> parsed = SlowOperations.jsonParseMap(raw);

            return new SchedulerLock(
                    (String) parsed.get("sessionId"),
                    parsed.get("pid") instanceof Number ? ((Number) parsed.get("pid")).intValue() : 0,
                    parsed.get("acquiredAt") instanceof Number ? ((Number) parsed.get("acquiredAt")).longValue() : 0
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Try to create an exclusive lock file.
     */
    private static boolean tryCreateExclusive(SchedulerLock lock, Path dir) {
        try {
            Path path = getLockPath(dir);
            Files.createDirectories(path.getParent());

            // Try to create file exclusively
            String content = SlowOperations.jsonStringify(Map.of(
                    "sessionId", lock.sessionId(),
                    "pid", lock.pid(),
                    "acquiredAt", lock.acquiredAt()
            ));

            try {
                Files.writeString(path, content, StandardOpenOption.CREATE_NEW);
                return true;
            } catch (FileAlreadyExistsException e) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a process is running.
     */
    private static boolean isProcessRunning(int pid) {
        try {
            ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
            return handle != null && handle.isAlive();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Try to acquire the scheduler lock.
     */
    public static boolean tryAcquireSchedulerLock(SchedulerLockOptions opts) {
        Path dir = opts != null ? opts.dir() : null;
        String sessionId = opts != null && opts.lockIdentity() != null
                ? opts.lockIdentity()
                : UUID.randomUUID().toString().substring(0, 8);

        SchedulerLock lock = new SchedulerLock(
                sessionId,
                (int) ProcessHandle.current().pid(),
                System.currentTimeMillis()
        );

        if (tryCreateExclusive(lock, dir)) {
            Debug.log("[ScheduledTasks] acquired scheduler lock (PID " + lock.pid() + ")");
            return true;
        }

        SchedulerLock existing = readLock(dir);

        // Already ours
        if (existing != null && existing.sessionId().equals(sessionId)) {
            // Update PID if changed
            if (existing.pid() != lock.pid()) {
                try {
                    Path path = getLockPath(dir);
                    Files.writeString(path, SlowOperations.jsonStringify(Map.of(
                            "sessionId", lock.sessionId(),
                            "pid", lock.pid(),
                            "acquiredAt", lock.acquiredAt()
                    )));
                } catch (Exception e) {
                    // Ignore
                }
            }
            return true;
        }

        // Another live session
        if (existing != null && isProcessRunning(existing.pid())) {
            Debug.log("[ScheduledTasks] scheduler lock held by session " +
                    existing.sessionId() + " (PID " + existing.pid() + ")");
            return false;
        }

        // Stale - try to recover
        if (existing != null) {
            Debug.log("[ScheduledTasks] recovering stale scheduler lock from PID " + existing.pid());
            try {
                Files.delete(getLockPath(dir));
            } catch (Exception e) {
                // Ignore
            }
        }

        return tryCreateExclusive(lock, dir);
    }

    /**
     * Release the scheduler lock.
     */
    public static void releaseSchedulerLock(SchedulerLockOptions opts) {
        Path dir = opts != null ? opts.dir() : null;
        String sessionId = opts != null && opts.lockIdentity() != null
                ? opts.lockIdentity()
                : UUID.randomUUID().toString().substring(0, 8);

        SchedulerLock existing = readLock(dir);
        if (existing == null || !existing.sessionId().equals(sessionId)) {
            return;
        }

        try {
            Files.delete(getLockPath(dir));
            Debug.log("[ScheduledTasks] released scheduler lock");
        } catch (Exception e) {
            // Already gone
        }
    }
}