/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code random utilities
 */
package com.anthropic.claudecode.utils;

import java.time.Instant;

import java.util.*;
import java.time.Instant;

import java.util.concurrent.ThreadLocalRandom;
import java.time.Instant;

import java.security.*;

/**
 * Random generation utilities.
 */
public final class RandomUtils {
    private RandomUtils() {}

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String ALPHABETIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String NUMERIC = "0123456789";
    private static final String HEX = "0123456789abcdef";

    /**
     * Get thread-local random.
     */
    public static ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }

    /**
     * Get random int.
     */
    public static int nextInt() {
        return random().nextInt();
    }

    /**
     * Get random int in range [0, bound).
     */
    public static int nextInt(int bound) {
        return random().nextInt(bound);
    }

    /**
     * Get random int in range [min, max].
     */
    public static int nextInt(int min, int max) {
        return random().nextInt(min, max + 1);
    }

    /**
     * Get random long.
     */
    public static long nextLong() {
        return random().nextLong();
    }

    /**
     * Get random long in range [min, max].
     */
    public static long nextLong(long min, long max) {
        return random().nextLong(min, max + 1);
    }

    /**
     * Get random double.
     */
    public static double nextDouble() {
        return random().nextDouble();
    }

    /**
     * Get random double in range [min, max).
     */
    public static double nextDouble(double min, double max) {
        return min + random().nextDouble() * (max - min);
    }

    /**
     * Get random boolean.
     */
    public static boolean nextBoolean() {
        return random().nextBoolean();
    }

    /**
     * Get random boolean with probability.
     */
    public static boolean nextBoolean(double probability) {
        return random().nextDouble() < probability;
    }

    /**
     * Generate random string of given length.
     */
    public static String randomString(int length) {
        return randomString(length, ALPHANUMERIC);
    }

    /**
     * Generate random string with given characters.
     */
    public static String randomString(int length, String chars) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random().nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Generate random alphanumeric string.
     */
    public static String randomAlphanumeric(int length) {
        return randomString(length, ALPHANUMERIC);
    }

    /**
     * Generate random alphabetic string.
     */
    public static String randomAlphabetic(int length) {
        return randomString(length, ALPHABETIC);
    }

    /**
     * Generate random numeric string.
     */
    public static String randomNumeric(int length) {
        return randomString(length, NUMERIC);
    }

    /**
     * Generate random hex string.
     */
    public static String randomHex(int length) {
        return randomString(length, HEX);
    }

    /**
     * Generate UUID string.
     */
    public static String randomUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate short UUID (without dashes).
     */
    public static String randomShortUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Generate secure random string.
     */
    public static String secureRandomString(int length) {
        return secureRandomString(length, ALPHANUMERIC);
    }

    /**
     * Generate secure random string with given characters.
     */
    public static String secureRandomString(int length, String chars) {
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Generate secure random bytes.
     */
    public static byte[] secureRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    /**
     * Pick random element from array.
     */
    public static <T> T randomElement(T[] array) {
        if (array == null || array.length == 0) return null;
        return array[random().nextInt(array.length)];
    }

    /**
     * Pick random element from list.
     */
    public static <T> T randomElement(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(random().nextInt(list.size()));
    }

    /**
     * Pick random element from collection.
     */
    public static <T> T randomElement(Collection<T> collection) {
        if (collection == null || collection.isEmpty()) return null;
        int index = random().nextInt(collection.size());
        int i = 0;
        for (T element : collection) {
            if (i++ == index) return element;
        }
        return null;
    }

    /**
     * Pick N random elements from list (with replacement).
     */
    public static <T> List<T> randomElements(List<T> list, int n) {
        if (list == null || list.isEmpty()) return List.of();
        List<T> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(randomElement(list));
        }
        return result;
    }

    /**
     * Pick N random elements from list (without replacement).
     */
    public static <T> List<T> randomDistinctElements(List<T> list, int n) {
        if (list == null || list.isEmpty()) return List.of();
        n = Math.min(n, list.size());
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        return copy.subList(0, n);
    }

    /**
     * Shuffle list in place.
     */
    public static <T> void shuffle(List<T> list) {
        Collections.shuffle(list, random());
    }

    /**
     * Return shuffled copy of list.
     */
    public static <T> List<T> shuffled(List<T> list) {
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy, random());
        return copy;
    }

    /**
     * Shuffle array in place.
     */
    public static <T> void shuffle(T[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int j = random().nextInt(i + 1);
            T temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    /**
     * Generate random delay with jitter.
     */
    public static long jitter(long baseMs, double jitterFactor) {
        double jitter = baseMs * jitterFactor;
        return (long) (baseMs + (random().nextDouble() * 2 - 1) * jitter);
    }

    /**
     * Generate random delay with exponential backoff and jitter.
     */
    public static long exponentialBackoff(int attempt, long baseMs, long maxMs, double jitterFactor) {
        long delay = (long) (baseMs * Math.pow(2, attempt));
        delay = Math.min(delay, maxMs);
        return jitter(delay, jitterFactor);
    }

    /**
     * Generate random sample from normal distribution.
     */
    public static double nextGaussian(double mean, double stdDev) {
        return mean + random().nextGaussian() * stdDev;
    }

    /**
     * Generate random sample from exponential distribution.
     */
    public static double nextExponential(double lambda) {
        return -Math.log(1 - random().nextDouble()) / lambda;
    }

    /**
     * Generate random index with weights.
     */
    public static int weightedRandom(double[] weights) {
        double total = 0;
        for (double w : weights) total += w;

        double r = random().nextDouble() * total;
        double sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += weights[i];
            if (r <= sum) return i;
        }
        return weights.length - 1;
    }

    /**
     * Generate random enum value.
     */
    public static <T extends Enum<T>> T randomEnum(Class<T> enumClass) {
        T[] values = enumClass.getEnumConstants();
        return randomElement(values);
    }

    /**
     * Generate random timestamp within range.
     */
    public static long randomTimestamp(long minMs, long maxMs) {
        return nextLong(minMs, maxMs);
    }

    /**
     * Generate random date within range.
     */
    public static java.time.Instant randomInstant(java.time.Instant min, java.time.Instant max) {
        long minMs = min.toEpochMilli();
        long maxMs = max.toEpochMilli();
        return java.time.Instant.ofEpochMilli(nextLong(minMs, maxMs));
    }

    /**
     * Generate random color as hex string.
     */
    public static String randomColor() {
        return String.format("#%06x", nextInt(0, 0xFFFFFF));
    }

    /**
     * Generate random IPv4 address.
     */
    public static String randomIpv4() {
        return nextInt(1, 255) + "." + nextInt(0, 255) + "." + nextInt(0, 255) + "." + nextInt(0, 255);
    }

    /**
     * Generate random port number.
     */
    public static int randomPort() {
        return nextInt(1024, 65535);
    }

    /**
     * Generate random MAC address.
     */
    public static String randomMacAddress() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i > 0) sb.append(":");
            sb.append(String.format("%02x", nextInt(0, 255)));
        }
        return sb.toString();
    }

    /**
     * Generate random user agent string.
     */
    public static String randomUserAgent() {
        String[] browsers = {"Chrome", "Firefox", "Safari", "Edge"};
        String[] oses = {"Windows", "Macintosh", "Linux", "Android", "iOS"};
        return randomElement(browsers) + "/" + nextInt(80, 120) + " (" + randomElement(oses) + ")";
    }
}