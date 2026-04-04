/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/completionCache
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Completion cache - Caches completion results for deduplication.
 */
public final class CompletionCache<T> {
    private final ConcurrentHashMap<String, CacheEntry<T>> cache = new ConcurrentHashMap<>();
    private final long defaultTtlMs;
    private final int maxSize;

    /**
     * Create a completion cache.
     */
    public CompletionCache(long defaultTtlMs, int maxSize) {
        this.defaultTtlMs = defaultTtlMs;
        this.maxSize = maxSize;
    }

    /**
     * Create a completion cache with defaults.
     */
    public CompletionCache() {
        this(60000, 1000); // 1 minute TTL, 1000 max entries
    }

    /**
     * Get a cached completion.
     */
    public T get(String key) {
        CacheEntry<T> entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }

        return entry.value;
    }

    /**
     * Put a completion in the cache.
     */
    public void put(String key, T value) {
        put(key, value, defaultTtlMs);
    }

    /**
     * Put a completion in the cache with custom TTL.
     */
    public void put(String key, T value, long ttlMs) {
        if (cache.size() >= maxSize) {
            evictExpired();
            if (cache.size() >= maxSize) {
                // Remove oldest entries
                cache.keySet().stream()
                    .limit(maxSize / 10)
                    .forEach(cache::remove);
            }
        }

        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttlMs));
    }

    /**
     * Get or compute a completion.
     */
    public T getOrCompute(String key, java.util.function.Supplier<T> supplier) {
        T cached = get(key);
        if (cached != null) {
            return cached;
        }

        T value = supplier.get();
        if (value != null) {
            put(key, value);
        }
        return value;
    }

    /**
     * Get or compute a completion asynchronously.
     */
    public CompletableFuture<T> getOrComputeAsync(String key, java.util.function.Supplier<CompletableFuture<T>> supplier) {
        T cached = get(key);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return supplier.get().thenApply(value -> {
            if (value != null) {
                put(key, value);
            }
            return value;
        });
    }

    /**
     * Remove a cached completion.
     */
    public void remove(String key) {
        cache.remove(key);
    }

    /**
     * Check if a key exists.
     */
    public boolean contains(String key) {
        CacheEntry<T> entry = cache.get(key);
        if (entry == null) {
            return false;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }

        return true;
    }

    /**
     * Clear the cache.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Get cache size.
     */
    public int size() {
        return cache.size();
    }

    /**
     * Evict expired entries.
     */
    public void evictExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Get cache statistics.
     */
    public CacheStats getStats() {
        long now = System.currentTimeMillis();
        int valid = 0;
        int expired = 0;

        for (CacheEntry<T> entry : cache.values()) {
            if (entry.expiresAt > now) {
                valid++;
            } else {
                expired++;
            }
        }

        return new CacheStats(valid, expired, maxSize);
    }

    /**
     * Cache entry record.
     */
    private record CacheEntry<T>(T value, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    /**
     * Cache statistics record.
     */
    public record CacheStats(int validEntries, int expiredEntries, int maxSize) {
        public int totalEntries() {
            return validEntries + expiredEntries;
        }

        public double utilizationPercent() {
            return (totalEntries() * 100.0) / maxSize;
        }
    }
}