/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/logging/logFormatter
 */
package com.anthropic.claudecode.services.logging;

import java.util.*;
import java.time.*;

/**
 * Log formatter - Format log entries.
 */
public final class LogFormatter {
    private final FormatStyle style;
    private final boolean includeTimestamp;
    private final boolean includeThread;
    private final boolean includeContext;

    /**
     * Format style enum.
     */
    public enum FormatStyle {
        SIMPLE,
        DETAILED,
        JSON,
        ANSI
    }

    /**
     * Create log formatter.
     */
    public LogFormatter(FormatStyle style) {
        this.style = style;
        this.includeTimestamp = true;
        this.includeThread = false;
        this.includeContext = true;
    }

    /**
     * Create simple formatter.
     */
    public static LogFormatter simple() {
        return new LogFormatter(FormatStyle.SIMPLE);
    }

    /**
     * Create detailed formatter.
     */
    public static LogFormatter detailed() {
        return new LogFormatter(FormatStyle.DETAILED);
    }

    /**
     * Create JSON formatter.
     */
    public static LogFormatter json() {
        return new LogFormatter(FormatStyle.JSON);
    }

    /**
     * Create ANSI formatter.
     */
    public static LogFormatter ansi() {
        return new LogFormatter(FormatStyle.ANSI);
    }

    /**
     * Format log entry.
     */
    public String format(LoggingService.LogEntry entry) {
        return switch (style) {
            case SIMPLE -> formatSimple(entry);
            case DETAILED -> formatDetailed(entry);
            case JSON -> formatJson(entry);
            case ANSI -> formatAnsi(entry);
        };
    }

    /**
     * Format simple.
     */
    private String formatSimple(LoggingService.LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(entry.level()).append("] ");
        sb.append(entry.message());
        return sb.toString();
    }

    /**
     * Format detailed.
     */
    private String formatDetailed(LoggingService.LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        if (includeTimestamp) {
            sb.append(formatTimestamp(entry.timestamp())).append(" ");
        }
        sb.append("[").append(entry.level()).append("] ");
        sb.append("[").append(entry.category()).append("] ");
        if (includeThread) {
            sb.append("[").append(entry.threadName()).append("] ");
        }
        sb.append(entry.message());

        if (includeContext && entry.context() != null && !entry.context().isEmpty()) {
            sb.append(" | Context: ");
            entry.context().forEach((k, v) ->
                sb.append(k).append("=").append(v).append(" ")
            );
        }

        if (entry.exception() != null) {
            sb.append("\n  Exception: ").append(entry.exception().getClass().getName());
            sb.append(": ").append(entry.exception().getMessage());
        }

        return sb.toString();
    }

    /**
     * Format JSON.
     */
    private String formatJson(LoggingService.LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"timestamp\":\"").append(entry.timestamp()).append("\"");
        sb.append(",\"level\":\"").append(entry.level()).append("\"");
        sb.append(",\"category\":\"").append(entry.category()).append("\"");
        sb.append(",\"message\":\"").append(escapeJson(entry.message())).append("\"");

        if (includeThread) {
            sb.append(",\"thread\":\"").append(entry.threadName()).append("\"");
        }

        if (includeContext && entry.context() != null && !entry.context().isEmpty()) {
            sb.append(",\"context\":{");
            boolean first = true;
            for (Map.Entry<String, Object> e : entry.context().entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(e.getKey()).append("\":");
                sb.append("\"").append(e.getValue()).append("\"");
                first = false;
            }
            sb.append("}");
        }

        if (entry.exception() != null) {
            sb.append(",\"exception\":{");
            sb.append("\"type\":\"").append(entry.exception().getClass().getName()).append("\"");
            sb.append(",\"message\":\"").append(escapeJson(entry.exception().getMessage())).append("\"");
            sb.append("}");
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Format ANSI.
     */
    private String formatAnsi(LoggingService.LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        String levelColor = getLevelColor(entry.level());

        sb.append(levelColor);
        sb.append("[").append(entry.level()).append("]");
        sb.append("\033[0m"); // Reset

        sb.append(" ");
        sb.append(entry.message());

        if (entry.exception() != null) {
            sb.append("\n\033[31m"); // Red for exception
            sb.append("Exception: ").append(entry.exception().getMessage());
            sb.append("\033[0m");
        }

        return sb.toString();
    }

    /**
     * Get ANSI color for level.
     */
    private String getLevelColor(LoggingService.LogLevel level) {
        return switch (level) {
            case DEBUG -> "\033[36m";    // Cyan
            case INFO -> "\033[32m";     // Green
            case WARNING -> "\033[33m";  // Yellow
            case ERROR -> "\033[31m";    // Red
            case CRITICAL -> "\033[35m"; // Magenta
        };
    }

    /**
     * Format timestamp.
     */
    private String formatTimestamp(Instant timestamp) {
        return timestamp.toString();
    }

    /**
     * Escape JSON string.
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}