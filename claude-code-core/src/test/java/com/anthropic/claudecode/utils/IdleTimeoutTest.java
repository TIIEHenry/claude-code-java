/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IdleTimeout.
 */
class IdleTimeoutTest {

    @Test
    @DisplayName("IdleTimeout createIdleTimeoutManager returns manager")
    void createIdleTimeoutManager() {
        IdleTimeout.IdleTimeoutManager manager = IdleTimeout.createIdleTimeoutManager(() -> true);
        assertNotNull(manager);
    }

    @Test
    @DisplayName("IdleTimeout IdleTimeoutManager start does not throw")
    void startNoThrow() {
        IdleTimeout.IdleTimeoutManager manager = IdleTimeout.createIdleTimeoutManager(() -> true);
        assertDoesNotThrow(() -> manager.start());
    }

    @Test
    @DisplayName("IdleTimeout IdleTimeoutManager stop does not throw")
    void stopNoThrow() {
        IdleTimeout.IdleTimeoutManager manager = IdleTimeout.createIdleTimeoutManager(() -> true);
        assertDoesNotThrow(() -> manager.stop());
    }

    @Test
    @DisplayName("IdleTimeout IdleTimeoutManager start and stop")
    void startAndStop() {
        IdleTimeout.IdleTimeoutManager manager = IdleTimeout.createIdleTimeoutManager(() -> true);
        manager.start();
        manager.stop();
        // Should not throw
        assertTrue(true);
    }

    @Test
    @DisplayName("IdleTimeout IdleTimeoutManager multiple starts")
    void multipleStarts() {
        IdleTimeout.IdleTimeoutManager manager = IdleTimeout.createIdleTimeoutManager(() -> true);
        manager.start();
        manager.start();
        manager.stop();
        // Should not throw
        assertTrue(true);
    }

    @Test
    @DisplayName("IdleTimeout IdleTimeoutManager multiple stops")
    void multipleStops() {
        IdleTimeout.IdleTimeoutManager manager = IdleTimeout.createIdleTimeoutManager(() -> true);
        manager.stop();
        manager.stop();
        // Should not throw
        assertTrue(true);
    }
}