/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code caching utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Generic caching utilities.
 */
public final class CacheUtils {
    private CacheUtils() {}

    /**
     * Time-based cache entry.
     */
    public record CacheEntry<T>(T value, long expiryTime) {
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    /**
     * Simple time-based cache.
     */
    public static class SimpleCache<K, V> {
        private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
        private final long ttlMs;
        private final int maxSize;

        public SimpleCache(long ttlMs) {
            this(ttlMs, 1000);
        }

        public SimpleCache(long ttlMs, int maxSize) {
            this.ttlMs = ttlMs;
            this.maxSize = maxSize;
        }

        public V get(K key) {
            CacheEntry<V> entry = cache.get(key);
            if (entry == null) return null;
            if (entry.isExpired()) {
                cache.remove(key);
                return null;
            }
            return entry.value();
        }

        public void put(K key, V value) {
            if (cache.size() >= maxSize) {
                // Remove oldest entries
                cleanup();
            }
            cache.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttlMs));
        }

        public V computeIfAbsent(K key, Supplier<V> supplier) {
            V value = get(key);
            if (value != null) return value;

            value = supplier.get();
            if (value != null) {
                put(key, value);
            }
            return value;
        }

        public void remove(K key) {
            cache.remove(key);
        }

        public void clear() {
            cache.clear();
        }

        public void cleanup() {
            long now = System.currentTimeMillis();
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }

        public int size() {
            return cache.size();
        }

        public boolean containsKey(K key) {
            return get(key) != null;
        }

