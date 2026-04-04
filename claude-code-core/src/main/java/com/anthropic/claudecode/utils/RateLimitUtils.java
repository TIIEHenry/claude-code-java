/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code rate limiting utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Rate limiting utilities.
 */
public final class RateLimitUtils {
    private RateLimitUtils() {}

    /**
     * Token bucket rate limiter.
     */
    public static class TokenBucket {
        private final int capacity;
        private final int refillRate; // tokens per second
        private final AtomicInteger tokens;
        private final AtomicLong lastRefillTime;

        public TokenBucket(int capacity, int refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
        }

        public boolean tryConsume(int tokensNeeded) {
            refill();

            int currentTokens = tokens.get();
            if (currentTokens < tokensNeeded) {
                return false;
            }

            return tokens.compareAndSet(currentTokens, currentTokens - tokensNeeded);
        }

        public boolean tryConsume() {
            return tryConsume(1);
        }

        public void consume(int tokensNeeded) throws InterruptedException {
            while (!tryConsume(tokensNeeded)) {
                Thread.sleep(100);
            }
        }

        public void consume() throws InterruptedException {
            consume(1);
        }

        public boolean tryConsumeOrWait(int tokensNeeded, long maxWaitMs) {
            long deadline = System.currentTimeMillis() + maxWaitMs;
            while (!tryConsume(tokensNeeded)) {
                if (System.currentTimeMillis() >= deadline) {
                    return false;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return true;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long lastRefill = lastRefillTime.get();

            long elapsed = now - lastRefill;
            if (elapsed > 1000) {
                int tokensToAdd = (int) (elapsed / 1000.0 * refillRate);
                int newTokens = Math.min(capacity, tokens.get() + tokensToAdd);

                if (tokens.compareAndSet(tokens.get(), newTokens)) {
                    lastRefillTime.compareAndSet(lastRefill, now);
                }
            }
        }

        public int getAvailableTokens() {
            refill();
            return tokens.get();
        }

        public double getWaitTimeMs(int tokensNeeded) {
            refill();
            int available = tokens.get();
            if (available >= tokensNeeded) {
                return 0;
            }
            int deficit = tokensNeeded - available;
            return deficit * 1000.0 / refillRate;
        }
    }

    /**
     * Sliding window rate limiter.
     */
    public static class SlidingWindow {
        private final int maxRequests;
        private final long windowSizeMs;
        private final ConcurrentLinkedDeque<Long> timestamps;

        public SlidingWindow(int maxRequests, long windowSizeMs) {
            this.maxRequests = maxRequests;
            this.windowSizeMs = windowSizeMs;
            this.timestamps = new ConcurrentLinkedDeque<>();
        }

        public boolean tryAcquire() {
            long now = System.currentTimeMillis();
            long windowStart = now - windowSizeMs;

            // Remove expired timestamps
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);
                return true;
            }

            return false;
        }

