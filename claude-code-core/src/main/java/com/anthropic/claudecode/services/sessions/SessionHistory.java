/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/sessions/sessionHistory
 */
package com.anthropic.claudecode.services.sessions;

import java.util.*;
import java.time.*;

/**
 * Session history - Manage session history.
 */
public final class SessionHistory {
    private final List<HistoryEntry> history = new ArrayList<>();
    private final int maxEntries = 1000;

    /**
     * History entry record.
     */
    public record HistoryEntry(
        String sessionId,
        String sessionTitle,
        String projectPath,
        String summary,
        int messageCount,
        Instant createdAt,
        Instant lastAccessed,
        boolean isFavorite
    ) {
        public String getDisplayText() {
            return String.format("%s (%d messages) - %s",
                sessionTitle,
                messageCount,
                createdAt
            );
        }
    }

    /**
     * Add session to history.
     */
    public void addSession(SessionService.Session session) {
        HistoryEntry entry = new HistoryEntry(
            session.id(),
            session.title(),
            session.projectPath(),
            generateSummary(session),
            session.getMessageCount(),
            session.createdAt(),
            session.updatedAt(),
            false
        );

        history.add(entry);
        trimHistory();
    }

    /**
     * Generate summary.
     */
    private String generateSummary(SessionService.Session session) {
        if (session.messages().isEmpty()) {
            return "Empty session";
        }

        return session.messages()
            .stream()
            .filter(m -> m.role() == SessionService.MessageRole.USER)
            .findFirst()
            .map(m -> truncate(m.content(), 100))
            .orElse("No user messages");
    }

    /**
     * Truncate string.
     */
    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    /**
     * Trim history to max entries.
     */
    private void trimHistory() {
        while (history.size() > maxEntries) {
            history.remove(0);
        }
    }

    /**
     * Get all history.
     */
    public List<HistoryEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /**
     * Get recent history.
     */
    public List<HistoryEntry> getRecentHistory(int limit) {
        int start = Math.max(0, history.size() - limit);
        return history.subList(start, history.size());
    }

    /**
     * Search history.
     */
    public List<HistoryEntry> searchHistory(String query) {
        return history.stream()
            .filter(e -> e.sessionTitle().contains(query) ||
                        e.summary().contains(query) ||
                        e.projectPath().contains(query))
            .toList();
    }

    /**
     * Get favorites.
     */
    public List<HistoryEntry> getFavorites() {
        return history.stream()
            .filter(HistoryEntry::isFavorite)
            .toList();
    }

    /**
     * Toggle favorite.
     */
    public void toggleFavorite(String sessionId) {
        for (int i = 0; i < history.size(); i++) {
            HistoryEntry entry = history.get(i);
            if (entry.sessionId().equals(sessionId)) {
                history.set(i, new HistoryEntry(
                    entry.sessionId(),
                    entry.sessionTitle(),
                    entry.projectPath(),
                    entry.summary(),
                    entry.messageCount(),
                    entry.createdAt(),
                    entry.lastAccessed(),
                    !entry.isFavorite()
                ));
                break;
            }
        }
    }

    /**
     * Clear history.
     */
    public void clearHistory() {
        history.clear();
    }

    /**
     * Get history stats.
     */
    public HistoryStats getStats() {
        int favorites = (int) history.stream().filter(HistoryEntry::isFavorite).count();
        return new HistoryStats(
            history.size(),
            favorites,
            history.size() > 0 ? history.get(0).createdAt() : null,
            history.size() > 0 ? history.get(history.size() - 1).createdAt() : null
        );
    }

    /**
     * History stats record.
     */
    public record HistoryStats(
        int totalSessions,
        int favorites,
        Instant oldestSession,
        Instant newestSession
    ) {}
}