/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/withRetry.ts
 */
package com.anthropic.claudecode.services.api;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * API retry service with exponential backoff.
 */
public final class ApiRetryService {
    private static final int DEFAULT_MAX_RETRIES = 10;
    private static final int FLOOR_OUTPUT_TOKENS = 3000;
    private static final int MAX_529_RETRIES = 3;
    private static final long BASE_DELAY_MS = 500;
    private static final long PERSISTENT_MAX_BACKOFF_MS = 5 * 60 * 1000;
    private static final long PERSISTENT_RESET_CAP_MS = 6 * 60 * 60 * 1000;
    private static final long HEARTBEAT_INTERVAL_MS = 30_000;
    private static final long DEFAULT_FAST_MODE_FALLBACK_HOLD_MS = 30 * 60 * 1000;
    private static final long SHORT_RETRY_THRESHOLD_MS = 20 * 1000;
    private static final long MIN_COOLDOWN_MS = 10 * 60 * 1000;

    private ApiRetryService() {}

    /**
     * Retry context.
     */
    public record RetryContext(
        Integer maxTokensOverride,
        String model,
        ThinkingConfig thinkingConfig,
        Boolean fastMode
    ) {
        public RetryContext(String model, ThinkingConfig thinkingConfig) {
            this(null, model, thinkingConfig, null);
        }
    }

    /**
     * Thinking configuration.
     */
    public record ThinkingConfig(
        String type, // "enabled", "disabled", "adaptive"
        Integer budgetTokens
    ) {}

    /**
     * Retry options.
     */
    public record RetryOptions(
        Integer maxRetries,
        String model,
        String fallbackModel,
        ThinkingConfig thinkingConfig,
        Boolean fastMode,
        CancellationToken signal,
        String querySource,
        Integer initialConsecutive529Errors
    ) {
        public RetryOptions(String model, ThinkingConfig thinkingConfig) {
            this(null, model, null, thinkingConfig, null, null, null, null);
        }
    }

    /**
     * Cannot retry error.
     */
    public static class CannotRetryError extends RuntimeException {
        private final Throwable originalError;
        private final RetryContext retryContext;

        public CannotRetryError(Throwable originalError, RetryContext retryContext) {
            super(originalError.getMessage(), originalError);
            this.originalError = originalError;
            this.retryContext = retryContext;
        }

        public Throwable getOriginalError() { return originalError; }
        public RetryContext getRetryContext() { return retryContext; }
    }

    /**
     * Fallback triggered error.
     */
    public static class FallbackTriggeredError extends RuntimeException {
        private final String originalModel;
        private final String fallbackModel;

        public FallbackTriggeredError(String originalModel, String fallbackModel) {
            super("Model fallback triggered: " + originalModel + " -> " + fallbackModel);
            this.originalModel = originalModel;
            this.fallbackModel = fallbackModel;
        }

        public String getOriginalModel() { return originalModel; }
        public String getFallbackModel() { return fallbackModel; }
    }

    /**
     * Retry result with yield messages.
     */
    public record RetryResult<T>(
        T value,
        List<SystemMessage> systemMessages,
        Throwable error
    ) {
        public static <T> RetryResult<T> success(T value) {
            return new RetryResult<>(value, Collections.emptyList(), null);
        }

        public static <T> RetryResult<T> failure(Throwable error) {
            return new RetryResult<>(null, Collections.emptyList(), error);
        }
    }

    /**
     * System API message for yielding during retries.
     */
    public record SystemMessage(
        String type,
        String subtype,
        String message,
        long delayMs,
        int attempt,
        int maxRetries
    ) {}

    /**
     * Cancellation token.
     */
    public static class CancellationToken {
        private volatile boolean cancelled = false;

        public void cancel() { cancelled = true; }
        public boolean isCancelled() { return cancelled; }
    }

    /**
     * Execute an operation with retry logic.
     */
    public static <T> CompletableFuture<RetryResult<T>> withRetry(
            Supplier<CompletableFuture<T>> clientSupplier,
            BiFunction<T, Integer, CompletableFuture<T>> operation,
            RetryOptions options) {

        CompletableFuture<RetryResult<T>> result = new CompletableFuture<>();
        int maxRetries = options.maxRetries() != null ? options.maxRetries() : getDefaultMaxRetries();
        RetryContext context = new RetryContext(
            null,
            options.model(),
            options.thinkingConfig(),
            options.fastMode()
        );

        executeWithRetry(clientSupplier, operation, options, context, maxRetries, 1, 0, null, result);
        return result;
    }

