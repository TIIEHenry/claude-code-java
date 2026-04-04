/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Memoizer.
 */
class MemoizerTest {

    @Test
    @DisplayName("Memoizer memoize supplier caches result")
    void memoizeSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = Memoizer.memoize(() -> counter.incrementAndGet());

        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());
        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("Memoizer memoize supplier invalidate")
    void memoizeSupplierInvalidate() {
        AtomicInteger counter = new AtomicInteger(0);
        Memoizer.MemoizedSupplier<Integer> memoized =
            new Memoizer.MemoizedSupplier<>(() -> counter.incrementAndGet());

        assertEquals(1, memoized.get());
        memoized.invalidate();
        assertEquals(2, memoized.get());
    }

    @Test
    @DisplayName("Memoizer memoize supplier isComputed")
    void memoizeSupplierIsComputed() {
        Memoizer.MemoizedSupplier<Integer> memoized =
            new Memoizer.MemoizedSupplier<>(() -> 42);

        assertFalse(memoized.isComputed());
        memoized.get();
        assertTrue(memoized.isComputed());
    }

    @Test
    @DisplayName("Memoizer memoize function caches by key")
    void memoizeFunction() {
        AtomicInteger counter = new AtomicInteger(0);
        Function<Integer, Integer> memoized = Memoizer.memoize(x -> counter.incrementAndGet() + x);

        assertEquals(2, memoized.apply(1));
        assertEquals(2, memoized.apply(1));
        assertEquals(4, memoized.apply(2));
        assertEquals(2, counter.get());
    }

    @Test
    @DisplayName("Memoizer memoize function invalidate key")
    void memoizeFunctionInvalidate() {
        AtomicInteger counter = new AtomicInteger(0);
        Memoizer.MemoizedFunction<Integer, Integer> memoized =
            new Memoizer.MemoizedFunction<>(x -> counter.incrementAndGet());

        assertEquals(1, memoized.apply(1));
        memoized.invalidate(1);
        assertEquals(2, memoized.apply(1));
    }

    @Test
    @DisplayName("Memoizer memoize function invalidateAll")
    void memoizeFunctionInvalidateAll() {
        Memoizer.MemoizedFunction<Integer, Integer> memoized =
            new Memoizer.MemoizedFunction<>(x -> x * 2);

        memoized.apply(1);
        memoized.apply(2);
        assertEquals(2, memoized.size());

        memoized.invalidateAll();
        assertEquals(0, memoized.size());
    }

    @Test
    @DisplayName("Memoizer memoize function contains")
    void memoizeFunctionContains() {
        Memoizer.MemoizedFunction<Integer, Integer> memoized =
            new Memoizer.MemoizedFunction<>(x -> x * 2);

        assertFalse(memoized.contains(1));
        memoized.apply(1);
        assertTrue(memoized.contains(1));
    }

    @Test
    @DisplayName("Memoizer memoizeWithTtl supplier expires")
    void memoizeWithTtlSupplier() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = Memoizer.memoizeWithTtl(
            () -> counter.incrementAndGet(),
            Duration.ofMillis(50)
        );

        assertEquals(1, memoized.get());
        Thread.sleep(100);
        assertEquals(2, memoized.get());
    }

    @Test
    @DisplayName("Memoizer TtlMemoizedSupplier remainingTime")
    void ttlMemoizedSupplierRemainingTime() {
        Memoizer.TtlMemoizedSupplier<Integer> memoized =
            new Memoizer.TtlMemoizedSupplier<>(() -> 42, Duration.ofSeconds(10));

        memoized.get();
        Duration remaining = memoized.remainingTime();

        assertTrue(remaining.toMillis() > 0);
    }

    @Test
    @DisplayName("Memoizer memoizeWithTtl function expires")
    void memoizeWithTtlFunction() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Function<Integer, Integer> memoized = Memoizer.memoizeWithTtl(
            x -> counter.incrementAndGet(),
            Duration.ofMillis(50)
        );

        assertEquals(1, memoized.apply(1));
        Thread.sleep(100);
        assertEquals(2, memoized.apply(1));
    }

    @Test
    @DisplayName("Memoizer memoizeWithLimit limits size")
    void memoizeWithLimit() {
        AtomicInteger counter = new AtomicInteger(0);
        Function<Integer, Integer> memoized = Memoizer.memoizeWithLimit(
            x -> counter.incrementAndGet(),
            2
        );

        assertEquals(1, memoized.apply(1));
        assertEquals(2, memoized.apply(2));
        assertEquals(3, memoized.apply(3)); // Evicts oldest
    }

    @Test
    @DisplayName("Memoizer memoizeWithKey uses custom key")
    void memoizeWithKey() {
        AtomicInteger counter = new AtomicInteger(0);
        Function<String, Integer> memoized = Memoizer.memoizeWithKey(
            s -> counter.incrementAndGet(),
            String::length
        );

        assertEquals(1, memoized.apply("ab"));
        assertEquals(1, memoized.apply("cd")); // Same length, same key
        assertEquals(2, memoized.apply("abc")); // Different length
    }

    @Test
    @DisplayName("Memoizer MemoStats record")
    void memoStatsRecord() {
        Memoizer.MemoStats stats = new Memoizer.MemoStats(80, 20, 100);

        assertEquals(80, stats.hits());
        assertEquals(20, stats.misses());
        assertEquals(100, stats.size());
        assertEquals(0.8, stats.hitRate(), 0.001);
    }

    @Test
    @DisplayName("Memoizer MemoStats hitRate zero total")
    void memoStatsHitRateZero() {
        Memoizer.MemoStats stats = new Memoizer.MemoStats(0, 0, 0);

        assertEquals(0.0, stats.hitRate(), 0.001);
    }

    @Test
    @DisplayName("Memoizer memoize function size")
    void memoizeFunctionSize() {
        Memoizer.MemoizedFunction<Integer, Integer> memoized =
            new Memoizer.MemoizedFunction<>(x -> x * 2);

        assertEquals(0, memoized.size());
        memoized.apply(1);
        memoized.apply(2);
        assertEquals(2, memoized.size());
    }

    @Test
    @DisplayName("Memoizer TtlMemoizedFunction cleanExpired")
    void ttlMemoizedFunctionCleanExpired() throws InterruptedException {
        Memoizer.TtlMemoizedFunction<Integer, Integer> memoized =
            new Memoizer.TtlMemoizedFunction<>(x -> x * 2, Duration.ofMillis(50));

        memoized.apply(1);
        Thread.sleep(100);
        memoized.cleanExpired();
    }

    @Test
    @DisplayName("Memoizer TtlMemoizedSupplier invalidate")
    void ttlMemoizedSupplierInvalidate() {
        Memoizer.TtlMemoizedSupplier<Integer> memoized =
            new Memoizer.TtlMemoizedSupplier<>(() -> 42, Duration.ofSeconds(10));

        memoized.get();
        memoized.invalidate();
        assertEquals(Duration.ZERO, memoized.remainingTime());
    }

    @Test
    @DisplayName("Memoizer BoundedMemoizedFunction invalidate")
    void boundedMemoizedFunctionInvalidate() {
        Memoizer.BoundedMemoizedFunction<Integer, Integer> memoized =
            new Memoizer.BoundedMemoizedFunction<>(x -> x * 2, 10);

        Integer result = memoized.apply(1);
        assertEquals(2, result);
        memoized.invalidate(1);
    }

    @Test
    @DisplayName("Memoizer BoundedMemoizedFunction invalidateAll")
    void boundedMemoizedFunctionInvalidateAll() {
        Memoizer.BoundedMemoizedFunction<Integer, Integer> memoized =
            new Memoizer.BoundedMemoizedFunction<>(x -> x * 2, 10);

        memoized.apply(1);
        memoized.apply(2);
        memoized.invalidateAll();
    }

    @Test
    @DisplayName("Memoizer TtlMemoizedFunction invalidate")
    void ttlMemoizedFunctionInvalidate() {
        Memoizer.TtlMemoizedFunction<Integer, Integer> memoized =
            new Memoizer.TtlMemoizedFunction<>(x -> x * 2, Duration.ofSeconds(10));

        memoized.apply(1);
        memoized.invalidate(1);
    }

    @Test
    @DisplayName("Memoizer TtlMemoizedFunction invalidateAll")
    void ttlMemoizedFunctionInvalidateAll() {
        Memoizer.TtlMemoizedFunction<Integer, Integer> memoized =
            new Memoizer.TtlMemoizedFunction<>(x -> x * 2, Duration.ofSeconds(10));

        memoized.apply(1);
        memoized.apply(2);
        memoized.invalidateAll();
    }

    @Test
    @DisplayName("Memoizer KeyedMemoizedFunction invalidateKey")
    void keyedMemoizedFunctionInvalidateKey() {
        Memoizer.KeyedMemoizedFunction<String, Integer, Integer> memoized =
            new Memoizer.KeyedMemoizedFunction<>(String::length, String::length);

        memoized.apply("ab");
        memoized.invalidateKey(2);
        memoized.apply("ab");
    }

    @Test
    @DisplayName("Memoizer KeyedMemoizedFunction invalidateAll")
    void keyedMemoizedFunctionInvalidateAll() {
        Memoizer.KeyedMemoizedFunction<String, Integer, Integer> memoized =
            new Memoizer.KeyedMemoizedFunction<>(String::length, String::length);

        memoized.apply("ab");
        memoized.apply("abc");
        memoized.invalidateAll();
    }
}