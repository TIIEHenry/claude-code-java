/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code fullscreen utilities
 */
package com.anthropic.claudecode.utils;

/**
 * Fullscreen mode utilities.
 */
public final class Fullscreen {
    private Fullscreen() {}

    private static Boolean tmuxControlModeProbed = null;
    private static boolean loggedTmuxCcDisable = false;
    private static boolean checkedTmuxMouseHint = false;

    /**
     * Check if running in tmux control mode (-CC).
     */
    public static boolean isTmuxControlMode() {
        if (tmuxControlModeProbed != null) {
            return tmuxControlModeProbed;
        }

        // Environment heuristic
        String tmux = System.getenv("TMUX");
        if (tmux == null || tmux.isEmpty()) {
            tmuxControlModeProbed = false;
            return false;
        }

        String termProgram = System.getenv("TERM_PROGRAM");
        if (!"iTerm.app".equals(termProgram)) {
            tmuxControlModeProbed = false;
            return false;
        }

        String term = System.getenv("TERM");
        if (term != null && (term.startsWith("screen") || term.startsWith("tmux"))) {
            tmuxControlModeProbed = false;
            return false;
        }

        tmuxControlModeProbed = true;
        return true;
    }

    /**
     * Check if fullscreen env is enabled.
     */
    public static boolean isFullscreenEnvEnabled() {
        String noFlicker = System.getenv("CLAUDE_CODE_NO_FLICKER");

        // Explicit opt-out
        if ("false".equalsIgnoreCase(noFlicker) || "0".equals(noFlicker)) {
            return false;
        }

        // Explicit opt-in
        if ("true".equalsIgnoreCase(noFlicker) || "1".equals(noFlicker)) {
            return true;
        }

        // Auto-disable under tmux -CC
        if (isTmuxControlMode()) {
            if (!loggedTmuxCcDisable) {
                loggedTmuxCcDisable = true;
            }
            return false;
        }

        // Ant default
        return "ant".equals(System.getenv("USER_TYPE"));
    }

    /**
     * Check if mouse tracking is enabled.
     */
    public static boolean isMouseTrackingEnabled() {
        String disableMouse = System.getenv("CLAUDE_CODE_DISABLE_MOUSE");
        return !isTruthy(disableMouse);
    }

    /**
     * Check if mouse clicks are disabled.
     */
    public static boolean isMouseClicksDisabled() {
        String disableClicks = System.getenv("CLAUDE_CODE_DISABLE_MOUSE_CLICKS");
        return isTruthy(disableClicks);
    }

    /**
     * Check if fullscreen is active.
     */
    public static boolean isFullscreenActive() {
        return isInteractive() && isFullscreenEnvEnabled();
    }

    /**
     * Get tmux mouse hint.
     */
    public static String maybeGetTmuxMouseHint() {
        if (System.getenv("TMUX") == null) return null;
        if (!isFullscreenActive() || isTmuxControlMode()) return null;
        if (checkedTmuxMouseHint) return null;

        checkedTmuxMouseHint = true;
        return "tmux detected · scroll with PgUp/PgDn · or add 'set -g mouse on' to ~/.tmux.conf for wheel scroll";
    }

    /**
     * Reset for testing.
     */
    public static void resetForTesting() {
        tmuxControlModeProbed = null;
        loggedTmuxCcDisable = false;
        checkedTmuxMouseHint = false;
    }

    private static boolean isTruthy(String value) {
        if (value == null) return false;
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    private static boolean isInteractive() {
        // Would be set by bootstrap state in real impl
        String interactive = System.getenv("CLAUDE_INTERACTIVE");
        return isTruthy(interactive);
    }
}