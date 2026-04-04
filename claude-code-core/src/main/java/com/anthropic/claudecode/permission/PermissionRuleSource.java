/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code types/permissions.ts
 */
package com.anthropic.claudecode.permission;

import java.util.*;

/**
 * Source of a permission rule.
 */
public enum PermissionRuleSource {
    USER_SETTINGS("userSettings"),
    PROJECT_SETTINGS("projectSettings"),
    LOCAL_SETTINGS("localSettings"),
    FLAG_SETTINGS("flagSettings"),
    POLICY_SETTINGS("policySettings"),
    CLI_ARG("cliArg"),
    COMMAND("command"),
    SESSION("session");

    private final String value;

    PermissionRuleSource(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}