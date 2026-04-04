/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for McpStringUtils.
 */
class McpStringUtilsTest {

    @Test
    @DisplayName("McpStringUtils mcpInfoFromString parses valid MCP tool name")
    void mcpInfoFromStringValid() {
        McpStringUtils.McpInfo info = McpStringUtils.mcpInfoFromString("mcp__myserver__mytool");

        assertNotNull(info);
        assertEquals("myserver", info.serverName());
        assertEquals("mytool", info.toolName());
    }

    @Test
    @DisplayName("McpStringUtils mcpInfoFromString handles underscores in tool name")
    void mcpInfoFromStringUnderscoresInTool() {
        McpStringUtils.McpInfo info = McpStringUtils.mcpInfoFromString("mcp__server__tool__subtool");

        assertNotNull(info);
        assertEquals("server", info.serverName());
        assertEquals("tool__subtool", info.toolName());
    }

    @Test
    @DisplayName("McpStringUtils mcpInfoFromString returns null for non-MCP")
    void mcpInfoFromStringNonMcp() {
        assertNull(McpStringUtils.mcpInfoFromString("mytool"));
        assertNull(McpStringUtils.mcpInfoFromString("tool__name"));
        assertNull(McpStringUtils.mcpInfoFromString("other__server__tool"));
    }

    @Test
    @DisplayName("McpStringUtils mcpInfoFromString returns null for null input")
    void mcpInfoFromStringNull() {
        assertNull(McpStringUtils.mcpInfoFromString(null));
    }

    @Test
    @DisplayName("McpStringUtils mcpInfoFromString returns null for incomplete format")
    void mcpInfoFromStringIncomplete() {
        assertNull(McpStringUtils.mcpInfoFromString("mcp__"));
        assertNull(McpStringUtils.mcpInfoFromString("mcp__server"));
        assertNull(McpStringUtils.mcpInfoFromString("mcp__server__"));
    }

    @Test
    @DisplayName("McpStringUtils getMcpPrefix generates correct prefix")
    void getMcpPrefix() {
        assertEquals("mcp__myserver__", McpStringUtils.getMcpPrefix("myserver"));
        assertEquals("mcp__my_server__", McpStringUtils.getMcpPrefix("my.server"));
        assertEquals("mcp__test_server__", McpStringUtils.getMcpPrefix("test server"));
    }

    @Test
    @DisplayName("McpStringUtils buildMcpToolName builds correct name")
    void buildMcpToolName() {
        assertEquals("mcp__myserver__mytool", McpStringUtils.buildMcpToolName("myserver", "mytool"));
        assertEquals("mcp__my_server__my_tool", McpStringUtils.buildMcpToolName("my.server", "my.tool"));
    }

    @Test
    @DisplayName("McpStringUtils getToolNameForPermissionCheck for MCP tool")
    void getToolNameForPermissionCheckMcp() {
        McpStringUtils.McpInfo mcpInfo = new McpStringUtils.McpInfo("server", "tool");
        McpStringUtils.ToolInfo toolInfo = new McpStringUtils.ToolInfo("mcp__server__tool", mcpInfo);

        assertEquals("mcp__server__tool", McpStringUtils.getToolNameForPermissionCheck(toolInfo));
    }

    @Test
    @DisplayName("McpStringUtils getToolNameForPermissionCheck for builtin tool")
    void getToolNameForPermissionCheckBuiltin() {
        McpStringUtils.ToolInfo toolInfo = new McpStringUtils.ToolInfo("Read", null);

        assertEquals("Read", McpStringUtils.getToolNameForPermissionCheck(toolInfo));
    }

    @Test
    @DisplayName("McpStringUtils getMcpDisplayName extracts display name")
    void getMcpDisplayName() {
        assertEquals("mytool", McpStringUtils.getMcpDisplayName("mcp__myserver__mytool", "myserver"));
        assertEquals("tool_name", McpStringUtils.getMcpDisplayName("mcp__server__tool_name", "server"));
    }

    @Test
    @DisplayName("McpStringUtils extractMcpToolDisplayName extracts tool name")
    void extractMcpToolDisplayName() {
        assertEquals("Add comment to issue", McpStringUtils.extractMcpToolDisplayName("github - Add comment to issue (MCP)"));
        assertEquals("Create repository", McpStringUtils.extractMcpToolDisplayName("gitlab - Create repository (MCP)"));
    }

    @Test
    @DisplayName("McpStringUtils extractMcpToolDisplayName handles no MCP suffix")
    void extractMcpToolDisplayNameNoSuffix() {
        assertEquals("Add comment to issue", McpStringUtils.extractMcpToolDisplayName("github - Add comment to issue"));
    }

    @Test
    @DisplayName("McpStringUtils extractMcpToolDisplayName handles no server prefix")
    void extractMcpToolDisplayNameNoPrefix() {
        assertEquals("Add comment to issue", McpStringUtils.extractMcpToolDisplayName("Add comment to issue (MCP)"));
    }

    @Test
    @DisplayName("McpStringUtils extractMcpToolDisplayName handles null")
    void extractMcpToolDisplayNameNull() {
        assertNull(McpStringUtils.extractMcpToolDisplayName(null));
    }

    @Test
    @DisplayName("McpStringUtils extractMcpToolDisplayName handles empty")
    void extractMcpToolDisplayNameEmpty() {
        assertEquals("", McpStringUtils.extractMcpToolDisplayName(""));
    }

    @Test
    @DisplayName("McpStringUtils isMcpToolName identifies MCP tools")
    void isMcpToolName() {
        assertTrue(McpStringUtils.isMcpToolName("mcp__server__tool"));
        assertTrue(McpStringUtils.isMcpToolName("mcp__anything"));
        assertFalse(McpStringUtils.isMcpToolName("Read"));
        assertFalse(McpStringUtils.isMcpToolName("Edit"));
        assertFalse(McpStringUtils.isMcpToolName(null));
    }

    @Test
    @DisplayName("McpStringUtils parseToolNameParts splits correctly")
    void parseToolNameParts() {
        String[] parts = McpStringUtils.parseToolNameParts("mcp__server__tool");

        assertEquals(3, parts.length);
        assertEquals("mcp", parts[0]);
        assertEquals("server", parts[1]);
        assertEquals("tool", parts[2]);
    }

    @Test
    @DisplayName("McpStringUtils parseToolNameParts handles null")
    void parseToolNamePartsNull() {
        String[] parts = McpStringUtils.parseToolNameParts(null);

        assertEquals(0, parts.length);
    }

    @Test
    @DisplayName("McpStringUtils McpInfo record")
    void mcpInfoRecord() {
        McpStringUtils.McpInfo info = new McpStringUtils.McpInfo("server", "tool");

        assertEquals("server", info.serverName());
        assertEquals("tool", info.toolName());
    }

    @Test
    @DisplayName("McpStringUtils ToolInfo record")
    void toolInfoRecord() {
        McpStringUtils.McpInfo mcpInfo = new McpStringUtils.McpInfo("server", "tool");
        McpStringUtils.ToolInfo toolInfo = new McpStringUtils.ToolInfo("mcp__server__tool", mcpInfo);

        assertEquals("mcp__server__tool", toolInfo.name());
        assertEquals(mcpInfo, toolInfo.mcpInfo());
    }
}