/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ArrayUtil.
 */
class ArrayUtilTest {

    @Test
    @DisplayName("ArrayUtil intersperse with function inserts separators")
    void intersperseFunction() {
        List<String> items = List.of("a", "b", "c");
        Function<Integer, String> separator = i -> "-";

        List<String> result = ArrayUtil.intersperse(items, separator);

        assertEquals(List.of("a", "-", "b", "-", "c"), result);
    }

    @Test
    @DisplayName("ArrayUtil intersperse handles empty list")
    void intersperseEmpty() {
        List<String> items = List.of();
        Function<Integer, String> separator = i -> "-";

        List<String> result = ArrayUtil.intersperse(items, separator);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("ArrayUtil intersperse handles null")
    void intersperseNull() {
        Function<Integer, String> separator = i -> "-";

        List<String> result = ArrayUtil.intersperse((List<String>) null, separator);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("ArrayUtil intersperseConstant inserts constant separator")
    void intersperseConstant() {
        List<Integer> items = List.of(1, 2, 3);

        List<Integer> result = ArrayUtil.intersperseConstant(items, 0);

        assertEquals(List.of(1, 0, 2, 0, 3), result);
    }

    @Test
    @DisplayName("ArrayUtil intersperse with constant value")
    void intersperseConstantValue() {
        List<String> items = List.of("a", "b", "c");

        List<String> result = ArrayUtil.intersperseConstant(items, ",");

        assertEquals(List.of("a", ",", "b", ",", "c"), result);
    }

    @Test
    @DisplayName("ArrayUtil count counts matching elements")
    void countWorks() {
        List<Integer> items = List.of(1, 2, 3, 4, 5, 6);
        Predicate<Integer> isEven = x -> x % 2 == 0;

        int count = ArrayUtil.count(items, isEven);

        assertEquals(3, count);
    }

    @Test
    @DisplayName("ArrayUtil count with iterable")
    void countIterable() {
        Predicate<String> startsWithA = s -> s.startsWith("a");

        int count = ArrayUtil.count(List.of("apple", "banana", "avocado"), startsWithA);

        assertEquals(2, count);
    }

    @Test
    @DisplayName("ArrayUtil uniq returns unique elements")
    void uniqWorks() {
        List<Integer> items = List.of(1, 2, 2, 3, 3, 3, 1);

        List<Integer> result = ArrayUtil.uniq(items);

        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    @DisplayName("ArrayUtil uniqBy returns unique by key")
    void uniqByWorks() {
        List<String> items = List.of("apple", "apricot", "banana", "blueberry");
        Function<String, Character> firstChar = s -> s.charAt(0);

        List<String> result = ArrayUtil.uniqBy(items, firstChar);

        assertEquals(2, result.size());
        assertEquals("apple", result.get(0));
        assertEquals("banana", result.get(1));
    }

    @Test
    @DisplayName("ArrayUtil chunk splits into groups")
    void chunkWorks() {
        List<Integer> items = List.of(1, 2, 3, 4, 5, 6, 7);

        List<List<Integer>> result = ArrayUtil.chunk(items, 3);

        assertEquals(3, result.size());
        assertEquals(List.of(1, 2, 3), result.get(0));
        assertEquals(List.of(4, 5, 6), result.get(1));
        assertEquals(List.of(7), result.get(2));
    }

    @Test
    @DisplayName("ArrayUtil flatten flattens list of lists")
    void flattenWorks() {
        List<List<Integer>> lists = List.of(
            List.of(1, 2),
            List.of(3, 4),
            List.of(5)
        );

        List<Integer> result = ArrayUtil.flatten(lists);

        assertEquals(List.of(1, 2, 3, 4, 5), result);
    }

    @Test
    @DisplayName("ArrayUtil zip combines two lists")
    void zipWorks() {
        List<String> as = List.of("a", "b", "c");
        List<Integer> bs = List.of(1, 2, 3);

        List<Map.Entry<String, Integer>> result = ArrayUtil.zip(as, bs);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals(1, result.get(0).getValue());
    }

    @Test
    @DisplayName("ArrayUtil zip handles different lengths")
    void zipDifferentLengths() {
        List<String> as = List.of("a", "b", "c", "d");
        List<Integer> bs = List.of(1, 2);

        List<Map.Entry<String, Integer>> result = ArrayUtil.zip(as, bs);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("ArrayUtil take takes first n elements")
    void takeWorks() {
        List<Integer> items = List.of(1, 2, 3, 4, 5);

        List<Integer> result = ArrayUtil.take(items, 3);

        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    @DisplayName("ArrayUtil take handles n larger than list")
    void takeMoreThanList() {
        List<Integer> items = List.of(1, 2, 3);

        List<Integer> result = ArrayUtil.take(items, 10);

        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    @DisplayName("ArrayUtil drop drops first n elements")
    void dropWorks() {
        List<Integer> items = List.of(1, 2, 3, 4, 5);

        List<Integer> result = ArrayUtil.drop(items, 2);

        assertEquals(List.of(3, 4, 5), result);
    }

    @Test
    @DisplayName("ArrayUtil last returns last element")
    void lastWorks() {
        List<Integer> items = List.of(1, 2, 3);

        Integer result = ArrayUtil.last(items);

        assertEquals(3, result);
    }

    @Test
    @DisplayName("ArrayUtil last returns null for empty list")
    void lastEmpty() {
        Integer result = ArrayUtil.last(List.of());

        assertNull(result);
    }

    @Test
    @DisplayName("ArrayUtil first returns first element")
    void firstWorks() {
        List<Integer> items = List.of(1, 2, 3);

        Integer result = ArrayUtil.first(items);

        assertEquals(1, result);
    }

    @Test
    @DisplayName("ArrayUtil first returns null for empty list")
    void firstEmpty() {
        Integer result = ArrayUtil.first(List.of());

        assertNull(result);
    }

    @Test
    @DisplayName("ArrayUtil partition splits by predicate")
    void partitionWorks() {
        List<Integer> items = List.of(1, 2, 3, 4, 5, 6);
        Predicate<Integer> isEven = x -> x % 2 == 0;

        Map.Entry<List<Integer>, List<Integer>> result = ArrayUtil.partition(items, isEven);

        assertEquals(List.of(2, 4, 6), result.getKey());
        assertEquals(List.of(1, 3, 5), result.getValue());
    }

    @Test
    @DisplayName("ArrayUtil groupBy groups by key")
    void groupByWorks() {
        List<String> items = List.of("apple", "apricot", "banana", "cherry");
        Function<String, Character> firstChar = s -> s.charAt(0);

        Map<Character, List<String>> result = ArrayUtil.groupBy(items, firstChar);

        assertEquals(3, result.size());
        assertEquals(List.of("apple", "apricot"), result.get('a'));
        assertEquals(List.of("banana"), result.get('b'));
        assertEquals(List.of("cherry"), result.get('c'));
    }

    @Test
    @DisplayName("ArrayUtil findIndex finds first matching index")
    void findIndexWorks() {
        List<Integer> items = List.of(1, 2, 3, 4, 5);
        Predicate<Integer> isGreaterThanThree = x -> x > 3;

        int index = ArrayUtil.findIndex(items, isGreaterThanThree);

        assertEquals(3, index);
    }

    @Test
    @DisplayName("ArrayUtil findIndex returns -1 when not found")
    void findIndexNotFound() {
        List<Integer> items = List.of(1, 2, 3);
        Predicate<Integer> isGreaterThanTen = x -> x > 10;

        int index = ArrayUtil.findIndex(items, isGreaterThanTen);

        assertEquals(-1, index);
    }

    @Test
    @DisplayName("ArrayUtil findLastIndex finds last matching index")
    void findLastIndexWorks() {
        List<Integer> items = List.of(1, 2, 3, 4, 3, 2, 1);
        Predicate<Integer> isThree = x -> x == 3;

        int index = ArrayUtil.findLastIndex(items, isThree);

        assertEquals(4, index);
    }

    @Test
    @DisplayName("ArrayUtil reverse reverses list")
    void reverseWorks() {
        List<Integer> items = List.of(1, 2, 3, 4, 5);

        List<Integer> result = ArrayUtil.reverse(items);

        assertEquals(List.of(5, 4, 3, 2, 1), result);
    }

    @Test
    @DisplayName("ArrayUtil reverse does not modify original")
    void reverseOriginalUnchanged() {
        List<Integer> items = List.of(1, 2, 3);

        ArrayUtil.reverse(items);

        assertEquals(List.of(1, 2, 3), items);
    }
}