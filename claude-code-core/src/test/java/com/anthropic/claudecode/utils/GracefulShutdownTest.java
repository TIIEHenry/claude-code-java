/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GracefulShutdown.
 */
class GracefulShutdownTest {

    @BeforeEach
    void setUp() {
        GracefulShutdown.resetShutdownState();
    }

    @Test
    @DisplayName("GracefulShutdown ExitReason enum")
    void exitReasonEnum() {
        GracefulShutdown.ExitReason[] reasons = GracefulShutdown.ExitReason.values();
        assertEquals(5, reasons.length);
        assertEquals(GracefulShutdown.ExitReason.SIGINT, GracefulShutdown.ExitReason.valueOf("SIGINT"));
        assertEquals(GracefulShutdown.ExitReason.SIGTERM, GracefulShutdown.ExitReason.valueOf("SIGTERM"));
        assertEquals(GracefulShutdown.ExitReason.SIGHUP, GracefulShutdown.ExitReason.valueOf("SIGHUP"));
        assertEquals(GracefulShutdown.ExitReason.ORPHAN_DETECTED, GracefulShutdown.ExitReason.valueOf("ORPHAN_DETECTED"));
        assertEquals(GracefulShutdown.ExitReason.OTHER, GracefulShutdown.ExitReason.valueOf("OTHER"));
    }

    @Test
    @DisplayName("GracefulShutdown isShuttingDown initially false")
    void isShuttingDownInitiallyFalse() {
        assertFalse(GracefulShutdown.isShuttingDown());
    }

    @Test
    @DisplayName("GracefulShutdown resetShutdownState")
    void resetShutdownState() {
        GracefulShutdown.resetShutdownState();
        assertFalse(GracefulShutdown.isShuttingDown());
    }

    @Test
    @DisplayName("GracefulShutdown addCleanupFunction does not throw")
    void addCleanupFunction() {
        assertDoesNotThrow(() -> GracefulShutdown.addCleanupFunction(() -> {}));
    }

    @Test
    @DisplayName("GracefulShutdown removeCleanupFunction does not throw")
    void removeCleanupFunction() {
        Runnable cleanup = () -> {};
        GracefulShutdown.addCleanupFunction(cleanup);
        assertDoesNotThrow(() -> GracefulShutdown.removeCleanupFunction(cleanup));
    }

    @Test
    @DisplayName("GracefulShutdown setupGracefulShutdown does not throw")
    void setupGracefulShutdown() {
        // Note: This adds a shutdown hook, be careful about side effects
        assertDoesNotThrow(() -> GracefulShutdown.setupGracefulShutdown());
    }

    @Test
    @DisplayName("GracefulShutdown getPendingShutdownForTesting")
    void getPendingShutdownForTesting() {
        assertNull(GracefulShutdown.getPendingShutdownForTesting());
    }
}