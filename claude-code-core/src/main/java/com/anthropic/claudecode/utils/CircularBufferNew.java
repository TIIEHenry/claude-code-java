/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code circular buffer
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * A fixed-size circular buffer that automatically evicts the oldest items
 * when the buffer is full. Useful for maintaining a rolling window of data.
 */
public final class CircularBufferNew<T> {
    private final Object[] buffer;
    private final int capacity;
    private int head = 0;
    private int size = 0;

    /**
     * Create a circular buffer with given capacity.
     */
    public CircularBufferNew(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.buffer = new Object[capacity];
    }

    /**
     * Add an item to the buffer. If the buffer is full,
     * the oldest item will be evicted.
     */
    public void add(T item) {
        buffer[head] = item;
        head = (head + 1) % capacity;
        if (size < capacity) {
            size++;
        }
    }

    /**
     * Add multiple items to the buffer at once.
     */
    public void addAll(Collection<T> items) {
        for (T item : items) {
            add(item);
        }
    }

    /**
     * Add multiple items from array.
     */
    public void addAll(T[] items) {
        for (T item : items) {
            add(item);
        }
    }

    /**
     * Get the most recent N items from the buffer.
     * Returns fewer items if the buffer contains less than N items.
     */
    public List<T> getRecent(int count) {
        List<T> result = new ArrayList<>();
        int start = size < capacity ? 0 : head;
        int available = Math.min(count, size);

        for (int i = 0; i < available; i++) {
            int index = (start + size - available + i) % capacity;
            result.add((T) buffer[index]);
        }

        return result;
    }

    /**
     * Get all items currently in the buffer, in order from oldest to newest.
     */
    public List<T> toArray() {
        if (size == 0) {
            return List.of();
        }

        List<T> result = new ArrayList<>(size);
        int start = size < capacity ? 0 : head;

        for (int i = 0; i < size; i++) {
            int index = (start + i) % capacity;
            result.add((T) buffer[index]);
        }

        return result;
    }

    /**
     * Get the oldest item.
     */
    public Optional<T> getOldest() {
        if (size == 0) {
            return Optional.empty();
        }
        int start = size < capacity ? 0 : head;
        return Optional.of((T) buffer[start]);
    }

    /**
     * Get the newest item.
     */
    public Optional<T> getNewest() {
        if (size == 0) {
            return Optional.empty();
        }
        int newestIndex = (head - 1 + capacity) % capacity;
        return Optional.of((T) buffer[newestIndex]);
    }

    /**
     * Get item at specific position (0 = oldest, size-1 = newest).
     */
    public Optional<T> get(int position) {
        if (position < 0 || position >= size) {
            return Optional.empty();
        }
        int start = size < capacity ? 0 : head;
        int index = (start + position) % capacity;
        return Optional.of((T) buffer[index]);
    }

    /**
     * Clear all items from the buffer.
     */
    public void clear() {
        Arrays.fill(buffer, null);
        head = 0;
        size = 0;
    }

    /**
     * Get the current number of items in the buffer.
     */
    public int size() {
        return size;
    }

    /**
     * Check if buffer is empty.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Check if buffer is full.
     */
    public boolean isFull() {
        return size == capacity;
    }

    /**
     * Get the capacity of the buffer.
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Remove and return the oldest item.
     */
    public Optional<T> removeOldest() {
        if (size == 0) {
            return Optional.empty();
        }

        int start = size < capacity ? 0 : head;
        T item = (T) buffer[start];
        buffer[start] = null;

        // Shift head if buffer was full
        if (size == capacity) {
            head = (head + 1) % capacity;
        }

        size--;
        return Optional.of(item);
    }

    /**
     * Check if buffer contains an item.
     */
    public boolean contains(T item) {
        if (item == null) {
            for (int i = 0; i < size; i++) {
                int index = (head - size + i + capacity) % capacity;
                if (buffer[index] == null) {
                    return true;
                }
            }
            return false;
        }

        for (int i = 0; i < size; i++) {
            int index = (head - size + i + capacity) % capacity;
            if (item.equals(buffer[index])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stream all items.
     */
    public java.util.stream.Stream<T> stream() {
        return toArray().stream();
    }

    /**
     * Iterate over items in order (oldest to newest).
     */
    public Iterator<T> iterator() {
        return toArray().iterator();
    }

    /**
     * For each item in order.
     */
    public void forEach(java.util.function.Consumer<T> action) {
        int start = size < capacity ? 0 : head;
        for (int i = 0; i < size; i++) {
            int index = (start + i) % capacity;
            action.accept((T) buffer[index]);
        }
    }

    @Override
    public String toString() {
        return "CircularBufferNew{size=" + size + ", capacity=" + capacity + ", items=" + toArray() + "}";
    }
}