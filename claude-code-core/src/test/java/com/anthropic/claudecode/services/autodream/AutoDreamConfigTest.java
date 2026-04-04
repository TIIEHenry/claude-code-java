/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.autodream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AutoDreamConfig.
 */
class AutoDreamConfigTest {

    @Test
    @DisplayName("AutoDreamConfig defaults")
    void defaults() {
        AutoDreamConfig config = AutoDreamConfig.defaults();

        assertEquals(24, config.getMinHours());
        assertEquals(5, config.getMinSessions());
        assertFalse(config.isEnabled());
    }

    @Test
    @DisplayName("AutoDreamConfig default constructor")
    void defaultConstructor() {
        AutoDreamConfig config = new AutoDreamConfig();

        assertEquals(24, config.getMinHours());
        assertEquals(5, config.getMinSessions());
        assertFalse(config.isEnabled());
    }

    @Test
    @DisplayName("AutoDreamConfig setMinHours")
    void setMinHours() {
        AutoDreamConfig config = new AutoDreamConfig();
        config.setMinHours(48);

        assertEquals(48, config.getMinHours());
    }

    @Test
    @DisplayName("AutoDreamConfig setMinHours minimum is 1")
    void setMinHoursMinimum() {
        AutoDreamConfig config = new AutoDreamConfig();
        config.setMinHours(0);

        assertEquals(1, config.getMinHours());

        config.setMinHours(-5);
        assertEquals(1, config.getMinHours());
    }

    @Test
    @DisplayName("AutoDreamConfig setMinSessions")
    void setMinSessions() {
        AutoDreamConfig config = new AutoDreamConfig();
        config.setMinSessions(10);

        assertEquals(10, config.getMinSessions());
    }

    @Test
    @DisplayName("AutoDreamConfig setMinSessions minimum is 1")
    void setMinSessionsMinimum() {
        AutoDreamConfig config = new AutoDreamConfig();
        config.setMinSessions(0);

        assertEquals(1, config.getMinSessions());

        config.setMinSessions(-5);
        assertEquals(1, config.getMinSessions());
    }

    @Test
    @DisplayName("AutoDreamConfig setEnabled")
    void setEnabled() {
        AutoDreamConfig config = new AutoDreamConfig();
        config.setEnabled(true);

        assertTrue(config.isEnabled());

        config.setEnabled(false);
        assertFalse(config.isEnabled());
    }

    @Test
    @DisplayName("AutoDreamConfig fromFeatureFlags empty")
    void fromFeatureFlagsEmpty() {
        AutoDreamConfig config = AutoDreamConfig.fromFeatureFlags(Map.of());

        assertEquals(24, config.getMinHours());
        assertEquals(5, config.getMinSessions());
        assertFalse(config.isEnabled());
    }

    @Test
    @DisplayName("AutoDreamConfig fromFeatureFlags with values")
    void fromFeatureFlagsWithValues() {
        Map<String, Object> flags = Map.of(
            "minHours", 12,
            "minSessions", 3,
            "enabled", true
        );

        AutoDreamConfig config = AutoDreamConfig.fromFeatureFlags(flags);

        assertEquals(12, config.getMinHours());
        assertEquals(3, config.getMinSessions());
        assertTrue(config.isEnabled());
    }

    @Test
    @DisplayName("AutoDreamConfig fromFeatureFlags ignores non-numeric hours")
    void fromFeatureFlagsNonNumericHours() {
        Map<String, Object> flags = Map.of(
            "minHours", "not-a-number"
        );

        AutoDreamConfig config = AutoDreamConfig.fromFeatureFlags(flags);

        assertEquals(24, config.getMinHours()); // default
    }

    @Test
    @DisplayName("AutoDreamConfig fromFeatureFlags ignores non-numeric sessions")
    void fromFeatureFlagsNonNumericSessions() {
        Map<String, Object> flags = Map.of(
            "minSessions", "not-a-number"
        );

        AutoDreamConfig config = AutoDreamConfig.fromFeatureFlags(flags);

        assertEquals(5, config.getMinSessions()); // default
    }

    @Test
    @DisplayName("AutoDreamConfig fromFeatureFlags ignores non-boolean enabled")
    void fromFeatureFlagsNonBooleanEnabled() {
        Map<String, Object> flags = Map.of(
            "enabled", "yes"
        );

        AutoDreamConfig config = AutoDreamConfig.fromFeatureFlags(flags);

        assertFalse(config.isEnabled()); // default
    }
}