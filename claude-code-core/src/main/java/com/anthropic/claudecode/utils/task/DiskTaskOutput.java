/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code disk task output utilities
 */
package com.anthropic.claudecode.utils.task;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Disk task output for async disk writes with write queue.
 * Handles large task outputs that spill over from memory to disk.
 */
public final class DiskTaskOutput {
    private DiskTaskOutput() {}

    // Maximum task output bytes (5GB cap)
    public static final long MAX_TASK_OUTPUT_BYTES = 5L * 1024 * 1024 * 1024;

    // Write queue for async writes
    private static final ConcurrentLinkedQueue<WriteRequest> writeQueue = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean isWriting = new AtomicBoolean(false);
    private static volatile boolean shutdown = false;

    // Background writer thread
    private static final Thread writerThread;

    static {
        writerThread = new Thread(() -> {
            while (!shutdown) {
                try {
                    WriteRequest req = writeQueue.poll();
                    if (req != null) {
                        executeWrite(req);
                    } else {
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    // Log error and continue
                }
            }
        }, "disk-task-output-writer");
        writerThread.start();
    }

    /**
     * Write request for async disk write.
     */
    private static final class WriteRequest {
        final Path file;
        final byte[] data;
        final long offset;
        final CompletableFuture<Void> future;
        final boolean append;

        WriteRequest(Path file, byte[] data, long offset, boolean append, CompletableFuture<Void> future) {
            this.file = file;
            this.data = data;
            this.offset = offset;
            this.append = append;
            this.future = future;
        }
    }

    /**
     * Execute a write request to disk.
     */
    private static void executeWrite(WriteRequest req) {
        try {
            // Check symlink security - don't follow symlinks for task output files
            if (Files.isSymbolicLink(req.file)) {
                req.future.completeExceptionally(new IOException("Cannot write to symlink: " + req.file));
                return;
            }

            // Ensure parent directory exists
            Path parent = req.file.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            // Write data
            if (req.append) {
                // Append mode - write to end of file
                Files.write(req.file, req.data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                // Write at specific offset
                try (FileChannel channel = FileChannel.open(req.file,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE)) {
                    channel.position(req.offset);
                    channel.write(java.nio.ByteBuffer.wrap(req.data));
                }
            }

            // Check size cap
            long size = Files.size(req.file);
            if (size > MAX_TASK_OUTPUT_BYTES) {
                // Truncate to cap
                truncateToCap(req.file);
            }

            req.future.complete(null);

        } catch (Exception e) {
            req.future.completeExceptionally(e);
        }
    }

    /**
     * Truncate file to MAX_TASK_OUTPUT_BYTES cap.
     */
    private static void truncateToCap(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.truncate(MAX_TASK_OUTPUT_BYTES);
        }
    }

    /**
     * Queue an async write to disk.
     */
    public static CompletableFuture<Void> queueWrite(Path file, byte[] data, long offset) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        WriteRequest req = new WriteRequest(file, data, offset, false, future);
        writeQueue.add(req);
        return future;
    }

    /**
     * Queue an async append to disk.
     */
    public static CompletableFuture<Void> queueAppend(Path file, byte[] data) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        WriteRequest req = new WriteRequest(file, data, 0, true, future);
        writeQueue.add(req);
        return future;
    }

    /**
     * Check if file is a symlink (security check).
     * O_NOFOLLOW equivalent - don't follow symlinks.
     */
    public static boolean isSafePath(Path path) {
        // Check if path or any parent component is a symlink
        try {
            Path current = path;
            while (current != null) {
                if (Files.isSymbolicLink(current)) {
                    return false;
                }
                current = current.getParent();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Create a safe task output file (no symlinks).
     */
    public static Path createSafeTaskOutputFile(Path baseDir, String taskId) throws IOException {
        Path file = baseDir.resolve("task-" + taskId + ".output");

        // Ensure no symlinks in path
        if (!isSafePath(file)) {
            throw new IOException("Unsafe path (contains symlink): " + file);
        }

        // Create file if not exists
        if (!Files.exists(file)) {
            Files.createFile(file);
        }

        return file;
    }

    /**
     * Read task output from disk with offset.
     */
    public static String readOutput(Path file, long offset, int limit) throws IOException {
        if (!isSafePath(file)) {
            throw new IOException("Unsafe path (contains symlink): " + file);
        }

        if (!Files.exists(file)) {
            return "";
        }

        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            channel.position(offset);

            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(Math.min(limit, 1024 * 1024));
            int read = channel.read(buffer);
            if (read <= 0) {
                return "";
            }

            buffer.flip();
            return new String(buffer.array(), 0, read);
        }
    }

    /**
     * Get file size for task output.
     */
    public static long getFileSize(Path file) throws IOException {
        if (!Files.exists(file)) {
            return 0;
        }
        return Files.size(file);
    }

    /**
     * Delete task output file.
     */
    public static boolean deleteOutputFile(Path file) {
        try {
            if (isSafePath(file) && Files.exists(file)) {
                Files.delete(file);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Shutdown the disk output system.
     */
    public static void shutdown() {
        shutdown = true;
        writerThread.interrupt();

        // Wait for remaining writes to complete
        while (!writeQueue.isEmpty()) {
            WriteRequest req = writeQueue.poll();
            if (req != null) {
                try {
                    executeWrite(req);
                } catch (Exception e) {
                    req.future.completeExceptionally(e);
                }
            }
        }
    }

    /**
     * Get current write queue size.
     */
    public static int getQueueSize() {
        return writeQueue.size();
    }
}