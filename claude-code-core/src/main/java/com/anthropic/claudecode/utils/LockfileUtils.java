/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/lockfile
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.util.concurrent.*;
import java.time.*;

/**
 * Lockfile utilities - File-based locking for cross-process synchronization.
 */
public final class LockfileUtils {
    private static final ConcurrentHashMap<String, LockHandle> activeLocks = new ConcurrentHashMap<>();

    /**
     * Try to acquire a file lock.
     */
    public static LockHandle tryAcquire(Path lockPath) {
        try {
            // Check if already locked by this process
            LockHandle existing = activeLocks.get(lockPath.toString());
            if (existing != null && existing.isValid()) {
                return existing;
            }

            // Check if lock file exists and is stale
            if (Files.exists(lockPath)) {
                String content = Files.readString(lockPath);
                long lockTime = parseLockTime(content);

                // Consider lock stale after 1 hour
                if (lockTime > 0 && System.currentTimeMillis() - lockTime > 3600000) {
                    Files.delete(lockPath);
                } else {
                    return null; // Lock is held by another process
                }
            }

            // Create lock file
            Files.createDirectories(lockPath.getParent());
            String lockContent = System.currentTimeMillis() + ":" + ProcessHandle.current().pid();
            Files.writeString(lockPath, lockContent);

            LockHandle handle = new LockHandle(lockPath, true);
            activeLocks.put(lockPath.toString(), handle);
            return handle;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Release a file lock.
     */
    public static void release(LockHandle handle) {
        if (handle == null || !handle.isValid()) {
            return;
        }

        try {
            Path lockPath = handle.getPath();
            if (Files.exists(lockPath)) {
                // Verify we own the lock
                String content = Files.readString(lockPath);
                String[] parts = content.split(":");
                if (parts.length >= 2) {
                    long pid = Long.parseLong(parts[1]);
                    if (pid == ProcessHandle.current().pid()) {
                        Files.delete(lockPath);
                    }
                }
            }

            handle.invalidate();
            activeLocks.remove(lockPath.toString());
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Check if a lock is currently held.
     */
    public static boolean isLocked(Path lockPath) {
        if (!Files.exists(lockPath)) {
            return false;
        }

        try {
            String content = Files.readString(lockPath);
            long lockTime = parseLockTime(content);

            // Consider lock stale after 1 hour
            return System.currentTimeMillis() - lockTime <= 3600000;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Force release a lock (use with caution).
     */
    public static void forceRelease(Path lockPath) {
        try {
            Files.deleteIfExists(lockPath);
            activeLocks.remove(lockPath.toString());
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Execute a task with a lock.
     */
    public static <T> T withLock(Path lockPath, Callable<T> task) throws Exception {
        LockHandle handle = tryAcquire(lockPath);
        if (handle == null) {
            throw new IllegalStateException("Could not acquire lock: " + lockPath);
        }

        try {
            return task.call();
        } finally {
            release(handle);
        }
    }

    /**
     * Execute a task with a lock, with timeout.
     */
    public static <T> T withLock(Path lockPath, long timeoutMs, Callable<T> task) throws Exception {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            LockHandle handle = tryAcquire(lockPath);
            if (handle != null) {
                try {
                    return task.call();
                } finally {
                    release(handle);
                }
            }
            Thread.sleep(100);
        }

        throw new TimeoutException("Could not acquire lock within timeout: " + lockPath);
    }

    private static long parseLockTime(String content) {
        try {
            String[] parts = content.split(":");
            return Long.parseLong(parts[0]);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Lock handle record.
     */
    public static class LockHandle {
        private final Path path;
        private volatile boolean valid;

        public LockHandle(Path path, boolean valid) {
            this.path = path;
            this.valid = valid;
        }

        public Path getPath() {
            return path;
        }

        public boolean isValid() {
            return valid;
        }

        public void invalidate() {
            this.valid = false;
        }
    }
}