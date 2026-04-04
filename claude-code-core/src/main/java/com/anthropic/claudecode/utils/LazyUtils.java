/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code lazy evaluation utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

/**
 * Lazy evaluation utilities.
 */
public final class LazyUtils {
    private LazyUtils() {}

    /**
     * Lazy value container.
     */
    public static class Lazy<T> implements Supplier<T> {
        private final Supplier<T> supplier;
        private volatile T value;
        private volatile boolean computed = false;
        private final Object lock = new Object();

        public Lazy(Supplier<T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        public T get() {
            if (!computed) {
                synchronized (lock) {
                    if (!computed) {
                        value = supplier.get();
                        computed = true;
                    }
                }
            }
            return value;
        }

        public boolean isComputed() {
            return computed;
        }

        public Optional<T> getIfComputed() {
            return computed ? Optional.ofNullable(value) : Optional.empty();
        }

        public void reset() {
            synchronized (lock) {
                computed = false;
                value = null;
            }
        }

        public <R> Lazy<R> map(Function<T, R> mapper) {
            return new Lazy<>(() -> mapper.apply(get()));
        }

        public <R> Lazy<R> flatMap(Function<T, Lazy<R>> mapper) {
            return new Lazy<>(() -> mapper.apply(get()).get());
        }

        public static <T> Lazy<T> of(Supplier<T> supplier) {
            return new Lazy<>(supplier);
        }

        public static <T> Lazy<T> ofValue(T value) {
            Lazy<T> lazy = new Lazy<>(() -> value);
            lazy.computed = true;
            lazy.value = value;
            return lazy;
        }
    }

    /**
     * Memoized supplier.
     */
    public static <T> Supplier<T> memoize(Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    /**
     * Memoized function.
     */
    public static <T, R> Function<T, R> memoize(Function<T, R> function) {
        Map<T, R> cache = new ConcurrentHashMap<>();
        return t -> cache.computeIfAbsent(t, function);
    }

    /**
     * Memoized function with expiration.
     */
    public static <T, R> Function<T, R> memoize(Function<T, R> function, long ttlMs) {
        ConcurrentHashMap<T, ExpiringEntry<R>> cache = new ConcurrentHashMap<>();
        return t -> {
            ExpiringEntry<R> entry = cache.get(t);
            long now = System.currentTimeMillis();
            if (entry != null && now < entry.expiry) {
                return entry.value;
            }
            R result = function.apply(t);
            cache.put(t, new ExpiringEntry<>(result, now + ttlMs));
            return result;
        };
    }

    private record ExpiringEntry<V>(V value, long expiry) {}

    /**
     * Thunk (delayed computation).
     */
    public static class Thunk<T> {
        private final Supplier<T> computation;
        private volatile T cachedResult;
        private volatile boolean evaluated = false;

        public Thunk(Supplier<T> computation) {
            this.computation = computation;
        }

        public T force() {
            if (!evaluated) {
                synchronized (this) {
                    if (!evaluated) {
                        cachedResult = computation.get();
                        evaluated = true;
                    }
                }
            }
            return cachedResult;
        }

        public boolean isEvaluated() {
            return evaluated;
        }

        public Optional<T> getValue() {
            return evaluated ? Optional.ofNullable(cachedResult) : Optional.empty();
        }
    }

    /**
     * Create a thunk.
     */
    public static <T> Thunk<T> thunk(Supplier<T> computation) {
        return new Thunk<>(computation);
    }

    /**
     * Lazy sequence.
     */
    public static class LazySeq<T> implements Iterable<T> {
        private final T head;
        private final Lazy<LazySeq<T>> tail;
        private volatile boolean realized = false;

        private LazySeq(T head, Lazy<LazySeq<T>> tail) {
            this.head = head;
            this.tail = tail;
        }

        public T head() {
            return head;
        }

        public LazySeq<T> tail() {
            return tail.get();
        }

        public boolean isRealized() {
            return realized;
        }

        public LazySeq<T> realize() {
            LazySeq<T> current = this;
            while (current != null && !current.realized) {
                current.realized = true;
                current = current.tail.getIfComputed().orElse(null);
            }
            return this;
        }

        public List<T> take(int n) {
            List<T> result = new ArrayList<>();
            LazySeq<T> current = this;
            for (int i = 0; i < n && current != null; i++) {
                result.add(current.head);
                current = current.tail.get();
            }
            return result;
        }

        public LazySeq<T> drop(int n) {
            LazySeq<T> current = this;
            for (int i = 0; i < n && current != null; i++) {
                current = current.tail.get();
            }
            return current;
        }

        public LazySeq<T> filter(Predicate<T> predicate) {
            LazySeq<T> current = this;
            while (current != null) {
                if (predicate.test(current.head)) {
                    LazySeq<T> finalCurrent = current;
                    return new LazySeq<>(current.head, new Lazy<>(() -> finalCurrent.tail().filter(predicate)));
                }
                current = current.tail.get();
            }
            return null;
        }

        public <R> LazySeq<R> map(Function<T, R> mapper) {
            return new LazySeq<>(mapper.apply(head), new Lazy<>(() -> tail.get().map(mapper)));
        }

        public static <T> LazySeq<T> of(T head, Supplier<LazySeq<T>> tail) {
            return new LazySeq<>(head, new Lazy<>(tail));
        }

        public static <T> LazySeq<T> iterate(T seed, Function<T, T> next) {
            return new LazySeq<>(seed, new Lazy<>(() -> iterate(next.apply(seed), next)));
        }

        public static <T> LazySeq<T> generate(Supplier<T> generator) {
            return new LazySeq<>(generator.get(), new Lazy<>(() -> generate(generator)));
        }

        public static LazySeq<Integer> range(int start) {
            return iterate(start, i -> i + 1);
        }

        public static LazySeq<Integer> range(int start, int end) {
            if (start >= end) return null;
            return new LazySeq<>(start, new Lazy<>(() -> range(start + 1, end)));
        }

        @Override
        public Iterator<T> iterator() {
            return new LazySeqIterator<>(this);
        }

        private static class LazySeqIterator<T> implements Iterator<T> {
            private LazySeq<T> current;

            LazySeqIterator(LazySeq<T> seq) {
                this.current = seq;
            }

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                if (current == null) throw new NoSuchElementException();
                T value = current.head;
                current = current.tail.get();
                return value;
            }
        }
    }

