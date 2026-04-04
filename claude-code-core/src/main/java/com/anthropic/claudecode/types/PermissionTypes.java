/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code types/permissions
 */
package com.anthropic.claudecode.types;

import java.time.Instant;
import java.util.*;

/**
 * Permission types - Permission type definitions.
 */
public final class PermissionTypes {

    /**
     * Permission mode enum.
     */
    public enum PermissionMode {
        DEFAULT("default"),
        ACCEPT_EDITS("acceptEdits"),
        BYPASS_PERMISSIONS("bypassPermissions"),
        DONT_ASK("dontAsk"),
        PLAN("plan"),
        AUTO("auto"),
        BUBBLE("bubble");

        private final String value;

        PermissionMode(String value) {
            this.value = value;
        }

        public String getValue() { return value; }

        public static PermissionMode fromValue(String value) {
            for (PermissionMode mode : values()) {
                if (mode.value.equals(value)) return mode;
            }
            return DEFAULT;
        }
    }

    /**
     * Permission action enum.
     */
    public enum PermissionAction {
        ALLOW,
        DENY,
        ALLOW_ALWAYS,
        DENY_ALWAYS,
        ASK_ALWAYS
    }

    /**
     * Permission source enum.
     */
    public enum PermissionSource {
        USER,
        PROJECT,
        SYSTEM,
        DEFAULT,
        PLUGIN
    }

    /**
     * Permission result sealed interface.
     * Uses non-generic sealed interface with generic implementing classes.
     */
    public sealed interface PermissionResult permits Allow, Deny, Ask {
        boolean isAllowed();
        boolean isDenied();
        boolean needsAsk();
    }

    /**
     * Allow result.
     */
    public final class Allow<T> implements PermissionResult {
        private final T data;
        private final String reason;

        public Allow(T data) { this(data, null); }
        public Allow(T data, String reason) {
            this.data = data;
            this.reason = reason;
        }

        public T data() { return data; }
        public String reason() { return reason; }

        @Override public boolean isAllowed() { return true; }
        @Override public boolean isDenied() { return false; }
        @Override public boolean needsAsk() { return false; }
    }

    /**
     * Deny result.
     */
    public final class Deny implements PermissionResult {
        private final String reason;
        private final boolean permanent;

        public Deny(String reason) { this(reason, false); }
        public Deny(String reason, boolean permanent) {
            this.reason = reason;
            this.permanent = permanent;
        }

        public String reason() { return reason; }
        public boolean permanent() { return permanent; }

        @Override public boolean isAllowed() { return false; }
        @Override public boolean isDenied() { return true; }
        @Override public boolean needsAsk() { return false; }
    }

    /**
     * Ask result.
     */
    public final class Ask<T> implements PermissionResult {
        private final String message;
        private final List<PermissionOption> options;
        private final T context;

        public Ask(String message, List<PermissionOption> options, T context) {
            this.message = message;
            this.options = options;
            this.context = context;
        }

        public String message() { return message; }
        public List<PermissionOption> options() { return options; }
        public T context() { return context; }

        @Override public boolean isAllowed() { return false; }
        @Override public boolean isDenied() { return false; }
        @Override public boolean needsAsk() { return true; }
    }

    /**
     * Permission option record.
     */
    public record PermissionOption(
        String id,
        String label,
        String description,
        PermissionAction action,
        boolean isDefault
    ) {
        public static PermissionOption allow() {
            return new PermissionOption("allow", "Allow", "Allow this action", PermissionAction.ALLOW, false);
        }

        public static PermissionOption deny() {
            return new PermissionOption("deny", "Deny", "Deny this action", PermissionAction.DENY, false);
        }

        public static PermissionOption allowAlways() {
            return new PermissionOption("allow_always", "Always Allow", "Always allow this action", PermissionAction.ALLOW_ALWAYS, false);
        }
    }

    /**
     * Permission rule record.
     */
    public record PermissionRule(
        String id,
        String pattern,
        PermissionAction action,
        String description,
        String source,
        boolean isDefault,
        Instant createdAt
    ) {
        public boolean matches(String input) {
            if (pattern.equals("*")) return true;
            if (pattern.endsWith("*")) {
                return input.startsWith(pattern.substring(0, pattern.length() - 1));
            }
            if (pattern.startsWith("*")) {
                return input.endsWith(pattern.substring(1));
            }
            return input.equals(pattern);
        }
    }

    /**
     * Permission check context record.
     */
    public record PermissionContext(
        String toolName,
        String operation,
        String resource,
        Map<String, Object> params,
        String sessionId,
        String userId
    ) {
        public static PermissionContext forTool(String toolName, Map<String, Object> params) {
            return new PermissionContext(toolName, null, null, params, null, null);
        }

        public static PermissionContext forFile(String operation, String path) {
            return new PermissionContext("file", operation, path, new HashMap<>(), null, null);
        }
    }

    /**
     * Permission decision record.
     */
    public record PermissionDecision(
        String id,
        String ruleId,
        PermissionAction action,
        String reason,
        Instant decidedAt,
        String decidedBy
    ) {}
}