/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PriorityQueue.
 */
class PriorityQueueTest {

    @Test
    @DisplayName("PriorityQueue creates with comparator")
    void createsWithComparator() {
        Comparator<Integer> comparator = Comparator.naturalOrder();
        PriorityQueue<Integer> queue = new PriorityQueue<>(comparator);

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("PriorityQueue minQueue creates min queue")
    void minQueue() {
        PriorityQueue<Integer> queue = PriorityQueue.minQueue();

        queue.add(5);
        queue.add(3);
        queue.add(7);

        assertEquals(3, queue.peek());
        assertEquals(3, queue.poll());
        assertEquals(5, queue.poll());
        assertEquals(7, queue.poll());
    }

    @Test
    @DisplayName("PriorityQueue maxQueue creates max queue")
    void maxQueue() {
        PriorityQueue<Integer> queue = PriorityQueue.maxQueue();

        queue.add(5);
        queue.add(3);
        queue.add(7);

        assertEquals(7, queue.peek());
        assertEquals(7, queue.poll());
        assertEquals(5, queue.poll());
        assertEquals(3, queue.poll());
    }

    @Test
    @DisplayName("PriorityQueue add adds element")
    void addWorks() {
        PriorityQueue<Integer> queue = PriorityQueue.minQueue();

        assertTrue(queue.add(5));
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("PriorityQueue offer adds element")
    void offerWorks() {
        PriorityQueue<Integer> queue = PriorityQueue.minQueue();

        assertTrue(queue.offer(5));
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("PriorityQueue peek returns head without removing")
    void peekWorks() {
        PriorityQueue<Integer> queue = PriorityQueue.minQueue();

        queue.add(5);
        queue.add(3);

        assertEquals(3, queue.peek());
        assertEquals(2, queue.size());
    }

    @Test
    @DisplayName("PriorityQueue peek returns null when empty")
    void peekEmpty() {
        PriorityQueue<Integer> queue = PriorityQueue.minQueue();

        assertNull(queue.peek());
    }

    @Test
    @DisplayName("PriorityQueue poll removes and returns head")
    void pollWorks() {
        PriorityQueue<Integer> queue = PriorityQueue.minQueue();

        queue.add(5);
        queue.add(3);

        assertEquals(3, queue.poll());
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("PriorityQueue poll returns null when empty")
    void pollEmpty() {
        PriorityQueue<Integer> queue = PriorityQueue.minQueue();

        assertNull(queue.poll());
    }

    @Test
    @DisplayName("PriorityQueue remove removes element")
    void removeWorks() {
        PriorityQueue<Integer> queue = PriorityQueue.minQueue();

        queue.add(5);
        queue.add(3);

        assertTrue(queue.remove(5));
        assertEquals(1, queue.size());
        assertEquals(3, queue.peek());
    }

    @Test
    @DisplayName("PriorityQueue remove returns false for missing")
    void removeMissing() {
        PriorityQueue<Integer> queue = PriorityQueue.minQueue();

        queue.add(5);

        assertFalse(queue.remove(10));
    }

    @Test
    @DisplayName("PriorityQueue contains checks element")
    void containsWorks() {
        PriorityQueue<Integer> queue = PriorityQueue.minQueue();

        queue.add(5);

        assertTrue(queue.contains(5));
        assertFalse(queue.contains(10));
    }

    @Test
    @DisplayName("PriorityQueue clear removes all")
    void clearWorks() {
        PriorityQueue<Integer> queue = PriorityQueue.minQueue();

        queue.add(5);
        queue.add(3);
        queue.clear();

        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("PriorityQueue drain returns all")
    void drainWorks() {
        PriorityQueue<Integer> queue = PriorityQueue.minQueue();

        queue.add(5);
        queue.add(3);
        queue.add(7);

        List<Integer> drained = queue.drain();

        assertEquals(3, drained.size());
        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("PriorityQueue drain with limit")
    void drainWithLimit() {
        PriorityQueue<Integer> queue = PriorityQueue.minQueue();

        queue.add(5);
        queue.add(3);
        queue.add(7);

        List<Integer> drained = queue.drain(2);

        assertEquals(2, drained.size());
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("PriorityQueue drainTo works")
    void drainToWorks() {
        PriorityQueue<Integer> queue = PriorityQueue.minQueue();
        List<Integer> target = new ArrayList<>();

        queue.add(5);
        queue.add(3);

        int count = queue.drainTo(target);

        assertEquals(2, count);
        assertEquals(2, target.size());
    }

    @Test
    @DisplayName("PriorityQueue drainTo with limit")
    void drainToWithLimit() {
        PriorityQueue<Integer> queue = PriorityQueue.minQueue();
        List<Integer> target = new ArrayList<>();

        queue.add(5);
        queue.add(3);
        queue.add(7);

        int count = queue.drainTo(target, 2);

        assertEquals(2, count);
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("PriorityQueue toList returns sorted")
    void toListWorks() {
        PriorityQueue<Integer> queue = PriorityQueue.minQueue();

        queue.add(5);
        queue.add(3);
        queue.add(7);

        List<Integer> list = queue.toList();

        assertEquals(3, list.size());
        // Should be sorted
        assertEquals(3, list.get(0));
        assertEquals(5, list.get(1));
        assertEquals(7, list.get(2));
    }

    @Test
    @DisplayName("PriorityQueue of creates with comparator")
    void ofWorks() {
        Comparator<String> comparator = Comparator.naturalOrder();
        PriorityQueue<String> queue = PriorityQueue.of(comparator);

        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("PriorityQueue fromCollection creates from collection")
    void fromCollectionWorks() {
        PriorityQueue<Integer> queue = PriorityQueue.fromCollection(List.of(5, 3, 7));

        assertEquals(3, queue.size());
        assertEquals(3, queue.peek());
    }

    @Test
    @DisplayName("PriorityQueue BoundedPriorityQueue limits size")
    void boundedQueue() {
        Comparator<Integer> comparator = Comparator.naturalOrder();
        PriorityQueue.BoundedPriorityQueue<Integer> queue =
            new PriorityQueue.BoundedPriorityQueue<>(3, comparator);

        assertTrue(queue.offer(5));
        assertTrue(queue.offer(3));
        assertTrue(queue.offer(7));
        assertFalse(queue.offer(10)); // Larger than max, rejected
        assertTrue(queue.offer(1)); // Smaller than max (7), accepted and 7 removed

        assertEquals(3, queue.size());
        assertEquals(1, queue.peek());
    }

    @Test
    @DisplayName("PriorityQueue BoundedPriorityQueue poll works")
    void boundedQueuePoll() {
        Comparator<Integer> comparator = Comparator.naturalOrder();
        PriorityQueue.BoundedPriorityQueue<Integer> queue =
            new PriorityQueue.BoundedPriorityQueue<>(3, comparator);

        queue.offer(5);
        queue.offer(3);

        assertEquals(3, queue.poll());
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("PriorityQueue BoundedPriorityQueue toList returns sorted")
    void boundedQueueToList() {
        Comparator<Integer> comparator = Comparator.naturalOrder();
        PriorityQueue.BoundedPriorityQueue<Integer> queue =
            new PriorityQueue.BoundedPriorityQueue<>(5, comparator);

        queue.offer(5);
        queue.offer(3);
        queue.offer(7);

        List<Integer> list = queue.toList();

        assertEquals(3, list.size());
        assertEquals(3, list.get(0));
    }

    @Test
    @DisplayName("PriorityQueue BoundedPriorityQueue clear works")
    void boundedQueueClear() {
        Comparator<Integer> comparator = Comparator.naturalOrder();
        PriorityQueue.BoundedPriorityQueue<Integer> queue =
            new PriorityQueue.BoundedPriorityQueue<>(5, comparator);

        queue.offer(5);
        queue.offer(3);
        queue.clear();

        assertTrue(queue.isEmpty());
    }
}