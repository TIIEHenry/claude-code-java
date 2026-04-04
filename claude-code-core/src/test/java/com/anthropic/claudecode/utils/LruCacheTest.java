/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LruCache.
 */
class LruCacheTest {

    @Test
    @DisplayName("LruCache creates with max size")
    void createsWithMaxSize() {
        LruCache<String, Integer> cache = new LruCache<>(10);
        assertEquals(10, cache.maxSize());
        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("LruCache put and get works")
    void putAndGetWorks() {
        LruCache<String, Integer> cache = new LruCache<>(10);
        cache.put("key", 42);

        assertEquals(42, cache.get("key"));
        assertEquals(1, cache.size());
    }

    @Test
    @DisplayName("LruCache get returns null for missing key")
    void getReturnsNull() {
        LruCache<String, Integer> cache = new LruCache<>(10);
        assertNull(cache.get("missing"));
    }

    @Test
    @DisplayName("LruCache getOrDefault works")
    void getOrDefaultWorks() {
        LruCache<String, Integer> cache = new LruCache<>(10);
        cache.put("key", 42);

        assertEquals(42, cache.getOrDefault("key", 0));
        assertEquals(0, cache.getOrDefault("missing", 0));
    }

    @Test
    @DisplayName("LruCache computeIfAbsent computes when missing")
    void computeIfAbsentComputes() {
        LruCache<String, Integer> cache = new LruCache<>(10);
        Function<String, Integer> loader = k -> k.length();

        int value = cache.computeIfAbsent("hello", loader);

        assertEquals(5, value);
        assertEquals(5, cache.get("hello"));
    }

    @Test
    @DisplayName("LruCache computeIfAbsent returns cached value")
    void computeIfAbsentCached() {
        LruCache<String, Integer> cache = new LruCache<>(10);
        cache.put("key", 42);

        int value = cache.computeIfAbsent("key", k -> 100);

        assertEquals(42, value);
    }

    @Test
    @DisplayName("LruCache putIfAbsent works")
    void putIfAbsentWorks() {
        LruCache<String, Integer> cache = new LruCache<>(10);

        assertNull(cache.putIfAbsent("key", 42));
        assertEquals(42, cache.putIfAbsent("key", 100)); // Returns old value
        assertEquals(42, cache.get("key")); // Value unchanged
    }

    @Test
    @DisplayName("LruCache remove works")
    void removeWorks() {
        LruCache<String, Integer> cache = new LruCache<>(10);
        cache.put("key", 42);

        assertEquals(42, cache.remove("key"));
        assertNull(cache.get("key"));
    }

    @Test
    @DisplayName("LruCache containsKey works")
    void containsKeyWorks() {
        LruCache<String, Integer> cache = new LruCache<>(10);
        cache.put("key", 42);

        assertTrue(cache.containsKey("key"));
        assertFalse(cache.containsKey("missing"));
    }

    @Test
    @DisplayName("LruCache containsValue works")
    void containsValueWorks() {
        LruCache<String, Integer> cache = new LruCache<>(10);
        cache.put("key", 42);

        assertTrue(cache.containsValue(42));
        assertFalse(cache.containsValue(100));
    }

    @Test
    @DisplayName("LruCache clear works")
    void clearWorks() {
        LruCache<String, Integer> cache = new LruCache<>(10);
        cache.put("key1", 1);
        cache.put("key2", 2);

        cache.clear();

        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
    }

    @Test
    @DisplayName("LruCache evicts oldest when full")
    void evictsWhenFull() {
        LruCache<String, Integer> cache = new LruCache<>(3);

        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        cache.put("d", 4); // Should evict oldest

        assertTrue(cache.size() <= 3);
    }

    @Test
    @DisplayName("LruCache keys returns all keys")
    void keysReturnsAll() {
        LruCache<String, Integer> cache = new LruCache<>(10);
        cache.put("a", 1);
        cache.put("b", 2);

        Set<String> keys = cache.keys();

        assertEquals(2, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));
    }

    @Test
    @DisplayName("LruCache hitRate calculates correctly")
    void hitRateWorks() {
        LruCache<String, Integer> cache = new LruCache<>(10);
        cache.put("key", 42);

        cache.get("key"); // hit
        cache.get("missing"); // miss

        assertEquals(0.5, cache.hitRate());
    }

    @Test
    @DisplayName("LruCache stats returns statistics")
    void statsWorks() {
        LruCache<String, Integer> cache = new LruCache<>(10);
        cache.put("key", 42);
        cache.get("key");

        LruCache.CacheStats stats = cache.stats();

        assertEquals(1, stats.size());
        assertEquals(10, stats.maxSize());
        assertEquals(1, stats.hits());
        assertEquals(0, stats.misses());
    }

    @Test
    @DisplayName("LruCache CacheStats format works")
    void cacheStatsFormat() {
        LruCache.CacheStats stats = new LruCache.CacheStats(5, 10, 3, 2, 0.6);

        String formatted = stats.format();

        assertTrue(formatted.contains("size=5/10"));
        assertTrue(formatted.contains("hits=3"));
        assertTrue(formatted.contains("misses=2"));
    }

    @Test
    @DisplayName("LruCache builder creates cache")
    void builderCreates() {
        LruCache<String, Integer> cache = LruCache.<String, Integer>builder()
            .maxSize(100)
            .build();

        assertEquals(100, cache.maxSize());
    }

    @Test
    @DisplayName("LruCache builder buildLoading creates loading cache")
    void builderLoadingCache() {
        LruCache.LoadingLruCache<String, Integer> cache = LruCache.<String, Integer>builder()
            .maxSize(100)
            .loader(k -> k.length())
            .buildLoading();

        assertEquals(5, cache.get("hello")); // Auto-loaded
    }

    @Test
    @DisplayName("LruCache ThreadSafeLruCache works")
    void threadSafeWorks() {
        LruCache.ThreadSafeLruCache<String, Integer> cache = new LruCache.ThreadSafeLruCache<>(10);

        cache.put("key", 42);
        assertEquals(42, cache.get("key"));

        cache.computeIfAbsent("hello", k -> k.length());
        assertEquals(5, cache.get("hello"));

        cache.remove("key");
        assertNull(cache.get("key"));
    }
}