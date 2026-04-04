/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/analytics/sink.ts
 */
package com.anthropic.claudecode.services.analytics;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Analytics event sink for batching and sending events.
 */
public final class AnalyticsSink {
    private static final int MAX_BATCH_SIZE = 100;
    private static final long FLUSH_INTERVAL_MS = 10000; // 10 seconds

    private final List<Map<String, Object>> eventQueue = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Consumer<List<Map<String, Object>>> sender;
    private volatile boolean isFlushing = false;

    public AnalyticsSink(Consumer<List<Map<String, Object>>> sender) {
        this.sender = sender;
        scheduler.scheduleAtFixedRate(this::flush, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Add an event to the sink.
     */
    public void addEvent(Map<String, Object> event) {
        eventQueue.add(event);
        if (eventQueue.size() >= MAX_BATCH_SIZE) {
            flush();
        }
    }

    /**
     * Flush all pending events.
     */
    public synchronized void flush() {
        if (isFlushing || eventQueue.isEmpty()) {
            return;
        }

        isFlushing = true;
        try {
            List<Map<String, Object>> batch = new ArrayList<>(eventQueue);
            eventQueue.clear();
            sender.accept(batch);
        } finally {
            isFlushing = false;
        }
    }

    /**
     * Shutdown the sink.
     */
    public void shutdown() {
        flush();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}