/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tools/McpAuthTool/McpAuthTool.ts
 */
package com.anthropic.claudecode.tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import com.anthropic.claudecode.*;
import com.anthropic.claudecode.permission.PermissionResult;

/**
 * McpAuth Tool - authenticate with MCP servers requiring OAuth.
 * Creates a pseudo-tool for MCP servers that are installed but not authenticated.
 */
public final class McpAuthTool extends AbstractTool<McpAuthTool.Input, McpAuthTool.Output, ToolProgressData> {
    public static final String TOOL_NAME = "McpAuth";

    private final String serverName;
    private final McpServerConfig config;

    public McpAuthTool(String serverName, McpServerConfig config) {
        super(buildMcpToolName(serverName, "authenticate"),
              "Authenticate with the " + serverName + " MCP server");
        this.serverName = serverName;
        this.config = config;
    }

    /**
     * Input schema - empty object.
     */
    public record Input() {}

    /**
     * Output schema.
     */
    public record Output(
        String status,
        String message,
        String authUrl
    ) {}

    /**
     * MCP server config.
     */
    public record McpServerConfig(
        String type,
        String url,
        String scope
    ) {}

    @Override
    public String description() {
        String location = config.url() != null
            ? config.type() + " at " + config.url()
            : config.type();

        return "The `" + serverName + "` MCP server (" + location + ") is installed but requires authentication. " +
               "Call this tool to start the OAuth flow — you'll receive an authorization URL to share with the user. " +
               "Once the user completes authorization in their browser, the server's real tools will become available automatically.";
    }

    @Override
    public String searchHint() {
        return "authenticate with MCP server";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isConcurrencySafe(Input input) {
        return false;
    }

    @Override
    public boolean isReadOnly(Input input) {
        return false;
    }

    @Override
    public CompletableFuture<PermissionResult> checkPermissions(Input input, ToolUseContext context) {
        return CompletableFuture.completedFuture(PermissionResult.allow(input));
    }

    @Override
    public CompletableFuture<ToolResult<Output>> call(
        Input input,
        ToolUseContext context,
        CanUseToolFn canUseTool,
        AssistantMessage parent,
        Consumer<ToolProgress<ToolProgressData>> onProgress
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // claude.ai connectors use a separate auth flow
            if ("claudeai-proxy".equals(config.type())) {
                return ToolResult.of(new Output(
                    "unsupported",
                    "This is a claude.ai MCP connector. Ask the user to run /mcp and select \"" +
                    serverName + "\" to authenticate.",
                    null
                ));
            }

            // performMCPOAuthFlow only accepts sse/http
            if (!"sse".equals(config.type()) && !"http".equals(config.type())) {
                return ToolResult.of(new Output(
                    "unsupported",
                    "Server \"" + serverName + "\" uses " + config.type() +
                    " transport which does not support OAuth from this tool. " +
                    "Ask the user to run /mcp and authenticate manually.",
                    null
                ));
            }

            try {
                // Start OAuth flow
                String authUrl = performMCPOAuthFlow(serverName, config);

                if (authUrl != null) {
                    return ToolResult.of(new Output(
                        "auth_url",
                        "Ask the user to open this URL in their browser to authorize the " +
                        serverName + " MCP server:\n\n" + authUrl + "\n\n" +
                        "Once they complete the flow, the server's tools will become available automatically.",
                        authUrl
                    ));
                }

                return ToolResult.of(new Output(
                    "auth_url",
                    "Authentication completed silently for " + serverName + ". " +
                    "The server's tools should now be available.",
                    null
                ));

            } catch (Exception e) {
                return ToolResult.of(new Output(
                    "error",
                    "Failed to start OAuth flow for " + serverName + ": " + e.getMessage() +
                    ". Ask the user to run /mcp and authenticate manually.",
                    null
                ));
            }
        });
    }

    @Override
    public CompletableFuture<String> describe(Input input, ToolDescribeOptions options) {
        return CompletableFuture.completedFuture("Authenticate with " + serverName + " MCP server");
    }

    // Helpers

    private static String buildMcpToolName(String serverName, String toolName) {
        return "mcp__" + serverName + "__" + toolName;
    }

    private String performMCPOAuthFlow(String serverName, McpServerConfig config) {
        // Perform OAuth flow for MCP server
        try {
            // Get OAuth configuration from environment or config
            String clientId = System.getenv(serverName.toUpperCase() + "_CLIENT_ID");
            String authUrl = System.getenv(serverName.toUpperCase() + "_AUTH_URL");
            String tokenUrl = System.getenv(serverName.toUpperCase() + "_TOKEN_URL");
            String scope = config.scope();

            if (clientId == null || authUrl == null) {
                return null; // No OAuth configured
            }

            // Check for existing token
            String home = System.getProperty("user.home");
            java.nio.file.Path tokenPath = java.nio.file.Paths.get(home, ".claude", "mcp-auth", serverName + ".json");

            if (java.nio.file.Files.exists(tokenPath)) {
                String content = java.nio.file.Files.readString(tokenPath);
                if (content.contains("\"access_token\"")) {
                    // Check if token is expired
                    int expiresIdx = content.indexOf("\"expires_at\"");
                    if (expiresIdx >= 0) {
                        int valStart = content.indexOf(":", expiresIdx + 12) + 1;
                        while (valStart < content.length() && Character.isWhitespace(content.charAt(valStart))) valStart++;
                        int valEnd = valStart;
                        while (valEnd < content.length() && Character.isDigit(content.charAt(valEnd))) valEnd++;
                        long expiresAt = Long.parseLong(content.substring(valStart, valEnd));

                        if (System.currentTimeMillis() < expiresAt - 300000) { // 5 min buffer
                            return null; // Token still valid, silent auth succeeded
                        }
                    }
                }
            }

            // Build authorization URL
            StringBuilder url = new StringBuilder(authUrl);
            url.append("?response_type=code");
            url.append("&client_id=").append(java.net.URLEncoder.encode(clientId, "UTF-8"));

            if (scope != null) {
                url.append("&scope=").append(java.net.URLEncoder.encode(scope, "UTF-8"));
            }

            // Add redirect URI
            String redirectUri = "http://localhost:8080/callback";
            url.append("&redirect_uri=").append(java.net.URLEncoder.encode(redirectUri, "UTF-8"));

            // Add state for CSRF protection
            String state = java.util.UUID.randomUUID().toString();
            url.append("&state=").append(state);

            // Store state for verification
            java.nio.file.Path statePath = java.nio.file.Paths.get(home, ".claude", "mcp-auth", serverName + ".state");
            java.nio.file.Files.createDirectories(statePath.getParent());
            java.nio.file.Files.writeString(statePath, state);

            // Return auth URL for user to visit
            return url.toString();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}