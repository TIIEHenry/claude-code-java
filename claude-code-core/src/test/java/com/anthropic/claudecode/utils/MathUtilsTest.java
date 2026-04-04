/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MathUtils.
 */
class MathUtilsTest {

    @Test
    @DisplayName("MathUtils clamp limits int value")
    void clampInt() {
        assertEquals(5, MathUtils.clamp(5, 0, 10));
        assertEquals(0, MathUtils.clamp(-5, 0, 10));
        assertEquals(10, MathUtils.clamp(15, 0, 10));
    }

    @Test
    @DisplayName("MathUtils clamp limits long value")
    void clampLong() {
        assertEquals(5L, MathUtils.clamp(5L, 0L, 10L));
        assertEquals(0L, MathUtils.clamp(-5L, 0L, 10L));
        assertEquals(10L, MathUtils.clamp(15L, 0L, 10L));
    }

    @Test
    @DisplayName("MathUtils clamp limits double value")
    void clampDouble() {
        assertEquals(5.0, MathUtils.clamp(5.0, 0.0, 10.0), 0.001);
        assertEquals(0.0, MathUtils.clamp(-5.0, 0.0, 10.0), 0.001);
        assertEquals(10.0, MathUtils.clamp(15.0, 0.0, 10.0), 0.001);
    }

    @Test
    @DisplayName("MathUtils inRange checks inclusive range")
    void inRangeWorks() {
        assertTrue(MathUtils.inRange(5, 0, 10));
        assertTrue(MathUtils.inRange(0, 0, 10));
        assertTrue(MathUtils.inRange(10, 0, 10));
        assertFalse(MathUtils.inRange(-1, 0, 10));
        assertFalse(MathUtils.inRange(11, 0, 10));
    }

    @Test
    @DisplayName("MathUtils inRangeExclusive checks exclusive range")
    void inRangeExclusiveWorks() {
        assertTrue(MathUtils.inRangeExclusive(5, 0, 10));
        assertFalse(MathUtils.inRangeExclusive(0, 0, 10));
        assertFalse(MathUtils.inRangeExclusive(10, 0, 10));
    }

    @Test
    @DisplayName("MathUtils lerp interpolates")
    void lerpWorks() {
        assertEquals(0.0, MathUtils.lerp(0, 10, 0), 0.001);
        assertEquals(5.0, MathUtils.lerp(0, 10, 0.5), 0.001);
        assertEquals(10.0, MathUtils.lerp(0, 10, 1), 0.001);
    }

    @Test
    @DisplayName("MathUtils inverseLerp inverts interpolation")
    void inverseLerpWorks() {
        assertEquals(0.0, MathUtils.inverseLerp(0, 10, 0), 0.001);
        assertEquals(0.5, MathUtils.inverseLerp(0, 10, 5), 0.001);
        assertEquals(1.0, MathUtils.inverseLerp(0, 10, 10), 0.001);
    }

    @Test
    @DisplayName("MathUtils map maps between ranges")
    void mapWorks() {
        assertEquals(50.0, MathUtils.map(5, 0, 10, 0, 100), 0.001);
    }

    @Test
    @DisplayName("MathUtils smoothStep interpolates smoothly")
    void smoothStepWorks() {
        assertEquals(0.0, MathUtils.smoothStep(0, 10, 0), 0.001);
        assertEquals(1.0, MathUtils.smoothStep(0, 10, 10), 0.001);
        assertTrue(MathUtils.smoothStep(0, 10, 5) > 0 && MathUtils.smoothStep(0, 10, 5) < 1);
    }

    @Test
    @DisplayName("MathUtils round rounds to places")
    void roundWorks() {
        assertEquals(3.14, MathUtils.round(3.14159, 2), 0.001);
        assertEquals(3.142, MathUtils.round(3.14159, 3), 0.001);
    }

    @Test
    @DisplayName("MathUtils roundUp rounds up to multiple")
    void roundUpWorks() {
        assertEquals(10, MathUtils.roundUp(7, 5));
        assertEquals(10, MathUtils.roundUp(10, 5));
    }

