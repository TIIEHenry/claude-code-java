/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code error log sink utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Error log sink implementation for file-based error logging.
 */
public final class ErrorLogSink {
    private ErrorLogSink() {}

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_INSTANT;

    private static final Map<String, BufferedWriter> logWriters = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    /**
     * Get the path to the errors log file.
     */
    public static Path getErrorsPath() {
        String date = LocalDate.now().format(DATE_FORMAT);
        return CachePaths.errors().resolve(date + ".jsonl");
    }

    /**
     * Get the path to MCP logs for a server.
     */
    public static Path getMCPLogsPath(String serverName) {
        String date = LocalDate.now().format(DATE_FORMAT);
        return CachePaths.mcpLogs(serverName).resolve(date + ".jsonl");
    }

    /**
     * Initialize the error log sink.
     */
    public static void initializeErrorLogSink() {
        if (initialized) return;
        initialized = true;

        // Register cleanup hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (BufferedWriter writer : logWriters.values()) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            logWriters.clear();
        }));

        Debug.logForDebugging("Error log sink initialized");
    }

    /**
     * Log an error to the error log file.
     */
    public static void logError(Throwable error) {
        String userType = System.getenv("USER_TYPE");
        if (!"ant".equals(userType)) return;

        String errorStr = getStackTrace(error);
        String context = extractContext(error);

        Debug.logForDebugging(error.getClass().getSimpleName() + ": " + context + errorStr);

        appendToLog(getErrorsPath(), Map.of(
                "error", context + errorStr,
                "timestamp", Instant.now().toString(),
                "cwd", System.getProperty("user.dir"),
                "userType", userType,
                "sessionId", getSessionId()
        ));
    }

    /**
     * Log an MCP error.
     */
    public static void logMCPError(String serverName, Throwable error) {
        Debug.logForDebugging("MCP server \"" + serverName + "\" " + error);

        Path logFile = getMCPLogsPath(serverName);
        String errorStr = getStackTrace(error);

        appendToLog(logFile, Map.of(
                "error", errorStr,
                "timestamp", Instant.now().toString(),
                "sessionId", getSessionId(),
                "cwd", System.getProperty("user.dir")
        ));
    }

    /**
     * Log an MCP debug message.
     */
    public static void logMCPDebug(String serverName, String message) {
        Debug.logForDebugging("MCP server \"" + serverName + "\": " + message);

        Path logFile = getMCPLogsPath(serverName);

        appendToLog(logFile, Map.of(
                "debug", message,
                "timestamp", Instant.now().toString(),
                "sessionId", getSessionId(),
                "cwd", System.getProperty("user.dir")
        ));
    }

    /**
     * Append to log file.
     */
    private static void appendToLog(Path path, Map<String, Object> message) {
        try {
            BufferedWriter writer = getLogWriter(path);
            String json = toJson(message);
            writer.write(json);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Get or create a log writer.
     */
    private static BufferedWriter getLogWriter(Path path) throws IOException {
        return logWriters.computeIfAbsent(path.toString(), p -> {
            try {
                Files.createDirectories(path.getParent());
                return Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create log writer", e);
            }
        });
    }

    /**
     * Get stack trace as string.
     */
    private static String getStackTrace(Throwable error) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        error.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Extract context from error (for HTTP errors).
     */
    private static String extractContext(Throwable error) {
        // In real implementation, would extract from HTTP client errors
        return "";
    }

    /**
     * Get session ID.
     */
    private static String getSessionId() {
        String sessionId = System.getenv("CLAUDE_CODE_SESSION_ID");
        return sessionId != null ? sessionId : UUID.randomUUID().toString();
    }

    /**
     * Convert map to JSON string (simple implementation).
     */
    private static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else {
                sb.append(value);
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Escape JSON string.
     */
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Flush all log writers (for testing).
     */
    public static void flushLogWriters() {
        for (BufferedWriter writer : logWriters.values()) {
            try {
                writer.flush();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * Clear all log writers (for testing).
     */
    public static void clearLogWriters() {
        for (BufferedWriter writer : logWriters.values()) {
            try {
                writer.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        logWriters.clear();
    }
}