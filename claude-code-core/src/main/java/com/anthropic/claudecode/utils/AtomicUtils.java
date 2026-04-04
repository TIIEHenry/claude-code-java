/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code atomic utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Atomic reference utilities.
 */
public final class AtomicUtils {
    private AtomicUtils() {}

    /**
     * Atomic reference with compare-and-set operations.
     */
    public static <T> AtomicReference<T> create(T initialValue) {
        return new AtomicReference<>(initialValue);
    }

    /**
     * Atomic integer with operations.
     */
    public static AtomicInteger createInt(int initialValue) {
        return new AtomicInteger(initialValue);
    }

    /**
     * Atomic long with operations.
     */
    public static AtomicLong createLong(long initialValue) {
        return new AtomicLong(initialValue);
    }

    /**
     * Atomic boolean with operations.
     */
    public static AtomicBoolean createBoolean(boolean initialValue) {
        return new AtomicBoolean(initialValue);
    }

    /**
     * Update atomic reference with function.
     */
    public static <T> T updateAndGet(AtomicReference<T> ref, UnaryOperator<T> updateFunction) {
        return ref.updateAndGet(updateFunction);
    }

    /**
     * Get and update atomic reference.
     */
    public static <T> T getAndUpdate(AtomicReference<T> ref, UnaryOperator<T> updateFunction) {
        return ref.getAndUpdate(updateFunction);
    }

    /**
     * Accumulate atomic reference.
     */
    public static <T> T accumulateAndGet(AtomicReference<T> ref, T x, BinaryOperator<T> accumulatorFunction) {
        return ref.accumulateAndGet(x, accumulatorFunction);
    }

