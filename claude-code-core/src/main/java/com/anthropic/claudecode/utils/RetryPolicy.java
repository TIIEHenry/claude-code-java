/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code retry policy
 */
package com.anthropic.claudecode.utils;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Retry policy with configurable strategies.
 */
public final class RetryPolicy {
    private final int maxAttempts;
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double backoffMultiplier;
    private final Set<Class<? extends Exception>> retryableExceptions;
    private final Predicate<Exception> retryPredicate;
    private final Consumer<Exception> onRetry;
    private final Consumer<Exception> onFailure;

    public RetryPolicy(int maxAttempts) {
        this(maxAttempts, Duration.ZERO);
    }

    public RetryPolicy(int maxAttempts, Duration initialDelay) {
        this(maxAttempts, initialDelay, Duration.ofMinutes(1), 2.0,
            Set.of(), e -> true, e -> {}, e -> {});
    }

    public RetryPolicy(int maxAttempts, Duration initialDelay, Duration maxDelay,
            double backoffMultiplier, Set<Class<? extends Exception>> retryableExceptions,
            Predicate<Exception> retryPredicate, Consumer<Exception> onRetry,
            Consumer<Exception> onFailure) {
        this.maxAttempts = maxAttempts;
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
        this.backoffMultiplier = backoffMultiplier;
        this.retryableExceptions = retryableExceptions;
        this.retryPredicate = retryPredicate;
        this.onRetry = onRetry;
        this.onFailure = onFailure;
    }

