/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BloomFilter.
 */
class BloomFilterTest {

    @Test
    @DisplayName("BloomFilter creates with size and hash count")
    void createsWithSizeAndHashCount() {
        BloomFilter<String> filter = new BloomFilter<>(1000, 3);

        assertEquals(1000, filter.size());
        assertEquals(3, filter.hashCount());
        assertEquals(0, filter.addedCount());
    }

    @Test
    @DisplayName("BloomFilter creates with expected size and rate")
    void createsWithExpectedSizeAndRate() {
        BloomFilter<String> filter = new BloomFilter<>(100, 0.01);

        assertTrue(filter.size() > 0);
        assertTrue(filter.hashCount() > 0);
    }

    @Test
    @DisplayName("BloomFilter add adds item")
    void addWorks() {
        BloomFilter<String> filter = new BloomFilter<>(1000, 3);

        filter.add("test");

        assertEquals(1, filter.addedCount());
        assertTrue(filter.bitCount() > 0);
    }

    @Test
    @DisplayName("BloomFilter add ignores null")
    void addNull() {
        BloomFilter<String> filter = new BloomFilter<>(1000, 3);

        filter.add(null);

        assertEquals(0, filter.addedCount());
    }

    @Test
    @DisplayName("BloomFilter addAll adds all items")
    void addAllWorks() {
        BloomFilter<String> filter = new BloomFilter<>(1000, 3);

        filter.addAll(List.of("a", "b", "c"));

        assertEquals(3, filter.addedCount());
    }

    @Test
    @DisplayName("BloomFilter mightContain returns true for added")
    void mightContainAdded() {
        BloomFilter<String> filter = new BloomFilter<>(1000, 3);

        filter.add("test");

        assertTrue(filter.mightContain("test"));
    }

    @Test
    @DisplayName("BloomFilter mightContain returns false for null")
    void mightContainNull() {
        BloomFilter<String> filter = new BloomFilter<>(1000, 3);

        assertFalse(filter.mightContain(null));
    }

    @Test
    @DisplayName("BloomFilter definitelyNotPresent returns true for missing")
    void definitelyNotPresent() {
        BloomFilter<String> filter = new BloomFilter<>(1000, 3);

        filter.add("test");

        assertTrue(filter.definitelyNotPresent("other"));
    }

    @Test
    @DisplayName("BloomFilter clear resets filter")
    void clearWorks() {
        BloomFilter<String> filter = new BloomFilter<>(1000, 3);

        filter.add("test");
        filter.clear();

        assertEquals(0, filter.addedCount());
        assertEquals(0, filter.bitCount());
    }

    @Test
    @DisplayName("BloomFilter bitCount returns bits set")
    void bitCountWorks() {
        BloomFilter<String> filter = new BloomFilter<>(1000, 3);

        filter.add("test");

        assertTrue(filter.bitCount() > 0);
        assertTrue(filter.bitCount() <= 3);
    }

    @Test
    @DisplayName("BloomFilter expectedFalsePositiveRate returns rate")
    void expectedFalsePositiveRate() {
        BloomFilter<String> filter = new BloomFilter<>(1000, 3);

        assertEquals(0.0, filter.expectedFalsePositiveRate());

        filter.add("test");

        assertTrue(filter.expectedFalsePositiveRate() >= 0);
    }

    @Test
    @DisplayName("BloomFilter union combines filters")
    void unionWorks() {
        BloomFilter<String> filter1 = new BloomFilter<>(1000, 3);
        BloomFilter<String> filter2 = new BloomFilter<>(1000, 3);

        filter1.add("a");
        filter2.add("b");

        filter1.union(filter2);

        assertTrue(filter1.mightContain("a"));
        assertTrue(filter1.mightContain("b"));
    }

    @Test
    @DisplayName("BloomFilter union fails on incompatible")
    void unionIncompatible() {
        BloomFilter<String> filter1 = new BloomFilter<>(1000, 3);
        BloomFilter<String> filter2 = new BloomFilter<>(500, 3);

        assertThrows(IllegalArgumentException.class, () -> filter1.union(filter2));
    }

    @Test
    @DisplayName("BloomFilter intersect combines filters")
    void intersectWorks() {
        BloomFilter<String> filter1 = new BloomFilter<>(1000, 3);
        BloomFilter<String> filter2 = new BloomFilter<>(1000, 3);

        filter1.add("a");
        filter1.add("b");
        filter2.add("b");
        filter2.add("c");

        filter1.intersect(filter2);

        assertTrue(filter1.mightContain("b"));
    }

    @Test
    @DisplayName("BloomFilter intersect fails on incompatible")
    void intersectIncompatible() {
        BloomFilter<String> filter1 = new BloomFilter<>(1000, 3);
        BloomFilter<String> filter2 = new BloomFilter<>(1000, 5);

        assertThrows(IllegalArgumentException.class, () -> filter1.intersect(filter2));
    }

    @Test
    @DisplayName("BloomFilter BloomFilterUtils create works")
    void bloomFilterUtilsCreate() {
        BloomFilter<String> filter = BloomFilter.BloomFilterUtils.create(100);

        assertTrue(filter.size() > 0);
    }

    @Test
    @DisplayName("BloomFilter BloomFilterUtils create with rate works")
    void bloomFilterUtilsCreateWithRate() {
        BloomFilter<String> filter = BloomFilter.BloomFilterUtils.create(100, 0.05);

        assertTrue(filter.size() > 0);
    }

    @Test
    @DisplayName("BloomFilter BloomFilterUtils fromCollection works")
    void bloomFilterUtilsFromCollection() {
        BloomFilter<String> filter = BloomFilter.BloomFilterUtils.fromCollection(
            List.of("a", "b", "c"), 0.01);

        assertEquals(3, filter.addedCount());
        assertTrue(filter.mightContain("a"));
        assertTrue(filter.mightContain("b"));
        assertTrue(filter.mightContain("c"));
    }

    @Test
    @DisplayName("BloomFilter handles many items")
    void manyItems() {
        BloomFilter<Integer> filter = new BloomFilter<>(1000, 5);

        for (int i = 0; i < 100; i++) {
            filter.add(i);
        }

        assertEquals(100, filter.addedCount());

        // Check some items
        assertTrue(filter.mightContain(50));
        assertTrue(filter.mightContain(0));
        assertTrue(filter.mightContain(99));
    }

    @Test
    @DisplayName("BloomFilter has false positives but no false negatives")
    void noFalseNegatives() {
        BloomFilter<String> filter = new BloomFilter<>(1000, 3);

        filter.add("test");

        // Should never have false negatives
        assertTrue(filter.mightContain("test"));
    }
}