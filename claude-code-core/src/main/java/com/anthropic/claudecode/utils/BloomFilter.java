/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code bloom filter
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;

/**
 * Bloom filter - probabilistic membership test.
 */
public final class BloomFilter<T> {
    private final BitSet bits;
    private final int size;
    private final int hashCount;
    private final Function<T, Integer>[] hashFunctions;
    private int addedCount = 0;

    @SuppressWarnings("unchecked")
    public BloomFilter(int expectedSize, double falsePositiveRate) {
        // Calculate optimal size and hash count
        this.size = optimalSize(expectedSize, falsePositiveRate);
        this.hashCount = optimalHashCount(expectedSize, size);
        this.bits = new BitSet(size);
        this.hashFunctions = new Function[hashCount];

        // Create hash functions
        for (int i = 0; i < hashCount; i++) {
            final int seed = i;
            hashFunctions[i] = item -> {
                int hash = item.hashCode();
                hash = hash ^ (seed * 0x9e3779b9);
                hash = (hash ^ (hash >>> 16)) * 0x85ebca6b;
                hash = (hash ^ (hash >>> 13)) * 0xc2b2ae35;
                hash = hash ^ (hash >>> 16);
                return Math.abs(hash % size);
            };
        }
    }

    public BloomFilter(int size, int hashCount) {
        this.size = size;
        this.hashCount = hashCount;
        this.bits = new BitSet(size);
        this.hashFunctions = new Function[hashCount];

        for (int i = 0; i < hashCount; i++) {
            final int seed = i;
            hashFunctions[i] = item -> {
                int hash = item.hashCode();
                hash = hash ^ (seed * 0x9e3779b9);
                hash = (hash ^ (hash >>> 16)) * 0x85ebca6b;
                hash = (hash ^ (hash >>> 13)) * 0xc2b2ae35;
                hash = hash ^ (hash >>> 16);
                return Math.abs(hash % size);
            };
        }
    }

    /**
     * Add item to filter.
     */
    public void add(T item) {
        if (item == null) return;
        for (Function<T, Integer> hashFn : hashFunctions) {
            bits.set(hashFn.apply(item));
        }
        addedCount++;
    }

    /**
     * Add all items.
     */
    public void addAll(Collection<T> items) {
        items.forEach(this::add);
    }

    /**
     * Check if item might be present.
     */
    public boolean mightContain(T item) {
        if (item == null) return false;
        for (Function<T, Integer> hashFn : hashFunctions) {
            if (!bits.get(hashFn.apply(item))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if item is definitely not present.
     */
    public boolean definitelyNotPresent(T item) {
        return !mightContain(item);
    }

    /**
     * Clear filter.
     */
    public void clear() {
        bits.clear();
        addedCount = 0;
    }

    /**
     * Get size in bits.
     */
    public int size() {
        return size;
    }

    /**
     * Get hash count.
     */
    public int hashCount() {
        return hashCount;
    }

    /**
     * Get added count.
     */
    public int addedCount() {
        return addedCount;
    }

    /**
     * Get bit count set.
     */
    public int bitCount() {
        return bits.cardinality();
    }

    /**
     * Calculate expected false positive rate.
     */
    public double expectedFalsePositiveRate() {
        if (addedCount == 0) return 0;
        double p = Math.pow(1.0 - Math.exp(-hashCount * addedCount / (double) size), hashCount);
        return p;
    }

    /**
     * Union with another bloom filter.
     */
    public void union(BloomFilter<T> other) {
        if (this.size != other.size || this.hashCount != other.hashCount) {
            throw new IllegalArgumentException("Incompatible bloom filters");
        }
        this.bits.or(other.bits);
    }

    /**
     * Intersect with another bloom filter.
     */
    public void intersect(BloomFilter<T> other) {
        if (this.size != other.size || this.hashCount != other.hashCount) {
            throw new IllegalArgumentException("Incompatible bloom filters");
        }
        this.bits.and(other.bits);
    }

    /**
     * Calculate optimal size.
     */
    private static int optimalSize(int n, double p) {
        return (int) Math.ceil(-n * Math.log(p) / Math.pow(Math.log(2), 2));
    }

    /**
     * Calculate optimal hash count.
     */
    private static int optimalHashCount(int n, int m) {
        return (int) Math.ceil((m / n) * Math.log(2));
    }

    /**
     * Bloom filter utilities.
     */
    public static final class BloomFilterUtils {
        private BloomFilterUtils() {}

        /**
         * Create for expected size with 1% false positive rate.
         */
        public static <T> BloomFilter<T> create(int expectedSize) {
            return new BloomFilter<>(expectedSize, 0.01);
        }

        /**
         * Create with custom false positive rate.
         */
        public static <T> BloomFilter<T> create(int expectedSize, double falsePositiveRate) {
            return new BloomFilter<>(expectedSize, falsePositiveRate);
        }

        /**
         * Create from collection.
         */
        public static <T> BloomFilter<T> fromCollection(Collection<T> items, double falsePositiveRate) {
            BloomFilter<T> filter = new BloomFilter<>(items.size(), falsePositiveRate);
            filter.addAll(items);
            return filter;
        }
    }
}