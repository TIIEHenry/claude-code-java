/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BiMap.
 */
class BiMapTest {

    @Test
    @DisplayName("BiMap creates empty")
    void createsEmpty() {
        BiMap<String, Integer> map = new BiMap<>();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    @DisplayName("BiMap put adds bidirectional mapping")
    void putAddsMapping() {
        BiMap<String, Integer> map = new BiMap<>();

        map.put("one", 1);

        assertEquals(1, map.get("one"));
        assertEquals("one", map.getKey(1));
    }

    @Test
    @DisplayName("BiMap put removes old mapping")
    void putRemovesOldMapping() {
        BiMap<String, Integer> map = new BiMap<>();

        map.put("one", 1);
        map.put("one", 2);

        assertEquals(2, map.get("one"));
        assertNull(map.getKey(1)); // Old value removed
        assertEquals("one", map.getKey(2));
    }

    @Test
    @DisplayName("BiMap put removes old key for value")
    void putRemovesOldKey() {
        BiMap<String, Integer> map = new BiMap<>();

        map.put("one", 1);
        map.put("two", 1); // Value 1 now maps to "two"

        assertNull(map.get("one")); // Old key removed
        assertEquals("two", map.getKey(1));
    }

    @Test
    @DisplayName("BiMap putIfAbsent keeps existing")
    void putIfAbsentKeepsExisting() {
        BiMap<String, Integer> map = new BiMap<>();

        map.put("one", 1);
        Integer result = map.putIfAbsent("one", 2);

        assertEquals(1, result);
        assertEquals(1, map.get("one"));
    }

    @Test
    @DisplayName("BiMap putIfAbsent adds when absent")
    void putIfAbsentAdds() {
        BiMap<String, Integer> map = new BiMap<>();

        Integer result = map.putIfAbsent("one", 1);

        assertNull(result);
        assertEquals(1, map.get("one"));
    }

    @Test
    @DisplayName("BiMap forcePut allows duplicate values")
    void forcePutAllowsDuplicate() {
        BiMap<String, Integer> map = new BiMap<>();

        map.forcePut("one", 1);
        map.forcePut("one", 2);

        assertEquals(2, map.get("one"));
    }

    @Test
    @DisplayName("BiMap get returns value")
    void getWorks() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);

        assertEquals(1, map.get("one"));
        assertNull(map.get("missing"));
    }

    @Test
    @DisplayName("BiMap getKey returns key")
    void getKeyWorks() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);

        assertEquals("one", map.getKey(1));
        assertNull(map.getKey(2));
    }

    @Test
    @DisplayName("BiMap getOrDefault returns default")
    void getOrDefaultWorks() {
        BiMap<String, Integer> map = new BiMap<>();

        assertEquals(0, map.getOrDefault("missing", 0));
    }

    @Test
    @DisplayName("BiMap getKeyOrDefault returns default")
    void getKeyOrDefaultWorks() {
        BiMap<String, Integer> map = new BiMap<>();

        assertEquals("default", map.getKeyOrDefault(0, "default"));
    }

    @Test
    @DisplayName("BiMap containsKey checks key")
    void containsKeyWorks() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);

        assertTrue(map.containsKey("one"));
        assertFalse(map.containsKey("missing"));
    }

    @Test
    @DisplayName("BiMap containsValue checks value")
    void containsValueWorks() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);

        assertTrue(map.containsValue(1));
        assertFalse(map.containsValue(2));
    }

    @Test
    @DisplayName("BiMap remove removes by key")
    void removeWorks() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);

        Integer removed = map.remove("one");

        assertEquals(1, removed);
        assertTrue(map.isEmpty());
        assertNull(map.getKey(1));
    }

    @Test
    @DisplayName("BiMap removeValue removes by value")
    void removeValueWorks() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);

        String removed = map.removeValue(1);

        assertEquals("one", removed);
        assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("BiMap keys returns all keys")
    void keysWorks() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);
        map.put("two", 2);

        Set<String> keys = map.keys();

        assertEquals(2, keys.size());
        assertTrue(keys.contains("one"));
        assertTrue(keys.contains("two"));
    }

    @Test
    @DisplayName("BiMap values returns all values")
    void valuesWorks() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);
        map.put("two", 2);

        Set<Integer> values = map.values();

        assertEquals(2, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
    }

    @Test
    @DisplayName("BiMap entries returns all entries")
    void entriesWorks() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);

        Set<Map.Entry<String, Integer>> entries = map.entries();

        assertEquals(1, entries.size());
    }

    @Test
    @DisplayName("BiMap inverse returns inverse view")
    void inverseWorks() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);

        BiMap<Integer, String> inverse = map.inverse();

        assertEquals("one", inverse.get(1));
        assertEquals(1, inverse.getKey("one"));
    }

    @Test
    @DisplayName("BiMap forEach iterates")
    void forEachWorks() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);
        map.put("two", 2);

        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> sb.append(k).append(":").append(v));

        assertTrue(sb.toString().contains("one:1"));
        assertTrue(sb.toString().contains("two:2"));
    }

    @Test
    @DisplayName("BiMap toMap returns copy")
    void toMapWorks() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);

        Map<String, Integer> copy = map.toMap();

        assertEquals(1, copy.size());
        assertEquals(1, copy.get("one"));
    }

    @Test
    @DisplayName("BiMap clear removes all")
    void clearWorks() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.clear();

        assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("BiMap toString shows contents")
    void toStringWorks() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);

        String str = map.toString();

        assertTrue(str.contains("one"));
        assertTrue(str.contains("1"));
    }

    @Test
    @DisplayName("BiMap BiMapUtils fromMap creates from map")
    void biMapUtilsFromMap() {
        Map<String, Integer> source = Map.of("one", 1, "two", 2);
        BiMap<String, Integer> map = BiMap.BiMapUtils.fromMap(source);

        assertEquals(2, map.size());
        assertEquals(1, map.get("one"));
    }

    @Test
    @DisplayName("BiMap BiMapUtils of creates from entries")
    void biMapUtilsOf() {
        BiMap<String, Integer> map = BiMap.BiMapUtils.of("one", 1);

        assertEquals(1, map.size());
        assertEquals(1, map.get("one"));
    }

    @Test
    @DisplayName("BiMap BiMapUtils of with two entries")
    void biMapUtilsOfTwo() {
        BiMap<String, Integer> map = BiMap.BiMapUtils.of("one", 1, "two", 2);

        assertEquals(2, map.size());
    }

    @Test
    @DisplayName("BiMap BiMapUtils of with three entries")
    void biMapUtilsOfThree() {
        BiMap<String, Integer> map = BiMap.BiMapUtils.of("one", 1, "two", 2, "three", 3);

        assertEquals(3, map.size());
    }
}