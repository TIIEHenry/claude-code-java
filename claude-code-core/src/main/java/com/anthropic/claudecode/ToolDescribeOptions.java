/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode;

import com.anthropic.claudecode.utils.permissions.ToolPermissionContext;

import java.util.List;

/**
 * Options for describing a tool.
 */
public record ToolDescribeOptions(
        boolean isNonInteractiveSession,
        ToolPermissionContext toolPermissionContext,
        List<Tool<?, ?, ?>> tools
) {
    public static ToolDescribeOptions empty() {
        return new ToolDescribeOptions(false, null, List.of());
    }
}