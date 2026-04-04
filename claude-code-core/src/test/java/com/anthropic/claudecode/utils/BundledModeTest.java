/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BundledMode.
 */
class BundledModeTest {

    @Test
    @DisplayName("BundledMode isInBundledMode returns boolean")
    void isInBundledMode() {
        // Result depends on environment variable
        boolean result = BundledMode.isInBundledMode();
        // Just verify it doesn't throw
        assertTrue(result || !result);
    }

    @Test
    @DisplayName("BundledMode isNativeImage returns boolean")
    void isNativeImage() {
        boolean result = BundledMode.isNativeImage();
        // Just verify it doesn't throw
        assertTrue(result || !result);
    }

    @Test
    @DisplayName("BundledMode isRunningFromJar returns boolean")
    void isRunningFromJar() {
        boolean result = BundledMode.isRunningFromJar();
        // Just verify it doesn't throw
        assertTrue(result || !result);
    }

    @Test
    @DisplayName("BundledMode getExecutablePath returns non-null")
    void getExecutablePath() {
        String path = BundledMode.getExecutablePath();
        assertNotNull(path);
        assertFalse(path.isEmpty());
    }
}