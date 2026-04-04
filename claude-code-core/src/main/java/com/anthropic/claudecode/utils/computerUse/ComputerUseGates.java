/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/computerUse/gates
 */
package com.anthropic.claudecode.utils.computerUse;

import java.util.*;
import java.util.concurrent.*;

/**
 * Computer use gates - Permission gates for computer use actions.
 */
public final class ComputerUseGates {
    private static volatile boolean gateEnabled = true;
    private static volatile int maxActionsPerSession = 100;
    private static volatile int actionCount = 0;

    /**
     * Gate result record.
     */
    public record GateResult(
        boolean allowed,
        String reason,
        GateType gateType
    ) {
        public static GateResult allow() {
            return new GateResult(true, null, null);
        }

        public static GateResult deny(String reason, GateType gateType) {
            return new GateResult(false, reason, gateType);
        }
    }

    /**
     * Gate type enum.
     */
    public enum GateType {
        SESSION_LIMIT,
        RATE_LIMIT,
        PERMISSION,
        SAFETY,
        USER_CONFIRMATION,
        SYSTEM_LOCK
    }

    /**
     * Check gate for action.
     */
    public static GateResult checkGate(ComputerUseCommon.ComputerAction action) {
        if (!gateEnabled) {
            return GateResult.deny("Computer use gates disabled", GateType.PERMISSION);
        }

        // Check session limit
        if (actionCount >= maxActionsPerSession) {
            return GateResult.deny(
                "Session limit reached: " + maxActionsPerSession,
                GateType.SESSION_LIMIT
            );
        }

        // Check safety for dangerous actions
        if (isDangerousAction(action)) {
            return GateResult.deny(
                "Action requires user confirmation",
                GateType.USER_CONFIRMATION
            );
        }

        actionCount++;
        return GateResult.allow();
    }

    /**
     * Check if action is dangerous.
     */
    private static boolean isDangerousAction(ComputerUseCommon.ComputerAction action) {
        // Check for dangerous key combinations
        if (action.action() == ComputerUseCommon.ActionType.KEY) {
            List<String> keys = action.keys();
            if (keys != null) {
                // Ctrl+Alt+Del, etc.
                if (keys.contains("Control") && keys.contains("Alt") && keys.contains("Delete")) {
                    return true;
                }
                // Command+Q (macOS quit)
                if (keys.contains("Meta") && keys.contains("q")) {
                    return true;
                }
            }
        }

        // Right-click might be context-sensitive
        if (action.action() == ComputerUseCommon.ActionType.RIGHT_CLICK) {
            return true;
        }

        return false;
    }

    /**
     * Enable/disable gates.
     */
    public static void setGateEnabled(boolean enabled) {
        gateEnabled = enabled;
    }

    /**
     * Set max actions per session.
     */
    public static void setMaxActionsPerSession(int max) {
        maxActionsPerSession = max;
    }

    /**
     * Reset action count.
     */
    public static void resetActionCount() {
        actionCount = 0;
    }

    /**
     * Get action count.
     */
    public static int getActionCount() {
        return actionCount;
    }

    /**
     * Gate configuration record.
     */
    public record GateConfig(
        boolean enabled,
        int maxActionsPerSession,
        int maxActionsPerMinute,
        boolean requireConfirmationForDangerous,
        Set<ComputerUseCommon.ActionType> blockedActions
    ) {
        public static GateConfig defaultConfig() {
            return new GateConfig(
                true,
                100,
                10,
                true,
                Collections.emptySet()
            );
        }
    }
}