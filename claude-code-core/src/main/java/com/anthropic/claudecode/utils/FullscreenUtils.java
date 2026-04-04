/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code fullscreen utilities
 */
package com.anthropic.claudecode.utils;

/**
 * Fullscreen environment utilities.
 */
public final class FullscreenUtils {
    private FullscreenUtils() {}

    /**
     * Check if fullscreen environment is enabled.
     * This checks for environment variables that indicate fullscreen mode.
     */
    public static boolean isFullscreenEnvEnabled() {
        String fullscreenEnv = System.getenv("CLAUDE_CODE_FULLSCREEN");
        String termProgram = System.getenv("TERM_PROGRAM");

        // Check explicit fullscreen flag
        if (fullscreenEnv != null && isTruthy(fullscreenEnv)) {
            return true;
        }

        // Check if running in certain terminal programs that support fullscreen
        if (termProgram != null) {
            return termProgram.equals("iTerm.app") ||
                   termProgram.equals("Apple_Terminal") ||
                   termProgram.equals("vscode");
        }

        return false;
    }

    /**
     * Check if running in a fullscreen-capable terminal.
     */
    public static boolean isFullscreenCapable() {
        return System.console() != null || isFullscreenEnvEnabled();
    }

    /**
     * Check if alternate screen buffer is supported.
     */
    public static boolean supportsAlternateScreen() {
        String term = System.getenv("TERM");
        if (term == null) {
            return false;
        }
        // Most modern terminals support alternate screen
        return term.startsWith("xterm") ||
               term.contains("256color") ||
               term.equals("screen") ||
               term.equals("tmux");
    }

    private static boolean isTruthy(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String normalized = value.toLowerCase().trim();
        return "1".equals(normalized) ||
               "true".equals(normalized) ||
               "yes".equals(normalized) ||
               "on".equals(normalized);
    }
}