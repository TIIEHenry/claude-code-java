/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.function.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FunctionalUtils.
 */
class FunctionalUtilsTest {

    @Test
    @DisplayName("FunctionalUtils compose functions")
    void compose() {
        Function<String, Integer> parseInt = Integer::parseInt;
        Function<Integer, String> toString = Object::toString;

        Function<String, String> composed = FunctionalUtils.compose(toString, parseInt);
        assertEquals("42", composed.apply("42"));
    }

    @Test
    @DisplayName("FunctionalUtils pipe functions")
    void pipe() {
        Function<String, Integer> parseInt = Integer::parseInt;
        Function<Integer, String> toString = Object::toString;

        Function<String, String> piped = FunctionalUtils.pipe(parseInt, toString);
        assertEquals("42", piped.apply("42"));
    }

    @Test
    @DisplayName("FunctionalUtils identity function")
    void identity() {
        Function<String, String> identity = FunctionalUtils.identity();
        assertEquals("test", identity.apply("test"));
        assertNull(identity.apply(null));
    }

    @Test
    @DisplayName("FunctionalUtils constant function")
    void constant() {
        Function<String, Integer> constant = FunctionalUtils.constant(42);
        assertEquals(42, constant.apply("anything"));
        assertEquals(42, constant.apply(null));
    }

    @Test
    @DisplayName("FunctionalUtils alwaysTrue predicate")
    void alwaysTrue() {
        Predicate<String> alwaysTrue = FunctionalUtils.alwaysTrue();
        assertTrue(alwaysTrue.test("anything"));
        assertTrue(alwaysTrue.test(null));
    }

    @Test
    @DisplayName("FunctionalUtils alwaysFalse predicate")
    void alwaysFalse() {
        Predicate<String> alwaysFalse = FunctionalUtils.alwaysFalse();
        assertFalse(alwaysFalse.test("anything"));
        assertFalse(alwaysFalse.test(null));
    }

    @Test
    @DisplayName("FunctionalUtils not predicate")
    void not() {
        Predicate<String> isEmpty = String::isEmpty;
        Predicate<String> notEmpty = FunctionalUtils.not(isEmpty);

        assertTrue(notEmpty.test("hello"));
        assertFalse(notEmpty.test(""));
    }

    @Test
    @DisplayName("FunctionalUtils curry bi-function")
    void curry() {
        BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
        Function<Integer, Function<Integer, Integer>> curried = FunctionalUtils.curry(add);

        assertEquals(15, curried.apply(10).apply(5));
    }

    @Test
    @DisplayName("FunctionalUtils uncurry function")
    void uncurry() {
        Function<Integer, Function<Integer, Integer>> curried = a -> b -> a * b;
        BiFunction<Integer, Integer, Integer> uncurried = FunctionalUtils.uncurry(curried);

        assertEquals(50, uncurried.apply(10, 5));
    }

    @Test
    @DisplayName("FunctionalUtils flip bi-function")
    void flip() {
        BiFunction<String, Integer, String> repeat = (s, n) -> s.repeat(n);
        BiFunction<Integer, String, String> flipped = FunctionalUtils.flip(repeat);

        assertEquals("aaa", flipped.apply(3, "a"));
    }

    @Test
    @DisplayName("FunctionalUtils memoize caches results")
    void memoize() {
        AtomicInteger counter = new AtomicInteger(0);
        Function<String, Integer> memoized = FunctionalUtils.memoize(s -> counter.incrementAndGet());

        assertEquals(1, memoized.apply("a"));
        assertEquals(1, memoized.apply("a")); // Cached
        assertEquals(2, memoized.apply("b"));
        assertEquals(2, memoized.apply("b")); // Cached
    }

    @Test
    @DisplayName("FunctionalUtils memoize with TTL")
    void memoizeWithTtl() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Function<String, Integer> memoized = FunctionalUtils.memoize(s -> counter.incrementAndGet(), 50);

        assertEquals(1, memoized.apply("a"));
        assertEquals(1, memoized.apply("a")); // Cached within TTL

