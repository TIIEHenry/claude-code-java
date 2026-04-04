/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code auto mode denials tracking
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Tracks commands recently denied by the auto mode classifier.
 * Populated from useCanUseTool, read from RecentDenialsTab in /permissions.
 */
public final class AutoModeDenials {
    private AutoModeDenials() {}

    /**
     * Auto mode denial record.
     */
    public record AutoModeDenial(
            String toolName,
            String display,      // Human-readable description
            String reason,
            long timestamp
    ) {}

    private static final int MAX_DENIALS = 20;
    private static List<AutoModeDenial> denials = new ArrayList<>();

    /**
     * Record an auto mode denial.
     */
    public static void recordAutoModeDenial(AutoModeDenial denial) {
        // Check if transcript classifier is enabled
        if (!isTranscriptClassifierEnabled()) return;

        denials.add(0, denial);
        if (denials.size() > MAX_DENIALS) {
            denials = new ArrayList<>(denials.subList(0, MAX_DENIALS));
        }
    }

    /**
     * Get all recorded denials.
     */
    public static List<AutoModeDenial> getAutoModeDenials() {
        return Collections.unmodifiableList(denials);
    }

    /**
     * Clear all denials.
     */
    public static void clearDenials() {
        denials.clear();
    }

    /**
     * Get denial count.
     */
    public static int getDenialCount() {
        return denials.size();
    }

    /**
     * Check if transcript classifier is enabled.
     */
    private static boolean isTranscriptClassifierEnabled() {
        return Boolean.parseBoolean(System.getenv("CLAUDE_CODE_TRANSCRIPT_CLASSIFIER"));
    }
}