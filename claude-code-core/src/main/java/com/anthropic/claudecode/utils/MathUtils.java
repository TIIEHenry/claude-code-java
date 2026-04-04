/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code math utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.math.*;

/**
 * Mathematical utilities.
 */
public final class MathUtils {
    private MathUtils() {}

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /**
     * Clamp value to range.
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamp value to range.
     */
    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamp value to range.
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Check if value is in range (inclusive).
     */
    public static boolean inRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Check if value is in range (exclusive).
     */
    public static boolean inRangeExclusive(int value, int min, int max) {
        return value > min && value < max;
    }

    /**
     * Linear interpolation.
     */
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * Inverse linear interpolation.
     */
    public static double inverseLerp(double a, double b, double value) {
        return (value - a) / (b - a);
    }

    /**
     * Map value from one range to another.
     */
    public static double map(double value, double inMin, double inMax, double outMin, double outMax) {
        return outMin + (value - inMin) * (outMax - outMin) / (inMax - inMin);
    }

    /**
     * Smooth step interpolation.
     */
    public static double smoothStep(double edge0, double edge1, double x) {
        double t = clamp((x - edge0) / (edge1 - edge0), 0, 1);
        return t * t * (3 - 2 * t);
    }

    /**
     * Round to decimal places.
     */
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Round up to nearest multiple.
     */
    public static int roundUp(int value, int multiple) {
        return ((value + multiple - 1) / multiple) * multiple;
    }

    /**
     * Round down to nearest multiple.
     */
    public static int roundDown(int value, int multiple) {
        return (value / multiple) * multiple;
    }

    /**
     * Check if number is power of two.
     */
    public static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

    /**
     * Next power of two.
     */
    public static int nextPowerOfTwo(int n) {
        if (n <= 0) return 1;
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        return n + 1;
    }

    /**
     * Previous power of two.
     */
    public static int previousPowerOfTwo(int n) {
        return Integer.highestOneBit(n);
    }

    /**
     * Log base 2.
     */
    public static int log2(int n) {
        if (n <= 0) throw new IllegalArgumentException();
        return 31 - Integer.numberOfLeadingZeros(n);
    }

    /**
     * Log base 2 (double).
     */
    public static double log2(double n) {
        return Math.log(n) / Math.log(2);
    }

