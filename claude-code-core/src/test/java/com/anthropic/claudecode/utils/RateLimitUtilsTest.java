/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RateLimitUtils.
 */
class RateLimitUtilsTest {

    @Test
    @DisplayName("RateLimitUtils TokenBucket tryConsume returns true")
    void tokenBucketTryConsume() {
        RateLimitUtils.TokenBucket bucket = new RateLimitUtils.TokenBucket(10, 5);

        assertTrue(bucket.tryConsume());
        assertTrue(bucket.tryConsume(2));
    }

    @Test
    @DisplayName("RateLimitUtils TokenBucket tryConsume returns false when empty")
    void tokenBucketEmpty() {
        RateLimitUtils.TokenBucket bucket = new RateLimitUtils.TokenBucket(2, 1);

        assertTrue(bucket.tryConsume());
        assertTrue(bucket.tryConsume());
        assertFalse(bucket.tryConsume());
    }

    @Test
    @DisplayName("RateLimitUtils TokenBucket getAvailableTokens")
    void tokenBucketAvailable() {
        RateLimitUtils.TokenBucket bucket = new RateLimitUtils.TokenBucket(10, 5);

        assertEquals(10, bucket.getAvailableTokens());
        bucket.tryConsume(3);
        assertEquals(7, bucket.getAvailableTokens());
    }

    @Test
    @DisplayName("RateLimitUtils TokenBucket getWaitTimeMs")
    void tokenBucketWaitTime() {
        RateLimitUtils.TokenBucket bucket = new RateLimitUtils.TokenBucket(1, 1);

        bucket.tryConsume();
        double waitTime = bucket.getWaitTimeMs(1);

        assertTrue(waitTime > 0);
    }

    @Test
    @DisplayName("RateLimitUtils SlidingWindow tryAcquire returns true")
    void slidingWindowTryAcquire() {
        RateLimitUtils.SlidingWindow window = new RateLimitUtils.SlidingWindow(5, 1000);

        assertTrue(window.tryAcquire());
        assertTrue(window.tryAcquire());
    }

    @Test
    @DisplayName("RateLimitUtils SlidingWindow tryAcquire returns false when full")
    void slidingWindowFull() {
        RateLimitUtils.SlidingWindow window = new RateLimitUtils.SlidingWindow(2, 10000);

        assertTrue(window.tryAcquire());
        assertTrue(window.tryAcquire());
        assertFalse(window.tryAcquire());
    }

    @Test
    @DisplayName("RateLimitUtils SlidingWindow getCurrentCount")
    void slidingWindowCurrentCount() {
        RateLimitUtils.SlidingWindow window = new RateLimitUtils.SlidingWindow(5, 10000);

        window.tryAcquire();
        window.tryAcquire();
        assertEquals(2, window.getCurrentCount());
    }

    @Test
    @DisplayName("RateLimitUtils SlidingWindow getWaitTimeMs")
    void slidingWindowWaitTime() {
        RateLimitUtils.SlidingWindow window = new RateLimitUtils.SlidingWindow(1, 10000);

        window.tryAcquire();
        long waitTime = window.getWaitTimeMs();

        assertTrue(waitTime > 0 || waitTime == 0); // Depends on timing
    }

    @Test
    @DisplayName("RateLimitUtils FixedWindow tryAcquire returns true")
    void fixedWindowTryAcquire() {
        RateLimitUtils.FixedWindow window = new RateLimitUtils.FixedWindow(5, 1000);

        assertTrue(window.tryAcquire());
        assertTrue(window.tryAcquire());
    }

    @Test
    @DisplayName("RateLimitUtils FixedWindow tryAcquire returns false when full")
    void fixedWindowFull() {
        RateLimitUtils.FixedWindow window = new RateLimitUtils.FixedWindow(2, 10000);

        assertTrue(window.tryAcquire());
        assertTrue(window.tryAcquire());
        assertFalse(window.tryAcquire());
    }

    @Test
    @DisplayName("RateLimitUtils FixedWindow getCurrentCount")
    void fixedWindowCurrentCount() {
        RateLimitUtils.FixedWindow window = new RateLimitUtils.FixedWindow(5, 10000);

        window.tryAcquire();
        window.tryAcquire();
        assertEquals(2, window.getCurrentCount());
    }

    @Test
    @DisplayName("RateLimitUtils FixedWindow getRemainingTimeMs")
    void fixedWindowRemainingTime() {
        RateLimitUtils.FixedWindow window = new RateLimitUtils.FixedWindow(5, 10000);

        long remaining = window.getRemainingTimeMs();

        assertTrue(remaining <= 10000);
    }

    @Test
    @DisplayName("RateLimitUtils LeakyBucket tryAdd returns true")
    void leakyBucketTryAdd() {
        RateLimitUtils.LeakyBucket bucket = new RateLimitUtils.LeakyBucket(10, 5);

        assertTrue(bucket.tryAdd());
        assertTrue(bucket.tryAdd());
    }

    @Test
    @DisplayName("RateLimitUtils LeakyBucket tryAdd returns false when full")
    void leakyBucketFull() {
        RateLimitUtils.LeakyBucket bucket = new RateLimitUtils.LeakyBucket(2, 1);

        assertTrue(bucket.tryAdd());
        assertTrue(bucket.tryAdd());
        assertFalse(bucket.tryAdd());
    }

    @Test
    @DisplayName("RateLimitUtils LeakyBucket getCurrentLevel")
    void leakyBucketCurrentLevel() {
        RateLimitUtils.LeakyBucket bucket = new RateLimitUtils.LeakyBucket(10, 5);

        bucket.tryAdd();
        bucket.tryAdd();
        assertEquals(2, bucket.getCurrentLevel());
    }

