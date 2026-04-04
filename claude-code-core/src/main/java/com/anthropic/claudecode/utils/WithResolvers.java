/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code Promise.withResolvers equivalent
 */
package com.anthropic.claudecode.utils;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Promise resolver utilities - Java equivalent of Promise.withResolvers().
 *
 * In TypeScript, Promise.withResolvers() provides a way to create a promise
 * with its resolve and reject functions exposed. In Java, the equivalent
 * is using CompletableFuture with its complete() and completeExceptionally()
 * methods.
 */
public final class WithResolvers {
    private WithResolvers() {}

    /**
     * Result container holding a CompletableFuture and its completion methods.
     */
    public record Resolvers<T>(
            CompletableFuture<T> promise,
            Consumer<T> resolve,
            Consumer<Throwable> reject
    ) {
        /**
         * Complete the promise successfully with a value.
         */
        public void complete(T value) {
            promise.complete(value);
        }

        /**
         * Complete the promise exceptionally with an error.
         */
        public void completeExceptionally(Throwable error) {
            promise.completeExceptionally(error);
        }
    }

    /**
     * Create a CompletableFuture with exposed resolve and reject functions.
     * This is equivalent to Promise.withResolvers() in JavaScript.
     */
    public static <T> Resolvers<T> withResolvers() {
        CompletableFuture<T> promise = new CompletableFuture<>();

        Consumer<T> resolve = promise::complete;
        Consumer<Throwable> reject = promise::completeExceptionally;

        return new Resolvers<>(promise, resolve, reject);
    }

    /**
     * Create a resolver that can only be resolved once.
     */
    public static <T> Resolvers<T> withResolversOnce() {
        CompletableFuture<T> promise = new CompletableFuture<>();

        // These consumers ensure the promise is only completed once
        Consumer<T> resolve = value -> {
            if (!promise.isDone()) {
                promise.complete(value);
            }
        };

        Consumer<Throwable> reject = error -> {
            if (!promise.isDone()) {
                promise.completeExceptionally(error);
            }
        };

        return new Resolvers<>(promise, resolve, reject);
    }

    /**
     * Create a resolver with timeout support.
     */
    public static <T> Resolvers<T> withResolversAndTimeout(long timeoutMs) {
        CompletableFuture<T> promise = new CompletableFuture<>();

        // Set up timeout
        promise.orTimeout(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        Consumer<T> resolve = promise::complete;
        Consumer<Throwable> reject = promise::completeExceptionally;

        return new Resolvers<>(promise, resolve, reject);
    }

    /**
     * Create a deferred promise - useful for lazy resolution.
     */
    public static <T> DeferredPromise<T> deferred() {
        return new DeferredPromise<>();
    }

    /**
     * Deferred promise that allows external resolution.
     */
    public static final class DeferredPromise<T> {
        private final CompletableFuture<T> promise = new CompletableFuture<>();

        /**
         * Get the underlying promise.
         */
        public CompletableFuture<T> promise() {
            return promise;
        }

        /**
         * Resolve the promise with a value.
         */
        public void resolve(T value) {
            promise.complete(value);
        }

        /**
         * Reject the promise with an error.
         */
        public void reject(Throwable error) {
            promise.completeExceptionally(error);
        }

        /**
         * Check if the promise is done.
         */
        public boolean isDone() {
            return promise.isDone();
        }

        /**
         * Check if the promise is completed successfully.
         */
        public boolean isCompletedSuccessfully() {
            return promise.isDone() && !promise.isCompletedExceptionally();
        }

        /**
         * Check if the promise is completed exceptionally.
         */
        public boolean isCompletedExceptionally() {
            return promise.isCompletedExceptionally();
        }
    }
}