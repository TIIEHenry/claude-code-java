/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FlushGate.
 */
class FlushGateTest {

    @Test
    @DisplayName("FlushGate create returns instance")
    void create() {
        FlushGate.FlushGateInstance gate = FlushGate.create();

        assertNotNull(gate);
        assertEquals(0, gate.getPendingCount());
        assertFalse(gate.isFlushing());
    }

    @Test
    @DisplayName("FlushGate enter increments count")
    void enter() {
        FlushGate.FlushGateInstance gate = FlushGate.create();

        gate.enter();
        assertEquals(1, gate.getPendingCount());

        gate.enter();
        assertEquals(2, gate.getPendingCount());
    }

    @Test
    @DisplayName("FlushGate exit decrements count")
    void exit() {
        FlushGate.FlushGateInstance gate = FlushGate.create();

        gate.enter();
        gate.enter();
        assertEquals(2, gate.getPendingCount());

        gate.exit();
        assertEquals(1, gate.getPendingCount());
    }

    @Test
    @DisplayName("FlushGate run with supplier")
    void runSupplier() {
        FlushGate.FlushGateInstance gate = FlushGate.create();

        String result = gate.run(() -> {
            assertEquals(1, gate.getPendingCount());
            return "test";
        });

        assertEquals("test", result);
        assertEquals(0, gate.getPendingCount());
    }

    @Test
    @DisplayName("FlushGate run with runnable")
    void runRunnable() {
        FlushGate.FlushGateInstance gate = FlushGate.create();
        int[] counter = {0};

        gate.run(() -> {
            assertEquals(1, gate.getPendingCount());
            counter[0]++;
        });

        assertEquals(1, counter[0]);
        assertEquals(0, gate.getPendingCount());
    }

    @Test
    @DisplayName("FlushGate flush completes when no pending")
    void flushNoPending() {
        FlushGate.FlushGateInstance gate = FlushGate.create();

        CompletableFuture<Void> future = gate.flush();

        assertTrue(future.isDone());
    }

    @Test
    @DisplayName("FlushGate flush waits for pending")
    void flushWaitsForPending() throws Exception {
        FlushGate.FlushGateInstance gate = FlushGate.create();

        gate.enter();
        gate.enter();

        CompletableFuture<Void> future = gate.flush();

        assertFalse(future.isDone());

        gate.exit();
        assertFalse(future.isDone());

        gate.exit();
        assertTrue(future.isDone());
    }

    @Test
    @DisplayName("FlushGate reset clears state")
    void reset() {
        FlushGate.FlushGateInstance gate = FlushGate.create();

        gate.enter();
        gate.enter();
        gate.flush();

        gate.reset();

        assertEquals(0, gate.getPendingCount());
        assertFalse(gate.isFlushing());
    }

    @Test
    @DisplayName("FlushGate isFlushing after flush")
    void isFlushing() {
        FlushGate.FlushGateInstance gate = FlushGate.create();

        assertFalse(gate.isFlushing());
        gate.flush();
        assertTrue(gate.isFlushing());
    }

    @Test
    @DisplayName("FlushGate getPendingCount returns count")
    void getPendingCount() {
        FlushGate.FlushGateInstance gate = FlushGate.create();

        assertEquals(0, gate.getPendingCount());
        gate.enter();
        assertEquals(1, gate.getPendingCount());
        gate.enter();
        assertEquals(2, gate.getPendingCount());
        gate.exit();
        assertEquals(1, gate.getPendingCount());
    }

    @Test
    @DisplayName("FlushGate multiple enters and exits")
    void multipleEntersExits() {
        FlushGate.FlushGateInstance gate = FlushGate.create();

        for (int i = 0; i < 10; i++) {
            gate.enter();
        }

        assertEquals(10, gate.getPendingCount());

        for (int i = 0; i < 10; i++) {
            gate.exit();
        }

        assertEquals(0, gate.getPendingCount());
    }

    @Test
    @DisplayName("FlushGate flush completes after pending cleared")
    void flushCompletesAfterPendingCleared() throws Exception {
        FlushGate.FlushGateInstance gate = FlushGate.create();

        gate.enter();

        CompletableFuture<Void> flushFuture = gate.flush();

        // Run async to exit after flush starts
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
                gate.exit();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        flushFuture.get(1, TimeUnit.SECONDS);
        assertTrue(flushFuture.isDone());
    }

    @Test
    @DisplayName("FlushGate run protects against exceptions")
    void runProtectsAgainstExceptions() {
        FlushGate.FlushGateInstance gate = FlushGate.create();

        assertThrows(RuntimeException.class, () ->
            gate.run(() -> {
                throw new RuntimeException("test");
            })
        );

        // Count should still be 0 after exception
        assertEquals(0, gate.getPendingCount());
    }
}