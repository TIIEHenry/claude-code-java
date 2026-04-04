/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code collection utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Collection manipulation utilities.
 */
public final class CollectionUtils {
    private CollectionUtils() {}

    /**
     * Create a list from varargs.
     */
    @SafeVarargs
    public static <T> List<T> listOf(T... elements) {
        return List.of(elements);
    }

    /**
     * Create a mutable list from varargs.
     */
    @SafeVarargs
    public static <T> List<T> mutableListOf(T... elements) {
        return new ArrayList<>(List.of(elements));
    }

    /**
     * Create a set from varargs.
     */
    @SafeVarargs
    public static <T> Set<T> setOf(T... elements) {
        return Set.of(elements);
    }

    /**
     * Create a mutable set from varargs.
     */
    @SafeVarargs
    public static <T> Set<T> mutableSetOf(T... elements) {
        return new HashSet<>(Set.of(elements));
    }

    /**
     * Create a map from key-value pairs.
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) {
        Map<K, V> map = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Create a map entry.
     */
    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    /**
     * Check if collection is null or empty.
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Check if collection is not empty.
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    /**
     * Check if map is null or empty.
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Check if map is not empty.
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    /**
     * Get first element or null.
     */
    public static <T> T firstOrNull(List<T> list) {
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    /**
     * Get first element or default.
     */
    public static <T> T firstOrElse(List<T> list, T defaultValue) {
        return list != null && !list.isEmpty() ? list.get(0) : defaultValue;
    }

    /**
     * Get first element matching predicate.
     */
    public static <T> Optional<T> firstMatch(Collection<T> collection, Predicate<T> predicate) {
        if (collection == null) return Optional.empty();
        return collection.stream().filter(predicate).findFirst();
    }

    /**
     * Get last element or null.
     */
    public static <T> T lastOrNull(List<T> list) {
        return list != null && !list.isEmpty() ? list.get(list.size() - 1) : null;
    }

    /**
     * Get last element or default.
     */
    public static <T> T lastOrElse(List<T> list, T defaultValue) {
        return list != null && !list.isEmpty() ? list.get(list.size() - 1) : defaultValue;
    }

    /**
     * Partition list by predicate.
     */
    public static <T> Partitioned<T> partition(List<T> list, Predicate<T> predicate) {
        List<T> matching = new ArrayList<>();
        List<T> notMatching = new ArrayList<>();
        for (T item : list) {
            if (predicate.test(item)) {
                matching.add(item);
            } else {
                notMatching.add(item);
            }
        }
        return new Partitioned<>(matching, notMatching);
    }

    /**
     * Partitioned result record.
     */
    public record Partitioned<T>(List<T> matching, List<T> notMatching) {}

    /**
     * Chunk list into batches.
     */
    public static <T> List<List<T>> chunked(List<T> list, int chunkSize) {
        if (list == null || list.isEmpty()) return List.of();
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            chunks.add(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
        return chunks;
    }

    /**
     * Flatten nested lists.
     */
    @SafeVarargs
    public static <T> List<T> flatten(List<T>... lists) {
        return Arrays.stream(lists)
            .flatMap(List::stream)
            .toList();
    }

    /**
     * Flatten nested collections.
     */
    public static <T> List<T> flatten(Collection<Collection<T>> collections) {
        return collections.stream()
            .flatMap(Collection::stream)
            .toList();
    }

    /**
     * Distinct by key.
     */
    public static <T, K> List<T> distinctBy(Collection<T> collection, Function<T, K> keyExtractor) {
        Set<K> seen = new HashSet<>();
        return collection.stream()
            .filter(item -> seen.add(keyExtractor.apply(item)))
            .toList();
    }

    /**
     * Group by key.
     */
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> collection, Function<T, K> keyExtractor) {
        return collection.stream()
            .collect(Collectors.groupingBy(keyExtractor));
    }

    /**
     * Group by key and value.
     */
    public static <T, K, V> Map<K, List<V>> groupBy(Collection<T> collection,
            Function<T, K> keyExtractor, Function<T, V> valueExtractor) {
        return collection.stream()
            .collect(Collectors.groupingBy(keyExtractor,
                Collectors.mapping(valueExtractor, Collectors.toList())));
    }

    /**
     * Associate to map.
     */
    public static <T, K, V> Map<K, V> associate(Collection<T> collection,
            Function<T, K> keyExtractor, Function<T, V> valueExtractor) {
        return collection.stream()
            .collect(Collectors.toMap(keyExtractor, valueExtractor, (a, b) -> b, LinkedHashMap::new));
    }

    /**
     * Zip two lists.
     */
    public static <A, B> List<Pair<A, B>> zip(List<A> list1, List<B> list2) {
        int size = Math.min(list1.size(), list2.size());
        List<Pair<A, B>> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(new Pair<>(list1.get(i), list2.get(i)));
        }
        return result;
    }

    /**
     * Pair record.
     */
    public record Pair<A, B>(A first, B second) {}

