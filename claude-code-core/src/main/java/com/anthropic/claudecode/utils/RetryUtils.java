/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code retry utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Retry policy and execution utilities.
 */
public final class RetryUtils {
    private RetryUtils() {}

    /**
     * Retry policy configuration.
     */
    public record RetryPolicy(
        int maxAttempts,
        long initialDelayMs,
        double backoffMultiplier,
        long maxDelayMs,
        Set<Class<? extends Exception>> retryableExceptions
    ) {
        public static RetryPolicy defaults() {
            return new RetryPolicy(3, 1000, 2.0, 30000, Set.of(Exception.class));
        }

        public static RetryPolicy of(int maxAttempts) {
            return new RetryPolicy(maxAttempts, 1000, 2.0, 30000, Set.of(Exception.class));
        }

        public static RetryPolicy exponentialBackoff(int maxAttempts, long initialDelayMs) {
            return new RetryPolicy(maxAttempts, initialDelayMs, 2.0, 30000, Set.of(Exception.class));
        }

        public static RetryPolicy linearBackoff(int maxAttempts, long delayMs) {
            return new RetryPolicy(maxAttempts, delayMs, 1.0, delayMs, Set.of(Exception.class));
        }

        public static RetryPolicy immediate(int maxAttempts) {
            return new RetryPolicy(maxAttempts, 0, 1.0, 0, Set.of(Exception.class));
        }

        public RetryPolicy withRetryableExceptions(Set<Class<? extends Exception>> exceptions) {
            return new RetryPolicy(maxAttempts, initialDelayMs, backoffMultiplier, maxDelayMs, exceptions);
        }

        public RetryPolicy withMaxDelay(long maxDelayMs) {
            return new RetryPolicy(maxAttempts, initialDelayMs, backoffMultiplier, maxDelayMs, retryableExceptions);
        }

        public long getDelayForAttempt(int attempt) {
            if (attempt <= 0) return 0;
            long delay = (long) (initialDelayMs * Math.pow(backoffMultiplier, attempt - 1));
            return Math.min(delay, maxDelayMs);
        }
    }

    /**
     * Retry result.
     */
    public record RetryResult<T>(T result, Exception lastError, int attempts, boolean succeeded) {}

    /**
     * Execute with retry.
     */
    public static <T> RetryResult<T> executeWithRetry(Supplier<T> action, RetryPolicy policy) {
        Exception lastError = null;
        int attempts = 0;

        while (attempts < policy.maxAttempts()) {
            attempts++;
            try {
                T result = action.get();
                return new RetryResult<>(result, null, attempts, true);
            } catch (Exception e) {
                lastError = e;

                if (!isRetryable(e, policy.retryableExceptions())) {
                    break;
                }

                if (attempts < policy.maxAttempts()) {
                    long delay = policy.getDelayForAttempt(attempts);
                    if (delay > 0) {
                        sleep(delay);
                    }
                }
            }
        }

        return new RetryResult<>(null, lastError, attempts, false);
    }

    /**
     * Execute with retry and consumer.
     */
    public static <T> RetryResult<T> executeWithRetry(
        Supplier<T> action,
        RetryPolicy policy,
        Consumer<Exception> onFailure
    ) {
        Exception lastError = null;
        int attempts = 0;

        while (attempts < policy.maxAttempts()) {
            attempts++;
            try {
                T result = action.get();
                return new RetryResult<>(result, null, attempts, true);
            } catch (Exception e) {
                lastError = e;
                onFailure.accept(e);

                if (!isRetryable(e, policy.retryableExceptions())) {
                    break;
                }

                if (attempts < policy.maxAttempts()) {
                    long delay = policy.getDelayForAttempt(attempts);
                    if (delay > 0) {
                        sleep(delay);
                    }
                }
            }
        }

        return new RetryResult<>(null, lastError, attempts, false);
    }

    /**
     * Execute with retry asynchronously.
     */
    public static <T> CompletableFuture<RetryResult<T>> executeWithRetryAsync(
        Supplier<T> action,
        RetryPolicy policy
    ) {
        return CompletableFuture.supplyAsync(() -> executeWithRetry(action, policy));
    }

    /**
     * Execute with retry returning void.
     */
    public static RetryResult<Void> executeWithRetry(Runnable action, RetryPolicy policy) {
        return executeWithRetry(() -> {
            action.run();
            return null;
        }, policy);
    }

    /**
     * Execute until success or max attempts.
     */
    public static <T> T retryUntilSuccess(Supplier<T> action, int maxAttempts, long delayMs) {
        RetryPolicy policy = RetryPolicy.linearBackoff(maxAttempts, delayMs);
        RetryResult<T> result = executeWithRetry(action, policy);
        if (!result.succeeded()) {
            throw new RuntimeException("Retry failed after " + result.attempts() + " attempts", result.lastError());
        }
        return result.result();
    }

