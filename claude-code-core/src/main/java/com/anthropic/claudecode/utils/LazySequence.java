/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code lazy sequence
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Lazy sequence - generates values on demand.
 */
public final class LazySequence<T> implements Iterable<T> {
    private final Supplier<Optional<T>> generator;
    private final java.util.Queue<T> buffer = new java.util.ArrayDeque<>();
    private boolean exhausted = false;

    public LazySequence(Supplier<Optional<T>> generator) {
        this.generator = generator;
    }

    /**
     * Get next element.
     */
    public Optional<T> next() {
        if (!buffer.isEmpty()) {
            return Optional.of(buffer.poll());
        }
        if (exhausted) {
            return Optional.empty();
        }

        Optional<T> result = generator.get();
        if (result.isEmpty()) {
            exhausted = true;
        }
        return result;
    }

    /**
     * Take N elements.
     */
    public List<T> take(int n) {
        List<T> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Optional<T> next = next();
            if (next.isEmpty()) break;
            result.add(next.get());
        }
        return result;
    }

    /**
     * Take while predicate is true.
     */
    public List<T> takeWhile(Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        while (true) {
            Optional<T> next = next();
            if (next.isEmpty() || !predicate.test(next.get())) {
                if (next.isPresent()) {
                    buffer.add(next.get());
                }
                break;
            }
            result.add(next.get());
        }
        return result;
    }

    /**
     * Drop N elements.
     */
    public LazySequence<T> drop(int n) {
        for (int i = 0; i < n; i++) {
            next();
        }
        return this;
    }

    /**
     * Drop while predicate is true.
     */
    public LazySequence<T> dropWhile(Predicate<T> predicate) {
        while (true) {
            Optional<T> next = next();
            if (next.isEmpty() || !predicate.test(next.get())) {
                if (next.isPresent()) {
                    buffer.add(next.get());
                }
                break;
            }
        }
        return this;
    }

    /**
     * Filter sequence.
     */
    public LazySequence<T> filter(Predicate<T> predicate) {
        return new LazySequence<>(() -> {
            while (true) {
                Optional<T> next = LazySequence.this.next();
                if (next.isEmpty()) return Optional.empty();
                if (predicate.test(next.get())) return next;
            }
        });
    }

    /**
     * Map sequence.
     */
    public <R> LazySequence<R> map(Function<T, R> mapper) {
        return new LazySequence<>(() -> next().map(mapper));
    }

    /**
     * Flat map sequence.
     */
    public <R> LazySequence<R> flatMap(Function<T, LazySequence<R>> mapper) {
        LazySequence<R>[] current = new LazySequence[]{null};
        return new LazySequence<>(() -> {
            while (true) {
                if (current[0] == null) {
                    Optional<T> next = LazySequence.this.next();
                    if (next.isEmpty()) return Optional.empty();
                    current[0] = mapper.apply(next.get());
                }
                Optional<R> next = current[0].next();
                if (next.isPresent()) return next;
                current[0] = null;
            }
        });
    }

    /**
     * Fold/reduce sequence.
     */
    public <A> A fold(A initial, BiFunction<A, T, A> accumulator) {
        A acc = initial;
        while (true) {
            Optional<T> next = next();
            if (next.isEmpty()) break;
            acc = accumulator.apply(acc, next.get());
        }
        return acc;
    }

    /**
     * Reduce sequence.
     */
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        Optional<T> first = next();
        if (first.isEmpty()) return Optional.empty();

        T acc = first.get();
        while (true) {
            Optional<T> next = next();
            if (next.isEmpty()) break;
            acc = accumulator.apply(acc, next.get());
        }
        return Optional.of(acc);
    }

    /**
     * For each element.
     */
    @Override
    public void forEach(Consumer<? super T> action) {
        while (true) {
            Optional<T> next = next();
            if (next.isEmpty()) break;
            action.accept(next.get());
        }
    }

    /**
     * Collect all elements (careful with infinite sequences).
     */
    public List<T> toList() {
        List<T> result = new ArrayList<>();
        forEach(result::add);
        return result;
    }

    /**
     * Collect to set.
     */
    public Set<T> toSet() {
        Set<T> result = new HashSet<>();
        forEach(result::add);
        return result;
    }

    /**
     * Count elements (consumes sequence).
     */
    public long count() {
        long count = 0;
        while (next().isPresent()) {
            count++;
        }
        return count;
    }

    /**
     * Check if any matches.
     */
    public boolean anyMatch(Predicate<T> predicate) {
        while (true) {
            Optional<T> next = next();
            if (next.isEmpty()) return false;
            if (predicate.test(next.get())) {
                buffer.add(next.get());
                return true;
            }
        }
    }

    /**
     * Check if all match.
     */
    public boolean allMatch(Predicate<T> predicate) {
        while (true) {
            Optional<T> next = next();
            if (next.isEmpty()) return true;
            if (!predicate.test(next.get())) {
                buffer.add(next.get());
                return false;
            }
        }
    }

    /**
     * Check if none match.
     */
    public boolean noneMatch(Predicate<T> predicate) {
        return !anyMatch(predicate);
    }

    /**
     * Find first matching.
     */
    public Optional<T> findFirst(Predicate<T> predicate) {
        while (true) {
            Optional<T> next = next();
            if (next.isEmpty()) return Optional.empty();
            if (predicate.test(next.get())) return next;
        }
    }

    /**
     * Is exhausted.
     */
    public boolean isExhausted() {
        return exhausted && buffer.isEmpty();
    }

    /**
     * Convert to stream.
     */
    public java.util.stream.Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return !LazySequence.this.isExhausted();
            }

            @Override
            public T next() {
                return LazySequence.this.next().orElseThrow();
            }
        };
    }

    /**
     * Lazy sequence utilities.
     */
    public static final class LazySequenceUtils {
        private LazySequenceUtils() {}

        /**
         * Create from iterable.
         */
        public static <T> LazySequence<T> fromIterable(Iterable<T> iterable) {
            Iterator<T> iterator = iterable.iterator();
            return new LazySequence<>(() ->
                iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty()
            );
        }

        /**
         * Create from stream.
         */
        public static <T> LazySequence<T> fromStream(java.util.stream.Stream<T> stream) {
            Iterator<T> iterator = stream.iterator();
            return new LazySequence<>(() ->
                iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty()
            );
        }

        /**
         * Create infinite sequence from supplier.
         */
        public static <T> LazySequence<T> generate(Supplier<T> supplier) {
            return new LazySequence<>(() -> Optional.of(supplier.get()));
        }

        /**
         * Create sequence from function (iterate).
         */
        public static <T> LazySequence<T> iterate(T seed, UnaryOperator<T> next) {
            AtomicReference<T> current = new AtomicReference<>(seed);
            AtomicBoolean first = new AtomicBoolean(true);
            return new LazySequence<>(() -> {
                if (first.compareAndSet(true, false)) {
                    return Optional.of(current.get());
                }
                current.set(next.apply(current.get()));
                return Optional.of(current.get());
            });
        }

        /**
         * Create range.
         */
        public static LazySequence<Integer> range(int start, int end) {
            AtomicInteger current = new AtomicInteger(start);
            return new LazySequence<>(() -> {
                int val = current.get();
                if (val >= end) return Optional.empty();
                current.incrementAndGet();
                return Optional.of(val);
            });
        }

        /**
         * Create range with step.
         */
        public static LazySequence<Integer> range(int start, int end, int step) {
            AtomicInteger current = new AtomicInteger(start);
            return new LazySequence<>(() -> {
                int val = current.get();
                if (val >= end) return Optional.empty();
                current.addAndGet(step);
                return Optional.of(val);
            });
        }

        /**
         * Repeat value.
         */
        public static <T> LazySequence<T> repeat(T value) {
            return new LazySequence<>(() -> Optional.of(value));
        }

        /**
         * Cycle through values.
         */
        @SafeVarargs
        public static <T> LazySequence<T> cycle(T... values) {
            return cycle(Arrays.asList(values));
        }

        /**
         * Cycle through values.
         */
        public static <T> LazySequence<T> cycle(List<T> values) {
            if (values.isEmpty()) {
                return new LazySequence<>(Optional::empty);
            }
            AtomicInteger index = new AtomicInteger(0);
            return new LazySequence<>(() -> {
                int i = index.getAndUpdate(v -> (v + 1) % values.size());
                return Optional.of(values.get(i));
            });
        }

        /**
         * Fibonacci sequence.
         */
        public static LazySequence<Long> fibonacci() {
            AtomicLong a = new AtomicLong(0);
            AtomicLong b = new AtomicLong(1);
            AtomicBoolean first = new AtomicBoolean(true);
            return new LazySequence<>(() -> {
                if (first.compareAndSet(true, false)) {
                    return Optional.of(a.get());
                }
                long next = a.get() + b.get();
                a.set(b.get());
                b.set(next);
                return Optional.of(a.get());
            });
        }

        /**
         * Primes sequence.
         */
        public static LazySequence<Long> primes() {
            AtomicLong current = new AtomicLong(2);
            return new LazySequence<>(() -> {
                long n = current.get();
                while (!isPrime(n)) {
                    n++;
                }
                current.set(n + 1);
                return Optional.of(n);
            });
        }

        private static boolean isPrime(long n) {
            if (n < 2) return false;
            if (n == 2) return true;
            if (n % 2 == 0) return false;
            for (long i = 3; i * i <= n; i += 2) {
                if (n % i == 0) return false;
            }
            return true;
        }
    }

    // Helper atomic classes
    private static class AtomicReference<T> extends java.util.concurrent.atomic.AtomicReference<T> {
        public AtomicReference(T value) { super(value); }
    }

    private static class AtomicInteger extends java.util.concurrent.atomic.AtomicInteger {
        public AtomicInteger(int value) { super(value); }
    }

    private static class AtomicLong extends java.util.concurrent.atomic.AtomicLong {
        public AtomicLong(long value) { super(value); }
    }

    private static class AtomicBoolean extends java.util.concurrent.atomic.AtomicBoolean {
        public AtomicBoolean(boolean value) { super(value); }
    }
}