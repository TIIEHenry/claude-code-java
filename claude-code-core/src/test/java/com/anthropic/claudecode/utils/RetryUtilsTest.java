/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RetryUtils.
 */
class RetryUtilsTest {

    @Test
    @DisplayName("RetryUtils RetryPolicy defaults creates default policy")
    void retryPolicyDefaults() {
        RetryUtils.RetryPolicy policy = RetryUtils.RetryPolicy.defaults();

        assertEquals(3, policy.maxAttempts());
        assertEquals(1000, policy.initialDelayMs());
        assertEquals(2.0, policy.backoffMultiplier());
    }

    @Test
    @DisplayName("RetryUtils RetryPolicy of creates with max attempts")
    void retryPolicyOf() {
        RetryUtils.RetryPolicy policy = RetryUtils.RetryPolicy.of(5);

        assertEquals(5, policy.maxAttempts());
    }

    @Test
    @DisplayName("RetryUtils RetryPolicy exponentialBackoff creates")
    void retryPolicyExponential() {
        RetryUtils.RetryPolicy policy = RetryUtils.RetryPolicy.exponentialBackoff(3, 500);

        assertEquals(3, policy.maxAttempts());
        assertEquals(500, policy.initialDelayMs());
        assertEquals(2.0, policy.backoffMultiplier());
    }

    @Test
    @DisplayName("RetryUtils RetryPolicy linearBackoff creates")
    void retryPolicyLinear() {
        RetryUtils.RetryPolicy policy = RetryUtils.RetryPolicy.linearBackoff(3, 1000);

        assertEquals(3, policy.maxAttempts());
        assertEquals(1000, policy.initialDelayMs());
        assertEquals(1.0, policy.backoffMultiplier());
    }

    @Test
    @DisplayName("RetryUtils RetryPolicy immediate creates")
    void retryPolicyImmediate() {
        RetryUtils.RetryPolicy policy = RetryUtils.RetryPolicy.immediate(5);

        assertEquals(5, policy.maxAttempts());
        assertEquals(0, policy.initialDelayMs());
    }

    @Test
    @DisplayName("RetryUtils RetryPolicy getDelayForAttempt calculates exponential")
    void retryPolicyGetDelay() {
        RetryUtils.RetryPolicy policy = RetryUtils.RetryPolicy.exponentialBackoff(3, 1000);

        assertEquals(1000, policy.getDelayForAttempt(1)); // 1000 * 2^0
        assertEquals(2000, policy.getDelayForAttempt(2)); // 1000 * 2^1
        assertEquals(4000, policy.getDelayForAttempt(3)); // 1000 * 2^2
    }

    @Test
    @DisplayName("RetryUtils RetryPolicy withMaxDelay limits delay")
    void retryPolicyWithMaxDelay() {
        RetryUtils.RetryPolicy policy = RetryUtils.RetryPolicy.defaults().withMaxDelay(2000);

        assertEquals(2000, policy.maxDelayMs());
    }

    @Test
    @DisplayName("RetryUtils executeWithRetry succeeds on first attempt")
    void executeWithRetrySuccess() {
        AtomicInteger calls = new AtomicInteger(0);

        RetryUtils.RetryResult<String> result = RetryUtils.executeWithRetry(
            () -> {
                calls.incrementAndGet();
                return "success";
            },
            RetryUtils.RetryPolicy.immediate(3)
        );

        assertTrue(result.succeeded());
        assertEquals("success", result.result());
        assertEquals(1, calls.get());
        assertNull(result.lastError());
    }

    @Test
    @DisplayName("RetryUtils executeWithRetry retries on failure")
    void executeWithRetryRetries() {
        AtomicInteger calls = new AtomicInteger(0);

        RetryUtils.RetryResult<String> result = RetryUtils.executeWithRetry(
            () -> {
                int count = calls.incrementAndGet();
                if (count < 3) {
                    throw new RuntimeException("Fail");
                }
                return "success";
            },
            RetryUtils.RetryPolicy.immediate(5)
        );

        assertTrue(result.succeeded());
        assertEquals("success", result.result());
        assertEquals(3, calls.get());
    }

    @Test
    @DisplayName("RetryUtils executeWithRetry fails after max attempts")
    void executeWithRetryFailsAfterMax() {
        AtomicInteger calls = new AtomicInteger(0);

        RetryUtils.RetryResult<String> result = RetryUtils.executeWithRetry(
            () -> {
                calls.incrementAndGet();
                throw new RuntimeException("Always fails");
            },
            RetryUtils.RetryPolicy.immediate(2)
        );

        assertFalse(result.succeeded());
        assertNull(result.result());
        assertEquals(2, calls.get());
        assertNotNull(result.lastError());
    }

