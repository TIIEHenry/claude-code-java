/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code bootstrap utilities.
 */
package com.anthropic.claudecode.bootstrap;

import java.util.UUID;

/**
 * Bootstrap utilities for session initialization.
 */
public final class BootstrapUtils {
    private BootstrapUtils() {}

    /**
     * Generate a new session ID.
     */
    public static String generateSessionId() {
        return "session_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    /**
     * Generate a new agent ID.
     */
    public static String generateAgentId(String label) {
        String hex = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        if (label != null && !label.isEmpty()) {
            return "a" + label.toLowerCase().replaceAll("[^a-z0-9]", "") + "-" + hex;
        }
        return "a" + hex;
    }

    /**
     * Initialize the application state.
     */
    public static void initialize(String cwd, boolean isInteractive) {
        AppState state = AppState.getInstance();
        state.setOriginalCwd(cwd);
        state.setProjectRoot(cwd);
        state.setCwd(cwd);
        state.setInteractive(isInteractive);
        state.setSessionId(generateSessionId());
    }

    /**
     * Shutdown and cleanup.
     */
    public static void shutdown() {
        // Cleanup resources
        AppState.resetState();
    }
}