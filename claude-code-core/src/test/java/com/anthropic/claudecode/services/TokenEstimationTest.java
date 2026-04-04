/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TokenEstimation.
 */
class TokenEstimationTest {

    @Test
    @DisplayName("estimateTokens handles null input")
    void estimateTokensNull() {
        assertEquals(0, TokenEstimation.estimateTokens(null));
    }

    @Test
    @DisplayName("estimateTokens handles empty string")
    void estimateTokensEmpty() {
        assertEquals(0, TokenEstimation.estimateTokens(""));
    }

    @Test
    @DisplayName("estimateTokens counts characters")
    void estimateTokensCountsCharacters() {
        // 16 chars / 4 = 4 tokens + 4 overhead = 8
        int tokens = TokenEstimation.estimateTokens("This is a test!!");
        assertTrue(tokens > 0);
    }

    @Test
    @DisplayName("estimateTokensForMessages handles null")
    void estimateTokensForMessagesNull() {
        assertEquals(0, TokenEstimation.estimateTokensForMessages(null));
    }

    @Test
    @DisplayName("estimateTokensForMessages handles empty list")
    void estimateTokensForMessagesEmpty() {
        assertEquals(0, TokenEstimation.estimateTokensForMessages(List.of()));
    }

    @Test
    @DisplayName("estimateTokensForMessages counts messages")
    void estimateTokensForMessagesWorks() {
        List<Map<String, Object>> messages = List.of(
            Map.of("role", "user", "content", "Hello")
        );

        int tokens = TokenEstimation.estimateTokensForMessages(messages);
        assertTrue(tokens > 0);
    }

    @Test
    @DisplayName("estimateTokensForMessage handles null")
    void estimateTokensForMessageNull() {
        assertEquals(0, TokenEstimation.estimateTokensForMessage(null));
    }

    @Test
    @DisplayName("estimateTokensForMessage counts string content")
    void estimateTokensForMessageStringContent() {
        Map<String, Object> message = Map.of(
            "role", "user",
            "content", "Hello, world!"
        );

        int tokens = TokenEstimation.estimateTokensForMessage(message);
        assertTrue(tokens > 0);
    }

    @Test
    @DisplayName("roughTokenCountEstimation works")
    void roughTokenCountEstimationWorks() {
        assertEquals(0, TokenEstimation.roughTokenCountEstimation(null));
        assertEquals(0, TokenEstimation.roughTokenCountEstimation(""));
        // 12 chars / 4 = 3 tokens
        assertEquals(3, TokenEstimation.roughTokenCountEstimation("Hello world!"));
    }

    @Test
    @DisplayName("estimateSystemPromptTokens adds overhead")
    void estimateSystemPromptTokensWorks() {
        assertEquals(0, TokenEstimation.estimateSystemPromptTokens(null));
        assertEquals(0, TokenEstimation.estimateSystemPromptTokens(""));

        int tokens = TokenEstimation.estimateSystemPromptTokens("You are helpful");
        assertTrue(tokens > 0);
    }

    @Test
    @DisplayName("estimateToolsTokens handles null and empty")
    void estimateToolsTokensNullEmpty() {
        assertEquals(0, TokenEstimation.estimateToolsTokens(null));
        assertEquals(0, TokenEstimation.estimateToolsTokens(List.of()));
    }

    @Test
    @DisplayName("estimateToolsTokens counts tools")
    void estimateToolsTokensWorks() {
        List<Map<String, Object>> tools = List.of(
            Map.of("name", "bash", "description", "Execute bash commands")
        );

        int tokens = TokenEstimation.estimateToolsTokens(tools);
        assertTrue(tokens > 0);
    }
}