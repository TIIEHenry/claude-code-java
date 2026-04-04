/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/autoDream
 */
package com.anthropic.claudecode.services.autodream;

import java.util.*;
import java.util.concurrent.*;

/**
 * Auto dream config - Configuration for background memory consolidation.
 */
public final class AutoDreamConfig {
    private int minHours;
    private int minSessions;
    private boolean enabled;

    public AutoDreamConfig() {
        this.minHours = 24;
        this.minSessions = 5;
        this.enabled = false;
    }

    /**
     * Get minimum hours between consolidations.
     */
    public int getMinHours() {
        return minHours;
    }

    /**
     * Set minimum hours between consolidations.
     */
    public void setMinHours(int minHours) {
        this.minHours = Math.max(1, minHours);
    }

    /**
     * Get minimum sessions before consolidation.
     */
    public int getMinSessions() {
        return minSessions;
    }

    /**
     * Set minimum sessions before consolidation.
     */
    public void setMinSessions(int minSessions) {
        this.minSessions = Math.max(1, minSessions);
    }

    /**
     * Check if auto dream is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable auto dream.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Create default config.
     */
    public static AutoDreamConfig defaults() {
        return new AutoDreamConfig();
    }

    /**
     * Create config from feature flags.
     */
    public static AutoDreamConfig fromFeatureFlags(Map<String, Object> flags) {
        AutoDreamConfig config = new AutoDreamConfig();

        if (flags.containsKey("minHours")) {
            Object value = flags.get("minHours");
            if (value instanceof Number) {
                config.setMinHours(((Number) value).intValue());
            }
        }

        if (flags.containsKey("minSessions")) {
            Object value = flags.get("minSessions");
            if (value instanceof Number) {
                config.setMinSessions(((Number) value).intValue());
            }
        }

        if (flags.containsKey("enabled")) {
            Object value = flags.get("enabled");
            if (value instanceof Boolean) {
                config.setEnabled((Boolean) value);
            }
        }

        return config;
    }
}