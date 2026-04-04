/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code rate limiting utilities
 */
package com.anthropic.claudecode.utils;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Rate limiting utilities.
 */
public final class RateLimiter {
    private final int maxRequests;
    private final Duration window;
    private final ConcurrentLinkedDeque<Instant> timestamps = new ConcurrentLinkedDeque<>();
    private final AtomicInteger currentRequests = new AtomicInteger(0);

    public RateLimiter(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.window = window;
    }

    /**
     * Try to acquire a permit.
     */
    public boolean tryAcquire() {
        cleanupOldTimestamps();

        if (currentRequests.get() >= maxRequests) {
            return false;
        }

        timestamps.addLast(Instant.now());
        currentRequests.incrementAndGet();
        return true;
    }

    /**
     * Acquire a permit, blocking if necessary.
     */
    public void acquire() throws InterruptedException {
        while (!tryAcquire()) {
            Thread.sleep(100);
        }
    }

    /**
     * Acquire a permit with timeout.
     */
    public boolean tryAcquire(Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);

        while (Instant.now().isBefore(deadline)) {
            if (tryAcquire()) {
                return true;
            }
            Thread.sleep(100);
        }

        return false;
    }

    /**
     * Get the number of remaining permits.
     */
    public int getRemainingPermits() {
        cleanupOldTimestamps();
        return Math.max(0, maxRequests - currentRequests.get());
    }

    /**
     * Get the time until a permit becomes available.
     */
    public Duration getTimeUntilAvailable() {
        cleanupOldTimestamps();

        if (currentRequests.get() < maxRequests) {
            return Duration.ZERO;
        }

        Instant oldest = timestamps.peekFirst();
        if (oldest == null) {
            return Duration.ZERO;
        }

        Instant availableAt = oldest.plus(window);
        Instant now = Instant.now();

        if (now.isAfter(availableAt)) {
            return Duration.ZERO;
        }

        return Duration.between(now, availableAt);
    }

    /**
     * Clear all timestamps.
     */
    public void reset() {
        timestamps.clear();
        currentRequests.set(0);
    }

    private void cleanupOldTimestamps() {
        Instant cutoff = Instant.now().minus(window);

        while (!timestamps.isEmpty()) {
            Instant oldest = timestamps.peekFirst();
            if (oldest != null && oldest.isBefore(cutoff)) {
                timestamps.pollFirst();
                currentRequests.decrementAndGet();
            } else {
                break;
            }
        }
    }

    /**
     * Create a rate limiter for API calls.
     */
    public static RateLimiter forApiCalls(int requestsPerMinute) {
        return new RateLimiter(requestsPerMinute, Duration.ofMinutes(1));
    }

    /**
     * Create a rate limiter for token bucket algorithm.
     */
    public static TokenBucket tokenBucket(int capacity, int refillRate, Duration refillPeriod) {
        return new TokenBucket(capacity, refillRate, refillPeriod);
    }

    /**
     * Token bucket rate limiter.
     */
    public static class TokenBucket {
        private final int capacity;
        private final int refillRate;
        private final Duration refillPeriod;
        private final AtomicInteger tokens;
        private final AtomicLong lastRefillTime;

        public TokenBucket(int capacity, int refillRate, Duration refillPeriod) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.refillPeriod = refillPeriod;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = new AtomicLong(System.nanoTime());
        }

        public boolean tryConsume(int tokensRequested) {
            refill();

            while (true) {
                int current = tokens.get();
                if (current < tokensRequested) {
                    return false;
                }

                if (tokens.compareAndSet(current, current - tokensRequested)) {
                    return true;
                }
            }
        }

        public boolean tryConsume() {
            return tryConsume(1);
        }

        public int getAvailableTokens() {
            refill();
            return tokens.get();
        }

        private void refill() {
            long now = System.nanoTime();
            long lastRefill = lastRefillTime.get();

            long elapsedNanos = now - lastRefill;
            long refillPeriodNanos = refillPeriod.toNanos();

            if (elapsedNanos >= refillPeriodNanos) {
                long periods = elapsedNanos / refillPeriodNanos;
                int tokensToAdd = (int) (periods * refillRate);

                while (true) {
                    int current = tokens.get();
                    int newTokens = Math.min(capacity, current + tokensToAdd);

                    if (tokens.compareAndSet(current, newTokens)) {
                        lastRefillTime.compareAndSet(lastRefill, lastRefill + periods * refillPeriodNanos);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Sliding window rate limiter with better accuracy.
     */
    public static class SlidingWindowRateLimiter {
        private final int maxRequests;
        private final Duration window;
        private final ConcurrentSkipListMap<Instant, AtomicInteger> requests = new ConcurrentSkipListMap<>();
        private final AtomicInteger totalCount = new AtomicInteger(0);

        public SlidingWindowRateLimiter(int maxRequests, Duration window) {
            this.maxRequests = maxRequests;
            this.window = window;
        }

        public synchronized boolean tryAcquire() {
            cleanup();

            if (totalCount.get() >= maxRequests) {
                return false;
            }

            Instant now = Instant.now();
            requests.computeIfAbsent(now, k -> new AtomicInteger(0)).incrementAndGet();
            totalCount.incrementAndGet();
            return true;
        }

        public int getCurrentCount() {
            cleanup();
            return totalCount.get();
        }

        public int getRemainingPermits() {
            cleanup();
            return Math.max(0, maxRequests - totalCount.get());
        }

        private synchronized void cleanup() {
            Instant cutoff = Instant.now().minus(window);

            Iterator<Map.Entry<Instant, AtomicInteger>> it = requests.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Instant, AtomicInteger> entry = it.next();
                if (entry.getKey().isBefore(cutoff)) {
                    totalCount.addAndGet(-entry.getValue().get());
                    it.remove();
                } else {
                    break;
                }
            }
        }
    }
}