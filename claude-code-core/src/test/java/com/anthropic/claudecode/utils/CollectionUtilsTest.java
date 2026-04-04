/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CollectionUtils.
 */
class CollectionUtilsTest {

    @Test
    @DisplayName("CollectionUtils listOf creates list")
    void listOfCreates() {
        List<String> list = CollectionUtils.listOf("a", "b", "c");

        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("CollectionUtils mutableListOf creates mutable list")
    void mutableListOfCreates() {
        List<String> list = CollectionUtils.mutableListOf("a", "b");

        list.add("c");
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("CollectionUtils setOf creates set")
    void setOfCreates() {
        Set<String> set = CollectionUtils.setOf("a", "b", "c");

        assertEquals(3, set.size());
    }

    @Test
    @DisplayName("CollectionUtils mapOf creates map")
    void mapOfCreates() {
        Map<String, Integer> map = CollectionUtils.mapOf(
            CollectionUtils.entry("a", 1),
            CollectionUtils.entry("b", 2)
        );

        assertEquals(2, map.size());
        assertEquals(1, map.get("a"));
    }

    @Test
    @DisplayName("CollectionUtils isEmpty checks collection")
    void isEmptyCollection() {
        assertTrue(CollectionUtils.isEmpty((Collection<?>) null));
        assertTrue(CollectionUtils.isEmpty(List.of()));
        assertFalse(CollectionUtils.isEmpty(List.of("a")));
    }

    @Test
    @DisplayName("CollectionUtils isEmpty checks map")
    void isEmptyMap() {
        assertTrue(CollectionUtils.isEmpty((Map<?, ?>) null));
        assertTrue(CollectionUtils.isEmpty(Map.of()));
        assertFalse(CollectionUtils.isEmpty(Map.of("a", 1)));
    }

    @Test
    @DisplayName("CollectionUtils isNotEmpty checks")
    void isNotEmpty() {
        assertFalse(CollectionUtils.isNotEmpty((Collection<?>) null));
        assertTrue(CollectionUtils.isNotEmpty(List.of("a")));
    }

    @Test
    @DisplayName("CollectionUtils firstOrNull returns first")
    void firstOrNull() {
        assertEquals("a", CollectionUtils.firstOrNull(List.of("a", "b")));
        assertNull(CollectionUtils.firstOrNull(null));
        assertNull(CollectionUtils.firstOrNull(List.of()));
    }

    @Test
    @DisplayName("CollectionUtils firstOrElse returns first or default")
    void firstOrElse() {
        assertEquals("a", CollectionUtils.firstOrElse(List.of("a"), "default"));
        assertEquals("default", CollectionUtils.firstOrElse(null, "default"));
    }

    @Test
    @DisplayName("CollectionUtils firstMatch finds matching")
    void firstMatch() {
        Optional<Integer> result = CollectionUtils.firstMatch(List.of(1, 2, 3, 4), i -> i > 2);

        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    @DisplayName("CollectionUtils lastOrNull returns last")
    void lastOrNull() {
        assertEquals("b", CollectionUtils.lastOrNull(List.of("a", "b")));
        assertNull(CollectionUtils.lastOrNull(null));
    }

    @Test
    @DisplayName("CollectionUtils partition splits list")
    void partition() {
        CollectionUtils.Partitioned<Integer> result =
            CollectionUtils.partition(List.of(1, 2, 3, 4, 5), i -> i % 2 == 0);

        assertEquals(2, result.matching().size());
        assertEquals(3, result.notMatching().size());
    }

    @Test
    @DisplayName("CollectionUtils chunked splits into chunks")
    void chunked() {
        List<List<Integer>> chunks = CollectionUtils.chunked(List.of(1, 2, 3, 4, 5), 2);

        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
    }

    @Test
    @DisplayName("CollectionUtils flatten flattens lists")
    void flatten() {
        List<Integer> flat = CollectionUtils.flatten(List.of(1, 2), List.of(3, 4));

        assertEquals(4, flat.size());
    }

    @Test
    @DisplayName("CollectionUtils distinctBy removes duplicates")
    void distinctBy() {
        List<String> result = CollectionUtils.distinctBy(List.of("a", "bb", "c", "ddd"), String::length);

        assertEquals(3, result.size()); // "a", "bb", "ddd" (first of each length)
    }

    @Test
    @DisplayName("CollectionUtils groupBy groups elements")
    void groupBy() {
        Map<Integer, List<String>> groups = CollectionUtils.groupBy(
            List.of("a", "bb", "c", "dd"), String::length
        );

        assertEquals(2, groups.size());
        assertEquals(2, groups.get(1).size());
    }

    @Test
    @DisplayName("CollectionUtils associate creates map")
    void associate() {
        Map<String, Integer> map = CollectionUtils.associate(
            List.of("a", "bb"),
            s -> s,
            String::length
        );

        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("bb"));
    }

    @Test
    @DisplayName("CollectionUtils zip combines lists")
    void zip() {
        List<CollectionUtils.Pair<String, Integer>> pairs =
            CollectionUtils.zip(List.of("a", "b"), List.of(1, 2));

        assertEquals(2, pairs.size());
        assertEquals("a", pairs.get(0).first());
        assertEquals(1, pairs.get(0).second());
    }

    @Test
    @DisplayName("CollectionUtils zipWithIndex adds indices")
    void zipWithIndex() {
        List<CollectionUtils.Indexed<String>> indexed =
            CollectionUtils.zipWithIndex(List.of("a", "b", "c"));

        assertEquals(3, indexed.size());
        assertEquals(0, indexed.get(0).index());
        assertEquals("a", indexed.get(0).value());
    }

    @Test
    @DisplayName("CollectionUtils take takes first N")
    void take() {
        List<Integer> result = CollectionUtils.take(List.of(1, 2, 3, 4, 5), 3);

        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
    }

    @Test
    @DisplayName("CollectionUtils drop drops first N")
    void drop() {
        List<Integer> result = CollectionUtils.drop(List.of(1, 2, 3, 4, 5), 2);

        assertEquals(3, result.size());
        assertEquals(3, result.get(0));
    }

    @Test
    @DisplayName("CollectionUtils takeWhile takes while true")
    void takeWhile() {
        List<Integer> result = CollectionUtils.takeWhile(List.of(1, 2, 3, 4, 5), i -> i < 4);

        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("CollectionUtils dropWhile drops while true")
    void dropWhile() {
        List<Integer> result = CollectionUtils.dropWhile(List.of(1, 2, 3, 4, 5), i -> i < 4);

        assertEquals(2, result.size());
        assertEquals(4, result.get(0));
    }

    @Test
    @DisplayName("CollectionUtils windowed creates windows")
    void windowed() {
        List<List<Integer>> windows = CollectionUtils.windowed(List.of(1, 2, 3, 4), 2);

        assertEquals(3, windows.size());
    }

    @Test
    @DisplayName("CollectionUtils reversed reverses list")
    void reversed() {
        List<Integer> result = CollectionUtils.reversed(List.of(1, 2, 3));

        assertEquals(3, result.get(0));
        assertEquals(1, result.get(2));
    }

    @Test
    @DisplayName("CollectionUtils sortedBy sorts")
    void sortedBy() {
        List<String> result = CollectionUtils.sortedBy(List.of("ccc", "a", "bb"), String::length);

        assertEquals("a", result.get(0));
        assertEquals("ccc", result.get(2));
    }

    @Test
    @DisplayName("CollectionUtils sumInts sums integers")
    void sumInts() {
        assertEquals(10, CollectionUtils.sumInts(List.of(1, 2, 3, 4)));
    }

    @Test
    @DisplayName("CollectionUtils sumLongs sums longs")
    void sumLongs() {
        assertEquals(10L, CollectionUtils.sumLongs(List.of(1L, 2L, 3L, 4L)));
    }

    @Test
    @DisplayName("CollectionUtils frequencies counts occurrences")
    void frequencies() {
        Map<String, Long> freq = CollectionUtils.frequencies(List.of("a", "b", "a", "c", "a"));

        assertEquals(3L, freq.get("a"));
        assertEquals(1L, freq.get("b"));
    }

    @Test
    @DisplayName("CollectionUtils getOrElse safe get")
    void getOrElse() {
        assertEquals("a", CollectionUtils.getOrElse(List.of("a", "b"), 0, "default"));
        assertEquals("default", CollectionUtils.getOrElse(List.of("a"), 5, "default"));
    }

    @Test
    @DisplayName("CollectionUtils getOrElse map safe get")
    void getOrElseMap() {
        Map<String, Integer> map = Map.of("a", 1);

        assertEquals(1, CollectionUtils.getOrElse(map, "a", 0));
        assertEquals(0, CollectionUtils.getOrElse(map, "b", 0));
    }

    @Test
    @DisplayName("CollectionUtils join joins collection")
    void join() {
        assertEquals("a,b,c", CollectionUtils.join(List.of("a", "b", "c"), ","));
    }

    @Test
    @DisplayName("CollectionUtils join with prefix/suffix")
    void joinWithPrefixSuffix() {
        assertEquals("[a,b,c]", CollectionUtils.join(List.of("a", "b", "c"), ",", "[", "]"));
    }

    @Test
    @DisplayName("CollectionUtils mapNotNull maps and filters nulls")
    void mapNotNull() {
        List<Integer> result = CollectionUtils.mapNotNull(List.of("a", "", "c"), s ->
            s.isEmpty() ? null : s.length()
        );

        assertEquals(2, result.size());
        assertEquals(1, result.get(0));
    }

    @Test
    @DisplayName("CollectionUtils rotate rotates list")
    void rotate() {
        List<Integer> result = CollectionUtils.rotate(List.of(1, 2, 3, 4), 1);

        assertEquals(2, result.get(0));
        assertEquals(1, result.get(3));
    }

    @Test
    @DisplayName("CollectionUtils findDuplicates finds duplicates")
    void findDuplicates() {
        Set<Integer> dups = CollectionUtils.findDuplicates(List.of(1, 2, 1, 3, 2, 4));

        assertEquals(2, dups.size());
        assertTrue(dups.contains(1));
        assertTrue(dups.contains(2));
    }
}