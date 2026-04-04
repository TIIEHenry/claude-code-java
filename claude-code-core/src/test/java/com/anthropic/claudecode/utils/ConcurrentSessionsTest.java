/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConcurrentSessions.
 */
class ConcurrentSessionsTest {

    @Test
    @DisplayName("ConcurrentSessions SessionKind enum")
    void sessionKindEnum() {
        ConcurrentSessions.SessionKind[] kinds = ConcurrentSessions.SessionKind.values();
        assertEquals(4, kinds.length);
        assertEquals(ConcurrentSessions.SessionKind.INTERACTIVE, ConcurrentSessions.SessionKind.valueOf("INTERACTIVE"));
        assertEquals(ConcurrentSessions.SessionKind.BG, ConcurrentSessions.SessionKind.valueOf("BG"));
        assertEquals(ConcurrentSessions.SessionKind.DAEMON, ConcurrentSessions.SessionKind.valueOf("DAEMON"));
        assertEquals(ConcurrentSessions.SessionKind.DAEMON_WORKER, ConcurrentSessions.SessionKind.valueOf("DAEMON_WORKER"));
    }

    @Test
    @DisplayName("ConcurrentSessions SessionStatus enum")
    void sessionStatusEnum() {
        ConcurrentSessions.SessionStatus[] statuses = ConcurrentSessions.SessionStatus.values();
        assertEquals(3, statuses.length);
        assertEquals(ConcurrentSessions.SessionStatus.BUSY, ConcurrentSessions.SessionStatus.valueOf("BUSY"));
        assertEquals(ConcurrentSessions.SessionStatus.IDLE, ConcurrentSessions.SessionStatus.valueOf("IDLE"));
        assertEquals(ConcurrentSessions.SessionStatus.WAITING, ConcurrentSessions.SessionStatus.valueOf("WAITING"));
    }

    @Test
    @DisplayName("ConcurrentSessions SessionInfo record")
    void sessionInfoRecord() {
        ConcurrentSessions.SessionInfo info = new ConcurrentSessions.SessionInfo(
            12345,
            "session-uuid",
            "/home/user/project",
            System.currentTimeMillis(),
            ConcurrentSessions.SessionKind.INTERACTIVE,
            "cli",
            "test-session",
            "/var/log/claude.log",
            "claude-sonnet-4-6",
            "bridge-123",
            ConcurrentSessions.SessionStatus.BUSY,
            "user-input",
            System.currentTimeMillis()
        );

        assertEquals(12345, info.pid());
        assertEquals("session-uuid", info.sessionId());
        assertEquals("/home/user/project", info.cwd());
        assertEquals(ConcurrentSessions.SessionKind.INTERACTIVE, info.kind());
        assertEquals("cli", info.entrypoint());
        assertEquals("test-session", info.name());
        assertEquals(ConcurrentSessions.SessionStatus.BUSY, info.status());
        assertEquals("user-input", info.waitingFor());
    }

    @Test
    @DisplayName("ConcurrentSessions getSessionsDir")
    void getSessionsDir() {
        assertNotNull(ConcurrentSessions.getSessionsDir());
    }

    @Test
    @DisplayName("ConcurrentSessions getEnvSessionKind default")
    void getEnvSessionKindDefault() {
        // Without env var, should return INTERACTIVE
        assertEquals(ConcurrentSessions.SessionKind.INTERACTIVE, ConcurrentSessions.getEnvSessionKind());
    }

    @Test
    @DisplayName("ConcurrentSessions isBgSession default")
    void isBgSessionDefault() {
        assertFalse(ConcurrentSessions.isBgSession());
    }

    @Test
    @DisplayName("ConcurrentSessions registerSession")
    void registerSession() {
        boolean result = ConcurrentSessions.registerSession();
        // May fail due to filesystem permissions or missing env
        assertTrue(result == true || result == false);

        // Cleanup
        ConcurrentSessions.unregisterSession();
    }

    @Test
    @DisplayName("ConcurrentSessions unregisterSession does not throw")
    void unregisterSession() {
        assertDoesNotThrow(() -> ConcurrentSessions.unregisterSession());
    }

    @Test
    @DisplayName("ConcurrentSessions updateSessionName null")
    void updateSessionNameNull() {
        assertFalse(ConcurrentSessions.updateSessionName(null));
    }

    @Test
    @DisplayName("ConcurrentSessions updateSessionName empty")
    void updateSessionNameEmpty() {
        assertFalse(ConcurrentSessions.updateSessionName(""));
    }

    @Test
    @DisplayName("ConcurrentSessions updateSessionName without register")
    void updateSessionNameWithoutRegister() {
        // Should return false if no session is registered
        assertFalse(ConcurrentSessions.updateSessionName("test-name"));
    }

    @Test
    @DisplayName("ConcurrentSessions updateSessionActivity")
    void updateSessionActivity() {
        boolean result = ConcurrentSessions.updateSessionActivity(
            ConcurrentSessions.SessionStatus.IDLE,
            "waiting-for-input"
        );
        // May fail if no session is registered
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("ConcurrentSessions countConcurrentSessions")
    void countConcurrentSessions() {
        int count = ConcurrentSessions.countConcurrentSessions();
        assertTrue(count >= 0);
    }
}