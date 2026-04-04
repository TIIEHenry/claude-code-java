/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AtomicUtils.
 */
class AtomicUtilsTest {

    @Test
    @DisplayName("AtomicUtils create creates AtomicReference")
    void createWorks() {
        AtomicReference<String> ref = AtomicUtils.create("test");

        assertEquals("test", ref.get());
    }

    @Test
    @DisplayName("AtomicUtils createInt creates AtomicInteger")
    void createIntWorks() {
        AtomicInteger i = AtomicUtils.createInt(5);

        assertEquals(5, i.get());
    }

    @Test
    @DisplayName("AtomicUtils createLong creates AtomicLong")
    void createLongWorks() {
        AtomicLong l = AtomicUtils.createLong(10L);

        assertEquals(10L, l.get());
    }

    @Test
    @DisplayName("AtomicUtils createBoolean creates AtomicBoolean")
    void createBooleanWorks() {
        AtomicBoolean b = AtomicUtils.createBoolean(true);

        assertTrue(b.get());
    }

    @Test
    @DisplayName("AtomicUtils updateAndGet updates")
    void updateAndGetWorks() {
        AtomicReference<Integer> ref = new AtomicReference<>(5);

        int result = AtomicUtils.updateAndGet(ref, n -> n * 2);

        assertEquals(10, result);
    }

    @Test
    @DisplayName("AtomicUtils getAndUpdate returns old value")
    void getAndUpdateWorks() {
        AtomicReference<Integer> ref = new AtomicReference<>(5);

        int old = AtomicUtils.getAndUpdate(ref, n -> n * 2);

        assertEquals(5, old);
        assertEquals(10, ref.get());
    }

    @Test
    @DisplayName("AtomicUtils accumulateAndGet accumulates")
    void accumulateAndGetWorks() {
        AtomicReference<Integer> ref = new AtomicReference<>(5);

        int result = AtomicUtils.accumulateAndGet(ref, 3, Integer::sum);

        assertEquals(8, result);
    }

    @Test
    @DisplayName("AtomicUtils compareAndSetWithRetry succeeds")
    void compareAndSetWithRetrySuccess() {
        AtomicReference<Integer> ref = new AtomicReference<>(5);

        boolean result = AtomicUtils.compareAndSetWithRetry(
            ref,
            n -> n == 5,
            n -> n * 2,
            3
        );

        assertTrue(result);
        assertEquals(10, ref.get());
    }

