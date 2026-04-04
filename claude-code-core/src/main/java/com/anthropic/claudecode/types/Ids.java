/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code types/ids.ts
 */
package com.anthropic.claudecode.types;

import java.util.regex.Pattern;

/**
 * Branded types for session and agent IDs.
 */
public final class Ids {
    private Ids() {}

    // Agent ID pattern: a + optional label + 16 hex chars
    private static final Pattern AGENT_ID_PATTERN = Pattern.compile("^a(?:.+-)?[0-9a-f]{16}$");

    /**
     * Session ID type wrapper.
     * Use for compile-time type safety to prevent mixing up session and agent IDs.
     */
    public record SessionId(String value) {
        public static SessionId of(String id) {
            return new SessionId(id);
        }
    }

    /**
     * Agent ID type wrapper.
     * Use for compile-time type safety to prevent mixing up session and agent IDs.
     */
    public record AgentId(String value) {
        public static AgentId of(String id) {
            return new AgentId(id);
        }
    }

    /**
     * Cast a raw string to SessionId.
     */
    public static SessionId asSessionId(String id) {
        return new SessionId(id);
    }

    /**
     * Cast a raw string to AgentId.
     */
    public static AgentId asAgentId(String id) {
        return new AgentId(id);
    }

    /**
     * Validate and create an AgentId.
     * Returns null if the string doesn't match the expected format.
     */
    public static AgentId toAgentId(String s) {
        if (s != null && AGENT_ID_PATTERN.matcher(s).matches()) {
            return new AgentId(s);
        }
        return null;
    }

    /**
     * Check if a string is a valid agent ID format.
     */
    public static boolean isValidAgentId(String s) {
        return s != null && AGENT_ID_PATTERN.matcher(s).matches();
    }
}