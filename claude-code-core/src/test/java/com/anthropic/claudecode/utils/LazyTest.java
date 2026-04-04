/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Lazy.
 */
class LazyTest {

    @Test
    @DisplayName("Lazy of creates lazy value")
    void ofCreates() {
        AtomicInteger calls = new AtomicInteger(0);
        Lazy<Integer> lazy = Lazy.of(() -> {
            calls.incrementAndGet();
            return 42;
        });

        assertFalse(lazy.isEvaluated());
        assertEquals(42, lazy.get());
        assertTrue(lazy.isEvaluated());
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("Lazy get caches value")
    void getCaches() {
        AtomicInteger calls = new AtomicInteger(0);
        Lazy<Integer> lazy = Lazy.of(() -> {
            calls.incrementAndGet();
            return 42;
        });

        assertEquals(42, lazy.get());
        assertEquals(42, lazy.get());
        assertEquals(42, lazy.get());
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("Lazy isEvaluated returns false initially")
    void isEvaluatedFalse() {
        Lazy<Integer> lazy = Lazy.of(() -> 42);

        assertFalse(lazy.isEvaluated());
    }

    @Test
    @DisplayName("Lazy isEvaluated returns true after get")
    void isEvaluatedTrue() {
        Lazy<Integer> lazy = Lazy.of(() -> 42);

        lazy.get();
        assertTrue(lazy.isEvaluated());
    }

    @Test
    @DisplayName("Lazy reset clears cached value")
    void resetClears() {
        AtomicInteger calls = new AtomicInteger(0);
        Lazy<Integer> lazy = Lazy.of(() -> {
            calls.incrementAndGet();
            return 42;
        });

        assertEquals(42, lazy.get());
        assertEquals(1, calls.get());

        lazy.reset();
        assertFalse(lazy.isEvaluated());

        assertEquals(42, lazy.get());
        assertEquals(2, calls.get());
    }

    @Test
    @DisplayName("Lazy ofValue creates already evaluated")
    void ofValueCreates() {
        Lazy<String> lazy = Lazy.ofValue("test");

        assertTrue(lazy.isEvaluated());
        assertEquals("test", lazy.get());
    }

    @Test
    @DisplayName("Lazy handles null value")
    void handlesNull() {
        Lazy<String> lazy = Lazy.of(() -> null);

        assertNull(lazy.get());
        assertTrue(lazy.isEvaluated());
    }

    @Test
    @DisplayName("Lazy is thread-safe")
    void threadSafe() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);
        Lazy<Integer> lazy = Lazy.of(() -> {
            calls.incrementAndGet();
            return 42;
        });

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                assertEquals(42, lazy.get());
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertEquals(1, calls.get());
    }
}