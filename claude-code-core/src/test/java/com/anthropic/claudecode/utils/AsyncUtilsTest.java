/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AsyncUtils.
 */
class AsyncUtilsTest {

    @Test
    @DisplayName("sleep does not throw")
    void sleepWorks() {
        // Very short sleep should complete without exception
        assertDoesNotThrow(() -> AsyncUtils.sleep(1));
    }

    @Test
    @DisplayName("retryWithBackoff succeeds on first try")
    void retrySucceedsFirstTry() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);

        String result = AsyncUtils.retryWithBackoff(() -> {
            calls.incrementAndGet();
            return "success";
        }, 3, 10);

        assertEquals("success", result);
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("retryWithBackoff retries on failure")
    void retryRetriesOnFailure() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);

        String result = AsyncUtils.retryWithBackoff(() -> {
            int count = calls.incrementAndGet();
            if (count < 3) {
                throw new RuntimeException("Failed");
            }
            return "success";
        }, 5, 10);

        assertEquals("success", result);
        assertEquals(3, calls.get());
    }

    @Test
    @DisplayName("retryWithBackoff throws after max retries")
    void retryThrowsAfterMaxRetries() {
        AtomicInteger calls = new AtomicInteger(0);

        Exception exception = assertThrows(Exception.class, () -> {
            AsyncUtils.retryWithBackoff(() -> {
                calls.incrementAndGet();
                throw new RuntimeException("Always fails");
            }, 2, 10);
        });

        assertTrue(exception.getMessage().contains("Always fails"));
        assertEquals(3, calls.get()); // initial + 2 retries
    }

    @Test
    @DisplayName("withTimeout returns result for fast operation")
    void withTimeoutFastOperation() {
        Optional<String> result = AsyncUtils.withTimeout(
            () -> "done",
            1000
        );

        assertTrue(result.isPresent());
        assertEquals("done", result.get());
    }

    @Test
    @DisplayName("withTimeout returns empty for timeout")
    void withTimeoutTimeout() {
        Optional<String> result = AsyncUtils.withTimeout(
            () -> {
                Thread.sleep(500);
                return "done";
            },
            10
        );

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("debounce returns a runnable")
    void debounceReturnsRunnable() {
        Runnable debounced = AsyncUtils.debounce(() -> {}, 100);
        assertNotNull(debounced);
    }
}