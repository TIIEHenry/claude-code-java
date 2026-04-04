/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code buffered writer
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Buffered writer that batches writes and flushes periodically or on overflow.
 */
public final class BufferedWriterUtil {
    private BufferedWriterUtil() {}

    /**
     * Create a buffered writer.
     */
    public static BufferedWriter createBufferedWriter(Consumer<String> writeFn,
                                                       long flushIntervalMs,
                                                       int maxBufferSize,
                                                       long maxBufferBytes,
                                                       boolean immediateMode) {
        return new BufferedWriter(writeFn, flushIntervalMs, maxBufferSize, maxBufferBytes, immediateMode);
    }

    /**
     * Create a buffered writer with defaults.
     */
    public static BufferedWriter createBufferedWriter(Consumer<String> writeFn) {
        return new BufferedWriter(writeFn, 1000, 100, Long.MAX_VALUE, false);
    }

    /**
     * Buffered writer interface.
     */
    public interface BufferedWriterI {
        void write(String content);
        void flush();
        void dispose();
    }

    /**
     * Buffered writer implementation.
     */
    public static final class BufferedWriter implements BufferedWriterI {
        private final List<String> buffer = new ArrayList<>();
        private long bufferBytes = 0;
        private final Consumer<String> writeFn;
        private final long flushIntervalMs;
        private final int maxBufferSize;
        private final long maxBufferBytes;
        private final boolean immediateMode;
        private ScheduledFuture<?> flushTask;
        private final ScheduledExecutorService scheduler;
        private List<String> pendingOverflow = null;
        private volatile boolean disposed = false;

        private BufferedWriter(Consumer<String> writeFn, long flushIntervalMs,
                               int maxBufferSize, long maxBufferBytes, boolean immediateMode) {
            this.writeFn = writeFn;
            this.flushIntervalMs = flushIntervalMs;
            this.maxBufferSize = maxBufferSize;
            this.maxBufferBytes = maxBufferBytes;
            this.immediateMode = immediateMode;
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
        }

        @Override
        public synchronized void write(String content) {
            if (disposed) return;

            if (immediateMode) {
                writeFn.accept(content);
                return;
            }

            buffer.add(content);
            bufferBytes += content.length();
            scheduleFlush();

            if (buffer.size() >= maxBufferSize || bufferBytes >= maxBufferBytes) {
                flushDeferred();
            }
        }

        private void scheduleFlush() {
            if (flushTask == null && !disposed) {
                flushTask = scheduler.schedule(this::doFlush, flushIntervalMs, TimeUnit.MILLISECONDS);
            }
        }

        private synchronized void doFlush() {
            flushTask = null;
            flushInternal();
        }

        @Override
        public synchronized void flush() {
            flushInternal();
        }

        private void flushInternal() {
            if (pendingOverflow != null) {
                StringBuilder sb = new StringBuilder();
                for (String s : pendingOverflow) {
                    sb.append(s);
                }
                writeFn.accept(sb.toString());
                pendingOverflow = null;
            }

            if (buffer.isEmpty()) return;

            StringBuilder sb = new StringBuilder();
            for (String s : buffer) {
                sb.append(s);
            }
            writeFn.accept(sb.toString());

            buffer.clear();
            bufferBytes = 0;

            if (flushTask != null) {
                flushTask.cancel(false);
                flushTask = null;
            }
        }

        private void flushDeferred() {
            if (pendingOverflow != null) {
                // Coalesce into existing pending overflow
                pendingOverflow.addAll(buffer);
                buffer.clear();
                bufferBytes = 0;
                if (flushTask != null) {
                    flushTask.cancel(false);
                    flushTask = null;
                }
                return;
            }

            List<String> detached = new ArrayList<>(buffer);
            buffer.clear();
            bufferBytes = 0;

            if (flushTask != null) {
                flushTask.cancel(false);
                flushTask = null;
            }

            pendingOverflow = detached;
            scheduler.submit(() -> {
                synchronized (BufferedWriter.this) {
                    if (pendingOverflow != null) {
                        StringBuilder sb = new StringBuilder();
                        for (String s : pendingOverflow) {
                            sb.append(s);
                        }
                        writeFn.accept(sb.toString());
                        pendingOverflow = null;
                    }
                }
            });
        }

        @Override
        public synchronized void dispose() {
            if (disposed) return;
            disposed = true;
            flushInternal();
            scheduler.shutdown();
        }
    }
}