    /**
     * Zip with index.
     */
    public static <T> List<Indexed<T>> zipWithIndex(List<T> list) {
        List<Indexed<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            result.add(new Indexed<>(i, list.get(i)));
        }
        return result;
    }

    /**
     * Indexed element record.
     */
    public record Indexed<T>(int index, T value) {}

    /**
     * Interleave two lists.
     */
    public static <T> List<T> interleave(List<T> list1, List<T> list2) {
        List<T> result = new ArrayList<>();
        int maxSize = Math.max(list1.size(), list2.size());
        for (int i = 0; i < maxSize; i++) {
            if (i < list1.size()) result.add(list1.get(i));
            if (i < list2.size()) result.add(list2.get(i));
        }
        return result;
    }

    /**
     * Take first N elements.
     */
    public static <T> List<T> take(List<T> list, int n) {
        if (list == null) return List.of();
        return list.stream().limit(n).toList();
    }

    /**
     * Take while predicate is true.
     */
    public static <T> List<T> takeWhile(List<T> list, Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        for (T item : list) {
            if (!predicate.test(item)) break;
            result.add(item);
        }
        return result;
    }

    /**
     * Drop first N elements.
     */
    public static <T> List<T> drop(List<T> list, int n) {
        if (list == null) return List.of();
        return list.stream().skip(n).toList();
    }

    /**
     * Drop while predicate is true.
     */
    public static <T> List<T> dropWhile(List<T> list, Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        boolean dropping = true;
        for (T item : list) {
            if (dropping && predicate.test(item)) continue;
            dropping = false;
            result.add(item);
        }
        return result;
    }

    /**
     * Sliding window.
     */
    public static <T> List<List<T>> windowed(List<T> list, int size, int step) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i <= list.size() - size; i += step) {
            result.add(list.subList(i, i + size));
        }
        return result;
    }

    /**
     * Sliding window with step 1.
     */
    public static <T> List<List<T>> windowed(List<T> list, int size) {
        return windowed(list, size, 1);
    }

    /**
     * Reverse list.
     */
    public static <T> List<T> reversed(List<T> list) {
        List<T> result = new ArrayList<>(list);
        Collections.reverse(result);
        return result;
    }

    /**
     * Sort list by key.
     */
    public static <T, K extends Comparable<K>> List<T> sortedBy(List<T> list, Function<T, K> keyExtractor) {
        return list.stream()
            .sorted(Comparator.comparing(keyExtractor))
            .toList();
    }

    /**
     * Sort list by key descending.
     */
    public static <T, K extends Comparable<K>> List<T> sortedByDescending(List<T> list, Function<T, K> keyExtractor) {
        return list.stream()
            .sorted(Comparator.comparing(keyExtractor).reversed())
            .toList();
    }

    /**
     * Sum integers.
     */
    public static int sumInts(Collection<Integer> collection) {
        return collection.stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Sum longs.
     */
    public static long sumLongs(Collection<Long> collection) {
        return collection.stream().mapToLong(Long::longValue).sum();
    }

    /**
     * Sum doubles.
     */
    public static double sumDoubles(Collection<Double> collection) {
        return collection.stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * Average of doubles.
     */
    public static OptionalDouble average(Collection<Double> collection) {
        return collection.stream().mapToDouble(Double::doubleValue).average();
    }

    /**
     * Min by comparator.
     */
    public static <T> Optional<T> minBy(Collection<T> collection, Comparator<T> comparator) {
        return collection.stream().min(comparator);
    }

    /**
     * Max by comparator.
     */
    public static <T> Optional<T> maxBy(Collection<T> collection, Comparator<T> comparator) {
        return collection.stream().max(comparator);
    }

    /**
     * Count occurrences.
     */
    public static <T> Map<T, Long> frequencies(Collection<T> collection) {
        return collection.stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    /**
     * Get or default.
     */
    public static <T> T getOrElse(List<T> list, int index, T defaultValue) {
        return index >= 0 && index < list.size() ? list.get(index) : defaultValue;
    }

    /**
     * Safe get from map.
     */
    public static <K, V> V getOrElse(Map<K, V> map, K key, V defaultValue) {
        return map != null ? map.getOrDefault(key, defaultValue) : defaultValue;
    }

    /**
     * Compute if absent.
     */
    public static <K, V> V computeIfAbsent(Map<K, V> map, K key, Supplier<V> supplier) {
        if (!map.containsKey(key)) {
            map.put(key, supplier.get());
        }
        return map.get(key);
    }

    /**
     * Join collection to string.
     */
    public static <T> String join(Collection<T> collection, String delimiter) {
        return collection.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(delimiter));
    }

    /**
     * Join collection to string with prefix/suffix.
     */
    public static <T> String join(Collection<T> collection, String delimiter, String prefix, String suffix) {
        return collection.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(delimiter, prefix, suffix));
    }

    /**
     * Map and filter nulls.
     */
    public static <T, R> List<R> mapNotNull(Collection<T> collection, Function<T, R> mapper) {
        return collection.stream()
            .map(mapper)
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Flat map and filter nulls.
     */
    public static <T, R> List<R> flatMapNotNull(Collection<T> collection, Function<T, Collection<R>> mapper) {
        return collection.stream()
            .map(mapper)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Rotate list by N positions.
     */
    public static <T> List<T> rotate(List<T> list, int positions) {
        if (list.isEmpty()) return list;
        int size = list.size();
        int offset = ((positions % size) + size) % size;
        List<T> result = new ArrayList<>();
        result.addAll(list.subList(offset, size));
        result.addAll(list.subList(0, offset));
        return result;
    }

    /**
     * Find duplicates.
     */
    public static <T> Set<T> findDuplicates(Collection<T> collection) {
        Set<T> seen = new HashSet<>();
        Set<T> duplicates = new HashSet<>();
        for (T item : collection) {
            if (!seen.add(item)) {
                duplicates.add(item);
            }
        }
        return duplicates;
    }
}