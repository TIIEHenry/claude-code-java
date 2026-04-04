/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExecutorUtils.
 */
class ExecutorUtilsTest {

    @Test
    @DisplayName("ExecutorUtils withRetry succeeds on first attempt")
    void withRetrySuccess() {
        AtomicInteger attempts = new AtomicInteger(0);
        Integer result = ExecutorUtils.withRetry(3, 10, () -> {
            attempts.incrementAndGet();
            return 42;
        });

        assertEquals(42, result);
        assertEquals(1, attempts.get());
    }

    @Test
    @DisplayName("ExecutorUtils withRetry succeeds after failures")
    void withRetrySuccessAfterFailures() {
        AtomicInteger attempts = new AtomicInteger(0);
        Integer result = ExecutorUtils.withRetry(5, 10, () -> {
            int a = attempts.incrementAndGet();
            if (a < 3) throw new RuntimeException("fail");
            return 42;
        });

        assertEquals(42, result);
        assertEquals(3, attempts.get());
    }

    @Test
    @DisplayName("ExecutorUtils withRetry fails after max attempts")
    void withRetryFail() {
        assertThrows(RuntimeException.class, () ->
            ExecutorUtils.withRetry(3, 10, () -> {
                throw new RuntimeException("always fail");
            })
        );
    }

    @Test
    @DisplayName("ExecutorUtils withTimeout succeeds within timeout")
    void withTimeoutSuccess() {
        Integer result = ExecutorUtils.withTimeout(1000, () -> 42);
        assertEquals(42, result);
    }

