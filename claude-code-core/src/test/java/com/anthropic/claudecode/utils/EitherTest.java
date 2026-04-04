/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Either.
 */
class EitherTest {

    @Test
    @DisplayName("Either left creates left value")
    void leftCreation() {
        Either<String, Integer> either = Either.left("error");

        assertTrue(either.isLeft());
        assertFalse(either.isRight());
    }

    @Test
    @DisplayName("Either right creates right value")
    void rightCreation() {
        Either<String, Integer> either = Either.right(42);

        assertFalse(either.isLeft());
        assertTrue(either.isRight());
    }

    @Test
    @DisplayName("Either getLeft returns value for left")
    void getLeftLeft() {
        Either<String, Integer> either = Either.left("error");

        Optional<String> left = either.getLeft();
        assertTrue(left.isPresent());
        assertEquals("error", left.get());
    }

    @Test
    @DisplayName("Either getLeft returns empty for right")
    void getLeftRight() {
        Either<String, Integer> either = Either.right(42);

        Optional<String> left = either.getLeft();
        assertFalse(left.isPresent());
    }

    @Test
    @DisplayName("Either getRight returns value for right")
    void getRightRight() {
        Either<String, Integer> either = Either.right(42);

        Optional<Integer> right = either.getRight();
        assertTrue(right.isPresent());
        assertEquals(42, right.get());
    }

    @Test
    @DisplayName("Either getRight returns empty for left")
    void getRightLeft() {
        Either<String, Integer> either = Either.left("error");

        Optional<Integer> right = either.getRight();
        assertFalse(right.isPresent());
    }

    @Test
    @DisplayName("Either getOrElse returns value for right")
    void getOrElseRight() {
        Either<String, Integer> either = Either.right(42);

        assertEquals(42, either.getOrElse(0));
    }

    @Test
    @DisplayName("Either getOrElse returns default for left")
    void getOrElseLeft() {
        Either<String, Integer> either = Either.left("error");

        assertEquals(0, either.getOrElse(0));
    }

    @Test
    @DisplayName("Either getOrElseGet returns value for right")
    void getOrElseGetRight() {
        Either<String, Integer> either = Either.right(42);

        assertEquals(42, either.getOrElseGet(e -> 0));
    }

    @Test
    @DisplayName("Either getOrElseGet applies function for left")
    void getOrElseGetLeft() {
        Either<String, Integer> either = Either.left("error");

        assertEquals(5, either.getOrElseGet(e -> e.length()));
    }

    @Test
    @DisplayName("Either getOrThrow returns value for right")
    void getOrThrowRight() {
        Either<String, Integer> either = Either.right(42);

        assertEquals(42, either.getOrThrow(e -> new RuntimeException(e)));
    }

