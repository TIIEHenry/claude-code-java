/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for McpTypes.
 */
class McpTypesTest {

    @Test
    @DisplayName("McpTypes ConfigScope enum values")
    void configScopeEnum() {
        McpTypes.ConfigScope[] scopes = McpTypes.ConfigScope.values();
        assertEquals(7, scopes.length);
        assertEquals(McpTypes.ConfigScope.LOCAL, McpTypes.ConfigScope.valueOf("LOCAL"));
        assertEquals(McpTypes.ConfigScope.USER, McpTypes.ConfigScope.valueOf("USER"));
        assertEquals(McpTypes.ConfigScope.PROJECT, McpTypes.ConfigScope.valueOf("PROJECT"));
        assertEquals(McpTypes.ConfigScope.DYNAMIC, McpTypes.ConfigScope.valueOf("DYNAMIC"));
        assertEquals(McpTypes.ConfigScope.ENTERPRISE, McpTypes.ConfigScope.valueOf("ENTERPRISE"));
    }

    @Test
    @DisplayName("McpTypes Transport enum values")
    void transportEnum() {
        McpTypes.Transport[] transports = McpTypes.Transport.values();
        assertEquals(6, transports.length);
        assertEquals(McpTypes.Transport.STDIO, McpTypes.Transport.valueOf("STDIO"));
        assertEquals(McpTypes.Transport.SSE, McpTypes.Transport.valueOf("SSE"));
        assertEquals(McpTypes.Transport.HTTP, McpTypes.Transport.valueOf("HTTP"));
        assertEquals(McpTypes.Transport.WS, McpTypes.Transport.valueOf("WS"));
        assertEquals(McpTypes.Transport.SDK, McpTypes.Transport.valueOf("SDK"));
    }

    @Test
    @DisplayName("McpTypes McpOAuthConfig record")
    void mcpOAuthConfigRecord() {
        McpTypes.McpOAuthConfig oauth = new McpTypes.McpOAuthConfig(
            "client-123", 8080, "https://auth.example.com", true
        );

        assertEquals("client-123", oauth.clientId());
        assertEquals(8080, oauth.callbackPort());
        assertEquals("https://auth.example.com", oauth.authServerMetadataUrl());
        assertTrue(oauth.xaa());
    }

    @Test
    @DisplayName("McpTypes McpStdioServerConfig record")
    void mcpStdioServerConfigRecord() {
        McpTypes.McpStdioServerConfig config = new McpTypes.McpStdioServerConfig(
            "node", List.of("server.js"), Map.of("NODE_ENV", "production")
        );

        assertEquals("node", config.command());
        assertEquals(1, config.args().size());
        assertEquals("server.js", config.args().get(0));
        assertEquals(McpTypes.Transport.STDIO, config.type());
    }

    @Test
    @DisplayName("McpTypes McpSSEServerConfig record")
    void mcpSSEServerConfigRecord() {
        McpTypes.McpSSEServerConfig config = new McpTypes.McpSSEServerConfig(
            "https://api.example.com/sse", Map.of("Authorization", "Bearer token"), null, null
        );

        assertEquals("https://api.example.com/sse", config.url());
        assertEquals("Bearer token", config.headers().get("Authorization"));
        assertEquals(McpTypes.Transport.SSE, config.type());
    }

    @Test
    @DisplayName("McpTypes McpSSEIDEServerConfig record")
    void mcpSseIdeServerConfigRecord() {
        McpTypes.McpSSEIDEServerConfig config = new McpTypes.McpSSEIDEServerConfig(
            "http://localhost:3000/sse", "vscode", false
        );

        assertEquals("http://localhost:3000/sse", config.url());
        assertEquals("vscode", config.ideName());
        assertFalse(config.ideRunningInWindows());
        assertEquals(McpTypes.Transport.SSE_IDE, config.type());
    }

    @Test
    @DisplayName("McpTypes McpWebSocketIDEServerConfig record")
    void mcpWebSocketIdeServerConfigRecord() {
        McpTypes.McpWebSocketIDEServerConfig config = new McpTypes.McpWebSocketIDEServerConfig(
            "ws://localhost:3000/ws", "vscode", "token123", true
        );

        assertEquals("ws://localhost:3000/ws", config.url());
        assertEquals("vscode", config.ideName());
        assertEquals("token123", config.authToken());
        assertTrue(config.ideRunningInWindows());
        assertEquals(McpTypes.Transport.WS, config.type());
    }

    @Test
    @DisplayName("McpTypes McpHTTPServerConfig record")
    void mcpHttpServerConfigRecord() {
        McpTypes.McpHTTPServerConfig config = new McpTypes.McpHTTPServerConfig(
            "https://api.example.com", Map.of("X-API-Key", "key123"), null, null
        );

        assertEquals("https://api.example.com", config.url());
        assertEquals(McpTypes.Transport.HTTP, config.type());
    }

