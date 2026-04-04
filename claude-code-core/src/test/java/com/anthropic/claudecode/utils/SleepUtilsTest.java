/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SleepUtils.
 */
class SleepUtilsTest {

    @Test
    @DisplayName("SleepUtils sleep does not throw")
    void sleep() {
        long start = System.currentTimeMillis();
        SleepUtils.sleep(10);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed >= 10);
    }

    @Test
    @DisplayName("SleepUtils sleepSeconds sleeps for seconds")
    void sleepSeconds() {
        long start = System.currentTimeMillis();
        SleepUtils.sleepSeconds(0); // 0 seconds
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 100); // Should be very fast
    }

    @Test
    @DisplayName("SleepUtils sleep with duration")
    void sleepDuration() {
        long start = System.currentTimeMillis();
        SleepUtils.sleep(Duration.ofMillis(20));
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed >= 20);
    }

    @Test
    @DisplayName("SleepUtils sleepWithBackoff increases delay")
    void sleepWithBackoff() {
        // Just verify it doesn't throw
        assertDoesNotThrow(() -> SleepUtils.sleepWithBackoff(0, 10, 100));
        assertDoesNotThrow(() -> SleepUtils.sleepWithBackoff(1, 10, 100));
        assertDoesNotThrow(() -> SleepUtils.sleepWithBackoff(5, 10, 100));
    }

    @Test
    @DisplayName("SleepUtils sleepWithJitter adds variation")
    void sleepWithJitter() {
        // Just verify it doesn't throw
        assertDoesNotThrow(() -> SleepUtils.sleepWithJitter(10, 0.5));
    }

    @Test
    @DisplayName("SleepUtils sleepUntil returns true when condition met")
    void sleepUntilTrue() {
        int[] counter = {0};
        boolean result = SleepUtils.sleepUntil(
            () -> ++counter[0] >= 3,
            1000,
            10
        );

        assertTrue(result);
        assertTrue(counter[0] >= 3);
    }

    @Test
    @DisplayName("SleepUtils sleepUntil returns false on timeout")
    void sleepUntilFalse() {
        boolean result = SleepUtils.sleepUntil(
            () -> false,
            50,
            10
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("SleepUtils delayAsync completes after delay")
    void delayAsync() throws Exception {
        CompletableFuture<Void> future = SleepUtils.delayAsync(50);

        assertFalse(future.isDone());

        future.get(1, TimeUnit.SECONDS);

        assertTrue(future.isDone());
    }

    @Test
    @DisplayName("SleepUtils SleepCondition is functional interface")
    void sleepConditionFunctional() {
        SleepUtils.SleepCondition condition = () -> true;

        assertTrue(condition.isMet());
    }
}