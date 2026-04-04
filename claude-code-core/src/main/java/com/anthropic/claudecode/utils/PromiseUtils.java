/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code promise utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

/**
 * Promise-like asynchronous utilities.
 */
public final class PromiseUtils {
    private PromiseUtils() {}

    /**
     * Promise - represents an async operation.
     */
    public static final class Promise<T> {
        private final CompletableFuture<T> future;

        private Promise(CompletableFuture<T> future) {
            this.future = future;
        }

        /**
         * Create resolved promise.
         */
        public static <T> Promise<T> resolve(T value) {
            return new Promise<>(CompletableFuture.completedFuture(value));
        }

        /**
         * Create rejected promise.
         */
        public static <T> Promise<T> reject(Throwable error) {
            CompletableFuture<T> f = new CompletableFuture<>();
            f.completeExceptionally(error);
            return new Promise<>(f);
        }

        /**
         * Create pending promise.
         */
        public static <T> Promise<T> pending() {
            return new Promise<>(new CompletableFuture<>());
        }

        /**
         * Create from supplier.
         */
        public static <T> Promise<T> from(Supplier<T> supplier) {
            return new Promise<>(CompletableFuture.supplyAsync(supplier));
        }

        /**
         * Create from completable future.
         */
        public static <T> Promise<T> from(CompletableFuture<T> future) {
            return new Promise<>(future);
        }

        /**
         * Then - map the result.
         */
        public <R> Promise<R> then(Function<T, R> mapper) {
            return new Promise<>(future.thenApply(mapper));
        }

        /**
         * Then - chain another promise.
         */
        public <R> Promise<R> thenCompose(Function<T, Promise<R>> mapper) {
            return new Promise<>(future.thenCompose(t -> mapper.apply(t).future));
        }

        /**
         * Catch - handle errors.
         */
        public Promise<T> catch_(Function<Throwable, T> handler) {
            return new Promise<>(future.exceptionally(handler));
        }

        /**
         * Finally - always execute.
         */
        public Promise<T> finally_(Runnable action) {
            return new Promise<>(future.whenComplete((v, e) -> action.run()));
        }

        /**
         * Then apply when successful.
         */
        public Promise<T> thenAccept(Consumer<T> consumer) {
            return new Promise<>(future.thenAccept(consumer).thenApply(v -> null));
        }

        /**
         * Then accept on current value.
         */
        public Promise<T> tap(Consumer<T> consumer) {
            return new Promise<>(future.thenApply(t -> {
                consumer.accept(t);
                return t;
            }));
        }

        /**
         * Wait for result.
         */
        public T await() throws Throwable {
            try {
                return future.join();
            } catch (CompletionException e) {
                throw e.getCause();
            }
        }

