/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code linked list
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;

/**
 * Doubly linked list implementation.
 */
public final class LinkedList<T> implements Iterable<T> {
    private Node<T> head;
    private Node<T> tail;
    private int size;

    public LinkedList() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    /**
     * Add to front.
     */
    public void addFirst(T value) {
        Node<T> node = new Node<>(value);
        if (head == null) {
            head = tail = node;
        } else {
            node.next = head;
            head.prev = node;
            head = node;
        }
        size++;
    }

    /**
     * Add to end.
     */
    public void addLast(T value) {
        Node<T> node = new Node<>(value);
        if (tail == null) {
            head = tail = node;
        } else {
            node.prev = tail;
            tail.next = node;
            tail = node;
        }
        size++;
    }

    /**
     * Add (same as addLast).
     */
    public void add(T value) {
        addLast(value);
    }

    /**
     * Remove from front.
     */
    public T removeFirst() {
        if (head == null) return null;
        T value = head.value;
        head = head.next;
        if (head == null) {
            tail = null;
        } else {
            head.prev = null;
        }
        size--;
        return value;
    }

    /**
     * Remove from end.
     */
    public T removeLast() {
        if (tail == null) return null;
        T value = tail.value;
        tail = tail.prev;
        if (tail == null) {
            head = null;
        } else {
            tail.next = null;
        }
        size--;
        return value;
    }

    /**
     * Get first.
     */
    public T getFirst() {
        return head != null ? head.value : null;
    }

    /**
     * Get last.
     */
    public T getLast() {
        return tail != null ? tail.value : null;
    }

    /**
     * Get at index.
     */
    public T get(int index) {
        if (index < 0 || index >= size) return null;
        Node<T> current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current.value;
    }

    /**
     * Set at index.
     */
    public void set(int index, T value) {
        if (index < 0 || index >= size) return;
        Node<T> current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        current.value = value;
    }

    /**
     * Insert at index.
     */
    public void insert(int index, T value) {
        if (index <= 0) {
            addFirst(value);
        } else if (index >= size) {
            addLast(value);
        } else {
            Node<T> current = head;
            for (int i = 0; i < index; i++) {
                current = current.next;
            }
            Node<T> node = new Node<>(value);
            node.prev = current.prev;
            node.next = current;
            current.prev.next = node;
            current.prev = node;
            size++;
        }
    }

    /**
     * Remove at index.
     */
    public T remove(int index) {
        if (index < 0 || index >= size) return null;
        if (index == 0) return removeFirst();
        if (index == size - 1) return removeLast();

        Node<T> current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        current.prev.next = current.next;
        current.next.prev = current.prev;
        size--;
        return current.value;
    }

    /**
     * Remove first occurrence.
     */
    public boolean remove(T value) {
        Node<T> current = head;
        while (current != null) {
            if (Objects.equals(current.value, value)) {
                if (current.prev != null) {
                    current.prev.next = current.next;
                } else {
                    head = current.next;
                }
                if (current.next != null) {
                    current.next.prev = current.prev;
                } else {
                    tail = current.prev;
                }
                size--;
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /**
     * Contains value.
     */
    public boolean contains(T value) {
        Node<T> current = head;
        while (current != null) {
            if (Objects.equals(current.value, value)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /**
     * Index of value.
     */
    public int indexOf(T value) {
        Node<T> current = head;
        int index = 0;
        while (current != null) {
            if (Objects.equals(current.value, value)) {
                return index;
            }
            current = current.next;
            index++;
        }
        return -1;
    }

    /**
     * Size.
     */
    public int size() {
        return size;
    }

    /**
     * Is empty.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Clear.
     */
    public void clear() {
        head = tail = null;
        size = 0;
    }

    /**
     * To list.
     */
    public List<T> toList() {
        List<T> result = new ArrayList<>(size);
        Node<T> current = head;
        while (current != null) {
            result.add(current.value);
            current = current.next;
        }
        return result;
    }

    /**
     * Reverse.
     */
    public void reverse() {
        Node<T> current = head;
        Node<T> temp = null;
        while (current != null) {
            temp = current.prev;
            current.prev = current.next;
            current.next = temp;
            current = current.prev;
        }
        if (temp != null) {
            head = temp.prev;
        }
    }

    /**
     * For each - override to provide custom iteration.
     */
    @Override
    public void forEach(java.util.function.Consumer<? super T> action) {
        Node<T> current = head;
        while (current != null) {
            action.accept(current.value);
            current = current.next;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> current = head;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                if (current == null) throw new NoSuchElementException();
                T value = current.value;
                current = current.next;
                return value;
            }
        };
    }

    @Override
    public String toString() {
        return toList().toString();
    }

    /**
     * Node class.
     */
    private static final class Node<T> {
        T value;
        Node<T> prev;
        Node<T> next;

        Node(T value) {
            this.value = value;
        }
    }
}