    /**
     * Compare and set with retry.
     */
    public static <T> boolean compareAndSetWithRetry(AtomicReference<T> ref,
            Predicate<T> expectedPredicate, UnaryOperator<T> updateFunction, int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            T current = ref.get();
            if (!expectedPredicate.test(current)) {
                return false;
            }
            T updated = updateFunction.apply(current);
            if (ref.compareAndSet(current, updated)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Atomic counter.
     */
    public static final class AtomicCounter {
        private final AtomicInteger counter;

        public AtomicCounter(int initialValue) {
            this.counter = new AtomicInteger(initialValue);
        }

        public AtomicCounter() {
            this(0);
        }

        public int increment() {
            return counter.incrementAndGet();
        }

        public int decrement() {
            return counter.decrementAndGet();
        }

        public int get() {
            return counter.get();
        }

        public void set(int value) {
            counter.set(value);
        }

        public int addAndGet(int delta) {
            return counter.addAndGet(delta);
        }

        public int getAndAdd(int delta) {
            return counter.getAndAdd(delta);
        }

        public int getAndIncrement() {
            return counter.getAndIncrement();
        }

        public int getAndDecrement() {
            return counter.getAndDecrement();
        }

        public void reset() {
            counter.set(0);
        }

        public boolean compareAndSet(int expected, int newValue) {
            return counter.compareAndSet(expected, newValue);
        }
    }

    /**
     * Atomic accumulator.
     */
    public static final class AtomicAccumulator<T> {
        private final AtomicReference<T> value;
        private final BinaryOperator<T> accumulator;

        public AtomicAccumulator(T initialValue, BinaryOperator<T> accumulator) {
            this.value = new AtomicReference<>(initialValue);
            this.accumulator = accumulator;
        }

        public T accumulate(T delta) {
            return value.updateAndGet(current -> accumulator.apply(current, delta));
        }

        public T get() {
            return value.get();
        }

        public void set(T newValue) {
            value.set(newValue);
        }

        public void reset(T resetValue) {
            value.set(resetValue);
        }
    }

    /**
     * Atomic max tracker.
     */
    public static final class AtomicMax {
        private final AtomicLong max;

        public AtomicMax(long initialValue) {
            this.max = new AtomicLong(initialValue);
        }

        public AtomicMax() {
            this(Long.MIN_VALUE);
        }

        public long updateAndGetMax(long candidate) {
            return max.accumulateAndGet(candidate, Math::max);
        }

        public long getMax() {
            return max.get();
        }

        public void reset() {
            max.set(Long.MIN_VALUE);
        }
    }

    /**
     * Atomic min tracker.
     */
    public static final class AtomicMin {
        private final AtomicLong min;

        public AtomicMin(long initialValue) {
            this.min = new AtomicLong(initialValue);
        }

        public AtomicMin() {
            this(Long.MAX_VALUE);
        }

        public long updateAndGetMin(long candidate) {
            return min.accumulateAndGet(candidate, Math::min);
        }

        public long getMin() {
            return min.get();
        }

        public void reset() {
            min.set(Long.MAX_VALUE);
        }
    }

    /**
     * Atomic sum tracker.
     */
    public static final class AtomicSum {
        private final AtomicLong sum;

        public AtomicSum(long initialValue) {
            this.sum = new AtomicLong(initialValue);
        }

        public AtomicSum() {
            this(0);
        }

        public long add(long delta) {
            return sum.addAndGet(delta);
        }

        public long get() {
            return sum.get();
        }

        public void reset() {
            sum.set(0);
        }
    }

    /**
     * Atomic average tracker.
     */
    public static final class AtomicAverage {
        private final AtomicLong sum = new AtomicLong(0);
        private final AtomicInteger count = new AtomicInteger(0);

        public void add(long value) {
            sum.addAndGet(value);
            count.incrementAndGet();
        }

        public double getAverage() {
            int n = count.get();
            return n > 0 ? (double) sum.get() / n : 0;
        }

        public long getSum() {
            return sum.get();
        }

        public int getCount() {
            return count.get();
        }

        public void reset() {
            sum.set(0);
            count.set(0);
        }
    }

    /**
     * Atomic flag (one-time set).
     */
    public static final class AtomicFlag {
        private final AtomicBoolean flag = new AtomicBoolean(false);

        public boolean setOnce() {
            return flag.compareAndSet(false, true);
        }

        public boolean get() {
            return flag.get();
        }

        public boolean isSet() {
            return flag.get();
        }

        public void reset() {
            flag.set(false);
        }
    }

    /**
     * Atomic latch (count down to zero).
     */
    public static final class AtomicLatch {
        private final AtomicInteger count;

        public AtomicLatch(int initialCount) {
            this.count = new AtomicInteger(initialCount);
        }

        public boolean countDown() {
            while (true) {
                int current = count.get();
                if (current <= 0) return true;
                if (count.compareAndSet(current, current - 1)) {
                    return current - 1 == 0;
                }
            }
        }

        public boolean isReleased() {
            return count.get() <= 0;
        }

        public int getCount() {
            return count.get();
        }

        public void reset(int newCount) {
            count.set(newCount);
        }
    }

    /**
     * Atomic condition (wait until condition met).
     */
    public static final class AtomicCondition {
        private final AtomicBoolean condition = new AtomicBoolean(false);
        private final ConditionObject lock = new ConditionObject();

        public void signal() {
            condition.set(true);
            synchronized (lock) {
                lock.notifyAll();
            }
        }

        public void await() throws InterruptedException {
            while (!condition.get()) {
                synchronized (lock) {
                    lock.wait();
                }
            }
        }

        public boolean await(long timeoutMs) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (!condition.get()) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) return false;
                synchronized (lock) {
                    lock.wait(remaining);
                }
            }
            return true;
        }

        public boolean isSignaled() {
            return condition.get();
        }

        public void reset() {
            condition.set(false);
        }

        private static final class ConditionObject {}
    }

    /**
     * Atomic once (lazy initialization).
     */
    public static final class AtomicOnce<T> {
        private final AtomicBoolean initialized = new AtomicBoolean(false);
        private volatile T value;

        public T getOrInitialize(Supplier<T> initializer) {
            if (initialized.get()) {
                return value;
            }
            if (initialized.compareAndSet(false, true)) {
                value = initializer.get();
            }
            return value;
        }

        public Optional<T> get() {
            return initialized.get() ? Optional.ofNullable(value) : Optional.empty();
        }

        public boolean isInitialized() {
            return initialized.get();
        }

        public void reset() {
            initialized.set(false);
            value = null;
        }
    }
}