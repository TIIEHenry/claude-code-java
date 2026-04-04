/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code Either type
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Either type - represents a value that can be either Left (error) or Right (success).
 */
public sealed interface Either<L, R> permits Either.Left, Either.Right {

    boolean isLeft();
    boolean isRight();

    Optional<L> getLeft();
    Optional<R> getRight();

    R getOrElse(R defaultValue);
    R getOrElseGet(Function<L, R> fallback);
    <X extends Throwable> R getOrThrow(Function<L, X> exceptionMapper) throws X;

    Either<L, R> ifLeft(Consumer<L> action);
    Either<L, R> ifRight(Consumer<R> action);

    <T> Either<L, T> map(Function<R, T> mapper);
    <T> Either<T, R> mapLeft(Function<L, T> mapper);
    <T> Either<L, T> flatMap(Function<R, Either<L, T>> mapper);

    <T> T fold(Function<L, T> leftMapper, Function<R, T> rightMapper);

    Either<R, L> swap();

    Optional<Either<L, R>> filter(Predicate<R> predicate);

    static <L, R> Either<L, R> left(L value) {
        return new Left<>(value);
    }

    static <L, R> Either<L, R> right(R value) {
        return new Right<>(value);
    }

    static <L, R> Either<L, R> fromOptional(Optional<R> optional, L leftValue) {
        return optional.isPresent() ? right(optional.get()) : left(leftValue);
    }

    static <L, R> Either<L, R> fromOptional(Optional<R> optional, Supplier<L> leftSupplier) {
        return optional.isPresent() ? right(optional.get()) : left(leftSupplier.get());
    }

    static <R> Either<String, R> tryCatch(Supplier<R> supplier) {
        try {
            return right(supplier.get());
        } catch (Exception e) {
            return left(e.getMessage());
        }
    }

    static <L, R> Either<L, R> tryCatch(Supplier<R> supplier, Function<Exception, L> errorMapper) {
        try {
            return right(supplier.get());
        } catch (Exception e) {
            return left(errorMapper.apply(e));
        }
    }

    /**
     * Left value (error case).
     */
    public record Left<L, R>(L value) implements Either<L, R> {
        @Override
        public boolean isLeft() { return true; }

        @Override
        public boolean isRight() { return false; }

        @Override
        public Optional<L> getLeft() { return Optional.ofNullable(value); }

        @Override
        public Optional<R> getRight() { return Optional.empty(); }

        @Override
        public R getOrElse(R defaultValue) { return defaultValue; }

        @Override
        public R getOrElseGet(Function<L, R> fallback) { return fallback.apply(value); }

        @Override
        public <X extends Throwable> R getOrThrow(Function<L, X> exceptionMapper) throws X {
            throw exceptionMapper.apply(value);
        }

        @Override
        public Either<L, R> ifLeft(Consumer<L> action) {
            action.accept(value);
            return this;
        }

        @Override
        public Either<L, R> ifRight(Consumer<R> action) {
            return this;
        }

        @Override
        public <T> Either<L, T> map(Function<R, T> mapper) {
            return new Left<>(value);
        }

        @Override
        public <T> Either<T, R> mapLeft(Function<L, T> mapper) {
            return new Left<>(mapper.apply(value));
        }

        @Override
        public <T> Either<L, T> flatMap(Function<R, Either<L, T>> mapper) {
            return new Left<>(value);
        }

        @Override
        public <T> T fold(Function<L, T> leftMapper, Function<R, T> rightMapper) {
            return leftMapper.apply(value);
        }

        @Override
        public Either<R, L> swap() {
            return new Right<>(value);
        }

        @Override
        public Optional<Either<L, R>> filter(Predicate<R> predicate) {
            return Optional.of(this);
        }
    }

    /**
     * Right value (success case).
     */
    public record Right<L, R>(R value) implements Either<L, R> {
        @Override
        public boolean isLeft() { return false; }

        @Override
        public boolean isRight() { return true; }

        @Override
        public Optional<L> getLeft() { return Optional.empty(); }

        @Override
        public Optional<R> getRight() { return Optional.ofNullable(value); }

        @Override
        public R getOrElse(R defaultValue) { return value; }

        @Override
        public R getOrElseGet(Function<L, R> fallback) { return value; }

        @Override
        public <X extends Throwable> R getOrThrow(Function<L, X> exceptionMapper) throws X {
            return value;
        }

        @Override
        public Either<L, R> ifLeft(Consumer<L> action) {
            return this;
        }

        @Override
        public Either<L, R> ifRight(Consumer<R> action) {
            action.accept(value);
            return this;
        }

        @Override
        public <T> Either<L, T> map(Function<R, T> mapper) {
            return new Right<>(mapper.apply(value));
        }

        @Override
        public <T> Either<T, R> mapLeft(Function<L, T> mapper) {
            return new Right<>(value);
        }

        @Override
        public <T> Either<L, T> flatMap(Function<R, Either<L, T>> mapper) {
            return mapper.apply(value);
        }

        @Override
        public <T> T fold(Function<L, T> leftMapper, Function<R, T> rightMapper) {
            return rightMapper.apply(value);
        }

        @Override
        public Either<R, L> swap() {
            return new Left<>(value);
        }

        @Override
        public Optional<Either<L, R>> filter(Predicate<R> predicate) {
            return predicate.test(value) ? Optional.of(this) : Optional.empty();
        }
    }

    /**
     * Either utilities.
     */
    public static final class EitherUtils {
        private EitherUtils() {}

        /**
         * Collect all right values.
         */
        public static <L, R> List<R> collectRight(Collection<Either<L, R>> eithers) {
            return eithers.stream()
                .filter(Either::isRight)
                .map(e -> e.getRight().orElse(null))
                .filter(Objects::nonNull)
                .toList();
        }

        /**
         * Collect all left values.
         */
        public static <L, R> List<L> collectLeft(Collection<Either<L, R>> eithers) {
            return eithers.stream()
                .filter(Either::isLeft)
                .map(e -> e.getLeft().orElse(null))
                .filter(Objects::nonNull)
                .toList();
        }

        /**
         * Partition eithers into lefts and rights.
         */
        public static <L, R> Map<Boolean, List<Either<L, R>>> partition(Collection<Either<L, R>> eithers) {
            return eithers.stream()
                .collect(Collectors.partitioningBy(Either::isRight));
        }

        /**
         * Sequence list of eithers.
         */
        public static <L, R> Either<L, List<R>> sequence(List<Either<L, R>> eithers) {
            List<L> lefts = collectLeft(eithers);
            if (!lefts.isEmpty()) {
                return left(lefts.get(0));
            }
            return right(collectRight(eithers));
        }

        /**
         * First left or all rights.
         */
        public static <L, R> Either<List<L>, List<R>> firstLeftOrAllRights(List<Either<L, R>> eithers) {
            List<L> lefts = collectLeft(eithers);
            if (!lefts.isEmpty()) {
                return left(lefts);
            }
            return right(collectRight(eithers));
        }

        /**
         * Map over collection.
         */
        public static <L, R, T> List<Either<L, T>> mapAll(Collection<Either<L, R>> eithers, Function<R, T> mapper) {
            return eithers.stream()
                .map(e -> e.map(mapper))
                .toList();
        }

        /**
         * Bimap - map both sides.
         */
        public static <L, R, L2, R2> Either<L2, R2> bimap(Either<L, R> either,
                Function<L, L2> leftMapper, Function<R, R2> rightMapper) {
            return either.fold(
                l -> left(leftMapper.apply(l)),
                r -> right(rightMapper.apply(r))
            );
        }
    }
}