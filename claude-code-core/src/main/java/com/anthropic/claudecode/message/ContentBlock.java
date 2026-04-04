/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.message;

import java.util.Map;

/**
 * Content block types for messages.
 */
public sealed interface ContentBlock permits
    ContentBlock.Text,
    ContentBlock.Image,
    ContentBlock.ToolUse,
    ContentBlock.ToolResult,
    ContentBlock.Thinking {

    String type();

    /**
     * Text content block.
     */
    record Text(String type, String text) implements ContentBlock {
        public Text(String text) {
            this("text", text);
        }
    }

    /**
     * Image content block.
     */
    record Image(String type, Source source) implements ContentBlock {
        public Image(Source source) {
            this("image", source);
        }

        public record Source(String type, String mediaType, String data) {}
    }

    /**
     * Tool use content block.
     */
    record ToolUse(String type, String id, String name, Map<String, Object> input) implements ContentBlock {
        public ToolUse(String id, String name, Map<String, Object> input) {
            this("tool_use", id, name, input);
        }
    }

    /**
     * Tool result content block.
     */
    record ToolResult(String type, String toolUseId, Object content, boolean isError) implements ContentBlock {
        public ToolResult(String toolUseId, Object content) {
            this("tool_result", toolUseId, content, false);
        }

        public ToolResult(String toolUseId, Object content, boolean isError) {
            this("tool_result", toolUseId, content, isError);
        }
    }

    /**
     * Thinking content block.
     */
    record Thinking(String type, String thinking) implements ContentBlock {
        public Thinking(String thinking) {
            this("thinking", thinking);
        }
    }
}