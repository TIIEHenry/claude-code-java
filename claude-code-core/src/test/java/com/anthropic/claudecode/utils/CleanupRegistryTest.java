/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CleanupRegistry.
 */
class CleanupRegistryTest {

    @BeforeEach
    void setUp() {
        CleanupRegistry.clear();
    }

    @Test
    @DisplayName("CleanupRegistry registerCleanup adds function")
    void registerCleanup() {
        Runnable unregister = CleanupRegistry.registerCleanup(() -> {
            // Cleanup function
        });

        assertEquals(1, CleanupRegistry.size());
        unregister.run();
        assertEquals(0, CleanupRegistry.size());
    }

    @Test
    @DisplayName("CleanupRegistry register with name")
    void registerWithName() {
        Runnable unregister = CleanupRegistry.register("testCleanup", () -> {
            // Cleanup function
        });

        assertEquals(1, CleanupRegistry.size());
        unregister.run();
        assertEquals(0, CleanupRegistry.size());
    }

    @Test
    @DisplayName("CleanupRegistry registerCleanup async")
    void registerCleanupAsync() {
        Runnable unregister = CleanupRegistry.registerCleanup(
            () -> CompletableFuture.completedFuture(null)
        );

        assertEquals(1, CleanupRegistry.size());
        unregister.run();
        assertEquals(0, CleanupRegistry.size());
    }

    @Test
    @DisplayName("CleanupRegistry runCleanupFunctions")
    void runCleanupFunctions() throws Exception {
        CleanupRegistry.registerCleanup(() -> {
            // Cleanup function 1
        });
        CleanupRegistry.registerCleanup(() -> {
            // Cleanup function 2
        });

        CompletableFuture<Void> future = CleanupRegistry.runCleanupFunctions();
        assertDoesNotThrow(() -> future.get(5, java.util.concurrent.TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("CleanupRegistry clear")
    void clear() {
        CleanupRegistry.registerCleanup(() -> {});
        CleanupRegistry.registerCleanup(() -> {});
        assertEquals(2, CleanupRegistry.size());

        CleanupRegistry.clear();
        assertEquals(0, CleanupRegistry.size());
    }

    @Test
    @DisplayName("CleanupRegistry size")
    void size() {
        assertEquals(0, CleanupRegistry.size());

        CleanupRegistry.registerCleanup(() -> {});
        assertEquals(1, CleanupRegistry.size());

        CleanupRegistry.registerCleanup(() -> {});
        assertEquals(2, CleanupRegistry.size());

        CleanupRegistry.clear();
        assertEquals(0, CleanupRegistry.size());
    }

    @Test
    @DisplayName("CleanupRegistry multiple unregister calls")
    void multipleUnregister() {
        Runnable unregister = CleanupRegistry.registerCleanup(() -> {});
        assertEquals(1, CleanupRegistry.size());

        unregister.run();
        assertEquals(0, CleanupRegistry.size());

        // Second unregister should not throw
        unregister.run();
        assertEquals(0, CleanupRegistry.size());
    }
}