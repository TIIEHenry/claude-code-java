/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/array.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;

/**
 * Array utility functions.
 */
public final class ArrayUtil {
    private ArrayUtil() {}

    /**
     * Intersperse elements between array items.
     *
     * @param items The original list
     * @param separator Function to generate separator element given the index
     * @return New list with separators interspersed
     */
    public static <T> List<T> intersperse(List<T> items, Function<Integer, T> separator) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        List<T> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                result.add(separator.apply(i));
            }
            result.add(items.get(i));
        }
        return result;
    }

    /**
     * Intersperse a constant separator between array items.
     */
    public static <T> List<T> intersperseConstant(List<T> items, T separator) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        List<T> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                result.add(separator);
            }
            result.add(items.get(i));
        }
        return result;
    }

    /**
     * Alias for intersperseConstant for backward compatibility.
     */
    public static <T> List<T> intersperse(List<T> items, T separator) {
        return intersperseConstant(items, separator);
    }

    /**
     * Count elements matching a predicate.
     *
     * @param arr The array to count in
     * @param pred The predicate to test
     * @return Number of matching elements
     */
    public static <T> int count(List<T> arr, Predicate<T> pred) {
        int n = 0;
        for (T x : arr) {
            if (pred.test(x)) n++;
        }
        return n;
    }

    /**
     * Count elements matching a predicate in an iterable.
     */
    public static <T> int count(Iterable<T> iter, Predicate<T> pred) {
        int n = 0;
        for (T x : iter) {
            if (pred.test(x)) n++;
        }
        return n;
    }

    /**
     * Return unique elements preserving order.
     */
    public static <T> List<T> uniq(Iterable<T> items) {
        Set<T> seen = new HashSet<>();
        List<T> result = new ArrayList<>();
        for (T item : items) {
            if (seen.add(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Return unique elements by key function.
     */
    public static <T, K> List<T> uniqBy(Iterable<T> items, Function<T, K> keyFn) {
        Set<K> seen = new HashSet<>();
        List<T> result = new ArrayList<>();
        for (T item : items) {
            K key = keyFn.apply(item);
            if (seen.add(key)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Chunk an array into groups of size n.
     */
    public static <T> List<List<T>> chunk(List<T> items, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i += size) {
            result.add(new ArrayList<>(items.subList(i, Math.min(i + size, items.size()))));
        }
        return result;
    }

    /**
     * Flatten a list of lists.
     */
    public static <T> List<T> flatten(List<List<T>> lists) {
        List<T> result = new ArrayList<>();
        for (List<T> list : lists) {
            result.addAll(list);
        }
        return result;
    }

    /**
     * Zip two lists together.
     */
    public static <A, B> List<Map.Entry<A, B>> zip(List<A> as, List<B> bs) {
        List<Map.Entry<A, B>> result = new ArrayList<>();
        int len = Math.min(as.size(), bs.size());
        for (int i = 0; i < len; i++) {
            result.add(new AbstractMap.SimpleEntry<>(as.get(i), bs.get(i)));
        }
        return result;
    }

    /**
     * Take first n elements.
     */
    public static <T> List<T> take(List<T> items, int n) {
        return new ArrayList<>(items.subList(0, Math.min(n, items.size())));
    }

    /**
     * Drop first n elements.
     */
    public static <T> List<T> drop(List<T> items, int n) {
        return new ArrayList<>(items.subList(Math.min(n, items.size()), items.size()));
    }

    /**
     * Get last element or null if empty.
     */
    public static <T> T last(List<T> items) {
        return items.isEmpty() ? null : items.get(items.size() - 1);
    }

    /**
     * Get first element or null if empty.
     */
    public static <T> T first(List<T> items) {
        return items.isEmpty() ? null : items.get(0);
    }

    /**
     * Partition a list by a predicate.
     */
    public static <T> Map.Entry<List<T>, List<T>> partition(List<T> items, Predicate<T> pred) {
        List<T> matching = new ArrayList<>();
        List<T> notMatching = new ArrayList<>();
        for (T item : items) {
            if (pred.test(item)) {
                matching.add(item);
            } else {
                notMatching.add(item);
            }
        }
        return new AbstractMap.SimpleEntry<>(matching, notMatching);
    }

    /**
     * Group elements by key function.
     */
    public static <T, K> Map<K, List<T>> groupBy(List<T> items, Function<T, K> keyFn) {
        Map<K, List<T>> result = new HashMap<>();
        for (T item : items) {
            K key = keyFn.apply(item);
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        return result;
    }

    /**
     * Find index of element matching predicate.
     */
    public static <T> int findIndex(List<T> items, Predicate<T> pred) {
        for (int i = 0; i < items.size(); i++) {
            if (pred.test(items.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find last index of element matching predicate.
     */
    public static <T> int findLastIndex(List<T> items, Predicate<T> pred) {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (pred.test(items.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Reverse a list.
     */
    public static <T> List<T> reverse(List<T> items) {
        List<T> result = new ArrayList<>(items);
        Collections.reverse(result);
        return result;
    }
}