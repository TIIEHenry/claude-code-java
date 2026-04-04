/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code concurrent sessions utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.concurrent.*;

/**
 * Concurrent session tracking utilities.
 */
public final class ConcurrentSessions {
    private ConcurrentSessions() {}

    /**
     * Session kind enum.
     */
    public enum SessionKind {
        INTERACTIVE, BG, DAEMON, DAEMON_WORKER
    }

    /**
     * Session status enum.
     */
    public enum SessionStatus {
        BUSY, IDLE, WAITING
    }

    /**
     * Session info record.
     */
    public record SessionInfo(
            int pid,
            String sessionId,
            String cwd,
            long startedAt,
            SessionKind kind,
            String entrypoint,
            String name,
            String logPath,
            String agent,
            String bridgeSessionId,
            SessionStatus status,
            String waitingFor,
            Long updatedAt
    ) {}

    /**
     * Get the sessions directory.
     */
    public static Path getSessionsDir() {
        return Paths.get(EnvUtils.getClaudeConfigHomeDir()).resolve("sessions");
    }

    /**
     * Get session kind from environment.
     */
    public static SessionKind getEnvSessionKind() {
        String kind = System.getenv("CLAUDE_CODE_SESSION_KIND");
        if ("bg".equals(kind)) return SessionKind.BG;
        if ("daemon".equals(kind)) return SessionKind.DAEMON;
        if ("daemon-worker".equals(kind)) return SessionKind.DAEMON_WORKER;
        return SessionKind.INTERACTIVE;
    }

    /**
     * Check if this is a background session.
     */
    public static boolean isBgSession() {
        return getEnvSessionKind() == SessionKind.BG;
    }

    /**
     * Register this session.
     */
    public static boolean registerSession() {
        SessionKind kind = getEnvSessionKind();
        Path dir = getSessionsDir();
        Path pidFile = dir.resolve(ProcessHandle.current().pid() + ".json");

        try {
            Files.createDirectories(dir);
            Files.setPosixFilePermissions(dir, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE
            ));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("pid", ProcessHandle.current().pid());
            data.put("sessionId", UUID.randomUUID().toString());
            data.put("cwd", System.getProperty("user.dir"));
            data.put("startedAt", System.currentTimeMillis());
            data.put("kind", kind.name().toLowerCase());
            data.put("entrypoint", System.getenv("CLAUDE_CODE_ENTRYPOINT"));

            String messagingSocket = System.getenv("CLAUDE_CODE_MESSAGING_SOCKET");
            if (messagingSocket != null) {
                data.put("messagingSocketPath", messagingSocket);
            }

            String sessionName = System.getenv("CLAUDE_CODE_SESSION_NAME");
            if (sessionName != null) {
                data.put("name", sessionName);
            }

            String logPath = System.getenv("CLAUDE_CODE_SESSION_LOG");
            if (logPath != null) {
                data.put("logPath", logPath);
            }

            String agent = System.getenv("CLAUDE_CODE_AGENT");
            if (agent != null) {
                data.put("agent", agent);
            }

            Files.writeString(pidFile, toJson(data));
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Update session name.
     */
    public static boolean updateSessionName(String name) {
        if (name == null || name.isEmpty()) return false;
        return updatePidFile(Map.of("name", name));
    }

    /**
     * Update session activity.
     */
    public static boolean updateSessionActivity(SessionStatus status, String waitingFor) {
        Map<String, Object> patch = new LinkedHashMap<>();
        if (status != null) {
            patch.put("status", status.name().toLowerCase());
        }
        if (waitingFor != null) {
            patch.put("waitingFor", waitingFor);
        }
        patch.put("updatedAt", System.currentTimeMillis());
        return updatePidFile(patch);
    }

    private static boolean updatePidFile(Map<String, Object> patch) {
        Path pidFile = getSessionsDir().resolve(ProcessHandle.current().pid() + ".json");
        try {
            if (!Files.exists(pidFile)) return false;
            String content = Files.readString(pidFile);
            // Simple merge - in real implementation would use JSON library
            Map<String, Object> data = parseJson(content);
            data.putAll(patch);
            Files.writeString(pidFile, toJson(data));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Count concurrent sessions.
     */
    public static int countConcurrentSessions() {
        Path dir = getSessionsDir();
        if (!Files.exists(dir)) return 0;

        int count = 0;
        try (var stream = Files.list(dir)) {
            for (Path file : stream.toList()) {
                String name = file.getFileName().toString();
                if (!name.matches("^\\d+\\.json$")) continue;

                String pidStr = name.substring(0, name.length() - 5);
                try {
                    int pid = Integer.parseInt(pidStr);
                    if (ProcessHandle.current().pid() == pid) {
                        count++;
                    } else if (isProcessRunning(pid)) {
                        count++;
                    } else {
                        // Stale - delete
                        try {
                            Files.delete(file);
                        } catch (Exception ignored) {}
                    }
                } catch (NumberFormatException ignored) {}
            }
        } catch (Exception e) {
            return 0;
        }
        return count;
    }

    /**
     * Check if a process is running.
     */
    private static boolean isProcessRunning(int pid) {
        return ProcessHandle.of(pid).isPresent();
    }

    /**
     * Unregister this session.
     */
    public static void unregisterSession() {
        Path pidFile = getSessionsDir().resolve(ProcessHandle.current().pid() + ".json");
        try {
            Files.deleteIfExists(pidFile);
        } catch (Exception ignored) {}
    }

    // Simple JSON helpers - real implementation would use Jackson/Gson
    private static String toJson(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : data.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v instanceof String) {
                sb.append("\"").append(v).append("\"");
            } else if (v instanceof Number) {
                sb.append(v);
            } else {
                sb.append("\"").append(v).append("\"");
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJson(String json) {
        // Simplified - real implementation would use JSON parser
        Map<String, Object> result = new LinkedHashMap<>();
        // Just return empty map for now
        return result;
    }
}