/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code flush gate
 */
package com.anthropic.claudecode.utils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Flush gate for batching operations.
 */
public final class FlushGate {
    private FlushGate() {}

    /**
     * Create a new flush gate.
     */
    public static FlushGateInstance create() {
        return new FlushGateInstance();
    }

    /**
     * Flush gate instance.
     */
    public static class FlushGateInstance {
        private final AtomicInteger pendingCount = new AtomicInteger(0);
        private final CompletableFuture<Void> flushFuture = new CompletableFuture<>();
        private volatile boolean flushing = false;

        /**
         * Enter the gate (increment pending count).
         */
        public void enter() {
            pendingCount.incrementAndGet();
        }

        /**
         * Exit the gate (decrement pending count).
         */
        public void exit() {
            int remaining = pendingCount.decrementAndGet();
            if (remaining == 0 && flushing) {
                flushFuture.complete(null);
            }
        }

        /**
         * Run with gate protection.
         */
        public <T> T run(java.util.function.Supplier<T> supplier) {
            enter();
            try {
                return supplier.get();
            } finally {
                exit();
            }
        }

        /**
         * Run with gate protection (void).
         */
        public void run(Runnable runnable) {
            enter();
            try {
                runnable.run();
            } finally {
                exit();
            }
        }

        /**
         * Start flushing and wait for all pending operations.
         */
        public CompletableFuture<Void> flush() {
            flushing = true;
            if (pendingCount.get() == 0) {
                flushFuture.complete(null);
            }
            return flushFuture;
        }

        /**
         * Get pending count.
         */
        public int getPendingCount() {
            return pendingCount.get();
        }

        /**
         * Check if flushing.
         */
        public boolean isFlushing() {
            return flushing;
        }

        /**
         * Reset the gate for reuse.
         */
        public void reset() {
            pendingCount.set(0);
            flushing = false;
        }
    }
}