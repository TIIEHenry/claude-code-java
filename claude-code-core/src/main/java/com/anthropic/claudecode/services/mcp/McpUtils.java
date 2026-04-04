/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/utils.ts
 */
package com.anthropic.claudecode.services.mcp;

import java.util.*;
import java.util.regex.Pattern;

/**
 * MCP utility functions.
 */
public final class McpUtils {
    private McpUtils() {}

    // Pattern for MCP tool names: mcp__<server>__<tool>
    private static final Pattern MCP_TOOL_PATTERN = Pattern.compile("^mcp__(.+)__(.+)$");

    /**
     * Normalize MCP tool name for use as tool name.
     * Converts "server/tool" to "mcp__server__tool".
     */
    public static String normalizeToolName(String serverName, String toolName) {
        return "mcp__" + serverName + "__" + toolName;
    }

    /**
     * Parse an MCP tool name into server and tool components.
     */
    public static ParsedToolName parseToolName(String fullName) {
        if (fullName == null || !fullName.startsWith("mcp__")) {
            return null;
        }

        String[] parts = fullName.split("__");
        if (parts.length < 3) {
            return null;
        }

        String serverName = parts[1];
        String toolName = String.join("__", Arrays.copyOfRange(parts, 2, parts.length));

        return new ParsedToolName(serverName, toolName);
    }

    /**
     * Check if a tool name is an MCP tool.
     */
    public static boolean isMcpTool(String toolName) {
        return toolName != null && toolName.startsWith("mcp__");
    }

    /**
     * Get server name from MCP tool name.
     */
    public static String getServerName(String toolName) {
        ParsedToolName parsed = parseToolName(toolName);
        return parsed != null ? parsed.serverName() : null;
    }

    /**
     * Get original tool name from MCP tool name.
     */
    public static String getOriginalToolName(String toolName) {
        ParsedToolName parsed = parseToolName(toolName);
        return parsed != null ? parsed.toolName() : null;
    }

    /**
     * Sanitize server name for use in tool names.
     */
    public static String sanitizeServerName(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown";
        }
        // Replace non-alphanumeric characters with underscore
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * Sanitize tool name for use in MCP tool names.
     */
    public static String sanitizeToolNamePart(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown";
        }
        // Replace non-alphanumeric characters with underscore
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    /**
     * Build environment map for STDIO server.
     */
    public static Map<String, String> buildStdioEnv(Map<String, String> serverEnv) {
        Map<String, String> env = new HashMap<>(System.getenv());

        // Add server-specific env vars
        if (serverEnv != null) {
            env.putAll(serverEnv);
        }

        // Add PATH if not present
        if (!env.containsKey("PATH")) {
            env.put("PATH", "/usr/local/bin:/usr/bin:/bin");
        }

        return env;
    }

    /**
     * Check if URL is an official MCP URL.
     */
    public static boolean isOfficialMcpUrl(String url) {
        if (url == null) return false;

        // Official MCP registry URLs
        return url.startsWith("https://github.com/anthropics/") ||
               url.startsWith("https://github.com/modelcontextprotocol/") ||
               url.contains("mcp.anthropic.com");
    }

    /**
     * Parsed tool name record.
     */
    public record ParsedToolName(String serverName, String toolName) {}
}