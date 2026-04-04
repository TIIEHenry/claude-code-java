/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TupleUtils.
 */
class TupleUtilsTest {

    @Test
    @DisplayName("TupleUtils Pair of creates pair")
    void pairOf() {
        TupleUtils.Pair<String, Integer> pair = TupleUtils.Pair.of("key", 42);

        assertEquals("key", pair.first());
        assertEquals(42, pair.second());
    }

    @Test
    @DisplayName("TupleUtils Pair apply uses bifunction")
    void pairApply() {
        TupleUtils.Pair<Integer, Integer> pair = new TupleUtils.Pair<>(3, 4);

        int result = pair.apply((a, b) -> a + b);

        assertEquals(7, result);
    }

    @Test
    @DisplayName("TupleUtils Pair map transforms both")
    void pairMap() {
        TupleUtils.Pair<String, Integer> pair = new TupleUtils.Pair<>("hello", 5);
        TupleUtils.Pair<Integer, String> mapped = pair.map(String::length, i -> "val:" + i);

        assertEquals(5, mapped.first());
        assertEquals("val:5", mapped.second());
    }

    @Test
    @DisplayName("TupleUtils Pair mapFirst transforms first")
    void pairMapFirst() {
        TupleUtils.Pair<String, Integer> pair = new TupleUtils.Pair<>("hello", 42);

        TupleUtils.Pair<Integer, Integer> mapped = pair.mapFirst(String::length);

        assertEquals(5, mapped.first());
        assertEquals(42, mapped.second());
    }

    @Test
    @DisplayName("TupleUtils Pair mapSecond transforms second")
    void pairMapSecond() {
        TupleUtils.Pair<String, Integer> pair = new TupleUtils.Pair<>("hello", 5);

        TupleUtils.Pair<String, String> mapped = pair.mapSecond(i -> "len:" + i);

        assertEquals("hello", mapped.first());
        assertEquals("len:5", mapped.second());
    }

    @Test
    @DisplayName("TupleUtils Pair swap exchanges values")
    void pairSwap() {
        TupleUtils.Pair<String, Integer> pair = new TupleUtils.Pair<>("key", 42);

        TupleUtils.Pair<Integer, String> swapped = pair.swap();

        assertEquals(42, swapped.first());
        assertEquals("key", swapped.second());
    }

    @Test
    @DisplayName("TupleUtils Pair toList creates list")
    void pairToList() {
        TupleUtils.Pair<String, Integer> pair = new TupleUtils.Pair<>("a", 1);

        List<Object> list = pair.toList();

        assertEquals(2, list.size());
        assertEquals("a", list.get(0));
        assertEquals(1, list.get(1));
    }

    @Test
    @DisplayName("TupleUtils Triple of creates triple")
    void tripleOf() {
        TupleUtils.Triple<String, Integer, Boolean> triple = TupleUtils.Triple.of("a", 1, true);

        assertEquals("a", triple.first());
        assertEquals(1, triple.second());
        assertTrue(triple.third());
    }

    @Test
    @DisplayName("TupleUtils Triple dropThird returns pair")
    void tripleDropThird() {
        TupleUtils.Triple<String, Integer, Boolean> triple = new TupleUtils.Triple<>("a", 1, true);

        TupleUtils.Pair<String, Integer> pair = triple.dropThird();

        assertEquals("a", pair.first());
        assertEquals(1, pair.second());
    }

    @Test
    @DisplayName("TupleUtils Quad of creates quad")
    void quadOf() {
        TupleUtils.Quad<String, Integer, Boolean, Double> quad = TupleUtils.Quad.of("a", 1, true, 2.0);

        assertEquals("a", quad.first());
        assertEquals(1, quad.second());
        assertTrue(quad.third());
        assertEquals(2.0, quad.fourth());
    }

