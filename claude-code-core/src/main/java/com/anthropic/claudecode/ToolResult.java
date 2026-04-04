/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code ToolResult type
 */
package com.anthropic.claudecode;

import com.anthropic.claudecode.message.Message;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Result from a tool execution.
 *
 * <p>Corresponds to ToolResult in Tool.ts.
 */
public record ToolResult<O>(
        O data,
        List<Message> newMessages,
        Function<ToolUseContext, ToolUseContext> contextModifier,
        McpMeta mcpMeta
) {
    /**
     * Create a simple result with just data.
     */
    public static <O> ToolResult<O> of(O data) {
        return new ToolResult<>(data, List.of(), null, null);
    }

    /**
     * Create a result with data and new messages.
     */
    public static <O> ToolResult<O> of(O data, List<Message> newMessages) {
        return new ToolResult<>(data, newMessages, null, null);
    }

    /**
     * Create a result with all fields.
     */
    public static <O> ToolResult<O> of(O data, List<Message> newMessages,
                                        Function<ToolUseContext, ToolUseContext> contextModifier) {
        return new ToolResult<>(data, newMessages, contextModifier, null);
    }

    /**
     * MCP protocol metadata to pass through to SDK consumers.
     */
    public record McpMeta(
            Map<String, Object> meta,
            Map<String, Object> structuredContent
    ) {}
}