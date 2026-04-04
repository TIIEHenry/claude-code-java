/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CacheUtils.
 */
class CacheUtilsTest {

    @Test
    @DisplayName("CacheUtils CacheEntry isExpired works")
    void cacheEntryExpired() throws InterruptedException {
        CacheUtils.CacheEntry<String> entry = new CacheUtils.CacheEntry<>("value", System.currentTimeMillis() + 100);

        assertFalse(entry.isExpired());

        Thread.sleep(150);

        assertTrue(entry.isExpired());
    }

    @Test
    @DisplayName("CacheUtils CacheEntry value returns value")
    void cacheEntryValue() {
        CacheUtils.CacheEntry<String> entry = new CacheUtils.CacheEntry<>("test", Long.MAX_VALUE);

        assertEquals("test", entry.value());
    }

    @Test
    @DisplayName("CacheUtils SimpleCache put and get works")
    void simpleCachePutGet() {
        CacheUtils.SimpleCache<String, String> cache = new CacheUtils.SimpleCache<>(60000);

        cache.put("key", "value");

        assertEquals("value", cache.get("key"));
    }

    @Test
    @DisplayName("CacheUtils SimpleCache get returns null for missing key")
    void simpleCacheGetMissing() {
        CacheUtils.SimpleCache<String, String> cache = new CacheUtils.SimpleCache<>(60000);

        assertNull(cache.get("missing"));
    }

    @Test
    @DisplayName("CacheUtils SimpleCache remove works")
    void simpleCacheRemove() {
        CacheUtils.SimpleCache<String, String> cache = new CacheUtils.SimpleCache<>(60000);
        cache.put("key", "value");

        cache.remove("key");

        assertNull(cache.get("key"));
    }

