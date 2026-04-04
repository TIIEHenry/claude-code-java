/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/sessions
 */
package com.anthropic.claudecode.services.sessions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.time.*;
import java.nio.file.*;

/**
 * Session service - Manage conversation sessions.
 */
public final class SessionService {
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private volatile String activeSessionId = null;
    private final SessionPersistence persistence = new SessionPersistence();

    /**
     * Session record.
     */
    public record Session(
        String id,
        String title,
        SessionType type,
        List<MessageEntry> messages,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt,
        SessionStatus status,
        String projectPath
    ) {
        public static Session create(String id, String title, String projectPath) {
            return new Session(
                id,
                title,
                SessionType.CONVERSATION,
                new ArrayList<>(),
                new HashMap<>(),
                Instant.now(),
                Instant.now(),
                SessionStatus.ACTIVE,
                projectPath
            );
        }

        public Session addMessage(MessageEntry message) {
            List<MessageEntry> newMessages = new ArrayList<>(messages);
            newMessages.add(message);
            return new Session(
                id, title, type, newMessages, metadata,
                createdAt, Instant.now(), status, projectPath
            );
        }

        public Session updateTitle(String newTitle) {
            return new Session(
                id, newTitle, type, messages, metadata,
                createdAt, Instant.now(), status, projectPath
            );
        }

        public Session setStatus(SessionStatus newStatus) {
            return new Session(
                id, title, type, messages, metadata,
                createdAt, Instant.now(), newStatus, projectPath
            );
        }

        public int getMessageCount() {
            return messages.size();
        }

        public Optional<MessageEntry> getLastMessage() {
            if (messages.isEmpty()) return Optional.empty();
            return Optional.of(messages.get(messages.size() - 1));
        }
    }

    /**
     * Session type enum.
     */
    public enum SessionType {
        CONVERSATION,
        TASK,
        AGENT,
        WORKFLOW,
        MCP_SESSION
    }

    /**
     * Session status enum.
     */
    public enum SessionStatus {
        ACTIVE,
        PAUSED,
        COMPLETED,
        ERROR,
        ARCHIVED
    }

    /**
     * Message entry record.
     */
    public record MessageEntry(
        String id,
        MessageRole role,
        String content,
        List<AttachmentInfo> attachments,
        Instant timestamp,
        Map<String, Object> metadata
    ) {}

    /**
     * Message role enum.
     */
    public enum MessageRole {
        USER,
        ASSISTANT,
        SYSTEM,
        TOOL
    }

    /**
     * Attachment info record.
     */
    public record AttachmentInfo(
        String id,
        String name,
        String type,
        String path,
        long size
    ) {}

    /**
     * Create new session.
     */
    public Session createSession(String title, String projectPath) {
        String id = UUID.randomUUID().toString();
        Session session = Session.create(id, title, projectPath);
        sessions.put(id, session);
        activeSessionId = id;
        return session;
    }

    /**
     * Get session by ID.
     */
    public Session getSession(String id) {
        return sessions.get(id);
    }

    /**
     * Get active session.
     */
    public Optional<Session> getActiveSession() {
        if (activeSessionId == null) return Optional.empty();
        return Optional.ofNullable(sessions.get(activeSessionId));
    }

    /**
     * Set active session.
     */
    public void setActiveSession(String id) {
        if (sessions.containsKey(id)) {
            activeSessionId = id;
        }
    }

    /**
     * Update session.
     */
    public Session updateSession(Session session) {
        sessions.put(session.id(), session);
        return session;
    }

    /**
     * Add message to active session.
     */
    public void addMessage(MessageEntry message) {
        getActiveSession().ifPresent(session -> {
            Session updated = session.addMessage(message);
            sessions.put(updated.id(), updated);
            persistence.saveAsync(updated);
        });
    }

    /**
     * List all sessions.
     */
    public List<Session> listSessions() {
        return new ArrayList<>(sessions.values());
    }

    /**
     * List sessions by status.
     */
    public List<Session> listSessions(SessionStatus status) {
        return sessions.values()
            .stream()
            .filter(s -> s.status() == status)
            .toList();
    }