        public Set<K> keys() {
            return new HashSet<>(cache.keySet());
        }
    }

    /**
     * LRU Cache implementation.
     */
    public static class LruCache<K, V> {
        private final int maxSize;
        private final LinkedHashMap<K, V> cache;

        public LruCache(int maxSize) {
            this.maxSize = maxSize;
            this.cache = new LinkedHashMap<K, V>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > LruCache.this.maxSize;
                }
            };
        }

        public synchronized V get(K key) {
            return cache.get(key);
        }

        public synchronized void put(K key, V value) {
            cache.put(key, value);
        }

        public synchronized V computeIfAbsent(K key, Supplier<V> supplier) {
            V value = cache.get(key);
            if (value != null) return value;
            value = supplier.get();
            cache.put(key, value);
            return value;
        }

        public synchronized void remove(K key) {
            cache.remove(key);
        }

        public synchronized void clear() {
            cache.clear();
        }

        public synchronized int size() {
            return cache.size();
        }

        public synchronized boolean containsKey(K key) {
            return cache.containsKey(key);
        }
    }

    /**
     * Loading cache with async refresh.
     */
    public static class LoadingCache<K, V> {
        private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<K, Long> loadTimes = new ConcurrentHashMap<>();
        private final Function<K, V> loader;
        private final long refreshAfterMs;
        private final long expireAfterMs;

        public LoadingCache(Function<K, V> loader, long refreshAfterMs, long expireAfterMs) {
            this.loader = loader;
            this.refreshAfterMs = refreshAfterMs;
            this.expireAfterMs = expireAfterMs;
        }

        public V get(K key) {
            Long loadTime = loadTimes.get(key);
            long now = System.currentTimeMillis();

            if (loadTime != null) {
                if (now - loadTime > expireAfterMs) {
                    // Expired, reload synchronously
                    return reload(key);
                }
                if (now - loadTime > refreshAfterMs) {
                    // Refresh in background
                    CompletableFuture.runAsync(() -> reload(key));
                }
            }

            return cache.computeIfAbsent(key, k -> {
                loadTimes.put(k, now);
                return loader.apply(k);
            });
        }

        private V reload(K key) {
            long now = System.currentTimeMillis();
            V value = loader.apply(key);
            cache.put(key, value);
            loadTimes.put(key, now);
            return value;
        }

        public void put(K key, V value) {
            cache.put(key, value);
            loadTimes.put(key, System.currentTimeMillis());
        }

        public void invalidate(K key) {
            cache.remove(key);
            loadTimes.remove(key);
        }

        public void invalidateAll() {
            cache.clear();
            loadTimes.clear();
        }
    }

    /**
     * Memoization wrapper for functions.
     */
    public static <T, R> Function<T, R> memoize(Function<T, R> function) {
        Map<T, R> cache = new ConcurrentHashMap<>();
        return t -> cache.computeIfAbsent(t, function);
    }

    /**
     * Memoization with time-based expiration.
     */
    public static <T, R> Function<T, R> memoize(Function<T, R> function, long ttlMs) {
        ConcurrentHashMap<T, CacheEntry<R>> cache = new ConcurrentHashMap<>();
        return t -> {
            CacheEntry<R> entry = cache.get(t);
            if (entry != null && !entry.isExpired()) {
                return entry.value();
            }
            R result = function.apply(t);
            cache.put(t, new CacheEntry<>(result, System.currentTimeMillis() + ttlMs));
            return result;
        };
    }

    /**
     * Memoize a supplier.
     */
    public static <T> Supplier<T> memoize(Supplier<T> supplier) {
        AtomicReference<T> value = new AtomicReference<>();
        AtomicBoolean initialized = new AtomicBoolean();
        return () -> {
            if (!initialized.get()) {
                synchronized (supplier) {
                    if (!initialized.get()) {
                        value.set(supplier.get());
                        initialized.set(true);
                    }
                }
            }
            return value.get();
        };
    }

    /**
     * Create a simple cache.
     */
    public static <K, V> SimpleCache<K, V> createSimpleCache(long ttlMs) {
        return new SimpleCache<>(ttlMs);
    }

    /**
     * Create an LRU cache.
     */
    public static <K, V> LruCache<K, V> createLruCache(int maxSize) {
        return new LruCache<>(maxSize);
    }

    /**
     * Create a loading cache.
     */
    public static <K, V> LoadingCache<K, V> createLoadingCache(
            Function<K, V> loader, long refreshAfterMs, long expireAfterMs) {
        return new LoadingCache<>(loader, refreshAfterMs, expireAfterMs);
    }

    // Helper classes for atomic operations
    private static class AtomicReference<T> extends java.util.concurrent.atomic.AtomicReference<T> {
        public AtomicReference() { super(); }
    }

    private static class AtomicBoolean extends java.util.concurrent.atomic.AtomicBoolean {
        public AtomicBoolean() { super(); }
    }

    /**
     * Two-level cache (L1 memory, L2 optional persistent).
     */
    public static class TwoLevelCache<K, V> {
        private final SimpleCache<K, V> l1;
        private final Function<K, Optional<V>> l2Reader;
        private final BiConsumer<K, V> l2Writer;
        private final Function<K, V> loader;

        public TwoLevelCache(
                long l1TtlMs,
                Function<K, Optional<V>> l2Reader,
                BiConsumer<K, V> l2Writer,
                Function<K, V> loader) {
            this.l1 = new SimpleCache<>(l1TtlMs);
            this.l2Reader = l2Reader;
            this.l2Writer = l2Writer;
            this.loader = loader;
        }

        public V get(K key) {
            // Check L1
            V value = l1.get(key);
            if (value != null) return value;

            // Check L2
            if (l2Reader != null) {
                Optional<V> l2Value = l2Reader.apply(key);
                if (l2Value.isPresent()) {
                    l1.put(key, l2Value.get());
                    return l2Value.get();
                }
            }

            // Load
            value = loader.apply(key);
            if (value != null) {
                l1.put(key, value);
                if (l2Writer != null) {
                    l2Writer.accept(key, value);
                }
            }
            return value;
        }

        public void put(K key, V value) {
            l1.put(key, value);
            if (l2Writer != null) {
                l2Writer.accept(key, value);
            }
        }

        public void invalidate(K key) {
            l1.remove(key);
        }
    }

    /**
     * Statistics tracking cache.
     */
    public static class StatsCache<K, V> {
        private final SimpleCache<K, V> delegate;
        private final AtomicLong hits = new AtomicLong();
        private final AtomicLong misses = new AtomicLong();

        public StatsCache(long ttlMs) {
            this.delegate = new SimpleCache<>(ttlMs);
        }

        public V get(K key) {
            V value = delegate.get(key);
            if (value != null) {
                hits.incrementAndGet();
            } else {
                misses.incrementAndGet();
            }
            return value;
        }

        public void put(K key, V value) {
            delegate.put(key, value);
        }

        public long getHits() { return hits.get(); }
        public long getMisses() { return misses.get(); }
        public long getTotalRequests() { return hits.get() + misses.get(); }

        public double getHitRate() {
            long total = getTotalRequests();
            return total > 0 ? (double) hits.get() / total : 0;
        }

        public void resetStats() {
            hits.set(0);
            misses.set(0);
        }
    }

    private static class AtomicLong extends java.util.concurrent.atomic.AtomicLong {
        public AtomicLong() { super(); }
    }
}