    @Test
    @DisplayName("McpTypes McpWebSocketServerConfig record")
    void mcpWebSocketServerConfigRecord() {
        McpTypes.McpWebSocketServerConfig config = new McpTypes.McpWebSocketServerConfig(
            "wss://api.example.com/ws", Map.of("Authorization", "Bearer token"), null
        );

        assertEquals("wss://api.example.com/ws", config.url());
        assertEquals(McpTypes.Transport.WS, config.type());
    }

    @Test
    @DisplayName("McpTypes McpSdkServerConfig record")
    void mcpSdkServerConfigRecord() {
        McpTypes.McpSdkServerConfig config = new McpTypes.McpSdkServerConfig("my-sdk-server");

        assertEquals("my-sdk-server", config.name());
        assertEquals(McpTypes.Transport.SDK, config.type());
    }

    @Test
    @DisplayName("McpTypes McpClaudeAIProxyServerConfig record")
    void mcpClaudeAiProxyServerConfigRecord() {
        McpTypes.McpClaudeAIProxyServerConfig config = new McpTypes.McpClaudeAIProxyServerConfig(
            "https://claude.ai/proxy", "proxy-123"
        );

        assertEquals("https://claude.ai/proxy", config.url());
        assertEquals("proxy-123", config.id());
        assertEquals(McpTypes.Transport.SSE, config.type());
    }

    @Test
    @DisplayName("McpTypes ScopedMcpServerConfig record")
    void scopedMcpServerConfigRecord() {
        McpTypes.McpStdioServerConfig serverConfig = new McpTypes.McpStdioServerConfig(
            "node", List.of("server.js"), Map.of()
        );
        McpTypes.ScopedMcpServerConfig scoped = new McpTypes.ScopedMcpServerConfig(
            serverConfig, McpTypes.ConfigScope.USER, "my-plugin"
        );

        assertEquals(serverConfig, scoped.config());
        assertEquals(McpTypes.ConfigScope.USER, scoped.scope());
        assertEquals("my-plugin", scoped.pluginSource());
    }

    @Test
    @DisplayName("McpTypes ServerInfo record")
    void serverInfoRecord() {
        McpTypes.ServerInfo info = new McpTypes.ServerInfo("my-server", "1.0.0");

        assertEquals("my-server", info.name());
        assertEquals("1.0.0", info.version());
    }

    @Test
    @DisplayName("McpTypes ServerCapabilities record")
    void serverCapabilitiesRecord() {
        McpTypes.ServerCapabilities caps = new McpTypes.ServerCapabilities(true, true, false, true);

        assertTrue(caps.tools());
        assertTrue(caps.resources());
        assertFalse(caps.prompts());
        assertTrue(caps.logging());
    }

    @Test
    @DisplayName("McpTypes ServerCapabilities empty")
    void serverCapabilitiesEmpty() {
        McpTypes.ServerCapabilities caps = McpTypes.ServerCapabilities.empty();

        assertFalse(caps.tools());
        assertFalse(caps.resources());
        assertFalse(caps.prompts());
        assertFalse(caps.logging());
    }

    @Test
    @DisplayName("McpTypes ConnectedMCPServer record")
    void connectedMcpServerRecord() {
        McpTypes.McpStdioServerConfig serverConfig = new McpTypes.McpStdioServerConfig(
            "node", List.of(), Map.of()
        );
        McpTypes.ScopedMcpServerConfig scoped = new McpTypes.ScopedMcpServerConfig(
            serverConfig, McpTypes.ConfigScope.LOCAL, null
        );
        McpTypes.ServerCapabilities caps = McpTypes.ServerCapabilities.empty();
        McpTypes.ServerInfo info = new McpTypes.ServerInfo("server", "1.0");

        McpTypes.ConnectedMCPServer connected = new McpTypes.ConnectedMCPServer(
            "my-server", scoped, caps, info, "Test server", () -> {}
        );

        assertEquals("my-server", connected.name());
        assertEquals("connected", connected.type());
        assertEquals(scoped, connected.config());
        assertEquals(caps, connected.capabilities());
        assertEquals(info, connected.serverInfo());
        assertEquals("Test server", connected.instructions());
    }

    @Test
    @DisplayName("McpTypes FailedMCPServer record")
    void failedMcpServerRecord() {
        McpTypes.McpStdioServerConfig serverConfig = new McpTypes.McpStdioServerConfig(
            "node", List.of(), Map.of()
        );
        McpTypes.ScopedMcpServerConfig scoped = new McpTypes.ScopedMcpServerConfig(
            serverConfig, McpTypes.ConfigScope.LOCAL, null
        );

        McpTypes.FailedMCPServer failed = new McpTypes.FailedMCPServer(
            "my-server", scoped, "Connection refused"
        );

        assertEquals("my-server", failed.name());
        assertEquals("failed", failed.type());
        assertEquals("Connection refused", failed.error());
    }

