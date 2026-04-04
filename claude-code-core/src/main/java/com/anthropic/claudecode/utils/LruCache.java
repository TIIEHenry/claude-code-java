/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code LRU cache
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * LRU (Least Recently Used) cache implementation.
 */
public class LruCache<K, V> {
    private final int maxSize;
    private final Map<K, V> cache;
    private final LinkedHashMap<K, V> lruMap;
    private long hits = 0;
    private long misses = 0;

    public LruCache(int maxSize) {
        this.maxSize = maxSize;
        this.lruMap = new LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LruCache.this.maxSize;
            }
        };
        this.cache = lruMap;
    }

    /**
     * Get value by key.
     */
    public V get(K key) {
        V value = cache.get(key);
        if (value != null) {
            hits++;
            return value;
        }
        misses++;
        return null;
    }

    /**
     * Get or compute value.
     */
    public V getOrDefault(K key, V defaultValue) {
        V value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get or compute value.
     */
    public V computeIfAbsent(K key, Function<K, V> loader) {
        V value = cache.get(key);
        if (value != null) {
            hits++;
            return value;
        }
        misses++;
        value = loader.apply(key);
        if (value != null) {
            put(key, value);
        }
        return value;
    }

    /**
     * Put key-value pair.
     */
    public void put(K key, V value) {
        cache.put(key, value);
    }

    /**
     * Put if absent.
     */
    public V putIfAbsent(K key, V value) {
        return cache.putIfAbsent(key, value);
    }

    /**
     * Remove key.
     */
    public V remove(K key) {
        return cache.remove(key);
    }

    /**
     * Check if contains key.
     */
    public boolean containsKey(K key) {
        return cache.containsKey(key);
    }

    /**
     * Check if contains value.
     */
    public boolean containsValue(V value) {
        return cache.containsValue(value);
    }

    /**
     * Clear cache.
     */
    public void clear() {
        cache.clear();
        hits = 0;
        misses = 0;
    }

    /**
     * Get size.
     */
    public int size() {
        return cache.size();
    }

    /**
     * Check if empty.
     */
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    /**
     * Get max size.
     */
    public int maxSize() {
        return maxSize;
    }

    /**
     * Get all keys.
     */
    public Set<K> keys() {
        return new LinkedHashSet<>(cache.keySet());
    }

    /**
     * Get all values.
     */
    public Collection<V> values() {
        return new ArrayList<>(cache.values());
    }

    /**
     * Get all entries.
     */
    public Set<Map.Entry<K, V>> entries() {
        return new LinkedHashSet<>(cache.entrySet());
    }

    /**
     * Get hit rate.
     */
    public double hitRate() {
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0;
    }

    /**
     * Get statistics.
     */
    public CacheStats stats() {
        return new CacheStats(size(), maxSize, hits, misses, hitRate());
    }

    /**
     * Cache statistics.
     */
    public record CacheStats(int size, int maxSize, long hits, long misses, double hitRate) {
        public String format() {
            return String.format("LruCache[size=%d/%d, hits=%d, misses=%d, hitRate=%.2f%%]",
                size, maxSize, hits, misses, hitRate * 100);
        }
    }

    /**
     * LRU cache builder.
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * Builder class.
     */
    public static final class Builder<K, V> {
        private int maxSize = 1000;
        private Function<K, V> loader;

        public Builder<K, V> maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder<K, V> loader(Function<K, V> loader) {
            this.loader = loader;
            return this;
        }

        public LruCache<K, V> build() {
            return new LruCache<>(maxSize);
        }

        public LoadingLruCache<K, V> buildLoading() {
            if (loader == null) {
                throw new IllegalStateException("Loader is required for loading cache");
            }
            return new LoadingLruCache<>(maxSize, loader);
        }
    }

    /**
     * Loading LRU cache with automatic loading.
     */
    public static final class LoadingLruCache<K, V> extends LruCache<K, V> {
        private final Function<K, V> loader;

        public LoadingLruCache(int maxSize, Function<K, V> loader) {
            super(maxSize);
            this.loader = loader;
        }

        @Override
        public V get(K key) {
            return computeIfAbsent(key, loader);
        }
    }

    /**
     * Thread-safe LRU cache.
     */
    public static final class ThreadSafeLruCache<K, V> {
        private final LruCache<K, V> cache;

        public ThreadSafeLruCache(int maxSize) {
            this.cache = new LruCache<>(maxSize);
        }

        public synchronized V get(K key) {
            return cache.get(key);
        }

        public synchronized V computeIfAbsent(K key, Function<K, V> loader) {
            return cache.computeIfAbsent(key, loader);
        }

        public synchronized void put(K key, V value) {
            cache.put(key, value);
        }

        public synchronized V remove(K key) {
            return cache.remove(key);
        }

        public synchronized void clear() {
            cache.clear();
        }

        public synchronized int size() {
            return cache.size();
        }

        public synchronized CacheStats stats() {
            return cache.stats();
        }
    }
}