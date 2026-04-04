/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code option utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Enhanced Option type for better null handling.
 */
public sealed interface Option<T> extends Iterable<T> permits
    Option.Some, Option.None {

    boolean isPresent();
    boolean isEmpty();
    T get();
    T getOrElse(T defaultValue);
    T getOrElseGet(Supplier<T> supplier);
    T getOrThrow(Supplier<RuntimeException> exceptionSupplier);
    Optional<T> toOptional();

    /**
     * Map the value if present.
     */
    <R> Option<R> map(Function<T, R> mapper);

    /**
     * Flat map the value if present.
     */
    <R> Option<R> flatMap(Function<T, Option<R>> mapper);

    /**
     * Filter the value.
     */
    Option<T> filter(Predicate<T> predicate);

    /**
     * Execute action if present.
     */
    Option<T> ifPresent(Consumer<T> action);

    /**
     * Execute action if empty.
     */
    Option<T> ifEmpty(Runnable action);

    /**
     * Execute action in either case.
     */
    Option<T> either(Consumer<T> ifPresent, Runnable ifEmpty);

    /**
     * Convert to stream.
     */
    java.util.stream.Stream<T> stream();

    /**
     * Some value.
     */
    public record Some<T>(T value) implements Option<T> {
        @Override
        public boolean isPresent() { return true; }

        @Override
        public boolean isEmpty() { return false; }

        @Override
        public T get() { return value; }

        @Override
        public T getOrElse(T defaultValue) { return value; }

        @Override
        public T getOrElseGet(Supplier<T> supplier) { return value; }

        @Override
        public T getOrThrow(Supplier<RuntimeException> exceptionSupplier) { return value; }

        @Override
        public Optional<T> toOptional() { return Optional.ofNullable(value); }

        @Override
        public <R> Option<R> map(Function<T, R> mapper) {
            return new Some<>(mapper.apply(value));
        }

        @Override
        public <R> Option<R> flatMap(Function<T, Option<R>> mapper) {
            return mapper.apply(value);
        }

        @Override
        public Option<T> filter(Predicate<T> predicate) {
            return predicate.test(value) ? this : new None<>();
        }

        @Override
        public Option<T> ifPresent(Consumer<T> action) {
            action.accept(value);
            return this;
        }

        @Override
        public Option<T> ifEmpty(Runnable action) {
            return this;
        }

        @Override
        public Option<T> either(Consumer<T> ifPresent, Runnable ifEmpty) {
            ifPresent.accept(value);
            return this;
        }

        @Override
        public java.util.stream.Stream<T> stream() {
            return java.util.stream.Stream.of(value);
        }

        @Override
        public Iterator<T> iterator() {
            return java.util.stream.Stream.of(value).iterator();
        }
    }

    /**
     * No value.
     */
    public final class None<T> implements Option<T> {
        @Override
        public boolean isPresent() { return false; }

        @Override
        public boolean isEmpty() { return true; }

        @Override
        public T get() {
            throw new NoSuchElementException("Option is empty");
        }

        @Override
        public T getOrElse(T defaultValue) { return defaultValue; }

        @Override
        public T getOrElseGet(Supplier<T> supplier) { return supplier.get(); }

        @Override
        public T getOrThrow(Supplier<RuntimeException> exceptionSupplier) {
            throw exceptionSupplier.get();
        }

        @Override
        public Optional<T> toOptional() { return Optional.empty(); }

        @Override
        public <R> Option<R> map(Function<T, R> mapper) {
            return new None<>();
        }

        @Override
        public <R> Option<R> flatMap(Function<T, Option<R>> mapper) {
            return new None<>();
        }

        @Override
        public Option<T> filter(Predicate<T> predicate) {
            return this;
        }

        @Override
        public Option<T> ifPresent(Consumer<T> action) {
            return this;
        }

        @Override
        public Option<T> ifEmpty(Runnable action) {
            action.run();
            return this;
        }

        @Override
        public Option<T> either(Consumer<T> ifPresent, Runnable ifEmpty) {
            ifEmpty.run();
            return this;
        }

        @Override
        public java.util.stream.Stream<T> stream() {
            return java.util.stream.Stream.empty();
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof None;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "None";
        }
    }

    /**
     * Create a Some value.
     */
    static <T> Option<T> some(T value) {
        return new Some<>(value);
    }

    /**
     * Create a None value.
     */
    static <T> Option<T> none() {
        return new None<>();
    }

    /**
     * Create from nullable value.
     */
    static <T> Option<T> of(T value) {
        return value != null ? new Some<>(value) : new None<>();
    }

    /**
     * Create from Optional.
     */
    static <T> Option<T> fromOptional(Optional<T> optional) {
        return optional.map(Option::some).orElseGet(Option::none);
    }

    /**
     * Combine two options.
     */
    static <T, U, R> Option<R> combine(Option<T> opt1, Option<U> opt2, BiFunction<T, U, R> combiner) {
        if (opt1.isPresent() && opt2.isPresent()) {
            return some(combiner.apply(opt1.get(), opt2.get()));
        }
        return none();
    }

    /**
     * First non-empty option.
     */
    @SafeVarargs
    static <T> Option<T> firstOf(Option<T>... options) {
        for (Option<T> opt : options) {
            if (opt.isPresent()) {
                return opt;
            }
        }
        return none();
    }

    /**
     * All non-empty values.
     */
    @SafeVarargs
    static <T> List<T> valuesOf(Option<T>... options) {
        return Arrays.stream(options)
            .filter(Option::isPresent)
            .map(Option::get)
            .toList();
    }
}