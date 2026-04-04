/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code comparator utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;

/**
 * Comparator utilities.
 */
public final class ComparatorUtils {
    private ComparatorUtils() {}

    /**
     * Natural order comparator.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> Comparator<T> naturalOrder() {
        return (a, b) -> a.compareTo(b);
    }

    /**
     * Reverse order comparator.
     */
    public static <T extends Comparable<T>> Comparator<T> reverseOrder() {
        return Comparator.reverseOrder();
    }

    /**
     * Null-safe comparator (nulls first).
     */
    public static <T> Comparator<T> nullsFirst(Comparator<T> comparator) {
        return Comparator.nullsFirst(comparator);
    }

    /**
     * Null-safe comparator (nulls last).
     */
    public static <T> Comparator<T> nullsLast(Comparator<T> comparator) {
        return Comparator.nullsLast(comparator);
    }

    /**
     * Compare by extracted key.
     */
    public static <T, U extends Comparable<U>> Comparator<T> comparing(
            Function<T, U> keyExtractor) {
        return Comparator.comparing(keyExtractor);
    }

    /**
     * Compare by extracted key with custom comparator.
     */
    public static <T, U> Comparator<T> comparing(
            Function<T, U> keyExtractor, Comparator<U> keyComparator) {
        return Comparator.comparing(keyExtractor, keyComparator);
    }

    /**
     * Compare by int key.
     */
    public static <T> Comparator<T> comparingInt(ToIntFunction<T> keyExtractor) {
        return Comparator.comparingInt(keyExtractor);
    }

    /**
     * Compare by long key.
     */
    public static <T> Comparator<T> comparingLong(ToLongFunction<T> keyExtractor) {
        return Comparator.comparingLong(keyExtractor);
    }

    /**
     * Compare by double key.
     */
    public static <T> Comparator<T> comparingDouble(ToDoubleFunction<T> keyExtractor) {
        return Comparator.comparingDouble(keyExtractor);
    }

