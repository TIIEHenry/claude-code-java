/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ContextAnalysis.
 */
class ContextAnalysisTest {

    @Test
    @DisplayName("ContextAnalysis TokenStats record")
    void tokenStatsRecord() {
        ContextAnalysis.TokenStats stats = new ContextAnalysis.TokenStats(
            100, 50, 150,
            Map.of("Read", 3),
            Map.of("id1", 1)
        );

        assertEquals(100, stats.inputTokens());
        assertEquals(50, stats.outputTokens());
        assertEquals(150, stats.totalTokens());
        assertEquals(1, stats.toolRequests().size());
        assertEquals(1, stats.toolResults().size());
    }

    @Test
    @DisplayName("ContextAnalysis analyzeContext empty list")
    void analyzeContextEmpty() {
        List<Map<String, Object>> messages = new ArrayList<>();
        ContextAnalysis.TokenStats stats = ContextAnalysis.analyzeContext(messages);

        assertEquals(0, stats.inputTokens());
        assertEquals(0, stats.outputTokens());
        assertEquals(0, stats.totalTokens());
    }

    @Test
    @DisplayName("ContextAnalysis analyzeContext user message")
    void analyzeContextUserMessage() {
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", "Hello World");
        messages.add(msg);

        ContextAnalysis.TokenStats stats = ContextAnalysis.analyzeContext(messages);

        assertTrue(stats.inputTokens() > 0);
        assertEquals(0, stats.outputTokens());
    }

    @Test
    @DisplayName("ContextAnalysis analyzeContext assistant message")
    void analyzeContextAssistantMessage() {
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "assistant");
        msg.put("content", "Response text");
        messages.add(msg);

        ContextAnalysis.TokenStats stats = ContextAnalysis.analyzeContext(messages);

        assertEquals(0, stats.inputTokens());
        assertTrue(stats.outputTokens() > 0);
    }

    @Test
    @DisplayName("ContextAnalysis analyzeContext tool use")
    void analyzeContextToolUse() {
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "assistant");

        Map<String, Object> toolUse = new HashMap<>();
        toolUse.put("type", "tool_use");
        toolUse.put("name", "Read");

        msg.put("content", List.of(toolUse));
        messages.add(msg);

        ContextAnalysis.TokenStats stats = ContextAnalysis.analyzeContext(messages);

        assertEquals(1, stats.toolRequests().get("Read"));
    }

    @Test
    @DisplayName("ContextAnalysis analyzeContext tool result")
    void analyzeContextToolResult() {
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "user");

        Map<String, Object> toolResult = new HashMap<>();
        toolResult.put("type", "tool_result");
        toolResult.put("tool_use_id", "id-123");

        msg.put("content", List.of(toolResult));
        messages.add(msg);

        ContextAnalysis.TokenStats stats = ContextAnalysis.analyzeContext(messages);

        assertEquals(1, stats.toolResults().get("id-123"));
    }

    @Test
    @DisplayName("ContextAnalysis estimateTokens null")
    void estimateTokensNull() {
        assertEquals(0, ContextAnalysis.estimateTokens(null));
    }

    @Test
    @DisplayName("ContextAnalysis estimateTokens string")
    void estimateTokensString() {
        int tokens = ContextAnalysis.estimateTokens("Hello World");
        assertEquals(2, tokens); // 11 chars / 4 = 2
    }

    @Test
    @DisplayName("ContextAnalysis estimateTokens list")
    void estimateTokensList() {
        Map<String, Object> block = new HashMap<>();
        block.put("text", "Test content");

        int tokens = ContextAnalysis.estimateTokens(List.of(block));
        assertTrue(tokens > 0);
    }

    @Test
    @DisplayName("ContextAnalysis estimateTokensFromString empty")
    void estimateTokensFromStringEmpty() {
        assertEquals(0, ContextAnalysis.estimateTokensFromString(""));
        assertEquals(0, ContextAnalysis.estimateTokensFromString(null));
    }

    @Test
    @DisplayName("ContextAnalysis estimateTokensFromString basic")
    void estimateTokensFromStringBasic() {
        int tokens = ContextAnalysis.estimateTokensFromString("aaaaaaaa"); // 8 chars
        assertEquals(2, tokens); // 8 / 4 = 2
    }

    @Test
    @DisplayName("ContextAnalysis getContextUsagePercentage")
    void getContextUsagePercentage() {
        assertEquals(50.0, ContextAnalysis.getContextUsagePercentage(50000, 100000));
        assertEquals(0.0, ContextAnalysis.getContextUsagePercentage(0, 100000));
        assertEquals(0.0, ContextAnalysis.getContextUsagePercentage(50000, 0));
    }

    @Test
    @DisplayName("ContextAnalysis isContextNearLimit")
    void isContextNearLimit() {
        assertTrue(ContextAnalysis.isContextNearLimit(90000, 100000, 80));
        assertFalse(ContextAnalysis.isContextNearLimit(50000, 100000, 80));
    }
}