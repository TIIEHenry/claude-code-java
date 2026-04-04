/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SlidingWindowCounter.
 */
class SlidingWindowCounterTest {

    @Test
    @DisplayName("SlidingWindowCounter creates with window size")
    void createsWithWindowSize() {
        SlidingWindowCounter counter = new SlidingWindowCounter(Duration.ofMinutes(1));

        assertEquals(Duration.ofMinutes(1), counter.getWindowSize());
        assertEquals(0, counter.getCount());
    }

    @Test
    @DisplayName("SlidingWindowCounter increment increases count")
    void incrementWorks() {
        SlidingWindowCounter counter = new SlidingWindowCounter(Duration.ofMinutes(1));

        counter.increment();

        assertEquals(1, counter.getCount());
    }

    @Test
    @DisplayName("SlidingWindowCounter increment by value works")
    void incrementByValue() {
        SlidingWindowCounter counter = new SlidingWindowCounter(Duration.ofMinutes(1));

        counter.increment(5);

        assertEquals(5, counter.getCount());
    }

    @Test
    @DisplayName("SlidingWindowCounter multiple increments accumulate")
    void multipleIncrements() {
        SlidingWindowCounter counter = new SlidingWindowCounter(Duration.ofMinutes(1));

        counter.increment();
        counter.increment();
        counter.increment(3);

        assertEquals(5, counter.getCount());
    }

    @Test
    @DisplayName("SlidingWindowCounter getTotalCount returns all time count")
    void getTotalCount() {
        SlidingWindowCounter counter = new SlidingWindowCounter(Duration.ofSeconds(1));

        counter.increment();
        counter.increment();

        assertEquals(2, counter.getTotalCount());
    }

    @Test
    @DisplayName("SlidingWindowCounter getRatePerSecond returns rate")
    void getRatePerSecond() {
        SlidingWindowCounter counter = new SlidingWindowCounter(Duration.ofSeconds(10));

        counter.increment(10);

        double rate = counter.getRatePerSecond();

        // 10 events over 10 seconds = 1 per second
        assertTrue(rate > 0);
    }

    @Test
    @DisplayName("SlidingWindowCounter getRatePerMinute returns rate")
    void getRatePerMinute() {
        SlidingWindowCounter counter = new SlidingWindowCounter(Duration.ofMinutes(1));

        counter.increment(60);

        double rate = counter.getRatePerMinute();

        assertTrue(rate > 0);
    }

    @Test
    @DisplayName("SlidingWindowCounter reset clears counts")
    void resetWorks() {
        SlidingWindowCounter counter = new SlidingWindowCounter(Duration.ofMinutes(1));

        counter.increment(10);
        counter.reset();

        assertEquals(0, counter.getCount());
        assertEquals(0, counter.getTotalCount());
    }

    @Test
    @DisplayName("SlidingWindowCounter getBucketCount returns buckets")
    void getBucketCount() {
        SlidingWindowCounter counter = new SlidingWindowCounter(Duration.ofMinutes(1));

        counter.increment();

        assertTrue(counter.getBucketCount() > 0);
    }

    @Test
    @DisplayName("SlidingWindowCounter stats returns statistics")
    void statsWorks() {
        SlidingWindowCounter counter = new SlidingWindowCounter(Duration.ofMinutes(1));

        counter.increment(10);

        SlidingWindowCounter.WindowStats stats = counter.stats();

        assertEquals(10, stats.windowCount());
        assertEquals(10, stats.totalCount());
        assertTrue(stats.ratePerSecond() > 0);
        assertEquals(Duration.ofMinutes(1), stats.windowSize());
    }

    @Test
    @DisplayName("SlidingWindowCounter WindowStats format returns string")
    void windowStatsFormat() {
        SlidingWindowCounter.WindowStats stats = new SlidingWindowCounter.WindowStats(
            100, 500, 10.0, Duration.ofMinutes(1)
        );

        String formatted = stats.format();

        assertTrue(formatted.contains("count=100"));
        assertTrue(formatted.contains("total=500"));
    }

    @Test
    @DisplayName("SlidingWindowCounter SlidingWindowUtils perSecond creates counter")
    void slidingWindowUtilsPerSecond() {
        SlidingWindowCounter counter = SlidingWindowCounter.SlidingWindowUtils.perSecond();

        assertEquals(Duration.ofSeconds(1), counter.getWindowSize());
    }

    @Test
    @DisplayName("SlidingWindowCounter SlidingWindowUtils perMinute creates counter")
    void slidingWindowUtilsPerMinute() {
        SlidingWindowCounter counter = SlidingWindowCounter.SlidingWindowUtils.perMinute();

        assertEquals(Duration.ofMinutes(1), counter.getWindowSize());
    }

    @Test
    @DisplayName("SlidingWindowCounter SlidingWindowUtils perHour creates counter")
    void slidingWindowUtilsPerHour() {
        SlidingWindowCounter counter = SlidingWindowCounter.SlidingWindowUtils.perHour();

        assertEquals(Duration.ofHours(1), counter.getWindowSize());
    }

    @Test
    @DisplayName("SlidingWindowCounter SlidingWindowUtils of creates counter")
    void slidingWindowUtilsOf() {
        SlidingWindowCounter counter = SlidingWindowCounter.SlidingWindowUtils.of(Duration.ofSeconds(30));

        assertEquals(Duration.ofSeconds(30), counter.getWindowSize());
    }

    @Test
    @DisplayName("SlidingWindowCounter getCount with duration works")
    void getCountWithDuration() {
        SlidingWindowCounter counter = new SlidingWindowCounter(Duration.ofMinutes(5));

        counter.increment(10);

        long count = counter.getCount(Duration.ofMinutes(1));

        assertEquals(10, count); // All events are within last minute
    }
}