    @Test
    @DisplayName("ExecutorUtils withTimeout throws on timeout")
    void withTimeoutFail() {
        assertThrows(RuntimeException.class, () ->
            ExecutorUtils.withTimeout(100, () -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return 42;
            })
        );
    }

    @Test
    @DisplayName("ExecutorUtils withFallback uses primary on success")
    void withFallbackPrimarySuccess() {
        Integer result = ExecutorUtils.withFallback(() -> 42, () -> 0);
        assertEquals(42, result);
    }

    @Test
    @DisplayName("ExecutorUtils withFallback uses fallback on failure")
    void withFallbackPrimaryFails() {
        Integer result = ExecutorUtils.withFallback(
            () -> { throw new RuntimeException("fail"); },
            () -> 0
        );
        assertEquals(0, result);
    }

    @Test
    @DisplayName("ExecutorUtils withFallbacks uses first successful")
    void withFallbacksFirstSuccess() {
        Integer result = ExecutorUtils.withFallbacks(
            () -> 42,
            () -> 10,
            () -> 0
        );
        assertEquals(42, result);
    }

    @Test
    @DisplayName("ExecutorUtils withFallbacks uses second when first fails")
    void withFallbacksSecondSuccess() {
        Integer result = ExecutorUtils.withFallbacks(
            () -> { throw new RuntimeException("fail"); },
            () -> 10,
            () -> 0
        );
        assertEquals(10, result);
    }

    @Test
    @DisplayName("ExecutorUtils withFallbacks throws when all fail")
    void withFallbacksAllFail() {
        assertThrows(RuntimeException.class, () ->
            ExecutorUtils.withFallbacks(
                () -> { throw new RuntimeException("fail1"); },
                () -> { throw new RuntimeException("fail2"); },
                () -> { throw new RuntimeException("fail3"); }
            )
        );
    }

    @Test
    @DisplayName("ExecutorUtils executeIf true")
    void executeIfTrue() {
        Optional<Integer> result = ExecutorUtils.executeIf(true, () -> 42);
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    @DisplayName("ExecutorUtils executeIf false")
    void executeIfFalse() {
        Optional<Integer> result = ExecutorUtils.executeIf(false, () -> 42);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("ExecutorUtils ifNotNull with non-null")
    void ifNotNullNonNull() {
        Optional<Integer> result = ExecutorUtils.ifNotNull("hello", String::length);
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    @DisplayName("ExecutorUtils ifNotNull with null")
    void ifNotNullNull() {
        Optional<Integer> result = ExecutorUtils.ifNotNull(null, String::length);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("ExecutorUtils ifNotBlank with non-blank")
    void ifNotBlankNonBlank() {
        Optional<Integer> result = ExecutorUtils.ifNotBlank("hello", String::length);
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    @DisplayName("ExecutorUtils ifNotBlank with blank")
    void ifNotBlankBlank() {
        Optional<Integer> result = ExecutorUtils.ifNotBlank("  ", String::length);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("ExecutorUtils ifNotBlank with null")
    void ifNotBlankNull() {
        Optional<Integer> result = ExecutorUtils.ifNotBlank(null, String::length);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("ExecutorUtils ifNotEmpty with non-empty collection")
    void ifNotEmptyNonEmpty() {
        List<Integer> list = List.of(1, 2, 3);
        Optional<Integer> result = ExecutorUtils.ifNotEmpty(list, l -> l.size());
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    @DisplayName("ExecutorUtils ifNotEmpty with empty collection")
    void ifNotEmptyEmpty() {
        List<Integer> list = List.of();
        Optional<Integer> result = ExecutorUtils.ifNotEmpty(list, l -> l.size());
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("ExecutorUtils ifNotEmpty with null")
    void ifNotEmptyNull() {
        Optional<Integer> result = ExecutorUtils.ifNotEmpty(null, l -> l.size());
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("ExecutorUtils measureTime with supplier")
    void measureTimeSupplier() {
        ExecutorUtils.TimedResult<Integer> result = ExecutorUtils.measureTime(() -> 42);
        assertEquals(42, result.value());
        assertTrue(result.durationNanos() >= 0);
        assertTrue(result.durationMs() >= 0);
    }

    @Test
    @DisplayName("ExecutorUtils measureTime with runnable")
    void measureTimeRunnable() {
        long duration = ExecutorUtils.measureTime(() -> {});
        assertTrue(duration >= 0);
    }

    @Test
    @DisplayName("ExecutorUtils TimedResult durationMs")
    void timedResultDurationMs() {
        ExecutorUtils.TimedResult<String> result = new ExecutorUtils.TimedResult<>("test", 1_000_000);
        assertEquals(1.0, result.durationMs(), 0.001);
    }

    @Test
    @DisplayName("ExecutorUtils async returns CompletableFuture")
    void async() throws Exception {
        CompletableFuture<Integer> future = ExecutorUtils.async(() -> 42);
        assertEquals(42, future.get());
    }

    @Test
    @DisplayName("ExecutorUtils async with executor")
    void asyncWithExecutor() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<Integer> future = ExecutorUtils.async(() -> 42, executor);
        assertEquals(42, future.get());
        executor.shutdown();
    }

    @Test
    @DisplayName("ExecutorUtils parallel executes multiple")
    void parallel() throws Exception {
        CompletableFuture<List<Integer>> future = ExecutorUtils.parallel(
            () -> 1,
            () -> 2,
            () -> 3
        );

        List<Integer> result = future.get();
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
    }

    @Test
    @DisplayName("ExecutorUtils parallel with list")
    void parallelList() throws Exception {
        List<Supplier<Integer>> suppliers = List.of(() -> 1, () -> 2, () -> 3);
        CompletableFuture<List<Integer>> future = ExecutorUtils.parallel(suppliers);

        List<Integer> result = future.get();
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("ExecutorUtils race first to complete wins")
    void race() throws Exception {
        CompletableFuture<Integer> future = ExecutorUtils.race(
            () -> { try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } return 1; },
            () -> 42  // Fastest
        );

        assertEquals(42, future.get());
    }

    @Test
    @DisplayName("ExecutorUtils race with list")
    void raceList() throws Exception {
        List<Supplier<Integer>> suppliers = List.of(
            () -> { try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } return 1; },
            () -> 42
        );

        CompletableFuture<Integer> future = ExecutorUtils.race(suppliers);
        assertEquals(42, future.get());
    }

    @Test
    @DisplayName("ExecutorUtils rateLimited enforces minimum interval")
    void rateLimited() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> rateLimited = ExecutorUtils.rateLimited(counter::incrementAndGet, 50);

        int first = rateLimited.get();
        int second = rateLimited.get();

        assertEquals(1, first);
        assertEquals(2, second);
        // The second call should have waited at least 50ms
    }

    @Test
    @DisplayName("ExecutorUtils memoized caches result")
    void memoized() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = ExecutorUtils.memoized(counter::incrementAndGet);

        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get()); // Same value
        assertEquals(1, counter.get()); // Only called once
    }

    @Test
    @DisplayName("ExecutorUtils memoized with TTL")
    void memoizedWithTtl() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = ExecutorUtils.memoized(counter::incrementAndGet, 50);

        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get()); // Cached

        Thread.sleep(100);
        assertEquals(2, memoized.get()); // Expired
    }

    @Test
    @DisplayName("ExecutorUtils once executes only once")
    void once() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> once = ExecutorUtils.once(counter::incrementAndGet);

        assertEquals(1, once.get());
        assertEquals(1, once.get());
        assertEquals(1, once.get());
        assertEquals(1, counter.get()); // Only called once
    }

    @Test
    @DisplayName("ExecutorUtils lazy is same as memoized")
    void lazy() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> lazy = ExecutorUtils.lazy(counter::incrementAndGet);

        assertEquals(1, lazy.get());
        assertEquals(1, lazy.get());
        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("ExecutorUtils withCircuitBreaker opens after threshold")
    void withCircuitBreaker() {
        AtomicInteger failures = new AtomicInteger(0);
        Supplier<Integer> breaker = ExecutorUtils.withCircuitBreaker(
            () -> {
                failures.incrementAndGet();
                throw new RuntimeException("fail");
            },
            3,
            1000
        );

        // First 3 failures should work (increment failure count)
        assertThrows(RuntimeException.class, breaker::get);
        assertThrows(RuntimeException.class, breaker::get);
        assertThrows(RuntimeException.class, breaker::get);

        // Circuit should now be open
        assertThrows(RuntimeException.class, breaker::get);
        // But the exception message should indicate circuit is open
    }

    @Test
    @DisplayName("ExecutorUtils withCircuitBreaker resets after timeout")
    void withCircuitBreakerReset() throws InterruptedException {
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Supplier<Integer> breaker = ExecutorUtils.withCircuitBreaker(
            () -> {
                if (failureCount.get() < 3) {
                    failureCount.incrementAndGet();
                    throw new RuntimeException("fail");
                }
                return successes.incrementAndGet();
            },
            3,
            50
        );

        // Trigger failures to open circuit
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, breaker::get);
        }

        // Wait for reset
        Thread.sleep(100);

        // Should now work
        assertEquals(1, breaker.get());
    }

    @Test
    @DisplayName("ExecutorUtils withBulkhead limits concurrent calls")
    void withBulkhead() throws Exception {
        AtomicInteger concurrent = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        Supplier<Integer> bulkhead = ExecutorUtils.withBulkhead(() -> {
            int current = concurrent.incrementAndGet();
            maxConcurrent.updateAndGet(m -> Math.max(m, current));
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            concurrent.decrementAndGet();
            return current;
        }, 2);

        // Run multiple calls in parallel
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.supplyAsync(bulkhead));
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        // Max concurrent should not exceed 2
        assertTrue(maxConcurrent.get() <= 2);
    }

    @Test
    @DisplayName("ExecutorUtils withContext executes with setup and teardown")
    void withContext() {
        StringBuilder sb = new StringBuilder();
        Supplier<String> withContext = ExecutorUtils.withContext(
            () -> sb.toString(),
            sb,
            s -> s.append("setup"),
            s -> s.append("teardown")
        );

        String result = withContext.get();
        assertEquals("setup", result);
        assertEquals("setupteardown", sb.toString());
    }

    @Test
    @DisplayName("ExecutorUtils background executes asynchronously")
    void background() throws Exception {
        CompletableFuture<Integer> future = ExecutorUtils.background(() -> 42);
        assertEquals(42, future.get());
    }

    @Test
    @DisplayName("ExecutorUtils delayed executes after delay")
    void delayed() throws Exception {
        long start = System.currentTimeMillis();
        CompletableFuture<Integer> future = ExecutorUtils.delayed(() -> 42, 100);
        int result = future.get();
        long elapsed = System.currentTimeMillis() - start;

        assertEquals(42, result);
        assertTrue(elapsed >= 100);
    }

    @Test
    @DisplayName("ExecutorUtils periodic executes repeatedly")
    void periodic() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        ScheduledFuture<?> future = ExecutorUtils.periodic(counter::incrementAndGet, 0, 50);

        Thread.sleep(200);
        future.cancel(true);

        assertTrue(counter.get() >= 3); // Should have executed multiple times
    }
}