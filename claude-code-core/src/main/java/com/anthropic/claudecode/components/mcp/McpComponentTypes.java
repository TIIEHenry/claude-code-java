/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/mcp
 */
package com.anthropic.claudecode.components.mcp;

import java.util.*;

/**
 * MCP component types - Types for MCP UI components.
 */
public final class McpComponentTypes {

    /**
     * MCP server status enum.
     */
    public enum ServerStatus {
        CONNECTED,
        CONNECTING,
        DISCONNECTED,
        ERROR,
        NEEDS_AUTH
    }

    /**
     * MCP server info for display.
     */
    public record McpServerDisplay(
        String name,
        ServerStatus status,
        int toolCount,
        int resourceCount,
        String error,
        long connectedAt
    ) {
        public String getStatusText() {
            return switch (status) {
                case CONNECTED -> "Connected";
                case CONNECTING -> "Connecting...";
                case DISCONNECTED -> "Disconnected";
                case ERROR -> "Error";
                case NEEDS_AUTH -> "Needs Authentication";
            };
        }

        public String getStatusIcon() {
            return switch (status) {
                case CONNECTED -> "✓";
                case CONNECTING -> "⏳";
                case DISCONNECTED -> "○";
                case ERROR -> "✗";
                case NEEDS_AUTH -> "🔒";
            };
        }
    }

    /**
     * MCP tool info for display.
     */
    public record McpToolDisplay(
        String name,
        String serverName,
        String description,
        boolean isEnabled
    ) {}

    /**
     * MCP resource info for display.
     */
    public record McpResourceDisplay(
        String uri,
        String serverName,
        String name,
        String mimeType
    ) {}

    /**
     * MCP connection summary.
     */
    public record McpConnectionSummary(
        int totalServers,
        int connectedServers,
        int totalTools,
        int totalResources,
        List<McpServerDisplay> servers
    ) {
        public static McpConnectionSummary empty() {
            return new McpConnectionSummary(0, 0, 0, 0, Collections.emptyList());
        }
    }

    /**
     * MCP filter options.
     */
    public record McpFilterOptions(
        String searchText,
        ServerStatus statusFilter,
        boolean showDisabled
    ) {
        public McpFilterOptions() {
            this(null, null, true);
        }
    }

    /**
     * MCP sort options.
     */
    public enum McpSortBy {
        NAME,
        STATUS,
        TOOL_COUNT,
        CONNECTED_TIME
    }
}