    /**
     * Execute with retry.
     */
    public <T> T execute(Supplier<T> operation) throws RetryExhaustedException {
        Exception lastException = null;
        Duration currentDelay = initialDelay;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;

                // Check if exception is retryable
                if (!isRetryable(e, attempt)) {
                    onFailure.accept(e);
                    throw new RetryExhaustedException("Non-retryable exception", e, attempt);
                }

                // Call retry callback
                onRetry.accept(e);

                // Wait before retry (except last attempt)
                if (attempt < maxAttempts) {
                    sleep(currentDelay);
                    currentDelay = calculateNextDelay(currentDelay);
                }
            }
        }

        onFailure.accept(lastException);
        throw new RetryExhaustedException(
            "Retry policy exhausted after " + maxAttempts + " attempts",
            lastException,
            maxAttempts
        );
    }

    /**
     * Execute with retry and fallback.
     */
    public <T> T execute(Supplier<T> operation, Supplier<T> fallback) {
        try {
            return execute(operation);
        } catch (RetryExhaustedException e) {
            return fallback.get();
        }
    }

    /**
     * Execute runnable with retry.
     */
    public void execute(Runnable operation) throws RetryExhaustedException {
        execute(() -> {
            operation.run();
            return null;
        });
    }

    /**
     * Execute with retry result.
     */
    public <T> RetryResult<T> executeForResult(Supplier<T> operation) {
        Exception lastException = null;
        Duration currentDelay = initialDelay;
        int attempts = 0;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            attempts = attempt;
            try {
                T result = operation.get();
                return new RetryResult<>(result, attempt, true, null, Duration.ZERO);
            } catch (Exception e) {
                lastException = e;

                if (!isRetryable(e, attempt)) {
                    return new RetryResult<>(null, attempt, false, e, Duration.ZERO);
                }

                onRetry.accept(e);

                if (attempt < maxAttempts) {
                    sleep(currentDelay);
                    currentDelay = calculateNextDelay(currentDelay);
                }
            }
        }

        onFailure.accept(lastException);
        return new RetryResult<>(null, maxAttempts, false, lastException, Duration.ZERO);
    }

    /**
     * Check if exception is retryable.
     */
    private boolean isRetryable(Exception e, int attempt) {
        if (attempt >= maxAttempts) return false;

        // Check predicate first
        if (!retryPredicate.test(e)) return false;

        // Check exception types
        if (retryableExceptions.isEmpty()) return true;

        return retryableExceptions.stream()
            .anyMatch(type -> type.isInstance(e));
    }

    /**
     * Calculate next delay with exponential backoff.
     */
    private Duration calculateNextDelay(Duration currentDelay) {
        long nextDelayNanos = (long) (currentDelay.toNanos() * backoffMultiplier);
        long maxDelayNanos = maxDelay.toNanos();
        return Duration.ofNanos(Math.min(nextDelayNanos, maxDelayNanos));
    }

    /**
     * Sleep for duration.
     */
    private void sleep(Duration duration) {
        if (duration.isZero() || duration.isNegative()) return;

        try {
            Thread.sleep(duration.toMillis(), (int) (duration.toNanos() % 1_000_000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry interrupted", e);
        }
    }

    /**
     * Get max attempts.
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Get initial delay.
     */
    public Duration getInitialDelay() {
        return initialDelay;
    }

    /**
     * Get max delay.
     */
    public Duration getMaxDelay() {
        return maxDelay;
    }

    /**
     * Create builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class.
     */
    public static final class Builder {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofMillis(100);
        private Duration maxDelay = Duration.ofSeconds(30);
        private double backoffMultiplier = 2.0;
        private Set<Class<? extends Exception>> retryableExceptions = new HashSet<>();
        private Predicate<Exception> retryPredicate = e -> true;
        private Consumer<Exception> onRetry = e -> {};
        private Consumer<Exception> onFailure = e -> {};

        public Builder maxAttempts(int attempts) {
            this.maxAttempts = attempts;
            return this;
        }

        public Builder initialDelay(Duration delay) {
            this.initialDelay = delay;
            return this;
        }

        public Builder initialDelay(long millis) {
            this.initialDelay = Duration.ofMillis(millis);
            return this;
        }

        public Builder maxDelay(Duration delay) {
            this.maxDelay = delay;
            return this;
        }

        public Builder maxDelay(long seconds) {
            this.maxDelay = Duration.ofSeconds(seconds);
            return this;
        }

        public Builder backoffMultiplier(double multiplier) {
            this.backoffMultiplier = multiplier;
            return this;
        }

        public Builder exponentialBackoff() {
            this.backoffMultiplier = 2.0;
            return this;
        }

        public Builder linearBackoff() {
            this.backoffMultiplier = 1.0;
            return this;
        }

        public Builder noBackoff() {
            this.backoffMultiplier = 0;
            this.initialDelay = Duration.ZERO;
            return this;
        }

        @SafeVarargs
        public final Builder retryOn(Class<? extends Exception>... exceptions) {
            this.retryableExceptions.addAll(Arrays.asList(exceptions));
            return this;
        }

        public Builder retryPredicate(Predicate<Exception> predicate) {
            this.retryPredicate = predicate;
            return this;
        }

        public Builder onRetry(Consumer<Exception> callback) {
            this.onRetry = callback;
            return this;
        }

        public Builder onFailure(Consumer<Exception> callback) {
            this.onFailure = callback;
            return this;
        }

        public Builder retryOnHttp5xx() {
            return retryPredicate(e ->
                e.getClass().getName().contains("Http") &&
                e.getMessage() != null &&
                e.getMessage().contains("500")
            );
        }

        public Builder retryOnTimeout() {
            return retryOn(
                TimeoutException.class,
                java.net.SocketTimeoutException.class
            );
        }

        public Builder retryOnConnectionError() {
            return retryPredicate(e ->
                e.getClass().getName().contains("Connect") ||
                e.getClass().getName().contains("Socket")
            );
        }

        public RetryPolicy build() {
            return new RetryPolicy(maxAttempts, initialDelay, maxDelay, backoffMultiplier,
                retryableExceptions, retryPredicate, onRetry, onFailure);
        }
    }

    /**
     * Retry result.
     */
    public record RetryResult<T>(T value, int attempts, boolean success, Exception lastError, Duration totalTime) {
        public boolean succeeded() {
            return success;
        }

        public boolean failed() {
            return !success;
        }

        public Optional<T> getValue() {
            return Optional.ofNullable(value);
        }

        public Optional<Exception> getError() {
            return Optional.ofNullable(lastError);
        }
    }

    /**
     * Retry exhausted exception.
     */
    public static final class RetryExhaustedException extends RuntimeException {
        private final int attempts;

        public RetryExhaustedException(String message, Exception cause, int attempts) {
            super(message, cause);
            this.attempts = attempts;
        }

        public int getAttempts() {
            return attempts;
        }
    }

    /**
     * Retry utilities.
     */
    public static final class RetryUtils {
        private RetryUtils() {}

        /**
         * Simple retry with fixed attempts.
         */
        public static <T> T retry(int maxAttempts, Supplier<T> operation) {
            return new RetryPolicy(maxAttempts).execute(operation);
        }

        /**
         * Retry with exponential backoff.
         */
        public static <T> T retryWithBackoff(int maxAttempts, Duration initialDelay, Supplier<T> operation) {
            return RetryPolicy.builder()
                .maxAttempts(maxAttempts)
                .initialDelay(initialDelay)
                .exponentialBackoff()
                .build()
                .execute(operation);
        }

        /**
         * Retry forever until success (use carefully).
         */
        public static <T> T retryForever(Duration delay, Supplier<T> operation) {
            while (true) {
                try {
                    return operation.get();
                } catch (Exception e) {
                    DurationUtils.sleepQuietly(delay);
                }
            }
        }

        /**
         * Retry until condition met.
         */
        public static <T> T retryUntil(int maxAttempts, Supplier<T> operation, Predicate<T> condition) {
            for (int i = 0; i < maxAttempts; i++) {
                T result = operation.get();
                if (condition.test(result)) {
                    return result;
                }
                DurationUtils.sleepQuietly(Duration.ofMillis(100));
            }
            throw new RetryExhaustedException("Condition not met after " + maxAttempts + " attempts", null, maxAttempts);
        }
    }
}