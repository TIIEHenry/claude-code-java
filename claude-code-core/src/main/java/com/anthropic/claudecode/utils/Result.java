/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code result utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.*;

/**
 * Result type for error handling without exceptions.
 */
public sealed interface Result<T> permits Result.Ok, Result.Err {

    boolean isOk();
    boolean isErr();
    Optional<T> getValue();
    Optional<String> getError();
    T getOrElse(T defaultValue);
    T getOrThrow() throws RuntimeException;

    /**
     * Map the success value.
     */
    <R> Result<R> map(Function<T, R> mapper);

    /**
     * Flat map the success value.
     */
    <R> Result<R> flatMap(Function<T, Result<R>> mapper);

    /**
     * Map the error.
     */
    Result<T> mapError(Function<String, String> mapper);

    /**
     * Execute action on success.
     */
    Result<T> onSuccess(Consumer<T> action);

    /**
     * Execute action on error.
     */
    Result<T> onFailure(Consumer<String> action);

    /**
     * Recover from error.
     */
    Result<T> recover(Function<String, T> recovery);

    /**
     * Ok result.
     */
    public record Ok<T>(T value) implements Result<T> {
        @Override
        public boolean isOk() { return true; }

        @Override
        public boolean isErr() { return false; }

        @Override
        public Optional<T> getValue() { return Optional.ofNullable(value); }

        @Override
        public Optional<String> getError() { return Optional.empty(); }

        @Override
        public T getOrElse(T defaultValue) { return value; }

        @Override
        public T getOrThrow() { return value; }

        @Override
        public <R> Result<R> map(Function<T, R> mapper) {
            return new Ok<>(mapper.apply(value));
        }

        @Override
        public <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
            return mapper.apply(value);
        }

        @Override
        public Result<T> mapError(Function<String, String> mapper) {
            return this;
        }

        @Override
        public Result<T> onSuccess(Consumer<T> action) {
            action.accept(value);
            return this;
        }

        @Override
        public Result<T> onFailure(Consumer<String> action) {
            return this;
        }

        @Override
        public Result<T> recover(Function<String, T> recovery) {
            return this;
        }
    }

    /**
     * Error result.
     */
    public record Err<T>(String error) implements Result<T> {
        @Override
        public boolean isOk() { return false; }

        @Override
        public boolean isErr() { return true; }

        @Override
        public Optional<T> getValue() { return Optional.empty(); }

        @Override
        public Optional<String> getError() { return Optional.ofNullable(error); }

        @Override
        public T getOrElse(T defaultValue) { return defaultValue; }

        @Override
        public T getOrThrow() {
            throw new RuntimeException(error);
        }

        @Override
        public <R> Result<R> map(Function<T, R> mapper) {
            return new Err<>(error);
        }

        @Override
        public <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
            return new Err<>(error);
        }

        @Override
        public Result<T> mapError(Function<String, String> mapper) {
            return new Err<>(mapper.apply(error));
        }

        @Override
        public Result<T> onSuccess(Consumer<T> action) {
            return this;
        }

        @Override
        public Result<T> onFailure(Consumer<String> action) {
            action.accept(error);
            return this;
        }

        @Override
        public Result<T> recover(Function<String, T> recovery) {
            return new Ok<>(recovery.apply(error));
        }
    }

    /**
     * Create an Ok result.
     */
    static <T> Result<T> ok(T value) {
        return new Ok<>(value);
    }

    /**
     * Create an Ok result with null.
     */
    static <T> Result<T> ok() {
        return new Ok<>(null);
    }

    /**
     * Create an Err result.
     */
    static <T> Result<T> err(String error) {
        return new Err<>(error);
    }

    /**
     * Create an Err result with format.
     */
    static <T> Result<T> err(String format, Object... args) {
        return new Err<>(String.format(format, args));
    }

    /**
     * Create an Err result from a list of errors.
     */
    static <T> Result<T> err(List<String> errors) {
        return new Err<>(String.join("; ", errors));
    }

    /**
     * Create from a supplier that may throw.
     */
    static <T> Result<T> from(Supplier<T> supplier) {
        try {
            return new Ok<>(supplier.get());
        } catch (Exception e) {
            return new Err<>(e.getMessage());
        }
    }

    /**
     * Create from a callable.
     */
    static <T> Result<T> fromCallable(Callable<T> callable) {
        try {
            return new Ok<>(callable.call());
        } catch (Exception e) {
            return new Err<>(e.getMessage());
        }
    }

    /**
     * Combine multiple results.
     */
    @SafeVarargs
    static <T> Result<List<T>> all(Result<T>... results) {
        return all(Arrays.asList(results));
    }

    /**
     * Combine multiple results.
     */
    static <T> Result<List<T>> all(List<Result<T>> results) {
        List<T> values = new ArrayList<>();
        for (Result<T> result : results) {
            if (result.isErr()) {
                return new Err<>(result.getError().get());
            }
            values.add(result.getValue().orElse(null));
        }
        return new Ok<>(values);
    }

    /**
     * First successful result.
     */
    @SafeVarargs
    static <T> Result<T> any(Result<T>... results) {
        return any(Arrays.asList(results));
    }

    /**
     * First successful result.
     */
    static <T> Result<T> any(List<Result<T>> results) {
        List<String> errors = new ArrayList<>();
        for (Result<T> result : results) {
            if (result.isOk()) {
                return result;
            }
            result.getError().ifPresent(errors::add);
        }
        return new Err<>("All results failed: " + String.join("; ", errors));
    }

    /**
     * Partition results into successes and failures.
     */
    static <T> Partitioned<T> partition(List<Result<T>> results) {
        List<T> successes = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (Result<T> result : results) {
            if (result.isOk()) {
                result.getValue().ifPresent(successes::add);
            } else {
                result.getError().ifPresent(failures::add);
            }
        }

        return new Partitioned<>(successes, failures);
    }

    /**
     * Partitioned results record.
     */
    record Partitioned<T>(List<T> successes, List<String> failures) {}
}