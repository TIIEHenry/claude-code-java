/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code context
 */
package com.anthropic.claudecode.context;

import java.util.*;
import java.util.concurrent.*;

/**
 * Statistics context for tracking metrics.
 */
public class StatsContext {
    private final LongAdder queryCount = new LongAdder();
    private final LongAdder tokenCount = new LongAdder();
    private final LongAdder toolCallCount = new LongAdder();
    private final LongAdder errorCount = new LongAdder();
    private final Map<String, LongAdder> customStats = new ConcurrentHashMap<>();

    /**
     * Increment query count.
     */
    public void incrementQueryCount() {
        queryCount.increment();
    }

    /**
     * Add tokens.
     */
    public void addTokens(long tokens) {
        tokenCount.add(tokens);
    }

    /**
     * Increment tool call count.
     */
    public void incrementToolCallCount() {
        toolCallCount.increment();
    }

    /**
     * Increment error count.
     */
    public void incrementErrorCount() {
        errorCount.increment();
    }

    /**
     * Increment custom stat.
     */
    public void incrementCustomStat(String name) {
        customStats.computeIfAbsent(name, k -> new LongAdder()).increment();
    }

    /**
     * Get query count.
     */
    public long getQueryCount() {
        return queryCount.sum();
    }

    /**
     * Get token count.
     */
    public long getTokenCount() {
        return tokenCount.sum();
    }

    /**
     * Get tool call count.
     */
    public long getToolCallCount() {
        return toolCallCount.sum();
    }

    /**
     * Get error count.
     */
    public long getErrorCount() {
        return errorCount.sum();
    }

    /**
     * Get custom stat.
     */
    public long getCustomStat(String name) {
        LongAdder adder = customStats.get(name);
        return adder != null ? adder.sum() : 0;
    }

    /**
     * Get all stats as map.
     */
    public Map<String, Long> toMap() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("queryCount", getQueryCount());
        stats.put("tokenCount", getTokenCount());
        stats.put("toolCallCount", getToolCallCount());
        stats.put("errorCount", getErrorCount());
        for (Map.Entry<String, LongAdder> entry : customStats.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().sum());
        }
        return stats;
    }

    /**
     * Reset all stats.
     */
    public void reset() {
        queryCount.reset();
        tokenCount.reset();
        toolCallCount.reset();
        errorCount.reset();
        customStats.values().forEach(LongAdder::reset);
    }

    /**
     * Java 8 compatible LongAdder.
     */
    private static class LongAdder {
        private volatile long value = 0;

        void increment() {
            value++;
        }

        void add(long delta) {
            value += delta;
        }

        void reset() {
            value = 0;
        }

        long sum() {
            return value;
        }
    }
}