    @Test
    @DisplayName("MathUtils roundDown rounds down to multiple")
    void roundDownWorks() {
        assertEquals(5, MathUtils.roundDown(7, 5));
        assertEquals(10, MathUtils.roundDown(10, 5));
    }

    @Test
    @DisplayName("MathUtils isPowerOfTwo checks power of two")
    void isPowerOfTwoWorks() {
        assertTrue(MathUtils.isPowerOfTwo(1));
        assertTrue(MathUtils.isPowerOfTwo(2));
        assertTrue(MathUtils.isPowerOfTwo(4));
        assertTrue(MathUtils.isPowerOfTwo(1024));
        assertFalse(MathUtils.isPowerOfTwo(0));
        assertFalse(MathUtils.isPowerOfTwo(3));
        assertFalse(MathUtils.isPowerOfTwo(-2));
    }

    @Test
    @DisplayName("MathUtils nextPowerOfTwo returns next power")
    void nextPowerOfTwoWorks() {
        assertEquals(1, MathUtils.nextPowerOfTwo(0));
        assertEquals(1, MathUtils.nextPowerOfTwo(1));
        assertEquals(2, MathUtils.nextPowerOfTwo(2));
        assertEquals(4, MathUtils.nextPowerOfTwo(3));
        assertEquals(1024, MathUtils.nextPowerOfTwo(1000));
    }

    @Test
    @DisplayName("MathUtils previousPowerOfTwo returns previous power")
    void previousPowerOfTwoWorks() {
        assertEquals(512, MathUtils.previousPowerOfTwo(1000));
        assertEquals(4, MathUtils.previousPowerOfTwo(5));
    }

    @Test
    @DisplayName("MathUtils log2 returns log base 2")
    void log2Works() {
        assertEquals(0, MathUtils.log2(1));
        assertEquals(1, MathUtils.log2(2));
        assertEquals(2, MathUtils.log2(4));
        assertEquals(10, MathUtils.log2(1024));
    }

    @Test
    @DisplayName("MathUtils factorial computes factorial")
    void factorialWorks() {
        assertEquals(1, MathUtils.factorial(0));
        assertEquals(1, MathUtils.factorial(1));
        assertEquals(2, MathUtils.factorial(2));
        assertEquals(6, MathUtils.factorial(3));
        assertEquals(24, MathUtils.factorial(4));
        assertEquals(120, MathUtils.factorial(5));
    }

    @Test
    @DisplayName("MathUtils combinations computes n choose k")
    void combinationsWorks() {
        assertEquals(1, MathUtils.combinations(5, 0));
        assertEquals(5, MathUtils.combinations(5, 1));
        assertEquals(10, MathUtils.combinations(5, 2));
        assertEquals(1, MathUtils.combinations(5, 5));
    }

    @Test
    @DisplayName("MathUtils permutations computes permutations")
    void permutationsWorks() {
        assertEquals(1, MathUtils.permutations(5, 0));
        assertEquals(5, MathUtils.permutations(5, 1));
        assertEquals(20, MathUtils.permutations(5, 2));
    }

    @Test
    @DisplayName("MathUtils gcd computes greatest common divisor")
    void gcdWorks() {
        assertEquals(6, MathUtils.gcd(12, 18));
        assertEquals(1, MathUtils.gcd(7, 11));
        assertEquals(5, MathUtils.gcd(25, 15));
    }

    @Test
    @DisplayName("MathUtils lcm computes least common multiple")
    void lcmWorks() {
        assertEquals(36, MathUtils.lcm(12, 18));
        assertEquals(77, MathUtils.lcm(7, 11));
    }

    @Test
    @DisplayName("MathUtils isRelativelyPrime checks relative primality")
    void isRelativelyPrimeWorks() {
        assertTrue(MathUtils.isRelativelyPrime(7, 11));
        assertFalse(MathUtils.isRelativelyPrime(12, 18));
    }

    @Test
    @DisplayName("MathUtils mod returns positive modulo")
    void modWorks() {
        assertEquals(2, MathUtils.mod(-3, 5));
        assertEquals(0, MathUtils.mod(5, 5));
        assertEquals(3, MathUtils.mod(3, 5));
    }

