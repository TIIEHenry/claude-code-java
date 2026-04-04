/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code cache utility
 */
package com.anthropic.claudecode.utils.cache;

import java.util.*;
import java.util.concurrent.*;

/**
 * Simple in-memory cache with TTL support.
 */
public final class Cache<K, V> {
    private final int maxEntries;
    private final long maxAgeMs;
    private final ConcurrentHashMap<K, CacheEntry<V>> entries;
    private long hitCount = 0;
    private long missCount = 0;

    /**
     * Cache entry with timestamp.
     */
    private record CacheEntry<V>(V value, long timestamp) {
        boolean isExpired(long maxAgeMs) {
            return System.currentTimeMillis() - timestamp > maxAgeMs;
        }
    }

    public Cache(int maxEntries, long maxAgeMs) {
        this.maxEntries = maxEntries;
        this.maxAgeMs = maxAgeMs;
        this.entries = new ConcurrentHashMap<>();
    }

    /**
     * Get value from cache.
     */
    public V get(K key) {
        CacheEntry<V> entry = entries.get(key);
        if (entry == null) {
            missCount++;
            return null;
        }

        if (entry.isExpired(maxAgeMs)) {
            entries.remove(key);
            missCount++;
            return null;
        }

        hitCount++;
        return entry.value();
    }

    /**
     * Put value in cache.
     */
    public void put(K key, V value) {
        // Evict oldest entries if over limit
        if (entries.size() >= maxEntries) {
            evictOldest();
        }

        entries.put(key, new CacheEntry<>(value, System.currentTimeMillis()));
    }

    /**
     * Remove entry from cache.
     */
    public void remove(K key) {
        entries.remove(key);
    }

    /**
     * Clear cache.
     */
    public void clear() {
        entries.clear();
        hitCount = 0;
        missCount = 0;
    }

    /**
     * Get cache size.
     */
    public int size() {
        return entries.size();
    }

    /**
     * Get all keys.
     */
    public Set<K> keys() {
        return new HashSet<>(entries.keySet());
    }

    /**
     * Get hit count.
     */
    public long getHitCount() {
        return hitCount;
    }

    /**
     * Get miss count.
     */
    public long getMissCount() {
        return missCount;
    }

    /**
     * Check if key exists.
     */
    public boolean containsKey(K key) {
        CacheEntry<V> entry = entries.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired(maxAgeMs)) {
            entries.remove(key);
            return false;
        }
        return true;
    }

    /**
     * Evict oldest entries.
     */
    private void evictOldest() {
        // Remove expired entries first
        long now = System.currentTimeMillis();
        entries.entrySet().removeIf(e ->
            now - e.getValue().timestamp() > maxAgeMs);

        // If still over limit, remove random entries
        if (entries.size() >= maxEntries) {
            Iterator<K> it = entries.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }
}