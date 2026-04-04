/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.sessions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SessionService.
 */
class SessionServiceTest {

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService();
    }

    @Test
    @DisplayName("SessionService SessionType enum values")
    void sessionTypeEnum() {
        SessionService.SessionType[] types = SessionService.SessionType.values();
        assertEquals(5, types.length);
        assertEquals(SessionService.SessionType.CONVERSATION, SessionService.SessionType.valueOf("CONVERSATION"));
        assertEquals(SessionService.SessionType.TASK, SessionService.SessionType.valueOf("TASK"));
        assertEquals(SessionService.SessionType.AGENT, SessionService.SessionType.valueOf("AGENT"));
        assertEquals(SessionService.SessionType.WORKFLOW, SessionService.SessionType.valueOf("WORKFLOW"));
        assertEquals(SessionService.SessionType.MCP_SESSION, SessionService.SessionType.valueOf("MCP_SESSION"));
    }

    @Test
    @DisplayName("SessionService SessionStatus enum values")
    void sessionStatusEnum() {
        SessionService.SessionStatus[] statuses = SessionService.SessionStatus.values();
        assertEquals(5, statuses.length);
        assertEquals(SessionService.SessionStatus.ACTIVE, SessionService.SessionStatus.valueOf("ACTIVE"));
        assertEquals(SessionService.SessionStatus.PAUSED, SessionService.SessionStatus.valueOf("PAUSED"));
        assertEquals(SessionService.SessionStatus.COMPLETED, SessionService.SessionStatus.valueOf("COMPLETED"));
        assertEquals(SessionService.SessionStatus.ERROR, SessionService.SessionStatus.valueOf("ERROR"));
        assertEquals(SessionService.SessionStatus.ARCHIVED, SessionService.SessionStatus.valueOf("ARCHIVED"));
    }

    @Test
    @DisplayName("SessionService MessageRole enum values")
    void messageRoleEnum() {
        SessionService.MessageRole[] roles = SessionService.MessageRole.values();
        assertEquals(4, roles.length);
        assertEquals(SessionService.MessageRole.USER, SessionService.MessageRole.valueOf("USER"));
        assertEquals(SessionService.MessageRole.ASSISTANT, SessionService.MessageRole.valueOf("ASSISTANT"));
        assertEquals(SessionService.MessageRole.SYSTEM, SessionService.MessageRole.valueOf("SYSTEM"));
        assertEquals(SessionService.MessageRole.TOOL, SessionService.MessageRole.valueOf("TOOL"));
    }

    @Test
    @DisplayName("SessionService ExportFormat enum values")
    void exportFormatEnum() {
        SessionService.ExportFormat[] formats = SessionService.ExportFormat.values();
        assertEquals(3, formats.length);
        assertEquals(SessionService.ExportFormat.JSON, SessionService.ExportFormat.valueOf("JSON"));
        assertEquals(SessionService.ExportFormat.MARKDOWN, SessionService.ExportFormat.valueOf("MARKDOWN"));
        assertEquals(SessionService.ExportFormat.TEXT, SessionService.ExportFormat.valueOf("TEXT"));
    }

    @Test
    @DisplayName("SessionService AttachmentInfo record")
    void attachmentInfoRecord() {
        SessionService.AttachmentInfo attachment = new SessionService.AttachmentInfo(
            "att-1", "file.txt", "text/plain", "/path/to/file", 1024
        );

        assertEquals("att-1", attachment.id());
        assertEquals("file.txt", attachment.name());
        assertEquals("text/plain", attachment.type());
        assertEquals("/path/to/file", attachment.path());
        assertEquals(1024, attachment.size());
    }

    @Test
    @DisplayName("SessionService MessageEntry record")
    void messageEntryRecord() {
        Instant now = Instant.now();
        SessionService.MessageEntry message = new SessionService.MessageEntry(
            "msg-1",
            SessionService.MessageRole.USER,
            "Hello world",
            List.of(),
            now,
            Map.of("key", "value")
        );

        assertEquals("msg-1", message.id());
        assertEquals(SessionService.MessageRole.USER, message.role());
        assertEquals("Hello world", message.content());
        assertTrue(message.attachments().isEmpty());
        assertEquals(now, message.timestamp());
        assertEquals("value", message.metadata().get("key"));
    }

    @Test
    @DisplayName("SessionService Session record")
    void sessionRecord() {
        Instant now = Instant.now();
        SessionService.Session session = new SessionService.Session(
            "session-1",
            "Test Session",
            SessionService.SessionType.CONVERSATION,
            List.of(),
            Map.of(),
            now,
            now,
            SessionService.SessionStatus.ACTIVE,
            "/project/path"
        );

        assertEquals("session-1", session.id());
        assertEquals("Test Session", session.title());
        assertEquals(SessionService.SessionType.CONVERSATION, session.type());
        assertEquals(SessionService.SessionStatus.ACTIVE, session.status());
        assertEquals("/project/path", session.projectPath());
        assertEquals(0, session.getMessageCount());
    }

    @Test
    @DisplayName("SessionService Session.create factory method")
    void sessionCreate() {
        SessionService.Session session = SessionService.Session.create("id-123", "New Session", "/project");

        assertEquals("id-123", session.id());
        assertEquals("New Session", session.title());
        assertEquals(SessionService.SessionType.CONVERSATION, session.type());
        assertEquals(SessionService.SessionStatus.ACTIVE, session.status());
        assertEquals("/project", session.projectPath());
        assertTrue(session.messages().isEmpty());
        assertNotNull(session.createdAt());
        assertNotNull(session.updatedAt());
    }

    @Test
    @DisplayName("SessionService Session addMessage")
    void sessionAddMessage() {
        SessionService.Session session = SessionService.Session.create("id", "Test", "/project");
        SessionService.MessageEntry msg = new SessionService.MessageEntry(
            "msg-1", SessionService.MessageRole.USER, "content", List.of(), Instant.now(), Map.of()
        );

        SessionService.Session updated = session.addMessage(msg);

        assertEquals(1, updated.getMessageCount());
        assertEquals(0, session.getMessageCount()); // Original unchanged
    }

    @Test
    @DisplayName("SessionService Session updateTitle")
    void sessionUpdateTitle() {
        SessionService.Session session = SessionService.Session.create("id", "Old Title", "/project");

        SessionService.Session updated = session.updateTitle("New Title");

        assertEquals("New Title", updated.title());
        assertEquals("Old Title", session.title()); // Original unchanged
    }

    @Test
    @DisplayName("SessionService Session setStatus")
    void sessionSetStatus() {
        SessionService.Session session = SessionService.Session.create("id", "Test", "/project");

        SessionService.Session updated = session.setStatus(SessionService.SessionStatus.PAUSED);

        assertEquals(SessionService.SessionStatus.PAUSED, updated.status());
        assertEquals(SessionService.SessionStatus.ACTIVE, session.status()); // Original unchanged
    }

    @Test
    @DisplayName("SessionService Session getLastMessage")
    void sessionGetLastMessage() {
        SessionService.Session empty = SessionService.Session.create("id", "Test", "/project");
        assertTrue(empty.getLastMessage().isEmpty());

        SessionService.MessageEntry msg1 = new SessionService.MessageEntry(
            "msg-1", SessionService.MessageRole.USER, "first", List.of(), Instant.now(), Map.of()
        );
        SessionService.MessageEntry msg2 = new SessionService.MessageEntry(
            "msg-2", SessionService.MessageRole.ASSISTANT, "second", List.of(), Instant.now(), Map.of()
        );

        SessionService.Session session = empty.addMessage(msg1).addMessage(msg2);
        Optional<SessionService.MessageEntry> last = session.getLastMessage();

        assertTrue(last.isPresent());
        assertEquals("msg-2", last.get().id());
    }

    @Test
    @DisplayName("SessionService createSession creates and activates")
    void createSession() {
        SessionService.Session session = sessionService.createSession("My Session", "/project");

        assertNotNull(session);
        assertEquals("My Session", session.title());
        assertEquals("/project", session.projectPath());
        assertEquals(session.id(), sessionService.getActiveSession().orElseThrow().id());
    }

    @Test
    @DisplayName("SessionService getSession returns session")
    void getSession() {
        SessionService.Session created = sessionService.createSession("Test", "/project");

        SessionService.Session retrieved = sessionService.getSession(created.id());

        assertEquals(created, retrieved);
    }

    @Test
    @DisplayName("SessionService getSession returns null for missing")
    void getSessionMissing() {
        assertNull(sessionService.getSession("non-existent"));
    }

    @Test
    @DisplayName("SessionService getActiveSession returns empty when none")
    void getActiveSessionNone() {
        Optional<SessionService.Session> active = sessionService.getActiveSession();
        assertTrue(active.isEmpty());
    }

    @Test
    @DisplayName("SessionService setActiveSession changes active")
    void setActiveSession() {
        SessionService.Session s1 = sessionService.createSession("S1", "/p1");
        SessionService.Session s2 = sessionService.createSession("S2", "/p2");

        assertEquals(s2.id(), sessionService.getActiveSession().orElseThrow().id());

        sessionService.setActiveSession(s1.id());

        assertEquals(s1.id(), sessionService.getActiveSession().orElseThrow().id());
    }

    @Test
    @DisplayName("SessionService updateSession updates stored session")
    void updateSession() {
        SessionService.Session original = sessionService.createSession("Original", "/project");
        SessionService.Session updated = original.updateTitle("Updated");

        sessionService.updateSession(updated);

        assertEquals("Updated", sessionService.getSession(original.id()).title());
    }

    @Test
    @DisplayName("SessionService listSessions returns all")
    void listSessions() {
        sessionService.createSession("S1", "/p1");
        sessionService.createSession("S2", "/p2");

        List<SessionService.Session> sessions = sessionService.listSessions();

        assertEquals(2, sessions.size());
    }

    @Test
    @DisplayName("SessionService listSessions by status filters")
    void listSessionsByStatus() {
        SessionService.Session s1 = sessionService.createSession("S1", "/p1");
        sessionService.createSession("S2", "/p2");
        sessionService.updateSession(s1.setStatus(SessionService.SessionStatus.ARCHIVED));

        List<SessionService.Session> archived = sessionService.listSessions(SessionService.SessionStatus.ARCHIVED);

        assertEquals(1, archived.size());
        assertEquals("S1", archived.get(0).title());
    }

    @Test
    @DisplayName("SessionService archiveSession sets status")
    void archiveSession() {
        SessionService.Session session = sessionService.createSession("Test", "/project");

        sessionService.archiveSession(session.id());

        assertEquals(SessionService.SessionStatus.ARCHIVED, sessionService.getSession(session.id()).status());
    }

    @Test
    @DisplayName("SessionService deleteSession removes session")
    void deleteSession() {
        SessionService.Session session = sessionService.createSession("Test", "/project");

        sessionService.deleteSession(session.id());

        assertNull(sessionService.getSession(session.id()));
        assertTrue(sessionService.getActiveSession().isEmpty());
    }

    @Test
    @DisplayName("SessionService searchSessions finds matches")
    void searchSessions() {
        sessionService.createSession("Java Project", "/p1");
        sessionService.createSession("Python Project", "/p2");
        sessionService.createSession("Java Testing", "/p3");

        List<SessionService.Session> results = sessionService.searchSessions("Java");

        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("SessionService exportSession JSON format")
    void exportSessionJson() {
        SessionService.Session session = sessionService.createSession("Test", "/project");

        String exported = sessionService.exportSession(session.id(), SessionService.ExportFormat.JSON);

        assertTrue(exported.contains(session.id()));
        assertTrue(exported.startsWith("{"));
    }

    @Test
    @DisplayName("SessionService exportSession MARKDOWN format")
    void exportSessionMarkdown() {
        SessionService.Session session = sessionService.createSession("Test", "/project");
        SessionService.MessageEntry msg = new SessionService.MessageEntry(
            "msg-1", SessionService.MessageRole.USER, "Hello", List.of(), Instant.now(), Map.of()
        );
        sessionService.updateSession(session.addMessage(msg));

        String exported = sessionService.exportSession(session.id(), SessionService.ExportFormat.MARKDOWN);

        assertTrue(exported.contains("# Test"));
        assertTrue(exported.contains("USER:"));
        assertTrue(exported.contains("Hello"));
    }

    @Test
    @DisplayName("SessionService exportSession TEXT format")
    void exportSessionText() {
        SessionService.Session session = sessionService.createSession("Test", "/project");
        SessionService.MessageEntry msg = new SessionService.MessageEntry(
            "msg-1", SessionService.MessageRole.ASSISTANT, "Response", List.of(), Instant.now(), Map.of()
        );
        sessionService.updateSession(session.addMessage(msg));

        String exported = sessionService.exportSession(session.id(), SessionService.ExportFormat.TEXT);

        assertTrue(exported.contains("Session: Test"));
        assertTrue(exported.contains("ASSISTANT: Response"));
    }

    @Test
    @DisplayName("SessionService exportSession returns empty for missing")
    void exportSessionMissing() {
        String exported = sessionService.exportSession("non-existent", SessionService.ExportFormat.JSON);
        assertEquals("", exported);
    }
}