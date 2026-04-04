/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/agentId
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Agent ID utilities - Unique agent identification.
 */
public final class AgentIdUtils {
    private static final AtomicLong counter = new AtomicLong(0);
    private static final String PREFIX = "agent";
    private static volatile String sessionId;

    /**
     * Generate a unique agent ID.
     */
    public static String generate() {
        return PREFIX + "-" + sessionId() + "-" + counter.incrementAndGet();
    }

    /**
     * Generate a unique agent ID with prefix.
     */
    public static String generate(String prefix) {
        return prefix + "-" + sessionId() + "-" + counter.incrementAndGet();
    }

    /**
     * Generate a short agent ID.
     */
    public static String generateShort() {
        return Long.toHexString(counter.incrementAndGet());
    }

    /**
     * Get current session ID.
     */
    public static String sessionId() {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString().substring(0, 8);
        }
        return sessionId;
    }

    /**
     * Set session ID.
     */
    public static void setSessionId(String id) {
        sessionId = id;
    }

    /**
     * Get current counter value.
     */
    public static long getCounter() {
        return counter.get();
    }

    /**
     * Reset counter (for testing).
     */
    public static void resetCounter() {
        counter.set(0);
    }

    /**
     * Parse an agent ID.
     */
    public static AgentId parse(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        String[] parts = id.split("-");
        if (parts.length < 2) {
            return new AgentId(id, null, 0);
        }

        try {
            String prefix = parts[0];
            String session = parts.length > 2 ? parts[1] : null;
            long num = Long.parseLong(parts[parts.length - 1]);
            return new AgentId(id, session, num);
        } catch (NumberFormatException e) {
            return new AgentId(id, null, 0);
        }
    }

    /**
     * Validate an agent ID.
     */
    public static boolean isValid(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }

        AgentId parsed = parse(id);
        return parsed != null && parsed.number() > 0;
    }

    /**
     * Agent ID record.
     */
    public record AgentId(String fullId, String session, long number) {
        public String prefix() {
            String[] parts = fullId.split("-");
            return parts.length > 0 ? parts[0] : "";
        }
    }
}