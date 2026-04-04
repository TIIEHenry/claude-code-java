/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Pool.
 */
class PoolTest {

    @Test
    @DisplayName("Pool builder creates pool")
    void builderCreatesPool() {
        Pool<String> pool = Pool.<String>builder()
            .factory(() -> "test")
            .maxSize(5)
            .build();

        assertNotNull(pool);
    }

    @Test
    @DisplayName("Pool builder requires factory")
    void builderRequiresFactory() {
        assertThrows(IllegalStateException.class, () ->
            Pool.builder().maxSize(5).build()
        );
    }

    @Test
    @DisplayName("Pool borrow creates new object")
    void borrowCreatesNew() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Pool<Integer> pool = new Pool<>(counter::incrementAndGet, 5);

        Integer obj = pool.borrow();

        assertEquals(1, obj);
    }

    @Test
    @DisplayName("Pool borrow returns released object")
    void borrowReturnsReleased() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Pool<Integer> pool = new Pool<>(counter::incrementAndGet, 5);

        Integer obj1 = pool.borrow();
        pool.release(obj1);

        Integer obj2 = pool.borrow();

        assertEquals(1, counter.get()); // No new object created
    }

    @Test
    @DisplayName("Pool tryBorrow returns object")
    void tryBorrow() {
        AtomicInteger counter = new AtomicInteger(0);
        Pool<Integer> pool = new Pool<>(counter::incrementAndGet, 5);

        var result = pool.tryBorrow();

        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    @DisplayName("Pool release returns object to pool")
    void release() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Pool<Integer> pool = new Pool<>(counter::incrementAndGet, 5);

        Integer obj = pool.borrow();
        pool.release(obj);

        assertEquals(0, pool.getStats().borrowed());
    }

    @Test
    @DisplayName("Pool withBorrow executes function")
    void withBorrow() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Pool<Integer> pool = new Pool<>(counter::incrementAndGet, 5);

        Integer result = pool.withBorrow(obj -> obj * 2);

        assertEquals(2, result);
    }

    @Test
    @DisplayName("Pool getStats returns statistics")
    void getStats() throws Exception {
        Pool<String> pool = new Pool<>(() -> "test", 5);

        pool.borrow();

        Pool.PoolStats stats = pool.getStats();

        assertEquals(5, stats.maxSize());
        assertEquals(1, stats.created());
        assertEquals(1, stats.borrowed());
    }

    @Test
    @DisplayName("Pool PoolStats utilization")
    void poolStatsUtilization() {
        Pool.PoolStats stats = new Pool.PoolStats(10, 5, 3, 2, 5);

        assertEquals(0.3, stats.utilization(), 0.01);
    }

    @Test
    @DisplayName("Pool clear removes all objects")
    void clear() throws Exception {
        Pool<String> pool = new Pool<>(() -> "test", 5);

        pool.borrow();
        pool.borrow();
        pool.clear();

        assertEquals(0, pool.getStats().available());
    }

    @Test
    @DisplayName("Pool warmUp pre-populates pool")
    void warmUp() {
        AtomicInteger counter = new AtomicInteger(0);
        Pool<Integer> pool = new Pool<>(counter::incrementAndGet, 5);

        pool.warmUp(3);

        assertEquals(3, pool.getStats().available());
    }

    @Test
    @DisplayName("Pool borrow with timeout")
    void borrowWithTimeout() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Pool<Integer> pool = new Pool<>(counter::incrementAndGet, 5);

        Integer obj = pool.borrow(100, TimeUnit.MILLISECONDS);

        assertNotNull(obj);
    }

    @Test
    @DisplayName("Pool resetter is called on release")
    void resetterCalled() throws Exception {
        AtomicInteger resetCounter = new AtomicInteger(0);
        Pool<StringBuilder> pool = new Pool<>(
            StringBuilder::new,
            sb -> { resetCounter.incrementAndGet(); sb.setLength(0); },
            null,
            5
        );

        StringBuilder sb = pool.borrow();
        sb.append("test");
        pool.release(sb);

        assertEquals(1, resetCounter.get());
    }

    @Test
    @DisplayName("Pool destroyer is called when reset fails")
    void destroyerCalledOnResetFail() throws Exception {
        AtomicInteger destroyCounter = new AtomicInteger(0);
        Pool<String> pool = new Pool<>(
            () -> "test",
            s -> { throw new RuntimeException("reset failed"); },
            s -> destroyCounter.incrementAndGet(),
            5
        );

        String obj = pool.borrow();
        pool.release(obj);

        assertEquals(1, destroyCounter.get());
    }

    @Test
    @DisplayName("Pool maxSize limits created objects")
    void maxSizeLimits() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Pool<Integer> pool = new Pool<>(counter::incrementAndGet, 2);

        pool.borrow();
        pool.borrow();

        assertEquals(2, counter.get());
    }
}