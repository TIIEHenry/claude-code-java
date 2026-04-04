/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/memoize
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Memoization utilities - Caching function results.
 */
public final class MemoizeUtils {

    /**
     * Memoize a supplier with no expiration.
     */
    public static <T> Supplier<T> memoize(Supplier<T> supplier) {
        return new MemoizedSupplier<>(supplier);
    }

    /**
     * Memoize a supplier with TTL.
     */
    public static <T> Supplier<T> memoizeWithTTL(Supplier<T> supplier, long ttlMillis) {
        return new MemoizedSupplierWithTTL<>(supplier, ttlMillis);
    }

    /**
     * Memoize a function with no expiration.
     */
    public static <T, R> Function<T, R> memoize(Function<T, R> function) {
        return new MemoizedFunction<>(function);
    }

    /**
     * Memoize a function with TTL.
     */
    public static <T, R> Function<T, R> memoizeWithTTL(Function<T, R> function, long ttlMillis) {
        return new MemoizedFunctionWithTTL<>(function, ttlMillis);
    }

    /**
     * Memoize an async supplier.
     */
    public static <T> Supplier<CompletableFuture<T>> memoizeAsync(Supplier<CompletableFuture<T>> supplier) {
        return new MemoizedAsyncSupplier<>(supplier);
    }

    /**
     * Memoized supplier implementation.
     */
    private static class MemoizedSupplier<T> implements Supplier<T> {
        private final Supplier<T> delegate;
        private volatile T cached;
        private volatile boolean computed = false;

        public MemoizedSupplier(Supplier<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T get() {
            if (!computed) {
                synchronized (this) {
                    if (!computed) {
                        cached = delegate.get();
                        computed = true;
                    }
                }
            }
            return cached;
        }
    }

    /**
     * Memoized supplier with TTL implementation.
     */
    private static class MemoizedSupplierWithTTL<T> implements Supplier<T> {
        private final Supplier<T> delegate;
        private final long ttlMillis;
        private volatile T cached;
        private volatile long cachedTime = 0;

        public MemoizedSupplierWithTTL(Supplier<T> delegate, long ttlMillis) {
            this.delegate = delegate;
            this.ttlMillis = ttlMillis;
        }

        @Override
        public T get() {
            long now = System.currentTimeMillis();
            if (cached == null || now - cachedTime > ttlMillis) {
                synchronized (this) {
                    now = System.currentTimeMillis();
                    if (cached == null || now - cachedTime > ttlMillis) {
                        cached = delegate.get();
                        cachedTime = now;
                    }
                }
            }
            return cached;
        }
    }

    /**
     * Memoized function implementation.
     */
    private static class MemoizedFunction<T, R> implements Function<T, R> {
        private final Function<T, R> delegate;
        private final ConcurrentHashMap<T, R> cache = new ConcurrentHashMap<>();

        public MemoizedFunction(Function<T, R> delegate) {
            this.delegate = delegate;
        }

        @Override
        public R apply(T t) {
            return cache.computeIfAbsent(t, delegate);
        }
    }

    /**
     * Memoized function with TTL implementation.
     */
    private static class MemoizedFunctionWithTTL<T, R> implements Function<T, R> {
        private final Function<T, R> delegate;
        private final long ttlMillis;
        private final ConcurrentHashMap<T, CachedEntry<R>> cache = new ConcurrentHashMap<>();

        public MemoizedFunctionWithTTL(Function<T, R> delegate, long ttlMillis) {
            this.delegate = delegate;
            this.ttlMillis = ttlMillis;
        }

        @Override
        public R apply(T t) {
            long now = System.currentTimeMillis();
            CachedEntry<R> entry = cache.get(t);

            if (entry == null || now - entry.timestamp > ttlMillis) {
                R result = delegate.apply(t);
                cache.put(t, new CachedEntry<>(result, now));
                return result;
            }

            return entry.value;
        }
    }

    /**
     * Memoized async supplier implementation.
     */
    private static class MemoizedAsyncSupplier<T> implements Supplier<CompletableFuture<T>> {
        private final Supplier<CompletableFuture<T>> delegate;
        private volatile CompletableFuture<T> cached;

        public MemoizedAsyncSupplier(Supplier<CompletableFuture<T>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public CompletableFuture<T> get() {
            if (cached == null) {
                synchronized (this) {
                    if (cached == null) {
                        cached = delegate.get();
                    }
                }
            }
            return cached;
        }
    }

    /**
     * Cached entry record.
     */
    private record CachedEntry<R>(R value, long timestamp) {}

    /**
     * Create an LRU cache.
     */
    public static <K, V> LRUCache<K, V> createLRUCache(int maxSize) {
        return new LRUCache<>(maxSize);
    }

    /**
     * LRU Cache implementation with configurable max size.
     * Evicts least recently used entries when capacity is exceeded.
     */
    public static final class LRUCache<K, V> {
        private final LinkedHashMap<K, V> cache;
        private final int maxSize;

        public LRUCache(int maxSize) {
            this.maxSize = maxSize;
            // Access-order mode for LRU eviction
            this.cache = new LinkedHashMap<>(maxSize, 0.75f, true);
        }

        public V get(K key) {
            return cache.get(key);
        }

        public V peek(K key) {
            // Peek without updating recency
            if (!cache.containsKey(key)) return null;
            // Need to iterate to peek without access-order update
            for (Map.Entry<K, V> entry : cache.entrySet()) {
                if (entry.getKey().equals(key)) {
                    return entry.getValue();
                }
            }
            return null;
        }

        public void put(K key, V value) {
            cache.put(key, value);
            // Evict oldest if over capacity
            while (cache.size() > maxSize) {
                Iterator<K> it = cache.keySet().iterator();
                if (it.hasNext()) {
                    it.next();
                    it.remove();
                }
            }
        }

        public boolean containsKey(K key) {
            return cache.containsKey(key);
        }

        public boolean remove(K key) {
            return cache.remove(key) != null;
        }

        public void clear() {
            cache.clear();
        }

        public int size() {
            return cache.size();
        }

        public int maxSize() {
            return maxSize;
        }

        public Map<K, V> asMap() {
            return new LinkedHashMap<>(cache);
        }
    }

    /**
     * Memoize a function with LRU eviction policy.
     * Prevents unbounded memory growth by evicting least recently used entries.
     */
    public static <T, R> Function<T, R> memoizeWithLRU(Function<T, R> function, int maxCacheSize) {
        return new MemoizedFunctionWithLRU<>(function, maxCacheSize);
    }

    /**
     * Memoized function with LRU eviction.
     */
    private static class MemoizedFunctionWithLRU<T, R> implements Function<T, R> {
        private final Function<T, R> delegate;
        private final LRUCache<T, R> cache;

        public MemoizedFunctionWithLRU(Function<T, R> delegate, int maxCacheSize) {
            this.delegate = delegate;
            this.cache = new LRUCache<>(maxCacheSize);
        }

        @Override
        public R apply(T t) {
            R cached = cache.peek(t);
            if (cached != null) {
                return cached;
            }

            R result = delegate.apply(t);
            cache.put(t, result);
            return result;
        }

        public void clear() {
            cache.clear();
        }

        public int size() {
            return cache.size();
        }
    }
}