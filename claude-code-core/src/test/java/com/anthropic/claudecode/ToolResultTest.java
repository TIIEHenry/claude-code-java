/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import com.anthropic.claudecode.message.Message;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ToolResult.
 */
class ToolResultTest {

    @Test
    @DisplayName("ToolResult of creates simple result")
    void ofSimple() {
        ToolResult<String> result = ToolResult.of("data");

        assertEquals("data", result.data());
        assertTrue(result.newMessages().isEmpty());
        assertNull(result.contextModifier());
        assertNull(result.mcpMeta());
    }

    @Test
    @DisplayName("ToolResult of with messages")
    void ofWithMessages() {
        List<Message> messages = List.of();
        ToolResult<Integer> result = ToolResult.of(42, messages);

        assertEquals(42, result.data());
        assertEquals(messages, result.newMessages());
        assertNull(result.contextModifier());
    }

    @Test
    @DisplayName("ToolResult of with context modifier")
    void ofWithContextModifier() {
        ToolResult<String> result = ToolResult.of("data", List.of(), ctx -> ctx);

        assertEquals("data", result.data());
        assertNotNull(result.contextModifier());
    }

    @Test
    @DisplayName("ToolResult McpMeta record works")
    void mcpMetaWorks() {
        Map<String, Object> meta = Map.of("key", "value");
        Map<String, Object> content = Map.of("data", 123);

        ToolResult.McpMeta mcpMeta = new ToolResult.McpMeta(meta, content);

        assertEquals(meta, mcpMeta.meta());
        assertEquals(content, mcpMeta.structuredContent());
    }

    @Test
    @DisplayName("ToolResult with null data")
    void nullData() {
        ToolResult<Object> result = ToolResult.of(null);

        assertNull(result.data());
        assertTrue(result.newMessages().isEmpty());
    }

    @Test
    @DisplayName("ToolResult with complex data")
    void complexData() {
        Map<String, Object> data = Map.of(
            "files", List.of("a.txt", "b.txt"),
            "count", 2
        );

        ToolResult<Map<String, Object>> result = ToolResult.of(data);

        assertEquals(data, result.data());
        assertEquals(2, result.data().get("count"));
    }
}