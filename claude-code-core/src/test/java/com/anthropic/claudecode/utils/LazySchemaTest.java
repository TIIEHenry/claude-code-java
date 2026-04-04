/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LazySchema.
 */
class LazySchemaTest {

    @Test
    @DisplayName("LazySchema lazySchema creates supplier")
    void lazySchemaCreatesSupplier() {
        Supplier<String> lazy = LazySchema.lazySchema(() -> "value");

        assertNotNull(lazy);
    }

    @Test
    @DisplayName("LazySchema get returns value")
    void getReturnsValue() {
        Supplier<String> lazy = LazySchema.lazySchema(() -> "test value");

        assertEquals("test value", lazy.get());
    }

    @Test
    @DisplayName("LazySchema caches value")
    void cachesValue() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> lazy = LazySchema.lazySchema(() -> {
            counter.incrementAndGet();
            return 42;
        });

        lazy.get();
        lazy.get();
        lazy.get();

        assertEquals(1, counter.get()); // Factory called only once
    }

    @Test
    @DisplayName("LazySchema get returns same instance")
    void getReturnsSameInstance() {
        Supplier<Object> lazy = LazySchema.lazySchema(Object::new);

        Object first = lazy.get();
        Object second = lazy.get();

        assertSame(first, second);
    }

    @Test
    @DisplayName("LazySchema handles null factory result")
    void handlesNull() {
        Supplier<String> lazy = LazySchema.lazySchema(() -> null);

        assertNull(lazy.get());
        assertNull(lazy.get()); // Still null on second call
    }

    @Test
    @DisplayName("LazySchema is thread-safe")
    void threadSafe() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> lazy = LazySchema.lazySchema(() -> {
            counter.incrementAndGet();
            return 123;
        });

        // Run multiple threads
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                assertEquals(123, lazy.get());
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertEquals(1, counter.get()); // Factory called only once
    }
}