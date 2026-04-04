/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code stack
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;

/**
 * Stack implementation.
 */
public final class Stack<T> implements Iterable<T> {
    private static final int DEFAULT_CAPACITY = 16;
    private Object[] elements;
    private int size;

    public Stack() {
        this(DEFAULT_CAPACITY);
    }

    public Stack(int initialCapacity) {
        this.elements = new Object[initialCapacity];
        this.size = 0;
    }

    /**
     * Push element.
     */
    public void push(T element) {
        ensureCapacity();
        elements[size++] = element;
    }

    /**
     * Pop element.
     */
    @SuppressWarnings("unchecked")
    public T pop() {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        T element = (T) elements[--size];
        elements[size] = null;
        return element;
    }

    /**
     * Pop or null.
     */
    @SuppressWarnings("unchecked")
    public T popOrNull() {
        if (isEmpty()) return null;
        T element = (T) elements[--size];
        elements[size] = null;
        return element;
    }

    /**
     * Peek at top.
     */
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        return (T) elements[size - 1];
    }

    /**
     * Peek or null.
     */
    @SuppressWarnings("unchecked")
    public T peekOrNull() {
        if (isEmpty()) return null;
        return (T) elements[size - 1];
    }

    /**
     * Peek at depth.
     */
    @SuppressWarnings("unchecked")
    public T peek(int depth) {
        if (depth < 0 || depth >= size) {
            throw new IndexOutOfBoundsException("Depth: " + depth);
        }
        return (T) elements[size - 1 - depth];
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
        Arrays.fill(elements, 0, size, null);
        size = 0;
    }

    /**
     * Contains.
     */
    public boolean contains(T element) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(elements[i], element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Search for element (1-based from top).
     */
    public int search(T element) {
        for (int i = size - 1; i >= 0; i--) {
            if (Objects.equals(elements[i], element)) {
                return size - i;
            }
        }
        return -1;
    }

    /**
     * To list (bottom to top).
     */
    @SuppressWarnings("unchecked")
    public List<T> toList() {
        List<T> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add((T) elements[i]);
        }
        return result;
    }

    /**
     * To list reversed (top to bottom).
     */
    public List<T> toListReversed() {
        List<T> result = toList();
        Collections.reverse(result);
        return result;
    }

    /**
     * Ensure capacity.
     */
    private void ensureCapacity() {
        if (size == elements.length) {
            elements = Arrays.copyOf(elements, elements.length * 2);
        }
    }

    /**
     * Execute with push/pop.
     */
    public <R> R withPush(T element, Supplier<R> action) {
        push(element);
        try {
            return action.get();
        } finally {
            pop();
        }
    }

    /**
     * Execute with push/pop (void).
     */
    public void withPush(T element, Runnable action) {
        push(element);
        try {
            action.run();
        } finally {
            pop();
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
                return (T) elements[index++];
            }
        };
    }

    @Override
    public String toString() {
        return toList().toString();
    }

    /**
     * Stack utilities.
     */
    public static final class StackUtils {
        private StackUtils() {}

        /**
         * Create from elements.
         */
        @SafeVarargs
        public static <T> Stack<T> of(T... elements) {
            Stack<T> stack = new Stack<>(elements.length);
            for (T element : elements) {
                stack.push(element);
            }
            return stack;
        }

        /**
         * Create from collection.
         */
        public static <T> Stack<T> fromCollection(Collection<T> collection) {
            Stack<T> stack = new Stack<>(collection.size());
            collection.forEach(stack::push);
            return stack;
        }
    }
}