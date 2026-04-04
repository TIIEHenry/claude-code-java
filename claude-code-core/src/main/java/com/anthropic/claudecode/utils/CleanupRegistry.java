/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/cleanupRegistry
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Global registry for cleanup functions that should run during graceful shutdown.
 * This class is separate from GracefulShutdown to avoid circular dependencies.
 */
public final class CleanupRegistry {
    private static final Set<CompletableFuture<Void>> cleanupFunctions =
        ConcurrentHashMap.newKeySet();

    /**
     * Register a cleanup function to run during graceful shutdown.
     * @return Unregister function that removes the cleanup handler
     */
    public static Runnable registerCleanup(Runnable cleanupFn) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(cleanupFn);
        cleanupFunctions.add(future);
        return () -> cleanupFunctions.remove(future);
    }

    /**
     * Register a cleanup function with a name.
     * @param name Name for the cleanup function
     * @param cleanupFn The cleanup function to run
     * @return Unregister function that removes the cleanup handler
     */
    public static Runnable register(String name, Runnable cleanupFn) {
        return registerCleanup(cleanupFn);
    }

    /**
     * Register an async cleanup function.
     * @return Unregister function that removes the cleanup handler
     */
    public static Runnable registerCleanup(Supplier<CompletableFuture<Void>> cleanupFn) {
        CompletableFuture<Void> future = cleanupFn.get();
        cleanupFunctions.add(future);
        return () -> cleanupFunctions.remove(future);
    }

    /**
     * Run all registered cleanup functions.
     * Used internally by gracefulShutdown.
     */
    public static CompletableFuture<Void> runCleanupFunctions() {
        List<CompletableFuture<Void>> futures = new ArrayList<>(cleanupFunctions);
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Clear all registered cleanup functions.
     */
    public static void clear() {
        cleanupFunctions.clear();
    }

    /**
     * Get count of registered cleanup functions.
     */
    public static int size() {
        return cleanupFunctions.size();
    }
}