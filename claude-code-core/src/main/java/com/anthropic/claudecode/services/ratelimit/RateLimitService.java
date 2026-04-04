/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mockRateLimits
 */
package com.anthropic.claudecode.services.ratelimit;

import java.util.*;
import java.util.concurrent.*;

/**
 * Rate limit service - Rate limiting management.
 */
public final class RateLimitService {
    private final Map<String, RateLimit> limits = new ConcurrentHashMap<>();
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Rate limit record.
     */
    public record RateLimit(
        String name,
        int maxRequests,
        int maxTokens,
        long windowMs,
        String scope
    ) {
        public static RateLimit requests(String name, int max, long windowMs) {
            return new RateLimit(name, max, 0, windowMs, "requests");
        }

        public static RateLimit tokens(String name, int max, long windowMs) {
            return new RateLimit(name, 0, max, windowMs, "tokens");
        }
    }

    /**
     * Token bucket implementation.
     */
    private static final class TokenBucket {
        private double tokens;
        private final double maxTokens;
        private final double refillRate;
        private long lastRefill;

        TokenBucket(int maxTokens, long windowMs) {
            this.tokens = maxTokens;
            this.maxTokens = maxTokens;
            this.refillRate = (double) maxTokens / windowMs * 1000;
            this.lastRefill = System.currentTimeMillis();
        }

        synchronized boolean tryConsume(int amount) {
            refill();
            if (tokens >= amount) {
                tokens -= amount;
                return true;
            }
            return false;
        }

        synchronized void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefill;
            tokens = Math.min(maxTokens, tokens + elapsed * refillRate / 1000);
            lastRefill = now;
        }

        synchronized int getAvailable() {
            refill();
            return (int) tokens;
        }

        synchronized long getWaitTime(int amount) {
            refill();
            if (tokens >= amount) return 0;
            double needed = amount - tokens;
            return (long) (needed / refillRate * 1000);
        }
    }

    /**
     * Create rate limit service.
     */
    public RateLimitService() {
        // Default limits
        limits.put("api_requests", RateLimit.requests("api_requests", 1000, 60000));
        limits.put("api_tokens", RateLimit.tokens("api_tokens", 100000, 60000));
    }

    /**
     * Register limit.
     */
    public void registerLimit(RateLimit limit) {
        limits.put(limit.name(), limit);
        buckets.put(limit.name(), new TokenBucket(
            limit.maxRequests() > 0 ? limit.maxRequests() : limit.maxTokens(),
            limit.windowMs()
        ));
    }

    /**
     * Check if allowed.
     */
    public boolean isAllowed(String limitName, int amount) {
        TokenBucket bucket = buckets.get(limitName);
        if (bucket == null) return true;
        return bucket.tryConsume(amount);
    }

    /**
     * Get available capacity.
     */
    public int getAvailable(String limitName) {
        TokenBucket bucket = buckets.get(limitName);
        if (bucket == null) return Integer.MAX_VALUE;
        return bucket.getAvailable();
    }

    /**
     * Get wait time.
     */
    public long getWaitTime(String limitName, int amount) {
        TokenBucket bucket = buckets.get(limitName);
        if (bucket == null) return 0;
        return bucket.getWaitTime(amount);
    }

    /**
     * Wait until allowed.
     */
    public void waitForAllow(String limitName, int amount) throws InterruptedException {
        long waitTime = getWaitTime(limitName, amount);
        if (waitTime > 0) {
            Thread.sleep(waitTime);
        }
    }

    /**
     * Rate limit status record.
     */
    public record RateLimitStatus(
        String name,
        int limit,
        int remaining,
        long resetMs,
        boolean limited
    ) {
        public String format() {
            return String.format("%s: %d/%d (resets in %dms)",
                name, remaining, limit, resetMs);
        }
    }

    /**
     * Get status.
     */
    public RateLimitStatus getStatus(String limitName) {
        RateLimit limit = limits.get(limitName);
        TokenBucket bucket = buckets.get(limitName);

        if (limit == null || bucket == null) {
            return new RateLimitStatus(limitName, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, false);
        }

        int remaining = bucket.getAvailable();
        int max = limit.maxRequests() > 0 ? limit.maxRequests() : limit.maxTokens();

        return new RateLimitStatus(
            limitName,
            max,
            remaining,
            limit.windowMs(),
            remaining == 0
        );
    }

    /**
     * Get all statuses.
     */
    public Map<String, RateLimitStatus> getAllStatuses() {
        Map<String, RateLimitStatus> statuses = new HashMap<>();
        for (String name : limits.keySet()) {
            statuses.put(name, getStatus(name));
        }
        return statuses;
    }

    /**
     * Reset limit.
     */
    public void resetLimit(String limitName) {
        RateLimit limit = limits.get(limitName);
        if (limit != null) {
            int max = limit.maxRequests() > 0 ? limit.maxRequests() : limit.maxTokens();
            buckets.put(limitName, new TokenBucket(max, limit.windowMs()));
        }
    }

    /**
     * Reset all limits.
     */
    public void resetAll() {
        for (String name : limits.keySet()) {
            resetLimit(name);
        }
    }

    /**
     * Rate limit exceeded exception.
     */
    public static class RateLimitExceededException extends Exception {
        private final String limitName;
        private final long retryAfterMs;

        public RateLimitExceededException(String limitName, long retryAfterMs) {
            super("Rate limit exceeded: " + limitName);
            this.limitName = limitName;
            this.retryAfterMs = retryAfterMs;
        }

        public String getLimitName() { return limitName; }
        public long getRetryAfterMs() { return retryAfterMs; }
    }
}