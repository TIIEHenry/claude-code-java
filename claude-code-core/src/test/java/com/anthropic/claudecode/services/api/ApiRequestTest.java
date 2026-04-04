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
 * Tests for ApiRequest.
 */
class ApiRequestTest {

    @Test
    @DisplayName("ApiRequest record with all fields")
    void recordFields() {
        List<Map<String, Object>> messages = List.of(
            Map.of("role", "user", "content", "Hello")
        );
        List<Map<String, Object>> tools = List.of(
            Map.of("name", "test_tool")
        );

        ApiRequest request = new ApiRequest(
            "claude-opus-4-6",
            messages,
            8192,
            0.7,
            tools,
            "You are helpful",
            true
        );

        assertEquals("claude-opus-4-6", request.model());
        assertEquals(messages, request.messages());
        assertEquals(8192, request.maxTokens());
        assertEquals(0.7, request.temperature());
        assertEquals(tools, request.tools());
        assertEquals("You are helpful", request.system());
        assertTrue(request.stream());
    }

    @Test
    @DisplayName("ApiRequest record with minimal fields")
    void recordMinimal() {
        ApiRequest request = new ApiRequest(
            "claude-sonnet-4-6",
            List.of(),
            4096,
            null,
            null,
            null,
            false
        );

        assertEquals("claude-sonnet-4-6", request.model());
        assertTrue(request.messages().isEmpty());
        assertNull(request.temperature());
        assertNull(request.tools());
        assertNull(request.system());
        assertFalse(request.stream());
    }

    @Test
    @DisplayName("ApiRequest builder creates request")
    void builder() {
        ApiRequest request = ApiRequest.builder()
            .model("claude-opus-4-6")
            .maxTokens(16384)
            .temperature(0.5)
            .system("Custom system prompt")
            .stream(true)
            .build();

        assertEquals("claude-opus-4-6", request.model());
        assertEquals(16384, request.maxTokens());
        assertEquals(0.5, request.temperature());
        assertEquals("Custom system prompt", request.system());
        assertTrue(request.stream());
    }

    @Test
    @DisplayName("ApiRequest builder default model")
    void builderDefaultModel() {
        ApiRequest request = ApiRequest.builder().build();

        assertEquals("claude-sonnet-4-6", request.model());
        assertEquals(4096, request.maxTokens());
        assertFalse(request.stream());
    }

    @Test
    @DisplayName("ApiRequest builder messages")
    void builderMessages() {
        List<Map<String, Object>> messages = List.of(
            Map.of("role", "user", "content", "Test")
        );

        ApiRequest request = ApiRequest.builder()
            .messages(messages)
            .build();

        assertEquals(messages, request.messages());
    }

    @Test
    @DisplayName("ApiRequest builder addMessage")
    void builderAddMessage() {
        Map<String, Object> msg1 = Map.of("role", "user", "content", "Hello");
        Map<String, Object> msg2 = Map.of("role", "assistant", "content", "Hi");

        ApiRequest request = ApiRequest.builder()
            .addMessage(msg1)
            .addMessage(msg2)
            .build();

        assertEquals(2, request.messages().size());
        assertEquals(msg1, request.messages().get(0));
        assertEquals(msg2, request.messages().get(1));
    }

    @Test
    @DisplayName("ApiRequest builder tools")
    void builderTools() {
        List<Map<String, Object>> tools = List.of(
            Map.of("name", "tool1"),
            Map.of("name", "tool2")
        );

        ApiRequest request = ApiRequest.builder()
            .tools(tools)
            .build();

        assertEquals(tools, request.tools());
    }

    @Test
    @DisplayName("ApiRequest builder addTool")
    void builderAddTool() {
        Map<String, Object> tool1 = Map.of("name", "tool1", "description", "First tool");
        Map<String, Object> tool2 = Map.of("name", "tool2", "description", "Second tool");

        ApiRequest request = ApiRequest.builder()
            .addTool(tool1)
            .addTool(tool2)
            .build();

        assertEquals(2, request.tools().size());
        assertEquals(tool1, request.tools().get(0));
        assertEquals(tool2, request.tools().get(1));
    }

    @Test
    @DisplayName("ApiRequest builder returns immutable messages")
    void immutableMessages() {
        ApiRequest request = ApiRequest.builder()
            .addMessage(Map.of("role", "user", "content", "Test"))
            .build();

        assertThrows(UnsupportedOperationException.class, () ->
            request.messages().add(Map.of())
        );
    }

    @Test
    @DisplayName("ApiRequest builder returns immutable tools")
    void immutableTools() {
        ApiRequest request = ApiRequest.builder()
            .addTool(Map.of("name", "test"))
            .build();

        assertThrows(UnsupportedOperationException.class, () ->
            request.tools().add(Map.of())
        );
    }

    @Test
    @DisplayName("ApiRequest builder no tools returns null")
    void builderNoToolsReturnsNull() {
        ApiRequest request = ApiRequest.builder().build();

        assertNull(request.tools());
    }
}