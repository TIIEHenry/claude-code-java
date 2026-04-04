/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code queue
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Queue implementation.
 */
public final class Queue<T> implements Iterable<T> {
    private static final int DEFAULT_CAPACITY = 16;
    private Object[] elements;
    private int head;
    private int tail;
    private int size;

    public Queue() {
        this(DEFAULT_CAPACITY);
    }

    public Queue(int initialCapacity) {
        this.elements = new Object[initialCapacity];
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    /**
     * Enqueue element.
     */
    public void enqueue(T element) {
        ensureCapacity();
        elements[tail] = element;
        tail = (tail + 1) % elements.length;
        size++;
    }

    /**
     * Dequeue element.
     */
    @SuppressWarnings("unchecked")
    public T dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        T element = (T) elements[head];
        elements[head] = null;
        head = (head + 1) % elements.length;
        size--;
        return element;
    }

    /**
     * Dequeue or null.
     */
    @SuppressWarnings("unchecked")
    public T dequeueOrNull() {
        if (isEmpty()) return null;
        T element = (T) elements[head];
        elements[head] = null;
        head = (head + 1) % elements.length;
        size--;
        return element;
    }

    /**
     * Peek at front.
     */
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        return (T) elements[head];
    }

    /**
     * Peek or null.
     */
    @SuppressWarnings("unchecked")
    public T peekOrNull() {
        if (isEmpty()) return null;
        return (T) elements[head];
    }

    /**
     * Peek at back.
     */
    @SuppressWarnings("unchecked")
    public T peekLast() {
        if (isEmpty()) return null;
        int lastIndex = (tail - 1 + elements.length) % elements.length;
        return (T) elements[lastIndex];
    }

    /**
     * Is empty.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Size.
     */
    public int size() {
        return size;
    }

    /**
     * Clear.
     */
    public void clear() {
        Arrays.fill(elements, null);
        head = tail = size = 0;
    }

    /**
     * Contains.
     */
    public boolean contains(T element) {
        for (int i = 0; i < size; i++) {
            int index = (head + i) % elements.length;
            if (Objects.equals(elements[index], element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * To list.
     */
    @SuppressWarnings("unchecked")
    public List<T> toList() {
        List<T> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int index = (head + i) % elements.length;
            result.add((T) elements[index]);
        }
        return result;
    }

    /**
     * Drain all.
     */
    public List<T> drain() {
        List<T> result = toList();
        clear();
        return result;
    }

    /**
     * Drain up to N.
     */
    public List<T> drain(int maxElements) {
        List<T> result = new ArrayList<>(Math.min(maxElements, size));
        for (int i = 0; i < maxElements && !isEmpty(); i++) {
            result.add(dequeue());
        }
        return result;
    }

    /**
     * Ensure capacity.
     */
    private void ensureCapacity() {
        if (size == elements.length) {
            int newCapacity = elements.length * 2;
            Object[] newElements = new Object[newCapacity];
            for (int i = 0; i < size; i++) {
                newElements[i] = elements[(head + i) % elements.length];
            }
            elements = newElements;
            head = 0;
            tail = size;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            @SuppressWarnings("unchecked")
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                int arrayIndex = (head + index) % elements.length;
                index++;
                return (T) elements[arrayIndex];
            }
        };
    }

    @Override
    public String toString() {
        return toList().toString();
    }

    /**
     * Queue utilities.
     */
    public static final class QueueUtils {
        private QueueUtils() {}

        /**
         * Create from elements.
         */
        @SafeVarargs
        public static <T> Queue<T> of(T... elements) {
            Queue<T> queue = new Queue<>(elements.length);
            for (T element : elements) {
                queue.enqueue(element);
            }
            return queue;
        }

        /**
         * Create from collection.
         */
        public static <T> Queue<T> fromCollection(Collection<T> collection) {
            Queue<T> queue = new Queue<>(collection.size());
            collection.forEach(queue::enqueue);
            return queue;
        }

        /**
         * Create bounded queue.
         */
        public static <T> BoundedQueue<T> bounded(int maxSize) {
            return new BoundedQueue<>(maxSize);
        }
    }

    /**
     * Bounded queue with max size.
     */
    public static final class BoundedQueue<T> {
        private final int maxSize;
        private final Queue<T> queue;

        public BoundedQueue(int maxSize) {
            this.maxSize = maxSize;
            this.queue = new Queue<>(maxSize);
        }

        /**
         * Enqueue, returns true if added.
         */
        public boolean offer(T element) {
            if (queue.size() >= maxSize) {
                return false;
            }
            queue.enqueue(element);
            return true;
        }

        /**
         * Enqueue, removes oldest if full.
         */
        public void enqueue(T element) {
            if (queue.size() >= maxSize) {
                queue.dequeue();
            }
            queue.enqueue(element);
        }

        /**
         * Dequeue.
         */
        public T dequeue() {
            return queue.dequeue();
        }

        /**
         * Peek.
         */
        public T peek() {
            return queue.peek();
        }

        /**
         * Size.
         */
        public int size() {
            return queue.size();
        }

        /**
         * Max size.
         */
        public int maxSize() {
            return maxSize;
        }

        /**
         * Is empty.
         */
        public boolean isEmpty() {
            return queue.isEmpty();
        }

        /**
         * Is full.
         */
        public boolean isFull() {
            return queue.size() >= maxSize;
        }

        /**
         * To list.
         */
        public List<T> toList() {
            return queue.toList();
        }
    }
}