        public boolean tryAcquireOrWait(long maxWaitMs) {
            long deadline = System.currentTimeMillis() + maxWaitMs;
            while (!tryAcquire()) {
                if (System.currentTimeMillis() >= deadline) {
                    return false;
                }
                long oldest = timestamps.peekFirst();
                long waitTime = oldest + windowSizeMs - System.currentTimeMillis();
                if (waitTime > 0) {
                    try {
                        Thread.sleep(Math.min(waitTime, 100));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
            return true;
        }

        public void acquire() throws InterruptedException {
            while (!tryAcquire()) {
                Thread.sleep(100);
            }
        }

        public int getCurrentCount() {
            long now = System.currentTimeMillis();
            long windowStart = now - windowSizeMs;

            int count = 0;
            for (Long timestamp : timestamps) {
                if (timestamp >= windowStart) {
                    count++;
                }
            }
            return count;
        }

        public long getWaitTimeMs() {
            if (timestamps.isEmpty() || timestamps.size() < maxRequests) {
                return 0;
            }
            long oldest = timestamps.peekFirst();
            return oldest + windowSizeMs - System.currentTimeMillis();
        }
    }

    /**
     * Fixed window rate limiter.
     */
    public static class FixedWindow {
        private final int maxRequests;
        private final long windowSizeMs;
        private final AtomicInteger counter;
        private final AtomicLong windowStart;

        public FixedWindow(int maxRequests, long windowSizeMs) {
            this.maxRequests = maxRequests;
            this.windowSizeMs = windowSizeMs;
            this.counter = new AtomicInteger(0);
            this.windowStart = new AtomicLong(System.currentTimeMillis());
        }

        public boolean tryAcquire() {
            long now = System.currentTimeMillis();
            long currentWindowStart = windowStart.get();

            if (now - currentWindowStart >= windowSizeMs) {
                if (windowStart.compareAndSet(currentWindowStart, now)) {
                    counter.set(0);
                }
            }

            int currentCount = counter.get();
            if (currentCount < maxRequests) {
                return counter.compareAndSet(currentCount, currentCount + 1);
            }
            return false;
        }

        public void acquire() throws InterruptedException {
            while (!tryAcquire()) {
                Thread.sleep(100);
            }
        }

        public int getCurrentCount() {
            return counter.get();
        }

        public long getRemainingTimeMs() {
            long now = System.currentTimeMillis();
            long elapsed = now - windowStart.get();
            return Math.max(0, windowSizeMs - elapsed);
        }
    }

    /**
     * Leaky bucket rate limiter.
     */
    public static class LeakyBucket {
        private final int capacity;
        private final int leakRate; // requests per second
        private final AtomicInteger level;
        private final AtomicLong lastLeakTime;

        public LeakyBucket(int capacity, int leakRate) {
            this.capacity = capacity;
            this.leakRate = leakRate;
            this.level = new AtomicInteger(0);
            this.lastLeakTime = new AtomicLong(System.currentTimeMillis());
        }

        public boolean tryAdd() {
            leak();

            int currentLevel = level.get();
            if (currentLevel >= capacity) {
                return false;
            }

            return level.compareAndSet(currentLevel, currentLevel + 1);
        }

        private void leak() {
            long now = System.currentTimeMillis();
            long lastLeak = lastLeakTime.get();

            long elapsed = now - lastLeak;
            if (elapsed > 1000) {
                int leaked = (int) (elapsed / 1000.0 * leakRate);
                int newLevel = Math.max(0, level.get() - leaked);

                if (level.compareAndSet(level.get(), newLevel)) {
                    lastLeakTime.compareAndSet(lastLeak, now);
                }
            }
        }

        public int getCurrentLevel() {
            leak();
            return level.get();
        }
    }

    /**
     * Multi-key rate limiter.
     */
    public static class MultiKeyRateLimiter {
        private final int maxRequestsPerKey;
        private final long windowSizeMs;
        private final ConcurrentHashMap<String, SlidingWindow> limiters;

        public MultiKeyRateLimiter(int maxRequestsPerKey, long windowSizeMs) {
            this.maxRequestsPerKey = maxRequestsPerKey;
            this.windowSizeMs = windowSizeMs;
            this.limiters = new ConcurrentHashMap<>();
        }

        public boolean tryAcquire(String key) {
            SlidingWindow limiter = limiters.computeIfAbsent(key,
                k -> new SlidingWindow(maxRequestsPerKey, windowSizeMs));
            return limiter.tryAcquire();
        }

        public boolean tryAcquireOrWait(String key, long maxWaitMs) {
            SlidingWindow limiter = limiters.computeIfAbsent(key,
                k -> new SlidingWindow(maxRequestsPerKey, windowSizeMs));
            return limiter.tryAcquireOrWait(maxWaitMs);
        }

        public int getCurrentCount(String key) {
            SlidingWindow limiter = limiters.get(key);
            return limiter != null ? limiter.getCurrentCount() : 0;
        }

        public void cleanup(long maxIdleMs) {
            long now = System.currentTimeMillis();
            limiters.entrySet().removeIf(entry ->
                entry.getValue().timestamps.isEmpty() ||
                (entry.getValue().timestamps.peekLast() != null &&
                 now - entry.getValue().timestamps.peekLast() > maxIdleMs)
            );
        }
    }

    /**
     * Adaptive rate limiter that adjusts based on success/failure.
     */
    public static class AdaptiveRateLimiter {
        private final int minCapacity;
        private final int maxCapacity;
        private final AtomicInteger currentCapacity;
        private final AtomicInteger successCount;
        private final AtomicInteger failureCount;
        private final TokenBucket bucket;

        public AdaptiveRateLimiter(int minCapacity, int maxCapacity, int refillRate) {
            this.minCapacity = minCapacity;
            this.maxCapacity = maxCapacity;
            this.currentCapacity = new AtomicInteger(minCapacity);
            this.successCount = new AtomicInteger(0);
            this.failureCount = new AtomicInteger(0);
            this.bucket = new TokenBucket(minCapacity, refillRate);
        }

        public boolean tryAcquire() {
            return bucket.tryConsume();
        }

        public void recordSuccess() {
            int successes = successCount.incrementAndGet();
            if (successes % 10 == 0 && currentCapacity.get() < maxCapacity) {
                int newCapacity = Math.min(maxCapacity, currentCapacity.get() + 1);
                currentCapacity.set(newCapacity);
                // Update bucket capacity
                synchronized (bucket) {
                    bucket.tokens.set(newCapacity);
                }
            }
        }

        public void recordFailure() {
            int failures = failureCount.incrementAndGet();
            if (failures % 5 == 0 && currentCapacity.get() > minCapacity) {
                int newCapacity = Math.max(minCapacity, currentCapacity.get() - 1);
                currentCapacity.set(newCapacity);
                synchronized (bucket) {
                    bucket.tokens.set(newCapacity);
                }
            }
        }

        public int getCurrentCapacity() {
            return currentCapacity.get();
        }
    }

    /**
     * Create a simple token bucket rate limiter.
     */
    public static TokenBucket createTokenBucket(int capacity, int refillRate) {
        return new TokenBucket(capacity, refillRate);
    }

    /**
     * Create a sliding window rate limiter.
     */
    public static SlidingWindow createSlidingWindow(int maxRequests, long windowSizeMs) {
        return new SlidingWindow(maxRequests, windowSizeMs);
    }

    /**
     * Create a fixed window rate limiter.
     */
    public static FixedWindow createFixedWindow(int maxRequests, long windowSizeMs) {
        return new FixedWindow(maxRequests, windowSizeMs);
    }

    /**
     * Calculate retry delay based on rate limit headers.
     */
    public static long calculateRetryDelay(int remainingRequests, long resetTimeMs) {
        if (remainingRequests <= 0) {
            return resetTimeMs;
        }
        return 0;
    }

    /**
     * Parse rate limit headers from HTTP response.
     */
    public static RateLimitHeaders parseHeaders(Map<String, String> headers) {
        int limit = parseHeaderInt(headers.get("x-ratelimit-limit"));
        int remaining = parseHeaderInt(headers.get("x-ratelimit-remaining"));
        long resetTime = parseHeaderLong(headers.get("x-ratelimit-reset"));

        return new RateLimitHeaders(limit, remaining, resetTime);
    }

    private static int parseHeaderInt(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long parseHeaderLong(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Rate limit headers record.
     */
    public record RateLimitHeaders(int limit, int remaining, long resetTime) {
        public boolean isLimited() {
            return remaining <= 0;
        }

        public long getRetryDelayMs(long now) {
            if (!isLimited()) return 0;
            return Math.max(0, resetTime - now);
        }
    }
}