/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/config.ts
 */
package com.anthropic.claudecode.services.mcp;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP configuration management.
 */
public final class McpConfig {
    private McpConfig() {}

    private static final Map<String, McpTypes.ScopedMcpServerConfig> serverConfigs = new ConcurrentHashMap<>();

    /**
     * Add an MCP server config.
     */
    public static void addServerConfig(String name, McpTypes.ScopedMcpServerConfig config) {
        serverConfigs.put(name, config);
    }

    /**
     * Remove an MCP server config.
     */
    public static void removeServerConfig(String name) {
        serverConfigs.remove(name);
    }

    /**
     * Get an MCP server config.
     */
    public static McpTypes.ScopedMcpServerConfig getServerConfig(String name) {
        return serverConfigs.get(name);
    }

    /**
     * Get all server configs.
     */
    public static Map<String, McpTypes.ScopedMcpServerConfig> getAllServerConfigs() {
        return new HashMap<>(serverConfigs);
    }

    /**
     * Check if a server exists.
     */
    public static boolean hasServer(String name) {
        return serverConfigs.containsKey(name);
    }

    /**
     * Clear all server configs.
     */
    public static void clear() {
        serverConfigs.clear();
    }

    /**
     * Parse STDIO config from JSON.
     */
    public static McpTypes.McpStdioServerConfig parseStdioConfig(Map<String, Object> json) {
        String command = (String) json.get("command");
        List<String> args = new ArrayList<>();
        if (json.get("args") instanceof List) {
            for (Object arg : (List<?>) json.get("args")) {
                args.add(String.valueOf(arg));
            }
        }

        Map<String, String> env = new HashMap<>();
        if (json.get("env") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> envMap = (Map<String, Object>) json.get("env");
            for (Map.Entry<String, Object> entry : envMap.entrySet()) {
                env.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        return new McpTypes.McpStdioServerConfig(command, args, env);
    }

    /**
     * Parse SSE config from JSON.
     */
    public static McpTypes.McpSSEServerConfig parseSseConfig(Map<String, Object> json) {
        String url = (String) json.get("url");

        Map<String, String> headers = new HashMap<>();
        if (json.get("headers") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> headersMap = (Map<String, Object>) json.get("headers");
            for (Map.Entry<String, Object> entry : headersMap.entrySet()) {
                headers.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        String headersHelper = (String) json.get("headersHelper");

        // Parse OAuth if present
        McpTypes.McpOAuthConfig oauth = null;
        if (json.get("oauth") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> oauthMap = (Map<String, Object>) json.get("oauth");
            oauth = new McpTypes.McpOAuthConfig(
                (String) oauthMap.get("clientId"),
                oauthMap.get("callbackPort") != null ? ((Number) oauthMap.get("callbackPort")).intValue() : null,
                (String) oauthMap.get("authServerMetadataUrl"),
                Boolean.TRUE.equals(oauthMap.get("xaa"))
            );
        }

        return new McpTypes.McpSSEServerConfig(url, headers, headersHelper, oauth);
    }
}