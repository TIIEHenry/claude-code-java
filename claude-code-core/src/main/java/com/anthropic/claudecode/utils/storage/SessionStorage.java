/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/sessionStorage.ts
 */
package com.anthropic.claudecode.utils.storage;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Session storage utilities.
 *
 * Manages session data persistence and retrieval.
 */
public final class SessionStorage {
    private SessionStorage() {}

    private static volatile Path sessionsDirectory = null;

    /**
     * Get the sessions directory path.
     */
    public static Path getSessionsDirectory() {
        if (sessionsDirectory == null) {
            synchronized (SessionStorage.class) {
                if (sessionsDirectory == null) {
                    String homeDir = System.getProperty("user.home");
                    String configDir = System.getenv("CLAUDE_CONFIG_DIR");

                    if (configDir != null && !configDir.isEmpty()) {
                        sessionsDirectory = Paths.get(configDir, "sessions");
                    } else {
                        sessionsDirectory = Paths.get(homeDir, ".claude", "sessions");
                    }

                    // Ensure directory exists
                    try {
                        Files.createDirectories(sessionsDirectory);
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }
        return sessionsDirectory;
    }

    /**
     * Set sessions directory override.
     */
    public static void setSessionsDirectory(Path directory) {
        sessionsDirectory = directory;
    }

    /**
     * Get transcript path for a session.
     */
    public static Path getTranscriptPath(String sessionId) {
        return getSessionsDirectory().resolve(sessionId).resolve("transcript.jsonl");
    }

    /**
     * Get session directory for a session ID.
     */
    public static Path getSessionDirectory(String sessionId) {
        return getSessionsDirectory().resolve(sessionId);
    }

    /**
     * Check if a session exists.
     */
    public static boolean sessionExists(String sessionId) {
        return Files.isDirectory(getSessionDirectory(sessionId));
    }

    /**
     * Create a new session directory.
     */
    public static String createSession() {
        String sessionId = generateSessionId();
        Path sessionDir = getSessionDirectory(sessionId);

        try {
            Files.createDirectories(sessionDir);
            // Write metadata
            Path metadataPath = sessionDir.resolve("metadata.json");
            String metadata = String.format(
                    "{\"sessionId\":\"%s\",\"createdAt\":\"%s\"}",
                    sessionId,
                    Instant.now().toString()
            );
            Files.writeString(metadataPath, metadata);
        } catch (IOException e) {
            // Ignore
        }

        return sessionId;
    }

    /**
     * Generate a unique session ID.
     */
    private static String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Append a message to the transcript.
     */
    public static void appendToTranscript(String sessionId, String message) {
        Path transcriptPath = getTranscriptPath(sessionId);
        try {
            // Ensure parent directory exists
            Files.createDirectories(transcriptPath.getParent());

            // Append line
            String line = message + "\n";
            Files.writeString(transcriptPath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Read the full transcript.
     */
    public static List<String> readTranscript(String sessionId) {
        Path transcriptPath = getTranscriptPath(sessionId);
        if (!Files.exists(transcriptPath)) {
            return List.of();
        }

        try {
            return Files.readAllLines(transcriptPath);
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * Get the last N lines from the transcript.
     */
    public static List<String> getTranscriptTail(String sessionId, int lines) {
        List<String> allLines = readTranscript(sessionId);
        if (allLines.size() <= lines) {
            return allLines;
        }
        return allLines.subList(allLines.size() - lines, allLines.size());
    }

    /**
     * Delete a session.
     */
    public static boolean deleteSession(String sessionId) {
        Path sessionDir = getSessionDirectory(sessionId);
        try {
            Files.walk(sessionDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * List all sessions.
     */
    public static List<String> listSessions() {
        Path sessionsDir = getSessionsDirectory();
        if (!Files.isDirectory(sessionsDir)) {
            return List.of();
        }

        List<String> sessions = new ArrayList<>();
        try (var stream = Files.list(sessionsDir)) {
            stream.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .forEach(sessions::add);
        } catch (IOException e) {
            // Ignore
        }

        return sessions;
    }

    /**
     * Get session metadata.
     */
    public static SessionMetadata getMetadata(String sessionId) {
        Path metadataPath = getSessionDirectory(sessionId).resolve("metadata.json");
        if (!Files.exists(metadataPath)) {
            return new SessionMetadata(sessionId, null, null);
        }

        try {
            String content = Files.readString(metadataPath);
            // Simple parsing - in production would use Jackson/Gson
            String createdAt = extractField(content, "createdAt");
            String name = extractField(content, "name");
            return new SessionMetadata(sessionId, createdAt, name);
        } catch (IOException e) {
            return new SessionMetadata(sessionId, null, null);
        }
    }

    /**
     * Update session metadata.
     */
    public static void updateMetadata(String sessionId, String name) {
        Path metadataPath = getSessionDirectory(sessionId).resolve("metadata.json");
        try {
            SessionMetadata existing = getMetadata(sessionId);
            String metadata = String.format(
                    "{\"sessionId\":\"%s\",\"createdAt\":\"%s\",\"name\":\"%s\"}",
                    sessionId,
                    existing.createdAt() != null ? existing.createdAt() : Instant.now().toString(),
                    name != null ? name : ""
            );
            Files.writeString(metadataPath, metadata);
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Extract a field from simple JSON.
     */
    private static String extractField(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern);
        if (start < 0) return null;
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    /**
     * Session metadata record.
     */
    public record SessionMetadata(
            String sessionId,
            String createdAt,
            String name
    ) {}
}