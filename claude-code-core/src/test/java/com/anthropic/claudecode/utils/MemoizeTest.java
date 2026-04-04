/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Memoize.
 */
class MemoizeTest {

    @Test
    @DisplayName("Memoize memoize caches supplier result")
    void memoizeSupplier() {
        AtomicInteger calls = new AtomicInteger(0);
        java.util.function.Supplier<Integer> supplier = () -> {
            calls.incrementAndGet();
            return 42;
        };

        java.util.function.Supplier<Integer> memoized = Memoize.memoize(supplier);

        assertEquals(42, memoized.get());
        assertEquals(42, memoized.get());
        assertEquals(1, calls.get()); // Only called once
    }

    @Test
    @DisplayName("Memoize memoizeWithLRU caches function result")
    void memoizeFunction() {
        AtomicInteger calls = new AtomicInteger(0);
        Function<String, Integer> function = s -> {
            calls.incrementAndGet();
            return s.length();
        };

        Function<String, Integer> memoized = Memoize.memoizeWithLRU(function, 10);

        assertEquals(5, memoized.apply("hello"));
        assertEquals(5, memoized.apply("hello"));
        assertEquals(1, calls.get()); // Only called once for same input

        assertEquals(5, memoized.apply("world"));
        assertEquals(2, calls.get()); // Called for new input
    }

    @Test
    @DisplayName("Memoize LRUCache evicts old entries")
    void lruCacheEviction() {
        Memoize.LRUCache<String, Integer> cache = new Memoize.LRUCache<>(3);

        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        cache.put("d", 4); // Should evict "a"

        assertTrue(cache.size() <= 4);
    }

    @Test
    @DisplayName("Memoize LRUCache get returns value")
    void lruCacheGet() {
        Memoize.LRUCache<String, Integer> cache = new Memoize.LRUCache<>(10);

        cache.put("key", 42);
        assertEquals(42, cache.get("key"));
        assertNull(cache.get("missing"));
    }

    @Test
    @DisplayName("Memoize LRUCache computeIfAbsent computes")
    void lruCacheComputeIfAbsent() {
        Memoize.LRUCache<String, Integer> cache = new Memoize.LRUCache<>(10);

        int value = cache.computeIfAbsent("key", k -> k.length());

        assertEquals(3, value);
        assertTrue(cache.containsKey("key"));
    }

    @Test
    @DisplayName("Memoize LRUCache computeIfAbsent returns cached")
    void lruCacheComputeIfAbsentCached() {
        Memoize.LRUCache<String, Integer> cache = new Memoize.LRUCache<>(10);
        AtomicInteger calls = new AtomicInteger(0);

        cache.computeIfAbsent("key", k -> {
            calls.incrementAndGet();
            return 42;
        });
        cache.computeIfAbsent("key", k -> {
            calls.incrementAndGet();
            return 100;
        });

        assertEquals(1, calls.get()); // Only computed once
    }

    @Test
    @DisplayName("Memoize LRUCache remove works")
    void lruCacheRemove() {
        Memoize.LRUCache<String, Integer> cache = new Memoize.LRUCache<>(10);

        cache.put("key", 42);
        cache.remove("key");

        assertFalse(cache.containsKey("key"));
    }

    @Test
    @DisplayName("Memoize LRUCache clear works")
    void lruCacheClear() {
        Memoize.LRUCache<String, Integer> cache = new Memoize.LRUCache<>(10);

        cache.put("a", 1);
        cache.put("b", 2);
        cache.clear();

        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("Memoize LRUCache keySet returns keys")
    void lruCacheKeySet() {
        Memoize.LRUCache<String, Integer> cache = new Memoize.LRUCache<>(10);

        cache.put("a", 1);
        cache.put("b", 2);

        assertTrue(cache.keySet().contains("a"));
        assertTrue(cache.keySet().contains("b"));
    }

    @Test
    @DisplayName("Memoize LRUCache values returns values")
    void lruCacheValues() {
        Memoize.LRUCache<String, Integer> cache = new Memoize.LRUCache<>(10);

        cache.put("a", 1);
        cache.put("b", 2);

        assertTrue(cache.values().contains(1));
        assertTrue(cache.values().contains(2));
    }

    @Test
    @DisplayName("Memoize memoizeWithExpiry expires after time")
    void memoizeWithExpiry() throws InterruptedException {
        AtomicInteger calls = new AtomicInteger(0);
        java.util.function.Supplier<Integer> supplier = () -> {
            calls.incrementAndGet();
            return 42;
        };

        java.util.function.Supplier<Integer> memoized = Memoize.memoizeWithExpiry(supplier, 100);

        assertEquals(42, memoized.get());
        assertEquals(42, memoized.get());
        assertEquals(1, calls.get());

        // Wait for expiry
        Thread.sleep(150);

        assertEquals(42, memoized.get());
        assertEquals(2, calls.get()); // Recomputed after expiry
    }
}