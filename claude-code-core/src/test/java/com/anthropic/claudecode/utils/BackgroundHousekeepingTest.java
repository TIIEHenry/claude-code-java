/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BackgroundHousekeeping.
 */
class BackgroundHousekeepingTest {

    @AfterEach
    void tearDown() {
        BackgroundHousekeeping.stopBackgroundHousekeeping();
    }

    @Test
    @DisplayName("BackgroundHousekeeping startBackgroundHousekeeping does not throw")
    void startBackgroundHousekeeping() {
        assertDoesNotThrow(() -> BackgroundHousekeeping.startBackgroundHousekeeping());
    }

    @Test
    @DisplayName("BackgroundHousekeeping stopBackgroundHousekeeping does not throw")
    void stopBackgroundHousekeeping() {
        BackgroundHousekeeping.startBackgroundHousekeeping();
        assertDoesNotThrow(() -> BackgroundHousekeeping.stopBackgroundHousekeeping());
    }

    @Test
    @DisplayName("BackgroundHousekeeping start twice does not throw")
    void startTwice() {
        BackgroundHousekeeping.startBackgroundHousekeeping();
        assertDoesNotThrow(() -> BackgroundHousekeeping.startBackgroundHousekeeping());
    }

    @Test
    @DisplayName("BackgroundHousekeeping initServices does not throw")
    void initServices() {
        assertDoesNotThrow(() -> BackgroundHousekeeping.initServices());
    }

    @Test
    @DisplayName("BackgroundHousekeeping autoUpdateMarketplacesAndPlugins does not throw")
    void autoUpdateMarketplacesAndPlugins() {
        assertDoesNotThrow(() -> BackgroundHousekeeping.autoUpdateMarketplacesAndPlugins());
    }
}
