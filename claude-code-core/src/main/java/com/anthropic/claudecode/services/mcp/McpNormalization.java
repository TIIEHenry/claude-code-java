/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/normalization.ts
 */
package com.anthropic.claudecode.services.mcp;

/**
 * Pure utility functions for MCP name normalization.
 */
public final class McpNormalization {
    private McpNormalization() {}

    // Claude.ai server names are prefixed with this string
    private static final String CLAUDEAI_SERVER_PREFIX = "claude.ai ";

    /**
     * Normalize server names to be compatible with the API pattern ^[a-zA-Z0-9_-]{1,64}$.
     * Replaces any invalid characters (including dots and spaces) with underscores.
     *
     * For claude.ai servers (names starting with "claude.ai "), also collapses
     * consecutive underscores and strips leading/trailing underscores to prevent
     * interference with the __ delimiter used in MCP tool names.
     */
    public static String normalizeNameForMCP(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // Replace invalid characters with underscore
        String normalized = name.replaceAll("[^a-zA-Z0-9_-]", "_");

        // For claude.ai servers, collapse underscores and strip edges
        if (name.startsWith(CLAUDEAI_SERVER_PREFIX)) {
            normalized = normalized.replaceAll("_+", "_")
                                   .replaceAll("^_|_$", "");
        }

        return normalized;
    }

    /**
     * Validate that a name matches MCP naming requirements.
     */
    public static boolean isValidMcpName(String name) {
        if (name == null || name.isEmpty() || name.length() > 64) {
            return false;
        }
        return name.matches("^[a-zA-Z0-9_-]+$");
    }

    /**
     * Generate a valid MCP name from any string.
     * More aggressive than normalizeNameForMCP - ensures result is always valid.
     */
    public static String toValidMcpName(String name) {
        if (name == null || name.isEmpty()) {
            return "unnamed";
        }

        String normalized = normalizeNameForMCP(name);

        // Truncate to 64 characters
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64);
        }

        // Ensure non-empty after truncation
        if (normalized.isEmpty()) {
            return "unnamed";
        }

        // Strip leading/trailing underscores for cleaner names
        normalized = normalized.replaceAll("^_+|_+$", "");

        return normalized.isEmpty() ? "unnamed" : normalized;
    }
}