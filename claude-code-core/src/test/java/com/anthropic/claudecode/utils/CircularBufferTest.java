/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CircularBuffer.
 */
class CircularBufferTest {

    @Test
    @DisplayName("CircularBuffer starts empty")
    void startsEmpty() {
        CircularBuffer<String> buffer = new CircularBuffer<>(5);
        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.size());
        assertEquals(5, buffer.capacity());
    }

    @Test
    @DisplayName("add increases size")
    void addIncreasesSize() {
        CircularBuffer<Integer> buffer = new CircularBuffer<>(3);
        buffer.add(1);
        assertFalse(buffer.isEmpty());
        assertEquals(1, buffer.size());
        buffer.add(2);
        assertEquals(2, buffer.size());
        buffer.add(3);
        assertEquals(3, buffer.size());
        assertTrue(buffer.isFull());
    }

    @Test
    @DisplayName("get retrieves items in order")
    void getRetrievesItemsInOrder() {
        CircularBuffer<String> buffer = new CircularBuffer<>(5);
        buffer.add("a");
        buffer.add("b");
        buffer.add("c");

        assertEquals("a", buffer.get(0));
        assertEquals("b", buffer.get(1));
        assertEquals("c", buffer.get(2));
    }

    @Test
    @DisplayName("overflow replaces oldest items")
    void overflowReplacesOldest() {
        CircularBuffer<Integer> buffer = new CircularBuffer<>(3);
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        buffer.add(4);  // Overflows, removes 1
        buffer.add(5);  // Overflows, removes 2

        assertEquals(3, buffer.size());
        assertEquals(3, buffer.get(0));
        assertEquals(4, buffer.get(1));
        assertEquals(5, buffer.get(2));
    }

    @Test
    @DisplayName("toList returns all items")
    void toListReturnsAllItems() {
        CircularBuffer<String> buffer = new CircularBuffer<>(3);
        buffer.add("a");
        buffer.add("b");
        buffer.add("c");

        List<String> list = buffer.toList();
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

    @Test
    @DisplayName("clear resets buffer")
    void clearResetsBuffer() {
        CircularBuffer<Integer> buffer = new CircularBuffer<>(3);
        buffer.add(1);
        buffer.add(2);
        buffer.clear();

        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.size());
    }

    @Test
    @DisplayName("get throws for invalid index")
    void getThrowsForInvalidIndex() {
        CircularBuffer<Integer> buffer = new CircularBuffer<>(3);
        buffer.add(1);

        assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(5));
    }
}