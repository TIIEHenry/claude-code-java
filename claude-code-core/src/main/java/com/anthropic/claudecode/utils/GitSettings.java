/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code git settings utilities
 */
package com.anthropic.claudecode.utils;

import com.anthropic.claudecode.services.config.Settings;

/**
 * Git-related behaviors that depend on user settings.
 */
public final class GitSettings {
    private GitSettings() {}

    /**
     * Check if git instructions should be included.
     */
    public static boolean shouldIncludeGitInstructions() {
        String envVal = System.getenv("CLAUDE_CODE_DISABLE_GIT_INSTRUCTIONS");
        if (EnvUtils.isEnvTruthy(envVal)) return false;
        if (EnvUtils.isEnvDefinedFalsy(envVal)) return true;
        return Settings.getInstance().getAllSettings()
            .getOrDefault("includeGitInstructions", true)
            .equals(true);
    }
}