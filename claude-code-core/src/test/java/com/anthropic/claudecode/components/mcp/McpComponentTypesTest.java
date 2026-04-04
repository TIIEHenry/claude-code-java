/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.components.mcp;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for McpComponentTypes.
 */
@DisplayName("McpComponentTypes Tests")
class McpComponentTypesTest {

    @Test
    @DisplayName("ServerStatus enum has correct values")
    void serverStatusEnumHasCorrectValues() {
        McpComponentTypes.ServerStatus[] statuses = McpComponentTypes.ServerStatus.values();

        assertEquals(5, statuses.length);
        assertTrue(Arrays.asList(statuses).contains(McpComponentTypes.ServerStatus.CONNECTED));
        assertTrue(Arrays.asList(statuses).contains(McpComponentTypes.ServerStatus.CONNECTING));
        assertTrue(Arrays.asList(statuses).contains(McpComponentTypes.ServerStatus.DISCONNECTED));
        assertTrue(Arrays.asList(statuses).contains(McpComponentTypes.ServerStatus.ERROR));
        assertTrue(Arrays.asList(statuses).contains(McpComponentTypes.ServerStatus.NEEDS_AUTH));
    }

    @Test
    @DisplayName("McpServerDisplay record works correctly")
    void mcpServerDisplayRecordWorksCorrectly() {
        McpComponentTypes.McpServerDisplay display = new McpComponentTypes.McpServerDisplay(
            "test-server",
            McpComponentTypes.ServerStatus.CONNECTED,
            5,
            3,
            null,
            System.currentTimeMillis()
        );

        assertEquals("test-server", display.name());
        assertEquals(McpComponentTypes.ServerStatus.CONNECTED, display.status());
        assertEquals(5, display.toolCount());
        assertEquals(3, display.resourceCount());
        assertNull(display.error());
    }

    @Test
    @DisplayName("McpServerDisplay getStatusText works correctly")
    void mcpServerDisplayGetStatusTextWorksCorrectly() {
        assertEquals("Connected", new McpComponentTypes.McpServerDisplay("n", McpComponentTypes.ServerStatus.CONNECTED, 0, 0, null, 0).getStatusText());
        assertEquals("Connecting...", new McpComponentTypes.McpServerDisplay("n", McpComponentTypes.ServerStatus.CONNECTING, 0, 0, null, 0).getStatusText());
        assertEquals("Disconnected", new McpComponentTypes.McpServerDisplay("n", McpComponentTypes.ServerStatus.DISCONNECTED, 0, 0, null, 0).getStatusText());
        assertEquals("Error", new McpComponentTypes.McpServerDisplay("n", McpComponentTypes.ServerStatus.ERROR, 0, 0, null, 0).getStatusText());
        assertEquals("Needs Authentication", new McpComponentTypes.McpServerDisplay("n", McpComponentTypes.ServerStatus.NEEDS_AUTH, 0, 0, null, 0).getStatusText());
    }

    @Test
    @DisplayName("McpServerDisplay getStatusIcon works correctly")
    void mcpServerDisplayGetStatusIconWorksCorrectly() {
        assertEquals("✓", new McpComponentTypes.McpServerDisplay("n", McpComponentTypes.ServerStatus.CONNECTED, 0, 0, null, 0).getStatusIcon());
        assertEquals("⏳", new McpComponentTypes.McpServerDisplay("n", McpComponentTypes.ServerStatus.CONNECTING, 0, 0, null, 0).getStatusIcon());
        assertEquals("○", new McpComponentTypes.McpServerDisplay("n", McpComponentTypes.ServerStatus.DISCONNECTED, 0, 0, null, 0).getStatusIcon());
        assertEquals("✗", new McpComponentTypes.McpServerDisplay("n", McpComponentTypes.ServerStatus.ERROR, 0, 0, null, 0).getStatusIcon());
        assertEquals("🔒", new McpComponentTypes.McpServerDisplay("n", McpComponentTypes.ServerStatus.NEEDS_AUTH, 0, 0, null, 0).getStatusIcon());
    }

