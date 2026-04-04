/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api
 */
package com.anthropic.claudecode.services.api;

import java.util.*;

/**
 * API Response.
 */
public record ApiResponse(
    String id,
    String type,
    String role,
    List<ContentBlock> content,
    String model,
    String stopReason,
    int inputTokens,
    int outputTokens,
    Usage usage
) {
    /**
     * Content block - sealed interface for different content types.
     */
    public sealed interface ContentBlock permits ContentBlock.TextBlock, ContentBlock.ToolUseBlock {
        String type();

        record TextBlock(String text) implements ContentBlock {
            @Override public String type() { return "text"; }
        }

        record ToolUseBlock(String id, String name, Map<String, Object> input) implements ContentBlock {
            @Override public String type() { return "tool_use"; }
        }
    }

    public record Usage(
        int inputTokens,
        int outputTokens,
        int cacheCreationInputTokens,
        int cacheReadInputTokens
    ) {
        public Usage(int inputTokens, int outputTokens) {
            this(inputTokens, outputTokens, 0, 0);
        }
    }
}