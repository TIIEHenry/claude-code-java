/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code bulkhead pattern
 */
package com.anthropic.claudecode.utils;

import java.util.concurrent.CompletableFuture;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Bulkhead - limits concurrent executions.
 */
public final class Bulkhead {
    private final String name;
    private final int maxConcurrentCalls;
    private final Semaphore semaphore;
    private final AtomicInteger waitingCount = new AtomicInteger(0);

    public Bulkhead(String name, int maxConcurrentCalls) {
        this.name = name;
        this.maxConcurrentCalls = maxConcurrentCalls;
        this.semaphore = new Semaphore(maxConcurrentCalls);
    }

    /**
     * Execute with bulkhead protection.
     */
    public <T> T execute(Supplier<T> operation) throws BulkheadException {
        return execute(operation, Duration.ZERO);
    }

    /**
     * Execute with timeout.
     */
    public <T> T execute(Supplier<T> operation, Duration timeout) throws BulkheadException {
        boolean acquired = tryAcquire(timeout);
        if (!acquired) {
            throw new BulkheadException(name, "Bulkhead is full, cannot acquire permit");
        }

        try {
            return operation.get();
        } finally {
            release();
        }
    }

    /**
     * Execute runnable.
     */
    public void execute(Runnable operation) throws BulkheadException {
        execute(() -> {
            operation.run();
            return null;
        });
    }

    /**
     * Execute runnable with timeout.
     */
    public void execute(Runnable operation, Duration timeout) throws BulkheadException {
        execute(() -> {
            operation.run();
            return null;
        }, timeout);
    }

    /**
     * Try to acquire permit.
     */
    public boolean tryAcquire() {
        return tryAcquire(Duration.ZERO);
    }

    /**
     * Try to acquire with timeout.
     */
    public boolean tryAcquire(Duration timeout) {
        waitingCount.incrementAndGet();
        try {
            if (timeout.isZero()) {
                return semaphore.tryAcquire();
            }
            return semaphore.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            waitingCount.decrementAndGet();
        }
    }

    /**
     * Release permit.
     */
    public void release() {
        semaphore.release();
    }

    /**
     * Get available permits.
     */
    public int getAvailablePermits() {
        return semaphore.availablePermits();
    }

    /**
     * Get waiting count.
     */
    public int getWaitingCount() {
        return waitingCount.get();
    }

    /**
     * Get max concurrent calls.
     */
    public int getMaxConcurrentCalls() {
        return maxConcurrentCalls;
    }

    /**
     * Get name.
     */
    public String getName() {
        return name;
    }

    /**
     * Check if bulkhead is full.
     */
    public boolean isFull() {
        return getAvailablePermits() == 0;
    }

    /**
     * Get current concurrent calls.
     */
    public int getCurrentConcurrentCalls() {
        return maxConcurrentCalls - getAvailablePermits();
    }

    /**
     * Get statistics.
     */
    public BulkheadStats getStats() {
        return new BulkheadStats(
            name,
            maxConcurrentCalls,
            getCurrentConcurrentCalls(),
            getAvailablePermits(),
            waitingCount.get()
        );
    }

    @Override
    public String toString() {
        return String.format("Bulkhead[%s, max=%d, current=%d, waiting=%d]",
            name, maxConcurrentCalls, getCurrentConcurrentCalls(), waitingCount.get());
    }

    /**
     * Bulkhead exception.
     */
    public static final class BulkheadException extends RuntimeException {
        private final String bulkheadName;

        public BulkheadException(String name, String message) {
            super(message);
            this.bulkheadName = name;
        }

        public String getBulkheadName() {
            return bulkheadName;
        }
    }

    /**
     * Bulkhead statistics.
     */
    public record BulkheadStats(
        String name,
        int maxConcurrentCalls,
        int currentConcurrentCalls,
        int availablePermits,
        int waitingCount
    ) {
        public double utilization() {
            return maxConcurrentCalls > 0 ? (double) currentConcurrentCalls / maxConcurrentCalls : 0;
        }

        public String format() {
            return String.format("%s: %d/%d (%.1f%% utilized), %d waiting",
                name, currentConcurrentCalls, maxConcurrentCalls, utilization() * 100, waitingCount);
        }
    }

    /**
     * Bulkhead builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class.
     */
    public static final class Builder {
        private String name = "default";
        private int maxConcurrentCalls = 10;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder maxConcurrentCalls(int max) {
            this.maxConcurrentCalls = max;
            return this;
        }

        public Bulkhead build() {
            return new Bulkhead(name, maxConcurrentCalls);
        }
    }

    /**
     * Bulkhead registry.
     */
    public static final class BulkheadRegistry {
        private final Map<String, Bulkhead> bulkheads = new ConcurrentHashMap<>();

        public Bulkhead getOrCreate(String name, int maxConcurrentCalls) {
            return bulkheads.computeIfAbsent(name,
                n -> new Bulkhead(n, maxConcurrentCalls));
        }

        public Optional<Bulkhead> get(String name) {
            return Optional.ofNullable(bulkheads.get(name));
        }

        public void remove(String name) {
            bulkheads.remove(name);
        }

        public void clear() {
            bulkheads.clear();
        }

        public Map<String, BulkheadStats> getAllStats() {
            return bulkheads.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().getStats()
                ));
        }
    }
}