    @Test
    @DisplayName("McpToolDisplay record works correctly")
    void mcpToolDisplayRecordWorksCorrectly() {
        McpComponentTypes.McpToolDisplay tool = new McpComponentTypes.McpToolDisplay(
            "read_file",
            "filesystem",
            "Read file contents",
            true
        );

        assertEquals("read_file", tool.name());
        assertEquals("filesystem", tool.serverName());
        assertEquals("Read file contents", tool.description());
        assertTrue(tool.isEnabled());
    }

    @Test
    @DisplayName("McpResourceDisplay record works correctly")
    void mcpResourceDisplayRecordWorksCorrectly() {
        McpComponentTypes.McpResourceDisplay resource = new McpComponentTypes.McpResourceDisplay(
            "file:///path/to/file.txt",
            "filesystem",
            "file.txt",
            "text/plain"
        );

        assertEquals("file:///path/to/file.txt", resource.uri());
        assertEquals("filesystem", resource.serverName());
        assertEquals("file.txt", resource.name());
        assertEquals("text/plain", resource.mimeType());
    }

    @Test
    @DisplayName("McpConnectionSummary empty factory works correctly")
    void mcpConnectionSummaryEmptyFactoryWorksCorrectly() {
        McpComponentTypes.McpConnectionSummary summary = McpComponentTypes.McpConnectionSummary.empty();

        assertEquals(0, summary.totalServers());
        assertEquals(0, summary.connectedServers());
        assertEquals(0, summary.totalTools());
        assertEquals(0, summary.totalResources());
        assertTrue(summary.servers().isEmpty());
    }

    @Test
    @DisplayName("McpConnectionSummary record works correctly")
    void mcpConnectionSummaryRecordWorksCorrectly() {
        List<McpComponentTypes.McpServerDisplay> servers = List.of(
            new McpComponentTypes.McpServerDisplay("s1", McpComponentTypes.ServerStatus.CONNECTED, 5, 2, null, 0),
            new McpComponentTypes.McpServerDisplay("s2", McpComponentTypes.ServerStatus.DISCONNECTED, 0, 0, null, 0)
        );

        McpComponentTypes.McpConnectionSummary summary = new McpComponentTypes.McpConnectionSummary(
            2, 1, 5, 2, servers
        );

        assertEquals(2, summary.totalServers());
        assertEquals(1, summary.connectedServers());
        assertEquals(5, summary.totalTools());
        assertEquals(2, summary.totalResources());
        assertEquals(2, summary.servers().size());
    }

    @Test
    @DisplayName("McpFilterOptions default constructor works correctly")
    void mcpFilterOptionsDefaultConstructorWorksCorrectly() {
        McpComponentTypes.McpFilterOptions options = new McpComponentTypes.McpFilterOptions();

        assertNull(options.searchText());
        assertNull(options.statusFilter());
        assertTrue(options.showDisabled());
    }

    @Test
    @DisplayName("McpFilterOptions record works correctly")
    void mcpFilterOptionsRecordWorksCorrectly() {
        McpComponentTypes.McpFilterOptions options = new McpComponentTypes.McpFilterOptions(
            "test",
            McpComponentTypes.ServerStatus.CONNECTED,
            false
        );

        assertEquals("test", options.searchText());
        assertEquals(McpComponentTypes.ServerStatus.CONNECTED, options.statusFilter());
        assertFalse(options.showDisabled());
    }

    @Test
    @DisplayName("McpSortBy enum has correct values")
    void mcpSortByEnumHasCorrectValues() {
        McpComponentTypes.McpSortBy[] sortOptions = McpComponentTypes.McpSortBy.values();

        assertEquals(4, sortOptions.length);
        assertTrue(Arrays.asList(sortOptions).contains(McpComponentTypes.McpSortBy.NAME));
        assertTrue(Arrays.asList(sortOptions).contains(McpComponentTypes.McpSortBy.STATUS));
        assertTrue(Arrays.asList(sortOptions).contains(McpComponentTypes.McpSortBy.TOOL_COUNT));
        assertTrue(Arrays.asList(sortOptions).contains(McpComponentTypes.McpSortBy.CONNECTED_TIME));
    }
}