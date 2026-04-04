/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code config constants
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Config constants for Claude Code.
 * This class must remain dependency-free to avoid circular dependencies.
 */
public final class ConfigConstants {
    private ConfigConstants() {}

    /**
     * Valid notification channels.
     */
    public static final List<String> NOTIFICATION_CHANNELS = List.of(
            "auto",
            "iterm2",
            "iterm2_with_bell",
            "terminal_bell",
            "kitty",
            "ghostty",
            "notifications_disabled"
    );

    /**
     * Valid editor modes.
     */
    public static final List<String> EDITOR_MODES = List.of(
            "normal",
            "vim"
    );

    /**
     * Valid teammate modes for spawning.
     */
    public static final List<String> TEAMMATE_MODES = List.of(
            "auto",
            "tmux",
            "in-process"
    );

    /**
     * Check if a notification channel is valid.
     */
    public static boolean isValidNotificationChannel(String channel) {
        return NOTIFICATION_CHANNELS.contains(channel);
    }

    /**
     * Check if an editor mode is valid.
     */
    public static boolean isValidEditorMode(String mode) {
        return EDITOR_MODES.contains(mode);
    }

    /**
     * Check if a teammate mode is valid.
     */
    public static boolean isValidTeammateMode(String mode) {
        return TEAMMATE_MODES.contains(mode);
    }

    /**
     * Get default notification channel.
     */
    public static String getDefaultNotificationChannel() {
        return "auto";
    }

    /**
     * Get default editor mode.
     */
    public static String getDefaultEditorMode() {
        return "normal";
    }

    /**
     * Get default teammate mode.
     */
    public static String getDefaultTeammateMode() {
        return "auto";
    }
}