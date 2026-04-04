/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code sampler utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import java.util.stream.*;

/**
 * Sampling utilities for random selection and distribution.
 */
public final class SamplerUtils {
    private SamplerUtils() {}

    /**
     * Random sampler.
     */
    public static <T> T sample(List<T> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cannot sample from empty collection");
        }
        return items.get(ThreadLocalRandom.current().nextInt(items.size()));
    }

    /**
     * Random sampler with optional empty handling.
     */
    public static <T> Optional<T> sampleOptional(List<T> items) {
        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(items.get(ThreadLocalRandom.current().nextInt(items.size())));
    }

    /**
     * Weighted random sampler.
     */
    public static <T> T weightedSample(List<T> items, List<Double> weights) {
        if (items == null || items.isEmpty() || items.size() != weights.size()) {
            throw new IllegalArgumentException("Items and weights must have same size");
        }

        double totalWeight = weights.stream().mapToDouble(Double::doubleValue).sum();
        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;

        double cumulative = 0;
        for (int i = 0; i < items.size(); i++) {
            cumulative += weights.get(i);
            if (random <= cumulative) {
                return items.get(i);
            }
        }

        return items.get(items.size() - 1);
    }

    /**
     * Weighted sampler with weight function.
     */
    public static <T> T weightedSample(List<T> items, java.util.function.Function<T, Double> weightFunction) {
        List<Double> weights = items.stream()
            .map(weightFunction)
            .toList();
        return weightedSample(items, weights);
    }

    /**
     * Sample N items without replacement.
     */
    public static <T> List<T> sampleN(List<T> items, int n) {
        if (n >= items.size()) {
            return new ArrayList<>(items);
        }

        List<T> copy = new ArrayList<>(items);
        Collections.shuffle(copy);
        return copy.subList(0, n);
    }

    /**
     * Sample N items with replacement.
     */
    public static <T> List<T> sampleNWithReplacement(List<T> items, int n) {
        return IntStream.range(0, n)
            .mapToObj(i -> sample(items))
            .toList();
    }

    /**
     * Reservoir sampler for stream sampling.
     */
    public static <T> List<T> reservoirSample(Stream<T> stream, int k) {
        ReservoirSampler<T> sampler = new ReservoirSampler<>(k);
        stream.forEach(sampler::add);
        return sampler.sample();
    }

    /**
     * Reservoir sampler implementation.
     */
    public static final class ReservoirSampler<T> {
        private final int k;
        private final List<T> reservoir = new ArrayList<>();
        private long count = 0;
        private final RandomGenerator random = ThreadLocalRandom.current();

        public ReservoirSampler(int k) {
            this.k = k;
        }

        public void add(T item) {
            count++;
            if (reservoir.size() < k) {
                reservoir.add(item);
            } else {
                int j = random.nextInt(k);
                if (j == random.nextInt((int) count)) {
                    reservoir.set(j, item);
                }
            }
        }

        public List<T> sample() {
            return new ArrayList<>(reservoir);
        }

        public long getCount() {
            return count;
        }
    }

    /**
     * Bernoulli sampler (sample with probability p).
     */
    public static boolean bernoulli(double p) {
        return ThreadLocalRandom.current().nextDouble() < p;
    }

    /**
     * Bernoulli sampler with count.
     */
    public static int bernoulliCount(int n, double p) {
        return (int) IntStream.range(0, n)
            .filter(i -> bernoulli(p))
            .count();
    }

    /**
     * Poisson sampler.
     */
    public static int poisson(double lambda) {
        if (lambda <= 0) return 0;

        RandomGenerator random = ThreadLocalRandom.current();
        double l = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;

        do {
            k++;
            p *= random.nextDouble();
        } while (p > l);

        return k - 1;
    }

    /**
     * Geometric sampler.
     */
    public static int geometric(double p) {
        if (p <= 0 || p >= 1) {
            throw new IllegalArgumentException("p must be between 0 and 1");
        }

        RandomGenerator random = ThreadLocalRandom.current();
        int k = 0;
        while (random.nextDouble() >= p) {
            k++;
        }
        return k;
    }

    /**
     * Binomial sampler.
     */
    public static int binomial(int n, double p) {
        return (int) IntStream.range(0, n)
            .filter(i -> bernoulli(p))
            .count();
    }

    /**
     * Exponential sampler.
     */
    public static double exponential(double lambda) {
        if (lambda <= 0) {
            throw new IllegalArgumentException("lambda must be positive");
        }
        return -Math.log(ThreadLocalRandom.current().nextDouble()) / lambda;
    }

    /**
     * Uniform integer sampler.
     */
    public static int uniform(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * Uniform double sampler.
     */
    public static double uniform(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * Gaussian sampler.
     */
    public static double gaussian(double mean, double stdDev) {
        return ThreadLocalRandom.current().nextGaussian(mean, stdDev);
    }

    /**
     * Gaussian sampler (standard).
     */
    public static double gaussian() {
        return ThreadLocalRandom.current().nextGaussian();
    }

    /**
     * Log-normal sampler.
     */
    public static double logNormal(double mean, double stdDev) {
        return Math.exp(gaussian(mean, stdDev));
    }

    /**
     * Shuffle list.
     */
    public static <T> void shuffle(List<T> list) {
        Collections.shuffle(list, ThreadLocalRandom.current());
    }

    /**
     * Shuffled copy of list.
     */
    public static <T> List<T> shuffled(List<T> list) {
        List<T> copy = new ArrayList<>(list);
        shuffle(copy);
        return copy;
    }

    /**
     * Stratified sampler - sample evenly from groups.
     */
    public static <K, T> Map<K, List<T>> stratifiedSample(
            Map<K, List<T>> groups, int samplesPerGroup) {
        Map<K, List<T>> result = new HashMap<>();

        for (Map.Entry<K, List<T>> entry : groups.entrySet()) {
            List<T> groupItems = entry.getValue();
            if (groupItems.size() <= samplesPerGroup) {
                result.put(entry.getKey(), new ArrayList<>(groupItems));
            } else {
                result.put(entry.getKey(), sampleN(groupItems, samplesPerGroup));
            }
        }

        return result;
    }

    /**
     * Systematic sampler - select every nth item starting from random offset.
     */
    public static <T> List<T> systematicSample(List<T> items, int n) {
        if (n <= 0) throw new IllegalArgumentException("n must be positive");
        if (n >= items.size()) return new ArrayList<>(items);

        int start = ThreadLocalRandom.current().nextInt(n);
        List<T> result = new ArrayList<>();

        for (int i = start; i < items.size(); i += n) {
            result.add(items.get(i));
        }

        return result;
    }

    /**
     * Rejection sampler.
     */
    public static <T> List<T> rejectionSample(List<T> items, java.util.function.Predicate<T> predicate, int targetSize) {
        List<T> accepted = items.stream()
            .filter(predicate)
            .toList();

        if (accepted.size() <= targetSize) {
            return new ArrayList<>(accepted);
        }

        return sampleN(accepted, targetSize);
    }

    /**
     * Accept-reject sampler for custom distribution.
     */
    public static <T> T acceptReject(java.util.function.Supplier<T> generator,
            java.util.function.ToDoubleFunction<T> density,
            double maxDensity) {
        RandomGenerator random = ThreadLocalRandom.current();

        while (true) {
            T candidate = generator.get();
            double acceptanceProb = density.applyAsDouble(candidate) / maxDensity;

            if (random.nextDouble() < acceptanceProb) {
                return candidate;
            }
        }
    }

    /**
     * Percentile sampler.
     */
    public static <T extends Comparable<T>> T percentile(List<T> items, double percentile) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Cannot sample percentile from empty collection");
        }

        List<T> sorted = new ArrayList<>(items);
        Collections.sort(sorted);

        int index = (int) Math.floor(percentile * (sorted.size() - 1));
        return sorted.get(index);
    }

    /**
     * Median sampler.
     */
    public static <T extends Comparable<T>> T median(List<T> items) {
        return percentile(items, 0.5);
    }

    /**
     * Quartile sampler.
     */
    public static <T extends Comparable<T>> Map<String, T> quartiles(List<T> items) {
        Map<String, T> result = new HashMap<>();
        result.put("Q1", percentile(items, 0.25));
        result.put("Q2", percentile(items, 0.5));
        result.put("Q3", percentile(items, 0.75));
        return result;
    }

    /**
     * Dice roll sampler.
     */
    public static int rollDice(int sides) {
        return uniform(1, sides);
    }

    /**
     * Multiple dice roll sampler.
     */
    public static int rollDice(int numDice, int sides) {
        return IntStream.range(0, numDice)
            .map(i -> rollDice(sides))
            .sum();
    }

    /**
     * Coin flip.
     */
    public static boolean coinFlip() {
        return bernoulli(0.5);
    }

    /**
     * Multiple coin flips.
     */
    public static List<Boolean> coinFlips(int n) {
        return IntStream.range(0, n)
            .mapToObj(i -> coinFlip())
            .toList();
    }
}