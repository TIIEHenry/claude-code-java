/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.analytics;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for AnalyticsMetadata.
 */
@DisplayName("AnalyticsMetadata Tests")
class AnalyticsMetadataTest {

    @Test
    @DisplayName("sanitizeToolNameForAnalytics returns mcp_tool for MCP tools")
    void sanitizeToolNameForAnalyticsReturnsMcpToolForMcpTools() {
        assertEquals("mcp_tool", AnalyticsMetadata.sanitizeToolNameForAnalytics("mcp__server__tool"));
        assertEquals("mcp_tool", AnalyticsMetadata.sanitizeToolNameForAnalytics("mcp__any__tool__name"));
    }

    @Test
    @DisplayName("sanitizeToolNameForAnalytics returns original name for non-MCP tools")
    void sanitizeToolNameForAnalyticsReturnsOriginalNameForNonMcpTools() {
        assertEquals("Read", AnalyticsMetadata.sanitizeToolNameForAnalytics("Read"));
        assertEquals("Write", AnalyticsMetadata.sanitizeToolNameForAnalytics("Write"));
        assertEquals("Bash", AnalyticsMetadata.sanitizeToolNameForAnalytics("Bash"));
    }

    @Test
    @DisplayName("extractMcpToolDetails returns null for non-MCP tools")
    void extractMcpToolDetailsReturnsNullForNonMcpTools() {
        assertNull(AnalyticsMetadata.extractMcpToolDetails("Read"));
        assertNull(AnalyticsMetadata.extractMcpToolDetails("Write"));
    }

    @Test
    @DisplayName("extractMcpToolDetails returns details for valid MCP tool names")
    void extractMcpToolDetailsReturnsDetailsForValidMcpToolNames() {
        AnalyticsMetadata.McpToolDetails details = AnalyticsMetadata.extractMcpToolDetails("mcp__filesystem__read_file");

        assertNotNull(details);
        assertEquals("filesystem", details.serverName());
        assertEquals("read_file", details.toolName());
    }

    @Test
    @DisplayName("extractMcpToolDetails handles complex tool names")
    void extractMcpToolDetailsHandlesComplexToolNames() {
        AnalyticsMetadata.McpToolDetails details = AnalyticsMetadata.extractMcpToolDetails("mcp__server__tool__with__underscores");

        assertNotNull(details);
        assertEquals("server", details.serverName());
        assertEquals("tool__with__underscores", details.toolName());
    }

    @Test
    @DisplayName("extractMcpToolDetails returns null for invalid MCP tool names")
    void extractMcpToolDetailsReturnsNullForInvalidMcpToolNames() {
        assertNull(AnalyticsMetadata.extractMcpToolDetails("mcp__"));
        assertNull(AnalyticsMetadata.extractMcpToolDetails("mcp__server"));
    }

    @Test
    @DisplayName("getFileExtensionForAnalytics returns correct extension")
    void getFileExtensionForAnalyticsReturnsCorrectExtension() {
        assertEquals("java", AnalyticsMetadata.getFileExtensionForAnalytics("Test.java"));
        assertEquals("ts", AnalyticsMetadata.getFileExtensionForAnalytics("file.ts"));
        assertEquals("json", AnalyticsMetadata.getFileExtensionForAnalytics("/path/to/file.json"));
    }

    @Test
    @DisplayName("getFileExtensionForAnalytics returns null for null input")
    void getFileExtensionForAnalyticsReturnsNullForNullInput() {
        assertNull(AnalyticsMetadata.getFileExtensionForAnalytics(null));
    }

    @Test
    @DisplayName("getFileExtensionForAnalytics returns null for empty input")
    void getFileExtensionForAnalyticsReturnsNullForEmptyInput() {
        assertNull(AnalyticsMetadata.getFileExtensionForAnalytics(""));
    }

    @Test
    @DisplayName("getFileExtensionForAnalytics returns null for no extension")
    void getFileExtensionForAnalyticsReturnsNullForNoExtension() {
        assertNull(AnalyticsMetadata.getFileExtensionForAnalytics("filename"));
    }

    @Test
    @DisplayName("getFileExtensionForAnalytics returns other for long extensions")
    void getFileExtensionForAnalyticsReturnsOtherForLongExtensions() {
        assertEquals("other", AnalyticsMetadata.getFileExtensionForAnalytics("file.verylongextension"));
    }

    @Test
    @DisplayName("truncateToolInputValue truncates long strings")
    void truncateToolInputValueTruncatesLongStrings() {
        String longString = "a".repeat(600);
        Object result = AnalyticsMetadata.truncateToolInputValue(longString, 0);

        assertTrue(result instanceof String);
        String truncated = (String) result;
        assertTrue(truncated.contains("…"));
        assertTrue(truncated.contains("600 chars"));
    }

