/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code union-find
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Union-Find (Disjoint Set Union) data structure.
 */
public final class UnionFind<T> {
    private final Map<T, T> parent = new HashMap<>();
    private final Map<T, Integer> rank = new HashMap<>();
    private int componentCount = 0;

    /**
     * Add element to the structure.
     */
    public void add(T element) {
        if (!parent.containsKey(element)) {
            parent.put(element, element);
            rank.put(element, 0);
            componentCount++;
        }
    }

    /**
     * Add all elements.
     */
    public void addAll(Collection<T> elements) {
        elements.forEach(this::add);
    }

    /**
     * Find the root of element.
     */
    public T find(T element) {
        if (!parent.containsKey(element)) {
            add(element);
            return element;
        }

        // Path compression
        if (!parent.get(element).equals(element)) {
            parent.put(element, find(parent.get(element)));
        }
        return parent.get(element);
    }

    /**
     * Union two elements.
     */
    public void union(T a, T b) {
        T rootA = find(a);
        T rootB = find(b);

        if (rootA.equals(rootB)) return;

        // Union by rank
        int rankA = rank.get(rootA);
        int rankB = rank.get(rootB);

        if (rankA < rankB) {
            parent.put(rootA, rootB);
        } else if (rankA > rankB) {
            parent.put(rootB, rootA);
        } else {
            parent.put(rootB, rootA);
            rank.put(rootA, rankA + 1);
        }

        componentCount--;
    }

    /**
     * Check if two elements are connected.
     */
    public boolean connected(T a, T b) {
        return find(a).equals(find(b));
    }

    /**
     * Check if element exists.
     */
    public boolean contains(T element) {
        return parent.containsKey(element);
    }

    /**
     * Get number of elements.
     */
    public int size() {
        return parent.size();
    }

    /**
     * Get number of components.
     */
    public int componentCount() {
        return componentCount;
    }

    /**
     * Get all elements in same component.
     */
    public Set<T> getComponent(T element) {
        T root = find(element);
        Set<T> component = new HashSet<>();
        for (T e : parent.keySet()) {
            if (find(e).equals(root)) {
                component.add(e);
            }
        }
        return component;
    }

    /**
     * Get all components.
     */
    public Collection<Set<T>> getComponents() {
        Map<T, Set<T>> components = new HashMap<>();
        for (T element : parent.keySet()) {
            T root = find(element);
            components.computeIfAbsent(root, k -> new HashSet<>()).add(element);
        }
        return components.values();
    }

    /**
     * Get size of component.
     */
    public int getComponentSize(T element) {
        return getComponent(element).size();
    }

    /**
     * Clear structure.
     */
    public void clear() {
        parent.clear();
        rank.clear();
        componentCount = 0;
    }

    @Override
    public String toString() {
        return String.format("UnionFind[elements=%d, components=%d]", size(), componentCount);
    }

    /**
     * Union-Find utilities.
     */
    public static final class UnionFindUtils {
        private UnionFindUtils() {}

        /**
         * Create from collection.
         */
        public static <T> UnionFind<T> fromCollection(Collection<T> elements) {
            UnionFind<T> uf = new UnionFind<>();
            uf.addAll(elements);
            return uf;
        }

        /**
         * Create with pairs of connected elements.
         */
        public static <T> UnionFind<T> fromPairs(List<Map.Entry<T, T>> pairs) {
            UnionFind<T> uf = new UnionFind<>();
            for (Map.Entry<T, T> pair : pairs) {
                uf.union(pair.getKey(), pair.getValue());
            }
            return uf;
        }

        /**
         * Check if all elements are connected.
         */
        public static <T> boolean isFullyConnected(UnionFind<T> uf) {
            return uf.componentCount() <= 1;
        }

        /**
         * Get number of connections needed to fully connect.
         */
        public static <T> int connectionsNeeded(UnionFind<T> uf) {
            return Math.max(0, uf.componentCount() - 1);
        }
    }
}