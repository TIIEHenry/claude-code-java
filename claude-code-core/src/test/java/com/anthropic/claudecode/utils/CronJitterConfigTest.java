/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CronJitterConfig.
 */
class CronJitterConfigTest {

    @BeforeEach
    void setUp() {
        CronJitterConfig.reset();
    }

    @Test
    @DisplayName("CronJitterConfig Config record")
    void configRecord() {
        CronJitterConfig.Config config = new CronJitterConfig.Config(
            0.1, 900000L, 90000L, 0L, 30, 604800000L
        );

        assertEquals(0.1, config.recurringFrac());
        assertEquals(900000L, config.recurringCapMs());
        assertEquals(90000L, config.oneShotMaxMs());
        assertEquals(0L, config.oneShotFloorMs());
        assertEquals(30, config.oneShotMinuteMod());
        assertEquals(604800000L, config.recurringMaxAgeMs());
    }

    @Test
    @DisplayName("CronJitterConfig DEFAULT_CONFIG values")
    void defaultConfigValues() {
        CronJitterConfig.Config config = CronJitterConfig.DEFAULT_CONFIG;

        assertEquals(0.1, config.recurringFrac());
        assertEquals(15L * 60 * 1000, config.recurringCapMs()); // 15 minutes
        assertEquals(90L * 1000, config.oneShotMaxMs()); // 90 seconds
        assertEquals(0L, config.oneShotFloorMs());
        assertEquals(30, config.oneShotMinuteMod());
        assertEquals(7L * 24 * 60 * 60 * 1000, config.recurringMaxAgeMs()); // 7 days
    }

    @Test
    @DisplayName("CronJitterConfig getConfig returns default")
    void getConfigDefault() {
        CronJitterConfig.Config config = CronJitterConfig.getConfig();
        assertEquals(CronJitterConfig.DEFAULT_CONFIG, config);
    }

    @Test
    @DisplayName("CronJitterConfig setConfig")
    void setConfig() {
        CronJitterConfig.Config newConfig = new CronJitterConfig.Config(
            0.2, 60000L, 30000L, 1000L, 15, 86400000L
        );

        CronJitterConfig.setConfig(newConfig);
        assertEquals(newConfig, CronJitterConfig.getConfig());
    }

    @Test
    @DisplayName("CronJitterConfig reset")
    void reset() {
        CronJitterConfig.Config newConfig = new CronJitterConfig.Config(
            0.5, 1000L, 500L, 100L, 5, 1000L
        );

        CronJitterConfig.setConfig(newConfig);
        assertEquals(newConfig, CronJitterConfig.getConfig());

        CronJitterConfig.reset();
        assertEquals(CronJitterConfig.DEFAULT_CONFIG, CronJitterConfig.getConfig());
    }
}