    /**
     * Factorial.
     */
    public static long factorial(int n) {
        if (n < 0) throw new IllegalArgumentException();
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    /**
     * Factorial (BigInteger).
     */
    public static BigInteger factorialBig(int n) {
        if (n < 0) throw new IllegalArgumentException();
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    /**
     * Combinations (n choose k).
     */
    public static long combinations(int n, int k) {
        if (k < 0 || k > n) return 0;
        if (k == 0 || k == n) return 1;
        if (k > n / 2) k = n - k;
        long result = 1;
        for (int i = 0; i < k; i++) {
            result = result * (n - i) / (i + 1);
        }
        return result;
    }

    /**
     * Permutations.
     */
    public static long permutations(int n, int k) {
        if (k < 0 || k > n) return 0;
        long result = 1;
        for (int i = 0; i < k; i++) {
            result *= (n - i);
        }
        return result;
    }

    /**
     * Greatest common divisor.
     */
    public static long gcd(long a, long b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    /**
     * Least common multiple.
     */
    public static long lcm(long a, long b) {
        return Math.abs(a * b) / gcd(a, b);
    }

    /**
     * Check if relatively prime.
     */
    public static boolean isRelativelyPrime(long a, long b) {
        return gcd(a, b) == 1;
    }

    /**
     * Modulo with positive result.
     */
    public static int mod(int a, int b) {
        int result = a % b;
        return result < 0 ? result + b : result;
    }

    /**
     * Modulo with positive result.
     */
    public static long mod(long a, long b) {
        long result = a % b;
        return result < 0 ? result + b : result;
    }

    /**
     * Signed distance between two values (wrapping).
     */
    public static int signedDistance(int a, int b, int range) {
        int diff = mod(b - a, range);
        return diff > range / 2 ? diff - range : diff;
    }

    /**
     * Linear congruential generator seed.
     */
    private static long lcgSeed = System.nanoTime();

    /**
     * LCG random number.
     */
    public static long lcgNext() {
        lcgSeed = lcgSeed * 6364136223846793005L + 1442695040888963407L;
        return lcgSeed;
    }

    /**
     * Simple hash.
     */
    public static int hash(int x) {
        x = ((x >>> 16) ^ x) * 0x45d9f3b;
        x = ((x >>> 16) ^ x) * 0x45d9f3b;
        x = (x >>> 16) ^ x;
        return x;
    }

    /**
     * Combine two hashes.
     */
    public static int combineHash(int a, int b) {
        return a ^ (b + 0x9e3779b9 + (a << 6) + (a >>> 2));
    }

    /**
     * Is approximately equal.
     */
    public static boolean approxEquals(double a, double b, double epsilon) {
        return Math.abs(a - b) <= epsilon;
    }

    /**
     * Is approximately equal (relative).
     */
    public static boolean approxEqualsRelative(double a, double b, double tolerance) {
        if (a == b) return true;
        double diff = Math.abs(a - b);
        double max = Math.max(Math.abs(a), Math.abs(b));
        return diff / max <= tolerance;
    }

    /**
     * Degrees to radians.
     */
    public static double toRadians(double degrees) {
        return degrees * Math.PI / 180;
    }

    /**
     * Radians to degrees.
     */
    public static double toDegrees(double radians) {
        return radians * 180 / Math.PI;
    }

    /**
     * Normalize angle to [0, 2PI).
     */
    public static double normalizeAngle(double radians) {
        radians = radians % (2 * Math.PI);
        return radians < 0 ? radians + 2 * Math.PI : radians;
    }

    /**
     * Normalize angle to [-PI, PI).
     */
    public static double normalizeAngleCentered(double radians) {
        radians = normalizeAngle(radians);
        return radians > Math.PI ? radians - 2 * Math.PI : radians;
    }

    /**
     * Shortest angle difference.
     */
    public static double angleDifference(double a, double b) {
        double diff = normalizeAngle(b - a);
        return diff > Math.PI ? diff - 2 * Math.PI : diff;
    }

    /**
     * Linear interpolation between angles.
     */
    public static double lerpAngle(double a, double b, double t) {
        double diff = angleDifference(a, b);
        return normalizeAngle(a + diff * t);
    }

    /**
     * Percentage.
     */
    public static double percentage(double part, double whole) {
        return whole != 0 ? (part / whole) * 100 : 0;
    }

    /**
     * Percentage change.
     */
    public static double percentageChange(double oldValue, double newValue) {
        return oldValue != 0 ? ((newValue - oldValue) / Math.abs(oldValue)) * 100 : 0;
    }

    /**
     * Signum returning 1 for zero.
     */
    public static int signumZeroPositive(int value) {
        return value >= 0 ? 1 : -1;
    }

    /**
     * Signum returning 1 for zero.
     */
    public static int signumZeroPositive(double value) {
        return value >= 0 ? 1 : -1;
    }

    /**
     * Average of numbers.
     */
    public static double average(double... values) {
        if (values == null || values.length == 0) return 0;
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    /**
     * Median of numbers.
     */
    public static double median(double... values) {
        if (values == null || values.length == 0) return 0;
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int mid = sorted.length / 2;
        return sorted.length % 2 == 0 ? (sorted[mid - 1] + sorted[mid]) / 2 : sorted[mid];
    }

    /**
     * Standard deviation.
     */
    public static double standardDeviation(double... values) {
        if (values == null || values.length <= 1) return 0;
        double mean = average(values);
        double sumSquares = 0;
        for (double v : values) {
            sumSquares += (v - mean) * (v - mean);
        }
        return Math.sqrt(sumSquares / (values.length - 1));
    }

    /**
     * Variance.
     */
    public static double variance(double... values) {
        double stdDev = standardDeviation(values);
        return stdDev * stdDev;
    }

    /**
     * Percentage of total.
     */
    public static BigDecimal percentageOf(BigDecimal part, BigDecimal total, int scale) {
        if (total.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return part.multiply(HUNDRED).divide(total, scale, RoundingMode.HALF_UP);
    }

    /**
     * Sum of squares.
     */
    public static double sumOfSquares(double... values) {
        double sum = 0;
        for (double v : values) {
            sum += v * v;
        }
        return sum;
    }

    /**
     * Euclidean distance.
     */
    public static double euclideanDistance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * Manhattan distance.
     */
    public static double manhattanDistance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            sum += Math.abs(a[i] - b[i]);
        }
        return sum;
    }

    /**
     * Dot product.
     */
    public static double dotProduct(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    /**
     * Normalize vector.
     */
    public static double[] normalize(double[] vector) {
        double length = 0;
        for (double v : vector) length += v * v;
        length = Math.sqrt(length);
        if (length == 0) return vector;
        double[] result = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            result[i] = vector[i] / length;
        }
        return result;
    }
}