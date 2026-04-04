/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RetryPolicy.
 */
class RetryPolicyTest {

    @Test
    @DisplayName("RetryPolicy builder creates policy")
    void builderCreatesPolicy() {
        RetryPolicy policy = RetryPolicy.builder()
            .maxAttempts(3)
            .initialDelay(Duration.ofMillis(100))
            .build();

        assertEquals(3, policy.getMaxAttempts());
        assertEquals(Duration.ofMillis(100), policy.getInitialDelay());
    }

    @Test
    @DisplayName("RetryPolicy execute succeeds on first try")
    void executeSucceedsFirstTry() {
        RetryPolicy policy = new RetryPolicy(3);
        AtomicInteger attempts = new AtomicInteger(0);

        String result = policy.execute(() -> {
            attempts.incrementAndGet();
            return "success";
        });

        assertEquals("success", result);
        assertEquals(1, attempts.get());
    }

    @Test
    @DisplayName("RetryPolicy execute retries on failure")
    void executeRetriesOnFailure() {
        RetryPolicy policy = RetryPolicy.builder()
            .maxAttempts(3)
            .initialDelay(Duration.ZERO)
            .build();
        AtomicInteger attempts = new AtomicInteger(0);

        String result = policy.execute(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException("fail");
            }
            return "success";
        });

        assertEquals("success", result);
        assertEquals(3, attempts.get());
    }

    @Test
    @DisplayName("RetryPolicy execute throws after max attempts")
    void executeThrowsAfterMaxAttempts() {
        RetryPolicy policy = RetryPolicy.builder()
            .maxAttempts(2)
            .initialDelay(Duration.ZERO)
            .build();

        assertThrows(RetryPolicy.RetryExhaustedException.class, () ->
            policy.execute(() -> {
                throw new RuntimeException("always fail");
            })
        );
    }

    @Test
    @DisplayName("RetryPolicy execute with fallback")
    void executeWithFallback() {
        RetryPolicy policy = RetryPolicy.builder()
            .maxAttempts(2)
            .initialDelay(Duration.ZERO)
            .build();

        String result = policy.execute(
            () -> { throw new RuntimeException("fail"); },
            () -> "fallback"
        );

        assertEquals("fallback", result);
    }

    @Test
    @DisplayName("RetryPolicy execute runnable")
    void executeRunnable() {
        RetryPolicy policy = new RetryPolicy(1);
        AtomicInteger counter = new AtomicInteger(0);

        policy.execute(() -> counter.incrementAndGet());

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("RetryPolicy executeForResult returns result")
    void executeForResultSuccess() {
        RetryPolicy policy = new RetryPolicy(3);

        RetryPolicy.RetryResult<String> result = policy.executeForResult(() -> "success");

        assertTrue(result.succeeded());
        assertEquals("success", result.value());
        assertEquals(1, result.attempts());
    }

    @Test
    @DisplayName("RetryPolicy executeForResult returns failure")
    void executeForResultFailure() {
        RetryPolicy policy = RetryPolicy.builder()
            .maxAttempts(2)
            .initialDelay(Duration.ZERO)
            .build();

        RetryPolicy.RetryResult<String> result = policy.executeForResult(() -> {
            throw new RuntimeException("fail");
        });

        assertTrue(result.failed());
        assertNull(result.value());
        assertNotNull(result.lastError());
    }

    @Test
    @DisplayName("RetryPolicy retryOn filters exceptions")
    void retryOnFilters() {
        AtomicInteger attempts = new AtomicInteger(0);
        RetryPolicy policy = RetryPolicy.builder()
            .maxAttempts(3)
            .initialDelay(Duration.ZERO)
            .retryOn(IllegalArgumentException.class)
            .build();

        assertThrows(RetryPolicy.RetryExhaustedException.class, () ->
            policy.execute(() -> {
                attempts.incrementAndGet();
                throw new IllegalStateException("not retryable");
            })
        );

        assertEquals(1, attempts.get());
    }

    @Test
    @DisplayName("RetryPolicy retryPredicate filters")
    void retryPredicateFilters() {
        AtomicInteger attempts = new AtomicInteger(0);
        RetryPolicy policy = RetryPolicy.builder()
            .maxAttempts(3)
            .initialDelay(Duration.ZERO)
            .retryPredicate(e -> e.getMessage() != null && e.getMessage().contains("retry"))
            .build();

        // Exception message "no retry" doesn't contain "retry", so it should not retry
        // But wait - "no retry" DOES contain "retry"!
        // Let's use a message that clearly doesn't match
        assertThrows(RetryPolicy.RetryExhaustedException.class, () ->
            policy.execute(() -> {
                attempts.incrementAndGet();
                throw new RuntimeException("failed");
            })
        );

        assertEquals(1, attempts.get());
    }

    @Test
    @DisplayName("RetryPolicy onRetry callback called")
    void onRetryCallback() {
        AtomicInteger retryCount = new AtomicInteger(0);
        RetryPolicy policy = RetryPolicy.builder()
            .maxAttempts(3)
            .initialDelay(Duration.ZERO)
            .onRetry(e -> retryCount.incrementAndGet())
            .build();

        policy.execute(() -> {
            if (retryCount.get() < 2) throw new RuntimeException("fail");
            return "ok";
        });

        assertEquals(2, retryCount.get());
    }

    @Test
    @DisplayName("RetryPolicy exponentialBackoff")
    void exponentialBackoff() {
        RetryPolicy.Builder builder = RetryPolicy.builder()
            .exponentialBackoff();

        // Just verify it builds
        assertNotNull(builder.build());
    }

    @Test
    @DisplayName("RetryPolicy linearBackoff")
    void linearBackoff() {
        RetryPolicy.Builder builder = RetryPolicy.builder()
            .linearBackoff();

        assertNotNull(builder.build());
    }

    @Test
    @DisplayName("RetryPolicy noBackoff")
    void noBackoff() {
        RetryPolicy policy = RetryPolicy.builder()
            .noBackoff()
            .build();

        assertEquals(Duration.ZERO, policy.getInitialDelay());
    }

    @Test
    @DisplayName("RetryPolicy retryOnTimeout")
    void retryOnTimeout() {
        RetryPolicy.Builder builder = RetryPolicy.builder()
            .retryOnTimeout();

        assertNotNull(builder.build());
    }

    @Test
    @DisplayName("RetryPolicy retryOnConnectionError")
    void retryOnConnectionError() {
        RetryPolicy.Builder builder = RetryPolicy.builder()
            .retryOnConnectionError();

        assertNotNull(builder.build());
    }

    @Test
    @DisplayName("RetryPolicy RetryResult record")
    void retryResultRecord() {
        RetryPolicy.RetryResult<String> result = new RetryPolicy.RetryResult<>(
            "value", 2, true, null, Duration.ofMillis(100)
        );

        assertEquals("value", result.value());
        assertEquals(2, result.attempts());
        assertTrue(result.succeeded());
        assertFalse(result.failed());
        assertTrue(result.getValue().isPresent());
        assertTrue(result.getError().isEmpty());
    }

    @Test
    @DisplayName("RetryPolicy RetryExhaustedException")
    void retryExhaustedException() {
        Exception cause = new RuntimeException("cause");
        RetryPolicy.RetryExhaustedException ex =
            new RetryPolicy.RetryExhaustedException("message", cause, 3);

        assertEquals("message", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertEquals(3, ex.getAttempts());
    }

    @Test
    @DisplayName("RetryPolicy RetryUtils retry")
    void retryUtilsRetry() {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = RetryPolicy.RetryUtils.retry(3, () -> {
            attempts.incrementAndGet();
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(1, attempts.get());
    }

    @Test
    @DisplayName("RetryPolicy RetryUtils retryWithBackoff")
    void retryUtilsRetryWithBackoff() {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = RetryPolicy.RetryUtils.retryWithBackoff(
            3,
            Duration.ZERO,
            () -> {
                int a = attempts.incrementAndGet();
                if (a < 2) throw new RuntimeException("fail");
                return "ok";
            }
        );

        assertEquals("ok", result);
    }

    @Test
    @DisplayName("RetryPolicy RetryUtils retryUntil")
    void retryUtilsRetryUntil() {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = RetryPolicy.RetryUtils.retryUntil(
            5,
            () -> {
                int a = attempts.incrementAndGet();
                return "attempt" + a;
            },
            s -> s.equals("attempt3")
        );

        assertEquals("attempt3", result);
    }

    @Test
    @DisplayName("RetryPolicy getMaxAttempts")
    void getMaxAttempts() {
        RetryPolicy policy = new RetryPolicy(5);
        assertEquals(5, policy.getMaxAttempts());
    }

    @Test
    @DisplayName("RetryPolicy getInitialDelay")
    void getInitialDelay() {
        RetryPolicy policy = RetryPolicy.builder()
            .initialDelay(Duration.ofMillis(200))
            .build();
        assertEquals(Duration.ofMillis(200), policy.getInitialDelay());
    }

    @Test
    @DisplayName("RetryPolicy getMaxDelay")
    void getMaxDelay() {
        RetryPolicy policy = RetryPolicy.builder()
            .maxDelay(Duration.ofSeconds(10))
            .build();
        assertEquals(Duration.ofSeconds(10), policy.getMaxDelay());
    }
}