    @Test
    @DisplayName("McpTypes NeedsAuthMCPServer record")
    void needsAuthMcpServerRecord() {
        McpTypes.McpStdioServerConfig serverConfig = new McpTypes.McpStdioServerConfig(
            "node", List.of(), Map.of()
        );
        McpTypes.ScopedMcpServerConfig scoped = new McpTypes.ScopedMcpServerConfig(
            serverConfig, McpTypes.ConfigScope.LOCAL, null
        );

        McpTypes.NeedsAuthMCPServer needsAuth = new McpTypes.NeedsAuthMCPServer("my-server", scoped);

        assertEquals("my-server", needsAuth.name());
        assertEquals("needs-auth", needsAuth.type());
    }

    @Test
    @DisplayName("McpTypes PendingMCPServer record")
    void pendingMcpServerRecord() {
        McpTypes.McpStdioServerConfig serverConfig = new McpTypes.McpStdioServerConfig(
            "node", List.of(), Map.of()
        );
        McpTypes.ScopedMcpServerConfig scoped = new McpTypes.ScopedMcpServerConfig(
            serverConfig, McpTypes.ConfigScope.LOCAL, null
        );

        McpTypes.PendingMCPServer pending = new McpTypes.PendingMCPServer(
            "my-server", scoped, 2, 5
        );

        assertEquals("my-server", pending.name());
        assertEquals("pending", pending.type());
        assertEquals(2, pending.reconnectAttempt());
        assertEquals(5, pending.maxReconnectAttempts());
    }

    @Test
    @DisplayName("McpTypes DisabledMCPServer record")
    void disabledMcpServerRecord() {
        McpTypes.McpStdioServerConfig serverConfig = new McpTypes.McpStdioServerConfig(
            "node", List.of(), Map.of()
        );
        McpTypes.ScopedMcpServerConfig scoped = new McpTypes.ScopedMcpServerConfig(
            serverConfig, McpTypes.ConfigScope.LOCAL, null
        );

        McpTypes.DisabledMCPServer disabled = new McpTypes.DisabledMCPServer("my-server", scoped);

        assertEquals("my-server", disabled.name());
        assertEquals("disabled", disabled.type());
    }

    @Test
    @DisplayName("McpTypes SerializedTool record")
    void serializedToolRecord() {
        McpTypes.SerializedTool tool = new McpTypes.SerializedTool(
            "my-tool", "A test tool", Map.of("type", "object"), true, "original_tool"
        );

        assertEquals("my-tool", tool.name());
        assertEquals("A test tool", tool.description());
        assertTrue(tool.isMcp());
        assertEquals("original_tool", tool.originalToolName());
    }

    @Test
    @DisplayName("McpTypes SerializedClient record")
    void serializedClientRecord() {
        McpTypes.ServerCapabilities caps = new McpTypes.ServerCapabilities(true, false, false, false);
        McpTypes.SerializedClient client = new McpTypes.SerializedClient(
            "my-client", "connected", caps
        );

        assertEquals("my-client", client.name());
        assertEquals("connected", client.type());
        assertTrue(client.capabilities().tools());
    }

    @Test
    @DisplayName("McpTypes ServerResource record")
    void serverResourceRecord() {
        McpTypes.ServerResource resource = new McpTypes.ServerResource(
            "file:///path/to/file", "file.txt", "A text file", "text/plain", "my-server"
        );

        assertEquals("file:///path/to/file", resource.uri());
        assertEquals("file.txt", resource.name());
        assertEquals("A text file", resource.description());
        assertEquals("text/plain", resource.mimeType());
        assertEquals("my-server", resource.server());
    }

    @Test
    @DisplayName("McpTypes MCPCliState empty")
    void mcpCliStateEmpty() {
        McpTypes.MCPCliState state = McpTypes.MCPCliState.empty();

        assertTrue(state.clients().isEmpty());
        assertTrue(state.configs().isEmpty());
        assertTrue(state.tools().isEmpty());
        assertTrue(state.resources().isEmpty());
        assertTrue(state.normalizedNames().isEmpty());
    }

    @Test
    @DisplayName("McpTypes MCPCliState record")
    void mcpCliStateRecord() {
        McpTypes.SerializedTool tool = new McpTypes.SerializedTool(
            "tool", "desc", Map.of(), false, null
        );
        McpTypes.SerializedClient client = new McpTypes.SerializedClient(
            "client", "connected", McpTypes.ServerCapabilities.empty()
        );

        McpTypes.MCPCliState state = new McpTypes.MCPCliState(
            List.of(client),
            Map.of("server", new McpTypes.ScopedMcpServerConfig(
                new McpTypes.McpSdkServerConfig("sdk"),
                McpTypes.ConfigScope.USER,
                null
            )),
            List.of(tool),
            Map.of(),
            Map.of("normalized", "original")
        );

        assertEquals(1, state.clients().size());
        assertEquals(1, state.configs().size());
        assertEquals(1, state.tools().size());
        assertEquals(1, state.normalizedNames().size());
    }
}