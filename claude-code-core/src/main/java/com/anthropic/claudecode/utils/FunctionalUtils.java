/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code functional utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Functional programming utilities.
 */
public final class FunctionalUtils {
    private FunctionalUtils() {}

    /**
     * Compose two functions.
     */
    public static <A, B, C> Function<A, C> compose(Function<B, C> f, Function<A, B> g) {
        return f.compose(g);
    }

    /**
     * Pipe two functions (reverse compose).
     */
    public static <A, B, C> Function<A, C> pipe(Function<A, B> f, Function<B, C> g) {
        return f.andThen(g);
    }

    /**
     * Identity function.
     */
    public static <T> Function<T, T> identity() {
        return Function.identity();
    }

    /**
     * Constant function.
     */
    public static <T, R> Function<T, R> constant(R value) {
        return t -> value;
    }

    /**
     * Always true predicate.
     */
    public static <T> Predicate<T> alwaysTrue() {
        return t -> true;
    }

    /**
     * Always false predicate.
     */
    public static <T> Predicate<T> alwaysFalse() {
        return t -> false;
    }

    /**
     * Negate predicate.
     */
    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }

    /**
     * Curried bi-function.
     */
    public static <T, U, R> Function<T, Function<U, R>> curry(BiFunction<T, U, R> bif) {
        return t -> u -> bif.apply(t, u);
    }

    /**
     * Uncurried function.
     */
    public static <T, U, R> BiFunction<T, U, R> uncurry(Function<T, Function<U, R>> f) {
        return (t, u) -> f.apply(t).apply(u);
    }

    /**
     * Flip bi-function arguments.
     */
    public static <T, U, R> BiFunction<U, T, R> flip(BiFunction<T, U, R> bif) {
        return (u, t) -> bif.apply(t, u);
    }

    /**
     * Memoize a function.
     */
    public static <T, R> Function<T, R> memoize(Function<T, R> f) {
        Map<T, R> cache = new ConcurrentHashMap<>();
        return t -> cache.computeIfAbsent(t, f);
    }

    /**
     * Memoize with expiration.
     */
    public static <T, R> Function<T, R> memoize(Function<T, R> f, long ttlMs) {
        ConcurrentHashMap<T, CacheEntry<R>> cache = new ConcurrentHashMap<>();
        return t -> {
            CacheEntry<R> entry = cache.get(t);
            long now = System.currentTimeMillis();
            if (entry != null && now < entry.expiry()) {
                return entry.value();
            }
            R result = f.apply(t);
            cache.put(t, new CacheEntry<>(result, now + ttlMs));
            return result;
        };
    }

    private record CacheEntry<V>(V value, long expiry) {}

    /**
     * Lazy evaluation.
     */
    public static <T> Supplier<T> lazy(Supplier<T> supplier) {
        return new LazySupplier<>(supplier);
    }

    private static class LazySupplier<T> implements Supplier<T> {
        private final Supplier<T> supplier;
        private volatile T value;
        private volatile boolean computed = false;

        LazySupplier(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public T get() {
            if (!computed) {
                synchronized (this) {
                    if (!computed) {
                        value = supplier.get();
                        computed = true;
                    }
                }
            }
            return value;
        }
    }

    /**
     * Try-catch wrapper.
     */
    public static <T> Supplier<Result<T>> try_(Supplier<T> supplier) {
        return () -> {
            try {
                return Result.ok(supplier.get());
            } catch (Exception e) {
                return Result.err(e.getMessage());
            }
        };
    }

    /**
     * Try-catch for runnable.
     */
    public static Supplier<Result<Void>> tryRun(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
                return Result.ok(null);
            } catch (Exception e) {
                return Result.err(e.getMessage());
            }
        };
    }

    /**
     * Tap for side effects.
     */
    public static <T> Function<T, T> tap(Consumer<T> action) {
        return t -> {
            action.accept(t);
            return t;
        };
    }

    /**
     * Peek for streams.
     */
    public static <T> Consumer<T> peek(Consumer<T> action) {
        return action;
    }

    /**
     * Conditional function application.
     */
    public static <T> Function<T, T> when(Predicate<T> condition, Function<T, T> transform) {
        return t -> condition.test(t) ? transform.apply(t) : t;
    }

    /**
     * Conditional consumer.
     */
    public static <T> Consumer<T> when(Predicate<T> condition, Consumer<T> action) {
        return t -> {
            if (condition.test(t)) {
                action.accept(t);
            }
        };
    }

    /**
     * Either function application.
     */
    public static <T, R> Function<T, R> either(
            Predicate<T> condition,
            Function<T, R> ifTrue,
            Function<T, R> ifFalse) {
        return t -> condition.test(t) ? ifTrue.apply(t) : ifFalse.apply(t);
    }

    /**
     * Pattern matching style.
     */
    public static <T, R> PatternMatcher<T, R> match(T value) {
        return new PatternMatcher<>(value);
    }

    public static class PatternMatcher<T, R> {
        private final T value;
        private R result;
        private boolean matched = false;

        PatternMatcher(T value) {
            this.value = value;
        }

        public PatternMatcher<T, R> when(Predicate<T> condition, Function<T, R> transform) {
            if (!matched && condition.test(value)) {
                result = transform.apply(value);
                matched = true;
            }
            return this;
        }

        public PatternMatcher<T, R> when(T expected, R result) {
            return when(t -> Objects.equals(t, expected), t -> result);
        }

        public PatternMatcher<T, R> otherwise(Function<T, R> transform) {
            if (!matched) {
                result = transform.apply(value);
                matched = true;
            }
            return this;
        }

        public PatternMatcher<T, R> otherwise(R defaultValue) {
            return otherwise(t -> defaultValue);
        }

        public R get() {
            if (!matched) {
                throw new IllegalStateException("No pattern matched");
            }
            return result;
        }

        public Optional<R> getOptional() {
            return matched ? Optional.ofNullable(result) : Optional.empty();
        }
    }

    /**
     * Retry function.
     */
    public static <T> Supplier<T> retry(Supplier<T> supplier, int maxAttempts, long delayMs) {
        return () -> {
            Exception lastError = null;
            for (int i = 0; i < maxAttempts; i++) {
                try {
                    return supplier.get();
                } catch (Exception e) {
                    lastError = e;
                    if (i < maxAttempts - 1) {
                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(ie);
                        }
                    }
                }
            }
            throw new RuntimeException("Retry failed", lastError);
        };
    }

    /**
     * Timeout wrapper.
     */
    public static <T> Supplier<T> withTimeout(Supplier<T> supplier, long timeoutMs) {
        return () -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<T> future = executor.submit(supplier::get);
                return future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                throw new RuntimeException("Operation timed out", e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                executor.shutdownNow();
            }
        };
    }

    /**
     * Y combinator for recursion.
     */
    public static <T, R> Function<T, R> fix(Function<Function<T, R>, Function<T, R>> f) {
        return t -> f.apply(fix(f)).apply(t);
    }

    /**
     * Traverse collection with function.
     */
    public static <T, R> List<R> traverse(Collection<T> collection, Function<T, R> f) {
        return collection.stream().map(f).toList();
    }

    /**
     * Sequence list of options.
     */
    public static <T> Option<List<T>> sequenceOptions(List<Option<T>> options) {
        List<T> result = new ArrayList<>();
        for (Option<T> opt : options) {
            if (opt.isEmpty()) {
                return Option.none();
            }
            result.add(opt.get());
        }
        return Option.some(result);
    }

    /**
     * Sequence list of results.
     */
    public static <T> Result<List<T>> sequenceResults(List<Result<T>> results) {
        List<String> errors = new ArrayList<>();
        List<T> values = new ArrayList<>();

        for (Result<T> r : results) {
            if (r.isErr()) {
                r.getError().ifPresent(errors::add);
            } else {
                r.getValue().ifPresent(values::add);
            }
        }

        return errors.isEmpty() ? Result.ok(values) : Result.err(errors);
    }

    /**
     * Loop with state.
     */
    public static <S, R> R loop(S initial, Function<S, LoopState<S, R>> f) {
        S state = initial;
        while (true) {
            LoopState<S, R> result = f.apply(state);
            if (result.isDone()) {
                return result.value();
            }
            state = result.nextState();
        }
    }

    public record LoopState<S, R>(boolean done, S nextState, R value) {
        public static <S, R> LoopState<S, R> continue_(S state) {
            return new LoopState<>(false, state, null);
        }

        public static <S, R> LoopState<S, R> done(R value) {
            return new LoopState<>(true, null, value);
        }

        public boolean isDone() { return done; }
    }
}