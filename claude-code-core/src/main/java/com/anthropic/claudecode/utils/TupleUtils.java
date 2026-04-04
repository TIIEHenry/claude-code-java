/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tuple utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;

/**
 * Tuple types for multiple return values.
 */
public final class TupleUtils {
    private TupleUtils() {}

    /**
     * Pair (2-tuple).
     */
    public record Pair<A, B>(A first, B second) {
        public static <A, B> Pair<A, B> of(A first, B second) {
            return new Pair<>(first, second);
        }

        public <R> R apply(BiFunction<A, B, R> f) {
            return f.apply(first, second);
        }

        public void accept(BiConsumer<A, B> f) {
            f.accept(first, second);
        }

        public <C, D> Pair<C, D> map(Function<A, C> f1, Function<B, D> f2) {
            return new Pair<>(f1.apply(first), f2.apply(second));
        }

        public <C> Pair<C, B> mapFirst(Function<A, C> f) {
            return new Pair<>(f.apply(first), second);
        }

        public <D> Pair<A, D> mapSecond(Function<B, D> f) {
            return new Pair<>(first, f.apply(second));
        }

        public Pair<B, A> swap() {
            return new Pair<>(second, first);
        }

        public List<Object> toList() {
            return List.of(first, second);
        }
    }

    /**
     * Triple (3-tuple).
     */
    public record Triple<A, B, C>(A first, B second, C third) {
        public static <A, B, C> Triple<A, B, C> of(A first, B second, C third) {
            return new Triple<>(first, second, third);
        }

        public <R> R apply(TriFunction<A, B, C, R> f) {
            return f.apply(first, second, third);
        }

        public void accept(TriConsumer<A, B, C> f) {
            f.accept(first, second, third);
        }

        public <D, E, F> Triple<D, E, F> map(Function<A, D> f1, Function<B, E> f2, Function<C, F> f3) {
            return new Triple<>(f1.apply(first), f2.apply(second), f3.apply(third));
        }

        public List<Object> toList() {
            return List.of(first, second, third);
        }

        public Pair<A, B> dropThird() {
            return new Pair<>(first, second);
        }

        public Pair<B, C> dropFirst() {
            return new Pair<>(second, third);
        }

        public Pair<A, C> dropSecond() {
            return new Pair<>(first, third);
        }
    }

    /**
     * Quadruple (4-tuple).
     */
    public record Quad<A, B, C, D>(A first, B second, C third, D fourth) {
        public static <A, B, C, D> Quad<A, B, C, D> of(A first, B second, C third, D fourth) {
            return new Quad<>(first, second, third, fourth);
        }

        public List<Object> toList() {
            return List.of(first, second, third, fourth);
        }

        public Triple<A, B, C> dropFourth() {
            return new Triple<>(first, second, third);
        }
    }

    /**
     * Quintuple (5-tuple).
     */
    public record Quint<A, B, C, D, E>(A first, B second, C third, D fourth, E fifth) {
        public static <A, B, C, D, E> Quint<A, B, C, D, E> of(A first, B second, C third, D fourth, E fifth) {
            return new Quint<>(first, second, third, fourth, fifth);
        }

        public List<Object> toList() {
            return List.of(first, second, third, fourth, fifth);
        }
    }

    /**
     * Tri-function interface.
     */
    @FunctionalInterface
    public interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    /**
     * Tri-consumer interface.
     */
    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    /**
     * Create a pair.
     */
    public static <A, B> Pair<A, B> pair(A first, B second) {
        return new Pair<>(first, second);
    }

    /**
     * Create a triple.
     */
    public static <A, B, C> Triple<A, B, C> triple(A first, B second, C third) {
        return new Triple<>(first, second, third);
    }

    /**
     * Create a quad.
     */
    public static <A, B, C, D> Quad<A, B, C, D> quad(A first, B second, C third, D fourth) {
        return new Quad<>(first, second, third, fourth);
    }

    /**
     * Create a quint.
     */
    public static <A, B, C, D, E> Quint<A, B, C, D, E> quint(A first, B second, C third, D fourth, E fifth) {
        return new Quint<>(first, second, third, fourth, fifth);
    }

