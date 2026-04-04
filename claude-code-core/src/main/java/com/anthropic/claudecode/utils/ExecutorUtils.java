/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code executor utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Function executor utilities.
 */
public final class ExecutorUtils {
    private ExecutorUtils() {}

    /**
     * Execute function with retry.
     */
    public static <T> T withRetry(int maxAttempts, long delayMs, Supplier<T> supplier) {
        Exception lastException = null;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                return supplier.get();
            } catch (Exception e) {
                lastException = e;
                if (i < maxAttempts - 1) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                }
            }
        }
        throw new RuntimeException("Operation failed after " + maxAttempts + " attempts", lastException);
    }

    /**
     * Execute function with timeout.
     */
    public static <T> T withTimeout(long timeoutMs, Supplier<T> supplier) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<T> future = executor.submit(supplier::get);
            try {
                return future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new RuntimeException("Operation timed out after " + timeoutMs + "ms");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Execute function with fallback.
     */
    public static <T> T withFallback(Supplier<T> primary, Supplier<T> fallback) {
        try {
            return primary.get();
        } catch (Exception e) {
            return fallback.get();
        }
    }

    /**
     * Execute function with multiple fallbacks.
     */
    @SafeVarargs
    public static <T> T withFallbacks(Supplier<T>... suppliers) {
        Exception lastException = null;
        for (Supplier<T> supplier : suppliers) {
            try {
                return supplier.get();
            } catch (Exception e) {
                lastException = e;
            }
        }
        throw new RuntimeException("All suppliers failed", lastException);
    }

    /**
     * Execute if condition is true.
     */
    public static <T> Optional<T> executeIf(boolean condition, Supplier<T> supplier) {
        return condition ? Optional.ofNullable(supplier.get()) : Optional.empty();
    }

    /**
     * Execute if object is not null.
     */
    public static <T, R> Optional<R> ifNotNull(T obj, Function<T, R> function) {
        return obj != null ? Optional.ofNullable(function.apply(obj)) : Optional.empty();
    }

    /**
     * Execute if string is not blank.
     */
    public static <R> Optional<R> ifNotBlank(String str, Function<String, R> function) {
        return str != null && !str.isBlank() ? Optional.ofNullable(function.apply(str)) : Optional.empty();
    }

    /**
     * Execute if collection is not empty.
     */
    public static <T extends Collection<?>, R> Optional<R> ifNotEmpty(T collection, Function<T, R> function) {
        return collection != null && !collection.isEmpty() ? Optional.ofNullable(function.apply(collection)) : Optional.empty();
    }

    /**
     * Execute with measured time.
     */
    public static <T> TimedResult<T> measureTime(Supplier<T> supplier) {
        long start = System.nanoTime();
        T result = supplier.get();
        long duration = System.nanoTime() - start;
        return new TimedResult<>(result, duration);
    }

    /**
     * Execute with measured time (void).
     */
    public static long measureTime(Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        return System.nanoTime() - start;
    }

    /**
     * Timed result.
     */
    public record TimedResult<T>(T value, long durationNanos) {
        public double durationMs() {
            return durationNanos / 1_000_000.0;
        }
    }

    /**
     * Execute asynchronously.
     */
    public static <T> CompletableFuture<T> async(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier);
    }

    /**
     * Execute asynchronously with executor.
     */
    public static <T> CompletableFuture<T> async(Supplier<T> supplier, Executor executor) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    /**
     * Execute multiple in parallel.
     */
    @SafeVarargs
    public static <T> CompletableFuture<List<T>> parallel(Supplier<T>... suppliers) {
        return parallel(Arrays.asList(suppliers));
    }

    /**
     * Execute multiple in parallel.
     */
    public static <T> CompletableFuture<List<T>> parallel(List<Supplier<T>> suppliers) {
        List<CompletableFuture<T>> futures = suppliers.stream()
            .map(CompletableFuture::supplyAsync)
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }

    /**
     * Race multiple suppliers (first to complete wins).
     */
    @SafeVarargs
    public static <T> CompletableFuture<T> race(Supplier<T>... suppliers) {
        return race(Arrays.asList(suppliers));
    }

    /**
     * Race multiple suppliers.
     */
    public static <T> CompletableFuture<T> race(List<Supplier<T>> suppliers) {
        CompletableFuture<T> result = new CompletableFuture<>();
        for (Supplier<T> supplier : suppliers) {
            CompletableFuture.supplyAsync(supplier)
                .thenAccept(result::complete)
                .exceptionally(e -> {
                    result.completeExceptionally(e);
                    return null;
                });
        }
        return result;
    }

    /**
     * Execute with rate limiting.
     */
    public static <T> Supplier<T> rateLimited(Supplier<T> supplier, long minIntervalMs) {
        return new Supplier<T>() {
            private volatile long lastCall = 0;

            @Override
            public T get() {
                long now = System.currentTimeMillis();
                long elapsed = now - lastCall;
                if (elapsed < minIntervalMs) {
                    try {
                        Thread.sleep(minIntervalMs - elapsed);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                lastCall = System.currentTimeMillis();
                return supplier.get();
            }
        };
    }

    /**
     * Execute with memoization.
     */
    public static <T> Supplier<T> memoized(Supplier<T> supplier) {
        return new Supplier<T>() {
            private volatile boolean computed = false;
            private volatile T value;

            @Override
            public T get() {
                if (!computed) {
                    synchronized (this) {
                        if (!computed) {
                            value = supplier.get();
                            computed = true;
                        }
                    }
                }
                return value;
            }
        };
    }

    /**
     * Execute with memoization and TTL.
     */
    public static <T> Supplier<T> memoized(Supplier<T> supplier, long ttlMs) {
        return new Supplier<T>() {
            private volatile T value;
            private volatile long expiry = 0;

            @Override
            public T get() {
                long now = System.currentTimeMillis();
                if (value == null || now > expiry) {
                    synchronized (this) {
                        if (value == null || now > expiry) {
                            value = supplier.get();
                            expiry = now + ttlMs;
                        }
                    }
                }
                return value;
            }
        };
    }

    /**
     * Execute only once.
     */
    public static <T> Supplier<T> once(Supplier<T> supplier) {
        return new Supplier<T>() {
            private final AtomicBoolean executed = new AtomicBoolean(false);
            private volatile T value;

            @Override
            public T get() {
                if (executed.compareAndSet(false, true)) {
                    value = supplier.get();
                }
                return value;
            }
        };
    }

    /**
     * Execute lazily.
     */
    public static <T> Supplier<T> lazy(Supplier<T> supplier) {
        return memoized(supplier);
    }

    /**
     * Execute with circuit breaker.
     */
    public static <T> Supplier<T> withCircuitBreaker(
            Supplier<T> supplier,
            int failureThreshold,
            long resetTimeoutMs) {
        return new Supplier<T>() {
            private final AtomicInteger failures = new AtomicInteger(0);
            private volatile long lastFailure = 0;
            private volatile boolean open = false;

            @Override
            public T get() {
                if (open) {
                    if (System.currentTimeMillis() - lastFailure > resetTimeoutMs) {
                        open = false;
                        failures.set(0);
                    } else {
                        throw new RuntimeException("Circuit breaker is open");
                    }
                }

                try {
                    T result = supplier.get();
                    failures.set(0);
                    return result;
                } catch (Exception e) {
                    lastFailure = System.currentTimeMillis();
                    if (failures.incrementAndGet() >= failureThreshold) {
                        open = true;
                    }
                    throw e;
                }
            }
        };
    }

    /**
     * Execute with bulkhead (max concurrent calls).
     */
    public static <T> Supplier<T> withBulkhead(Supplier<T> supplier, int maxConcurrent) {
        Semaphore semaphore = new Semaphore(maxConcurrent);
        return () -> {
            try {
                semaphore.acquire();
                return supplier.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } finally {
                semaphore.release();
            }
        };
    }

    /**
     * Execute with context.
     */
    public static <T, C> Supplier<T> withContext(Supplier<T> supplier, C context, Consumer<C> setup, Consumer<C> teardown) {
        return () -> {
            setup.accept(context);
            try {
                return supplier.get();
            } finally {
                teardown.accept(context);
            }
        };
    }

    /**
     * Execute in background thread.
     */
    public static <T> CompletableFuture<T> background(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, Executors.newCachedThreadPool());
    }

    /**
     * Delayed execution.
     */
    public static <T> CompletableFuture<T> delayed(Supplier<T> supplier, long delayMs) {
        CompletableFuture<T> result = new CompletableFuture<>();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            try {
                result.complete(supplier.get());
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
        result.whenComplete((v, e) -> scheduler.shutdown());
        return result;
    }

    /**
     * Execute periodically.
     */
    public static <T> ScheduledFuture<?> periodic(Supplier<T> supplier, long initialDelayMs, long periodMs) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        return scheduler.scheduleAtFixedRate(supplier::get, initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
    }

    // Helper classes
    private static class AtomicBoolean extends java.util.concurrent.atomic.AtomicBoolean {
        public AtomicBoolean(boolean initialValue) { super(initialValue); }
    }

    private static class AtomicInteger extends java.util.concurrent.atomic.AtomicInteger {
        public AtomicInteger(int initialValue) { super(initialValue); }
    }

    private static class Semaphore extends java.util.concurrent.Semaphore {
        public Semaphore(int permits) { super(permits); }
    }
}