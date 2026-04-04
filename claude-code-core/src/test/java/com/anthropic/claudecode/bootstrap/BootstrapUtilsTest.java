/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BootstrapUtils.
 */
class BootstrapUtilsTest {

    @BeforeEach
    void setUp() {
        AppState.resetState();
    }

    @Test
    @DisplayName("BootstrapUtils generateSessionId returns non-null")
    void generateSessionId() {
        String id = BootstrapUtils.generateSessionId();

        assertNotNull(id);
        assertTrue(id.startsWith("session_"));
        assertEquals(32, id.length()); // "session_" (8) + 24 hex chars
    }

    @Test
    @DisplayName("BootstrapUtils generateSessionId generates unique IDs")
    void generateSessionIdUnique() {
        String id1 = BootstrapUtils.generateSessionId();
        String id2 = BootstrapUtils.generateSessionId();

        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("BootstrapUtils generateAgentId without label")
    void generateAgentIdWithoutLabel() {
        String id = BootstrapUtils.generateAgentId(null);

        assertNotNull(id);
        assertTrue(id.startsWith("a"));
        assertEquals(17, id.length()); // "a" + 16 hex chars
    }

    @Test
    @DisplayName("BootstrapUtils generateAgentId with empty label")
    void generateAgentIdWithEmptyLabel() {
        String id = BootstrapUtils.generateAgentId("");

        assertNotNull(id);
        assertTrue(id.startsWith("a"));
        assertEquals(17, id.length());
    }

    @Test
    @DisplayName("BootstrapUtils generateAgentId with label")
    void generateAgentIdWithLabel() {
        String id = BootstrapUtils.generateAgentId("MyAgent");

        assertNotNull(id);
        assertTrue(id.startsWith("amyagent-")); // Label is lowercased
        assertTrue(id.length() > 17);
    }

    @Test
    @DisplayName("BootstrapUtils generateAgentId removes special chars from label")
    void generateAgentIdSanitizesLabel() {
        String id = BootstrapUtils.generateAgentId("Test-Agent_123!");

        assertNotNull(id);
        // Label is sanitized: "Test-Agent_123!" -> "testagent123"
        // Then combined as: "a" + "testagent123" + "-" + hex
        assertTrue(id.startsWith("atestagent123-"));
        // The only dash should be the separator between label and hex
        int dashCount = id.length() - id.replace("-", "").length();
        assertEquals(1, dashCount);
    }

    @Test
    @DisplayName("BootstrapUtils generateAgentId generates unique IDs")
    void generateAgentIdUnique() {
        String id1 = BootstrapUtils.generateAgentId(null);
        String id2 = BootstrapUtils.generateAgentId(null);

        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("BootstrapUtils initialize sets up state")
    void initialize() {
        BootstrapUtils.initialize("/home/user/project", true);

        AppState state = AppState.getInstance();
        assertEquals("/home/user/project", state.getOriginalCwd());
        assertEquals("/home/user/project", state.getProjectRoot());
        assertEquals("/home/user/project", state.getCwd());
        assertTrue(state.isInteractive());
        assertNotNull(state.getSessionId());
    }

    @Test
    @DisplayName("BootstrapUtils initialize with non-interactive")
    void initializeNonInteractive() {
        BootstrapUtils.initialize("/home/user/project", false);

        AppState state = AppState.getInstance();
        assertFalse(state.isInteractive());
    }

    @Test
    @DisplayName("BootstrapUtils shutdown resets state")
    void shutdown() {
        BootstrapUtils.initialize("/home/user/project", true);
        AppState state = AppState.getInstance();
        assertNotNull(state.getSessionId());

        BootstrapUtils.shutdown();

        // After shutdown, getting a new instance should have default values
        AppState newState = AppState.getInstance();
        assertEquals("", newState.getOriginalCwd());
        assertEquals("", newState.getProjectRoot());
    }
}