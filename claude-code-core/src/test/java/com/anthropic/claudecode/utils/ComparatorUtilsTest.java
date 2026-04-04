/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ComparatorUtils.
 */
class ComparatorUtilsTest {

    @Test
    @DisplayName("ComparatorUtils naturalOrder compares")
    void naturalOrder() {
        Comparator<Integer> cmp = ComparatorUtils.naturalOrder();

        assertTrue(cmp.compare(1, 2) < 0);
        assertTrue(cmp.compare(2, 1) > 0);
        assertEquals(0, cmp.compare(1, 1));
    }

    @Test
    @DisplayName("ComparatorUtils reverseOrder compares reversed")
    void reverseOrder() {
        Comparator<Integer> cmp = ComparatorUtils.reverseOrder();

        assertTrue(cmp.compare(1, 2) > 0);
        assertTrue(cmp.compare(2, 1) < 0);
    }

    @Test
    @DisplayName("ComparatorUtils nullsFirst null comes first")
    void nullsFirst() {
        Comparator<String> cmp = ComparatorUtils.nullsFirst(String::compareTo);

        assertTrue(cmp.compare(null, "a") < 0);
        assertTrue(cmp.compare("a", null) > 0);
        assertEquals(0, cmp.compare(null, null));
    }

    @Test
    @DisplayName("ComparatorUtils nullsLast null comes last")
    void nullsLast() {
        Comparator<String> cmp = ComparatorUtils.nullsLast(String::compareTo);

        assertTrue(cmp.compare(null, "a") > 0);
        assertTrue(cmp.compare("a", null) < 0);
    }

    @Test
    @DisplayName("ComparatorUtils comparing by key")
    void comparing() {
        Comparator<String> cmp = ComparatorUtils.comparing(String::length);

        assertTrue(cmp.compare("a", "ab") < 0);
        assertTrue(cmp.compare("ab", "a") > 0);
    }

    @Test
    @DisplayName("ComparatorUtils comparingInt")
    void comparingInt() {
        Comparator<String> cmp = ComparatorUtils.comparingInt(String::length);

        assertTrue(cmp.compare("a", "abc") < 0);
    }

    @Test
    @DisplayName("ComparatorUtils chain chains comparators")
    void chain() {
        Comparator<String> cmp = ComparatorUtils.chain(
            ComparatorUtils.byLength(),
            String::compareTo
        );

        // Same length, so use natural order: "ab" < "cd"
        assertTrue(cmp.compare("ab", "cd") < 0);
        // Different length: shorter first
        assertTrue(cmp.compare("a", "ab") < 0);
    }

    @Test
    @DisplayName("ComparatorUtils chain from list")
    void chainList() {
        List<Comparator<String>> comparators = List.of(
            ComparatorUtils.byLength(),
            String::compareTo
        );
        Comparator<String> cmp = ComparatorUtils.chain(comparators);

        // Same length, so use natural order
        assertTrue(cmp.compare("ab", "cd") < 0);
    }

    @Test
    @DisplayName("ComparatorUtils comparingBy multiple keys")
    void comparingBy() {
        Comparator<String> cmp = ComparatorUtils.<String, Integer>comparingBy(String::length, String::length);

        assertEquals(0, cmp.compare("ab", "cd"));
        assertTrue(cmp.compare("ab", "abc") < 0);
    }

    @Test
    @DisplayName("ComparatorUtils reversed")
    void reversed() {
        Comparator<Integer> natural = ComparatorUtils.<Integer>naturalOrder();
        Comparator<Integer> cmp = ComparatorUtils.reversed(natural);

        assertTrue(cmp.compare(1, 2) > 0);
    }

    @Test
    @DisplayName("ComparatorUtils fromPredicate")
    void fromPredicate() {
        Comparator<Integer> cmp = ComparatorUtils.fromPredicate(i -> i > 5);

        assertTrue(cmp.compare(10, 3) < 0); // true first
        assertTrue(cmp.compare(3, 10) > 0);
    }

    @Test
    @DisplayName("ComparatorUtils allEqual returns 0")
    void allEqual() {
        Comparator<Integer> cmp = ComparatorUtils.allEqual();

        assertEquals(0, cmp.compare(1, 2));
        assertEquals(0, cmp.compare(100, 200));
    }

    @Test
    @DisplayName("ComparatorUtils byOrder list")
    void byOrderList() {
        Comparator<String> cmp = ComparatorUtils.byOrder(List.of("a", "b", "c"));

        assertTrue(cmp.compare("a", "b") < 0);
        assertTrue(cmp.compare("c", "a") > 0);
    }

    @Test
    @DisplayName("ComparatorUtils byOrder varargs")
    void byOrderVarargs() {
        Comparator<String> cmp = ComparatorUtils.byOrder("a", "b", "c");

        assertTrue(cmp.compare("a", "b") < 0);
    }

    @Test
    @DisplayName("ComparatorUtils lexicographic for lists")
    void lexicographic() {
        Comparator<List<Integer>> cmp = ComparatorUtils.lexicographic(Integer::compareTo);

        assertTrue(cmp.compare(List.of(1, 2), List.of(1, 3)) < 0);
        assertTrue(cmp.compare(List.of(1, 2, 3), List.of(1, 2)) > 0);
    }

    @Test
    @DisplayName("ComparatorUtils arrayLexicographic")
    void arrayLexicographic() {
        Comparator<Integer[]> cmp = ComparatorUtils.arrayLexicographic(Integer::compareTo);

        assertTrue(cmp.compare(new Integer[]{1, 2}, new Integer[]{1, 3}) < 0);
    }

    @Test
    @DisplayName("ComparatorUtils byLength")
    void byLength() {
        Comparator<String> cmp = ComparatorUtils.byLength();

        assertTrue(cmp.compare("a", "abc") < 0);
    }

