/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MemoizeUtils.
 */
class MemoizeUtilsTest {

    @Test
    @DisplayName("MemoizeUtils memoize supplier caches value")
    void memoizeSupplierCaches() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = MemoizeUtils.memoize(counter::incrementAndGet);

        memoized.get();
        memoized.get();
        memoized.get();

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("MemoizeUtils memoize supplier returns cached value")
    void memoizeSupplierReturnsCached() {
        Supplier<String> memoized = MemoizeUtils.memoize(() -> "computed");

        String result1 = memoized.get();
        String result2 = memoized.get();

        assertEquals("computed", result1);
        assertEquals(result1, result2);
    }

    @Test
    @DisplayName("MemoizeUtils memoizeWithTTL supplier expires")
    void memoizeWithTTLExpires() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = MemoizeUtils.memoizeWithTTL(counter::incrementAndGet, 50);

        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());

        Thread.sleep(100);

        assertEquals(2, memoized.get());
    }

    @Test
    @DisplayName("MemoizeUtils memoize function caches by argument")
    void memoizeFunctionCaches() {
        AtomicInteger counter = new AtomicInteger(0);
        Function<Integer, Integer> memoized = MemoizeUtils.memoize(x -> {
            counter.incrementAndGet();
            return x * 2;
        });

        assertEquals(4, memoized.apply(2));
        assertEquals(4, memoized.apply(2));
        assertEquals(6, memoized.apply(3));
        assertEquals(6, memoized.apply(3));

        assertEquals(2, counter.get());
    }

    @Test
    @DisplayName("MemoizeUtils memoizeWithTTL function expires")
    void memoizeWithTTLFunctionExpires() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Function<Integer, Integer> memoized = MemoizeUtils.memoizeWithTTL(
            x -> {
                counter.incrementAndGet();
                return x * 2;
            },
            50
        );

        assertEquals(4, memoized.apply(2));
        assertEquals(4, memoized.apply(2));

        Thread.sleep(100);

        assertEquals(4, memoized.apply(2));
        assertEquals(2, counter.get());
    }

    @Test
    @DisplayName("MemoizeUtils memoizeAsync supplier caches future")
    void memoizeAsyncCaches() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<CompletableFuture<Integer>> memoized = MemoizeUtils.memoizeAsync(
            () -> CompletableFuture.completedFuture(counter.incrementAndGet())
        );

        CompletableFuture<Integer> future1 = memoized.get();
        CompletableFuture<Integer> future2 = memoized.get();

        assertEquals(future1, future2);
        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("MemoizeUtils createLRUCache creates cache")
    void createLRUCache() {
        MemoizeUtils.LRUCache<String, Integer> cache = MemoizeUtils.createLRUCache(3);
        assertNotNull(cache);
        assertEquals(3, cache.maxSize());
    }

    @Test
    @DisplayName("MemoizeUtils LRUCache put and get")
    void lruCachePutGet() {
        MemoizeUtils.LRUCache<String, Integer> cache = MemoizeUtils.createLRUCache(3);

        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);

        assertEquals(1, cache.get("a"));
        assertEquals(2, cache.get("b"));
        assertEquals(3, cache.get("c"));
    }

    @Test
    @DisplayName("MemoizeUtils LRUCache evicts oldest")
    void lruCacheEvicts() {
        MemoizeUtils.LRUCache<String, Integer> cache = MemoizeUtils.createLRUCache(2);

        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3); // Should evict "a"

        assertNull(cache.get("a"));
        assertEquals(2, cache.get("b"));
        assertEquals(3, cache.get("c"));
    }

    @Test
    @DisplayName("MemoizeUtils LRUCache peek does not update recency")
    void lruCachePeek() {
        MemoizeUtils.LRUCache<String, Integer> cache = MemoizeUtils.createLRUCache(2);

        cache.put("a", 1);
        cache.put("b", 2);

        cache.peek("a"); // Should not update recency
        cache.put("c", 3); // Should evict "a" (oldest), not "b"

        assertNull(cache.get("a"));
        assertEquals(2, cache.get("b"));
        assertEquals(3, cache.get("c"));
    }

    @Test
    @DisplayName("MemoizeUtils LRUCache containsKey")
    void lruCacheContainsKey() {
        MemoizeUtils.LRUCache<String, Integer> cache = MemoizeUtils.createLRUCache(3);

        cache.put("a", 1);

        assertTrue(cache.containsKey("a"));
        assertFalse(cache.containsKey("b"));
    }

    @Test
    @DisplayName("MemoizeUtils LRUCache remove")
    void lruCacheRemove() {
        MemoizeUtils.LRUCache<String, Integer> cache = MemoizeUtils.createLRUCache(3);

        cache.put("a", 1);
        assertTrue(cache.remove("a"));
        assertFalse(cache.containsKey("a"));
        assertFalse(cache.remove("a")); // Already removed
    }

    @Test
    @DisplayName("MemoizeUtils LRUCache clear")
    void lruCacheClear() {
        MemoizeUtils.LRUCache<String, Integer> cache = MemoizeUtils.createLRUCache(3);

        cache.put("a", 1);
        cache.put("b", 2);
        cache.clear();

        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("MemoizeUtils LRUCache size")
    void lruCacheSize() {
        MemoizeUtils.LRUCache<String, Integer> cache = MemoizeUtils.createLRUCache(3);

        assertEquals(0, cache.size());
        cache.put("a", 1);
        assertEquals(1, cache.size());
        cache.put("b", 2);
        assertEquals(2, cache.size());
    }

    @Test
    @DisplayName("MemoizeUtils LRUCache asMap")
    void lruCacheAsMap() {
        MemoizeUtils.LRUCache<String, Integer> cache = MemoizeUtils.createLRUCache(3);

        cache.put("a", 1);
        cache.put("b", 2);

        var map = cache.asMap();
        assertEquals(2, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
    }

    @Test
    @DisplayName("MemoizeUtils memoizeWithLRU function caches")
    void memoizeWithLRUCaches() {
        AtomicInteger counter = new AtomicInteger(0);
        Function<Integer, Integer> memoized = MemoizeUtils.memoizeWithLRU(
            x -> {
                counter.incrementAndGet();
                return x * 2;
            },
            3
        );

        assertEquals(4, memoized.apply(2));
        assertEquals(4, memoized.apply(2));
        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("MemoizeUtils memoizeWithLRU evicts")
    void memoizeWithLRUEvicts() {
        AtomicInteger counter = new AtomicInteger(0);
        Function<Integer, Integer> memoized = MemoizeUtils.memoizeWithLRU(
            x -> {
                counter.incrementAndGet();
                return x * 2;
            },
            2
        );

        memoized.apply(1); // counter = 1
        memoized.apply(2); // counter = 2
        memoized.apply(3); // counter = 3, evicts 1
        memoized.apply(1); // counter = 4, recomputed

        assertEquals(4, counter.get());
    }

    @Test
    @DisplayName("MemoizeUtils LRUCache get updates recency")
    void lruCacheGetUpdatesRecency() {
        MemoizeUtils.LRUCache<String, Integer> cache = MemoizeUtils.createLRUCache(2);

        cache.put("a", 1);
        cache.put("b", 2);
        cache.get("a"); // "a" becomes most recently used
        cache.put("c", 3); // Should evict "b" (now oldest)

        assertEquals(1, cache.get("a"));
        assertNull(cache.get("b"));
        assertEquals(3, cache.get("c"));
    }

    @Test
    @DisplayName("MemoizeUtils LRUCache peek returns null for missing")
    void lruCachePeekNull() {
        MemoizeUtils.LRUCache<String, Integer> cache = MemoizeUtils.createLRUCache(3);
        assertNull(cache.peek("nonexistent"));
    }

    @Test
    @DisplayName("MemoizeUtils LRUCache maxSize")
    void lruCacheMaxSize() {
        MemoizeUtils.LRUCache<String, Integer> cache = MemoizeUtils.createLRUCache(10);
        assertEquals(10, cache.maxSize());
    }
}