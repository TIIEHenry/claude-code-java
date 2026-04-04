/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.compact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TimeBasedMCConfig.
 */
class TimeBasedMCConfigTest {

    @BeforeEach
    void setUp() {
        TimeBasedMCConfig.reset();
    }

    @Test
    @DisplayName("TimeBasedMCConfig defaults")
    void defaults() {
        TimeBasedMCConfig defaults = TimeBasedMCConfig.defaults();

        assertFalse(defaults.isEnabled());
        assertEquals(60, defaults.getGapThresholdMinutes());
        assertEquals(5, defaults.getKeepRecent());
    }

    @Test
    @DisplayName("TimeBasedMCConfig constructor with parameters")
    void constructorWithParams() {
        TimeBasedMCConfig config = new TimeBasedMCConfig(true, 30, 10);

        assertTrue(config.isEnabled());
        assertEquals(30, config.getGapThresholdMinutes());
        assertEquals(10, config.getKeepRecent());
    }

    @Test
    @DisplayName("TimeBasedMCConfig default constructor")
    void defaultConstructor() {
        TimeBasedMCConfig config = new TimeBasedMCConfig();

        assertFalse(config.isEnabled());
        assertEquals(60, config.getGapThresholdMinutes());
        assertEquals(5, config.getKeepRecent());
    }

    @Test
    @DisplayName("TimeBasedMCConfig getConfig returns current config")
    void getConfig() {
        TimeBasedMCConfig config = TimeBasedMCConfig.getConfig();

        assertNotNull(config);
        assertEquals(TimeBasedMCConfig.defaults().isEnabled(), config.isEnabled());
    }

    @Test
    @DisplayName("TimeBasedMCConfig setConfig updates config")
    void setConfig() {
        TimeBasedMCConfig newConfig = new TimeBasedMCConfig(true, 45, 8);
        TimeBasedMCConfig.setConfig(newConfig);

        TimeBasedMCConfig current = TimeBasedMCConfig.getConfig();
        assertTrue(current.isEnabled());
        assertEquals(45, current.getGapThresholdMinutes());
        assertEquals(8, current.getKeepRecent());
    }

    @Test
    @DisplayName("TimeBasedMCConfig reset restores defaults")
    void reset() {
        TimeBasedMCConfig.setConfig(new TimeBasedMCConfig(true, 120, 20));

        TimeBasedMCConfig.reset();

        TimeBasedMCConfig current = TimeBasedMCConfig.getConfig();
        assertFalse(current.isEnabled());
        assertEquals(60, current.getGapThresholdMinutes());
        assertEquals(5, current.getKeepRecent());
    }

    @Test
    @DisplayName("TimeBasedMCConfig Config record")
    void configRecord() {
        TimeBasedMCConfig.Config config = new TimeBasedMCConfig.Config(true, 30, 10);

        assertTrue(config.enabled());
        assertEquals(30, config.gapThresholdMinutes());
        assertEquals(10, config.keepRecent());
    }

    @Test
    @DisplayName("TimeBasedMCConfig Config defaults")
    void configDefaults() {
        TimeBasedMCConfig.Config defaults = TimeBasedMCConfig.Config.defaults();

        assertFalse(defaults.enabled());
        assertEquals(60, defaults.gapThresholdMinutes());
        assertEquals(5, defaults.keepRecent());
    }

    @Test
    @DisplayName("TimeBasedMCConfig ConfigSnapshot record")
    void configSnapshotRecord() {
        TimeBasedMCConfig.ConfigSnapshot snapshot = new TimeBasedMCConfig.ConfigSnapshot(
            true, 45, 7
        );

        assertTrue(snapshot.enabled());
        assertEquals(45, snapshot.gapThresholdMinutes());
        assertEquals(7, snapshot.keepRecent());
    }

    @Test
    @DisplayName("TimeBasedMCConfig ConfigSnapshot format")
    void configSnapshotFormat() {
        TimeBasedMCConfig.ConfigSnapshot snapshot = new TimeBasedMCConfig.ConfigSnapshot(
            true, 45, 7
        );

        String formatted = snapshot.format();

        assertTrue(formatted.contains("TimeBasedMCConfig"));
        assertTrue(formatted.contains("enabled=true"));
        assertTrue(formatted.contains("gapThreshold=45m"));
        assertTrue(formatted.contains("keepRecent=7"));
    }

    @Test
    @DisplayName("TimeBasedMCConfig getSnapshot returns snapshot")
    void getSnapshot() {
        TimeBasedMCConfig config = new TimeBasedMCConfig(true, 30, 8);
        TimeBasedMCConfig.ConfigSnapshot snapshot = config.getSnapshot();

        assertTrue(snapshot.enabled());
        assertEquals(30, snapshot.gapThresholdMinutes());
        assertEquals(8, snapshot.keepRecent());
    }
}