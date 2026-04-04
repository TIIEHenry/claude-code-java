/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code file locking utilities
 */
package com.anthropic.claudecode.utils.file;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * File locking utilities for cross-process coordination.
 */
public final class FileLocks {
    private FileLocks() {}

    private static final ConcurrentHashMap<String, FileLock> locks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, FileChannel> channels = new ConcurrentHashMap<>();

    /**
     * Try to acquire a lock on a file.
     */
    public static boolean tryLock(Path file) {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            FileChannel channel = FileChannel.open(
                    file,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE
            );

            FileLock lock = channel.tryLock();
            if (lock != null) {
                String key = file.toString();
                locks.put(key, lock);
                channels.put(key, channel);
                return true;
            }

            channel.close();
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Try to acquire a lock with timeout.
     */
    public static boolean tryLock(Path file, long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);

        while (System.nanoTime() < deadline) {
            if (tryLock(file)) {
                return true;
            }
            Thread.sleep(100);
        }

        return false;
    }

    /**
     * Release a lock on a file.
     */
    public static void unlock(Path file) {
        String key = file.toString();
        FileLock lock = locks.remove(key);
        FileChannel channel = channels.remove(key);

        if (lock != null) {
            try {
                lock.release();
            } catch (IOException e) {
                // Ignore
            }
        }

        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * Check if a file is locked by another process.
     */
    public static boolean isLocked(Path file) {
        try {
            if (!Files.exists(file)) {
                return false;
            }

            FileChannel channel = FileChannel.open(
                    file,
                    StandardOpenOption.WRITE
            );

            FileLock lock = channel.tryLock();
            if (lock != null) {
                lock.release();
                channel.close();
                return false;
            }

            channel.close();
            return true;
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * Run with exclusive lock.
     */
    public static <T> T withLock(Path file, java.util.function.Supplier<T> supplier) {
        if (!tryLock(file)) {
            throw new RuntimeException("Could not acquire lock on: " + file);
        }

        try {
            return supplier.get();
        } finally {
            unlock(file);
        }
    }

    /**
     * Run with exclusive lock (void).
     */
    public static void withLock(Path file, Runnable runnable) {
        if (!tryLock(file)) {
            throw new RuntimeException("Could not acquire lock on: " + file);
        }

        try {
            runnable.run();
        } finally {
            unlock(file);
        }
    }

    /**
     * Create a lock file path for a given resource.
     */
    public static Path getLockFilePath(Path resource) {
        return Paths.get(resource.toString() + ".lock");
    }

    /**
     * Acquire a process-wide lock.
     */
    public static ProcessLock acquireProcessLock(String name) {
        return new ProcessLock(name);
    }

    /**
     * Process-wide lock implementation.
     */
    public static class ProcessLock implements AutoCloseable {
        private final String name;
        private final Path lockFile;
        private volatile boolean held = false;

        public ProcessLock(String name) {
            this.name = name;
            this.lockFile = Paths.get(System.getProperty("java.io.tmpdir"), "claude-code-" + name + ".lock");
        }

        public boolean tryAcquire() {
            if (held) return true;

            held = tryLock(lockFile);
            return held;
        }

        public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
            if (held) return true;

            held = tryLock(lockFile, timeout, unit);
            return held;
        }

        public boolean isHeld() {
            return held;
        }

        @Override
        public void close() {
            if (held) {
                unlock(lockFile);
                held = false;
            }
        }
    }

    /**
     * Read/write lock for files.
     */
    public static class FileReadWriteLock {
        private final Path file;
        private final AtomicInteger readCount = new AtomicInteger(0);
        private final AtomicBoolean writeLocked = new AtomicBoolean(false);
        private final ReentrantLock lock = new ReentrantLock();

        public FileReadWriteLock(Path file) {
            this.file = file;
        }

        public void lockRead() {
            lock.lock();
            try {
                while (writeLocked.get()) {
                    Thread.onSpinWait();
                }
                readCount.incrementAndGet();
            } finally {
                lock.unlock();
            }
        }

        public void unlockRead() {
            readCount.decrementAndGet();
        }

        public void lockWrite() {
            lock.lock();
            try {
                while (readCount.get() > 0 || writeLocked.get()) {
                    Thread.onSpinWait();
                }
                writeLocked.set(true);
            } finally {
                lock.unlock();
            }
        }

        public void unlockWrite() {
            writeLocked.set(false);
        }

        public <T> T withReadLock(java.util.function.Supplier<T> supplier) {
            lockRead();
            try {
                return supplier.get();
            } finally {
                unlockRead();
            }
        }

        public <T> T withWriteLock(java.util.function.Supplier<T> supplier) {
            lockWrite();
            try {
                return supplier.get();
            } finally {
                unlockWrite();
            }
        }
    }
}