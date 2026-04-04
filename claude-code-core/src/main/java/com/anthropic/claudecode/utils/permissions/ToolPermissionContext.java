/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.permissions;

import java.util.*;

/**
 * Tool permission context - public wrapper for permission context.
 */
public record ToolPermissionContext(
    String mode,
    Map<String, List<String>> alwaysAllowRules,
    Map<String, List<String>> alwaysDenyRules,
    Map<String, List<String>> alwaysAskRules,
    Map<String, String> additionalWorkingDirectories,
    boolean isBypassPermissionsModeAvailable,
    boolean shouldAvoidPermissionPrompts
) {
    public static ToolPermissionContext createDefault() {
        return new ToolPermissionContext(
            "default",
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            false,
            false
        );
    }

    public static ToolPermissionContext empty() {
        return createDefault();
    }

    /**
     * Merge with another context (other takes precedence).
     */
    public ToolPermissionContext merge(ToolPermissionContext other) {
        if (other == null) return this;

        Map<String, List<String>> mergedAllow = new HashMap<>(this.alwaysAllowRules);
        mergedAllow.putAll(other.alwaysAllowRules);

        Map<String, List<String>> mergedDeny = new HashMap<>(this.alwaysDenyRules);
        mergedDeny.putAll(other.alwaysDenyRules);

        Map<String, List<String>> mergedAsk = new HashMap<>(this.alwaysAskRules);
        mergedAsk.putAll(other.alwaysAskRules);

        Map<String, String> mergedDirs = new HashMap<>(this.additionalWorkingDirectories);
        mergedDirs.putAll(other.additionalWorkingDirectories);

        return new ToolPermissionContext(
            other.mode != null ? other.mode : this.mode,
            mergedAllow,
            mergedDeny,
            mergedAsk,
            mergedDirs,
            other.isBypassPermissionsModeAvailable,
            other.shouldAvoidPermissionPrompts
        );
    }
}