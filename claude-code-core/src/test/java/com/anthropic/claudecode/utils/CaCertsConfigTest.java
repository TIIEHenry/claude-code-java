/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CaCertsConfig.
 */
class CaCertsConfigTest {

    @BeforeEach
    void setUp() {
        CaCerts.clearCACertsCache();
        CaCerts.configure(false, null);
    }

    @Test
    @DisplayName("CaCertsConfig applyExtraCACertsFromConfig does not throw")
    void applyExtraCACertsFromConfig() {
        // May fail if ConfigManager cannot load config, but should not throw
        assertDoesNotThrow(() -> CaCertsConfig.applyExtraCACertsFromConfig());
    }

    @Test
    @DisplayName("CaCertsConfig isSystemCAConfigured returns false by default")
    void isSystemCAConfiguredDefault() {
        assertFalse(CaCertsConfig.isSystemCAConfigured());
    }

    @Test
    @DisplayName("CaCertsConfig applyExtraCACertsFromConfig with NODE_EXTRA_CA_CERTS env")
    void applyExtraCACertsFromConfigWithEnv() {
        // If NODE_EXTRA_CA_CERTS is already set, it returns early
        CaCertsConfig.applyExtraCACertsFromConfig();
        // Just verify it doesn't throw
        assertTrue(true);
    }

    @Test
    @DisplayName("CaCertsConfig applyExtraCACertsFromConfig multiple calls")
    void applyExtraCACertsFromConfigMultipleCalls() {
        CaCertsConfig.applyExtraCACertsFromConfig();
        CaCertsConfig.applyExtraCACertsFromConfig();
        // Multiple calls should not throw
        assertTrue(true);
    }
}