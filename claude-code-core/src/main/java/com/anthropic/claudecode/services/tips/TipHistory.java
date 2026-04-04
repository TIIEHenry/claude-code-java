/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/tips/tipHistory.ts
 */
package com.anthropic.claudecode.services.tips;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages tip history for determining when tips were last shown.
 */
public final class TipHistory {
    private TipHistory() {}

    private static final Map<String, Integer> sessionsSinceLastShown = new ConcurrentHashMap<>();
    private static int currentSessionCount = 0;

    /**
     * Increment the session counter (called at session start).
     */
    public static void incrementSessionCount() {
        currentSessionCount++;
        // Increment all tip counters
        sessionsSinceLastShown.replaceAll((k, v) -> v + 1);
    }

    /**
     * Get the number of sessions since a tip was last shown.
     * Returns Integer.MAX_VALUE if never shown.
     */
    public static int getSessionsSinceLastShown(String tipId) {
        return sessionsSinceLastShown.getOrDefault(tipId, Integer.MAX_VALUE);
    }

    /**
     * Record that a tip was shown in the current session.
     */
    public static void recordTipShown(String tipId) {
        sessionsSinceLastShown.put(tipId, 0);
    }

    /**
     * Reset all tip history (for testing).
     */
    public static void reset() {
        sessionsSinceLastShown.clear();
        currentSessionCount = 0;
    }
}