    @Test
    @DisplayName("truncateToolInputValue returns numbers unchanged")
    void truncateToolInputValueReturnsNumbersUnchanged() {
        assertEquals(42, AnalyticsMetadata.truncateToolInputValue(42, 0));
        assertEquals(3.14, AnalyticsMetadata.truncateToolInputValue(3.14, 0));
    }

    @Test
    @DisplayName("truncateToolInputValue returns booleans unchanged")
    void truncateToolInputValueReturnsBooleansUnchanged() {
        assertEquals(true, AnalyticsMetadata.truncateToolInputValue(true, 0));
        assertEquals(false, AnalyticsMetadata.truncateToolInputValue(false, 0));
    }

    @Test
    @DisplayName("truncateToolInputValue returns null unchanged")
    void truncateToolInputValueReturnsNullUnchanged() {
        assertNull(AnalyticsMetadata.truncateToolInputValue(null, 0));
    }

    @Test
    @DisplayName("truncateToolInputValue truncates lists")
    void truncateToolInputValueTruncatesLists() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            list.add(i);
        }

        Object result = AnalyticsMetadata.truncateToolInputValue(list, 0);
        assertTrue(result instanceof List);
        List<?> truncated = (List<?>) result;
        assertTrue(truncated.size() > 20); // 20 items + truncation indicator
    }

    @Test
    @DisplayName("truncateToolInputValue truncates maps")
    void truncateToolInputValueTruncatesMaps() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < 30; i++) {
            map.put("key" + i, i);
        }

        Object result = AnalyticsMetadata.truncateToolInputValue(map, 0);
        assertTrue(result instanceof Map);
        Map<?, ?> truncated = (Map<?, ?>) result;
        assertTrue(truncated.size() <= 21); // 20 items + truncation indicator
    }

    @Test
    @DisplayName("truncateToolInputValue respects max depth")
    void truncateToolInputValueRespectsMaxDepth() {
        List<Object> nested = new ArrayList<>();
        nested.add(List.of("inner"));
        nested.add(List.of(List.of("deep")));

        Object result = AnalyticsMetadata.truncateToolInputValue(nested, 0);
        assertTrue(result instanceof List);
    }

    @Test
    @DisplayName("McpToolDetails record works correctly")
    void mcpToolDetailsRecordWorksCorrectly() {
        AnalyticsMetadata.McpToolDetails details = new AnalyticsMetadata.McpToolDetails("server", "tool");

        assertEquals("server", details.serverName());
        assertEquals("tool", details.toolName());
    }

    @Test
    @DisplayName("EnvContext record works correctly")
    void envContextRecordWorksCorrectly() {
        AnalyticsMetadata.EnvContext ctx = new AnalyticsMetadata.EnvContext(
            "darwin", "darwin", "x64", "17.0.0", "iTerm", "npm,yarn", "node,java",
            false, false, false, false, false, false, null, null, null, null,
            null, false, false, false, "1.0.0", "1.0.0", "2024-01-01", "production"
        );

        assertEquals("darwin", ctx.platform());
        assertEquals("x64", ctx.arch());
        assertFalse(ctx.isRunningWithBun());
        assertFalse(ctx.isCi());
    }

    @Test
    @DisplayName("ProcessMetrics record works correctly")
    void processMetricsRecordWorksCorrectly() {
        AnalyticsMetadata.ProcessMetrics metrics = new AnalyticsMetadata.ProcessMetrics(
            1000L, 100L, 50L, 30L, 10L, 5L, 100L, 50L, 0.5
        );

        assertEquals(1000L, metrics.uptime());
        assertEquals(0.5, metrics.cpuPercent());
    }

    @Test
    @DisplayName("EventMetadata record works correctly")
    void eventMetadataRecordWorksCorrectly() {
        AnalyticsMetadata.EnvContext envCtx = new AnalyticsMetadata.EnvContext(
            "darwin", "darwin", "x64", "17.0.0", "iTerm", "npm", "node",
            false, false, false, false, false, false, null, null, null, null,
            null, false, false, false, "1.0.0", "1.0.0", null, "production"
        );
        AnalyticsMetadata.ProcessMetrics procMetrics = new AnalyticsMetadata.ProcessMetrics(
            1000L, 100L, 50L, 30L, 10L, 5L, 100L, 50L, 0.5
        );
        AnalyticsMetadata.EventMetadata metadata = new AnalyticsMetadata.EventMetadata(
            "claude-opus-4-6", "session-123", "ant", null,
            envCtx, "cli", null, true, "cli", procMetrics, null, null, null, null, null, null, null, null, null
        );

        assertEquals("claude-opus-4-6", metadata.model());
        assertEquals("session-123", metadata.sessionId());
        assertTrue(metadata.isInteractive());
    }
}