    @Test
    @DisplayName("MathUtils hash hashes integers")
    void hashWorks() {
        int h1 = MathUtils.hash(123);
        int h2 = MathUtils.hash(123);
        assertEquals(h1, h2);
        assertNotEquals(h1, MathUtils.hash(124));
    }

    @Test
    @DisplayName("MathUtils combineHash combines hashes")
    void combineHashWorks() {
        int combined = MathUtils.combineHash(123, 456);
        assertNotNull(combined);
    }

    @Test
    @DisplayName("MathUtils approxEquals checks approximate equality")
    void approxEqualsWorks() {
        assertTrue(MathUtils.approxEquals(1.0, 1.001, 0.01));
        assertFalse(MathUtils.approxEquals(1.0, 1.1, 0.01));
    }

    @Test
    @DisplayName("MathUtils toRadians converts degrees")
    void toRadiansWorks() {
        assertEquals(0.0, MathUtils.toRadians(0), 0.001);
        assertEquals(Math.PI / 2, MathUtils.toRadians(90), 0.001);
        assertEquals(Math.PI, MathUtils.toRadians(180), 0.001);
    }

    @Test
    @DisplayName("MathUtils toDegrees converts radians")
    void toDegreesWorks() {
        assertEquals(0.0, MathUtils.toDegrees(0), 0.001);
        assertEquals(90.0, MathUtils.toDegrees(Math.PI / 2), 0.001);
        assertEquals(180.0, MathUtils.toDegrees(Math.PI), 0.001);
    }

    @Test
    @DisplayName("MathUtils normalizeAngle normalizes to [0, 2PI)")
    void normalizeAngleWorks() {
        assertEquals(0.0, MathUtils.normalizeAngle(0), 0.001);
        assertEquals(0.0, MathUtils.normalizeAngle(2 * Math.PI), 0.001);
        assertEquals(Math.PI, MathUtils.normalizeAngle(-Math.PI), 0.001);
    }

    @Test
    @DisplayName("MathUtils average computes average")
    void averageWorks() {
        assertEquals(2.0, MathUtils.average(1, 2, 3), 0.001);
        assertEquals(0.0, MathUtils.average());
        assertEquals(0.0, MathUtils.average(null));
    }

    @Test
    @DisplayName("MathUtils median computes median")
    void medianWorks() {
        assertEquals(2.0, MathUtils.median(1, 2, 3), 0.001);
        assertEquals(2.5, MathUtils.median(1, 2, 3, 4), 0.001);
    }

    @Test
    @DisplayName("MathUtils standardDeviation computes std dev")
    void standardDeviationWorks() {
        double stdDev = MathUtils.standardDeviation(2, 4, 4, 4, 5, 5, 7, 9);
        assertTrue(stdDev > 0);
    }

    @Test
    @DisplayName("MathUtils euclideanDistance computes distance")
    void euclideanDistanceWorks() {
        double dist = MathUtils.euclideanDistance(new double[]{0, 0}, new double[]{3, 4});
        assertEquals(5.0, dist, 0.001);
    }

    @Test
    @DisplayName("MathUtils manhattanDistance computes distance")
    void manhattanDistanceWorks() {
        double dist = MathUtils.manhattanDistance(new double[]{0, 0}, new double[]{3, 4});
        assertEquals(7.0, dist, 0.001);
    }

    @Test
    @DisplayName("MathUtils dotProduct computes dot product")
    void dotProductWorks() {
        double dot = MathUtils.dotProduct(new double[]{1, 2, 3}, new double[]{4, 5, 6});
        assertEquals(32.0, dot, 0.001);
    }

    @Test
    @DisplayName("MathUtils normalize normalizes vector")
    void normalizeWorks() {
        double[] normalized = MathUtils.normalize(new double[]{3, 4});
        assertEquals(0.6, normalized[0], 0.001);
        assertEquals(0.8, normalized[1], 0.001);
    }

    @Test
    @DisplayName("MathUtils percentage calculates percentage")
    void percentageWorks() {
        assertEquals(50.0, MathUtils.percentage(50, 100), 0.001);
    }
}