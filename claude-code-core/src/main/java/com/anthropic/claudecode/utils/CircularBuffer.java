/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Circular buffer for fixed-size history.
 */
public class CircularBuffer<T> {
    private final Object[] buffer;
    private int head = 0;
    private int size = 0;

    public CircularBuffer(int capacity) {
        this.buffer = new Object[capacity];
    }

    /**
     * Add an item.
     */
    public void add(T item) {
        buffer[head] = item;
        head = (head + 1) % buffer.length;
        if (size < buffer.length) {
            size++;
        }
    }

    /**
     * Get item at index.
     */
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index);
        }
        int actualIndex = (head - size + index + buffer.length) % buffer.length;
        return (T) buffer[actualIndex];
    }

    /**
     * Get size.
     */
    public int size() {
        return size;
    }

    /**
     * Get capacity.
     */
    public int capacity() {
        return buffer.length;
    }

    /**
     * Check if empty.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Check if full.
     */
    public boolean isFull() {
        return size == buffer.length;
    }

    /**
     * Clear buffer.
     */
    public void clear() {
        Arrays.fill(buffer, null);
        head = 0;
        size = 0;
    }

    /**
     * Convert to list.
     */
    @SuppressWarnings("unchecked")
    public List<T> toList() {
        List<T> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int actualIndex = (head - size + i + buffer.length) % buffer.length;
            result.add((T) buffer[actualIndex]);
        }
        return result;
    }
}