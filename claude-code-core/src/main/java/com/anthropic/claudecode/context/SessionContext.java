/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code context
 */
package com.anthropic.claudecode.context;

import java.util.*;
import java.util.concurrent.*;

/**
 * Session context for tracking session state.
 */
public class SessionContext {
    private final String sessionId;
    private final long createdAt;
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();
    private volatile boolean active = true;

    public SessionContext() {
        this.sessionId = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
    }

    public SessionContext(String sessionId) {
        this.sessionId = sessionId;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Get session ID.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Get creation time.
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Get metadata.
     */
    public Optional<Object> getMetadata(String key) {
        return Optional.ofNullable(metadata.get(key));
    }

    /**
     * Set metadata.
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * Check if session is active.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * End session.
     */
    public void end() {
        active = false;
    }

    /**
     * Get session duration in ms.
     */
    public long getDurationMs() {
        return System.currentTimeMillis() - createdAt;
    }

    @Override
    public String toString() {
        return "SessionContext{" +
            "sessionId='" + sessionId + '\'' +
            ", createdAt=" + createdAt +
            ", active=" + active +
            '}';
    }
}