    @Test
    @DisplayName("Either getOrThrow throws for left")
    void getOrThrowLeft() {
        Either<String, Integer> either = Either.left("error");

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> either.getOrThrow(e -> new RuntimeException(e)));
        assertEquals("error", ex.getMessage());
    }

    @Test
    @DisplayName("Either ifLeft executes for left")
    void ifLeftLeft() {
        Either<String, Integer> either = Either.left("error");
        boolean[] called = {false};

        either.ifLeft(e -> called[0] = true);

        assertTrue(called[0]);
    }

    @Test
    @DisplayName("Either ifLeft does not execute for right")
    void ifLeftRight() {
        Either<String, Integer> either = Either.right(42);
        boolean[] called = {false};

        either.ifLeft(e -> called[0] = true);

        assertFalse(called[0]);
    }

    @Test
    @DisplayName("Either ifRight executes for right")
    void ifRightRight() {
        Either<String, Integer> either = Either.right(42);
        boolean[] called = {false};

        either.ifRight(v -> called[0] = true);

        assertTrue(called[0]);
    }

    @Test
    @DisplayName("Either ifRight does not execute for left")
    void ifRightLeft() {
        Either<String, Integer> either = Either.left("error");
        boolean[] called = {false};

        either.ifRight(v -> called[0] = true);

        assertFalse(called[0]);
    }

    @Test
    @DisplayName("Either map transforms right value")
    void mapRight() {
        Either<String, Integer> either = Either.right(42);

        Either<String, String> mapped = either.map(v -> "value: " + v);

        assertTrue(mapped.isRight());
        assertEquals("value: 42", mapped.getRight().get());
    }

    @Test
    @DisplayName("Either map does nothing for left")
    void mapLeft() {
        Either<String, Integer> either = Either.left("error");

        Either<String, String> mapped = either.map(v -> "value: " + v);

        assertTrue(mapped.isLeft());
        assertEquals("error", mapped.getLeft().get());
    }

    @Test
    @DisplayName("Either mapLeft transforms left value")
    void mapLeftLeft() {
        Either<String, Integer> either = Either.left("error");

        Either<Integer, Integer> mapped = either.mapLeft(e -> e.length());

        assertTrue(mapped.isLeft());
        assertEquals(5, mapped.getLeft().get());
    }

    @Test
    @DisplayName("Either flatMap chains right")
    void flatMapRight() {
        Either<String, Integer> either = Either.right(42);

        Either<String, String> chained = either.flatMap(v -> Either.right("val: " + v));

        assertTrue(chained.isRight());
        assertEquals("val: 42", chained.getRight().get());
    }

    @Test
    @DisplayName("Either flatMap can produce left")
    void flatMapToLeft() {
        Either<String, Integer> either = Either.right(0);

        Either<String, String> chained = either.flatMap(v ->
            v == 0 ? Either.left("zero") : Either.right("val: " + v));

        assertTrue(chained.isLeft());
        assertEquals("zero", chained.getLeft().get());
    }

    @Test
    @DisplayName("Either fold applies right mapper for right")
    void foldRight() {
        Either<String, Integer> either = Either.right(42);

        String result = either.fold(e -> "error: " + e, v -> "value: " + v);

        assertEquals("value: 42", result);
    }

    @Test
    @DisplayName("Either fold applies left mapper for left")
    void foldLeft() {
        Either<String, Integer> either = Either.left("error");

        String result = either.fold(e -> "error: " + e, v -> "value: " + v);

        assertEquals("error: error", result);
    }

    @Test
    @DisplayName("Either swap swaps left to right")
    void swapLeftToRight() {
        Either<String, Integer> either = Either.left("error");

        Either<Integer, String> swapped = either.swap();

        assertTrue(swapped.isRight());
        assertEquals("error", swapped.getRight().get());
    }

    @Test
    @DisplayName("Either swap swaps right to left")
    void swapRightToLeft() {
        Either<String, Integer> either = Either.right(42);

        Either<Integer, String> swapped = either.swap();

        assertTrue(swapped.isLeft());
        assertEquals(42, swapped.getLeft().get());
    }

    @Test
    @DisplayName("Either filter keeps matching right")
    void filterKeepsMatching() {
        Either<String, Integer> either = Either.right(42);

        Optional<Either<String, Integer>> filtered = either.filter(v -> v > 0);

        assertTrue(filtered.isPresent());
        assertTrue(filtered.get().isRight());
    }

    @Test
    @DisplayName("Either filter removes non-matching right")
    void filterRemovesNonMatching() {
        Either<String, Integer> either = Either.right(-1);

        Optional<Either<String, Integer>> filtered = either.filter(v -> v > 0);

        assertFalse(filtered.isPresent());
    }

    @Test
    @DisplayName("Either filter keeps left unchanged")
    void filterKeepsLeft() {
        Either<String, Integer> either = Either.left("error");

        Optional<Either<String, Integer>> filtered = either.filter(v -> v > 0);

        assertTrue(filtered.isPresent());
        assertTrue(filtered.get().isLeft());
    }

    @Test
    @DisplayName("Either fromOptional returns right for present")
    void fromOptionalPresent() {
        Optional<Integer> opt = Optional.of(42);

        Either<String, Integer> either = Either.fromOptional(opt, "missing");

        assertTrue(either.isRight());
        assertEquals(42, either.getRight().get());
    }

    @Test
    @DisplayName("Either fromOptional returns left for empty")
    void fromOptionalEmpty() {
        Optional<Integer> opt = Optional.empty();

        Either<String, Integer> either = Either.fromOptional(opt, "missing");

        assertTrue(either.isLeft());
        assertEquals("missing", either.getLeft().get());
    }

    @Test
    @DisplayName("Either tryCatch returns right on success")
    void tryCatchSuccess() {
        Either<String, Integer> either = Either.tryCatch(() -> 42);

        assertTrue(either.isRight());
        assertEquals(42, either.getRight().get());
    }

    @Test
    @DisplayName("Either tryCatch returns left on exception")
    void tryCatchException() {
        Either<String, Integer> either = Either.tryCatch(() -> {
            throw new RuntimeException("failed");
        });

        assertTrue(either.isLeft());
        assertTrue(either.getLeft().get().contains("failed"));
    }

    @Test
    @DisplayName("Either EitherUtils collectRight collects rights")
    void collectRight() {
        List<Either<String, Integer>> eithers = List.of(
            Either.right(1),
            Either.left("error"),
            Either.right(2)
        );

        List<Integer> rights = Either.EitherUtils.collectRight(eithers);

        assertEquals(2, rights.size());
        assertEquals(1, rights.get(0));
        assertEquals(2, rights.get(1));
    }

    @Test
    @DisplayName("Either EitherUtils collectLeft collects lefts")
    void collectLeft() {
        List<Either<String, Integer>> eithers = List.of(
            Either.right(1),
            Either.left("error1"),
            Either.left("error2")
        );

        List<String> lefts = Either.EitherUtils.collectLeft(eithers);

        assertEquals(2, lefts.size());
        assertEquals("error1", lefts.get(0));
        assertEquals("error2", lefts.get(1));
    }

    @Test
    @DisplayName("Either EitherUtils sequence returns left on first left")
    void sequenceLeft() {
        List<Either<String, Integer>> eithers = List.of(
            Either.left("error"),
            Either.right(1)
        );

        Either<String, List<Integer>> result = Either.EitherUtils.sequence(eithers);

        assertTrue(result.isLeft());
        assertEquals("error", result.getLeft().get());
    }

    @Test
    @DisplayName("Either EitherUtils sequence returns right on all rights")
    void sequenceRight() {
        List<Either<String, Integer>> eithers = List.of(
            Either.right(1),
            Either.right(2)
        );

        Either<String, List<Integer>> result = Either.EitherUtils.sequence(eithers);

        assertTrue(result.isRight());
        assertEquals(2, result.getRight().get().size());
    }

    @Test
    @DisplayName("Either EitherUtils bimap transforms both")
    void bimapRight() {
        Either<String, Integer> either = Either.right(42);

        Either<Integer, String> bimapped = Either.EitherUtils.bimap(
            either,
            e -> e.length(),
            v -> "val: " + v
        );

        assertTrue(bimapped.isRight());
        assertEquals("val: 42", bimapped.getRight().get());
    }

    @Test
    @DisplayName("Either EitherUtils bimap transforms left")
    void bimapLeft() {
        Either<String, Integer> either = Either.left("error");

        Either<Integer, String> bimapped = Either.EitherUtils.bimap(
            either,
            e -> e.length(),
            v -> "val: " + v
        );

        assertTrue(bimapped.isLeft());
        assertEquals(5, bimapped.getLeft().get());
    }
}