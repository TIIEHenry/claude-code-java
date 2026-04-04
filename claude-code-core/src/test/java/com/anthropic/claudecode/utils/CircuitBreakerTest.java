/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CircuitBreaker.
 */
class CircuitBreakerTest {

    @Test
    @DisplayName("CircuitBreaker creates closed")
    void createsClosed() {
        CircuitBreaker cb = new CircuitBreaker("test", 5, Duration.ofSeconds(60));

        assertEquals("test", cb.getName());
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertEquals(5, cb.getFailureThreshold());
    }

    @Test
    @DisplayName("CircuitBreaker execute runs operation")
    void executeWorks() {
        CircuitBreaker cb = new CircuitBreaker("test", 5, Duration.ofSeconds(60));

        String result = cb.execute(() -> "test");

        assertEquals("test", result);
    }

    @Test
    @DisplayName("CircuitBreaker execute runnable works")
    void executeRunnable() {
        CircuitBreaker cb = new CircuitBreaker("test", 5, Duration.ofSeconds(60));
        boolean[] executed = {false};

        cb.execute(() -> executed[0] = true);

        assertTrue(executed[0]);
    }

    @Test
    @DisplayName("CircuitBreaker tracks failures")
    void tracksFailures() {
        CircuitBreaker cb = new CircuitBreaker("test", 3, Duration.ofSeconds(60));

        for (int i = 0; i < 3; i++) {
            try {
                cb.execute(() -> { throw new RuntimeException("fail"); });
            } catch (CircuitBreaker.CircuitBreakerException e) {
                // Expected
            }
        }

        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    }

    @Test
    @DisplayName("CircuitBreaker execute throws when open")
    void throwsWhenOpen() {
        CircuitBreaker cb = new CircuitBreaker("test", 1, Duration.ofSeconds(60));

        cb.open();

        assertThrows(CircuitBreaker.CircuitBreakerException.class, () -> cb.execute(() -> "test"));
    }

    @Test
    @DisplayName("CircuitBreaker execute with fallback uses fallback when open")
    void usesFallbackWhenOpen() {
        CircuitBreaker cb = new CircuitBreaker("test", 1, Duration.ofSeconds(60));

        cb.open();

        String result = cb.execute(() -> "primary", () -> "fallback");

        assertEquals("fallback", result);
    }

    @Test
    @DisplayName("CircuitBreaker open opens circuit")
    void openWorks() {
        CircuitBreaker cb = new CircuitBreaker("test", 5, Duration.ofSeconds(60));

        cb.open();

        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    }

    @Test
    @DisplayName("CircuitBreaker close closes circuit")
    void closeWorks() {
        CircuitBreaker cb = new CircuitBreaker("test", 5, Duration.ofSeconds(60));

        cb.open();
        cb.close();

        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
    }