    @Test
    @DisplayName("TupleUtils Quint of creates quint")
    void quintOf() {
        TupleUtils.Quint<String, Integer, Boolean, Double, Long> quint =
            TupleUtils.Quint.of("a", 1, true, 2.0, 3L);

        assertEquals("a", quint.first());
        assertEquals(1, quint.second());
        assertTrue(quint.third());
        assertEquals(2.0, quint.fourth());
        assertEquals(3L, quint.fifth());
    }

    @Test
    @DisplayName("TupleUtils pair creates pair")
    void pairStatic() {
        TupleUtils.Pair<String, Integer> pair = TupleUtils.pair("key", 42);

        assertEquals("key", pair.first());
        assertEquals(42, pair.second());
    }

    @Test
    @DisplayName("TupleUtils triple creates triple")
    void tripleStatic() {
        TupleUtils.Triple<String, Integer, Boolean> triple = TupleUtils.triple("a", 1, true);

        assertEquals("a", triple.first());
        assertEquals(1, triple.second());
        assertTrue(triple.third());
    }

    @Test
    @DisplayName("TupleUtils zip combines two lists")
    void zipTwo() {
        List<String> listA = List.of("a", "b", "c");
        List<Integer> listB = List.of(1, 2, 3);

        List<TupleUtils.Pair<String, Integer>> zipped = TupleUtils.zip(listA, listB);

        assertEquals(3, zipped.size());
        assertEquals("a", zipped.get(0).first());
        assertEquals(1, zipped.get(0).second());
    }

    @Test
    @DisplayName("TupleUtils zip handles different lengths")
    void zipDifferentLengths() {
        List<String> listA = List.of("a", "b");
        List<Integer> listB = List.of(1, 2, 3, 4);

        List<TupleUtils.Pair<String, Integer>> zipped = TupleUtils.zip(listA, listB);

        assertEquals(2, zipped.size()); // Uses shorter length
    }

    @Test
    @DisplayName("TupleUtils unzip separates pairs")
    void unzip() {
        List<TupleUtils.Pair<String, Integer>> pairs = List.of(
            new TupleUtils.Pair<>("a", 1),
            new TupleUtils.Pair<>("b", 2)
        );

        TupleUtils.Pair<List<String>, List<Integer>> unzipped = TupleUtils.unzip(pairs);

        assertEquals(List.of("a", "b"), unzipped.first());
        assertEquals(List.of(1, 2), unzipped.second());
    }

    @Test
    @DisplayName("TupleUtils indexed adds indices")
    void indexed() {
        List<String> list = List.of("a", "b", "c");

        List<TupleUtils.Pair<String, Integer>> indexed = TupleUtils.indexed(list);

        assertEquals(3, indexed.size());
        assertEquals("a", indexed.get(0).first());
        assertEquals(0, indexed.get(0).second());
        assertEquals("c", indexed.get(2).first());
        assertEquals(2, indexed.get(2).second());
    }

    @Test
    @DisplayName("TupleUtils withPrevious pairs adjacent")
    void withPrevious() {
        List<Integer> list = List.of(1, 2, 3);

        List<TupleUtils.Pair<Integer, Integer>> pairs = TupleUtils.withPrevious(list);

        assertEquals(2, pairs.size());
        assertEquals(1, pairs.get(0).first());
        assertEquals(2, pairs.get(0).second());
    }

    @Test
    @DisplayName("TupleUtils withNext pairs adjacent")
    void withNext() {
        List<Integer> list = List.of(1, 2, 3);

        List<TupleUtils.Pair<Integer, Integer>> pairs = TupleUtils.withNext(list);

        assertEquals(2, pairs.size());
        assertEquals(1, pairs.get(0).first());
        assertEquals(2, pairs.get(0).second());
    }

    @Test
    @DisplayName("TupleUtils cartesianProduct produces all combinations")
    void cartesianProduct() {
        List<String> listA = List.of("a", "b");
        List<Integer> listB = List.of(1, 2);

        List<TupleUtils.Pair<String, Integer>> product = TupleUtils.cartesianProduct(listA, listB);

        assertEquals(4, product.size());
    }
}