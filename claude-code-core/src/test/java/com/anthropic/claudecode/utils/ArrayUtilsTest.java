/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ArrayUtils.
 */
class ArrayUtilsTest {

    @Test
    @DisplayName("ArrayUtils count with predicate counts matching")
    void countWithPredicate() {
        List<Integer> list = List.of(1, 2, 3, 4, 5);

        long count = ArrayUtils.count(list, n -> n > 2);

        assertEquals(3, count);
    }

    @Test
    @DisplayName("ArrayUtils count returns size")
    void countReturnsSize() {
        List<Integer> list = List.of(1, 2, 3);

        long count = ArrayUtils.count(list);

        assertEquals(3, count);
    }

    @Test
    @DisplayName("ArrayUtils first returns first element")
    void firstReturnsFirst() {
        List<Integer> list = List.of(1, 2, 3);

        Optional<Integer> first = ArrayUtils.first(list);

        assertTrue(first.isPresent());
        assertEquals(1, first.get());
    }

    @Test
    @DisplayName("ArrayUtils first returns empty for empty list")
    void firstEmpty() {
        List<Integer> list = List.of();

        Optional<Integer> first = ArrayUtils.first(list);

        assertFalse(first.isPresent());
    }

    @Test
    @DisplayName("ArrayUtils last returns last element")
    void lastReturnsLast() {
        List<Integer> list = List.of(1, 2, 3);

        Optional<Integer> last = ArrayUtils.last(list);

        assertTrue(last.isPresent());
        assertEquals(3, last.get());
    }

    @Test
    @DisplayName("ArrayUtils last returns empty for empty list")
    void lastEmpty() {
        List<Integer> list = List.of();

        Optional<Integer> last = ArrayUtils.last(list);

        assertFalse(last.isPresent());
    }

    @Test
    @DisplayName("ArrayUtils take returns first n elements")
    void takeWorks() {
        List<Integer> list = List.of(1, 2, 3, 4, 5);

        List<Integer> result = ArrayUtils.take(list, 3);

        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    @DisplayName("ArrayUtils take handles more than size")
    void takeMoreThanSize() {
        List<Integer> list = List.of(1, 2);

        List<Integer> result = ArrayUtils.take(list, 5);

        assertEquals(List.of(1, 2), result);
    }

    @Test
    @DisplayName("ArrayUtils drop skips first n elements")
    void dropWorks() {
        List<Integer> list = List.of(1, 2, 3, 4, 5);

        List<Integer> result = ArrayUtils.drop(list, 2);

        assertEquals(List.of(3, 4, 5), result);
    }

    @Test
    @DisplayName("ArrayUtils chunk splits into chunks")
    void chunkWorks() {
        List<Integer> list = List.of(1, 2, 3, 4, 5);

        List<List<Integer>> chunks = ArrayUtils.chunk(list, 2);

        assertEquals(3, chunks.size());
        assertEquals(List.of(1, 2), chunks.get(0));
        assertEquals(List.of(3, 4), chunks.get(1));
        assertEquals(List.of(5), chunks.get(2));
    }

    @Test
    @DisplayName("ArrayUtils flatten flattens nested list")
    void flattenWorks() {
        List<List<Integer>> nested = List.of(List.of(1, 2), List.of(3, 4));

        List<Integer> flat = ArrayUtils.flatten(nested);

        assertEquals(List.of(1, 2, 3, 4), flat);
    }

    @Test
    @DisplayName("ArrayUtils zip combines two lists")
    void zipWorks() {
        List<Integer> listA = List.of(1, 2, 3);
        List<String> listB = List.of("a", "b", "c");

        List<Map.Entry<Integer, String>> zipped = ArrayUtils.zip(listA, listB);

        assertEquals(3, zipped.size());
        assertEquals(1, zipped.get(0).getKey());
        assertEquals("a", zipped.get(0).getValue());
    }

    @Test
    @DisplayName("ArrayUtils zip handles different lengths")
    void zipDifferentLengths() {
        List<Integer> listA = List.of(1, 2);
        List<String> listB = List.of("a", "b", "c");

        List<Map.Entry<Integer, String>> zipped = ArrayUtils.zip(listA, listB);

        assertEquals(2, zipped.size());
    }

    @Test
    @DisplayName("ArrayUtils unique returns distinct elements")
    void uniqueWorks() {
        List<Integer> list = List.of(1, 2, 2, 3, 3, 3);

        List<Integer> unique = ArrayUtils.unique(list);

        assertEquals(List.of(1, 2, 3), unique);
    }

    @Test
    @DisplayName("ArrayUtils partition splits by predicate")
    void partitionWorks() {
        List<Integer> list = List.of(1, 2, 3, 4, 5);

        Map<Boolean, List<Integer>> partitioned = ArrayUtils.partition(list, n -> n % 2 == 0);

        assertEquals(List.of(2, 4), partitioned.get(true));
        assertEquals(List.of(1, 3, 5), partitioned.get(false));
    }

    @Test
    @DisplayName("ArrayUtils groupBy groups by key")
    void groupByWorks() {
        List<String> list = List.of("apple", "apricot", "banana", "blueberry");

        Map<Character, List<String>> grouped = ArrayUtils.groupBy(list, s -> s.charAt(0));

        assertEquals(2, grouped.size());
        assertEquals(List.of("apple", "apricot"), grouped.get('a'));
        assertEquals(List.of("banana", "blueberry"), grouped.get('b'));
    }
}