/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Ids.
 */
class IdsTest {

    @Test
    @DisplayName("Ids SessionId record")
    void sessionIdRecord() {
        Ids.SessionId id = new Ids.SessionId("session-123");
        assertEquals("session-123", id.value());
    }

    @Test
    @DisplayName("Ids SessionId of factory")
    void sessionIdOf() {
        Ids.SessionId id = Ids.SessionId.of("test-session");
        assertEquals("test-session", id.value());
    }

    @Test
    @DisplayName("Ids AgentId record")
    void agentIdRecord() {
        Ids.AgentId id = new Ids.AgentId("agent-456");
        assertEquals("agent-456", id.value());
    }

    @Test
    @DisplayName("Ids AgentId of factory")
    void agentIdOf() {
        Ids.AgentId id = Ids.AgentId.of("test-agent");
        assertEquals("test-agent", id.value());
    }

    @Test
    @DisplayName("Ids asSessionId creates SessionId")
    void asSessionId() {
        Ids.SessionId id = Ids.asSessionId("my-session");
        assertEquals("my-session", id.value());
    }

    @Test
    @DisplayName("Ids asAgentId creates AgentId")
    void asAgentId() {
        Ids.AgentId id = Ids.asAgentId("my-agent");
        assertEquals("my-agent", id.value());
    }

    @Test
    @DisplayName("Ids toAgentId valid format")
    void toAgentIdValid() {
        // Valid agent IDs: a + optional label + 16 hex chars
        // The label, if present, must have at least one char before the dash
        Ids.AgentId id1 = Ids.toAgentId("a1234567890abcdef");
        assertNotNull(id1);
        assertEquals("a1234567890abcdef", id1.value());

        Ids.AgentId id2 = Ids.toAgentId("alabel-1234567890abcdef");
        assertNotNull(id2);
        assertEquals("alabel-1234567890abcdef", id2.value());

        Ids.AgentId id3 = Ids.toAgentId("ax-0011223344556677");
        assertNotNull(id3);
    }

    @Test
    @DisplayName("Ids toAgentId invalid format returns null")
    void toAgentIdInvalid() {
        // Invalid: too short
        assertNull(Ids.toAgentId("a123"));

        // Invalid: wrong prefix
        assertNull(Ids.toAgentId("b1234567890abcdef"));

        // Invalid: non-hex characters
        assertNull(Ids.toAgentId("axxxxxxxxxxxxxxxx"));

        // Invalid: null
        assertNull(Ids.toAgentId(null));

        // Invalid: empty string
        assertNull(Ids.toAgentId(""));
    }

    @Test
    @DisplayName("Ids isValidAgentId returns true for valid format")
    void isValidAgentIdTrue() {
        assertTrue(Ids.isValidAgentId("a1234567890abcdef"));
        assertTrue(Ids.isValidAgentId("alabel-fedcba0987654321"));
        assertTrue(Ids.isValidAgentId("ax-abcdef0123456789"));
    }

    @Test
    @DisplayName("Ids isValidAgentId returns false for invalid format")
    void isValidAgentIdFalse() {
        assertFalse(Ids.isValidAgentId(null));
        assertFalse(Ids.isValidAgentId(""));
        assertFalse(Ids.isValidAgentId("invalid"));
        assertFalse(Ids.isValidAgentId("a123")); // Too short
        assertFalse(Ids.isValidAgentId("b1234567890abcdef")); // Wrong prefix
        assertFalse(Ids.isValidAgentId("aGHIJKL")); // Non-hex chars
    }

    @Test
    @DisplayName("Ids SessionId equality")
    void sessionIdEquality() {
        Ids.SessionId id1 = Ids.SessionId.of("same");
        Ids.SessionId id2 = Ids.SessionId.of("same");
        Ids.SessionId id3 = Ids.SessionId.of("different");

        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
    }

    @Test
    @DisplayName("Ids AgentId equality")
    void agentIdEquality() {
        Ids.AgentId id1 = Ids.AgentId.of("same");
        Ids.AgentId id2 = Ids.AgentId.of("same");
        Ids.AgentId id3 = Ids.AgentId.of("different");

        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
    }
}