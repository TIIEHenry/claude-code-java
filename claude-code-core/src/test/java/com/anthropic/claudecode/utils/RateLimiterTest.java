/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RateLimiter.
 */
class RateLimiterTest {

    @Test
    @DisplayName("RateLimiter creates with max requests and window")
    void createsWithMaxRequests() {
        RateLimiter limiter = new RateLimiter(10, Duration.ofMinutes(1));

        assertEquals(10, limiter.getRemainingPermits());
    }

    @Test
    @DisplayName("RateLimiter tryAcquire succeeds when under limit")
    void tryAcquireSucceeds() {
        RateLimiter limiter = new RateLimiter(10, Duration.ofMinutes(1));

        assertTrue(limiter.tryAcquire());
        assertEquals(9, limiter.getRemainingPermits());
    }

    @Test
    @DisplayName("RateLimiter tryAcquire fails when at limit")
    void tryAcquireFailsAtLimit() {
        RateLimiter limiter = new RateLimiter(2, Duration.ofSeconds(10));

        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire()); // At limit
    }

    @Test
    @DisplayName("RateLimiter reset clears permits")
    void resetClears() {
        RateLimiter limiter = new RateLimiter(10, Duration.ofMinutes(1));

        limiter.tryAcquire();
        limiter.tryAcquire();
        limiter.reset();

        assertEquals(10, limiter.getRemainingPermits());
    }

    @Test
    @DisplayName("RateLimiter getTimeUntilAvailable returns zero when available")
    void getTimeUntilAvailableZero() {
        RateLimiter limiter = new RateLimiter(10, Duration.ofMinutes(1));

        assertEquals(Duration.ZERO, limiter.getTimeUntilAvailable());
    }

    @Test
    @DisplayName("RateLimiter forApiCalls creates per-minute limiter")
    void forApiCallsCreates() {
        RateLimiter limiter = RateLimiter.forApiCalls(60);

        assertEquals(60, limiter.getRemainingPermits());
    }

    @Test
    @DisplayName("RateLimiter TokenBucket creates with capacity")
    void tokenBucketCreates() {
        RateLimiter.TokenBucket bucket = RateLimiter.tokenBucket(10, 1, Duration.ofSeconds(1));

        assertEquals(10, bucket.getAvailableTokens());
    }

    @Test
    @DisplayName("RateLimiter TokenBucket tryConsume succeeds")
    void tokenBucketConsumeSucceeds() {
        RateLimiter.TokenBucket bucket = RateLimiter.tokenBucket(10, 1, Duration.ofSeconds(1));

        assertTrue(bucket.tryConsume(5));
        assertEquals(5, bucket.getAvailableTokens());
    }

    @Test
    @DisplayName("RateLimiter TokenBucket tryConsume fails when empty")
    void tokenBucketConsumeFails() {
        RateLimiter.TokenBucket bucket = RateLimiter.tokenBucket(2, 1, Duration.ofSeconds(1));

        assertTrue(bucket.tryConsume(2));
        assertFalse(bucket.tryConsume(1));
    }

    @Test
    @DisplayName("RateLimiter TokenBucket tryConsume single token")
    void tokenBucketConsumeSingle() {
        RateLimiter.TokenBucket bucket = RateLimiter.tokenBucket(5, 1, Duration.ofSeconds(1));

        assertTrue(bucket.tryConsume());
        assertEquals(4, bucket.getAvailableTokens());
    }

    @Test
    @DisplayName("RateLimiter SlidingWindowRateLimiter creates")
    void slidingWindowCreates() {
        RateLimiter.SlidingWindowRateLimiter limiter =
            new RateLimiter.SlidingWindowRateLimiter(10, Duration.ofMinutes(1));

        assertEquals(10, limiter.getRemainingPermits());
    }

    @Test
    @DisplayName("RateLimiter SlidingWindowRateLimiter tryAcquire")
    void slidingWindowTryAcquire() {
        RateLimiter.SlidingWindowRateLimiter limiter =
            new RateLimiter.SlidingWindowRateLimiter(5, Duration.ofSeconds(10));

        assertTrue(limiter.tryAcquire());
        assertEquals(1, limiter.getCurrentCount());
    }

    @Test
    @DisplayName("RateLimiter SlidingWindowRateLimiter blocks at limit")
    void slidingWindowBlocksAtLimit() {
        RateLimiter.SlidingWindowRateLimiter limiter =
            new RateLimiter.SlidingWindowRateLimiter(2, Duration.ofSeconds(10));

        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());
    }
}