/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code sliding window counter
 */
package com.anthropic.claudecode.utils;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Sliding window counter for rate limiting and statistics.
 */
public final class SlidingWindowCounter {
    private final Duration windowSize;
    private final ConcurrentNavigableMap<Long, Long> buckets;
    private final AtomicLong totalCount = new AtomicLong(0);

    public SlidingWindowCounter(Duration windowSize) {
        this.windowSize = windowSize;
        this.buckets = new ConcurrentSkipListMap<>();
    }

    /**
     * Increment counter.
     */
    public void increment() {
        increment(1);
    }

    /**
     * Increment counter by value.
     */
    public void increment(long value) {
        long bucketKey = getBucketKey();
        buckets.merge(bucketKey, value, Long::sum);
        totalCount.addAndGet(value);
        cleanOldBuckets();
    }

    /**
     * Get current count in window.
     */
    public long getCount() {
        cleanOldBuckets();
        return buckets.values().stream().mapToLong(Long::longValue).sum();
    }

    /**
     * Get count in last N seconds.
     */
    public long getCount(Duration duration) {
        long cutoff = System.currentTimeMillis() - duration.toMillis();
        return buckets.tailMap(cutoff).values().stream()
            .mapToLong(Long::longValue)
            .sum();
    }

    /**
     * Get average rate (per second).
     */
    public double getRatePerSecond() {
        long count = getCount();
        return count / (windowSize.toMillis() / 1000.0);
    }

    /**
     * Get average rate (per minute).
     */
    public double getRatePerMinute() {
        long count = getCount();
        return count / (windowSize.toMillis() / 60000.0);
    }

    /**
     * Get bucket key (timestamp rounded to second).
     */
    private long getBucketKey() {
        return System.currentTimeMillis() / 1000 * 1000;
    }

    /**
     * Clean old buckets outside window.
     */
    private void cleanOldBuckets() {
        long cutoff = System.currentTimeMillis() - windowSize.toMillis();
        NavigableMap<Long, Long> oldBuckets = buckets.headMap(cutoff, false);
        oldBuckets.clear();
    }

    /**
     * Reset counter.
     */
    public void reset() {
        buckets.clear();
        totalCount.set(0);
    }

    /**
     * Get total count (all time).
     */
    public long getTotalCount() {
        return totalCount.get();
    }

    /**
     * Get window size.
     */
    public Duration getWindowSize() {
        return windowSize;
    }

    /**
     * Get bucket count.
     */
    public int getBucketCount() {
        return buckets.size();
    }

    /**
     * Get statistics.
     */
    public WindowStats stats() {
        return new WindowStats(getCount(), getTotalCount(), getRatePerSecond(), windowSize);
    }

    /**
     * Window statistics.
     */
    public record WindowStats(long windowCount, long totalCount, double ratePerSecond, Duration windowSize) {
        public String format() {
            return String.format("SlidingWindow[count=%d, total=%d, rate=%.2f/s, window=%s]",
                windowCount, totalCount, ratePerSecond, DurationUtils.formatCompact(windowSize));
        }
    }

    /**
     * Sliding window counter utilities.
     */
    public static final class SlidingWindowUtils {
        private SlidingWindowUtils() {}

        /**
         * Create per-second counter.
         */
        public static SlidingWindowCounter perSecond() {
            return new SlidingWindowCounter(Duration.ofSeconds(1));
        }

        /**
         * Create per-minute counter.
         */
        public static SlidingWindowCounter perMinute() {
            return new SlidingWindowCounter(Duration.ofMinutes(1));
        }

        /**
         * Create per-hour counter.
         */
        public static SlidingWindowCounter perHour() {
            return new SlidingWindowCounter(Duration.ofHours(1));
        }

        /**
         * Create with custom window.
         */
        public static SlidingWindowCounter of(Duration windowSize) {
            return new SlidingWindowCounter(windowSize);
        }
    }
}