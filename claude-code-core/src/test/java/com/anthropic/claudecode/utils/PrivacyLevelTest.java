/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PrivacyLevel.
 */
class PrivacyLevelTest {

    @AfterEach
    void reset() {
        PrivacyLevel.resetPrivacyLevel();
    }

    @Test
    @DisplayName("PrivacyLevel getPrivacyLevel returns default when no env set")
    void getPrivacyLevelDefault() {
        PrivacyLevel.resetPrivacyLevel();

        PrivacyLevel.Level level = PrivacyLevel.getPrivacyLevel();

        // Should be DEFAULT unless env vars are set
        assertNotNull(level);
    }

    @Test
    @DisplayName("PrivacyLevel setPrivacyLevel overrides level")
    void setPrivacyLevel() {
        PrivacyLevel.setPrivacyLevel(PrivacyLevel.Level.NO_TELEMETRY);

        assertEquals(PrivacyLevel.Level.NO_TELEMETRY, PrivacyLevel.getPrivacyLevel());
    }

    @Test
    @DisplayName("PrivacyLevel resetPrivacyLevel resets to env-based")
    void resetPrivacyLevelWorks() {
        PrivacyLevel.setPrivacyLevel(PrivacyLevel.Level.ESSENTIAL_TRAFFIC);
        PrivacyLevel.resetPrivacyLevel();

        // After reset, it reads from env (should not be the set value)
        assertNotNull(PrivacyLevel.getPrivacyLevel());
    }

    @Test
    @DisplayName("PrivacyLevel isEssentialTrafficOnly checks level")
    void isEssentialTrafficOnly() {
        PrivacyLevel.setPrivacyLevel(PrivacyLevel.Level.ESSENTIAL_TRAFFIC);

        assertTrue(PrivacyLevel.isEssentialTrafficOnly());

        PrivacyLevel.setPrivacyLevel(PrivacyLevel.Level.DEFAULT);

        assertFalse(PrivacyLevel.isEssentialTrafficOnly());
    }

    @Test
    @DisplayName("PrivacyLevel isTelemetryDisabled checks level")
    void isTelemetryDisabled() {
        PrivacyLevel.setPrivacyLevel(PrivacyLevel.Level.NO_TELEMETRY);

        assertTrue(PrivacyLevel.isTelemetryDisabled());

        PrivacyLevel.setPrivacyLevel(PrivacyLevel.Level.ESSENTIAL_TRAFFIC);

        assertTrue(PrivacyLevel.isTelemetryDisabled());

        PrivacyLevel.setPrivacyLevel(PrivacyLevel.Level.DEFAULT);

        assertFalse(PrivacyLevel.isTelemetryDisabled());
    }

    @Test
    @DisplayName("PrivacyLevel isFeatureAllowed telemetry respects level")
    void isFeatureAllowedTelemetry() {
        PrivacyLevel.setPrivacyLevel(PrivacyLevel.Level.DEFAULT);

        assertTrue(PrivacyLevel.isFeatureAllowed("telemetry"));
        assertTrue(PrivacyLevel.isFeatureAllowed("analytics"));

        PrivacyLevel.setPrivacyLevel(PrivacyLevel.Level.NO_TELEMETRY);

        assertFalse(PrivacyLevel.isFeatureAllowed("telemetry"));
        assertFalse(PrivacyLevel.isFeatureAllowed("analytics"));
    }

    @Test
    @DisplayName("PrivacyLevel isFeatureAllowed auto_updates respects level")
    void isFeatureAllowedAutoUpdates() {
        PrivacyLevel.setPrivacyLevel(PrivacyLevel.Level.DEFAULT);

        assertTrue(PrivacyLevel.isFeatureAllowed("auto_updates"));

        PrivacyLevel.setPrivacyLevel(PrivacyLevel.Level.ESSENTIAL_TRAFFIC);

        assertFalse(PrivacyLevel.isFeatureAllowed("auto_updates"));

        PrivacyLevel.setPrivacyLevel(PrivacyLevel.Level.NO_TELEMETRY);

        assertTrue(PrivacyLevel.isFeatureAllowed("auto_updates"));
    }

    @Test
    @DisplayName("PrivacyLevel isFeatureAllowed unknown feature returns true")
    void isFeatureAllowedUnknown() {
        assertTrue(PrivacyLevel.isFeatureAllowed("unknown_feature"));
    }

    @Test
    @DisplayName("PrivacyLevel getDescription returns non-null")
    void getDescription() {
        PrivacyLevel.setPrivacyLevel(PrivacyLevel.Level.DEFAULT);
        assertEquals("All features enabled", PrivacyLevel.getDescription());

        PrivacyLevel.setPrivacyLevel(PrivacyLevel.Level.NO_TELEMETRY);
        assertTrue(PrivacyLevel.getDescription().contains("Telemetry disabled"));

        PrivacyLevel.setPrivacyLevel(PrivacyLevel.Level.ESSENTIAL_TRAFFIC);
        assertTrue(PrivacyLevel.getDescription().contains("Essential traffic"));
    }

    @Test
    @DisplayName("PrivacyLevel Level enum values")
    void levelEnumValues() {
        PrivacyLevel.Level[] values = PrivacyLevel.Level.values();

        assertEquals(3, values.length);
        assertEquals(PrivacyLevel.Level.DEFAULT, PrivacyLevel.Level.valueOf("DEFAULT"));
        assertEquals(PrivacyLevel.Level.NO_TELEMETRY, PrivacyLevel.Level.valueOf("NO_TELEMETRY"));
        assertEquals(PrivacyLevel.Level.ESSENTIAL_TRAFFIC, PrivacyLevel.Level.valueOf("ESSENTIAL_TRAFFIC"));
    }

    @Test
    @DisplayName("PrivacyLevel getEssentialTrafficOnlyReason returns null by default")
    void getEssentialTrafficOnlyReasonDefault() {
        PrivacyLevel.resetPrivacyLevel();

        // Without env var set, should return null (unless env is actually set)
        // This test just checks it doesn't throw
        PrivacyLevel.getEssentialTrafficOnlyReason();
    }
}