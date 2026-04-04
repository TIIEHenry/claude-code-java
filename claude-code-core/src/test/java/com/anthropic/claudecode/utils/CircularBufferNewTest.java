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
 * Tests for CircularBufferNew.
 */
class CircularBufferNewTest {

    @Test
    @DisplayName("CircularBufferNew constructor creates buffer")
    void constructor() {
        CircularBufferNew<String> buffer = new CircularBufferNew<>(5);

        assertEquals(5, buffer.capacity());
        assertEquals(0, buffer.size());
        assertTrue(buffer.isEmpty());
    }

    @Test
    @DisplayName("CircularBufferNew constructor throws for invalid capacity")
    void constructorInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new CircularBufferNew<>(0));
        assertThrows(IllegalArgumentException.class, () -> new CircularBufferNew<>(-1));
    }

    @Test
    @DisplayName("CircularBufferNew add increases size")
    void addIncreasesSize() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.add(1);
        assertEquals(1, buffer.size());
        buffer.add(2);
        assertEquals(2, buffer.size());
    }

    @Test
    @DisplayName("CircularBufferNew add evicts oldest when full")
    void addEvictsOldest() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(3);

        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        buffer.add(4);

        assertEquals(3, buffer.size());
        assertEquals(List.of(2, 3, 4), buffer.toArray());
    }

    @Test
    @DisplayName("CircularBufferNew addAll collection")
    void addAllCollection() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.addAll(List.of(1, 2, 3));

        assertEquals(3, buffer.size());
        assertEquals(List.of(1, 2, 3), buffer.toArray());
    }

    @Test
    @DisplayName("CircularBufferNew addAll array")
    void addAllArray() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.addAll(new Integer[]{1, 2, 3});

        assertEquals(3, buffer.size());
    }

    @Test
    @DisplayName("CircularBufferNew getRecent returns recent items")
    void getRecent() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.addAll(List.of(1, 2, 3, 4, 5));

        assertEquals(List.of(3, 4, 5), buffer.getRecent(3));
        assertEquals(List.of(1, 2, 3, 4, 5), buffer.getRecent(10));
    }

    @Test
    @DisplayName("CircularBufferNew toArray returns all items")
    void toArray() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.addAll(List.of(1, 2, 3));

        assertEquals(List.of(1, 2, 3), buffer.toArray());
    }

    @Test
    @DisplayName("CircularBufferNew toArray empty buffer")
    void toArrayEmpty() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        assertEquals(List.of(), buffer.toArray());
    }

    @Test
    @DisplayName("CircularBufferNew getOldest returns oldest")
    void getOldest() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.addAll(List.of(1, 2, 3));

        assertEquals(Optional.of(1), buffer.getOldest());
    }

    @Test
    @DisplayName("CircularBufferNew getOldest empty returns empty")
    void getOldestEmpty() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        assertEquals(Optional.empty(), buffer.getOldest());
    }

    @Test
    @DisplayName("CircularBufferNew getNewest returns newest")
    void getNewest() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.addAll(List.of(1, 2, 3));

        assertEquals(Optional.of(3), buffer.getNewest());
    }

    @Test
    @DisplayName("CircularBufferNew getNewest empty returns empty")
    void getNewestEmpty() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        assertEquals(Optional.empty(), buffer.getNewest());
    }

    @Test
    @DisplayName("CircularBufferNew get by position")
    void getByPosition() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.addAll(List.of(1, 2, 3));

        assertEquals(Optional.of(1), buffer.get(0));
        assertEquals(Optional.of(2), buffer.get(1));
        assertEquals(Optional.of(3), buffer.get(2));
    }

    @Test
    @DisplayName("CircularBufferNew get invalid position returns empty")
    void getInvalidPosition() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.addAll(List.of(1, 2, 3));

        assertEquals(Optional.empty(), buffer.get(-1));
        assertEquals(Optional.empty(), buffer.get(10));
    }

    @Test
    @DisplayName("CircularBufferNew clear empties buffer")
    void clear() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.addAll(List.of(1, 2, 3));
        buffer.clear();

        assertEquals(0, buffer.size());
        assertTrue(buffer.isEmpty());
    }

    @Test
    @DisplayName("CircularBufferNew isFull returns true when full")
    void isFull() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(3);

        assertFalse(buffer.isFull());
        buffer.addAll(List.of(1, 2, 3));
        assertTrue(buffer.isFull());
    }

    @Test
    @DisplayName("CircularBufferNew removeOldest removes and returns oldest")
    void removeOldest() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.add(1);
        buffer.add(2);

        assertEquals(Optional.of(1), buffer.removeOldest());
        assertEquals(1, buffer.size());
    }

    @Test
    @DisplayName("CircularBufferNew removeOldest empty returns empty")
    void removeOldestEmpty() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        assertEquals(Optional.empty(), buffer.removeOldest());
    }

    @Test
    @DisplayName("CircularBufferNew contains returns true for present item")
    void containsTrue() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.addAll(List.of(1, 2, 3));

        assertTrue(buffer.contains(2));
    }

    @Test
    @DisplayName("CircularBufferNew contains returns false for missing item")
    void containsFalse() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.addAll(List.of(1, 2, 3));

        assertFalse(buffer.contains(5));
    }

    @Test
    @DisplayName("CircularBufferNew stream returns stream")
    void stream() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.addAll(List.of(1, 2, 3));

        assertEquals(3, buffer.stream().count());
    }

    @Test
    @DisplayName("CircularBufferNew iterator iterates in order")
    void iterator() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.addAll(List.of(1, 2, 3));

        int sum = 0;
        for (Integer i : buffer.toArray()) {
            sum += i;
        }
        assertEquals(6, sum);
    }

    @Test
    @DisplayName("CircularBufferNew forEach iterates in order")
    void forEach() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.addAll(List.of(1, 2, 3));

        int[] sum = {0};
        buffer.forEach(i -> sum[0] += i);

        assertEquals(6, sum[0]);
    }

    @Test
    @DisplayName("CircularBufferNew toString returns string")
    void toStringTest() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(5);

        buffer.addAll(List.of(1, 2, 3));

        String str = buffer.toString();

        assertTrue(str.contains("CircularBufferNew"));
        assertTrue(str.contains("size=3"));
    }

    @Test
    @DisplayName("CircularBufferNew wraps around correctly")
    void wrapsAround() {
        CircularBufferNew<Integer> buffer = new CircularBufferNew<>(3);

        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        buffer.add(4);
        buffer.add(5);

        assertEquals(List.of(3, 4, 5), buffer.toArray());
        assertEquals(Optional.of(3), buffer.getOldest());
        assertEquals(Optional.of(5), buffer.getNewest());
    }
}