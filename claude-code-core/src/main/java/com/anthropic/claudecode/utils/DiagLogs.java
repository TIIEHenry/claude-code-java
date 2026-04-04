/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/diagLogs
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.time.*;
import java.util.function.*;

/**
 * Diagnostic logging - Logs diagnostic information to a logfile.
 *
 * Important: This MUST NOT be called with any PII, including
 * file paths, project names, repo names, prompts, etc.
 */
public final class DiagLogs {
    /**
     * Diagnostic log level enum.
     */
    public enum Level {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    /**
     * Diagnostic log entry record.
     */
    public record DiagnosticLogEntry(
        String timestamp,
        Level level,
        String event,
        Map<String, Object> data
    ) {}

    /**
     * Log for diagnostics (no PII).
     */
    public static void logForDiagnosticsNoPII(Level level, String event) {
        logForDiagnosticsNoPII(level, event, Collections.emptyMap());
    }

    /**
     * Log for diagnostics with data (no PII).
     */
    public static void logForDiagnosticsNoPII(Level level, String event, Map<String, Object> data) {
        String logFile = getDiagnosticLogFile();
        if (logFile == null) {
            return;
        }

        DiagnosticLogEntry entry = new DiagnosticLogEntry(
            Instant.now().toString(),
            level,
            event,
            data != null ? data : Collections.emptyMap()
        );

        String line = SlowOperations.jsonStringify(entry) + "\n";

        try {
            Files.createDirectories(Paths.get(logFile).getParent());
            Files.write(Paths.get(logFile), line.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            // Silently fail
        }
    }

    /**
     * Wrap async function with diagnostic timing logs.
     * Logs {event}_started before execution and {event}_completed after with duration_ms.
     */
    public static <T> CompletableFuture<T> withDiagnosticsTiming(
        String event,
        Supplier<CompletableFuture<T>> fn
    ) {
        return withDiagnosticsTiming(event, fn, null);
    }

    /**
     * Wrap async function with diagnostic timing logs and data extractor.
     */
    public static <T> CompletableFuture<T> withDiagnosticsTiming(
        String event,
        Supplier<CompletableFuture<T>> fn,
        Function<T, Map<String, Object>> getData
    ) {
        long startTime = System.currentTimeMillis();
        logForDiagnosticsNoPII(Level.INFO, event + "_started");

        return fn.get()
            .thenApply(result -> {
                Map<String, Object> additionalData = getData != null ? getData.apply(result) : Collections.emptyMap();
                Map<String, Object> completedData = new HashMap<>(additionalData);
                completedData.put("duration_ms", System.currentTimeMillis() - startTime);
                logForDiagnosticsNoPII(Level.INFO, event + "_completed", completedData);
                return result;
            })
            .exceptionally(error -> {
                Map<String, Object> failedData = new HashMap<>();
                failedData.put("duration_ms", System.currentTimeMillis() - startTime);
                logForDiagnosticsNoPII(Level.ERROR, event + "_failed", failedData);
                throw new CompletionException(error);
            });
    }

    /**
     * Get diagnostic log file path.
     */
    private static String getDiagnosticLogFile() {
        return System.getenv("CLAUDE_CODE_DIAGNOSTICS_FILE");
    }
}