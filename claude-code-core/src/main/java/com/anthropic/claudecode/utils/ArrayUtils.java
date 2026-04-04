/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/array.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Array utilities.
 */
public final class ArrayUtils {
    private ArrayUtils() {}

    /**
     * Count elements matching predicate.
     */
    public static <T> long count(Collection<T> collection, Predicate<T> predicate) {
        return collection.stream().filter(predicate).count();
    }

    /**
     * Count elements.
     */
    public static <T> long count(Collection<T> collection) {
        return collection.size();
    }

    /**
     * Get first element or empty.
     */
    public static <T> Optional<T> first(List<T> list) {
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Get last element or empty.
     */
    public static <T> Optional<T> last(List<T> list) {
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
    }

    /**
     * Take first n elements.
     */
    public static <T> List<T> take(List<T> list, int n) {
        return list.stream().limit(n).collect(Collectors.toList());
    }

    /**
     * Drop first n elements.
     */
    public static <T> List<T> drop(List<T> list, int n) {
        return list.stream().skip(n).collect(Collectors.toList());
    }

    /**
     * Chunk list into batches.
     */
    public static <T> List<List<T>> chunk(List<T> list, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return chunks;
    }

    /**
     * Flatten nested list.
     */
    public static <T> List<T> flatten(List<List<T>> lists) {
        return lists.stream().flatMap(List::stream).collect(Collectors.toList());
    }

    /**
     * Zip two lists.
     */
    public static <A, B> List<Map.Entry<A, B>> zip(List<A> listA, List<B> listB) {
        int size = Math.min(listA.size(), listB.size());
        List<Map.Entry<A, B>> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(Map.entry(listA.get(i), listB.get(i)));
        }
        return result;
    }

    /**
     * Get unique elements.
     */
    public static <T> List<T> unique(List<T> list) {
        return list.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Partition by predicate.
     */
    public static <T> Map<Boolean, List<T>> partition(List<T> list, Predicate<T> predicate) {
        Map<Boolean, List<T>> result = new HashMap<>();
        result.put(true, new ArrayList<>());
        result.put(false, new ArrayList<>());
        for (T item : list) {
            result.get(predicate.test(item)).add(item);
        }
        return result;
    }

    /**
     * Group by key.
     */
    public static <T, K> Map<K, List<T>> groupBy(List<T> list, Function<T, K> keyExtractor) {
        return list.stream().collect(Collectors.groupingBy(keyExtractor));
    }
}