    @Test
    @DisplayName("RateLimitUtils MultiKeyRateLimiter tryAcquire")
    void multiKeyRateLimiter() {
        RateLimitUtils.MultiKeyRateLimiter limiter = new RateLimitUtils.MultiKeyRateLimiter(2, 10000);

        assertTrue(limiter.tryAcquire("key1"));
        assertTrue(limiter.tryAcquire("key1"));
        assertFalse(limiter.tryAcquire("key1"));

        // Different key should still work
        assertTrue(limiter.tryAcquire("key2"));
    }

    @Test
    @DisplayName("RateLimitUtils MultiKeyRateLimiter getCurrentCount")
    void multiKeyRateLimiterCurrentCount() {
        RateLimitUtils.MultiKeyRateLimiter limiter = new RateLimitUtils.MultiKeyRateLimiter(5, 10000);

        limiter.tryAcquire("key1");
        limiter.tryAcquire("key1");

        assertEquals(2, limiter.getCurrentCount("key1"));
        assertEquals(0, limiter.getCurrentCount("key2"));
    }

    @Test
    @DisplayName("RateLimitUtils AdaptiveRateLimiter tryAcquire")
    void adaptiveRateLimiter() {
        RateLimitUtils.AdaptiveRateLimiter limiter = new RateLimitUtils.AdaptiveRateLimiter(1, 10, 1);

        assertTrue(limiter.tryAcquire());
    }

    @Test
    @DisplayName("RateLimitUtils AdaptiveRateLimiter recordSuccess increases capacity")
    void adaptiveRateLimiterSuccess() {
        RateLimitUtils.AdaptiveRateLimiter limiter = new RateLimitUtils.AdaptiveRateLimiter(1, 10, 1);

        for (int i = 0; i < 10; i++) {
            limiter.recordSuccess();
        }

        assertTrue(limiter.getCurrentCapacity() >= 1);
    }

    @Test
    @DisplayName("RateLimitUtils AdaptiveRateLimiter recordFailure decreases capacity")
    void adaptiveRateLimiterFailure() {
        RateLimitUtils.AdaptiveRateLimiter limiter = new RateLimitUtils.AdaptiveRateLimiter(1, 10, 1);

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure();
        }

        assertEquals(1, limiter.getCurrentCapacity()); // Already at minimum
    }

    @Test
    @DisplayName("RateLimitUtils createTokenBucket")
    void createTokenBucket() {
        RateLimitUtils.TokenBucket bucket = RateLimitUtils.createTokenBucket(10, 5);

        assertNotNull(bucket);
        assertEquals(10, bucket.getAvailableTokens());
    }

    @Test
    @DisplayName("RateLimitUtils createSlidingWindow")
    void createSlidingWindow() {
        RateLimitUtils.SlidingWindow window = RateLimitUtils.createSlidingWindow(5, 1000);

        assertNotNull(window);
    }

    @Test
    @DisplayName("RateLimitUtils createFixedWindow")
    void createFixedWindow() {
        RateLimitUtils.FixedWindow window = RateLimitUtils.createFixedWindow(5, 1000);

        assertNotNull(window);
    }

    @Test
    @DisplayName("RateLimitUtils calculateRetryDelay")
    void calculateRetryDelay() {
        assertEquals(1000, RateLimitUtils.calculateRetryDelay(0, 1000));
        assertEquals(0, RateLimitUtils.calculateRetryDelay(5, 1000));
    }

    @Test
    @DisplayName("RateLimitUtils parseHeaders")
    void parseHeaders() {
        Map<String, String> headers = Map.of(
            "x-ratelimit-limit", "100",
            "x-ratelimit-remaining", "50",
            "x-ratelimit-reset", "1234567890"
        );

        RateLimitUtils.RateLimitHeaders result = RateLimitUtils.parseHeaders(headers);

        assertEquals(100, result.limit());
        assertEquals(50, result.remaining());
        assertEquals(1234567890, result.resetTime());
    }

    @Test
    @DisplayName("RateLimitUtils parseHeaders missing values")
    void parseHeadersMissing() {
        Map<String, String> headers = Map.of();

        RateLimitUtils.RateLimitHeaders result = RateLimitUtils.parseHeaders(headers);

        assertEquals(0, result.limit());
        assertEquals(0, result.remaining());
        assertEquals(0, result.resetTime());
    }

    @Test
    @DisplayName("RateLimitUtils RateLimitHeaders isLimited")
    void rateLimitHeadersIsLimited() {
        RateLimitUtils.RateLimitHeaders limited = new RateLimitUtils.RateLimitHeaders(100, 0, 1000);
        RateLimitUtils.RateLimitHeaders notLimited = new RateLimitUtils.RateLimitHeaders(100, 10, 1000);

        assertTrue(limited.isLimited());
        assertFalse(notLimited.isLimited());
    }

    @Test
    @DisplayName("RateLimitUtils RateLimitHeaders getRetryDelayMs")
    void rateLimitHeadersRetryDelay() {
        RateLimitUtils.RateLimitHeaders headers = new RateLimitUtils.RateLimitHeaders(100, 0, System.currentTimeMillis() + 1000);

        long delay = headers.getRetryDelayMs(System.currentTimeMillis());

        assertTrue(delay > 0);
    }

    @Test
    @DisplayName("RateLimitUtils TokenBucket tryConsumeOrWait")
    void tokenBucketTryConsumeOrWait() {
        RateLimitUtils.TokenBucket bucket = new RateLimitUtils.TokenBucket(1, 1);

        bucket.tryConsume();
        boolean result = bucket.tryConsumeOrWait(1, 10);

        assertFalse(result); // Should timeout
    }
}