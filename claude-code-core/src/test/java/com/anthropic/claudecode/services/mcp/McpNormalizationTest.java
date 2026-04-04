/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for McpNormalization.
 */
class McpNormalizationTest {

    @Test
    @DisplayName("McpNormalization normalizeNameForMCP replaces invalid characters")
    void normalizeNameForMCPInvalidChars() {
        assertEquals("my_server", McpNormalization.normalizeNameForMCP("my.server"));
        assertEquals("my_server", McpNormalization.normalizeNameForMCP("my server"));
        assertEquals("my_server_name", McpNormalization.normalizeNameForMCP("my server name"));
        assertEquals("test_123", McpNormalization.normalizeNameForMCP("test@123"));
    }

    @Test
    @DisplayName("McpNormalization normalizeNameForMCP keeps valid characters")
    void normalizeNameForMCPValidChars() {
        assertEquals("myServer", McpNormalization.normalizeNameForMCP("myServer"));
        assertEquals("server-123", McpNormalization.normalizeNameForMCP("server-123"));
        assertEquals("test_name", McpNormalization.normalizeNameForMCP("test_name"));
    }

    @Test
    @DisplayName("McpNormalization normalizeNameForMCP handles null")
    void normalizeNameForMCPNull() {
        assertNull(McpNormalization.normalizeNameForMCP(null));
    }

    @Test
    @DisplayName("McpNormalization normalizeNameForMCP handles empty")
    void normalizeNameForMCPEmpty() {
        assertEquals("", McpNormalization.normalizeNameForMCP(""));
    }

    @Test
    @DisplayName("McpNormalization normalizeNameForMCP collapses underscores for claude.ai servers")
    void normalizeNameForMCPClaudeAiServers() {
        // claude.ai servers get special treatment - underscores collapsed
        assertEquals("claude_ai_my_service", McpNormalization.normalizeNameForMCP("claude.ai my.service"));
        // Multiple underscores collapsed
        String result = McpNormalization.normalizeNameForMCP("claude.ai my___service");
        assertTrue(result.contains("_"));
        assertFalse(result.contains("___"));
    }

    @Test
    @DisplayName("McpNormalization isValidMcpName validates correctly")
    void isValidMcpNameValid() {
        assertTrue(McpNormalization.isValidMcpName("myServer"));
        assertTrue(McpNormalization.isValidMcpName("server-123"));
        assertTrue(McpNormalization.isValidMcpName("test_name"));
        assertTrue(McpNormalization.isValidMcpName("UPPERCASE"));
        assertTrue(McpNormalization.isValidMcpName("a"));
    }

    @Test
    @DisplayName("McpNormalization isValidMcpName rejects invalid")
    void isValidMcpNameInvalid() {
        assertFalse(McpNormalization.isValidMcpName(null));
        assertFalse(McpNormalization.isValidMcpName(""));
        assertFalse(McpNormalization.isValidMcpName("my.server"));
        assertFalse(McpNormalization.isValidMcpName("my server"));
        assertFalse(McpNormalization.isValidMcpName("server@123"));
    }

    @Test
    @DisplayName("McpNormalization isValidMcpName rejects too long")
    void isValidMcpNameTooLong() {
        // 65 characters is too long (max 64)
        String longName = "a".repeat(65);
        assertFalse(McpNormalization.isValidMcpName(longName));
        // 64 characters is OK
        String maxName = "a".repeat(64);
        assertTrue(McpNormalization.isValidMcpName(maxName));
    }

    @Test
    @DisplayName("McpNormalization toValidMcpName generates valid names")
    void toValidMcpName() {
        assertEquals("myServer", McpNormalization.toValidMcpName("myServer"));
        assertEquals("my_server", McpNormalization.toValidMcpName("my.server"));
        assertEquals("server_123", McpNormalization.toValidMcpName("server@123"));
    }

    @Test
    @DisplayName("McpNormalization toValidMcpName handles null")
    void toValidMcpNameNull() {
        assertEquals("unnamed", McpNormalization.toValidMcpName(null));
    }

    @Test
    @DisplayName("McpNormalization toValidMcpName handles empty")
    void toValidMcpNameEmpty() {
        assertEquals("unnamed", McpNormalization.toValidMcpName(""));
    }

    @Test
    @DisplayName("McpNormalization toValidMcpName truncates long names")
    void toValidMcpNameTruncates() {
        String longName = "a".repeat(100);
        String result = McpNormalization.toValidMcpName(longName);
        assertTrue(result.length() <= 64);
        assertTrue(McpNormalization.isValidMcpName(result));
    }

    @Test
    @DisplayName("McpNormalization toValidMcpName strips leading/trailing underscores")
    void toValidMcpNameStripsUnderscores() {
        assertEquals("test", McpNormalization.toValidMcpName("_test_"));
        assertEquals("test", McpNormalization.toValidMcpName("__test__"));
    }

    @Test
    @DisplayName("McpNormalization toValidMcpName returns unnamed for all-underscore")
    void toValidMcpNameAllUnderscore() {
        assertEquals("unnamed", McpNormalization.toValidMcpName("___"));
        assertEquals("unnamed", McpNormalization.toValidMcpName("_"));
    }
}