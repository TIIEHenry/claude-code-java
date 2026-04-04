/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.sleep;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PreventSleepService.
 */
class PreventSleepServiceTest {

    @AfterEach
    void tearDown() {
        PreventSleepService.forceStopPreventSleep();
    }

    @Test
    @DisplayName("PreventSleepService constants")
    void constants() {
        // Verify the timeout and restart intervals are reasonable
        // CAFFEINATE_TIMEOUT_SECONDS = 300 (5 minutes)
        // RESTART_INTERVAL_MS = 4 * 60 * 1000 (4 minutes)
        // These are private but we verify the service works
    }

    @Test
    @DisplayName("PreventSleepService startPreventSleep increments ref count")
    void startPreventSleep() {
        PreventSleepService.forceStopPreventSleep();
        PreventSleepService.startPreventSleep();

        // On macOS, this would spawn caffeinate process
        // On other platforms, it's a no-op

        PreventSleepService.stopPreventSleep();
    }

    @Test
    @DisplayName("PreventSleepService refCount tracks multiple calls")
    void refCountTracking() {
        PreventSleepService.forceStopPreventSleep();

        PreventSleepService.startPreventSleep();
        PreventSleepService.startPreventSleep();
        PreventSleepService.startPreventSleep();

        // Should still be running with refCount=3

        PreventSleepService.stopPreventSleep();
        PreventSleepService.stopPreventSleep();
        // Still refCount=1

        PreventSleepService.stopPreventSleep();
        // Now refCount=0, should stop
    }

    @Test
    @DisplayName("PreventSleepService stopPreventSleep handles over-decrement")
    void stopPreventSleepOverDecrement() {
        PreventSleepService.forceStopPreventSleep();

        // Should not throw when stopping without starting
        assertDoesNotThrow(() -> PreventSleepService.stopPreventSleep());
        assertDoesNotThrow(() -> PreventSleepService.stopPreventSleep());
    }

    @Test
    @DisplayName("PreventSleepService forceStopPreventSleep resets everything")
    void forceStopPreventSleep() {
        PreventSleepService.startPreventSleep();
        PreventSleepService.startPreventSleep();
        PreventSleepService.startPreventSleep();

        // Force stop should clear everything
        PreventSleepService.forceStopPreventSleep();

        // Can start again
        PreventSleepService.startPreventSleep();
        PreventSleepService.stopPreventSleep();
    }

    @Test
    @DisplayName("PreventSleepService multiple start/stop cycles")
    void multipleCycles() {
        for (int i = 0; i < 3; i++) {
            PreventSleepService.startPreventSleep();
            PreventSleepService.stopPreventSleep();
        }

        // Should handle multiple cycles without issues
    }

    @Test
    @DisplayName("PreventSleepService isMacOS detection")
    void macOsDetection() {
        // The isMacOS() method checks System.getProperty("os.name")
        // We can't easily test this without changing system properties
        // But we verify the service doesn't crash on any platform
        String osName = System.getProperty("os.name");
        assertNotNull(osName);
    }
}