    // Internal retry loop
    private static <T> void executeWithRetry(
            Supplier<CompletableFuture<T>> clientSupplier,
            BiFunction<T, Integer, CompletableFuture<T>> operation,
            RetryOptions options,
            RetryContext context,
            int maxRetries,
            int attempt,
            int consecutive529Errors,
            Throwable lastError,
            CompletableFuture<RetryResult<T>> result) {

        // Check cancellation
        if (options.signal() != null && options.signal().isCancelled()) {
            result.complete(RetryResult.failure(new RuntimeException("Operation cancelled")));
            return;
        }

        // Get client and execute operation
        clientSupplier.get().thenCompose(client -> operation.apply(client, attempt))
            .whenComplete((value, error) -> {
                if (error == null) {
                    result.complete(RetryResult.success(value));
                    return;
                }

                Throwable actualError = error instanceof CompletionException ? error.getCause() : error;
                final Throwable finalLastError = actualError;

                // Check if we should retry
                int newConsecutive529Errors = consecutive529Errors;
                if (is529Error(actualError)) {
                    newConsecutive529Errors++;

                    // Check fallback
                    if (newConsecutive529Errors >= MAX_529_RETRIES && options.fallbackModel() != null) {
                        result.completeExceptionally(new FallbackTriggeredError(
                            options.model(), options.fallbackModel()));
                        return;
                    }
                }

                final int finalConsecutive529Errors = newConsecutive529Errors;

                // Check max retries
                boolean persistent = isPersistentRetryEnabled() && isTransientCapacityError(actualError);
                if (attempt > maxRetries && !persistent) {
                    result.completeExceptionally(new CannotRetryError(actualError, context));
                    return;
                }

                // Check if should retry
                if (!shouldRetry(actualError)) {
                    result.completeExceptionally(new CannotRetryError(actualError, context));
                    return;
                }

                // Calculate delay
                long delayMs = getRetryDelay(attempt, getRetryAfter(actualError));

                // Schedule retry
                CompletableFuture.runAsync(() -> executeWithRetry(
                    clientSupplier, operation, options, context, maxRetries,
                    attempt + 1, finalConsecutive529Errors, finalLastError, result),
                    CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS));
            });
    }

    /**
     * Get retry delay with exponential backoff.
     */
    public static long getRetryDelay(int attempt, String retryAfterHeader) {
        return getRetryDelay(attempt, retryAfterHeader, 32000);
    }

    /**
     * Get retry delay with exponential backoff and max delay.
     */
    public static long getRetryDelay(int attempt, String retryAfterHeader, long maxDelayMs) {
        if (retryAfterHeader != null) {
            try {
                int seconds = Integer.parseInt(retryAfterHeader);
                return seconds * 1000L;
            } catch (NumberFormatException ignored) {}
        }

        long baseDelay = Math.min(BASE_DELAY_MS * (long) Math.pow(2, attempt - 1), maxDelayMs);
        double jitter = Math.random() * 0.25 * baseDelay;
        return (long) (baseDelay + jitter);
    }

    /**
     * Get default max retries.
     */
    public static int getDefaultMaxRetries() {
        String maxRetriesEnv = System.getenv("CLAUDE_CODE_MAX_RETRIES");
        if (maxRetriesEnv != null) {
            try {
                return Integer.parseInt(maxRetriesEnv);
            } catch (NumberFormatException ignored) {}
        }
        return DEFAULT_MAX_RETRIES;
    }

    /**
     * Check if error is a 529 overloaded error.
     */
    public static boolean is529Error(Throwable error) {
        if (error instanceof ApiException) {
            ApiException apiError = (ApiException) error;
            return apiError.getStatusCode() == 529 ||
                   (apiError.getMessage() != null && apiError.getMessage().contains("\"type\":\"overloaded_error\""));
        }
        return false;
    }

    // Private helpers

    private static boolean isTransientCapacityError(Throwable error) {
        return is529Error(error) ||
               (error instanceof ApiException && ((ApiException) error).getStatusCode() == 429);
    }

    private static boolean isPersistentRetryEnabled() {
        return isEnvTruthy(System.getenv("CLAUDE_CODE_UNATTENDED_RETRY"));
    }

    private static boolean shouldRetry(Throwable error) {
        if (!(error instanceof ApiException)) {
            return error instanceof java.net.ConnectException ||
                   error instanceof java.net.SocketTimeoutException;
        }

        ApiException apiError = (ApiException) error;
        int status = apiError.getStatusCode();

        // 408 Request Timeout
        if (status == 408) return true;

        // 409 Conflict
        if (status == 409) return true;

        // 429 Rate limit - retry for non-subscribers
        if (status == 429) return true; // Simplified

        // 401 Unauthorized - clear cache and retry
        if (status == 401) return true;

        // 5xx server errors
        if (status >= 500) return true;

        return false;
    }

    private static String getRetryAfter(Throwable error) {
        if (error instanceof ApiException) {
            return ((ApiException) error).getHeader("retry-after");
        }
        return null;
    }

    private static boolean isEnvTruthy(String value) {
        return value != null && ("1".equals(value) || "true".equalsIgnoreCase(value));
    }
}