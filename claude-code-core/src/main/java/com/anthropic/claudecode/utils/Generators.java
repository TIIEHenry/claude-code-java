/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code generator utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Generator utilities for async iteration patterns.
 */
public final class Generators {
    private Generators() {}

    /**
     * Get the last element from a stream.
     */
    public static <A> A lastX(java.util.stream.Stream<A> stream) {
        Optional<A> last = stream.reduce((first, second) -> second);
        if (last.isEmpty()) {
            throw new IllegalStateException("No items in stream");
        }
        return last.get();
    }

    /**
     * Get the last element from a list.
     */
    public static <A> A lastX(List<A> list) {
        if (list.isEmpty()) {
            throw new IllegalStateException("No items in list");
        }
        return list.get(list.size() - 1);
    }

    /**
     * Collect all items from a CompletableFuture stream.
     */
    public static <A> CompletableFuture<List<A>> toArray(CompletableFuture<java.util.stream.Stream<A>> streamFuture) {
        return streamFuture.thenApply(stream -> stream.collect(Collectors.toList()));
    }

    /**
     * Create a stream from an array.
     */
    public static <T> java.util.stream.Stream<T> fromArray(T[] values) {
        return Arrays.stream(values);
    }

    /**
     * Create a stream from a collection.
     */
    public static <T> java.util.stream.Stream<T> fromCollection(Collection<T> values) {
        return values.stream();
    }

    /**
     * Run all suppliers concurrently up to a concurrency cap, returning results as they complete.
     */
    public static <A> CompletableFuture<List<A>> all(
            List<Supplier<CompletableFuture<A>>> suppliers,
            int concurrencyCap) {
        List<A> results = new ArrayList<>();
        Semaphore semaphore = new Semaphore(concurrencyCap);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Supplier<CompletableFuture<A>> supplier : suppliers) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    semaphore.acquire();
                    A result = supplier.get().join();
                    synchronized (results) {
                        results.add(result);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release();
                }
            });
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> results);
    }

    /**
     * Run all suppliers concurrently with unlimited concurrency.
     */
    public static <A> CompletableFuture<List<A>> all(List<Supplier<CompletableFuture<A>>> suppliers) {
        return all(suppliers, Integer.MAX_VALUE);
    }

    /**
     * Run suppliers and yield results as they complete (reactive pattern).
     */
    public static <A> java.util.stream.Stream<A> allYielding(
            List<Supplier<CompletableFuture<A>>> suppliers,
            int concurrencyCap) {
        // Use a blocking queue to yield results as they complete
        BlockingQueue<A> queue = new LinkedBlockingQueue<>();
        Semaphore semaphore = new Semaphore(concurrencyCap);
        AtomicInteger remaining = new AtomicInteger(suppliers.size());

        for (Supplier<CompletableFuture<A>> supplier : suppliers) {
            CompletableFuture.runAsync(() -> {
                try {
                    semaphore.acquire();
                    A result = supplier.get().join();
                    queue.put(result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release();
                    remaining.decrementAndGet();
                }
            });
        }

        // Create a stream that reads from the queue as results become available
        return StreamSupport.stream(
                new Spliterator<A>() {
                    @Override
                    public boolean tryAdvance(Consumer<? super A> action) {
                        if (remaining.get() == 0 && queue.isEmpty()) {
                            return false;
                        }
                        try {
                            A item = queue.poll(100, TimeUnit.MILLISECONDS);
                            if (item != null) {
                                action.accept(item);
                                return true;
                            }
                            return remaining.get() > 0;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                    }

                    @Override
                    public Spliterator<A> trySplit() {
                        return null;
                    }

                    @Override
                    public long estimateSize() {
                        return suppliers.size();
                    }

                    @Override
                    public int characteristics() {
                        return Spliterator.ORDERED | Spliterator.NONNULL;
                    }
                },
                false
        );
    }

    /**
     * Concatenate multiple streams into one.
     */
    public static <T> java.util.stream.Stream<T> concat(List<java.util.stream.Stream<T>> streams) {
        return streams.stream().flatMap(s -> s);
    }

    /**
     * Take first N elements from a stream.
     */
    public static <T> java.util.stream.Stream<T> take(java.util.stream.Stream<T> stream, int n) {
        return stream.limit(n);
    }

    /**
     * Filter stream by predicate.
     */
    public static <T> java.util.stream.Stream<T> filter(java.util.stream.Stream<T> stream, Predicate<T> predicate) {
        return stream.filter(predicate);
    }

    /**
     * Map stream elements.
     */
    public static <T, R> java.util.stream.Stream<R> map(java.util.stream.Stream<T> stream, Function<T, R> mapper) {
        return stream.map(mapper);
    }
}