    /**
     * Archive session.
     */
    public void archiveSession(String id) {
        Session session = sessions.get(id);
        if (session != null) {
            sessions.put(id, session.setStatus(SessionStatus.ARCHIVED));
            persistence.archiveAsync(session);
        }
    }

    /**
     * Delete session.
     */
    public void deleteSession(String id) {
        sessions.remove(id);
        persistence.deleteAsync(id);
        if (activeSessionId != null && activeSessionId.equals(id)) {
            activeSessionId = null;
        }
    }

    /**
     * Search sessions.
     */
    public List<Session> searchSessions(String query) {
        return sessions.values()
            .stream()
            .filter(s -> s.title().contains(query))
            .toList();
    }

    /**
     * Export session.
     */
    public String exportSession(String id, ExportFormat format) {
        Session session = sessions.get(id);
        if (session == null) return "";

        return switch (format) {
            case JSON -> exportJson(session);
            case MARKDOWN -> exportMarkdown(session);
            case TEXT -> exportText(session);
        };
    }

    private static String exportJson(Session session) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"id\": \"").append(escapeJson(session.id())).append("\",\n");
        sb.append("  \"title\": \"").append(escapeJson(session.title())).append("\",\n");
        sb.append("  \"type\": \"").append(session.type()).append("\",\n");
        sb.append("  \"status\": \"").append(session.status()).append("\",\n");
        sb.append("  \"projectPath\": \"").append(escapeJson(session.projectPath() != null ? session.projectPath() : "")).append("\",\n");
        sb.append("  \"createdAt\": \"").append(session.createdAt()).append("\",\n");
        sb.append("  \"updatedAt\": \"").append(session.updatedAt()).append("\",\n");
        sb.append("  \"messages\": [\n");

        for (int i = 0; i < session.messages().size(); i++) {
            MessageEntry msg = session.messages().get(i);
            sb.append("    {\n");
            sb.append("      \"id\": \"").append(escapeJson(msg.id())).append("\",\n");
            sb.append("      \"role\": \"").append(msg.role()).append("\",\n");
            sb.append("      \"content\": \"").append(escapeJson(msg.content())).append("\",\n");
            sb.append("      \"timestamp\": \"").append(msg.timestamp()).append("\"\n");
            sb.append("    }");
            if (i < session.messages().size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Escape JSON string.
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String exportMarkdown(Session session) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(session.title()).append("\n\n");
        for (MessageEntry msg : session.messages()) {
            sb.append("**").append(msg.role()).append(":**\n");
            sb.append(msg.content()).append("\n\n");
        }
        return sb.toString();
    }

    private String exportText(Session session) {
        StringBuilder sb = new StringBuilder();
        sb.append("Session: ").append(session.title()).append("\n\n");
        for (MessageEntry msg : session.messages()) {
            sb.append(msg.role()).append(": ").append(msg.content()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Export format enum.
     */
    public enum ExportFormat {
        JSON,
        MARKDOWN,
        TEXT
    }

    /**
     * Session persistence helper.
     */
    private static class SessionPersistence {
        private final Path sessionsDir;

        SessionPersistence() {
            String home = System.getProperty("user.home");
            this.sessionsDir = Paths.get(home, ".claude", "sessions");
        }

        void saveAsync(Session session) {
            CompletableFuture.runAsync(() -> {
                try {
                    Files.createDirectories(sessionsDir);
                    Path sessionFile = sessionsDir.resolve(session.id() + ".json");
                    Files.writeString(sessionFile, exportJson(session));
                } catch (Exception e) {
                    // Ignore save errors
                }
            });
        }

        Session load(String id) {
            try {
                Path sessionFile = sessionsDir.resolve(id + ".json");
                if (!Files.exists(sessionFile)) {
                    return null;
                }

                String content = Files.readString(sessionFile);
                return parseSessionJson(content);
            } catch (Exception e) {
                return null;
            }
        }

        List<Session> listAll() {
            List<Session> result = new ArrayList<>();
            try {
                if (!Files.exists(sessionsDir)) {
                    return result;
                }

                Files.list(sessionsDir)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            String content = Files.readString(p);
                            Session session = parseSessionJson(content);
                            if (session != null) {
                                result.add(session);
                            }
                        } catch (Exception e) {
                            // Skip invalid files
                        }
                    });

                // Sort by updatedAt descending
                result.sort((a, b) -> b.updatedAt().compareTo(a.updatedAt()));
            } catch (Exception e) {
                // Ignore list errors
            }
            return result;
        }

        List<SessionMeta> listMeta() {
            List<SessionMeta> result = new ArrayList<>();
            try {
                if (!Files.exists(sessionsDir)) {
                    return result;
                }

                Files.list(sessionsDir)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            String content = Files.readString(p);
                            SessionMeta meta = parseSessionMeta(content);
                            if (meta != null) {
                                result.add(meta);
                            }
                        } catch (Exception e) {
                            // Skip invalid files
                        }
                    });

                result.sort((a, b) -> b.updatedAt().compareTo(a.updatedAt()));
            } catch (Exception e) {
                // Ignore list errors
            }
            return result;
        }

        private Session parseSessionJson(String json) {
            try {
                Map<String, Object> map = parseJsonObject(json);

                String id = (String) map.get("id");
                String title = (String) map.get("title");
                String statusStr = (String) map.get("status");
                String projectPath = (String) map.get("projectPath");
                String createdAtStr = (String) map.get("createdAt");
                String updatedAtStr = (String) map.get("updatedAt");

                if (id == null) return null;

                List<MessageEntry> messages = new ArrayList<>();
                Object messagesObj = map.get("messages");
                if (messagesObj instanceof List) {
                    for (Object m : (List<?>) messagesObj) {
                        if (m instanceof Map) {
                            Map<?, ?> msgMap = (Map<?, ?>) m;
                            MessageEntry msg = new MessageEntry(
                                (String) msgMap.get("id"),
                                MessageRole.valueOf((String) msgMap.get("role")),
                                (String) msgMap.get("content"),
                                new ArrayList<>(),
                                Instant.parse((String) msgMap.get("timestamp")),
                                new HashMap<>()
                            );
                            messages.add(msg);
                        }
                    }
                }

                return new Session(
                    id,
                    title != null ? title : "Untitled",
                    SessionType.CONVERSATION,
                    messages,
                    new HashMap<>(),
                    createdAtStr != null ? Instant.parse(createdAtStr) : Instant.now(),
                    updatedAtStr != null ? Instant.parse(updatedAtStr) : Instant.now(),
                    statusStr != null ? SessionStatus.valueOf(statusStr) : SessionStatus.ACTIVE,
                    projectPath
                );
            } catch (Exception e) {
                return null;
            }
        }

        private SessionMeta parseSessionMeta(String json) {
            try {
                Map<String, Object> map = parseJsonObject(json);

                String id = (String) map.get("id");
                String title = (String) map.get("title");
                String projectPath = (String) map.get("projectPath");
                String createdAtStr = (String) map.get("createdAt");
                String updatedAtStr = (String) map.get("updatedAt");
                Integer messageCount = null;
                Object messagesObj = map.get("messages");
                if (messagesObj instanceof List) {
                    messageCount = ((List<?>) messagesObj).size();
                }

                if (id == null) return null;

                return new SessionMeta(
                    id,
                    title != null ? title : "Untitled",
                    projectPath,
                    messageCount != null ? messageCount : 0,
                    createdAtStr != null ? Instant.parse(createdAtStr) : Instant.now(),
                    updatedAtStr != null ? Instant.parse(updatedAtStr) : Instant.now()
                );
            } catch (Exception e) {
                return null;
            }
        }

        private Map<String, Object> parseJsonObject(String json) {
            Map<String, Object> result = new HashMap<>();
            if (json == null || json.isEmpty()) return result;

            json = json.trim();
            if (!json.startsWith("{") || !json.endsWith("}")) return result;

            json = json.substring(1, json.length() - 1).trim();
            if (json.isEmpty()) return result;

            int depth = 0;
            StringBuilder current = new StringBuilder();
            String currentKey = null;
            boolean inString = false;

            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);

                if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                    inString = !inString;
                    current.append(c);
                } else if (!inString) {
                    if (c == '{' || c == '[') {
                        depth++;
                        current.append(c);
                    } else if (c == '}' || c == ']') {
                        depth--;
                        current.append(c);
                    } else if (depth == 0 && c == ':') {
                        currentKey = current.toString().trim();
                        if (currentKey.startsWith("\"") && currentKey.endsWith("\"")) {
                            currentKey = currentKey.substring(1, currentKey.length() - 1);
                        }
                        current = new StringBuilder();
                    } else if (depth == 0 && c == ',') {
                        if (currentKey != null) {
                            result.put(currentKey, parseJsonValue(current.toString().trim()));
                        }
                        currentKey = null;
                        current = new StringBuilder();
                    } else {
                        current.append(c);
                    }
                } else {
                    current.append(c);
                }
            }

            if (currentKey != null) {
                result.put(currentKey, parseJsonValue(current.toString().trim()));
            }

            return result;
        }

        private Object parseJsonValue(String value) {
            if (value == null || value.isEmpty()) return null;
            value = value.trim();
            if (value.startsWith("\"") && value.endsWith("\"")) {
                return value.substring(1, value.length() - 1)
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
            }
            if (value.startsWith("{")) return parseJsonObject(value);
            if (value.startsWith("[")) return parseJsonArray(value);
            if ("true".equals(value)) return true;
            if ("false".equals(value)) return false;
            if ("null".equals(value)) return null;
            try {
                if (value.contains(".")) return Double.parseDouble(value);
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return value;
            }
        }

        private List<Object> parseJsonArray(String json) {
            List<Object> result = new ArrayList<>();
            if (json == null || json.isEmpty()) return result;

            json = json.trim();
            if (!json.startsWith("[") || !json.endsWith("]")) return result;

            json = json.substring(1, json.length() - 1).trim();
            if (json.isEmpty()) return result;

            int depth = 0;
            StringBuilder current = new StringBuilder();
            boolean inString = false;

            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);

                if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                    inString = !inString;
                    current.append(c);
                } else if (!inString) {
                    if (c == '{' || c == '[') {
                        depth++;
                        current.append(c);
                    } else if (c == '}' || c == ']') {
                        depth--;
                        current.append(c);
                    } else if (depth == 0 && c == ',') {
                        result.add(parseJsonValue(current.toString().trim()));
                        current = new StringBuilder();
                    } else {
                        current.append(c);
                    }
                } else {
                    current.append(c);
                }
            }

            if (current.length() > 0) {
                result.add(parseJsonValue(current.toString().trim()));
            }

            return result;
        }

        void archiveAsync(Session session) {
            CompletableFuture.runAsync(() -> {
                try {
                    Files.createDirectories(sessionsDir.resolve("archived"));

                    Path sessionFile = sessionsDir.resolve(session.id() + ".json");
                    Path archivedFile = sessionsDir.resolve("archived").resolve(session.id() + ".json");

                    if (Files.exists(sessionFile)) {
                        Files.move(sessionFile, archivedFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception e) {
                    // Ignore archive errors
                }
            });
        }

        void deleteAsync(String id) {
            CompletableFuture.runAsync(() -> {
                try {
                    Path sessionFile = sessionsDir.resolve(id + ".json");
                    Files.deleteIfExists(sessionFile);

                    // Also check archived
                    Path archivedFile = sessionsDir.resolve("archived").resolve(id + ".json");
                    Files.deleteIfExists(archivedFile);
                } catch (Exception e) {
                    // Ignore delete errors
                }
            });
        }

        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }

    /**
     * Session metadata for listing.
     */
    public record SessionMeta(
        String id,
        String title,
        String projectPath,
        int messageCount,
        Instant createdAt,
        Instant updatedAt
    ) {}

    /**
     * Load session from disk.
     */
    public Session loadSession(String id) {
        Session session = persistence.load(id);
        if (session != null) {
            sessions.put(id, session);
        }
        return session;
    }

    /**
     * List all saved sessions (metadata only).
     */
    public List<SessionMeta> listSavedSessions() {
        return persistence.listMeta();
    }

    /**
     * Load all saved sessions into memory.
     */
    public void loadAllSessions() {
        for (Session session : persistence.listAll()) {
            sessions.put(session.id(), session);
        }
    }
}