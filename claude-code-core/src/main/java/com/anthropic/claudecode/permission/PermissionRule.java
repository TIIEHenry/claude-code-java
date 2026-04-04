/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code types/permissions.ts
 */
package com.anthropic.claudecode.permission;

/**
 * A permission rule with its source and behavior.
 */
public record PermissionRule(
    PermissionRuleSource source,
    PermissionBehavior ruleBehavior,
    PermissionRuleValue ruleValue
) {}