/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StatsContext.
 */
class StatsContextTest {

    private StatsContext stats;

    @BeforeEach
    void setUp() {
        stats = new StatsContext();
    }

    @Test
    @DisplayName("StatsContext initial values are zero")
    void initialValuesAreZero() {
        assertEquals(0, stats.getQueryCount());
        assertEquals(0, stats.getTokenCount());
        assertEquals(0, stats.getToolCallCount());
        assertEquals(0, stats.getErrorCount());
    }

    @Test
    @DisplayName("StatsContext incrementQueryCount")
    void incrementQueryCount() {
        stats.incrementQueryCount();
        assertEquals(1, stats.getQueryCount());

        stats.incrementQueryCount();
        stats.incrementQueryCount();
        assertEquals(3, stats.getQueryCount());
    }

    @Test
    @DisplayName("StatsContext addTokens")
    void addTokens() {
        stats.addTokens(100);
        assertEquals(100, stats.getTokenCount());

        stats.addTokens(50);
        assertEquals(150, stats.getTokenCount());
    }

    @Test
    @DisplayName("StatsContext incrementToolCallCount")
    void incrementToolCallCount() {
        stats.incrementToolCallCount();
        stats.incrementToolCallCount();
        assertEquals(2, stats.getToolCallCount());
    }

    @Test
    @DisplayName("StatsContext incrementErrorCount")
    void incrementErrorCount() {
        stats.incrementErrorCount();
        assertEquals(1, stats.getErrorCount());
    }

    @Test
    @DisplayName("StatsContext incrementCustomStat")
    void incrementCustomStat() {
        stats.incrementCustomStat("custom1");
        stats.incrementCustomStat("custom1");
        stats.incrementCustomStat("custom2");

        assertEquals(2, stats.getCustomStat("custom1"));
        assertEquals(1, stats.getCustomStat("custom2"));
        assertEquals(0, stats.getCustomStat("nonexistent"));
    }

    @Test
    @DisplayName("StatsContext toMap returns all stats")
    void toMap() {
        stats.incrementQueryCount();
        stats.addTokens(500);
        stats.incrementToolCallCount();
        stats.incrementErrorCount();
        stats.incrementCustomStat("custom");

        Map<String, Long> map = stats.toMap();

        assertEquals(1, map.get("queryCount"));
        assertEquals(500, map.get("tokenCount"));
        assertEquals(1, map.get("toolCallCount"));
        assertEquals(1, map.get("errorCount"));
        assertEquals(1, map.get("custom"));
    }

    @Test
    @DisplayName("StatsContext reset clears all values")
    void reset() {
        stats.incrementQueryCount();
        stats.addTokens(100);
        stats.incrementToolCallCount();
        stats.incrementErrorCount();
        stats.incrementCustomStat("custom");

        stats.reset();

        assertEquals(0, stats.getQueryCount());
        assertEquals(0, stats.getTokenCount());
        assertEquals(0, stats.getToolCallCount());
        assertEquals(0, stats.getErrorCount());
        assertEquals(0, stats.getCustomStat("custom"));
    }

    @Test
    @DisplayName("StatsContext thread safety")
    void threadSafety() throws Exception {
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                stats.incrementQueryCount();
                stats.addTokens(10);
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(200, stats.getQueryCount());
        assertEquals(2000, stats.getTokenCount());
    }
}