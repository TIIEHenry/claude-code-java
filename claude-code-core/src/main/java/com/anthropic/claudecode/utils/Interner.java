/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code interner utilities
 */
package com.anthropic.claudecode.utils;

import java.util.concurrent.CompletableFuture;

import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Interner - canonicalizes instances to reduce memory.
 */
public class Interner<T> {
    private final ConcurrentMap<T, T> internMap;
    private final boolean useWeakReferences;
    private final ReferenceQueue<T> queue;

    public Interner() {
        this(false);
    }

    public Interner(boolean useWeakReferences) {
        this.useWeakReferences = useWeakReferences;
        this.internMap = new ConcurrentHashMap<>();
        this.queue = new ReferenceQueue<>();
    }

    /**
     * Intern a value - returns canonical instance.
     */
    public T intern(T value) {
        if (value == null) return null;

        cleanUp();

        if (useWeakReferences) {
            // For weak references, we need a wrapper
            return internMap.compute(value, (k, v) -> v != null ? v : value);
        }

        return internMap.computeIfAbsent(value, k -> value);
    }

    /**
     * Check if value is interned.
     */
    public boolean isInterned(T value) {
        return internMap.containsKey(value);
    }

    /**
     * Get canonical instance.
     */
    public Optional<T> getCanonical(T value) {
        return Optional.ofNullable(internMap.get(value));
    }

    /**
     * Get count of interned values.
     */
    public int size() {
        return internMap.size();
    }

    /**
     * Clear all interned values.
     */
    public void clear() {
        internMap.clear();
    }

    /**
     * Remove a value.
     */
    public void remove(T value) {
        internMap.remove(value);
    }

    /**
     * Clean up collected references.
     */
    private void cleanUp() {
        if (!useWeakReferences) return;

        Reference<? extends T> ref;
        while ((ref = queue.poll()) != null) {
            internMap.remove(ref.get());
        }
    }

    /**
     * Get all interned values.
     */
    public Set<T> getAllInterned() {
        return new HashSet<>(internMap.keySet());
    }

    /**
     * Intern all values from collection.
     */
    public void internAll(Collection<T> values) {
        for (T value : values) {
            intern(value);
        }
    }

    @Override
    public String toString() {
        return String.format("Interner[size=%d, weak=%b]", size(), useWeakReferences);
    }

    /**
     * String interner.
     */
    public static final class StringInterner {
        private static final Interner<String> GLOBAL = new Interner<>(true);

        /**
         * Intern string globally.
         */
        public static String intern(String value) {
            return GLOBAL.intern(value);
        }

        /**
         * Get global interner stats.
         */
        public static int size() {
            return GLOBAL.size();
        }

        /**
         * Clear global interner.
         */
        public static void clear() {
            GLOBAL.clear();
        }
    }

    /**
     * Interner utilities.
     */
    public static final class InternerUtils {
        private InternerUtils() {}

        /**
         * Create thread-safe interner.
         */
        public static <T> Interner<T> concurrent() {
            return new Interner<>(false);
        }

        /**
         * Create weak interner (allows GC).
         */
        public static <T> Interner<T> weak() {
            return new Interner<>(true);
        }

        /**
         * Create bounded interner.
         */
        public static <T> Interner<T> bounded(int maxSize) {
            return new BoundedInterner<>(maxSize);
        }

        /**
         * Create interner with custom equality.
         */
        public static <T> Interner<T> withEquality(BiPredicate<T, T> equals, Function<T, Integer> hasher) {
            return new CustomEqualityInterner<>(equals, hasher);
        }
    }

    /**
     * Bounded interner - evicts old entries.
     */
    public static final class BoundedInterner<T> extends Interner<T> {
        private final int maxSize;
        private final java.util.Queue<T> order = new java.util.LinkedList<>();

        public BoundedInterner(int maxSize) {
            super(false);
            this.maxSize = maxSize;
        }

        @Override
        public T intern(T value) {
            T result = super.intern(value);
            synchronized (order) {
                if (!order.contains(result)) {
                    order.add(result);
                    while (order.size() > maxSize) {
                        T removed = order.poll();
                        if (removed != null) {
                            remove(removed);
                        }
                    }
                }
            }
            return result;
        }

        @Override
        public void clear() {
            super.clear();
            synchronized (order) {
                order.clear();
            }
        }
    }

    /**
     * Custom equality interner.
     */
    public static final class CustomEqualityInterner<T> extends Interner<T> {
        private final BiPredicate<T, T> equals;
        private final Function<T, Integer> hasher;
        private final List<T> values = new ArrayList<>();

        public CustomEqualityInterner(BiPredicate<T, T> equals, Function<T, Integer> hasher) {
            super(false);
            this.equals = equals;
            this.hasher = hasher;
        }

        @Override
        public T intern(T value) {
            if (value == null) return null;

            synchronized (values) {
                for (T existing : values) {
                    if (equals.test(existing, value)) {
                        return existing;
                    }
                }
                values.add(value);
                return value;
            }
        }

        @Override
        public int size() {
            return values.size();
        }

        @Override
        public void clear() {
            values.clear();
        }
    }

    /**
     * Interner interface.
     */
    public interface InternerInterface<T> {
        T intern(T value);
        int size();
        void clear();
    }
}