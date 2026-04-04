/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code workload context utilities
 */
package com.anthropic.claudecode.utils;

/**
 * Workload context utilities for turn-scoped workload tagging.
 *
 * In TypeScript, this uses AsyncLocalStorage for context propagation.
 * In Java, we use ThreadLocal for similar context isolation.
 */
public final class WorkloadContext {
    private WorkloadContext() {}

    /**
     * Workload types - server-side sanitizer accepts only lowercase [a-z0-9_-]{0,32}.
     */
    public static final String WORKLOAD_CRON = "cron";

    private static final ThreadLocal<String> workloadStorage = ThreadLocal.withInitial(() -> null);

    /**
     * Get the current workload for this thread.
     */
    public static String getWorkload() {
        return workloadStorage.get();
    }

    /**
     * Set the workload for this thread.
     */
    public static void setWorkload(String workload) {
        workloadStorage.set(workload);
    }

    /**
     * Clear the workload for this thread.
     */
    public static void clearWorkload() {
        workloadStorage.remove();
    }

    /**
     * Run a function with a specific workload context.
     * ALWAYS establishes a new context boundary, even when workload is null.
     */
    public static <T> T runWithWorkload(String workload, java.util.function.Supplier<T> fn) {
        String previousWorkload = workloadStorage.get();
        try {
            workloadStorage.set(workload);
            return fn.get();
        } finally {
            if (previousWorkload != null) {
                workloadStorage.set(previousWorkload);
            } else {
                workloadStorage.remove();
            }
        }
    }

    /**
     * Run a Runnable with a specific workload context.
     */
    public static void runWithWorkload(String workload, Runnable fn) {
        String previousWorkload = workloadStorage.get();
        try {
            workloadStorage.set(workload);
            fn.run();
        } finally {
            if (previousWorkload != null) {
                workloadStorage.set(previousWorkload);
            } else {
                workloadStorage.remove();
            }
        }
    }

    /**
     * Check if current workload is cron.
     */
    public static boolean isCronWorkload() {
        return WORKLOAD_CRON.equals(getWorkload());
    }

    /**
     * Run with cron workload.
     */
    public static <T> T runAsCron(java.util.function.Supplier<T> fn) {
        return runWithWorkload(WORKLOAD_CRON, fn);
    }

    /**
     * Run Runnable with cron workload.
     */
    public static void runAsCron(Runnable fn) {
        runWithWorkload(WORKLOAD_CRON, fn);
    }

    /**
     * Execute asynchronously with workload context preserved.
     * Uses CompletableFuture to maintain context across async boundaries.
     */
    public static <T> java.util.concurrent.CompletableFuture<T> runAsyncWithWorkload(
            String workload,
            java.util.function.Supplier<T> fn
    ) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            return runWithWorkload(workload, fn);
        });
    }
}