        /**
         * Wait for result with timeout.
         */
        public T await(long timeout, TimeUnit unit) throws Throwable {
            try {
                return future.get(timeout, unit);
            } catch (TimeoutException e) {
                throw new TimeoutException("Promise timed out");
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        }

        /**
         * Get as optional (returns empty if rejected).
         */
        public Optional<T> getOptional() {
            try {
                return Optional.ofNullable(future.getNow(null));
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        /**
         * Check if done.
         */
        public boolean isDone() {
            return future.isDone();
        }

        /**
         * Check if completed exceptionally.
         */
        public boolean isCompletedExceptionally() {
            return future.isCompletedExceptionally();
        }

        /**
         * Cancel promise.
         */
        public boolean cancel() {
            return future.cancel(false);
        }

        /**
         * Get underlying future.
         */
        public CompletableFuture<T> toCompletableFuture() {
            return future;
        }

        /**
         * Static utilities.
         */
        public static final class PromiseStatics {
            private PromiseStatics() {}

            /**
             * All - wait for all promises.
             */
            @SafeVarargs
            public static <T> Promise<List<T>> all(Promise<T>... promises) {
                return all(Arrays.asList(promises));
            }

            /**
             * All - wait for all promises.
             */
            public static <T> Promise<List<T>> all(List<Promise<T>> promises) {
                List<CompletableFuture<T>> futures = promises.stream()
                    .map(p -> p.future)
                    .toList();

                CompletableFuture<Void> allOf = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
                );

                return new Promise<>(allOf.thenApply(v ->
                    futures.stream()
                        .map(CompletableFuture::join)
                        .toList()
                ));
            }

            /**
             * Race - first to complete wins.
             */
            @SafeVarargs
            public static <T> Promise<T> race(Promise<T>... promises) {
                return race(Arrays.asList(promises));
            }

            /**
             * Race - first to complete wins.
             */
            public static <T> Promise<T> race(List<Promise<T>> promises) {
                CompletableFuture<T> result = new CompletableFuture<>();

                for (Promise<T> promise : promises) {
                    promise.future.whenComplete((v, e) -> {
                        if (e != null) {
                            result.completeExceptionally(e);
                        } else {
                            result.complete(v);
                        }
                    });
                }

                return new Promise<>(result);
            }

            /**
             * Any - first successful wins.
             */
            @SafeVarargs
            public static <T> Promise<T> any(Promise<T>... promises) {
                return any(Arrays.asList(promises));
            }

            /**
             * Any - first successful wins.
             */
            public static <T> Promise<T> any(List<Promise<T>> promises) {
                List<CompletableFuture<T>> futures = promises.stream()
                    .map(p -> p.future)
                    .toList();

                CompletableFuture<T> result = new CompletableFuture<>();
                AtomicInteger failures = new AtomicInteger(0);

                for (CompletableFuture<T> future : futures) {
                    future.whenComplete((v, e) -> {
                        if (e != null) {
                            if (failures.incrementAndGet() == futures.size()) {
                                result.completeExceptionally(
                                    new ExecutionException("All promises failed", e)
                                );
                            }
                        } else {
                            result.complete(v);
                        }
                    });
                }

                return new Promise<>(result);
            }

            /**
             * All settled - wait for all, collect results.
             */
            @SafeVarargs
            public static <T> Promise<List<PromiseResult<T>>> allSettled(Promise<T>... promises) {
                return allSettled(Arrays.asList(promises));
            }

            /**
             * All settled - wait for all, collect results.
             */
            @SuppressWarnings("unchecked")
            public static <T> Promise<List<PromiseResult<T>>> allSettled(List<Promise<T>> promises) {
                List<CompletableFuture<PromiseResult<T>>> resultFutures = (List<CompletableFuture<PromiseResult<T>>>) (List<?>) promises.stream()
                    .map(p -> p.future.handle((v, e) ->
                        e != null ? new PromiseResult<>(null, e) : new PromiseResult<>(v, null)
                    ))
                    .toList();

                CompletableFuture<Void> allOf = CompletableFuture.allOf(
                    resultFutures.toArray(new CompletableFuture[0])
                );

                return new Promise<>(allOf.thenApply(v ->
                    resultFutures.stream()
                        .map(CompletableFuture::join)
                        .toList()
                ));
            }

            /**
             * Delay.
             */
            public static <T> Promise<T> delay(long millis, T value) {
                CompletableFuture<T> future = new CompletableFuture<>();
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.schedule(() -> {
                    future.complete(value);
                    scheduler.shutdown();
                }, millis, TimeUnit.MILLISECONDS);
                return new Promise<>(future);
            }

            /**
             * Timeout.
             */
            public static <T> Promise<T> timeout(Promise<T> promise, long millis) {
                CompletableFuture<T> result = new CompletableFuture<>();

                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                ScheduledFuture<?> timeoutFuture = scheduler.schedule(() -> {
                    result.completeExceptionally(new TimeoutException("Promise timed out"));
                    scheduler.shutdown();
                }, millis, TimeUnit.MILLISECONDS);

                promise.future.whenComplete((v, e) -> {
                    timeoutFuture.cancel(false);
                    scheduler.shutdown();
                    if (e != null) {
                        result.completeExceptionally(e);
                    } else {
                        result.complete(v);
                    }
                });

                return new Promise<>(result);
            }

            /**
             * Retry.
             */
            public static <T> Promise<T> retry(Supplier<Promise<T>> supplier, int maxAttempts, long delayMs) {
                return retry(supplier, maxAttempts, delayMs, 1);
            }

            private static <T> Promise<T> retry(Supplier<Promise<T>> supplier, int attemptsLeft, long delayMs, int attempt) {
                return supplier.get()
                    .catch_(error -> {
                        if (attemptsLeft <= 1) {
                            throw new CompletionException(error);
                        }
                        return delay(delayMs, null)
                            .then(v -> retry(supplier, attemptsLeft - 1, delayMs * 2, attempt + 1))
                            .thenCompose(p -> p)
                            .future.join();
                    });
            }
        }
    }

    /**
     * Promise result (settled).
     */
    public record PromiseResult<T>(T value, Throwable error) {
        public boolean succeeded() {
            return error == null;
        }

        public boolean failed() {
            return error != null;
        }

        public Optional<T> getValue() {
            return Optional.ofNullable(value);
        }

        public Optional<Throwable> getError() {
            return Optional.ofNullable(error);
        }
    }

    /**
     * Deferred - manually resolve/reject promise.
     */
    public static final class Deferred<T> {
        private final CompletableFuture<T> future = new CompletableFuture<>();
        private final Promise<T> promise = new Promise<>(future);

        /**
         * Resolve the promise.
         */
        public boolean resolve(T value) {
            return future.complete(value);
        }

        /**
         * Reject the promise.
         */
        public boolean reject(Throwable error) {
            return future.completeExceptionally(error);
        }

        /**
         * Get the promise.
         */
        public Promise<T> promise() {
            return promise;
        }

        /**
         * Check if resolved/rejected.
         */
        public boolean isDone() {
            return future.isDone();
        }
    }

    /**
     * Create a deferred.
     */
    public static <T> Deferred<T> deferred() {
        return new Deferred<>();
    }

    /**
     * Create resolved promise.
     */
    public static <T> Promise<T> resolve(T value) {
        return Promise.resolve(value);
    }

    /**
     * Create rejected promise.
     */
    public static <T> Promise<T> reject(Throwable error) {
        return Promise.reject(error);
    }

    /**
     * Create promise from supplier.
     */
    public static <T> Promise<T> from(Supplier<T> supplier) {
        return Promise.from(supplier);
    }

    /**
     * All promises.
     */
    @SafeVarargs
    public static <T> Promise<List<T>> all(Promise<T>... promises) {
        return Promise.PromiseStatics.all(promises);
    }

    /**
     * Race promises.
     */
    @SafeVarargs
    public static <T> Promise<T> race(Promise<T>... promises) {
        return Promise.PromiseStatics.race(promises);
    }

    /**
     * Any promise (first success).
     */
    @SafeVarargs
    public static <T> Promise<T> any(Promise<T>... promises) {
        return Promise.PromiseStatics.any(promises);
    }
}