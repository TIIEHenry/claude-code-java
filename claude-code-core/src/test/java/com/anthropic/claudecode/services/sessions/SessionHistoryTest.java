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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SessionHistory.
 */
class SessionHistoryTest {

    private SessionHistory sessionHistory;

    @BeforeEach
    void setUp() {
        sessionHistory = new SessionHistory();
    }

    @Test
    @DisplayName("SessionHistory HistoryEntry record")
    void historyEntryRecord() {
        Instant now = Instant.now();
        SessionHistory.HistoryEntry entry = new SessionHistory.HistoryEntry(
            "session-123",
            "Test Session",
            "/path/to/project",
            "Summary text",
            5,
            now,
            now,
            false
        );

        assertEquals("session-123", entry.sessionId());
        assertEquals("Test Session", entry.sessionTitle());
        assertEquals("/path/to/project", entry.projectPath());
        assertEquals("Summary text", entry.summary());
        assertEquals(5, entry.messageCount());
        assertEquals(now, entry.createdAt());
        assertEquals(now, entry.lastAccessed());
        assertFalse(entry.isFavorite());
    }

    @Test
    @DisplayName("SessionHistory HistoryEntry getDisplayText")
    void historyEntryGetDisplayText() {
        Instant now = Instant.now();
        SessionHistory.HistoryEntry entry = new SessionHistory.HistoryEntry(
            "session-123",
            "My Session",
            "/path",
            "summary",
            10,
            now,
            now,
            false
        );

        String display = entry.getDisplayText();

        assertTrue(display.contains("My Session"));
        assertTrue(display.contains("10"));
        assertTrue(display.contains("messages"));
    }

    @Test
    @DisplayName("SessionHistory addSession adds entry")
    void addSession() {
        SessionService.Session session = createTestSession("session-1", "Test Session");

        sessionHistory.addSession(session);

        List<SessionHistory.HistoryEntry> history = sessionHistory.getHistory();
        assertEquals(1, history.size());
        assertEquals("session-1", history.get(0).sessionId());
    }

    @Test
    @DisplayName("SessionHistory getHistory returns unmodifiable list")
    void getHistoryUnmodifiable() {
        SessionService.Session session = createTestSession("session-1", "Test");
        sessionHistory.addSession(session);

        List<SessionHistory.HistoryEntry> history = sessionHistory.getHistory();

        assertThrows(UnsupportedOperationException.class, () -> history.add(null));
    }

    @Test
    @DisplayName("SessionHistory getRecentHistory limits results")
    void getRecentHistory() {
        for (int i = 0; i < 10; i++) {
            sessionHistory.addSession(createTestSession("session-" + i, "Session " + i));
        }

        List<SessionHistory.HistoryEntry> recent = sessionHistory.getRecentHistory(5);

        assertEquals(5, recent.size());
        // Should be the last 5 entries
        assertEquals("session-5", recent.get(0).sessionId());
        assertEquals("session-9", recent.get(4).sessionId());
    }

    @Test
    @DisplayName("SessionHistory searchHistory finds matches")
    void searchHistory() {
        sessionHistory.addSession(createTestSession("s1", "Java Development"));
        sessionHistory.addSession(createTestSession("s2", "Python Scripts"));
        sessionHistory.addSession(createTestSession("s3", "Java Testing"));

        List<SessionHistory.HistoryEntry> results = sessionHistory.searchHistory("Java");

        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("SessionHistory searchHistory returns empty for no match")
    void searchHistoryNoMatch() {
        sessionHistory.addSession(createTestSession("s1", "Test Session"));

        List<SessionHistory.HistoryEntry> results = sessionHistory.searchHistory("nonexistent");

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("SessionHistory toggleFavorite toggles state")
    void toggleFavorite() {
        sessionHistory.addSession(createTestSession("session-1", "Test"));

        sessionHistory.toggleFavorite("session-1");
        List<SessionHistory.HistoryEntry> favorites = sessionHistory.getFavorites();
        assertEquals(1, favorites.size());

        sessionHistory.toggleFavorite("session-1");
        favorites = sessionHistory.getFavorites();
        assertTrue(favorites.isEmpty());
    }

    @Test
    @DisplayName("SessionHistory getFavorites returns only favorites")
    void getFavorites() {
        sessionHistory.addSession(createTestSession("s1", "Session 1"));
        sessionHistory.addSession(createTestSession("s2", "Session 2"));
        sessionHistory.addSession(createTestSession("s3", "Session 3"));

        sessionHistory.toggleFavorite("s1");
        sessionHistory.toggleFavorite("s3");

        List<SessionHistory.HistoryEntry> favorites = sessionHistory.getFavorites();

        assertEquals(2, favorites.size());
    }

    @Test
    @DisplayName("SessionHistory clearHistory removes all entries")
    void clearHistory() {
        sessionHistory.addSession(createTestSession("s1", "Session 1"));
        sessionHistory.addSession(createTestSession("s2", "Session 2"));

        sessionHistory.clearHistory();

        assertTrue(sessionHistory.getHistory().isEmpty());
    }

    @Test
    @DisplayName("SessionHistory getStats returns correct stats")
    void getStats() {
        sessionHistory.addSession(createTestSession("s1", "Session 1"));
        sessionHistory.addSession(createTestSession("s2", "Session 2"));
        sessionHistory.addSession(createTestSession("s3", "Session 3"));
        sessionHistory.toggleFavorite("s1");

        SessionHistory.HistoryStats stats = sessionHistory.getStats();

        assertEquals(3, stats.totalSessions());
        assertEquals(1, stats.favorites());
        assertNotNull(stats.oldestSession());
        assertNotNull(stats.newestSession());
    }

    @Test
    @DisplayName("SessionHistory getStats empty history")
    void getStatsEmpty() {
        SessionHistory.HistoryStats stats = sessionHistory.getStats();

        assertEquals(0, stats.totalSessions());
        assertEquals(0, stats.favorites());
        assertNull(stats.oldestSession());
        assertNull(stats.newestSession());
    }

    @Test
    @DisplayName("SessionHistory HistoryStats record")
    void historyStatsRecord() {
        Instant t1 = Instant.now().minusSeconds(3600);
        Instant t2 = Instant.now();

        SessionHistory.HistoryStats stats = new SessionHistory.HistoryStats(10, 2, t1, t2);

        assertEquals(10, stats.totalSessions());
        assertEquals(2, stats.favorites());
        assertEquals(t1, stats.oldestSession());
        assertEquals(t2, stats.newestSession());
    }

    // Helper method to create test sessions
    private SessionService.Session createTestSession(String id, String title) {
        return new SessionService.Session(
            id,
            title,
            SessionService.SessionType.CONVERSATION,
            List.of(new SessionService.MessageEntry(
                "msg-1",
                SessionService.MessageRole.USER,
                "Test message content",
                List.of(),
                Instant.now(),
                Map.of()
            )),
            Map.of(),
            Instant.now(),
            Instant.now(),
            SessionService.SessionStatus.ACTIVE,
            "/test/project"
        );
    }
}