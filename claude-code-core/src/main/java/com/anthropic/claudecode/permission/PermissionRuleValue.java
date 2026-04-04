/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code types/permissions.ts
 */
package com.anthropic.claudecode.permission;

/**
 * Permission rule value - specifies which tool and optional content.
 */
public record PermissionRuleValue(String toolName, String ruleContent) {
    public PermissionRuleValue(String toolName) {
        this(toolName, null);
    }
}