    /**
     * Chain comparators.
     */
    @SafeVarargs
    public static <T> Comparator<T> chain(Comparator<T>... comparators) {
        return (a, b) -> {
            for (Comparator<T> comparator : comparators) {
                int result = comparator.compare(a, b);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        };
    }

    /**
     * Chain comparators from list.
     */
    public static <T> Comparator<T> chain(List<Comparator<T>> comparators) {
        return (a, b) -> {
            for (Comparator<T> comparator : comparators) {
                int result = comparator.compare(a, b);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        };
    }

    /**
     * Compare by multiple keys.
     */
    @SafeVarargs
    public static <T, U extends Comparable<U>> Comparator<T> comparingBy(
            Function<T, U>... keyExtractors) {
        return (a, b) -> {
            for (Function<T, U> extractor : keyExtractors) {
                int result = extractor.apply(a).compareTo(extractor.apply(b));
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        };
    }

    /**
     * Reverse a comparator.
     */
    public static <T> Comparator<T> reversed(Comparator<T> comparator) {
        return comparator.reversed();
    }

    /**
     * Create a comparator from a predicate (true values first).
     */
    public static <T> Comparator<T> fromPredicate(Predicate<T> predicate) {
        return (a, b) -> {
            boolean aMatch = predicate.test(a);
            boolean bMatch = predicate.test(b);
            return Boolean.compare(bMatch, aMatch);
        };
    }

    /**
     * Create a comparator that always returns 0 (all elements equal).
     */
    public static <T> Comparator<T> allEqual() {
        return (a, b) -> 0;
    }

    /**
     * Create a comparator based on order of elements in a list.
     */
    public static <T> Comparator<T> byOrder(List<T> order) {
        Map<T, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < order.size(); i++) {
            indexMap.put(order.get(i), i);
        }
        return (a, b) -> {
            Integer idxA = indexMap.getOrDefault(a, Integer.MAX_VALUE);
            Integer idxB = indexMap.getOrDefault(b, Integer.MAX_VALUE);
            return idxA.compareTo(idxB);
        };
    }

    /**
     * Create a comparator based on order of elements.
     */
    @SafeVarargs
    public static <T> Comparator<T> byOrder(T... elements) {
        return byOrder(Arrays.asList(elements));
    }

    /**
     * Lexicographic comparator for lists.
     */
    public static <T> Comparator<List<T>> lexicographic(Comparator<T> elementComparator) {
        return (a, b) -> {
            int minSize = Math.min(a.size(), b.size());
            for (int i = 0; i < minSize; i++) {
                int result = elementComparator.compare(a.get(i), b.get(i));
                if (result != 0) {
                    return result;
                }
            }
            return Integer.compare(a.size(), b.size());
        };
    }

    /**
     * Lexicographic comparator for arrays.
     */
    public static <T> Comparator<T[]> arrayLexicographic(Comparator<T> elementComparator) {
        return (a, b) -> {
            int minLen = Math.min(a.length, b.length);
            for (int i = 0; i < minLen; i++) {
                int result = elementComparator.compare(a[i], b[i]);
                if (result != 0) {
                    return result;
                }
            }
            return Integer.compare(a.length, b.length);
        };
    }

    /**
     * String length comparator.
     */
    public static Comparator<String> byLength() {
        return Comparator.comparingInt(String::length);
    }

    /**
     * String length then natural order.
     */
    public static Comparator<String> byLengthThenNatural() {
        return chain(byLength(), naturalOrder());
    }

    /**
     * String length reversed.
     */
    public static Comparator<String> byLengthDescending() {
        return byLength().reversed();
    }

    /**
     * Compare strings ignoring case.
     */
    public static Comparator<String> ignoringCase() {
        return String.CASE_INSENSITIVE_ORDER;
    }

    /**
     * Compare by string representation.
     */
    public static <T> Comparator<T> byToString() {
        return Comparator.comparing(Object::toString);
    }

    /**
     * Compare by string representation ignoring case.
     */
    public static <T> Comparator<T> byToStringIgnoreCase() {
        return Comparator.comparing(o -> o.toString().toLowerCase());
    }

    /**
     * Compare optional values (empty first).
     */
    public static <T extends Comparable<T>> Comparator<Optional<T>> optionalFirst() {
        return (a, b) -> {
            if (a.isEmpty() && b.isEmpty()) return 0;
            if (a.isEmpty()) return -1;
            if (b.isEmpty()) return 1;
            return a.get().compareTo(b.get());
        };
    }

    /**
     * Compare optional values (empty last).
     */
    public static <T extends Comparable<T>> Comparator<Optional<T>> optionalLast() {
        return (a, b) -> {
            if (a.isEmpty() && b.isEmpty()) return 0;
            if (a.isEmpty()) return 1;
            if (b.isEmpty()) return -1;
            return a.get().compareTo(b.get());
        };
    }

    /**
     * Compare by boolean value (false first).
     */
    public static <T> Comparator<T> comparingBoolean(ToBooleanFunction<T> keyExtractor) {
        return (a, b) -> {
            boolean boolA = keyExtractor.applyAsBoolean(a);
            boolean boolB = keyExtractor.applyAsBoolean(b);
            return Boolean.compare(boolA, boolB);
        };
    }

    /**
     * Compare by boolean value (true first).
     */
    public static <T> Comparator<T> comparingBooleanDesc(ToBooleanFunction<T> keyExtractor) {
        return comparingBoolean(keyExtractor).reversed();
    }

    /**
     * Functional interface for boolean extraction.
     */
    @FunctionalInterface
    public interface ToBooleanFunction<T> {
        boolean applyAsBoolean(T value);
    }

    /**
     * Create a comparator with custom less-than logic.
     */
    public static <T> Comparator<T> fromPredicate(BiPredicate<T, T> isLessThan) {
        return (a, b) -> {
            if (isLessThan.test(a, b)) return -1;
            if (isLessThan.test(b, a)) return 1;
            return 0;
        };
    }

    /**
     * Min comparator (find minimum).
     */
    public static <T> Optional<T> min(Collection<T> collection, Comparator<T> comparator) {
        return collection.stream().min(comparator);
    }

    /**
     * Max comparator (find maximum).
     */
    public static <T> Optional<T> max(Collection<T> collection, Comparator<T> comparator) {
        return collection.stream().max(comparator);
    }

    /**
     * Find median element.
     */
    public static <T> Optional<T> median(List<T> list, Comparator<T> comparator) {
        if (list == null || list.isEmpty()) return Optional.empty();
        List<T> sorted = new ArrayList<>(list);
        sorted.sort(comparator);
        return Optional.of(sorted.get(sorted.size() / 2));
    }

    /**
     * Percentile element.
     */
    public static <T> Optional<T> percentile(List<T> list, double percentile, Comparator<T> comparator) {
        if (list == null || list.isEmpty()) return Optional.empty();
        List<T> sorted = new ArrayList<>(list);
        sorted.sort(comparator);
        int index = (int) (percentile * (sorted.size() - 1));
        return Optional.of(sorted.get(index));
    }

    /**
     * Sort and return new list.
     */
    public static <T> List<T> sorted(Collection<T> collection, Comparator<T> comparator) {
        List<T> result = new ArrayList<>(collection);
        result.sort(comparator);
        return result;
    }

    /**
     * Sort distinct and return new list.
     */
    public static <T> List<T> sortedDistinct(Collection<T> collection, Comparator<T> comparator) {
        return collection.stream()
            .distinct()
            .sorted(comparator)
            .toList();
    }

    /**
     * Top N elements.
     */
    public static <T> List<T> topN(Collection<T> collection, int n, Comparator<T> comparator) {
        return collection.stream()
            .sorted(comparator.reversed())
            .limit(n)
            .toList();
    }

    /**
     * Bottom N elements.
     */
    public static <T> List<T> bottomN(Collection<T> collection, int n, Comparator<T> comparator) {
        return collection.stream()
            .sorted(comparator)
            .limit(n)
            .toList();
    }

    /**
     * Is sorted check.
     */
    public static <T> boolean isSorted(List<T> list, Comparator<T> comparator) {
        for (int i = 1; i < list.size(); i++) {
            if (comparator.compare(list.get(i - 1), list.get(i)) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Binary search returning insertion point.
     */
    public static <T> int binarySearchInsertionPoint(List<T> sortedList, T element, Comparator<T> comparator) {
        int low = 0;
        int high = sortedList.size();

        while (low < high) {
            int mid = (low + high) >>> 1;
            int cmp = comparator.compare(sortedList.get(mid), element);
            if (cmp < 0) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }
}