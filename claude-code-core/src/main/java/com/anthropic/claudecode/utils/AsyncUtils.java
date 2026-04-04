/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Async utilities.
 */
public final class AsyncUtils {
    private AsyncUtils() {}

    /**
     * Sleep for milliseconds.
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Retry with backoff.
     */
    public static <T> T retryWithBackoff(Callable<T> callable, int maxRetries, long initialDelayMs) throws Exception {
        Exception lastException = null;
        long delay = initialDelayMs;

        for (int i = 0; i <= maxRetries; i++) {
            try {
                return callable.call();
            } catch (Exception e) {
                lastException = e;
                if (i < maxRetries) {
                    sleep(delay);
                    delay *= 2;
                }
            }
        }
        throw lastException;
    }

    /**
     * Simple callable interface.
     */
    @FunctionalInterface
    public interface Callable<T> {
        T call() throws Exception;
    }

    /**
     * Run with timeout.
     */
    public static <T> Optional<T> withTimeout(Callable<T> callable, long timeoutMs) {
        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            java.util.concurrent.Future<T> future = executor.submit(callable::call);
            T result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            executor.shutdown();
            return Optional.of(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Debounce a function call.
     */
    public static Runnable debounce(Runnable runnable, long delayMs) {
        return new Runnable() {
            private ScheduledFuture<?> future;
            private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

            @Override
            public void run() {
                if (future != null) {
                    future.cancel(false);
                }
                future = scheduler.schedule(runnable::run, delayMs, TimeUnit.MILLISECONDS);
            }
        };
    }
}