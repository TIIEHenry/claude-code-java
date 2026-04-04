/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Queue.
 */
class QueueTest {

    @Test
    @DisplayName("Queue creates empty")
    void createsEmpty() {
        Queue<String> queue = new Queue<>();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("Queue enqueue and dequeue works")
    void enqueueDequeue() {
        Queue<String> queue = new Queue<>();

        queue.enqueue("a");
        queue.enqueue("b");
        queue.enqueue("c");

        assertEquals(3, queue.size());
        assertEquals("a", queue.dequeue());
        assertEquals("b", queue.dequeue());
        assertEquals("c", queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("Queue dequeue throws when empty")
    void dequeueThrowsWhenEmpty() {
        Queue<String> queue = new Queue<>();

        assertThrows(NoSuchElementException.class, queue::dequeue);
    }

    @Test
    @DisplayName("Queue dequeueOrNull returns null when empty")
    void dequeueOrNullReturnsNull() {
        Queue<String> queue = new Queue<>();

        assertNull(queue.dequeueOrNull());
    }

    @Test
    @DisplayName("Queue peek returns front element")
    void peekWorks() {
        Queue<String> queue = new Queue<>();

        queue.enqueue("a");
        queue.enqueue("b");

        assertEquals("a", queue.peek());
        assertEquals(2, queue.size()); // Not removed
    }

    @Test
    @DisplayName("Queue peek throws when empty")
    void peekThrowsWhenEmpty() {
        Queue<String> queue = new Queue<>();

        assertThrows(NoSuchElementException.class, queue::peek);
    }

    @Test
    @DisplayName("Queue peekOrNull returns null when empty")
    void peekOrNullReturnsNull() {
        Queue<String> queue = new Queue<>();

        assertNull(queue.peekOrNull());
    }

    @Test
    @DisplayName("Queue peekLast returns last element")
    void peekLastWorks() {
        Queue<String> queue = new Queue<>();

        queue.enqueue("a");
        queue.enqueue("b");
        queue.enqueue("c");

        assertEquals("c", queue.peekLast());
    }

    @Test
    @DisplayName("Queue peekLast returns null when empty")
    void peekLastReturnsNull() {
        Queue<String> queue = new Queue<>();

        assertNull(queue.peekLast());
    }

    @Test
    @DisplayName("Queue contains finds element")
    void containsWorks() {
        Queue<String> queue = new Queue<>();

        queue.enqueue("a");
        queue.enqueue("b");

        assertTrue(queue.contains("a"));
        assertTrue(queue.contains("b"));
        assertFalse(queue.contains("c"));
    }

    @Test
    @DisplayName("Queue toList returns elements")
    void toListWorks() {
        Queue<String> queue = new Queue<>();

        queue.enqueue("a");
        queue.enqueue("b");
        queue.enqueue("c");

        List<String> list = queue.toList();

        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

    @Test
    @DisplayName("Queue clear removes all")
    void clearWorks() {
        Queue<String> queue = new Queue<>();

        queue.enqueue("a");
        queue.enqueue("b");
        queue.clear();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("Queue drain returns all and clears")
    void drainWorks() {
        Queue<String> queue = new Queue<>();

        queue.enqueue("a");
        queue.enqueue("b");

        List<String> drained = queue.drain();

        assertEquals(2, drained.size());
        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("Queue drain with max limits elements")
    void drainWithMax() {
        Queue<String> queue = new Queue<>();

        queue.enqueue("a");
        queue.enqueue("b");
        queue.enqueue("c");

        List<String> drained = queue.drain(2);

        assertEquals(2, drained.size());
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("Queue iterator works")
    void iteratorWorks() {
        Queue<String> queue = new Queue<>();

        queue.enqueue("a");
        queue.enqueue("b");

        StringBuilder sb = new StringBuilder();
        for (String s : queue) {
            sb.append(s);
        }

        assertEquals("ab", sb.toString());
    }

    @Test
    @DisplayName("Queue expands capacity")
    void expandsCapacity() {
        Queue<Integer> queue = new Queue<>(2);

        for (int i = 0; i < 100; i++) {
            queue.enqueue(i);
        }

        assertEquals(100, queue.size());
        assertEquals(0, queue.dequeue());
        assertEquals(99, queue.peekLast());
    }

    @Test
    @DisplayName("Queue QueueUtils of creates queue")
    void queueUtilsOf() {
        Queue<String> queue = Queue.QueueUtils.of("a", "b", "c");

        assertEquals(3, queue.size());
        assertEquals("a", queue.dequeue());
    }

    @Test
    @DisplayName("Queue QueueUtils fromCollection creates queue")
    void queueUtilsFromCollection() {
        Queue<String> queue = Queue.QueueUtils.fromCollection(List.of("a", "b"));

        assertEquals(2, queue.size());
    }

    @Test
    @DisplayName("Queue BoundedQueue offer respects limit")
    void boundedQueueOffer() {
        Queue.BoundedQueue<String> queue = Queue.QueueUtils.bounded(2);

        assertTrue(queue.offer("a"));
        assertTrue(queue.offer("b"));
        assertFalse(queue.offer("c")); // Full

        assertEquals(2, queue.size());
    }

    @Test
    @DisplayName("Queue BoundedQueue enqueue removes oldest")
    void boundedQueueEnqueue() {
        Queue.BoundedQueue<String> queue = Queue.QueueUtils.bounded(2);

        queue.enqueue("a");
        queue.enqueue("b");
        queue.enqueue("c"); // Should remove "a"

        assertEquals(2, queue.size());
        assertEquals("b", queue.peek());
    }

    @Test
    @DisplayName("Queue BoundedQueue isFull works")
    void boundedQueueIsFull() {
        Queue.BoundedQueue<String> queue = Queue.QueueUtils.bounded(2);

        assertFalse(queue.isFull());
        queue.enqueue("a");
        assertFalse(queue.isFull());
        queue.enqueue("b");
        assertTrue(queue.isFull());
    }

    @Test
    @DisplayName("Queue BoundedQueue maxSize returns max")
    void boundedQueueMaxSize() {
        Queue.BoundedQueue<String> queue = Queue.QueueUtils.bounded(5);

        assertEquals(5, queue.maxSize());
    }

    @Test
    @DisplayName("Queue toString returns list representation")
    void toStringWorks() {
        Queue<String> queue = new Queue<>();

        queue.enqueue("a");
        queue.enqueue("b");

        String str = queue.toString();

        assertTrue(str.contains("a"));
        assertTrue(str.contains("b"));
    }
}