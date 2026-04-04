/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code multi-map
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;

/**
 * Multi-map - maps keys to multiple values.
 */
public final class MultiMap<K, V> {
    private final Map<K, Collection<V>> map;
    private final Supplier<Collection<V>> collectionFactory;
    private int totalSize = 0;

    public MultiMap() {
        this(HashMap::new, ArrayList::new);
    }

    public MultiMap(Supplier<Map<K, Collection<V>>> mapFactory,
            Supplier<Collection<V>> collectionFactory) {
        this.map = mapFactory.get();
        this.collectionFactory = collectionFactory;
    }

    /**
     * Put value for key.
     */
    public void put(K key, V value) {
        map.computeIfAbsent(key, k -> collectionFactory.get()).add(value);
        totalSize++;
    }

    /**
     * Put all values for key.
     */
    public void putAll(K key, Collection<V> values) {
        Collection<V> collection = map.computeIfAbsent(key, k -> collectionFactory.get());
        collection.addAll(values);
        totalSize += values.size();
    }

    /**
     * Put all from another multi-map.
     */
    public void putAll(MultiMap<K, V> other) {
        for (Map.Entry<K, Collection<V>> entry : other.map.entrySet()) {
            putAll(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Get values for key.
     */
    public Collection<V> get(K key) {
        return map.getOrDefault(key, Collections.emptyList());
    }

    /**
     * Get first value for key.
     */
    public Optional<V> getFirst(K key) {
        Collection<V> values = map.get(key);
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        return values.stream().findFirst();
    }

    /**
     * Remove key.
     */
    public Collection<V> remove(K key) {
        Collection<V> removed = map.remove(key);
        if (removed != null) {
            totalSize -= removed.size();
        }
        return removed != null ? removed : Collections.emptyList();
    }

    /**
     * Remove value from key.
     */
    public boolean remove(K key, V value) {
        Collection<V> values = map.get(key);
        if (values != null && values.remove(value)) {
            totalSize--;
            if (values.isEmpty()) {
                map.remove(key);
            }
            return true;
        }
        return false;
    }

    /**
     * Contains key.
     */
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    /**
     * Contains value.
     */
    public boolean containsValue(K key, V value) {
        Collection<V> values = map.get(key);
        return values != null && values.contains(value);
    }

    /**
     * Contains value anywhere.
     */
    public boolean containsValue(V value) {
        return map.values().stream()
            .flatMap(Collection::stream)
            .anyMatch(v -> Objects.equals(v, value));
    }

    /**
     * Key count.
     */
    public int keyCount() {
        return map.size();
    }

    /**
     * Total value count.
     */
    public int size() {
        return totalSize;
    }

    /**
     * Is empty.
     */
    public boolean isEmpty() {
        return totalSize == 0;
    }

    /**
     * Clear.
     */
    public void clear() {
        map.clear();
        totalSize = 0;
    }

    /**
     * Get all keys.
     */
    public Set<K> keys() {
        return map.keySet();
    }

    /**
     * Get all values.
     */
    public Collection<V> values() {
        List<V> result = new ArrayList<>(totalSize);
        map.values().forEach(result::addAll);
        return result;
    }

    /**
     * Get all entries.
     */
    public Set<Map.Entry<K, Collection<V>>> entries() {
        return map.entrySet();
    }

    /**
     * For each key-values pair.
     */
    public void forEach(BiConsumer<K, Collection<V>> action) {
        map.forEach(action);
    }

    /**
     * For each key-value pair.
     */
    public void forEachValue(BiConsumer<K, V> action) {
        map.forEach((key, values) -> {
            for (V value : values) {
                action.accept(key, value);
            }
        });
    }

    /**
     * Count values for key.
     */
    public int count(K key) {
        Collection<V> values = map.get(key);
        return values != null ? values.size() : 0;
    }

    /**
     * Invert map.
     */
    public MultiMap<V, K> invert() {
        MultiMap<V, K> inverted = new MultiMap<>();
        forEachValue((k, v) -> inverted.put(v, k));
        return inverted;
    }

    @Override
    public String toString() {
        return map.toString();
    }

    /**
     * Multi-map utilities.
     */
    public static final class MultiMapUtils {
        private MultiMapUtils() {}

        /**
         * Create with ArrayList values.
         */
        public static <K, V> MultiMap<K, V> arrayList() {
            return new MultiMap<>();
        }

        /**
         * Create with HashSet values.
         */
        public static <K, V> MultiMap<K, V> hashSet() {
            return new MultiMap<>(HashMap::new, HashSet::new);
        }

        /**
         * Create with LinkedHashSet values (preserves order).
         */
        public static <K, V> MultiMap<K, V> linkedHashSet() {
            return new MultiMap<>(HashMap::new, LinkedHashSet::new);
        }

        /**
         * Create from pairs.
         */
        public static <K, V> MultiMap<K, V> fromPairs(List<Map.Entry<K, V>> pairs) {
            MultiMap<K, V> mm = new MultiMap<>();
            for (Map.Entry<K, V> entry : pairs) {
                mm.put(entry.getKey(), entry.getValue());
            }
            return mm;
        }
    }
}