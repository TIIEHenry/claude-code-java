/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/memoize.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * LRU cache utilities for memoization.
 */
public final class Memoize {
    private Memoize() {}

    /**
     * Create a memoized supplier with LRU cache.
     */
    public static <T> Supplier<T> memoize(Supplier<T> supplier) {
        return memoizeWithLRU(supplier, 100);
    }

    /**
     * Create a memoized supplier with LRU cache of specified size.
     */
    public static <T> Supplier<T> memoizeWithLRU(Supplier<T> supplier, int maxSize) {
        return new MemoizedSupplier<>(supplier, maxSize);
    }

    /**
     * Create a memoized function with LRU cache.
     */
    public static <K, V> Function<K, V> memoizeWithLRU(Function<K, V> function, int maxSize) {
        return new MemoizedFunction<>(function, maxSize);
    }

    /**
     * Memoized supplier implementation.
     */
    private static class MemoizedSupplier<T> implements Supplier<T> {
        private final Supplier<T> delegate;
        private volatile T cachedValue;
        private volatile boolean computed = false;

        MemoizedSupplier(Supplier<T> delegate, int maxSize) {
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
    }

    /**
     * Memoized function implementation with LRU cache.
     */
    private static class MemoizedFunction<K, V> implements Function<K, V> {
        private final Function<K, V> delegate;
        private final LRUCache<K, V> cache;

        MemoizedFunction(Function<K, V> delegate, int maxSize) {
            this.delegate = delegate;
            this.cache = new LRUCache<>(maxSize);
        }

        @Override
        public V apply(K key) {
            return cache.computeIfAbsent(key, delegate);
        }

        public LRUCache<K, V> getCache() {
            return cache;
        }
    }

    /**
     * Simple LRU cache implementation.
     */
    public static class LRUCache<K, V> {
        private final int maxSize;
        private final LinkedHashMap<K, V> cache;

        public LRUCache(int maxSize) {
            this.maxSize = maxSize;
            this.cache = new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > LRUCache.this.maxSize;
                }
            };
        }

        public V get(K key) {
            synchronized (cache) {
                return cache.get(key);
            }
        }

        public V put(K key, V value) {
            synchronized (cache) {
                cache.put(key, value);
                return value;
            }
        }

        public V computeIfAbsent(K key, Function<K, V> function) {
            synchronized (cache) {
                V value = cache.get(key);
                if (value == null) {
                    value = function.apply(key);
                    cache.put(key, value);
                }
                return value;
            }
        }

        public boolean containsKey(K key) {
            synchronized (cache) {
                return cache.containsKey(key);
            }
        }

        public void remove(K key) {
            synchronized (cache) {
                cache.remove(key);
            }
        }

        public void clear() {
            synchronized (cache) {
                cache.clear();
            }
        }

        public int size() {
            synchronized (cache) {
                return cache.size();
            }
        }

        public Set<K> keySet() {
            synchronized (cache) {
                return new HashSet<>(cache.keySet());
            }
        }

        public Collection<V> values() {
            synchronized (cache) {
                return new ArrayList<>(cache.values());
            }
        }
    }

    /**
     * Time-based memoization - caches for a specific duration.
     */
    public static <T> Supplier<T> memoizeWithExpiry(Supplier<T> supplier, long expiryMs) {
        return new ExpiringMemoizedSupplier<>(supplier, expiryMs);
    }

    private static class ExpiringMemoizedSupplier<T> implements Supplier<T> {
        private final Supplier<T> delegate;
        private final long expiryMs;
        private volatile T cachedValue;
        private volatile long lastComputed = 0;

        ExpiringMemoizedSupplier(Supplier<T> delegate, long expiryMs) {
            this.delegate = delegate;
            this.expiryMs = expiryMs;
        }

        @Override
        public T get() {
            long now = System.currentTimeMillis();
            if (cachedValue == null || (now - lastComputed) > expiryMs) {
                synchronized (this) {
                    now = System.currentTimeMillis();
                    if (cachedValue == null || (now - lastComputed) > expiryMs) {
                        cachedValue = delegate.get();
                        lastComputed = now;
                    }
                }
            }
            return cachedValue;
        }
    }

    /**
     * Memoize with key extractor.
     */
    public static <K, V> Function<K, V> memoize(Function<K, V> function) {
        return memoizeWithLRU(function, 100);
    }
}