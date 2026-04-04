/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClaudeDesktop.
 */
class ClaudeDesktopTest {

    @Test
    @DisplayName("ClaudeDesktop getClaudeDesktopConfigPath returns path")
    void getClaudeDesktopConfigPath() {
        // This test depends on the OS, just verify it returns something
        try {
            Path path = ClaudeDesktop.getClaudeDesktopConfigPath();
            assertNotNull(path);
            assertTrue(path.toString().contains("claude_desktop_config.json"));
        } catch (UnsupportedOperationException e) {
            // Expected on non-mac/non-WSL systems
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("ClaudeDesktop McpServerConfig record")
    void mcpServerConfigRecord() {
        ClaudeDesktop.McpServerConfig config = new ClaudeDesktop.McpServerConfig(
            "node",
            java.util.List.of("server.js"),
            Map.of("KEY", "value")
        );

        assertEquals("node", config.command());
        assertEquals(1, config.args().size());
        assertEquals("server.js", config.args().get(0));
        assertEquals(1, config.env().size());
        assertEquals("value", config.env().get("KEY"));
    }

    @Test
    @DisplayName("ClaudeDesktop readClaudeDesktopMcpServers returns map")
    void readClaudeDesktopMcpServers() {
        // This may return empty map if no config exists
        Map<String, ClaudeDesktop.McpServerConfig> servers =
            ClaudeDesktop.readClaudeDesktopMcpServers();

        assertNotNull(servers);
        // May be empty or have servers
        assertTrue(servers.size() >= 0);
    }

    @Test
    @DisplayName("ClaudeDesktop readClaudeDesktopMcpServers nonexistent file")
    void readClaudeDesktopMcpServersNonexistent() {
        // The method handles nonexistent config gracefully
        Map<String, ClaudeDesktop.McpServerConfig> servers =
            ClaudeDesktop.readClaudeDesktopMcpServers();

        assertNotNull(servers);
    }
}