    @Test
    @DisplayName("CircuitBreaker halfOpen half-opens circuit")
    void halfOpenWorks() {
        CircuitBreaker cb = new CircuitBreaker("test", 5, Duration.ofSeconds(60));

        cb.open();
        cb.halfOpen();

        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());
    }

    @Test
    @DisplayName("CircuitBreaker isHealthy checks closed")
    void isHealthy() {
        CircuitBreaker cb = new CircuitBreaker("test", 5, Duration.ofSeconds(60));

        assertTrue(cb.isHealthy());

        cb.open();

        assertFalse(cb.isHealthy());
    }

    @Test
    @DisplayName("CircuitBreaker isOpen checks open")
    void isOpen() {
        CircuitBreaker cb = new CircuitBreaker("test", 5, Duration.ofSeconds(60));

        assertFalse(cb.isOpen());

        cb.open();

        assertTrue(cb.isOpen());
    }

    @Test
    @DisplayName("CircuitBreaker isHalfOpen checks half-open")
    void isHalfOpen() {
        CircuitBreaker cb = new CircuitBreaker("test", 5, Duration.ofSeconds(60));

        cb.open();
        cb.halfOpen();

        assertTrue(cb.isHalfOpen());
    }

    @Test
    @DisplayName("CircuitBreaker getStats returns statistics")
    void getStatsWorks() {
        CircuitBreaker cb = new CircuitBreaker("test", 5, Duration.ofSeconds(60));

        CircuitBreaker.CircuitBreakerStats stats = cb.getStats();

        assertEquals("test", stats.name());
        assertEquals(CircuitBreaker.State.CLOSED, stats.state());
    }

    @Test
    @DisplayName("CircuitBreaker CircuitBreakerStats format returns string")
    void statsFormat() {
        CircuitBreaker.CircuitBreakerStats stats = new CircuitBreaker.CircuitBreakerStats(
            "test", CircuitBreaker.State.CLOSED, 2, 0, 5, Duration.ofSeconds(10), Duration.ofSeconds(5)
        );

        String formatted = stats.format();

        assertTrue(formatted.contains("test"));
        assertTrue(formatted.contains("CLOSED"));
    }

    @Test
    @DisplayName("CircuitBreaker toString shows info")
    void toStringWorks() {
        CircuitBreaker cb = new CircuitBreaker("test", 5, Duration.ofSeconds(60));

        String str = cb.toString();

        assertTrue(str.contains("test"));
        assertTrue(str.contains("CLOSED"));
    }

    @Test
    @DisplayName("CircuitBreaker Builder creates circuit breaker")
    void builderWorks() {
        CircuitBreaker cb = CircuitBreaker.builder()
            .name("test")
            .failureThreshold(3)
            .resetTimeout(Duration.ofSeconds(30))
            .build();

        assertEquals("test", cb.getName());
        assertEquals(3, cb.getFailureThreshold());
    }

    @Test
    @DisplayName("CircuitBreaker Builder resetTimeout with seconds")
    void builderResetTimeoutSeconds() {
        CircuitBreaker cb = CircuitBreaker.builder()
            .name("test")
            .resetTimeout(30)
            .build();

        assertEquals(Duration.ofSeconds(30), cb.getResetTimeout());
    }

    @Test
    @DisplayName("CircuitBreaker CircuitBreakerRegistry getOrCreate creates")
    void registryGetOrCreate() {
        CircuitBreaker.CircuitBreakerRegistry registry = new CircuitBreaker.CircuitBreakerRegistry();

        CircuitBreaker cb = registry.getOrCreate("test", 5, Duration.ofSeconds(60));

        assertEquals("test", cb.getName());
    }

    @Test
    @DisplayName("CircuitBreaker CircuitBreakerRegistry get returns existing")
    void registryGet() {
        CircuitBreaker.CircuitBreakerRegistry registry = new CircuitBreaker.CircuitBreakerRegistry();

        registry.getOrCreate("test", 5, Duration.ofSeconds(60));
        Optional<CircuitBreaker> found = registry.get("test");

        assertTrue(found.isPresent());
    }

    @Test
    @DisplayName("CircuitBreaker CircuitBreakerRegistry get returns empty for missing")
    void registryGetMissing() {
        CircuitBreaker.CircuitBreakerRegistry registry = new CircuitBreaker.CircuitBreakerRegistry();

        Optional<CircuitBreaker> found = registry.get("missing");

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("CircuitBreaker CircuitBreakerRegistry remove removes")
    void registryRemove() {
        CircuitBreaker.CircuitBreakerRegistry registry = new CircuitBreaker.CircuitBreakerRegistry();

        registry.getOrCreate("test", 5, Duration.ofSeconds(60));
        registry.remove("test");

        assertFalse(registry.get("test").isPresent());
    }

    @Test
    @DisplayName("CircuitBreaker CircuitBreakerException has name")
    void circuitBreakerException() {
        CircuitBreaker.CircuitBreakerException ex =
            new CircuitBreaker.CircuitBreakerException("test", "message");

        assertEquals("test", ex.getCircuitBreakerName());
        assertEquals("message", ex.getMessage());
    }

    @Test
    @DisplayName("CircuitBreaker execute with fallback uses fallback on failure")
    void usesFallbackOnFailure() {
        CircuitBreaker cb = new CircuitBreaker("test", 5, Duration.ofSeconds(60));

        String result = cb.execute(
            () -> { throw new RuntimeException("fail"); },
            () -> "fallback"
        );

        assertEquals("fallback", result);
    }

    @Test
    @DisplayName("CircuitBreaker getFailureCount returns count")
    void getFailureCount() {
        CircuitBreaker cb = new CircuitBreaker("test", 5, Duration.ofSeconds(60));

        try {
            cb.execute(() -> { throw new RuntimeException("fail"); });
        } catch (CircuitBreaker.CircuitBreakerException e) {
            // Expected
        }

        assertEquals(1, cb.getFailureCount());
    }
}