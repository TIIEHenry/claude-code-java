/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code priority queue
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Priority queue with additional features.
 */
public final class PriorityQueue<T> {
    private final PriorityQueueBackend<T> backend;
    private final Comparator<T> comparator;
    private final int capacity;

    public PriorityQueue(Comparator<T> comparator) {
        this(comparator, Integer.MAX_VALUE);
    }

    public PriorityQueue(Comparator<T> comparator, int capacity) {
        this.comparator = comparator;
        this.capacity = capacity;
        this.backend = new PriorityQueueBackend<>(comparator);
    }

    /**
     * Add element.
     */
    public boolean add(T element) {
        return backend.add(element);
    }

    /**
     * Offer element.
     */
    public boolean offer(T element) {
        return backend.offer(element);
    }

    /**
     * Peek at head.
     */
    public T peek() {
        return backend.peek();
    }

    /**
     * Remove and return head.
     */
    public T poll() {
        return backend.poll();
    }

    /**
     * Remove element.
     */
    public boolean remove(T element) {
        return backend.remove(element);
    }

    /**
     * Check if contains.
     */
    public boolean contains(T element) {
        return backend.contains(element);
    }

    /**
     * Get size.
     */
    public int size() {
        return backend.size();
    }

    /**
     * Check if empty.
     */
    public boolean isEmpty() {
        return backend.isEmpty();
    }

    /**
     * Clear queue.
     */
    public void clear() {
        backend.clear();
    }

    /**
     * Drain all elements.
     */
    public List<T> drain() {
        List<T> result = new ArrayList<>();
        T element;
        while ((element = poll()) != null) {
            result.add(element);
        }
        return result;
    }

    /**
     * Drain up to N elements.
     */
    public List<T> drain(int maxElements) {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < maxElements; i++) {
            T element = poll();
            if (element == null) break;
            result.add(element);
        }
        return result;
    }

    /**
     * Drain to collection.
     */
    public int drainTo(Collection<T> collection) {
        int count = 0;
        T element;
        while ((element = poll()) != null) {
            collection.add(element);
            count++;
        }
        return count;
    }

    /**
     * Drain to collection with limit.
     */
    public int drainTo(Collection<T> collection, int maxElements) {
        int count = 0;
        for (int i = 0; i < maxElements; i++) {
            T element = poll();
            if (element == null) break;
            collection.add(element);
            count++;
        }
        return count;
    }

    /**
     * To list (in priority order).
     */
    public List<T> toList() {
        List<T> copy = new ArrayList<>(backend);
        copy.sort(comparator);
        return copy;
    }

    /**
     * Min priority queue.
     */
    public static <T extends Comparable<T>> PriorityQueue<T> minQueue() {
        return new PriorityQueue<T>(Comparator.naturalOrder());
    }

    /**
     * Max priority queue.
     */
    public static <T extends Comparable<T>> PriorityQueue<T> maxQueue() {
        return new PriorityQueue<T>(Comparator.reverseOrder());
    }

    /**
     * Min priority queue with capacity.
     */
    public static <T extends Comparable<T>> PriorityQueue<T> minQueue(int capacity) {
        return new PriorityQueue<T>(Comparator.naturalOrder(), capacity);
    }

    /**
     * Max priority queue with capacity.
     */
    public static <T extends Comparable<T>> PriorityQueue<T> maxQueue(int capacity) {
        return new PriorityQueue<T>(Comparator.reverseOrder(), capacity);
    }

    /**
     * Create with custom comparator.
     */
    public static <T> PriorityQueue<T> of(Comparator<T> comparator) {
        return new PriorityQueue<>(comparator);
    }

    /**
     * Create from collection.
     */
    public static <T extends Comparable<T>> PriorityQueue<T> fromCollection(Collection<T> collection) {
        PriorityQueue<T> queue = minQueue();
        collection.forEach(queue::add);
        return queue;
    }

    /**
     * Backend wrapper.
     */
    private static final class PriorityQueueBackend<T> extends java.util.PriorityQueue<T> {
        public PriorityQueueBackend(Comparator<T> comparator) {
            super(comparator);
        }
    }

    /**
     * Bounded priority queue - rejects lowest priority when full.
     */
    public static final class BoundedPriorityQueue<T> {
        private final int maxSize;
        private final Comparator<T> comparator;
        private final List<T> elements;

        public BoundedPriorityQueue(int maxSize, Comparator<T> comparator) {
            this.maxSize = maxSize;
            this.comparator = comparator;
            this.elements = new ArrayList<>();
        }

        public boolean offer(T element) {
            if (elements.size() < maxSize) {
                elements.add(element);
                elements.sort(comparator);
                return true;
            }

            // Compare with lowest priority element (last in sorted list)
            int cmp = comparator.compare(element, elements.get(elements.size() - 1));
            if (cmp < 0) {
                elements.remove(elements.size() - 1);
                elements.add(element);
                elements.sort(comparator);
                return true;
            }

            return false;
        }

        public T poll() {
            if (elements.isEmpty()) return null;
            return elements.remove(0);
        }

        public T peek() {
            return elements.isEmpty() ? null : elements.get(0);
        }

        public int size() {
            return elements.size();
        }

        public boolean isEmpty() {
            return elements.isEmpty();
        }

        public List<T> toList() {
            return new ArrayList<>(elements);
        }

        public void clear() {
            elements.clear();
        }
    }
}