    /**
     * Create lazy sequence from function.
     */
    public static <T> LazySeq<T> lazySeq(T head, Supplier<LazySeq<T>> tail) {
        return LazySeq.of(head, tail);
    }

    /**
     * Create infinite lazy sequence.
     */
    public static <T> LazySeq<T> iterate(T seed, Function<T, T> next) {
        return LazySeq.iterate(seed, next);
    }

    /**
     * Create lazy sequence from generator.
     */
    public static <T> LazySeq<T> generate(Supplier<T> generator) {
        return LazySeq.generate(generator);
    }

    /**
     * Delayed computation with dependencies.
     */
    public static class Deferred<T> {
        private final List<Supplier<?>> dependencies = new ArrayList<>();
        private final Supplier<T> computation;
        private volatile T value;
        private volatile boolean computed = false;

        public Deferred(Supplier<T> computation) {
            this.computation = computation;
        }

        public <D> Deferred<T> dependsOn(Supplier<D> dependency) {
            dependencies.add(dependency);
            return this;
        }

        public T get() {
            if (!computed) {
                synchronized (this) {
                    if (!computed) {
                        // Ensure all dependencies are computed
                        for (Supplier<?> dep : dependencies) {
                            dep.get();
                        }
                        value = computation.get();
                        computed = true;
                    }
                }
            }
            return value;
        }

        public boolean isComputed() {
            return computed;
        }
    }

    /**
     * Create deferred computation.
     */
    public static <T> Deferred<T> deferred(Supplier<T> computation) {
        return new Deferred<>(computation);
    }

    /**
     * Lazy property.
     */
    public static class LazyProperty<T> {
        private T value;
        private boolean set = false;
        private final Supplier<T> defaultValue;

        public LazyProperty(Supplier<T> defaultValue) {
            this.defaultValue = defaultValue;
        }

        public T get() {
            if (!set) {
                synchronized (this) {
                    if (!set) {
                        value = defaultValue.get();
                        set = true;
                    }
                }
            }
            return value;
        }

        public void set(T value) {
            synchronized (this) {
                this.value = value;
                this.set = true;
            }
        }

        public boolean isSet() {
            return set;
        }

        public void reset() {
            synchronized (this) {
                set = false;
                value = null;
            }
        }
    }

    /**
     * Create lazy property.
     */
    public static <T> LazyProperty<T> lazyProperty(Supplier<T> defaultValue) {
        return new LazyProperty<>(defaultValue);
    }
}