    /**
     * Zip lists into pairs.
     */
    public static <A, B> List<Pair<A, B>> zip(List<A> listA, List<B> listB) {
        int size = Math.min(listA.size(), listB.size());
        List<Pair<A, B>> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(new Pair<>(listA.get(i), listB.get(i)));
        }
        return result;
    }

    /**
     * Zip lists into triples.
     */
    public static <A, B, C> List<Triple<A, B, C>> zip(List<A> listA, List<B> listB, List<C> listC) {
        int size = Math.min(listA.size(), Math.min(listB.size(), listC.size()));
        List<Triple<A, B, C>> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(new Triple<>(listA.get(i), listB.get(i), listC.get(i)));
        }
        return result;
    }

    /**
     * Unzip pairs into two lists.
     */
    public static <A, B> Pair<List<A>, List<B>> unzip(List<Pair<A, B>> pairs) {
        List<A> listA = new ArrayList<>();
        List<B> listB = new ArrayList<>();
        for (Pair<A, B> pair : pairs) {
            listA.add(pair.first());
            listB.add(pair.second());
        }
        return new Pair<>(listA, listB);
    }

    /**
     * Unzip triples into three lists.
     */
    public static <A, B, C> Triple<List<A>, List<B>, List<C>> unzipTriples(List<Triple<A, B, C>> triples) {
        List<A> listA = new ArrayList<>();
        List<B> listB = new ArrayList<>();
        List<C> listC = new ArrayList<>();
        for (Triple<A, B, C> triple : triples) {
            listA.add(triple.first());
            listB.add(triple.second());
            listC.add(triple.third());
        }
        return new Triple<>(listA, listB, listC);
    }

    /**
     * Map over pair.
     */
    public static <A, B, C, D> Pair<C, D> map(Pair<A, B> pair, Function<A, C> f1, Function<B, D> f2) {
        return pair.map(f1, f2);
    }

    /**
     * Map over triple.
     */
    public static <A, B, C, D, E, F> Triple<D, E, F> map(
            Triple<A, B, C> triple, Function<A, D> f1, Function<B, E> f2, Function<C, F> f3) {
        return triple.map(f1, f2, f3);
    }

    /**
     * Fold over pair.
     */
    public static <A, B, R> R fold(Pair<A, B> pair, R initial, BiFunction<R, A, R> f1, BiFunction<R, B, R> f2) {
        return f2.apply(f1.apply(initial, pair.first()), pair.second());
    }

    /**
     * Pair with index.
     */
    public static <T> List<Pair<T, Integer>> indexed(List<T> list) {
        List<Pair<T, Integer>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            result.add(new Pair<>(list.get(i), i));
        }
        return result;
    }

    /**
     * Pair with previous element.
     */
    public static <T> List<Pair<T, T>> withPrevious(List<T> list) {
        List<Pair<T, T>> result = new ArrayList<>();
        for (int i = 1; i < list.size(); i++) {
            result.add(new Pair<>(list.get(i - 1), list.get(i)));
        }
        return result;
    }

    /**
     * Pair with next element.
     */
    public static <T> List<Pair<T, T>> withNext(List<T> list) {
        List<Pair<T, T>> result = new ArrayList<>();
        for (int i = 0; i < list.size() - 1; i++) {
            result.add(new Pair<>(list.get(i), list.get(i + 1)));
        }
        return result;
    }

    /**
     * Adjacent pairs (with wraparound).
     */
    public static <T> List<Pair<T, T>> adjacentPairs(List<T> list) {
        List<Pair<T, T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            result.add(new Pair<>(list.get(i), list.get((i + 1) % list.size())));
        }
        return result;
    }

    /**
     * Cartesian product of two lists.
     */
    public static <A, B> List<Pair<A, B>> cartesianProduct(List<A> listA, List<B> listB) {
        List<Pair<A, B>> result = new ArrayList<>();
        for (A a : listA) {
            for (B b : listB) {
                result.add(new Pair<>(a, b));
            }
        }
        return result;
    }

    /**
     * Cartesian product of three lists.
     */
    public static <A, B, C> List<Triple<A, B, C>> cartesianProduct(
            List<A> listA, List<B> listB, List<C> listC) {
        List<Triple<A, B, C>> result = new ArrayList<>();
        for (A a : listA) {
            for (B b : listB) {
                for (C c : listC) {
                    result.add(new Triple<>(a, b, c));
                }
            }
        }
        return result;
    }

    /**
     * Nested tuple extraction.
     */
    public static <A, B, C> Triple<A, B, C> flatten(Pair<A, Pair<B, C>> nested) {
        return new Triple<>(nested.first(), nested.second().first(), nested.second().second());
    }

    /**
     * Nested tuple construction.
     */
    public static <A, B, C> Pair<A, Pair<B, C>> nest(Triple<A, B, C> triple) {
        return new Pair<>(triple.first(), new Pair<>(triple.second(), triple.third()));
    }
}