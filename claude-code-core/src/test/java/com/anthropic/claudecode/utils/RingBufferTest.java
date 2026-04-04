/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RingBuffer.
 */
class RingBufferTest {

    @Test
    @DisplayName("RingBuffer creates with capacity")
    void createsWithCapacity() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        assertEquals(5, buffer.capacity());
        assertEquals(0, buffer.size());
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());
    }

    @Test
    @DisplayName("RingBuffer fails with zero capacity")
    void failsWithZeroCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new RingBuffer<>(0));
    }

    @Test
    @DisplayName("RingBuffer fails with negative capacity")
    void failsWithNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new RingBuffer<>(-1));
    }

    @Test
    @DisplayName("RingBuffer add adds element")
    void addWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");

        assertEquals(1, buffer.size());
    }

    @Test
    @DisplayName("RingBuffer overwrites when full")
    void overwritesWhenFull() {
        RingBuffer<String> buffer = new RingBuffer<>(2);

        buffer.add("a");
        buffer.add("b");
        buffer.add("c"); // Overwrites "a"

        assertEquals(2, buffer.size());
        assertFalse(buffer.contains("a"));
        assertTrue(buffer.contains("b"));
        assertTrue(buffer.contains("c"));
    }

    @Test
    @DisplayName("RingBuffer remove removes oldest")
    void removeWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");
        buffer.add("b");

        Optional<String> removed = buffer.remove();

        assertTrue(removed.isPresent());
        assertEquals("a", removed.get());
        assertEquals(1, buffer.size());
    }

    @Test
    @DisplayName("RingBuffer remove returns empty when empty")
    void removeEmpty() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        Optional<String> removed = buffer.remove();

        assertFalse(removed.isPresent());
    }

    @Test
    @DisplayName("RingBuffer peek returns oldest")
    void peekWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");
        buffer.add("b");

        Optional<String> peeked = buffer.peek();

        assertTrue(peeked.isPresent());
        assertEquals("a", peeked.get());
        assertEquals(2, buffer.size()); // Not removed
    }

    @Test
    @DisplayName("RingBuffer peekLast returns newest")
    void peekLastWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");
        buffer.add("b");

        Optional<String> last = buffer.peekLast();

        assertTrue(last.isPresent());
        assertEquals("b", last.get());
    }

    @Test
    @DisplayName("RingBuffer get returns element at index")
    void getWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");
        buffer.add("b");
        buffer.add("c");

        assertEquals("a", buffer.get(0).orElse(null));
        assertEquals("b", buffer.get(1).orElse(null));
        assertEquals("c", buffer.get(2).orElse(null));
    }

    @Test
    @DisplayName("RingBuffer get returns empty for invalid index")
    void getInvalid() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");

        assertFalse(buffer.get(-1).isPresent());
        assertFalse(buffer.get(5).isPresent());
    }

    @Test
    @DisplayName("RingBuffer isFull checks capacity")
    void isFullWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(2);

        buffer.add("a");
        assertFalse(buffer.isFull());
        buffer.add("b");
        assertTrue(buffer.isFull());
    }

    @Test
    @DisplayName("RingBuffer available returns remaining space")
    void availableWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");

        assertEquals(4, buffer.available());
    }

    @Test
    @DisplayName("RingBuffer clear removes all")
    void clearWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");
        buffer.add("b");
        buffer.clear();

        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.size());
    }

    @Test
    @DisplayName("RingBuffer toList returns elements in order")
    void toListWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");
        buffer.add("b");
        buffer.add("c");

        List<String> list = buffer.toList();

        assertEquals(List.of("a", "b", "c"), list);
    }

    @Test
    @DisplayName("RingBuffer forEach iterates")
    void forEachWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");
        buffer.add("b");

        StringBuilder sb = new StringBuilder();
        buffer.forEach(sb::append);

        assertEquals("ab", sb.toString());
    }

    @Test
    @DisplayName("RingBuffer contains checks element")
    void containsWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");
        buffer.add("b");

        assertTrue(buffer.contains("a"));
        assertTrue(buffer.contains("b"));
        assertFalse(buffer.contains("c"));
    }

    @Test
    @DisplayName("RingBuffer find returns matching element")
    void findWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");
        buffer.add("b");

        Optional<String> found = buffer.find(s -> s.equals("b"));

        assertTrue(found.isPresent());
        assertEquals("b", found.get());
    }

    @Test
    @DisplayName("RingBuffer count counts matching elements")
    void countWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");
        buffer.add("ab");
        buffer.add("ac");

        assertEquals(3, buffer.count(s -> s.startsWith("a")));
        assertEquals(1, buffer.count(s -> s.equals("ab")));
    }

    @Test
    @DisplayName("RingBuffer map transforms elements")
    void mapWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");
        buffer.add("bb");

        RingBuffer<Integer> mapped = buffer.map(String::length);

        assertEquals(List.of(1, 2), mapped.toList());
    }

    @Test
    @DisplayName("RingBuffer filter filters elements")
    void filterWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");
        buffer.add("ab");
        buffer.add("abc");

        RingBuffer<String> filtered = buffer.filter(s -> s.length() > 1);

        assertEquals(2, filtered.size());
    }

    @Test
    @DisplayName("RingBuffer addAll adds all elements")
    void addAllWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.addAll(List.of("a", "b", "c"));

        assertEquals(3, buffer.size());
    }

    @Test
    @DisplayName("RingBuffer skip skips elements")
    void skipWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");
        buffer.add("b");
        buffer.add("c");

        buffer.skip(2);

        assertEquals(1, buffer.size());
        assertEquals("c", buffer.peek().orElse(null));
    }

    @Test
    @DisplayName("RingBuffer latest returns latest N elements")
    void latestWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");
        buffer.add("b");
        buffer.add("c");

        List<String> latest = buffer.latest(2);

        assertEquals(2, latest.size());
        assertTrue(latest.contains("b"));
        assertTrue(latest.contains("c"));
    }

    @Test
    @DisplayName("RingBuffer oldest returns oldest N elements")
    void oldestWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");
        buffer.add("b");
        buffer.add("c");

        List<String> oldest = buffer.oldest(2);

        assertEquals(2, oldest.size());
        assertEquals("a", oldest.get(0));
        assertEquals("b", oldest.get(1));
    }

    @Test
    @DisplayName("RingBuffer toString shows info")
    void toStringWorks() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.add("a");

        String str = buffer.toString();

        assertTrue(str.contains("size=1"));
        assertTrue(str.contains("capacity=5"));
    }

    @Test
    @DisplayName("RingBuffer RingBufferUtils ofCapacity creates buffer")
    void ringBufferUtilsOfCapacity() {
        RingBuffer<String> buffer = RingBuffer.RingBufferUtils.ofCapacity(5);

        assertEquals(5, buffer.capacity());
    }

    @Test
    @DisplayName("RingBuffer RingBufferUtils of creates with elements")
    void ringBufferUtilsOf() {
        RingBuffer<String> buffer = RingBuffer.RingBufferUtils.of(5, "a", "b");

        assertEquals(2, buffer.size());
    }

    @Test
    @DisplayName("RingBuffer SlidingWindow calculates average")
    void slidingWindowAverage() {
        RingBuffer.SlidingWindow<Integer> window = new RingBuffer.SlidingWindow<>(3);

        window.add(10);
        window.add(20);
        window.add(30);

        assertEquals(20.0, window.average());
        assertEquals(60.0, window.sum());
    }

    @Test
    @DisplayName("RingBuffer SlidingWindow slides when full")
    void slidingWindowSlides() {
        RingBuffer.SlidingWindow<Integer> window = new RingBuffer.SlidingWindow<>(2);

        window.add(10);
        window.add(20);
        window.add(30); // 10 slides out

        assertEquals(25.0, window.average());
        assertEquals(50.0, window.sum());
    }

    @Test
    @DisplayName("RingBuffer ThreadSafeRingBuffer is thread-safe")
    void threadSafeWorks() {
        RingBuffer.ThreadSafeRingBuffer<String> buffer = RingBuffer.RingBufferUtils.threadSafe(5);

        buffer.add("a");
        buffer.add("b");

        assertEquals(2, buffer.size());
        assertEquals("a", buffer.peek().orElse(null));
    }
}