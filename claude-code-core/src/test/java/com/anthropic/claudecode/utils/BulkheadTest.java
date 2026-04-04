/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Bulkhead.
 */
class BulkheadTest {

    @Test
    @DisplayName("Bulkhead creates with max concurrent calls")
    void createsWithMax() {
        Bulkhead bulkhead = new Bulkhead("test", 5);

        assertEquals("test", bulkhead.getName());
        assertEquals(5, bulkhead.getMaxConcurrentCalls());
        assertEquals(5, bulkhead.getAvailablePermits());
    }

    @Test
    @DisplayName("Bulkhead execute runs operation")
    void executeWorks() {
        Bulkhead bulkhead = new Bulkhead("test", 5);
        AtomicInteger counter = new AtomicInteger(0);

        bulkhead.execute(() -> counter.incrementAndGet());

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("Bulkhead execute returns value")
    void executeReturnsValue() {
        Bulkhead bulkhead = new Bulkhead("test", 5);

        String result = bulkhead.execute(() -> "test");

        assertEquals("test", result);
    }

    @Test
    @DisplayName("Bulkhead execute runnable works")
    void executeRunnable() {
        Bulkhead bulkhead = new Bulkhead("test", 5);
        AtomicInteger counter = new AtomicInteger(0);

        bulkhead.execute(() -> counter.incrementAndGet());

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("Bulkhead tryAcquire acquires permit")
    void tryAcquireWorks() {
        Bulkhead bulkhead = new Bulkhead("test", 2);

        assertTrue(bulkhead.tryAcquire());
        assertEquals(1, bulkhead.getAvailablePermits());
    }

    @Test
    @DisplayName("Bulkhead tryAcquire fails when full")
    void tryAcquireFailsWhenFull() {
        Bulkhead bulkhead = new Bulkhead("test", 1);

        assertTrue(bulkhead.tryAcquire());
        assertFalse(bulkhead.tryAcquire());
    }

    @Test
    @DisplayName("Bulkhead release returns permit")
    void releaseWorks() {
        Bulkhead bulkhead = new Bulkhead("test", 1);

        bulkhead.tryAcquire();
        bulkhead.release();

        assertEquals(1, bulkhead.getAvailablePermits());
    }

    @Test
    @DisplayName("Bulkhead isFull checks capacity")
    void isFullWorks() {
        Bulkhead bulkhead = new Bulkhead("test", 1);

        assertFalse(bulkhead.isFull());
        bulkhead.tryAcquire();
        assertTrue(bulkhead.isFull());
    }

    @Test
    @DisplayName("Bulkhead getCurrentConcurrentCalls returns count")
    void getCurrentConcurrentCalls() {
        Bulkhead bulkhead = new Bulkhead("test", 3);

        assertEquals(0, bulkhead.getCurrentConcurrentCalls());

        bulkhead.tryAcquire();
        assertEquals(1, bulkhead.getCurrentConcurrentCalls());

        bulkhead.tryAcquire();
        assertEquals(2, bulkhead.getCurrentConcurrentCalls());
    }

    @Test
    @DisplayName("Bulkhead getStats returns statistics")
    void getStatsWorks() {
        Bulkhead bulkhead = new Bulkhead("test", 5);

        bulkhead.tryAcquire();

        Bulkhead.BulkheadStats stats = bulkhead.getStats();

        assertEquals("test", stats.name());
        assertEquals(5, stats.maxConcurrentCalls());
        assertEquals(1, stats.currentConcurrentCalls());
        assertEquals(4, stats.availablePermits());
    }

    @Test
    @DisplayName("Bulkhead BulkheadStats utilization calculates")
    void statsUtilization() {
        Bulkhead.BulkheadStats stats = new Bulkhead.BulkheadStats("test", 10, 5, 5, 0);

        assertEquals(0.5, stats.utilization());
    }

    @Test
    @DisplayName("Bulkhead BulkheadStats format returns string")
    void statsFormat() {
        Bulkhead.BulkheadStats stats = new Bulkhead.BulkheadStats("test", 10, 5, 5, 2);

        String formatted = stats.format();

        assertTrue(formatted.contains("test"));
        assertTrue(formatted.contains("5/10"));
    }

    @Test
    @DisplayName("Bulkhead toString shows info")
    void toStringWorks() {
        Bulkhead bulkhead = new Bulkhead("test", 5);

        String str = bulkhead.toString();

        assertTrue(str.contains("test"));
        assertTrue(str.contains("max=5"));
    }

    @Test
    @DisplayName("Bulkhead Builder creates bulkhead")
    void builderWorks() {
        Bulkhead bulkhead = Bulkhead.builder()
            .name("test")
            .maxConcurrentCalls(3)
            .build();

        assertEquals("test", bulkhead.getName());
        assertEquals(3, bulkhead.getMaxConcurrentCalls());
    }

    @Test
    @DisplayName("Bulkhead BulkheadRegistry getOrCreate creates")
    void registryGetOrCreate() {
        Bulkhead.BulkheadRegistry registry = new Bulkhead.BulkheadRegistry();

        Bulkhead bulkhead = registry.getOrCreate("test", 5);

        assertEquals("test", bulkhead.getName());
    }

    @Test
    @DisplayName("Bulkhead BulkheadRegistry get returns existing")
    void registryGet() {
        Bulkhead.BulkheadRegistry registry = new Bulkhead.BulkheadRegistry();

        registry.getOrCreate("test", 5);
        Optional<Bulkhead> found = registry.get("test");

        assertTrue(found.isPresent());
    }

    @Test
    @DisplayName("Bulkhead BulkheadRegistry get returns empty for missing")
    void registryGetMissing() {
        Bulkhead.BulkheadRegistry registry = new Bulkhead.BulkheadRegistry();

        Optional<Bulkhead> found = registry.get("missing");

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Bulkhead BulkheadRegistry remove removes")
    void registryRemove() {
        Bulkhead.BulkheadRegistry registry = new Bulkhead.BulkheadRegistry();

        registry.getOrCreate("test", 5);
        registry.remove("test");

        assertFalse(registry.get("test").isPresent());
    }

    @Test
    @DisplayName("Bulkhead BulkheadRegistry clear clears all")
    void registryClear() {
        Bulkhead.BulkheadRegistry registry = new Bulkhead.BulkheadRegistry();

        registry.getOrCreate("test1", 5);
        registry.getOrCreate("test2", 5);
        registry.clear();

        assertTrue(registry.getAllStats().isEmpty());
    }

    @Test
    @DisplayName("Bulkhead BulkheadException has name")
    void bulkheadException() {
        Bulkhead.BulkheadException ex = new Bulkhead.BulkheadException("test", "message");

        assertEquals("test", ex.getBulkheadName());
        assertEquals("message", ex.getMessage());
    }

    @Test
    @DisplayName("Bulkhead execute throws when full")
    void executeThrowsWhenFull() {
        Bulkhead bulkhead = new Bulkhead("test", 1);

        bulkhead.tryAcquire(); // Use the only permit

        assertThrows(Bulkhead.BulkheadException.class, () -> bulkhead.execute(() -> "test"));
    }

    @Test
    @DisplayName("Bulkhead execute with timeout works")
    void executeWithTimeout() {
        Bulkhead bulkhead = new Bulkhead("test", 1);

        String result = bulkhead.execute(() -> "test", Duration.ofSeconds(1));

        assertEquals("test", result);
    }
}