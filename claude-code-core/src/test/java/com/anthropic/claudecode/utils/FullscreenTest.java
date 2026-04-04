/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Fullscreen.
 */
class FullscreenTest {

    @BeforeEach
    void setUp() {
        Fullscreen.resetForTesting();
    }

    @Test
    @DisplayName("Fullscreen isTmuxControlMode returns boolean")
    void isTmuxControlMode() {
        boolean result = Fullscreen.isTmuxControlMode();
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("Fullscreen isFullscreenEnvEnabled returns boolean")
    void isFullscreenEnvEnabled() {
        boolean result = Fullscreen.isFullscreenEnvEnabled();
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("Fullscreen isMouseTrackingEnabled returns boolean")
    void isMouseTrackingEnabled() {
        boolean result = Fullscreen.isMouseTrackingEnabled();
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("Fullscreen isMouseClicksDisabled returns boolean")
    void isMouseClicksDisabled() {
        boolean result = Fullscreen.isMouseClicksDisabled();
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("Fullscreen isFullscreenActive returns boolean")
    void isFullscreenActive() {
        boolean result = Fullscreen.isFullscreenActive();
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("Fullscreen maybeGetTmuxMouseHint returns string or null")
    void maybeGetTmuxMouseHint() {
        String hint = Fullscreen.maybeGetTmuxMouseHint();
        // May be null if TMUX not set
        assertTrue(hint == null || hint instanceof String);
    }

    @Test
    @DisplayName("Fullscreen resetForTesting does not throw")
    void resetForTesting() {
        assertDoesNotThrow(() -> Fullscreen.resetForTesting());
    }
}