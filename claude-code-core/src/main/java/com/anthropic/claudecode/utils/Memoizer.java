/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code memoizer utilities
 */
package com.anthropic.claudecode.utils;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Advanced memoization utilities.
 */
public final class Memoizer {
    private Memoizer() {}

    /**
     * Memoize supplier.
     */
    public static <T> Supplier<T> memoize(Supplier<T> supplier) {
        return new MemoizedSupplier<>(supplier);
    }

    /**
     * Memoize function.
     */
    public static <T, R> Function<T, R> memoize(Function<T, R> function) {
        return new MemoizedFunction<>(function);
    }

    /**
     * Memoize with TTL.
     */
    public static <T> Supplier<T> memoizeWithTtl(Supplier<T> supplier, Duration ttl) {
        return new TtlMemoizedSupplier<>(supplier, ttl);
    }

    /**
     * Memoize function with TTL.
     */
    public static <T, R> Function<T, R> memoizeWithTtl(Function<T, R> function, Duration ttl) {
        return new TtlMemoizedFunction<>(function, ttl);
    }

    /**
     * Memoize with size limit.
     */
    public static <T, R> Function<T, R> memoizeWithLimit(Function<T, R> function, int maxSize) {
        return new BoundedMemoizedFunction<>(function, maxSize);
    }

    /**
     * Memoize with custom key.
     */
    public static <T, K, R> Function<T, R> memoizeWithKey(
            Function<T, R> function,
            Function<T, K> keyExtractor) {
        return new KeyedMemoizedFunction<>(function, keyExtractor);
    }

    /**
     * Memoized supplier.
     */
    public static final class MemoizedSupplier<T> implements Supplier<T> {
        private final Supplier<T> delegate;
        private volatile T cachedValue;
        private volatile boolean computed = false;

        public MemoizedSupplier(Supplier<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T get() {
            if (!computed) {
                synchronized (this) {
                    if (!computed) {
                        cachedValue = delegate.get();
                        computed = true;
                    }
                }
            }
            return cachedValue;
        }

        public void invalidate() {
            synchronized (this) {
                computed = false;
                cachedValue = null;
            }
        }

        public boolean isComputed() {
            return computed;
        }
    }

    /**
     * Memoized function.
     */
    public static final class MemoizedFunction<T, R> implements Function<T, R> {
        private final Function<T, R> delegate;
        private final Map<T, R> cache = new ConcurrentHashMap<>();

        public MemoizedFunction(Function<T, R> delegate) {
            this.delegate = delegate;
        }

        @Override
        public R apply(T t) {
            return cache.computeIfAbsent(t, delegate);
        }

        public void invalidate(T key) {
            cache.remove(key);
        }

        public void invalidateAll() {
            cache.clear();
        }

        public int size() {
            return cache.size();
        }

        public boolean contains(T key) {
            return cache.containsKey(key);
        }
    }

    /**
     * TTL memoized supplier.
     */
    public static final class TtlMemoizedSupplier<T> implements Supplier<T> {
        private final Supplier<T> delegate;
        private final Duration ttl;
        private volatile T cachedValue;
        private volatile Instant expiry;
        private final Object lock = new Object();

        public TtlMemoizedSupplier(Supplier<T> delegate, Duration ttl) {
            this.delegate = delegate;
            this.ttl = ttl;
        }

        @Override
        public T get() {
            Instant now = Instant.now();
            if (expiry == null || now.isAfter(expiry)) {
                synchronized (lock) {
                    if (expiry == null || now.isAfter(expiry)) {
                        cachedValue = delegate.get();
                        expiry = now.plus(ttl);
                    }
                }
            }
            return cachedValue;
        }

        public void invalidate() {
            synchronized (lock) {
                expiry = null;
                cachedValue = null;
            }
        }

        public Duration remainingTime() {
            if (expiry == null) return Duration.ZERO;
            Duration remaining = Duration.between(Instant.now(), expiry);
            return remaining.isNegative() ? Duration.ZERO : remaining;
        }
    }