    @Test
    @DisplayName("CacheUtils SimpleCache clear works")
    void simpleCacheClear() {
        CacheUtils.SimpleCache<String, String> cache = new CacheUtils.SimpleCache<>(60000);
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        cache.clear();

        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("CacheUtils SimpleCache computeIfAbsent works")
    void simpleCacheComputeIfAbsent() {
        CacheUtils.SimpleCache<String, String> cache = new CacheUtils.SimpleCache<>(60000);

        String result = cache.computeIfAbsent("key", () -> "computed");

        assertEquals("computed", result);
        assertEquals("computed", cache.get("key"));
    }

    @Test
    @DisplayName("CacheUtils SimpleCache containsKey works")
    void simpleCacheContainsKey() {
        CacheUtils.SimpleCache<String, String> cache = new CacheUtils.SimpleCache<>(60000);
        cache.put("key", "value");

        assertTrue(cache.containsKey("key"));
        assertFalse(cache.containsKey("missing"));
    }

    @Test
    @DisplayName("CacheUtils SimpleCache size works")
    void simpleCacheSize() {
        CacheUtils.SimpleCache<String, String> cache = new CacheUtils.SimpleCache<>(60000);

        cache.put("key1", "value1");
        cache.put("key2", "value2");

        assertEquals(2, cache.size());
    }

    @Test
    @DisplayName("CacheUtils SimpleCache keys works")
    void simpleCacheKeys() {
        CacheUtils.SimpleCache<String, String> cache = new CacheUtils.SimpleCache<>(60000);
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        assertEquals(2, cache.keys().size());
        assertTrue(cache.keys().contains("key1"));
        assertTrue(cache.keys().contains("key2"));
    }

    @Test
    @DisplayName("CacheUtils LruCache put and get works")
    void lruCachePutGet() {
        CacheUtils.LruCache<String, String> cache = new CacheUtils.LruCache<>(3);

        cache.put("key", "value");

        assertEquals("value", cache.get("key"));
    }

    @Test
    @DisplayName("CacheUtils LruCache evicts oldest when full")
    void lruCacheEviction() {
        CacheUtils.LruCache<String, String> cache = new CacheUtils.LruCache<>(2);

        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");

        assertNull(cache.get("a"));
        assertEquals("2", cache.get("b"));
        assertEquals("3", cache.get("c"));
    }

    @Test
    @DisplayName("CacheUtils LruCache computeIfAbsent works")
    void lruCacheComputeIfAbsent() {
        CacheUtils.LruCache<String, String> cache = new CacheUtils.LruCache<>(3);

        String result = cache.computeIfAbsent("key", () -> "computed");

        assertEquals("computed", result);
    }

    @Test
    @DisplayName("CacheUtils LoadingCache loads values")
    void loadingCacheLoads() {
        Function<String, String> loader = k -> "loaded-" + k;
        CacheUtils.LoadingCache<String, String> cache = CacheUtils.createLoadingCache(loader, 60000, 120000);

        String result = cache.get("key");

        assertEquals("loaded-key", result);
    }

    @Test
    @DisplayName("CacheUtils LoadingCache caches loaded values")
    void loadingCacheCaches() {
        int[] callCount = {0};
        Function<String, String> loader = k -> {
            callCount[0]++;
            return "value";
        };
        CacheUtils.LoadingCache<String, String> cache = CacheUtils.createLoadingCache(loader, 60000, 120000);

        cache.get("key");
        cache.get("key");

        assertEquals(1, callCount[0]);
    }

    @Test
    @DisplayName("CacheUtils LoadingCache invalidate works")
    void loadingCacheInvalidate() {
        int[] callCount = {0};
        Function<String, String> loader = k -> {
            callCount[0]++;
            return "value";
        };
        CacheUtils.LoadingCache<String, String> cache = CacheUtils.createLoadingCache(loader, 60000, 120000);

        cache.get("key");
        cache.invalidate("key");
        cache.get("key");

        assertEquals(2, callCount[0]);
    }

    @Test
    @DisplayName("CacheUtils memoize caches function results")
    void memoizeWorks() {
        int[] callCount = {0};
        Function<String, String> fn = s -> {
            callCount[0]++;
            return s.toUpperCase();
        };

        Function<String, String> memoized = CacheUtils.memoize(fn);

        assertEquals("HELLO", memoized.apply("hello"));
        assertEquals("HELLO", memoized.apply("hello"));
        assertEquals(1, callCount[0]);
    }

    @Test
    @DisplayName("CacheUtils memoize supplier caches results")
    void memoizeSupplier() {
        int[] callCount = {0};

        java.util.function.Supplier<String> supplier = () -> {
            callCount[0]++;
            return "value";
        };

        java.util.function.Supplier<String> memoized = CacheUtils.memoize(supplier);

        assertEquals("value", memoized.get());
        assertEquals("value", memoized.get());
        assertEquals(1, callCount[0]);
    }

    @Test
    @DisplayName("CacheUtils createSimpleCache creates cache")
    void createSimpleCache() {
        CacheUtils.SimpleCache<String, String> cache = CacheUtils.createSimpleCache(60000);

        assertNotNull(cache);
        cache.put("key", "value");
        assertEquals("value", cache.get("key"));
    }

    @Test
    @DisplayName("CacheUtils createLruCache creates cache")
    void createLruCache() {
        CacheUtils.LruCache<String, String> cache = CacheUtils.createLruCache(10);

        assertNotNull(cache);
        cache.put("key", "value");
        assertEquals("value", cache.get("key"));
    }

    @Test
    @DisplayName("CacheUtils StatsCache tracks hits and misses")
    void statsCacheTracking() {
        CacheUtils.StatsCache<String, String> cache = new CacheUtils.StatsCache<>(60000);
        cache.put("key", "value");

        cache.get("key");  // hit
        cache.get("key");  // hit
        cache.get("missing");  // miss

        assertEquals(2, cache.getHits());
        assertEquals(1, cache.getMisses());
    }

    @Test
    @DisplayName("CacheUtils StatsCache hitRate works")
    void statsCacheHitRate() {
        CacheUtils.StatsCache<String, String> cache = new CacheUtils.StatsCache<>(60000);
        cache.put("key", "value");

        cache.get("key");  // hit
        cache.get("key");  // hit
        cache.get("missing");  // miss
        cache.get("missing");  // miss

        assertEquals(0.5, cache.getHitRate(), 0.01);
    }

    @Test
    @DisplayName("CacheUtils StatsCache resetStats works")
    void statsCacheReset() {
        CacheUtils.StatsCache<String, String> cache = new CacheUtils.StatsCache<>(60000);
        cache.put("key", "value");
        cache.get("key");
        cache.get("missing");

        cache.resetStats();

        assertEquals(0, cache.getHits());
        assertEquals(0, cache.getMisses());
    }

    @Test
    @DisplayName("CacheUtils TwoLevelCache checks both levels")
    void twoLevelCacheWorks() {
        CacheUtils.TwoLevelCache<String, String> cache = new CacheUtils.TwoLevelCache<>(
            60000,
            k -> Optional.of("l2-value"),
            (k, v) -> {},
            k -> "loaded-value"
        );

        String result = cache.get("key");

        assertEquals("l2-value", result);
    }

    @Test
    @DisplayName("CacheUtils TwoLevelCache loads when not in cache")
    void twoLevelCacheLoads() {
        CacheUtils.TwoLevelCache<String, String> cache = new CacheUtils.TwoLevelCache<>(
            60000,
            k -> Optional.empty(),
            (k, v) -> {},
            k -> "loaded-" + k
        );

        String result = cache.get("key");

        assertEquals("loaded-key", result);
    }
}