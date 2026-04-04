/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/types.ts
 */
package com.anthropic.claudecode.services.mcp;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * MCP configuration and connection types.
 */
public final class McpTypes {
    private McpTypes() {}

    /**
     * Configuration scope enum.
     */
    public enum ConfigScope {
        LOCAL,
        USER,
        PROJECT,
        DYNAMIC,
        ENTERPRISE,
        CLAUDEAI,
        MANAGED
    }

    /**
     * Transport type enum.
     */
    public enum Transport {
        STDIO,
        SSE,
        SSE_IDE,
        HTTP,
        WS,
        SDK
    }

    /**
     * Base MCP server config interface.
     */
    public sealed interface McpServerConfig permits
        McpStdioServerConfig,
        McpSSEServerConfig,
        McpSSEIDEServerConfig,
        McpWebSocketIDEServerConfig,
        McpHTTPServerConfig,
        McpWebSocketServerConfig,
        McpSdkServerConfig,
        McpClaudeAIProxyServerConfig {
        Transport type();
    }

    /**
     * OAuth config for MCP servers.
     */
    public record McpOAuthConfig(
        String clientId,
        Integer callbackPort,
        String authServerMetadataUrl,
        boolean xaa
    ) {}

    /**
     * STDIO server config.
     */
    public record McpStdioServerConfig(
        String command,
        List<String> args,
        Map<String, String> env
    ) implements McpServerConfig {
        @Override
        public Transport type() { return Transport.STDIO; }
    }

    /**
     * SSE server config.
     */
    public record McpSSEServerConfig(
        String url,
        Map<String, String> headers,
        String headersHelper,
        McpOAuthConfig oauth
    ) implements McpServerConfig {
        @Override
        public Transport type() { return Transport.SSE; }
    }

    /**
     * SSE IDE server config (internal).
     */
    public record McpSSEIDEServerConfig(
        String url,
        String ideName,
        boolean ideRunningInWindows
    ) implements McpServerConfig {
        @Override
        public Transport type() { return Transport.SSE_IDE; }
    }

    /**
     * WebSocket IDE server config (internal).
     */
    public record McpWebSocketIDEServerConfig(
        String url,
        String ideName,
        String authToken,
        boolean ideRunningInWindows
    ) implements McpServerConfig {
        @Override
        public Transport type() { return Transport.WS; }
    }

    /**
     * HTTP server config.
     */
    public record McpHTTPServerConfig(
        String url,
        Map<String, String> headers,
        String headersHelper,
        McpOAuthConfig oauth
    ) implements McpServerConfig {
        @Override
        public Transport type() { return Transport.HTTP; }
    }

    /**
     * WebSocket server config.
     */
    public record McpWebSocketServerConfig(
        String url,
        Map<String, String> headers,
        String headersHelper
    ) implements McpServerConfig {
        @Override
        public Transport type() { return Transport.WS; }
    }

    /**
     * SDK server config.
     */
    public record McpSdkServerConfig(String name) implements McpServerConfig {
        @Override
        public Transport type() { return Transport.SDK; }
    }

    /**
     * Claude.ai proxy server config.
     */
    public record McpClaudeAIProxyServerConfig(String url, String id) implements McpServerConfig {
        @Override
        public Transport type() { return Transport.SSE; }
    }

    /**
     * Scoped MCP server config.
     */
    public record ScopedMcpServerConfig(
        McpServerConfig config,
        ConfigScope scope,
        String pluginSource
    ) {}

    /**
     * Server info record.
     */
    public record ServerInfo(String name, String version) {}

    /**
     * Server capabilities record.
     */
    public record ServerCapabilities(
        boolean tools,
        boolean resources,
        boolean prompts,
        boolean logging
    ) {
        public static ServerCapabilities empty() {
            return new ServerCapabilities(false, false, false, false);
        }
    }

    /**
     * MCP server connection types.
     */
    public sealed interface MCPServerConnection permits
        ConnectedMCPServer,
        FailedMCPServer,
        NeedsAuthMCPServer,
        PendingMCPServer,
        DisabledMCPServer {
        String name();
        String type();
        ScopedMcpServerConfig config();
    }

    /**
     * Connected MCP server.
     */
    public record ConnectedMCPServer(
        String name,
        ScopedMcpServerConfig config,
        ServerCapabilities capabilities,
        ServerInfo serverInfo,
        String instructions,
        Runnable cleanup
    ) implements MCPServerConnection {
        @Override
        public String type() { return "connected"; }
    }

    /**
     * Failed MCP server.
     */
    public record FailedMCPServer(
        String name,
        ScopedMcpServerConfig config,
        String error
    ) implements MCPServerConnection {
        @Override
        public String type() { return "failed"; }
    }

    /**
     * MCP server needing auth.
     */
    public record NeedsAuthMCPServer(
        String name,
        ScopedMcpServerConfig config
    ) implements MCPServerConnection {
        @Override
        public String type() { return "needs-auth"; }
    }

    /**
     * Pending MCP server.
     */
    public record PendingMCPServer(
        String name,
        ScopedMcpServerConfig config,
        int reconnectAttempt,
        int maxReconnectAttempts
    ) implements MCPServerConnection {
        @Override
        public String type() { return "pending"; }
    }

    /**
     * Disabled MCP server.
     */
    public record DisabledMCPServer(
        String name,
        ScopedMcpServerConfig config
    ) implements MCPServerConnection {
        @Override
        public String type() { return "disabled"; }
    }

    /**
     * Serialized tool for CLI state.
     */
    public record SerializedTool(
        String name,
        String description,
        Map<String, Object> inputJSONSchema,
        boolean isMcp,
        String originalToolName
    ) {}

    /**
     * Serialized client for CLI state.
     */
    public record SerializedClient(
        String name,
        String type,
        ServerCapabilities capabilities
    ) {}

    /**
     * Resource from MCP server.
     */
    public record ServerResource(
        String uri,
        String name,
        String description,
        String mimeType,
        String server
    ) {}

    /**
     * MCP CLI state.
     */
    public record MCPCliState(
        List<SerializedClient> clients,
        Map<String, ScopedMcpServerConfig> configs,
        List<SerializedTool> tools,
        Map<String, List<ServerResource>> resources,
        Map<String, String> normalizedNames
    ) {
        public static MCPCliState empty() {
            return new MCPCliState(
                new ArrayList<>(),
                new HashMap<>(),
                new ArrayList<>(),
                new HashMap<>(),
                new HashMap<>()
            );
        }
    }
}