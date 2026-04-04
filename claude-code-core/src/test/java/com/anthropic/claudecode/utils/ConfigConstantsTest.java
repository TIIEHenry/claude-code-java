/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigConstants.
 */
class ConfigConstantsTest {

    @Test
    @DisplayName("ConfigConstants NOTIFICATION_CHANNELS contains expected values")
    void notificationChannels() {
        assertTrue(ConfigConstants.NOTIFICATION_CHANNELS.contains("auto"));
        assertTrue(ConfigConstants.NOTIFICATION_CHANNELS.contains("iterm2"));
        assertTrue(ConfigConstants.NOTIFICATION_CHANNELS.contains("iterm2_with_bell"));
        assertTrue(ConfigConstants.NOTIFICATION_CHANNELS.contains("terminal_bell"));
        assertTrue(ConfigConstants.NOTIFICATION_CHANNELS.contains("kitty"));
        assertTrue(ConfigConstants.NOTIFICATION_CHANNELS.contains("ghostty"));
        assertTrue(ConfigConstants.NOTIFICATION_CHANNELS.contains("notifications_disabled"));
        assertEquals(7, ConfigConstants.NOTIFICATION_CHANNELS.size());
    }

    @Test
    @DisplayName("ConfigConstants EDITOR_MODES contains expected values")
    void editorModes() {
        assertTrue(ConfigConstants.EDITOR_MODES.contains("normal"));
        assertTrue(ConfigConstants.EDITOR_MODES.contains("vim"));
        assertEquals(2, ConfigConstants.EDITOR_MODES.size());
    }

    @Test
    @DisplayName("ConfigConstants TEAMMATE_MODES contains expected values")
    void teammateModes() {
        assertTrue(ConfigConstants.TEAMMATE_MODES.contains("auto"));
        assertTrue(ConfigConstants.TEAMMATE_MODES.contains("tmux"));
        assertTrue(ConfigConstants.TEAMMATE_MODES.contains("in-process"));
        assertEquals(3, ConfigConstants.TEAMMATE_MODES.size());
    }

    @Test
    @DisplayName("ConfigConstants isValidNotificationChannel true for valid")
    void isValidNotificationChannelTrue() {
        assertTrue(ConfigConstants.isValidNotificationChannel("auto"));
        assertTrue(ConfigConstants.isValidNotificationChannel("iterm2"));
        assertTrue(ConfigConstants.isValidNotificationChannel("kitty"));
    }

    @Test
    @DisplayName("ConfigConstants isValidNotificationChannel false for invalid")
    void isValidNotificationChannelFalse() {
        assertFalse(ConfigConstants.isValidNotificationChannel("invalid"));
        assertFalse(ConfigConstants.isValidNotificationChannel(""));
        // Note: null causes NPE in List.contains()
    }

    @Test
    @DisplayName("ConfigConstants isValidEditorMode true for valid")
    void isValidEditorModeTrue() {
        assertTrue(ConfigConstants.isValidEditorMode("normal"));
        assertTrue(ConfigConstants.isValidEditorMode("vim"));
    }

    @Test
    @DisplayName("ConfigConstants isValidEditorMode false for invalid")
    void isValidEditorModeFalse() {
        assertFalse(ConfigConstants.isValidEditorMode("emacs"));
        assertFalse(ConfigConstants.isValidEditorMode(""));
        // Note: null causes NPE in List.contains()
    }

    @Test
    @DisplayName("ConfigConstants isValidTeammateMode true for valid")
    void isValidTeammateModeTrue() {
        assertTrue(ConfigConstants.isValidTeammateMode("auto"));
        assertTrue(ConfigConstants.isValidTeammateMode("tmux"));
        assertTrue(ConfigConstants.isValidTeammateMode("in-process"));
    }

    @Test
    @DisplayName("ConfigConstants isValidTeammateMode false for invalid")
    void isValidTeammateModeFalse() {
        assertFalse(ConfigConstants.isValidTeammateMode("invalid"));
        assertFalse(ConfigConstants.isValidTeammateMode(""));
        // Note: null causes NPE in List.contains()
    }

    @Test
    @DisplayName("ConfigConstants getDefaultNotificationChannel returns auto")
    void getDefaultNotificationChannel() {
        assertEquals("auto", ConfigConstants.getDefaultNotificationChannel());
    }

    @Test
    @DisplayName("ConfigConstants getDefaultEditorMode returns normal")
    void getDefaultEditorMode() {
        assertEquals("normal", ConfigConstants.getDefaultEditorMode());
    }

    @Test
    @DisplayName("ConfigConstants getDefaultTeammateMode returns auto")
    void getDefaultTeammateMode() {
        assertEquals("auto", ConfigConstants.getDefaultTeammateMode());
    }
}