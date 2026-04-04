/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code task output utilities
 */
package com.anthropic.claudecode.utils.task;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Task output handler managing stdout/stderr with memory buffering and disk spillover.
 * Uses a circular buffer for recent lines and optional disk storage for large outputs.
 */
public class TaskOutput {
    // Maximum in-memory bytes before spilling to disk
    public static final int MAX_MEMORY_BYTES = 1024 * 1024; // 1MB

    // Maximum recent lines in circular buffer
    public static final int MAX_RECENT_LINES = 1000;

    // Circular buffer for recent lines
    private final CircularBuffer<String> recentLines;

    // Memory buffer for current output
    private final StringBuilder memoryBuffer;

    // Disk output file (optional, for large outputs)
    private volatile Path diskFile;

    // Task ID
    private final String taskId;

    // Output offset (for reading)
    private volatile long outputOffset;

    // Polling executor (for file mode)
    private ScheduledExecutorService pollExecutor;

    // Output type
    public enum OutputType {
        STDOUT, STDERR, COMBINED
    }

    private final OutputType outputType;

    /**
     * Create task output handler.
     */
    public TaskOutput(String taskId, OutputType outputType) {
        this.taskId = taskId;
        this.outputType = outputType;
        this.recentLines = new CircularBuffer<>(MAX_RECENT_LINES);
        this.memoryBuffer = new StringBuilder();
        this.outputOffset = 0;
    }

    /**
     * Write data to task output.
     */
    public synchronized void write(String data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        // Add to memory buffer
        memoryBuffer.append(data);

        // Split into lines and add to circular buffer
        String[] lines = data.split("\n");
        for (String line : lines) {
            recentLines.add(line);
        }

        // Check if we need to spill to disk
        if (memoryBuffer.length() > MAX_MEMORY_BYTES) {
            spillToDisk();
        }
    }

    /**
     * Write byte data to task output.
     */
    public synchronized void write(byte[] data) {
        write(new String(data));
    }

    /**
     * Spill memory buffer to disk.
     */
    private void spillToDisk() {
        try {
            if (diskFile == null) {
                Path baseDir = Paths.get(System.getProperty("java.io.tmpdir"), "claude-code", "task-output");
                diskFile = DiskTaskOutput.createSafeTaskOutputFile(baseDir, taskId);
            }

            // Write current memory buffer to disk
            byte[] data = memoryBuffer.toString().getBytes();
            DiskTaskOutput.queueAppend(diskFile, data);

            // Clear memory buffer but keep recent lines
            memoryBuffer.setLength(0);

        } catch (Exception e) {
            // Keep in memory if disk write fails
        }
    }

    /**
     * Get all output as string.
     */
    public synchronized String getOutput() {
        if (diskFile != null && Files.exists(diskFile)) {
            try {
                // Read from disk + current memory buffer
                String diskContent = DiskTaskOutput.readOutput(diskFile, 0, Integer.MAX_VALUE);
                return diskContent + memoryBuffer.toString();
            } catch (Exception e) {
                // Fall back to memory buffer
            }
        }
        return memoryBuffer.toString();
    }

    /**
     * Get recent lines from circular buffer.
     */
    public synchronized List<String> getRecentLines(int count) {
        return recentLines.getLast(count);
    }

    /**
     * Get all recent lines.
     */
    public synchronized List<String> getRecentLines() {
        return recentLines.getAll();
    }

    /**
     * Get output starting from offset.
     */
    public synchronized String getOutputFromOffset(long offset) {
        if (diskFile != null) {
            try {
                long fileSize = DiskTaskOutput.getFileSize(diskFile);
                if (offset < fileSize) {
                    // Read from disk from offset
                    String diskContent = DiskTaskOutput.readOutput(diskFile, offset, Integer.MAX_VALUE);
                    return diskContent + memoryBuffer.toString();
                }
            } catch (IOException e) {
                // Fall back to memory
            }
        }
        return memoryBuffer.toString();
    }

    /**
     * Get current output offset.
     */
    public synchronized long getOutputOffset() {
        if (diskFile != null) {
            try {
                return DiskTaskOutput.getFileSize(diskFile);
            } catch (Exception e) {
                return memoryBuffer.length();
            }
        }
        return memoryBuffer.length();
    }

    /**
     * Start polling for output changes (for file mode).
     */
    public void startPolling(Consumer<String> onUpdate, long intervalMs) {
        if (pollExecutor != null) {
            return;
        }

        pollExecutor = Executors.newSingleThreadScheduledExecutor();
        pollExecutor.scheduleAtFixedRate(() -> {
            synchronized (this) {
                long currentOffset = getOutputOffset();
                if (currentOffset > outputOffset) {
                    String newOutput = getOutputFromOffset(outputOffset);
                    outputOffset = currentOffset;
                    if (onUpdate != null) {
                        onUpdate.accept(newOutput);
                    }
                }
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop polling.
     */
    public void stopPolling() {
        if (pollExecutor != null) {
            pollExecutor.shutdown();
            pollExecutor = null;
        }
    }

    /**
     * Clear task output.
     */
    public synchronized void clear() {
        memoryBuffer.setLength(0);
        recentLines.clear();

        if (diskFile != null) {
            DiskTaskOutput.deleteOutputFile(diskFile);
            diskFile = null;
        }

        outputOffset = 0;
    }

    /**
     * Get output size.
     */
    public synchronized long getSize() {
        long memorySize = memoryBuffer.length();
        if (diskFile != null) {
            try {
                return DiskTaskOutput.getFileSize(diskFile) + memorySize;
            } catch (Exception e) {
                return memorySize;
            }
        }
        return memorySize;
    }

    /**
     * Check if output is empty.
     */
    public synchronized boolean isEmpty() {
        return memoryBuffer.length() == 0 && recentLines.isEmpty();
    }

    /**
     * Get task ID.
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Get output type.
     */
    public OutputType getOutputType() {
        return outputType;
    }

    /**
     * Get disk file path (if spilled to disk).
     */
    public Path getDiskFile() {
        return diskFile;
    }

    /**
     * Cleanup resources.
     */
    public void cleanup() {
        stopPolling();
        clear();
    }

    /**
     * Circular buffer implementation for recent lines.
     */
    public static final class CircularBuffer<T> {
        private final Object[] buffer;
        private final int capacity;
        private volatile int head;
        private volatile int tail;
        private volatile int size;

        public CircularBuffer(int capacity) {
            this.capacity = capacity;
            this.buffer = new Object[capacity];
            this.head = 0;
            this.tail = 0;
            this.size = 0;
        }

        public synchronized void add(T item) {
            buffer[tail] = item;
            tail = (tail + 1) % capacity;

            if (size < capacity) {
                size++;
            } else {
                // Overwrite oldest
                head = (head + 1) % capacity;
            }
        }

        public synchronized List<T> getAll() {
            List<T> result = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int idx = (head + i) % capacity;
                result.add((T) buffer[idx]);
            }
            return result;
        }

        public synchronized List<T> getLast(int count) {
            int actualCount = Math.min(count, size);
            List<T> result = new ArrayList<>(actualCount);

            int startIdx = size - actualCount;
            for (int i = startIdx; i < size; i++) {
                int idx = (head + i) % capacity;
                result.add((T) buffer[idx]);
            }

            return result;
        }

        public synchronized void clear() {
            head = 0;
            tail = 0;
            size = 0;
            Arrays.fill(buffer, null);
        }

        public synchronized int size() {
            return size;
        }

        public synchronized boolean isEmpty() {
            return size == 0;
        }
    }
}