    /**
     * TTL memoized function.
     */
    public static final class TtlMemoizedFunction<T, R> implements Function<T, R> {
        private final Function<T, R> delegate;
        private final Duration ttl;
        private final Map<T, TtlEntry<R>> cache = new ConcurrentHashMap<>();

        public TtlMemoizedFunction(Function<T, R> delegate, Duration ttl) {
            this.delegate = delegate;
            this.ttl = ttl;
        }

        @Override
        public R apply(T t) {
            return cache.compute(t, (key, entry) -> {
                if (entry != null && !entry.isExpired()) {
                    return entry;
                }
                return new TtlEntry<>(delegate.apply(key), Instant.now().plus(ttl));
            }).value;
        }

        public void invalidate(T key) {
            cache.remove(key);
        }

        public void invalidateAll() {
            cache.clear();
        }

        public void cleanExpired() {
            cache.entrySet().removeIf(e -> e.getValue().isExpired());
        }

        private record TtlEntry<R>(R value, Instant expiry) {
            boolean isExpired() {
                return Instant.now().isAfter(expiry);
            }
        }
    }

    /**
     * Bounded memoized function.
     */
    public static final class BoundedMemoizedFunction<T, R> implements Function<T, R> {
        private final Function<T, R> delegate;
        private final int maxSize;
        private final Map<T, R> cache;
        private final java.util.Queue<T> order = new java.util.LinkedList<>();

        public BoundedMemoizedFunction(Function<T, R> delegate, int maxSize) {
            this.delegate = delegate;
            this.maxSize = maxSize;
            this.cache = new ConcurrentHashMap<>();
        }

        @Override
        public R apply(T t) {
            return cache.computeIfAbsent(t, key -> {
                synchronized (order) {
                    if (cache.size() >= maxSize && !cache.containsKey(key)) {
                        T oldest = order.poll();
                        if (oldest != null) {
                            cache.remove(oldest);
                        }
                    }
                    order.add(key);
                    return delegate.apply(key);
                }
            });
        }

        public void invalidate(T key) {
            cache.remove(key);
            synchronized (order) {
                order.remove(key);
            }
        }

        public void invalidateAll() {
            cache.clear();
            synchronized (order) {
                order.clear();
            }
        }
    }

    /**
     * Keyed memoized function.
     */
    public static final class KeyedMemoizedFunction<T, K, R> implements Function<T, R> {
        private final Function<T, R> delegate;
        private final Function<T, K> keyExtractor;
        private final Map<K, R> cache = new ConcurrentHashMap<>();

        public KeyedMemoizedFunction(Function<T, R> delegate, Function<T, K> keyExtractor) {
            this.delegate = delegate;
            this.keyExtractor = keyExtractor;
        }

        @Override
        public R apply(T t) {
            K key = keyExtractor.apply(t);
            return cache.computeIfAbsent(key, k -> delegate.apply(t));
        }

        public void invalidateKey(K key) {
            cache.remove(key);
        }

        public void invalidateAll() {
            cache.clear();
        }
    }

    /**
     * Async memoized supplier.
     */
    public static final class AsyncMemoizedSupplier<T> implements Supplier<CompletableFuture<T>> {
        private final Supplier<T> delegate;
        private CompletableFuture<T> future;

        public AsyncMemoizedSupplier(Supplier<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public CompletableFuture<T> get() {
            if (future == null) {
                synchronized (this) {
                    if (future == null) {
                        future = CompletableFuture.supplyAsync(delegate);
                    }
                }
            }
            return future;
        }

        public void invalidate() {
            synchronized (this) {
                future = null;
            }
        }
    }

    /**
     * Memoizer statistics.
     */
    public record MemoStats(int hits, int misses, int size) {
        public double hitRate() {
            int total = hits + misses;
            return total > 0 ? (double) hits / total : 0;
        }
    }
}