        Thread.sleep(100);
        assertEquals(2, memoized.apply("a")); // Expired, recompute
    }

    @Test
    @DisplayName("FunctionalUtils lazy evaluates once")
    void lazy() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> lazy = FunctionalUtils.lazy(() -> counter.incrementAndGet());

        assertEquals(1, lazy.get());
        assertEquals(1, lazy.get()); // Same value
        assertEquals(1, counter.get()); // Only called once
    }

    @Test
    @DisplayName("FunctionalUtils try_ returns Ok on success")
    void trySuccess() {
        Supplier<Result<Integer>> result = FunctionalUtils.try_(() -> 42);
        Result<Integer> r = result.get();

        assertTrue(r.isOk());
        assertEquals(42, r.getValue().orElse(null));
    }

    @Test
    @DisplayName("FunctionalUtils try_ returns Err on exception")
    void tryError() {
        Supplier<Result<Integer>> result = FunctionalUtils.try_(() -> {
            throw new RuntimeException("error");
        });
        Result<Integer> r = result.get();

        assertTrue(r.isErr());
        assertTrue(r.getError().isPresent());
    }

    @Test
    @DisplayName("FunctionalUtils tryRun returns Ok on success")
    void tryRunSuccess() {
        Supplier<Result<Void>> result = FunctionalUtils.tryRun(() -> {});
        Result<Void> r = result.get();

        assertTrue(r.isOk());
    }

    @Test
    @DisplayName("FunctionalUtils tryRun returns Err on exception")
    void tryRunError() {
        Supplier<Result<Void>> result = FunctionalUtils.tryRun(() -> {
            throw new RuntimeException("error");
        });
        Result<Void> r = result.get();

        assertTrue(r.isErr());
    }

    @Test
    @DisplayName("FunctionalUtils tap applies side effect")
    void tap() {
        StringBuilder sb = new StringBuilder();
        Function<String, String> tap = FunctionalUtils.tap(s -> sb.append(s));

        assertEquals("hello", tap.apply("hello"));
        assertEquals("hello", sb.toString());
    }

    @Test
    @DisplayName("FunctionalUtils peek returns consumer")
    void peek() {
        StringBuilder sb = new StringBuilder();
        Consumer<String> peek = FunctionalUtils.peek(s -> sb.append(s));

        peek.accept("hello");
        assertEquals("hello", sb.toString());
    }

    @Test
    @DisplayName("FunctionalUtils when with predicate and function")
    void whenPredicateFunction() {
        Function<Integer, Integer> whenPositive = FunctionalUtils.when(
            i -> i > 0,
            i -> i * 2
        );

        assertEquals(10, whenPositive.apply(5));
        assertEquals(-5, whenPositive.apply(-5)); // Unchanged
    }

    @Test
    @DisplayName("FunctionalUtils when with predicate and consumer")
    void whenPredicateConsumer() {
        StringBuilder sb = new StringBuilder();
        Consumer<String> whenNonEmpty = FunctionalUtils.<String>when(
            s -> !s.isEmpty(),
            (Consumer<String>) sb::append
        );

        whenNonEmpty.accept("hello");
        assertEquals("hello", sb.toString());

        whenNonEmpty.accept("");
        assertEquals("hello", sb.toString()); // Unchanged
    }

    @Test
    @DisplayName("FunctionalUtils either function")
    void either() {
        Function<Integer, String> either = FunctionalUtils.either(
            i -> i > 0,
            i -> "positive",
            i -> "negative or zero"
        );

        assertEquals("positive", either.apply(5));
        assertEquals("negative or zero", either.apply(-5));
        assertEquals("negative or zero", either.apply(0));
    }

    @Test
    @DisplayName("FunctionalUtils PatternMatcher when predicate")
    void patternMatcherWhenPredicate() {
        String result = FunctionalUtils.<Integer, String>match(5)
            .when(i -> i > 10, i -> "big")
            .when(i -> i > 0, i -> "positive")
            .otherwise("zero or negative")
            .get();

        assertEquals("positive", result);
    }

    @Test
    @DisplayName("FunctionalUtils PatternMatcher when value")
    void patternMatcherWhenValue() {
        String result = FunctionalUtils.<String, String>match("hello")
            .when("world", "matched world")
            .when("hello", "matched hello")
            .otherwise("no match")
            .get();

        assertEquals("matched hello", result);
    }

    @Test
    @DisplayName("FunctionalUtils PatternMatcher otherwise function")
    void patternMatcherOtherwiseFunction() {
        String result = FunctionalUtils.<Integer, String>match(-5)
            .when(i -> i > 0, i -> "positive")
            .otherwise(i -> "negative: " + i)
            .get();

        assertEquals("negative: -5", result);
    }

    @Test
    @DisplayName("FunctionalUtils PatternMatcher otherwise default")
    void patternMatcherOtherwiseDefault() {
        String result = FunctionalUtils.<Integer, String>match(0)
            .when(i -> i > 0, i -> "positive")
            .otherwise("default")
            .get();

        assertEquals("default", result);
    }

    @Test
    @DisplayName("FunctionalUtils PatternMatcher get throws if no match")
    void patternMatcherNoMatch() {
        assertThrows(IllegalStateException.class, () ->
            FunctionalUtils.<Integer, String>match(0)
                .when(i -> i > 0, i -> "positive")
                .get()
        );
    }

    @Test
    @DisplayName("FunctionalUtils PatternMatcher getOptional")
    void patternMatcherGetOptional() {
        Optional<String> matched = FunctionalUtils.<Integer, String>match(5)
            .when(i -> i > 0, i -> "positive")
            .getOptional();

        assertTrue(matched.isPresent());
        assertEquals("positive", matched.get());

        Optional<String> unmatched = FunctionalUtils.<Integer, String>match(-5)
            .when(i -> i > 0, i -> "positive")
            .getOptional();

        assertFalse(unmatched.isPresent());
    }

    @Test
    @DisplayName("FunctionalUtils retry succeeds eventually")
    void retrySuccess() {
        AtomicInteger attempts = new AtomicInteger(0);
        Supplier<Integer> retry = FunctionalUtils.retry(() -> {
            int a = attempts.incrementAndGet();
            if (a < 3) throw new RuntimeException("fail");
            return 42;
        }, 5, 10);

        assertEquals(42, retry.get());
        assertEquals(3, attempts.get());
    }

    @Test
    @DisplayName("FunctionalUtils retry fails after max attempts")
    void retryFail() {
        Supplier<Integer> retry = FunctionalUtils.retry(() -> {
            throw new RuntimeException("always fail");
        }, 3, 10);

        assertThrows(RuntimeException.class, retry::get);
    }

    @Test
    @DisplayName("FunctionalUtils withTimeout succeeds")
    void withTimeoutSuccess() {
        Supplier<Integer> withTimeout = FunctionalUtils.withTimeout(() -> 42, 1000);
        assertEquals(42, withTimeout.get());
    }

    @Test
    @DisplayName("FunctionalUtils withTimeout throws on timeout")
    void withTimeoutFail() {
        Supplier<Integer> withTimeout = FunctionalUtils.withTimeout(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 42;
        }, 100);

        assertThrows(RuntimeException.class, withTimeout::get);
    }

    @Test
    @DisplayName("FunctionalUtils fix Y combinator")
    void fix() {
        // Factorial using Y combinator
        Function<Integer, Integer> factorial = FunctionalUtils.fix(
            self -> n -> n <= 1 ? 1 : n * self.apply(n - 1)
        );

        assertEquals(1, factorial.apply(1));
        assertEquals(2, factorial.apply(2));
        assertEquals(6, factorial.apply(3));
        assertEquals(120, factorial.apply(5));
    }

    @Test
    @DisplayName("FunctionalUtils traverse collection")
    void traverse() {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);
        List<String> strings = FunctionalUtils.traverse(numbers, n -> "n" + n);

        assertEquals(5, strings.size());
        assertEquals("n1", strings.get(0));
        assertEquals("n5", strings.get(4));
    }

    @Test
    @DisplayName("FunctionalUtils sequenceOptions all present")
    void sequenceOptionsAllPresent() {
        List<Option<Integer>> options = List.of(
            Option.some(1),
            Option.some(2),
            Option.some(3)
        );

        Option<List<Integer>> result = FunctionalUtils.sequenceOptions(options);
        assertTrue(result.isPresent());
        assertEquals(List.of(1, 2, 3), result.get());
    }

    @Test
    @DisplayName("FunctionalUtils sequenceOptions one empty")
    void sequenceOptionsOneEmpty() {
        List<Option<Integer>> options = List.of(
            Option.some(1),
            Option.none(),
            Option.some(3)
        );

        Option<List<Integer>> result = FunctionalUtils.sequenceOptions(options);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("FunctionalUtils sequenceResults all ok")
    void sequenceResultsAllOk() {
        List<Result<Integer>> results = List.of(
            Result.ok(1),
            Result.ok(2),
            Result.ok(3)
        );

        Result<List<Integer>> result = FunctionalUtils.sequenceResults(results);
        assertTrue(result.isOk());
        assertEquals(List.of(1, 2, 3), result.getValue().orElse(null));
    }

    @Test
    @DisplayName("FunctionalUtils sequenceResults one err")
    void sequenceResultsOneErr() {
        List<Result<Integer>> results = List.of(
            Result.ok(1),
            Result.err("error"),
            Result.ok(3)
        );

        Result<List<Integer>> result = FunctionalUtils.sequenceResults(results);
        assertTrue(result.isErr());
    }

    @Test
    @DisplayName("FunctionalUtils loop with state")
    void loop() {
        // Sum numbers from 1 to 5
        int sum = FunctionalUtils.loop(
            new int[]{0, 1},  // [sum, current]
            state -> {
                if (state[1] > 5) {
                    return FunctionalUtils.LoopState.done(state[0]);
                }
                return FunctionalUtils.LoopState.continue_(new int[]{state[0] + state[1], state[1] + 1});
            }
        );

        assertEquals(15, sum); // 1+2+3+4+5 = 15
    }

    @Test
    @DisplayName("FunctionalUtils LoopState continue_")
    void loopStateContinue() {
        FunctionalUtils.LoopState<Integer, String> state = FunctionalUtils.LoopState.continue_(42);
        assertFalse(state.isDone());
        assertEquals(42, state.nextState());
        assertNull(state.value());
    }

    @Test
    @DisplayName("FunctionalUtils LoopState done")
    void loopStateDone() {
        FunctionalUtils.LoopState<Integer, String> state = FunctionalUtils.LoopState.done("result");
        assertTrue(state.isDone());
        assertEquals("result", state.value());
        assertNull(state.nextState());
    }
}