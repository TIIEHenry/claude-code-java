/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code ring buffer
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;

/**
 * Ring buffer - circular buffer implementation.
 */
public final class RingBuffer<T> {
    private final Object[] buffer;
    private final int capacity;
    private int head = 0;
    private int tail = 0;
    private int size = 0;
    private boolean full = false;

    public RingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.buffer = new Object[capacity];
    }

    /**
     * Add element to buffer (overwrites if full).
     */
    public void add(T element) {
        buffer[head] = element;
        head = (head + 1) % capacity;

        if (full) {
            tail = (tail + 1) % capacity;
        }

        size++;
        if (size > capacity) {
            size = capacity;
        }
        full = (size == capacity);
    }

    /**
     * Remove and return oldest element.
     */
    public Optional<T> remove() {
        if (isEmpty()) {
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        T element = (T) buffer[tail];
        buffer[tail] = null;
        tail = (tail + 1) % capacity;
        size--;
        full = false;

        return Optional.of(element);
    }

    /**
     * Peek at oldest element.
     */
    @SuppressWarnings("unchecked")
    public Optional<T> peek() {
        if (isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) buffer[tail]);
    }

    /**
     * Peek at newest element.
     */
    @SuppressWarnings("unchecked")
    public Optional<T> peekLast() {
        if (isEmpty()) {
            return Optional.empty();
        }
        int lastIndex = (head - 1 + capacity) % capacity;
        return Optional.ofNullable((T) buffer[lastIndex]);
    }

    /**
     * Get element at index (relative to start).
     */
    @SuppressWarnings("unchecked")
    public Optional<T> get(int index) {
        if (index < 0 || index >= size) {
            return Optional.empty();
        }
        int actualIndex = (tail + index) % capacity;
        return Optional.ofNullable((T) buffer[actualIndex]);
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
        return full;
    }

    /**
     * Get current size.
     */
    public int size() {
        return size;
    }

    /**
     * Get capacity.
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Get available space.
     */
    public int available() {
        return capacity - size;
    }

    /**
     * Clear buffer.
     */
    public void clear() {
        Arrays.fill(buffer, null);
        head = 0;
        tail = 0;
        size = 0;
        full = false;
    }

    /**
     * Convert to list (in order).
     */
    public List<T> toList() {
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int index = (tail + i) % capacity;
            @SuppressWarnings("unchecked")
            T element = (T) buffer[index];
            if (element != null) {
                list.add(element);
            }
        }
        return list;
    }

    /**
     * Iterate over elements.
     */
    public void forEach(Consumer<T> action) {
        for (int i = 0; i < size; i++) {
            int index = (tail + i) % capacity;
            @SuppressWarnings("unchecked")
            T element = (T) buffer[index];
            if (element != null) {
                action.accept(element);
            }
        }
    }

    /**
     * Stream of elements.
     */
    public java.util.stream.Stream<T> stream() {
        return toList().stream();
    }

    /**
     * Contains element.
     */
    public boolean contains(T element) {
        for (int i = 0; i < size; i++) {
            int index = (tail + i) % capacity;
            if (Objects.equals(buffer[index], element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find element matching predicate.
     */
    public Optional<T> find(Predicate<T> predicate) {
        for (int i = 0; i < size; i++) {
            int index = (tail + i) % capacity;
            @SuppressWarnings("unchecked")
            T element = (T) buffer[index];
            if (element != null && predicate.test(element)) {
                return Optional.of(element);
            }
        }
        return Optional.empty();
    }

    /**
     * Count elements matching predicate.
     */
    public int count(Predicate<T> predicate) {
        int count = 0;
        for (int i = 0; i < size; i++) {
            int index = (tail + i) % capacity;
            @SuppressWarnings("unchecked")
            T element = (T) buffer[index];
            if (element != null && predicate.test(element)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Map elements to new buffer.
     */
    public <R> RingBuffer<R> map(Function<T, R> mapper) {
        RingBuffer<R> result = new RingBuffer<>(capacity);
        forEach(e -> result.add(mapper.apply(e)));
        return result;
    }

    /**
     * Filter elements to new buffer.
     */
    public RingBuffer<T> filter(Predicate<T> predicate) {
        RingBuffer<T> result = new RingBuffer<>(size);
        forEach(e -> {
            if (predicate.test(e)) {
                result.add(e);
            }
        });
        return result;
    }

    /**
     * Add all elements.
     */
    public void addAll(Collection<T> elements) {
        for (T element : elements) {
            add(element);
        }
    }

    /**
     * Skip N elements (advance tail).
     */
    public void skip(int n) {
        if (n <= 0) return;
        int skip = Math.min(n, size);
        for (int i = 0; i < skip; i++) {
            buffer[tail] = null;
            tail = (tail + 1) % capacity;
        }
        size -= skip;
        full = false;
    }

    /**
     * Get latest N elements.
     */
    public List<T> latest(int n) {
        if (n <= 0 || isEmpty()) {
            return List.of();
        }
        int count = Math.min(n, size);
        List<T> result = new ArrayList<>(count);
        for (int i = count - 1; i >= 0; i--) {
            int index = (head - 1 - i + capacity) % capacity;
            @SuppressWarnings("unchecked")
            T element = (T) buffer[index];
            if (element != null) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Get oldest N elements.
     */
    public List<T> oldest(int n) {
        if (n <= 0 || isEmpty()) {
            return List.of();
        }
        int count = Math.min(n, size);
        List<T> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int index = (tail + i) % capacity;
            @SuppressWarnings("unchecked")
            T element = (T) buffer[index];
            if (element != null) {
                result.add(element);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "RingBuffer(size=" + size + ", capacity=" + capacity + ", elements=" + toList() + ")";
    }

    /**
     * Ring buffer utilities.
     */
    public static final class RingBufferUtils {
        private RingBufferUtils() {}

        /**
         * Create ring buffer.
         */
        public static <T> RingBuffer<T> ofCapacity(int capacity) {
            return new RingBuffer<>(capacity);
        }

        /**
         * Create ring buffer with initial elements.
         */
        public static <T> RingBuffer<T> of(int capacity, Collection<T> elements) {
            RingBuffer<T> buffer = new RingBuffer<>(capacity);
            buffer.addAll(elements);
            return buffer;
        }

        /**
         * Create ring buffer with initial elements.
         */
        @SafeVarargs
        public static <T> RingBuffer<T> of(int capacity, T... elements) {
            return of(capacity, Arrays.asList(elements));
        }

        /**
         * Create sliding window buffer.
         */
        public static <T extends Number> SlidingWindow<T> slidingWindow(int size) {
            return new SlidingWindow<>(size);
        }

        /**
         * Create thread-safe ring buffer.
         */
        public static <T> ThreadSafeRingBuffer<T> threadSafe(int capacity) {
            return new ThreadSafeRingBuffer<>(capacity);
        }
    }

    /**
     * Sliding window with statistics.
     */
    public static final class SlidingWindow<T extends Number> {
        private final RingBuffer<T> buffer;
        private double sum = 0;

        public SlidingWindow(int size) {
            this.buffer = new RingBuffer<>(size);
        }

        public void add(T value) {
            if (buffer.isFull()) {
                Optional<T> oldest = buffer.peek();
                if (oldest.isPresent()) {
                    sum -= oldest.get().doubleValue();
                }
            }
            buffer.add(value);
            sum += value.doubleValue();
        }

        public double average() {
            return buffer.isEmpty() ? 0 : sum / buffer.size();
        }

        public double sum() {
            return sum;
        }

        public int size() {
            return buffer.size();
        }

        public List<T> values() {
            return buffer.toList();
        }

        public void clear() {
            buffer.clear();
            sum = 0;
        }
    }

    /**
     * Thread-safe ring buffer.
     */
    public static final class ThreadSafeRingBuffer<T> {
        private final RingBuffer<T> buffer;

        public ThreadSafeRingBuffer(int capacity) {
            this.buffer = new RingBuffer<>(capacity);
        }

        public synchronized void add(T element) {
            buffer.add(element);
        }

        public synchronized Optional<T> remove() {
            return buffer.remove();
        }

        public synchronized Optional<T> peek() {
            return buffer.peek();
        }

        public synchronized boolean isEmpty() {
            return buffer.isEmpty();
        }

        public synchronized int size() {
            return buffer.size();
        }

        public synchronized List<T> toList() {
            return buffer.toList();
        }

        public synchronized void clear() {
            buffer.clear();
        }
    }
}