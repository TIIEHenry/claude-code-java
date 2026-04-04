/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code types/permissions.ts
 */
package com.anthropic.claudecode.utils.permissions;

import java.util.*;

/**
 * Permission result types.
 *
 * Sealed interface hierarchy representing different permission decisions.
 */
public sealed interface PermissionResult permits
    PermissionResult.Allow,
    PermissionResult.Deny,
    PermissionResult.Ask,
    PermissionResult.Passthrough {

    /**
     * Get the behavior type.
     */
    Behavior behavior();

    /**
     * Get the message.
     */
    String message();

    /**
     * Permission behavior enum.
     */
    enum Behavior {
        ALLOW("allow"),
        DENY("deny"),
        ASK("ask"),
        PASSTHROUGH("passthrough");

        private final String id;

        Behavior(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    /**
     * Allow result - tool can proceed.
     */
    record Allow(
        Map<String, Object> updatedInput,
        PermissionDecisionReason decisionReason
    ) implements PermissionResult {
        @Override
        public Behavior behavior() { return Behavior.ALLOW; }

        @Override
        public String message() { return null; }

        public static Allow of() {
            return new Allow(null, null);
        }

        public static Allow withInput(Map<String, Object> input) {
            return new Allow(input, null);
        }
    }

    /**
     * Deny result - tool cannot proceed.
     */
    record Deny(
        String message,
        PermissionDecisionReason decisionReason,
        boolean interrupt
    ) implements PermissionResult {
        @Override
        public Behavior behavior() { return Behavior.DENY; }

        public static Deny of(String message) {
            return new Deny(message, null, false);
        }

        public static Deny withReason(String message, PermissionDecisionReason reason) {
            return new Deny(message, reason, false);
        }
    }

    /**
     * Ask result - need user confirmation.
     */
    record Ask(
        String message,
        PermissionDecisionReason decisionReason,
        List<PermissionSuggestion> suggestions
    ) implements PermissionResult {
        @Override
        public Behavior behavior() { return Behavior.ASK; }

        public static Ask of(String message) {
            return new Ask(message, null, null);
        }

        public static Ask withReason(String message, PermissionDecisionReason reason) {
            return new Ask(message, reason, null);
        }

        public static Ask withSuggestions(String message, List<PermissionSuggestion> suggestions) {
            return new Ask(message, null, suggestions);
        }
    }

    /**
     * Passthrough result - no rule matched, proceed to next check.
     */
    record Passthrough(
        String message,
        PermissionDecisionReason decisionReason
    ) implements PermissionResult {
        @Override
        public Behavior behavior() { return Behavior.PASSTHROUGH; }

        public static Passthrough of(String message) {
            return new Passthrough(message, null);
        }
    }
}

/**
 * Permission decision reason - why a decision was made.
 */
sealed interface PermissionDecisionReason permits
    PermissionDecisionReason.Rule,
    PermissionDecisionReason.Hook,
    PermissionDecisionReason.Classifier,
    PermissionDecisionReason.Mode,
    PermissionDecisionReason.SafetyCheck,
    PermissionDecisionReason.SubcommandResults,
    PermissionDecisionReason.PermissionPromptTool,
    PermissionDecisionReason.SandboxOverride,
    PermissionDecisionReason.WorkingDir,
    PermissionDecisionReason.AsyncAgent,
    PermissionDecisionReason.Other {

    /**
     * Rule-based decision.
     */
    record Rule(PermissionRule rule) implements PermissionDecisionReason {}

    /**
     * Hook-based decision.
     */
    record Hook(String hookName, String reason) implements PermissionDecisionReason {}

    /**
     * Classifier-based decision.
     */
    record Classifier(String classifier, String reason) implements PermissionDecisionReason {}

    /**
     * Mode-based decision.
     */
    record Mode(String mode) implements PermissionDecisionReason {}

    /**
     * Safety check decision.
     */
    record SafetyCheck(String reason, boolean classifierApprovable) implements PermissionDecisionReason {}

    /**
     * Subcommand results decision.
     */
    record SubcommandResults(Map<String, PermissionResult> reasons) implements PermissionDecisionReason {}

    /**
     * Permission prompt tool decision.
     */
    record PermissionPromptTool(String permissionPromptToolName) implements PermissionDecisionReason {}

    /**
     * Sandbox override decision.
     */
    record SandboxOverride() implements PermissionDecisionReason {}

    /**
     * Working directory decision.
     */
    record WorkingDir(String reason) implements PermissionDecisionReason {}

    /**
     * Async agent decision.
     */
    record AsyncAgent(String reason) implements PermissionDecisionReason {}

    /**
     * Other decision.
     */
    record Other(String reason) implements PermissionDecisionReason {}
}

/**
 * Permission suggestion for the user.
 */
record PermissionSuggestion(
    String type,
    String description,
    Map<String, Object> data
) {
    public static PermissionSuggestion allowTool(String toolName) {
        return new PermissionSuggestion("allow", "Allow " + toolName, Map.of("tool", toolName));
    }

    public static PermissionSuggestion denyTool(String toolName) {
        return new PermissionSuggestion("deny", "Deny " + toolName, Map.of("tool", toolName));
    }
}