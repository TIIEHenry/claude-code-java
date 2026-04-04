/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/agentId.ts
 */
package com.anthropic.claudecode.utils;

/**
 * Deterministic Agent ID System.
 *
 * ID Formats:
 * - Agent IDs: `agentName@teamName`
 * - Request IDs: `{requestType}-{timestamp}@{agentId}`
 */
public final class AgentId {
    private AgentId() {}

    /**
     * Agent ID components.
     */
    public record AgentIdParts(String agentName, String teamName) {}

    /**
     * Request ID components.
     */
    public record RequestIdParts(String requestType, long timestamp, String agentId) {}

    /**
     * Formats an agent ID in the format `agentName@teamName`.
     */
    public static String formatAgentId(String agentName, String teamName) {
        return agentName + "@" + teamName;
    }

    /**
     * Parses an agent ID into its components.
     * Returns null if the ID doesn't contain the @ separator.
     */
    public static AgentIdParts parseAgentId(String agentId) {
        if (agentId == null || agentId.isEmpty()) {
            return null;
        }

        int atIndex = agentId.indexOf('@');
        if (atIndex == -1) {
            return null;
        }

        return new AgentIdParts(
            agentId.substring(0, atIndex),
            agentId.substring(atIndex + 1)
        );
    }

    /**
     * Formats a request ID in the format `{requestType}-{timestamp}@{agentId}`.
     */
    public static String generateRequestId(String requestType, String agentId) {
        long timestamp = System.currentTimeMillis();
        return requestType + "-" + timestamp + "@" + agentId;
    }

    /**
     * Parses a request ID into its components.
     * Returns null if the request ID doesn't match the expected format.
     */
    public static RequestIdParts parseRequestId(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            return null;
        }

        int atIndex = requestId.indexOf('@');
        if (atIndex == -1) {
            return null;
        }

        String prefix = requestId.substring(0, atIndex);
        String agentId = requestId.substring(atIndex + 1);

        int lastDashIndex = prefix.lastIndexOf('-');
        if (lastDashIndex == -1) {
            return null;
        }

        String requestType = prefix.substring(0, lastDashIndex);
        String timestampStr = prefix.substring(lastDashIndex + 1);

        try {
            long timestamp = Long.parseLong(timestampStr);
            return new RequestIdParts(requestType, timestamp, agentId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Validate an agent name (must not contain @).
     */
    public static boolean isValidAgentName(String agentName) {
        return agentName != null && !agentName.isEmpty() && !agentName.contains("@");
    }

    /**
     * Sanitize an agent name by removing @ symbols.
     */
    public static String sanitizeAgentName(String agentName) {
        if (agentName == null) return null;
        return agentName.replace("@", "-");
    }

    /**
     * Check if a string looks like an agent ID.
     */
    public static boolean isAgentId(String id) {
        return id != null && id.contains("@");
    }

    /**
     * Extract agent name from agent ID.
     */
    public static String getAgentName(String agentId) {
        AgentIdParts parts = parseAgentId(agentId);
        return parts != null ? parts.agentName() : null;
    }

    /**
     * Extract team name from agent ID.
     */
    public static String getTeamName(String agentId) {
        AgentIdParts parts = parseAgentId(agentId);
        return parts != null ? parts.teamName() : null;
    }
}