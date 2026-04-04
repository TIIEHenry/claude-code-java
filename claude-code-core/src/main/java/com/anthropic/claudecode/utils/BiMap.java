/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code bi-map
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;

/**
 * Bi-directional map - maintains inverse mapping.
 */
public final class BiMap<K, V> {
    private final Map<K, V> forward;
    private final Map<V, K> inverse;

    public BiMap() {
        this(new HashMap<>(), new HashMap<>());
    }

    public BiMap(Map<K, V> forward, Map<V, K> inverse) {
        this.forward = forward;
        this.inverse = inverse;
    }

    /**
     * Put key-value pair.
     */
    public V put(K key, V value) {
        // Remove existing mappings
        V oldValue = forward.remove(key);
        if (oldValue != null) {
            inverse.remove(oldValue);
        }

        K oldKey = inverse.remove(value);
        if (oldKey != null) {
            forward.remove(oldKey);
        }

        forward.put(key, value);
        inverse.put(value, key);
        return oldValue;
    }

    /**
     * Put if absent.
     */
    public V putIfAbsent(K key, V value) {
        if (forward.containsKey(key) || inverse.containsKey(value)) {
            return forward.get(key);
        }
        forward.put(key, value);
        inverse.put(value, key);
        return null;
    }

    /**
     * Force put (allows duplicate values).
     */
    public V forcePut(K key, V value) {
        V oldValue = forward.put(key, value);
        if (oldValue != null) {
            inverse.remove(oldValue);
        }
        inverse.put(value, key);
        return oldValue;
    }

    /**
     * Get value by key.
     */
    public V get(K key) {
        return forward.get(key);
    }

    /**
     * Get key by value.
     */
    public K getKey(V value) {
        return inverse.get(value);
    }

    /**
     * Get or default.
     */
    public V getOrDefault(K key, V defaultValue) {
        return forward.getOrDefault(key, defaultValue);
    }

    /**
     * Get key or default.
     */
    public K getKeyOrDefault(V value, K defaultKey) {
        return inverse.getOrDefault(value, defaultKey);
    }

    /**
     * Contains key.
     */
    public boolean containsKey(K key) {
        return forward.containsKey(key);
    }

    /**
     * Contains value.
     */
    public boolean containsValue(V value) {
        return inverse.containsKey(value);
    }

    /**
     * Remove by key.
     */
    public V remove(K key) {
        V value = forward.remove(key);
        if (value != null) {
            inverse.remove(value);
        }
        return value;
    }

    /**
     * Remove by value.
     */
    public K removeValue(V value) {
        K key = inverse.remove(value);
        if (key != null) {
            forward.remove(key);
        }
        return key;
    }

    /**
     * Size.
     */
    public int size() {
        return forward.size();
    }

    /**
     * Is empty.
     */
    public boolean isEmpty() {
        return forward.isEmpty();
    }

    /**
     * Clear.
     */
    public void clear() {
        forward.clear();
        inverse.clear();
    }

    /**
     * Get all keys.
     */
    public Set<K> keys() {
        return forward.keySet();
    }

    /**
     * Get all values.
     */
    public Set<V> values() {
        return inverse.keySet();
    }

    /**
     * Get all entries.
     */
    public Set<Map.Entry<K, V>> entries() {
        return forward.entrySet();
    }

    /**
     * Get inverse view.
     */
    public BiMap<V, K> inverse() {
        return new BiMap<>(inverse, forward);
    }

    /**
     * For each.
     */
    public void forEach(BiConsumer<K, V> action) {
        forward.forEach(action);
    }

    /**
     * To map.
     */
    public Map<K, V> toMap() {
        return new HashMap<>(forward);
    }

    @Override
    public String toString() {
        return forward.toString();
    }

    /**
     * Bi-map utilities.
     */
    public static final class BiMapUtils {
        private BiMapUtils() {}

        /**
         * Create from map.
         */
        public static <K, V> BiMap<K, V> fromMap(Map<K, V> map) {
            BiMap<K, V> biMap = new BiMap<>();
            map.forEach(biMap::put);
            return biMap;
        }

        /**
         * Create from pairs.
         */
        @SafeVarargs
        public static <K, V> BiMap<K, V> of(Map.Entry<K, V>... entries) {
            BiMap<K, V> biMap = new BiMap<>();
            for (Map.Entry<K, V> entry : entries) {
                biMap.put(entry.getKey(), entry.getValue());
            }
            return biMap;
        }

        /**
         * Create with entries.
         */
        public static <K, V> BiMap<K, V> of(K k1, V v1) {
            BiMap<K, V> biMap = new BiMap<>();
            biMap.put(k1, v1);
            return biMap;
        }

        public static <K, V> BiMap<K, V> of(K k1, V v1, K k2, V v2) {
            BiMap<K, V> biMap = new BiMap<>();
            biMap.put(k1, v1);
            biMap.put(k2, v2);
            return biMap;
        }

        public static <K, V> BiMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
            BiMap<K, V> biMap = new BiMap<>();
            biMap.put(k1, v1);
            biMap.put(k2, v2);
            biMap.put(k3, v3);
            return biMap;
        }
    }
}