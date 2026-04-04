/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code permission mode utilities
 */
package com.anthropic.claudecode.utils.permissions;

import java.util.*;

/**
 * Permission mode types and utilities.
 */
public final class PermissionModeUtils {
    private PermissionModeUtils() {}

    /**
     * Permission modes.
     */
    public enum PermissionMode {
        DEFAULT("Default", "Default", "", "text"),
        PLAN("Plan Mode", "Plan", "⏸", "planMode"),
        ACCEPT_EDITS("Accept edits", "Accept", "⏵⏵", "autoAccept"),
        BYPASS_PERMISSIONS("Bypass Permissions", "Bypass", "⏵⏵", "error"),
        DONT_ASK("Don't Ask", "DontAsk", "⏵⏵", "error"),
        AUTO("Auto mode", "Auto", "⏵⏵", "warning"),
        BUBBLE("Bubble", "Bubble", "", "text");

        private final String title;
        private final String shortTitle;
        private final String symbol;
        private final String color;

        PermissionMode(String title, String shortTitle, String symbol, String color) {
            this.title = title;
            this.shortTitle = shortTitle;
            this.symbol = symbol;
            this.color = color;
        }

        public String getTitle() {
            return title;
        }

        public String getShortTitle() {
            return shortTitle;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getColor() {
            return color;
        }
    }

    /**
     * External permission modes (exposed to users).
     */
    public enum ExternalPermissionMode {
        DEFAULT, PLAN, ACCEPT_EDITS, BYPASS_PERMISSIONS, DONT_ASK
    }

    // All permission modes
    public static final List<PermissionMode> PERMISSION_MODES = List.of(
            PermissionMode.DEFAULT,
            PermissionMode.PLAN,
            PermissionMode.ACCEPT_EDITS,
            PermissionMode.BYPASS_PERMISSIONS,
            PermissionMode.DONT_ASK,
            PermissionMode.AUTO,
            PermissionMode.BUBBLE
    );

    // External permission modes
    public static final List<ExternalPermissionMode> EXTERNAL_PERMISSION_MODES = List.of(
            ExternalPermissionMode.DEFAULT,
            ExternalPermissionMode.PLAN,
            ExternalPermissionMode.ACCEPT_EDITS,
            ExternalPermissionMode.BYPASS_PERMISSIONS,
            ExternalPermissionMode.DONT_ASK
    );

    /**
     * Check if mode is an external permission mode.
     */
    public static boolean isExternalPermissionMode(PermissionMode mode) {
        // External users can't have auto or bubble
        String userType = System.getenv("USER_TYPE");
        if (!"ant".equals(userType)) {
            return true;
        }
        return mode != PermissionMode.AUTO && mode != PermissionMode.BUBBLE;
    }

    /**
     * Convert to external permission mode.
     */
    public static ExternalPermissionMode toExternalPermissionMode(PermissionMode mode) {
        return switch (mode) {
            case PLAN -> ExternalPermissionMode.PLAN;
            case ACCEPT_EDITS -> ExternalPermissionMode.ACCEPT_EDITS;
            case BYPASS_PERMISSIONS -> ExternalPermissionMode.BYPASS_PERMISSIONS;
            case DONT_ASK -> ExternalPermissionMode.DONT_ASK;
            default -> ExternalPermissionMode.DEFAULT;
        };
    }

    /**
     * Parse permission mode from string.
     */
    public static PermissionMode permissionModeFromString(String str) {
        if (str == null || str.isEmpty()) {
            return PermissionMode.DEFAULT;
        }
        try {
            return PermissionMode.valueOf(str.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return PermissionMode.DEFAULT;
        }
    }

    /**
     * Get title for permission mode.
     */
    public static String permissionModeTitle(PermissionMode mode) {
        return mode.getTitle();
    }

    /**
     * Get short title for permission mode.
     */
    public static String permissionModeShortTitle(PermissionMode mode) {
        return mode.getShortTitle();
    }

    /**
     * Get symbol for permission mode.
     */
    public static String permissionModeSymbol(PermissionMode mode) {
        return mode.getSymbol();
    }

    /**
     * Get color for permission mode.
     */
    public static String getModeColor(PermissionMode mode) {
        return mode.getColor();
    }

    /**
     * Check if mode is default.
     */
    public static boolean isDefaultMode(PermissionMode mode) {
        return mode == null || mode == PermissionMode.DEFAULT;
    }

    /**
     * Check if mode allows auto-accept of edits.
     */
    public static boolean isAutoAcceptMode(PermissionMode mode) {
        return mode == PermissionMode.ACCEPT_EDITS ||
               mode == PermissionMode.BYPASS_PERMISSIONS ||
               mode == PermissionMode.DONT_ASK;
    }

    /**
     * Check if mode bypasses all permission checks.
     */
    public static boolean isBypassMode(PermissionMode mode) {
        return mode == PermissionMode.BYPASS_PERMISSIONS;
    }

    /**
     * Check if in plan mode.
     */
    public static boolean isPlanMode(PermissionMode mode) {
        return mode == PermissionMode.PLAN;
    }
}