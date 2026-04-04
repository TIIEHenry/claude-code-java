/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Betas.
 */
class BetasTest {

    @BeforeEach
    void clearBetas() {
        // Clear any active betas before each test
        Betas.getActiveBetas().forEach(Betas::disable);
    }

    @Test
    @DisplayName("Betas enable activates beta")
    void enableWorks() {
        Betas.enable("test-beta");

        assertTrue(Betas.isActive("test-beta"));
    }

    @Test
    @DisplayName("Betas disable deactivates beta")
    void disableWorks() {
        Betas.enable("test-beta");
        Betas.disable("test-beta");

        assertFalse(Betas.isActive("test-beta"));
    }

    @Test
    @DisplayName("Betas isActive returns false for inactive beta")
    void isActiveFalse() {
        assertFalse(Betas.isActive("non-existent-beta"));
    }

    @Test
    @DisplayName("Betas getActiveBetas returns active betas")
    void getActiveBetasWorks() {
        Betas.enable("beta1");
        Betas.enable("beta2");

        Set<String> active = Betas.getActiveBetas();

        assertTrue(active.contains("beta1"));
        assertTrue(active.contains("beta2"));
    }

    @Test
    @DisplayName("Betas register registers feature")
    void registerWorks() {
        Betas.BetaFeature feature = new Betas.BetaFeature(
            "test-feature",
            "Test description",
            Betas.BetaStatus.INTERNAL,
            "10%",
            List.of("req1", "req2")
        );

        Betas.register(feature);

        assertEquals(feature, Betas.getFeature("test-feature"));
    }

    @Test
    @DisplayName("Betas getFeature returns null for non-existent")
    void getFeatureNull() {
        assertNull(Betas.getFeature("non-existent"));
    }

    @Test
    @DisplayName("Betas isEnabled checks feature status")
    void isEnabledWorks() {
        Betas.BetaFeature feature = new Betas.BetaFeature(
            "enabled-feature",
            "Test",
            Betas.BetaStatus.ENABLED,
            "100%",
            List.of()
        );
        Betas.register(feature);

        assertTrue(Betas.isEnabled("enabled-feature"));
    }

    @Test
    @DisplayName("Betas isEnabled returns false for disabled feature")
    void isEnabledFalse() {
        Betas.BetaFeature feature = new Betas.BetaFeature(
            "disabled-feature",
            "Test",
            Betas.BetaStatus.DISABLED,
            "0%",
            List.of()
        );
        Betas.register(feature);

        assertFalse(Betas.isEnabled("disabled-feature"));
    }

    @Test
    @DisplayName("Betas isEnabled returns true for active beta")
    void isEnabledActive() {
        // Register the feature first
        Betas.BetaFeature feature = new Betas.BetaFeature(
            "active-beta",
            "Test",
            Betas.BetaStatus.DISABLED,
            "0%",
            List.of()
        );
        Betas.register(feature);
        Betas.enable("active-beta");

        assertTrue(Betas.isEnabled("active-beta"));
    }

    @Test
    @DisplayName("Betas BetaFeature isEnabled works")
    void betaFeatureIsEnabled() {
        Betas.BetaFeature enabled = new Betas.BetaFeature(
            "test", "test", Betas.BetaStatus.ENABLED, "100%", List.of()
        );
        Betas.BetaFeature rollout = new Betas.BetaFeature(
            "test", "test", Betas.BetaStatus.ROLLOUT, "50%", List.of()
        );
        Betas.BetaFeature disabled = new Betas.BetaFeature(
            "test", "test", Betas.BetaStatus.DISABLED, "0%", List.of()
        );

        assertTrue(enabled.isEnabled());
        assertTrue(rollout.isEnabled());
        assertFalse(disabled.isEnabled());
    }

    @Test
    @DisplayName("Betas getBetaHeaders returns headers")
    void getBetaHeadersWorks() {
        Betas.enable("beta1");
        Betas.enable("beta2");

        List<String> headers = Betas.getBetaHeaders();

        assertTrue(headers.contains("anthropic-beta:beta1"));
        assertTrue(headers.contains("anthropic-beta:beta2"));
    }

    @Test
    @DisplayName("Betas parse parses beta string")
    void parseWorks() {
        Betas.BetaFeature feature = Betas.parse("test-beta:Test description");

        assertEquals("test-beta", feature.name());
        assertEquals("Test description", feature.description());
        assertEquals(Betas.BetaStatus.INTERNAL, feature.status());
    }

    @Test
    @DisplayName("Betas parse handles no description")
    void parseNoDescription() {
        Betas.BetaFeature feature = Betas.parse("test-beta");

        assertEquals("test-beta", feature.name());
        assertEquals("", feature.description());
    }

    @Test
    @DisplayName("Betas BetaStatus enum values")
    void betaStatusEnum() {
        Betas.BetaStatus[] values = Betas.BetaStatus.values();

        assertEquals(5, values.length);
        assertEquals(Betas.BetaStatus.DISABLED, Betas.BetaStatus.valueOf("DISABLED"));
        assertEquals(Betas.BetaStatus.INTERNAL, Betas.BetaStatus.valueOf("INTERNAL"));
        assertEquals(Betas.BetaStatus.ROLLOUT, Betas.BetaStatus.valueOf("ROLLOUT"));
        assertEquals(Betas.BetaStatus.ENABLED, Betas.BetaStatus.valueOf("ENABLED"));
        assertEquals(Betas.BetaStatus.DEPRECATED, Betas.BetaStatus.valueOf("DEPRECATED"));
    }

    @Test
    @DisplayName("Betas BetaFeature record accessors")
    void betaFeatureRecord() {
        Betas.BetaFeature feature = new Betas.BetaFeature(
            "name",
            "description",
            Betas.BetaStatus.ENABLED,
            "100%",
            List.of("req1")
        );

        assertEquals("name", feature.name());
        assertEquals("description", feature.description());
        assertEquals(Betas.BetaStatus.ENABLED, feature.status());
        assertEquals("100%", feature.rolloutPercentage());
        assertEquals(List.of("req1"), feature.requirements());
    }

    @Test
    @DisplayName("Betas getAllFeatures returns features")
    void getAllFeaturesWorks() {
        Betas.BetaFeature feature = new Betas.BetaFeature(
            "feature1",
            "Test",
            Betas.BetaStatus.ENABLED,
            "100%",
            List.of()
        );
        Betas.register(feature);

        assertTrue(Betas.getAllFeatures().containsKey("feature1"));
    }
}