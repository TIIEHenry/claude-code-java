/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AgentId.
 */
class AgentIdTest {

    @Test
    @DisplayName("AgentId formatAgentId formats correctly")
    void formatAgentId() {
        String id = AgentId.formatAgentId("myAgent", "myTeam");

        assertEquals("myAgent@myTeam", id);
    }

    @Test
    @DisplayName("AgentId parseAgentId parses valid ID")
    void parseAgentIdValid() {
        AgentId.AgentIdParts parts = AgentId.parseAgentId("myAgent@myTeam");

        assertNotNull(parts);
        assertEquals("myAgent", parts.agentName());
        assertEquals("myTeam", parts.teamName());
    }

    @Test
    @DisplayName("AgentId parseAgentId returns null for null input")
    void parseAgentIdNull() {
        AgentId.AgentIdParts parts = AgentId.parseAgentId(null);

        assertNull(parts);
    }

    @Test
    @DisplayName("AgentId parseAgentId returns null for empty string")
    void parseAgentIdEmpty() {
        AgentId.AgentIdParts parts = AgentId.parseAgentId("");

        assertNull(parts);
    }

    @Test
    @DisplayName("AgentId parseAgentId returns null for ID without @")
    void parseAgentIdNoAt() {
        AgentId.AgentIdParts parts = AgentId.parseAgentId("noAtSign");

        assertNull(parts);
    }

    @Test
    @DisplayName("AgentId generateRequestId generates valid ID")
    void generateRequestId() {
        String requestId = AgentId.generateRequestId("query", "agent@team");

        assertTrue(requestId.startsWith("query-"));
        assertTrue(requestId.endsWith("@agent@team"));
    }

    @Test
    @DisplayName("AgentId parseRequestId parses valid ID")
    void parseRequestIdValid() {
        String requestId = AgentId.generateRequestId("query", "agent@team");
        AgentId.RequestIdParts parts = AgentId.parseRequestId(requestId);

        assertNotNull(parts);
        assertEquals("query", parts.requestType());
        assertEquals("agent@team", parts.agentId());
        assertTrue(parts.timestamp() > 0);
    }

    @Test
    @DisplayName("AgentId parseRequestId returns null for null input")
    void parseRequestIdNull() {
        AgentId.RequestIdParts parts = AgentId.parseRequestId(null);

        assertNull(parts);
    }

    @Test
    @DisplayName("AgentId parseRequestId returns null for empty string")
    void parseRequestIdEmpty() {
        AgentId.RequestIdParts parts = AgentId.parseRequestId("");

        assertNull(parts);
    }

    @Test
    @DisplayName("AgentId parseRequestId returns null for ID without @")
    void parseRequestIdNoAt() {
        AgentId.RequestIdParts parts = AgentId.parseRequestId("query-123");

        assertNull(parts);
    }

    @Test
    @DisplayName("AgentId parseRequestId returns null for ID without dash")
    void parseRequestIdNoDash() {
        AgentId.RequestIdParts parts = AgentId.parseRequestId("query@agent");

        assertNull(parts);
    }

    @Test
    @DisplayName("AgentId isValidAgentName returns true for valid name")
    void isValidAgentNameTrue() {
        assertTrue(AgentId.isValidAgentName("myAgent"));
        assertTrue(AgentId.isValidAgentName("agent123"));
    }

    @Test
    @DisplayName("AgentId isValidAgentName returns false for invalid name")
    void isValidAgentNameFalse() {
        assertFalse(AgentId.isValidAgentName(null));
        assertFalse(AgentId.isValidAgentName(""));
        assertFalse(AgentId.isValidAgentName("agent@team"));
    }

    @Test
    @DisplayName("AgentId sanitizeAgentName removes @")
    void sanitizeAgentName() {
        assertEquals("agent-team", AgentId.sanitizeAgentName("agent@team"));
        assertEquals("agent", AgentId.sanitizeAgentName("agent"));
    }

    @Test
    @DisplayName("AgentId sanitizeAgentName returns null for null input")
    void sanitizeAgentNameNull() {
        assertNull(AgentId.sanitizeAgentName(null));
    }

    @Test
    @DisplayName("AgentId isAgentId returns true for valid ID")
    void isAgentIdTrue() {
        assertTrue(AgentId.isAgentId("agent@team"));
    }

    @Test
    @DisplayName("AgentId isAgentId returns false for invalid ID")
    void isAgentIdFalse() {
        assertFalse(AgentId.isAgentId(null));
        assertFalse(AgentId.isAgentId("noAtSign"));
    }

    @Test
    @DisplayName("AgentId getAgentName extracts agent name")
    void getAgentName() {
        assertEquals("myAgent", AgentId.getAgentName("myAgent@myTeam"));
    }

    @Test
    @DisplayName("AgentId getAgentName returns null for invalid ID")
    void getAgentNameInvalid() {
        assertNull(AgentId.getAgentName("noAtSign"));
        assertNull(AgentId.getAgentName(null));
    }

    @Test
    @DisplayName("AgentId getTeamName extracts team name")
    void getTeamName() {
        assertEquals("myTeam", AgentId.getTeamName("myAgent@myTeam"));
    }

    @Test
    @DisplayName("AgentId getTeamName returns null for invalid ID")
    void getTeamNameInvalid() {
        assertNull(AgentId.getTeamName("noAtSign"));
        assertNull(AgentId.getTeamName(null));
    }

    @Test
    @DisplayName("AgentId AgentIdParts record works")
    void agentIdPartsRecord() {
        AgentId.AgentIdParts parts = new AgentId.AgentIdParts("agent", "team");

        assertEquals("agent", parts.agentName());
        assertEquals("team", parts.teamName());
    }

    @Test
    @DisplayName("AgentId RequestIdParts record works")
    void requestIdPartsRecord() {
        AgentId.RequestIdParts parts = new AgentId.RequestIdParts("query", 123456L, "agent@team");

        assertEquals("query", parts.requestType());
        assertEquals(123456L, parts.timestamp());
        assertEquals("agent@team", parts.agentId());
    }
}