    /**
     * Check if exception is retryable.
     */
    private static boolean isRetryable(Exception e, Set<Class<? extends Exception>> retryableExceptions) {
        for (Class<? extends Exception> exceptionClass : retryableExceptions) {
            if (exceptionClass.isInstance(e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sleep for specified milliseconds.
     */
    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Retry with custom condition.
     */
    public static <T> RetryResult<T> retryWhile(
        Supplier<T> action,
        Predicate<T> shouldRetry,
        int maxAttempts,
        long delayMs
    ) {
        int attempts = 0;
        T result = null;

        while (attempts < maxAttempts) {
            attempts++;
            result = action.get();

            if (!shouldRetry.test(result)) {
                return new RetryResult<>(result, null, attempts, true);
            }

            if (attempts < maxAttempts) {
                sleep(delayMs);
            }
        }

        return new RetryResult<>(result, null, attempts, false);
    }

    /**
     * Builder for retry policy.
     */
    public static class RetryPolicyBuilder {
        private int maxAttempts = 3;
        private long initialDelayMs = 1000;
        private double backoffMultiplier = 2.0;
        private long maxDelayMs = 30000;
        private Set<Class<? extends Exception>> retryableExceptions = Set.of(Exception.class);

        public RetryPolicyBuilder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public RetryPolicyBuilder initialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
            return this;
        }

        public RetryPolicyBuilder backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        public RetryPolicyBuilder maxDelayMs(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
            return this;
        }

        public RetryPolicyBuilder retryableExceptions(Set<Class<? extends Exception>> exceptions) {
            this.retryableExceptions = exceptions;
            return this;
        }

        public RetryPolicyBuilder addRetryableException(Class<? extends Exception> exception) {
            this.retryableExceptions = new HashSet<>(this.retryableExceptions);
            this.retryableExceptions.add(exception);
            return this;
        }

        public RetryPolicy build() {
            return new RetryPolicy(maxAttempts, initialDelayMs, backoffMultiplier, maxDelayMs, retryableExceptions);
        }
    }

    /**
     * Create retry policy builder.
     */
    public static RetryPolicyBuilder policyBuilder() {
        return new RetryPolicyBuilder();
    }

    /**
     * Create a retryable supplier.
     */
    public static <T> Supplier<T> retryable(Supplier<T> supplier, RetryPolicy policy) {
        return () -> {
            RetryResult<T> result = executeWithRetry(supplier, policy);
            if (!result.succeeded()) {
                throw new RuntimeException("Retry failed", result.lastError());
            }
            return result.result();
        };
    }

    /**
     * Circuit breaker state.
     */
    public enum CircuitState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    /**
     * Simple circuit breaker.
     */
    public static class CircuitBreaker {
        private final int failureThreshold;
        private final long resetTimeoutMs;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);

        public CircuitBreaker(int failureThreshold, long resetTimeoutMs) {
            this.failureThreshold = failureThreshold;
            this.resetTimeoutMs = resetTimeoutMs;
        }

        public <T> T execute(Supplier<T> action) throws Exception {
            CircuitState currentState = checkState();

            if (currentState == CircuitState.OPEN) {
                throw new CircuitBreakerOpenException("Circuit breaker is open");
            }

            try {
                T result = action.get();
                onSuccess();
                return result;
            } catch (Exception e) {
                onFailure();
                throw e;
            }
        }

        public void execute(Runnable action) throws Exception {
            execute(() -> {
                action.run();
                return null;
            });
        }

        private CircuitState checkState() {
            CircuitState currentState = state.get();

            if (currentState == CircuitState.OPEN) {
                long elapsed = System.currentTimeMillis() - lastFailureTime.get();
                if (elapsed >= resetTimeoutMs) {
                    state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN);
                    return CircuitState.HALF_OPEN;
                }
            }

            return state.get();
        }

        private void onSuccess() {
            failureCount.set(0);
            state.set(CircuitState.CLOSED);
        }

        private void onFailure() {
            int failures = failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());

            if (failures >= failureThreshold) {
                state.set(CircuitState.OPEN);
            }
        }

        public CircuitState getState() {
            return state.get();
        }

        public void reset() {
            failureCount.set(0);
            state.set(CircuitState.CLOSED);
        }

        public void forceOpen() {
            state.set(CircuitState.OPEN);
            lastFailureTime.set(System.currentTimeMillis());
        }
    }

    /**
     * Circuit breaker open exception.
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }

    // AtomicInteger and AtomicLong imports
    private static class AtomicInteger extends java.util.concurrent.atomic.AtomicInteger {
        public AtomicInteger(int initialValue) {
            super(initialValue);
        }
    }

    private static class AtomicLong extends java.util.concurrent.atomic.AtomicLong {
        public AtomicLong(long initialValue) {
            super(initialValue);
        }
    }

    private static class AtomicReference<T> extends java.util.concurrent.atomic.AtomicReference<T> {
        public AtomicReference(T initialValue) {
            super(initialValue);
        }
    }
}