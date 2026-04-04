/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SideQuery.
 */
class SideQueryTest {

    @Test
    @DisplayName("SideQuery SideQueryOptions builder")
    void sideQueryOptionsBuilder() {
        SideQuery.SideQueryOptions options = SideQuery.SideQueryOptions.builder()
            .model("claude-sonnet-4-6")
            .system("You are helpful")
            .maxTokens(2048)
            .build();

        assertEquals("claude-sonnet-4-6", options.model());
        assertEquals("You are helpful", options.system());
        assertEquals(2048, options.maxTokens());
    }

    @Test
    @DisplayName("SideQuery SideQueryOptions builder defaults")
    void sideQueryOptionsBuilderDefaults() {
        SideQuery.SideQueryOptions options = SideQuery.SideQueryOptions.builder()
            .model("test")
            .build();

        assertEquals(1024, options.maxTokens());
        assertEquals(2, options.maxRetries());
    }

    @Test
    @DisplayName("SideQuery SideQueryOptions addMessage")
    void sideQueryOptionsAddMessage() {
        SideQuery.SideQueryOptions options = SideQuery.SideQueryOptions.builder()
            .model("test")
            .addMessage(Map.of("role", "user", "content", "hello"))
            .build();

        assertEquals(1, options.messages().size());
        assertEquals("hello", options.messages().get(0).get("content"));
    }

    @Test
    @DisplayName("SideQuery SideQueryOptions all fields")
    void sideQueryOptionsAllFields() {
        SideQuery.SideQueryOptions options = SideQuery.SideQueryOptions.builder()
            .model("claude-opus")
            .system("system prompt")
            .messages(List.of(Map.of("role", "user")))
            .tools(List.of(Map.of("name", "test_tool")))
            .toolChoice(Map.of("type", "auto"))
            .outputFormat("json")
            .maxTokens(500)
            .maxRetries(5)
            .skipSystemPromptPrefix(true)
            .temperature(0.7)
            .thinking(1000)
            .stopSequences(List.of("END"))
            .querySource("test")
            .build();

        assertEquals("claude-opus", options.model());
        assertEquals("system prompt", options.system());
        assertEquals(1, options.messages().size());
        assertEquals(1, options.tools().size());
        assertEquals("auto", options.toolChoice().get("type"));
        assertEquals("json", options.outputFormat());
        assertEquals(500, options.maxTokens());
        assertEquals(5, options.maxRetries());
        assertTrue(options.skipSystemPromptPrefix());
        assertEquals(0.7, options.temperature());
        assertEquals(1000, options.thinking());
        assertEquals(1, options.stopSequences().size());
        assertEquals("test", options.querySource());
    }

    @Test
    @DisplayName("SideQuery execute returns result")
    void executeReturnsResult() throws Exception {
        SideQuery.SideQueryOptions options = SideQuery.SideQueryOptions.builder()
            .model("test")
            .build();

        SideQuery.SideQueryResult result = SideQuery.execute(options).get();

        assertNotNull(result);
        assertEquals("test", result.model());
    }

    @Test
    @DisplayName("SideQuery SideQueryResult record")
    void sideQueryResultRecord() {
        SideQuery.Usage usage = new SideQuery.Usage(100, 50, 20, 10);
        SideQuery.SideQueryResult result = new SideQuery.SideQueryResult(
            List.of(Map.of("type", "text")),
            usage,
            "req-123",
            "claude-sonnet"
        );

        assertEquals(1, result.content().size());
        assertEquals(usage, result.usage());
        assertEquals("req-123", result.requestId());
        assertEquals("claude-sonnet", result.model());
    }

    @Test
    @DisplayName("SideQuery Usage record")
    void usageRecord() {
        SideQuery.Usage usage = new SideQuery.Usage(100, 50, 20, 10);

        assertEquals(100, usage.inputTokens());
        assertEquals(50, usage.outputTokens());
        assertEquals(20, usage.cachedInputTokens());
        assertEquals(10, usage.cacheCreationTokens());
    }
}