/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/permissions/PermissionRule.ts
 */
package com.anthropic.claudecode.utils.permissions;

import java.util.*;

/**
 * Permission rule types.
 *
 * A permission rule consists of a source, behavior, and value.
 */
public record PermissionRule(
    PermissionRuleSource source,
    PermissionBehavior ruleBehavior,
    PermissionRuleValue ruleValue
) {
    /**
     * Create a rule from a string.
     */
    public static PermissionRule fromString(
        PermissionRuleSource source,
        PermissionBehavior behavior,
        String ruleString
    ) {
        PermissionRuleValue value = PermissionRuleValue.fromString(ruleString);
        return new PermissionRule(source, behavior, value);
    }
}

/**
 * Permission rule source enum.
 */
enum PermissionRuleSource {
    FLAG("flag"),
    LOCAL("local"),
    PROJECT("project"),
    USER("user"),
    POLICY("policy"),
    CLI_ARG("cliArg"),
    COMMAND("command"),
    SESSION("session");

    private final String id;

    PermissionRuleSource(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static PermissionRuleSource fromId(String id) {
        for (PermissionRuleSource source : values()) {
            if (source.id.equals(id)) {
                return source;
            }
        }
        return CLI_ARG;
    }
}

/**
 * Permission behavior enum.
 */
enum PermissionBehavior {
    ALLOW("allow"),
    DENY("deny"),
    ASK("ask");

    private final String id;

    PermissionBehavior(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static PermissionBehavior fromId(String id) {
        for (PermissionBehavior behavior : values()) {
            if (behavior.id.equals(id)) {
                return behavior;
            }
        }
        return ASK;
    }
}

/**
 * Permission rule value - the actual rule content.
 */
record PermissionRuleValue(
    String toolName,
    String ruleContent
) {
    /**
     * Parse a rule string like "Bash" or "Bash(npm:*)" into a rule value.
     */
    public static PermissionRuleValue fromString(String ruleString) {
        if (ruleString == null || ruleString.isEmpty()) {
            return new PermissionRuleValue(null, null);
        }

        // Check for content pattern: Tool(content)
        int parenStart = ruleString.indexOf('(');
        int parenEnd = ruleString.lastIndexOf(')');

        if (parenStart >= 0 && parenEnd > parenStart) {
            String toolName = ruleString.substring(0, parenStart);
            String content = ruleString.substring(parenStart + 1, parenEnd);
            return new PermissionRuleValue(toolName, content);
        }

        // Simple tool name
        return new PermissionRuleValue(ruleString, null);
    }

    /**
     * Convert to string representation.
     */
    public String toRuleString() {
        if (ruleContent == null) {
            return toolName;
        }
        return toolName + "(" + ruleContent + ")";
    }

    /**
     * Check if this rule matches a tool name.
     */
    public boolean matchesTool(String targetToolName) {
        if (toolName == null) return false;
        return toolName.equals(targetToolName);
    }

    /**
     * Check if this rule has content.
     */
    public boolean hasContent() {
        return ruleContent != null && !ruleContent.isEmpty();
    }

    /**
     * Check if this is a wildcard rule.
     */
    public boolean isWildcard() {
        return ruleContent != null && ruleContent.equals("*");
    }
}