/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code types/permissions.ts
 */
package com.anthropic.claudecode;

import com.anthropic.claudecode.permission.PermissionResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Permission checking function interface.
 *
 * <p>Corresponds to CanUseToolFn in hooks/useCanUseTool.tsx.
 */
@FunctionalInterface
public interface CanUseToolFn {

    /**
     * Check if a tool can be used with the given input.
     *
     * @param tool            The tool to check
     * @param input           Tool input
     * @param context         Tool use context
     * @param assistantMessage The assistant message containing the tool call
     * @param toolUseId       Unique ID for this tool use
     * @return Permission result
     */
    CompletableFuture<PermissionResult> check(
            Tool<?, ?, ?> tool,
            Object input,
            ToolUseContext context,
            AssistantMessage assistantMessage,
            String toolUseId
    );

    /**
     * Apply method - alias for check() for compatibility.
     *
     * @param tool            The tool to check
     * @param input           Tool input
     * @param context         Tool use context
     * @param assistantMessage The assistant message containing the tool call
     * @param toolUseId       Unique ID for this tool use
     * @return Permission result
     */
    default CompletableFuture<PermissionResult> apply(
            Tool<?, ?, ?> tool,
            Object input,
            ToolUseContext context,
            AssistantMessage assistantMessage,
            String toolUseId) {
        return check(tool, input, context, assistantMessage, toolUseId);
    }

    /**
     * Result indicating the tool use is allowed.
     */
    record AllowResult(Object input) {}

    /**
     * Result indicating the tool use is denied.
     */
    record DenyResult(String message, String behavior) {}
}