/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AgentIdUtils.
 */
class AgentIdUtilsTest {

    @BeforeEach
    void reset() {
        AgentIdUtils.resetCounter();
        AgentIdUtils.setSessionId(null);
    }

    @Test
    @DisplayName("AgentIdUtils generate returns non-null")
    void generateNotNull() {
        String id = AgentIdUtils.generate();
        assertNotNull(id);
    }

    @Test
    @DisplayName("AgentIdUtils generate format is prefix-session-counter")
    void generateFormat() {
        String id = AgentIdUtils.generate();
        assertTrue(id.startsWith("agent-"));
        String[] parts = id.split("-");
        assertEquals(3, parts.length);
        assertEquals("agent", parts[0]);
    }

    @Test
    @DisplayName("AgentIdUtils generate increments counter")
    void generateIncrementsCounter() {
        AgentIdUtils.generate();
        assertEquals(1, AgentIdUtils.getCounter());
        AgentIdUtils.generate();
        assertEquals(2, AgentIdUtils.getCounter());
    }

    @Test
    @DisplayName("AgentIdUtils generate with custom prefix")
    void generateWithPrefix() {
        String id = AgentIdUtils.generate("custom");
        assertTrue(id.startsWith("custom-"));
        String[] parts = id.split("-");
        assertTrue(parts[0].equals("custom"));
    }

    @Test
    @DisplayName("AgentIdUtils generateShort returns hex")
    void generateShortHex() {
        String id = AgentIdUtils.generateShort();
        assertTrue(id.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("AgentIdUtils sessionId returns consistent value")
    void sessionIdConsistent() {
        String session1 = AgentIdUtils.sessionId();
        String session2 = AgentIdUtils.sessionId();
        assertEquals(session1, session2);
    }

    @Test
    @DisplayName("AgentIdUtils sessionId is 8 characters")
    void sessionIdLength() {
        String session = AgentIdUtils.sessionId();
        assertEquals(8, session.length());
    }

    @Test
    @DisplayName("AgentIdUtils setSessionId overrides")
    void setSessionIdOverride() {
        AgentIdUtils.setSessionId("test1234");
        assertEquals("test1234", AgentIdUtils.sessionId());
    }

    @Test
    @DisplayName("AgentIdUtils resetCounter sets to zero")
    void resetCounterZero() {
        AgentIdUtils.generate();
        AgentIdUtils.generate();
        AgentIdUtils.resetCounter();
        assertEquals(0, AgentIdUtils.getCounter());
    }

    @Test
    @DisplayName("AgentIdUtils parse valid id")
    void parseValidId() {
        AgentIdUtils.resetCounter();
        AgentIdUtils.setSessionId("abcd1234");
        String id = AgentIdUtils.generate();

        AgentIdUtils.AgentId parsed = AgentIdUtils.parse(id);
        assertNotNull(parsed);
        assertEquals(id, parsed.fullId());
        assertEquals("abcd1234", parsed.session());
        assertEquals(1, parsed.number());
    }

    @Test
    @DisplayName("AgentIdUtils parse null returns null")
    void parseNull() {
        assertNull(AgentIdUtils.parse(null));
    }

    @Test
    @DisplayName("AgentIdUtils parse empty returns null")
    void parseEmpty() {
        assertNull(AgentIdUtils.parse(""));
    }

    @Test
    @DisplayName("AgentIdUtils parse simple id")
    void parseSimpleId() {
        AgentIdUtils.AgentId parsed = AgentIdUtils.parse("simple");
        assertNotNull(parsed);
        assertEquals("simple", parsed.fullId());
        assertNull(parsed.session());
        assertEquals(0, parsed.number());
    }

    @Test
    @DisplayName("AgentIdUtils parse id with dash but no number")
    void parseDashNoNumber() {
        AgentIdUtils.AgentId parsed = AgentIdUtils.parse("agent-session");
        assertNotNull(parsed);
        assertEquals("agent-session", parsed.fullId());
    }

    @Test
    @DisplayName("AgentIdUtils isValid true for valid id")
    void isValidTrue() {
        AgentIdUtils.resetCounter();
        AgentIdUtils.setSessionId("abcd1234");
        String id = AgentIdUtils.generate();

        assertTrue(AgentIdUtils.isValid(id));
    }

    @Test
    @DisplayName("AgentIdUtils isValid false for null")
    void isValidNullFalse() {
        assertFalse(AgentIdUtils.isValid(null));
    }

    @Test
    @DisplayName("AgentIdUtils isValid false for empty")
    void isValidEmptyFalse() {
        assertFalse(AgentIdUtils.isValid(""));
    }

    @Test
    @DisplayName("AgentIdUtils isValid false for no number")
    void isValidNoNumberFalse() {
        assertFalse(AgentIdUtils.isValid("agent-session"));
    }

    @Test
    @DisplayName("AgentIdUtils AgentId prefix extraction")
    void agentIdPrefix() {
        AgentIdUtils.AgentId agentId = new AgentIdUtils.AgentId("custom-session-123", "session", 123);
        assertEquals("custom", agentId.prefix());
    }

    @Test
    @DisplayName("AgentIdUtils AgentId record")
    void agentIdRecord() {
        AgentIdUtils.AgentId agentId = new AgentIdUtils.AgentId("agent-abcd1234-5", "abcd1234", 5);

        assertEquals("agent-abcd1234-5", agentId.fullId());
        assertEquals("abcd1234", agentId.session());
        assertEquals(5, agentId.number());
        assertEquals("agent", agentId.prefix());
    }

    @Test
    @DisplayName("AgentIdUtils getCounter initial value")
    void getCounterInitial() {
        AgentIdUtils.resetCounter();
        assertEquals(0, AgentIdUtils.getCounter());
    }
}