    @Test
    @DisplayName("RetryUtils retryUntilSuccess throws on failure")
    void retryUntilSuccessThrows() {
        AtomicInteger calls = new AtomicInteger(0);

        assertThrows(RuntimeException.class, () ->
            RetryUtils.retryUntilSuccess(
                () -> {
                    calls.incrementAndGet();
                    throw new RuntimeException("Fail");
                },
                2,
                0
            )
        );

        assertEquals(2, calls.get());
    }

    @Test
    @DisplayName("RetryUtils retryUntilSuccess returns on success")
    void retryUntilSuccessReturns() {
        AtomicInteger calls = new AtomicInteger(0);

        String result = RetryUtils.retryUntilSuccess(
            () -> {
                int count = calls.incrementAndGet();
                if (count < 2) {
                    throw new RuntimeException("Fail");
                }
                return "success";
            },
            3,
            0
        );

        assertEquals("success", result);
        assertEquals(2, calls.get());
    }

    @Test
    @DisplayName("RetryUtils policyBuilder creates custom policy")
    void policyBuilderCreates() {
        RetryUtils.RetryPolicy policy = RetryUtils.policyBuilder()
            .maxAttempts(5)
            .initialDelayMs(500)
            .backoffMultiplier(1.5)
            .maxDelayMs(10000)
            .build();

        assertEquals(5, policy.maxAttempts());
        assertEquals(500, policy.initialDelayMs());
        assertEquals(1.5, policy.backoffMultiplier());
        assertEquals(10000, policy.maxDelayMs());
    }

    @Test
    @DisplayName("RetryUtils RetryResult record works")
    void retryResultRecord() {
        RetryUtils.RetryResult<String> success = new RetryUtils.RetryResult<>("data", null, 1, true);

        assertEquals("data", success.result());
        assertNull(success.lastError());
        assertEquals(1, success.attempts());
        assertTrue(success.succeeded());
    }

    @Test
    @DisplayName("RetryUtils CircuitBreaker creates")
    void circuitBreakerCreates() {
        RetryUtils.CircuitBreaker breaker = new RetryUtils.CircuitBreaker(3, 5000);

        assertEquals(RetryUtils.CircuitState.CLOSED, breaker.getState());
    }

    @Test
    @DisplayName("RetryUtils CircuitBreaker executes successfully")
    void circuitBreakerExecutesSuccess() throws Exception {
        RetryUtils.CircuitBreaker breaker = new RetryUtils.CircuitBreaker(3, 5000);

        String result = breaker.execute(() -> "success");

        assertEquals("success", result);
        assertEquals(RetryUtils.CircuitState.CLOSED, breaker.getState());
    }

    @Test
    @DisplayName("RetryUtils CircuitBreaker opens after threshold")
    void circuitBreakerOpens() throws Exception {
        RetryUtils.CircuitBreaker breaker = new RetryUtils.CircuitBreaker(2, 10000);

        // Trigger failures
        try { breaker.execute(() -> { throw new RuntimeException("Fail"); }); } catch (Exception e) {}
        try { breaker.execute(() -> { throw new RuntimeException("Fail"); }); } catch (Exception e) {}

        assertEquals(RetryUtils.CircuitState.OPEN, breaker.getState());

        // Should throw CircuitBreakerOpenException
        assertThrows(RetryUtils.CircuitBreakerOpenException.class,
            () -> breaker.execute(() -> "success"));
    }

    @Test
    @DisplayName("RetryUtils CircuitBreaker reset works")
    void circuitBreakerReset() throws Exception {
        RetryUtils.CircuitBreaker breaker = new RetryUtils.CircuitBreaker(2, 10000);

        // Trigger failures to open
        try { breaker.execute(() -> { throw new RuntimeException("Fail"); }); } catch (Exception e) {}
        try { breaker.execute(() -> { throw new RuntimeException("Fail"); }); } catch (Exception e) {}

        breaker.reset();

        assertEquals(RetryUtils.CircuitState.CLOSED, breaker.getState());
        assertEquals("success", breaker.execute(() -> "success"));
    }

    @Test
    @DisplayName("RetryUtils CircuitBreaker forceOpen works")
    void circuitBreakerForceOpen() {
        RetryUtils.CircuitBreaker breaker = new RetryUtils.CircuitBreaker(3, 5000);

        breaker.forceOpen();

        assertEquals(RetryUtils.CircuitState.OPEN, breaker.getState());
    }
}