/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/sequential
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

/**
 * Sequential execution wrapper - Prevents race conditions.
 *
 * Ensures that concurrent calls to the wrapped function are executed one at a time
 * in the order they were received, while preserving the correct return values.
 */
public final class Sequential {
    /**
     * Create a sequential execution wrapper for async functions.
     */
    public static <T, R> Function<T, CompletableFuture<R>> sequential(
        Function<T, CompletableFuture<R>> fn
    ) {
        final java.util.Queue<QueueItem<T, R>> queue = new ConcurrentLinkedQueue<>();
        final AtomicBoolean processing = new AtomicBoolean(false);

        return arg -> {
            CompletableFuture<R> future = new CompletableFuture<>();
            queue.add(new QueueItem<>(arg, future));

            processQueue(queue, processing, fn);

            return future;
        };
    }

    /**
     * Create a sequential execution wrapper for suppliers.
     */
    public static <R> Supplier<CompletableFuture<R>> sequentialSupplier(
        Supplier<CompletableFuture<R>> fn
    ) {
        final java.util.Queue<QueueItem<Void, R>> queue = new ConcurrentLinkedQueue<>();
        final AtomicBoolean processing = new AtomicBoolean(false);

        return () -> {
            CompletableFuture<R> future = new CompletableFuture<>();
            queue.add(new QueueItem<>(null, future));

            processQueue(queue, processing, arg -> fn.get());

            return future;
        };
    }

    /**
     * Create a sequential execution wrapper for runnables.
     */
    public static Runnable sequentialRunnable(Runnable fn) {
        final java.util.Queue<CompletableFuture<Void>> queue = new ConcurrentLinkedQueue<>();
        final AtomicBoolean processing = new AtomicBoolean(false);

        return () -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            queue.add(future);

            processQueueVoid(queue, processing, fn);
        };
    }

    // Helper methods
    private static <T, R> void processQueue(
        java.util.Queue<QueueItem<T, R>> queue,
        AtomicBoolean processing,
        Function<T, CompletableFuture<R>> fn
    ) {
        if (!processing.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            while (!queue.isEmpty()) {
                QueueItem<T, R> item = queue.poll();
                if (item == null) break;

                try {
                    CompletableFuture<R> result = fn.apply(item.arg);
                    result.whenComplete((r, error) -> {
                        if (error != null) {
                            item.future.completeExceptionally(error);
                        } else {
                            item.future.complete(r);
                        }
                    });
                    result.join(); // Wait for completion
                } catch (Exception e) {
                    item.future.completeExceptionally(e);
                }
            }

            processing.set(false);

            // Check if new items were added while processing
            if (!queue.isEmpty()) {
                processQueue(queue, processing, fn);
            }
        });
    }

    private static void processQueueVoid(
        java.util.Queue<CompletableFuture<Void>> queue,
        AtomicBoolean processing,
        Runnable fn
    ) {
        if (!processing.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            while (!queue.isEmpty()) {
                CompletableFuture<Void> item = queue.poll();
                if (item == null) break;

                try {
                    fn.run();
                    item.complete(null);
                } catch (Exception e) {
                    item.completeExceptionally(e);
                }
            }

            processing.set(false);

            if (!queue.isEmpty()) {
                processQueueVoid(queue, processing, fn);
            }
        });
    }

    /**
     * Queue item for sequential execution.
     */
    private static final class QueueItem<T, R> {
        final T arg;
        final CompletableFuture<R> future;

        QueueItem(T arg, CompletableFuture<R> future) {
            this.arg = arg;
            this.future = future;
        }
    }
}