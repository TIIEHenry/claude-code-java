/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bufferedWriter
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Buffered writer - Efficient buffered file writing with periodic flush.
 */
public final class BufferedWriterUtils implements AutoCloseable {
    private final Path filePath;
    private final BufferedWriter writer;
    private final ScheduledExecutorService scheduler;
    private final int bufferSize;
    private final long flushIntervalMs;
    private volatile boolean closed = false;
    private int bufferCount = 0;

    /**
     * Create a buffered writer.
     */
    public BufferedWriterUtils(Path filePath, int bufferSize, long flushIntervalMs) throws IOException {
        this.filePath = filePath;
        this.bufferSize = bufferSize;
        this.flushIntervalMs = flushIntervalMs;

        Files.createDirectories(filePath.getParent());
        this.writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        // Schedule periodic flush
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(this::flushIfNeeded, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Write a line.
     */
    public void writeLine(String line) throws IOException {
        if (closed) {
            throw new IOException("Writer is closed");
        }

        writer.write(line);
        writer.newLine();
        bufferCount++;

        if (bufferCount >= bufferSize) {
            flush();
        }
    }

    /**
     * Write content without newline.
     */
    public void write(String content) throws IOException {
        if (closed) {
            throw new IOException("Writer is closed");
        }

        writer.write(content);
    }

    /**
     * Flush the buffer.
     */
    public synchronized void flush() throws IOException {
        if (!closed) {
            writer.flush();
            bufferCount = 0;
        }
    }

    private synchronized void flushIfNeeded() {
        try {
            if (bufferCount > 0 && !closed) {
                flush();
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Get the file path.
     */
    public Path getFilePath() {
        return filePath;
    }

    /**
     * Get the buffer count.
     */
    public int getBufferCount() {
        return bufferCount;
    }

    /**
     * Check if closed.
     */
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        scheduler.shutdown();

        try {
            flush();
        } catch (IOException e) {
            // Ignore
        }

        try {
            writer.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Create a buffered writer with default settings.
     */
    public static BufferedWriterUtils create(Path filePath) throws IOException {
        return new BufferedWriterUtils(filePath, 100, 5000);
    }

    /**
     * Create a buffered writer with custom settings.
     */
    public static BufferedWriterUtils create(Path filePath, int bufferSize, long flushIntervalMs) throws IOException {
        return new BufferedWriterUtils(filePath, bufferSize, flushIntervalMs);
    }
}