    @Test
    @DisplayName("ComparatorUtils byLengthThenNatural")
    void byLengthThenNatural() {
        Comparator<String> cmp = ComparatorUtils.byLengthThenNatural();

        assertTrue(cmp.compare("ab", "cd") < 0); // same length, natural order
    }

    @Test
    @DisplayName("ComparatorUtils byLengthDescending")
    void byLengthDescending() {
        Comparator<String> cmp = ComparatorUtils.byLengthDescending();

        assertTrue(cmp.compare("abc", "a") < 0);
    }

    @Test
    @DisplayName("ComparatorUtils ignoringCase")
    void ignoringCase() {
        Comparator<String> cmp = ComparatorUtils.ignoringCase();

        assertEquals(0, cmp.compare("ABC", "abc"));
    }

    @Test
    @DisplayName("ComparatorUtils byToString")
    void byToString() {
        Comparator<Integer> cmp = ComparatorUtils.byToString();

        assertTrue(cmp.compare(10, 2) < 0); // "10" < "2" lexicographically
    }

    @Test
    @DisplayName("ComparatorUtils optionalFirst empty first")
    void optionalFirst() {
        Comparator<Optional<Integer>> cmp = ComparatorUtils.optionalFirst();

        assertTrue(cmp.compare(Optional.empty(), Optional.of(1)) < 0);
        assertEquals(0, cmp.compare(Optional.empty(), Optional.empty()));
    }

    @Test
    @DisplayName("ComparatorUtils optionalLast empty last")
    void optionalLast() {
        Comparator<Optional<Integer>> cmp = ComparatorUtils.optionalLast();

        assertTrue(cmp.compare(Optional.empty(), Optional.of(1)) > 0);
    }

    @Test
    @DisplayName("ComparatorUtils comparingBoolean")
    void comparingBoolean() {
        Comparator<Integer> cmp = ComparatorUtils.comparingBoolean(i -> i > 5);

        assertTrue(cmp.compare(3, 10) < 0); // false < true
    }

    @Test
    @DisplayName("ComparatorUtils comparingBooleanDesc")
    void comparingBooleanDesc() {
        Comparator<Integer> cmp = ComparatorUtils.comparingBooleanDesc(i -> i > 5);

        assertTrue(cmp.compare(10, 3) < 0); // true first
    }

    @Test
    @DisplayName("ComparatorUtils fromPredicate BiPredicate")
    void fromPredicateBiPredicate() {
        Comparator<Integer> cmp = ComparatorUtils.fromPredicate((a, b) -> a < b);

        assertTrue(cmp.compare(1, 2) < 0);
        assertTrue(cmp.compare(2, 1) > 0);
    }

    @Test
    @DisplayName("ComparatorUtils min finds minimum")
    void min() {
        Optional<Integer> result = ComparatorUtils.min(List.of(3, 1, 2), Integer::compareTo);

        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    @DisplayName("ComparatorUtils max finds maximum")
    void max() {
        Optional<Integer> result = ComparatorUtils.max(List.of(3, 1, 2), Integer::compareTo);

        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    @DisplayName("ComparatorUtils median finds median")
    void median() {
        Optional<Integer> result = ComparatorUtils.median(List.of(1, 2, 3, 4, 5), Integer::compareTo);

        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    @DisplayName("ComparatorUtils percentile finds element")
    void percentile() {
        List<Integer> list = List.of(1, 2, 3, 4, 5);
        Optional<Integer> result = ComparatorUtils.percentile(list, 0.5, Integer::compareTo);

        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("ComparatorUtils sorted returns sorted list")
    void sorted() {
        List<Integer> result = ComparatorUtils.sorted(List.of(3, 1, 2), Integer::compareTo);

        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    @DisplayName("ComparatorUtils sortedDistinct removes duplicates")
    void sortedDistinct() {
        List<Integer> result = ComparatorUtils.sortedDistinct(List.of(3, 1, 2, 1, 3), Integer::compareTo);

        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    @DisplayName("ComparatorUtils topN returns top elements")
    void topN() {
        List<Integer> result = ComparatorUtils.topN(List.of(1, 2, 3, 4, 5), 3, Integer::compareTo);

        assertEquals(List.of(5, 4, 3), result);
    }

    @Test
    @DisplayName("ComparatorUtils bottomN returns bottom elements")
    void bottomN() {
        List<Integer> result = ComparatorUtils.bottomN(List.of(1, 2, 3, 4, 5), 3, Integer::compareTo);

        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    @DisplayName("ComparatorUtils isSorted true for sorted")
    void isSortedTrue() {
        assertTrue(ComparatorUtils.isSorted(List.of(1, 2, 3), Integer::compareTo));
    }

    @Test
    @DisplayName("ComparatorUtils isSorted false for unsorted")
    void isSortedFalse() {
        assertFalse(ComparatorUtils.isSorted(List.of(3, 1, 2), Integer::compareTo));
    }

    @Test
    @DisplayName("ComparatorUtils binarySearchInsertionPoint")
    void binarySearchInsertionPoint() {
        List<Integer> sorted = List.of(1, 3, 5, 7);
        int pos = ComparatorUtils.binarySearchInsertionPoint(sorted, 4, Integer::compareTo);

        assertEquals(2, pos); // Insert between 3 and 5
    }

    @Test
    @DisplayName("ComparatorUtils ToBooleanFunction interface")
    void toBooleanFunction() {
        ComparatorUtils.ToBooleanFunction<String> func = s -> s.isEmpty();
        assertFalse(func.applyAsBoolean("test"));
        assertTrue(func.applyAsBoolean(""));
    }
}