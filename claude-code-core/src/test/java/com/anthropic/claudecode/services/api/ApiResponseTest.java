/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ApiResponse.
 */
class ApiResponseTest {

    @Test
    @DisplayName("ApiResponse record with all fields")
    void recordFields() {
        List<ApiResponse.ContentBlock> content = List.of(
            new ApiResponse.ContentBlock.TextBlock("Hello world")
        );
        ApiResponse.Usage usage = new ApiResponse.Usage(100, 50, 10, 20);

        ApiResponse response = new ApiResponse(
            "msg-123",
            "message",
            "assistant",
            content,
            "claude-sonnet-4-6",
            "end_turn",
            100,
            50,
            usage
        );

        assertEquals("msg-123", response.id());
        assertEquals("message", response.type());
        assertEquals("assistant", response.role());
        assertEquals(content, response.content());
        assertEquals("claude-sonnet-4-6", response.model());
        assertEquals("end_turn", response.stopReason());
        assertEquals(100, response.inputTokens());
        assertEquals(50, response.outputTokens());
        assertEquals(usage, response.usage());
    }

    @Test
    @DisplayName("ApiResponse ContentBlock text block")
    void contentBlockText() {
        ApiResponse.ContentBlock.TextBlock block = new ApiResponse.ContentBlock.TextBlock(
            "This is the response text"
        );

        assertEquals("text", block.type());
        assertEquals("This is the response text", block.text());
    }

    @Test
    @DisplayName("ApiResponse ContentBlock tool use block")
    void contentBlockToolUse() {
        Map<String, Object> input = Map.of("file_path", "/src/Main.java");
        ApiResponse.ContentBlock.ToolUseBlock block = new ApiResponse.ContentBlock.ToolUseBlock(
            "toolu_123",
            "Read",
            input
        );

        assertEquals("tool_use", block.type());
        assertEquals("toolu_123", block.id());
        assertEquals("Read", block.name());
        assertEquals(input, block.input());
    }

    @Test
    @DisplayName("ApiResponse Usage with all fields")
    void usageAllFields() {
        ApiResponse.Usage usage = new ApiResponse.Usage(1000, 500, 100, 200);

        assertEquals(1000, usage.inputTokens());
        assertEquals(500, usage.outputTokens());
        assertEquals(100, usage.cacheCreationInputTokens());
        assertEquals(200, usage.cacheReadInputTokens());
    }

    @Test
    @DisplayName("ApiResponse Usage with two parameters")
    void usageTwoParams() {
        ApiResponse.Usage usage = new ApiResponse.Usage(100, 50);

        assertEquals(100, usage.inputTokens());
        assertEquals(50, usage.outputTokens());
        assertEquals(0, usage.cacheCreationInputTokens());
        assertEquals(0, usage.cacheReadInputTokens());
    }

    @Test
    @DisplayName("ApiResponse with empty content")
    void emptyContent() {
        ApiResponse response = new ApiResponse(
            "msg-1",
            "message",
            "assistant",
            List.of(),
            "claude-opus-4-6",
            "end_turn",
            0,
            0,
            new ApiResponse.Usage(0, 0)
        );

        assertTrue(response.content().isEmpty());
    }

    @Test
    @DisplayName("ApiResponse with multiple content blocks")
    void multipleContentBlocks() {
        List<ApiResponse.ContentBlock> content = List.of(
            new ApiResponse.ContentBlock.TextBlock("Let me help"),
            new ApiResponse.ContentBlock.ToolUseBlock("toolu_1", "Read", Map.of("file_path", "/test")),
            new ApiResponse.ContentBlock.TextBlock("Here's the result")
        );

        ApiResponse response = new ApiResponse(
            "msg-1",
            "message",
            "assistant",
            content,
            "claude-sonnet-4-6",
            "end_turn",
            100,
            200,
            new ApiResponse.Usage(100, 200)
        );

        assertEquals(3, response.content().size());
        assertEquals("text", response.content().get(0).type());
        assertEquals("tool_use", response.content().get(1).type());
        assertEquals("text", response.content().get(2).type());

        // Verify TextBlock content
        ApiResponse.ContentBlock.TextBlock textBlock = (ApiResponse.ContentBlock.TextBlock) content.get(0);
        assertEquals("Let me help", textBlock.text());

        // Verify ToolUseBlock content
        ApiResponse.ContentBlock.ToolUseBlock toolBlock = (ApiResponse.ContentBlock.ToolUseBlock) content.get(1);
        assertEquals("toolu_1", toolBlock.id());
        assertEquals("Read", toolBlock.name());
    }

    @Test
    @DisplayName("ContentBlock sealed interface allows instanceof checks")
    void contentBlockInstanceOf() {
        ApiResponse.ContentBlock textBlock = new ApiResponse.ContentBlock.TextBlock("text content");
        ApiResponse.ContentBlock toolBlock = new ApiResponse.ContentBlock.ToolUseBlock("id", "Tool", Map.of());

        assertTrue(textBlock instanceof ApiResponse.ContentBlock.TextBlock);
        assertTrue(toolBlock instanceof ApiResponse.ContentBlock.ToolUseBlock);

        if (textBlock instanceof ApiResponse.ContentBlock.TextBlock t) {
            assertEquals("text content", t.text());
        }

        if (toolBlock instanceof ApiResponse.ContentBlock.ToolUseBlock t) {
            assertEquals("Tool", t.name());
            assertEquals("id", t.id());
        }
    }
}