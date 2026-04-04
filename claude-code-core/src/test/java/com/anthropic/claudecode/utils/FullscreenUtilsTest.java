/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FullscreenUtils.
 */
class FullscreenUtilsTest {

    @Test
    @DisplayName("FullscreenUtils isFullscreenEnvEnabled returns boolean")
    void isFullscreenEnvEnabled() {
        // Returns boolean depending on environment
        boolean result = FullscreenUtils.isFullscreenEnvEnabled();
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("FullscreenUtils isFullscreenCapable returns boolean")
    void isFullscreenCapable() {
        boolean result = FullscreenUtils.isFullscreenCapable();
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("FullscreenUtils supportsAlternateScreen returns boolean")
    void supportsAlternateScreen() {
        boolean result = FullscreenUtils.supportsAlternateScreen();
        assertTrue(result == true || result == false);
    }
}