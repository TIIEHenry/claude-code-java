/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/permissions/autoModeState.ts
 */
package com.anthropic.claudecode.utils.permissions;

import java.util.concurrent.atomic.*;

/**
 * Auto mode state management.
 *
 * Tracks state for the auto mode classifier, including denial tracking
 * and interruption state.
 */
public final class AutoModeState {
    private AutoModeState() {}

    // State
    private static final AtomicInteger denialCount = new AtomicInteger(0);
    private static final AtomicInteger consecutiveDenials = new AtomicInteger(0);
    private static final AtomicBoolean interrupted = new AtomicBoolean(false);
    private static final AtomicBoolean enabled = new AtomicBoolean(false);
    private static final AtomicBoolean bypassEnabled = new AtomicBoolean(false);

    /**
     * Check if auto mode is enabled.
     */
    public static boolean isEnabled() {
        return enabled.get();
    }

    /**
     * Set auto mode enabled state.
     */
    public static void setEnabled(boolean value) {
        enabled.set(value);
    }

    /**
     * Check if bypass permissions is enabled.
     */
    public static boolean isBypassEnabled() {
        return bypassEnabled.get();
    }

    /**
     * Set bypass permissions state.
     */
    public static void setBypassEnabled(boolean value) {
        bypassEnabled.set(value);
    }

    /**
     * Check if auto mode was interrupted.
     */
    public static boolean isInterrupted() {
        return interrupted.get();
    }

    /**
     * Set interrupted state.
     */
    public static void setInterrupted(boolean value) {
        interrupted.set(value);
    }

    /**
     * Get denial count.
     */
    public static int getDenialCount() {
        return denialCount.get();
    }

    /**
     * Get consecutive denials count.
     */
    public static int getConsecutiveDenials() {
        return consecutiveDenials.get();
    }

    /**
     * Record a denial.
     */
    public static void recordDenial() {
        denialCount.incrementAndGet();
        consecutiveDenials.incrementAndGet();
    }

    /**
     * Record an allow (resets consecutive denials).
     */
    public static void recordAllow() {
        consecutiveDenials.set(0);
    }

    /**
     * Check if should fall back to interactive mode.
     * Triggers after too many consecutive denials.
     */
    public static boolean shouldFallbackToInteractive() {
        return consecutiveDenials.get() >= 3;
    }

    /**
     * Reset all state.
     */
    public static void reset() {
        denialCount.set(0);
        consecutiveDenials.set(0);
        interrupted.set(false);
        enabled.set(false);
        bypassEnabled.set(false);
    }

    /**
     * Get state summary.
     */
    public static AutoModeStateSnapshot getSnapshot() {
        return new AutoModeStateSnapshot(
            enabled.get(),
            bypassEnabled.get(),
            interrupted.get(),
            denialCount.get(),
            consecutiveDenials.get()
        );
    }

    /**
     * State snapshot record.
     */
    public record AutoModeStateSnapshot(
        boolean enabled,
        boolean bypassEnabled,
        boolean interrupted,
        int denialCount,
        int consecutiveDenials
    ) {}
}