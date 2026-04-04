/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/sleep
 */
package com.anthropic.claudecode.utils;

import java.time.Duration;
import java.util.concurrent.*;

/**
 * Sleep utilities - Thread sleep operations.
 */
public final class SleepUtils {

    /**
     * Sleep for the specified number of milliseconds.
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Sleep for the specified number of seconds.
     */
    public static void sleepSeconds(int seconds) {
        sleep(seconds * 1000L);
    }

    /**
     * Sleep for the specified duration.
     */
    public static void sleep(Duration duration) {
        sleep(duration.toMillis());
    }

    /**
     * Sleep with exponential backoff.
     */
    public static void sleepWithBackoff(int attempt, long baseDelayMs, long maxDelayMs) {
        long delay = Math.min(baseDelayMs * (1L << attempt), maxDelayMs);
        sleep(delay);
    }

    /**
     * Sleep with jitter (random variation).
     */
    public static void sleepWithJitter(long baseDelayMs, double jitterFactor) {
        double jitter = baseDelayMs * jitterFactor * Math.random();
        long delay = (long) (baseDelayMs + jitter - (baseDelayMs * jitterFactor / 2));
        sleep(delay);
    }

    /**
     * Sleep until a condition is met or timeout.
     */
    public static boolean sleepUntil(SleepCondition condition, long timeoutMs, long checkIntervalMs) {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition.isMet()) {
                return true;
            }
            sleep(checkIntervalMs);
        }

        return condition.isMet();
    }

    /**
     * Create a completable future that completes after a delay.
     */
    public static CompletableFuture<Void> delayAsync(long millis) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> future.complete(null), millis, TimeUnit.MILLISECONDS);
        scheduler.shutdown();
        return future;
    }

    /**
     * Sleep condition interface.
     */
    @FunctionalInterface
    public interface SleepCondition {
        boolean isMet();
    }
}