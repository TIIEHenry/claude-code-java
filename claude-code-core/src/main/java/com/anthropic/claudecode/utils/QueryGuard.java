/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/QueryGuard
 */
package com.anthropic.claudecode.utils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Query guard - Prevents concurrent query execution with deduplication.
 */
public final class QueryGuard<T> {
    private final ConcurrentHashMap<String, CompletableFuture<T>> pendingQueries = new ConcurrentHashMap<>();
    private final AtomicLong queryCounter = new AtomicLong(0);

    /**
     * Execute a query with deduplication.
     */
    public CompletableFuture<T> execute(String key, QueryGuard.Supplier<T> querySupplier) {
        return pendingQueries.computeIfAbsent(key, k -> {
            queryCounter.incrementAndGet();
            CompletableFuture<T> future = querySupplier.get();
            future.whenComplete((result, error) -> pendingQueries.remove(k));
            return future;
        });
    }

    /**
     * Execute a query without deduplication.
     */
    public CompletableFuture<T> executeFresh(String key, QueryGuard.Supplier<T> querySupplier) {
        queryCounter.incrementAndGet();
        return querySupplier.get();
    }

    /**
     * Cancel a pending query.
     */
    public boolean cancel(String key) {
        CompletableFuture<T> future = pendingQueries.get(key);
        if (future != null) {
            return future.cancel(false);
        }
        return false;
    }

    /**
     * Cancel all pending queries.
     */
    public void cancelAll() {
        pendingQueries.values().forEach(future -> future.cancel(false));
        pendingQueries.clear();
    }

    /**
     * Get pending query count.
     */
    public int getPendingCount() {
        return pendingQueries.size();
    }

    /**
     * Get total query count.
     */
    public long getTotalQueryCount() {
        return queryCounter.get();
    }

    /**
     * Check if a query is pending.
     */
    public boolean isPending(String key) {
        CompletableFuture<T> future = pendingQueries.get(key);
        return future != null && !future.isDone();
    }

    /**
     * Get pending query keys.
     */
    public java.util.Set<String> getPendingKeys() {
        return new java.util.HashSet<>(pendingQueries.keySet());
    }

    /**
     * Wait for all pending queries to complete.
     */
    public CompletableFuture<Void> waitForAll() {
        CompletableFuture<?>[] futures = pendingQueries.values().toArray(new CompletableFuture[0]);
        return CompletableFuture.allOf(futures);
    }

    /**
     * Supplier interface for async operations.
     */
    @FunctionalInterface
    public interface Supplier<T> {
        CompletableFuture<T> get();
    }
}