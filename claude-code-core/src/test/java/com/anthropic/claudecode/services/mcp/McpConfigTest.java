/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for McpConfig.
 */
class McpConfigTest {

    @BeforeEach
    void setUp() {
        McpConfig.clear();
    }

    @Test
    @DisplayName("McpConfig addServerConfig adds config")
    void addServerConfig() {
        McpTypes.McpStdioServerConfig serverConfig = new McpTypes.McpStdioServerConfig(
            "node", List.of("server.js"), Map.of()
        );
        McpTypes.ScopedMcpServerConfig scoped = new McpTypes.ScopedMcpServerConfig(
            serverConfig, McpTypes.ConfigScope.USER, null
        );

        McpConfig.addServerConfig("my-server", scoped);

        assertTrue(McpConfig.hasServer("my-server"));
        assertEquals(scoped, McpConfig.getServerConfig("my-server"));
    }

    @Test
    @DisplayName("McpConfig removeServerConfig removes config")
    void removeServerConfig() {
        McpTypes.McpStdioServerConfig serverConfig = new McpTypes.McpStdioServerConfig(
            "node", List.of("server.js"), Map.of()
        );
        McpTypes.ScopedMcpServerConfig scoped = new McpTypes.ScopedMcpServerConfig(
            serverConfig, McpTypes.ConfigScope.USER, null
        );

        McpConfig.addServerConfig("my-server", scoped);
        assertTrue(McpConfig.hasServer("my-server"));

        McpConfig.removeServerConfig("my-server");
        assertFalse(McpConfig.hasServer("my-server"));
    }

    @Test
    @DisplayName("McpConfig getServerConfig returns null for missing")
    void getServerConfigMissing() {
        assertNull(McpConfig.getServerConfig("non-existent"));
    }

    @Test
    @DisplayName("McpConfig getAllServerConfigs returns copy")
    void getAllServerConfigs() {
        McpTypes.McpStdioServerConfig serverConfig = new McpTypes.McpStdioServerConfig(
            "node", List.of("server.js"), Map.of()
        );
        McpTypes.ScopedMcpServerConfig scoped = new McpTypes.ScopedMcpServerConfig(
            serverConfig, McpTypes.ConfigScope.USER, null
        );

        McpConfig.addServerConfig("server1", scoped);

        Map<String, McpTypes.ScopedMcpServerConfig> all = McpConfig.getAllServerConfigs();
        assertEquals(1, all.size());

        // Verify it's a copy
        all.clear();
        assertTrue(McpConfig.hasServer("server1"));
    }

    @Test
    @DisplayName("McpConfig hasServer returns correct boolean")
    void hasServer() {
        assertFalse(McpConfig.hasServer("test"));

        McpTypes.McpStdioServerConfig serverConfig = new McpTypes.McpStdioServerConfig(
            "node", List.of(), Map.of()
        );
        McpTypes.ScopedMcpServerConfig scoped = new McpTypes.ScopedMcpServerConfig(
            serverConfig, McpTypes.ConfigScope.LOCAL, null
        );

        McpConfig.addServerConfig("test", scoped);
        assertTrue(McpConfig.hasServer("test"));
    }

    @Test
    @DisplayName("McpConfig clear removes all configs")
    void clear() {
        McpTypes.McpStdioServerConfig serverConfig = new McpTypes.McpStdioServerConfig(
            "node", List.of(), Map.of()
        );
        McpTypes.ScopedMcpServerConfig scoped = new McpTypes.ScopedMcpServerConfig(
            serverConfig, McpTypes.ConfigScope.LOCAL, null
        );

        McpConfig.addServerConfig("server1", scoped);
        McpConfig.addServerConfig("server2", scoped);
        assertEquals(2, McpConfig.getAllServerConfigs().size());

        McpConfig.clear();
        assertTrue(McpConfig.getAllServerConfigs().isEmpty());
    }

    @Test
    @DisplayName("McpConfig parseStdioConfig parses correctly")
    void parseStdioConfig() {
        Map<String, Object> json = Map.of(
            "command", "node",
            "args", List.of("server.js", "--port", "3000"),
            "env", Map.of("NODE_ENV", "production", "DEBUG", "true")
        );

        McpTypes.McpStdioServerConfig config = McpConfig.parseStdioConfig(json);

        assertEquals("node", config.command());
        assertEquals(3, config.args().size());
        assertEquals("server.js", config.args().get(0));
        assertEquals("--port", config.args().get(1));
        assertEquals("3000", config.args().get(2));
        assertEquals("production", config.env().get("NODE_ENV"));
        assertEquals("true", config.env().get("DEBUG"));
    }

    @Test
    @DisplayName("McpConfig parseStdioConfig with missing optional fields")
    void parseStdioConfigMinimal() {
        Map<String, Object> json = Map.of("command", "python");

        McpTypes.McpStdioServerConfig config = McpConfig.parseStdioConfig(json);

        assertEquals("python", config.command());
        assertTrue(config.args().isEmpty());
        assertTrue(config.env().isEmpty());
    }

    @Test
    @DisplayName("McpConfig parseSseConfig parses correctly")
    void parseSseConfig() {
        Map<String, Object> json = Map.of(
            "url", "https://api.example.com/sse",
            "headers", Map.of("Authorization", "Bearer token123"),
            "headersHelper", "myHelper"
        );

        McpTypes.McpSSEServerConfig config = McpConfig.parseSseConfig(json);

        assertEquals("https://api.example.com/sse", config.url());
        assertEquals("Bearer token123", config.headers().get("Authorization"));
        assertEquals("myHelper", config.headersHelper());
        assertNull(config.oauth());
    }

    @Test
    @DisplayName("McpConfig parseSseConfig with oauth")
    void parseSseConfigWithOauth() {
        Map<String, Object> json = Map.of(
            "url", "https://api.example.com/sse",
            "headers", Map.of(),
            "oauth", Map.of(
                "clientId", "client-123",
                "callbackPort", 8080,
                "authServerMetadataUrl", "https://auth.example.com",
                "xaa", true
            )
        );

        McpTypes.McpSSEServerConfig config = McpConfig.parseSseConfig(json);

        assertNotNull(config.oauth());
        assertEquals("client-123", config.oauth().clientId());
        assertEquals(8080, config.oauth().callbackPort());
        assertEquals("https://auth.example.com", config.oauth().authServerMetadataUrl());
        assertTrue(config.oauth().xaa());
    }

    @Test
    @DisplayName("McpConfig parseSseConfig minimal")
    void parseSseConfigMinimal() {
        Map<String, Object> json = Map.of("url", "https://api.example.com/sse");

        McpTypes.McpSSEServerConfig config = McpConfig.parseSseConfig(json);

        assertEquals("https://api.example.com/sse", config.url());
        assertTrue(config.headers().isEmpty());
        assertNull(config.headersHelper());
        assertNull(config.oauth());
    }
}