    @Test
    @DisplayName("AtomicUtils compareAndSetWithRetry fails when predicate false")
    void compareAndSetWithRetryPredicateFalse() {
        AtomicReference<Integer> ref = new AtomicReference<>(5);

        boolean result = AtomicUtils.compareAndSetWithRetry(
            ref,
            n -> n == 10,
            n -> n * 2,
            3
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("AtomicUtils AtomicCounter works")
    void atomicCounterWorks() {
        AtomicUtils.AtomicCounter counter = new AtomicUtils.AtomicCounter();

        assertEquals(1, counter.increment());
        assertEquals(0, counter.decrement());
        assertEquals(0, counter.get());
        counter.set(10);
        assertEquals(10, counter.get());
        assertEquals(15, counter.addAndGet(5));
        assertEquals(15, counter.getAndAdd(3));
        assertEquals(18, counter.get());
        counter.reset();
        assertEquals(0, counter.get());
    }

    @Test
    @DisplayName("AtomicUtils AtomicCounter compareAndSet works")
    void atomicCounterCompareAndSet() {
        AtomicUtils.AtomicCounter counter = new AtomicUtils.AtomicCounter(5);

        assertTrue(counter.compareAndSet(5, 10));
        assertEquals(10, counter.get());
        assertFalse(counter.compareAndSet(5, 20));
    }

    @Test
    @DisplayName("AtomicUtils AtomicAccumulator works")
    void atomicAccumulatorWorks() {
        AtomicUtils.AtomicAccumulator<Integer> acc =
            new AtomicUtils.AtomicAccumulator<>(0, Integer::sum);

        assertEquals(5, acc.accumulate(5));
        assertEquals(8, acc.accumulate(3));
        assertEquals(8, acc.get());
        acc.set(100);
        assertEquals(100, acc.get());
        acc.reset(0);
        assertEquals(0, acc.get());
    }

    @Test
    @DisplayName("AtomicUtils AtomicMax tracks max")
    void atomicMaxWorks() {
        AtomicUtils.AtomicMax max = new AtomicUtils.AtomicMax();

        assertEquals(5, max.updateAndGetMax(5));
        assertEquals(10, max.updateAndGetMax(10));
        assertEquals(10, max.updateAndGetMax(7));
        assertEquals(10, max.getMax());
        max.reset();
        assertEquals(Long.MIN_VALUE, max.getMax());
    }

    @Test
    @DisplayName("AtomicUtils AtomicMin tracks min")
    void atomicMinWorks() {
        AtomicUtils.AtomicMin min = new AtomicUtils.AtomicMin();

        assertEquals(10, min.updateAndGetMin(10));
        assertEquals(5, min.updateAndGetMin(5));
        assertEquals(5, min.updateAndGetMin(7));
        assertEquals(5, min.getMin());
        min.reset();
        assertEquals(Long.MAX_VALUE, min.getMin());
    }

    @Test
    @DisplayName("AtomicUtils AtomicSum tracks sum")
    void atomicSumWorks() {
        AtomicUtils.AtomicSum sum = new AtomicUtils.AtomicSum();

        assertEquals(5, sum.add(5));
        assertEquals(8, sum.add(3));
        assertEquals(8, sum.get());
        sum.reset();
        assertEquals(0, sum.get());
    }

    @Test
    @DisplayName("AtomicUtils AtomicAverage calculates average")
    void atomicAverageWorks() {
        AtomicUtils.AtomicAverage avg = new AtomicUtils.AtomicAverage();

        avg.add(10);
        avg.add(20);
        avg.add(30);

        assertEquals(20.0, avg.getAverage());
        assertEquals(60, avg.getSum());
        assertEquals(3, avg.getCount());
        avg.reset();
        assertEquals(0, avg.getCount());
    }

    @Test
    @DisplayName("AtomicUtils AtomicFlag works")
    void atomicFlagWorks() {
        AtomicUtils.AtomicFlag flag = new AtomicUtils.AtomicFlag();

        assertFalse(flag.isSet());
        assertTrue(flag.setOnce());
        assertTrue(flag.isSet());
        assertFalse(flag.setOnce()); // Already set
        flag.reset();
        assertFalse(flag.isSet());
    }

    @Test
    @DisplayName("AtomicUtils AtomicLatch works")
    void atomicLatchWorks() {
        AtomicUtils.AtomicLatch latch = new AtomicUtils.AtomicLatch(3);

        assertFalse(latch.isReleased());
        assertFalse(latch.countDown()); // 2 remaining
        assertFalse(latch.isReleased());
        assertFalse(latch.countDown()); // 1 remaining
        assertTrue(latch.countDown());  // Released
        assertTrue(latch.isReleased());
        latch.reset(2);
        assertEquals(2, latch.getCount());
    }

    @Test
    @DisplayName("AtomicUtils AtomicOnce initializes once")
    void atomicOnceWorks() {
        AtomicUtils.AtomicOnce<String> once = new AtomicUtils.AtomicOnce<>();

        assertFalse(once.isInitialized());
        assertFalse(once.get().isPresent());

        String value = once.getOrInitialize(() -> "test");

        assertEquals("test", value);
        assertTrue(once.isInitialized());

        // Second call returns same value
        String value2 = once.getOrInitialize(() -> "other");
        assertEquals("test", value2);

        once.reset();
        assertFalse(once.isInitialized());
    }

    @Test
    @DisplayName("AtomicUtils AtomicCondition signals")
    void atomicConditionWorks() {
        AtomicUtils.AtomicCondition condition = new AtomicUtils.AtomicCondition();

        assertFalse(condition.isSignaled());
        condition.signal();
        assertTrue(condition.isSignaled());
        condition.reset();
        assertFalse(condition.isSignaled());
    }
}