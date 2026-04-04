/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/mcpStringUtils.ts
 */
package com.anthropic.claudecode.services.mcp;

import java.util.*;

/**
 * Pure string utility functions for MCP tool/server name parsing.
 * This file has no heavy dependencies to keep it lightweight for
 * consumers that only need string parsing.
 */
public final class McpStringUtils {
    private McpStringUtils() {}

    /**
     * MCP tool info parsed from a string.
     */
    public record McpInfo(String serverName, String toolName) {}

    /**
     * Extracts MCP server information from a tool name string.
     * Expected format: "mcp__serverName__toolName"
     *
     * Known limitation: If a server name contains "__", parsing will be incorrect.
     * For example, "mcp__my__server__tool" would parse as server="my" and tool="server__tool"
     * instead of server="my__server" and tool="tool".
     */
    public static McpInfo mcpInfoFromString(String toolString) {
        if (toolString == null || !toolString.startsWith("mcp__")) {
            return null;
        }

        String[] parts = toolString.split("__");
        if (parts.length < 3) {
            return null;
        }

        String mcpPart = parts[0];
        String serverName = parts[1];

        if (!"mcp".equals(mcpPart) || serverName == null || serverName.isEmpty()) {
            return null;
        }

        // Join all parts after server name to preserve double underscores in tool names
        String toolName = null;
        if (parts.length > 2) {
            toolName = String.join("__", Arrays.copyOfRange(parts, 2, parts.length));
        }

        return new McpInfo(serverName, toolName);
    }

    /**
     * Generates the MCP tool/command name prefix for a given server.
     */
    public static String getMcpPrefix(String serverName) {
        return "mcp__" + McpNormalization.normalizeNameForMCP(serverName) + "__";
    }

    /**
     * Builds a fully qualified MCP tool name from server and tool names.
     * Inverse of mcpInfoFromString().
     */
    public static String buildMcpToolName(String serverName, String toolName) {
        return getMcpPrefix(serverName) + McpNormalization.normalizeNameForMCP(toolName);
    }

    /**
     * Returns the name to use for permission rule matching.
     * For MCP tools, uses the fully qualified mcp__server__tool name so that
     * deny rules targeting builtins don't match unprefixed MCP replacements.
     */
    public static String getToolNameForPermissionCheck(ToolInfo tool) {
        if (tool.mcpInfo() != null) {
            return buildMcpToolName(tool.mcpInfo().serverName(), tool.mcpInfo().toolName());
        }
        return tool.name();
    }

    /**
     * Tool info for permission checks.
     */
    public record ToolInfo(String name, McpInfo mcpInfo) {}

    /**
     * Extracts the display name from an MCP tool/command name.
     */
    public static String getMcpDisplayName(String fullName, String serverName) {
        String prefix = getMcpPrefix(serverName);
        return fullName.replace(prefix, "");
    }

    /**
     * Extracts just the tool/command display name from a userFacingName.
     * Example: "github - Add comment to issue (MCP)" -> "Add comment to issue"
     */
    public static String extractMcpToolDisplayName(String userFacingName) {
        if (userFacingName == null || userFacingName.isEmpty()) {
            return userFacingName;
        }

        // First, remove the (MCP) suffix if present
        String withoutSuffix = userFacingName.replaceAll("\\s*\\(MCP\\)\\s*$", "");

        // Trim the result
        withoutSuffix = withoutSuffix.trim();

        // Then, remove the server prefix (everything before " - ")
        int dashIndex = withoutSuffix.indexOf(" - ");
        if (dashIndex != -1) {
            return withoutSuffix.substring(dashIndex + 3).trim();
        }

        // If no dash found, return the string without (MCP)
        return withoutSuffix;
    }

    /**
     * Check if a string is an MCP tool name.
     */
    public static boolean isMcpToolName(String name) {
        return name != null && name.startsWith("mcp__");
    }

    /**
     * Parse tool name parts from full name.
     */
    public static String[] parseToolNameParts(String fullName) {
        if (fullName == null) {
            return new String[0];
        }
        return fullName.split("__");
    }
}