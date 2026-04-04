/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/debug
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.time.*;
import java.util.function.*;

/**
 * Debug logging utility - Conditional logging for debugging.
 */
public final class Debug {
    private static final Map<String, Integer> LEVEL_ORDER = Map.of(
        "verbose", 0,
        "debug", 1,
        "info", 2,
        "warn", 3,
        "error", 4
    );

    private static volatile boolean runtimeDebugEnabled = false;
    private static volatile boolean hasFormattedOutput = false;

    private static BufferedWriter debugWriter;
    private static final List<String> pendingWrites = new CopyOnWriteArrayList<>();

    /**
     * Debug log level enum.
     */
    public enum Level {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    /**
     * Get minimum debug log level.
     */
    public static Level getMinDebugLogLevel() {
        String raw = System.getenv("CLAUDE_CODE_DEBUG_LOG_LEVEL");
        if (raw != null) {
            try {
                return Level.valueOf(raw.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                // Use default
            }
        }
        return Level.DEBUG;
    }

    /**
     * Check if debug mode is enabled.
     */
    public static boolean isDebugMode() {
        return runtimeDebugEnabled ||
               isEnvTruthy("DEBUG") ||
               isEnvTruthy("DEBUG_SDK") ||
               hasDebugArg() ||
               isDebugToStdErr() ||
               getDebugFilePath() != null;
    }

    /**
     * Enable debug logging mid-session.
     */
    public static boolean enableDebugLogging() {
        boolean wasActive = isDebugMode() || "ant".equals(System.getenv("USER_TYPE"));
        runtimeDebugEnabled = true;
        return wasActive;
    }

    /**
     * Check if debug output goes to stderr.
     */
    public static boolean isDebugToStdErr() {
        String[] args = getArgs();
        for (String arg : args) {
            if ("--debug-to-stderr".equals(arg) || "-d2e".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get debug file path.
     */
    public static String getDebugFilePath() {
        String[] args = getArgs();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--debug-file=")) {
                return args[i].substring("--debug-file=".length());
            }
            if ("--debug-file".equals(args[i]) && i + 1 < args.length) {
                return args[i + 1];
            }
        }
        return null;
    }

    /**
     * Log for debugging.
     */
    public static void log(String message) {
        logForDebugging(message, Level.DEBUG);
    }

    /**
     * Log for debugging with category.
     */
    public static void log(String category, String message) {
        logForDebugging("[ " + category + " ] " + message, Level.DEBUG);
    }

    /**
     * Log for debugging.
     */
    public static void logForDebugging(String message) {
        logForDebugging(message, Level.DEBUG);
    }

    /**
     * Log for debugging with level.
     */
    public static void logForDebugging(String message, Level level) {
        if (LEVEL_ORDER.get(level.name().toLowerCase()) < LEVEL_ORDER.get(getMinDebugLogLevel().name().toLowerCase())) {
            return;
        }

        if (!shouldLogDebugMessage(message)) {
            return;
        }

        // Handle multiline messages
        if (hasFormattedOutput && message.contains("\n")) {
            message = SlowOperations.jsonStringify(message);
        }

        String timestamp = Instant.now().toString();
        String output = String.format("%s [%s] %s%n", timestamp, level.name(), message.trim());

        if (isDebugToStdErr()) {
            System.err.print(output);
            return;
        }

        getDebugWriter().write(output);
    }

    /**
     * Log ant-only error.
     */
    public static void logAntError(String context, Throwable error) {
        if (!"ant".equals(System.getenv("USER_TYPE"))) {
            return;
        }

        String stackTrace = getStackTrace(error);
        if (stackTrace != null) {
            logForDebugging("[ANT-ONLY] " + context + " stack trace:\n" + stackTrace, Level.ERROR);
        }
    }

    /**
     * Flush debug logs.
     */
    public static CompletableFuture<Void> flushDebugLogs() {
        if (debugWriter != null) {
            debugWriter.flush();
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Get debug log path.
     */
    public static String getDebugLogPath() {
        String customPath = getDebugFilePath();
        if (customPath != null) {
            return customPath;
        }

        String logsDir = System.getenv("CLAUDE_CODE_DEBUG_LOGS_DIR");
        if (logsDir != null) {
            return logsDir;
        }

        String configHome = System.getProperty("user.home") + "/.claude";
        String sessionId = System.getProperty("claude.code.session.id", "default");
        return configHome + "/debug/" + sessionId + ".txt";
    }

    /**
     * Set has formatted output.
     */
    public static void setHasFormattedOutput(boolean value) {
        hasFormattedOutput = value;
    }

    /**
     * Get has formatted output.
     */
    public static boolean getHasFormattedOutput() {
        return hasFormattedOutput;
    }

    // Helper methods
    private static boolean shouldLogDebugMessage(String message) {
        if ("test".equals(System.getenv("NODE_ENV")) && !isDebugToStdErr()) {
            return false;
        }

        if (!"ant".equals(System.getenv("USER_TYPE")) && !isDebugMode()) {
            return false;
        }

        return true;
    }

    private static BufferedWriter getDebugWriter() {
        if (debugWriter == null) {
            debugWriter = new BufferedWriter(getDebugLogPath());
        }
        return debugWriter;
    }

    private static boolean hasDebugArg() {
        String[] args = getArgs();
        for (String arg : args) {
            if ("--debug".equals(arg) || "-d".equals(arg) || arg.startsWith("--debug=")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEnvTruthy(String name) {
        String value = System.getenv(name);
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private static String[] getArgs() {
        // In a real implementation, these would be the actual command line args
        return new String[0];
    }

    private static String getStackTrace(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Simple buffered writer for debug logs.
     */
    private static final class BufferedWriter {
        private final String path;
        private final List<String> buffer = new CopyOnWriteArrayList<>();
        private final ScheduledExecutorService scheduler;

        BufferedWriter(String path) {
            this.path = path;
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            this.scheduler.scheduleAtFixedRate(
                this::flush,
                1000, 1000, TimeUnit.MILLISECONDS
            );
        }

        void write(String content) {
            if (isDebugMode()) {
                // Immediate mode
                try {
                    Files.createDirectories(Paths.get(path).getParent());
                    Files.write(Paths.get(path), content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (Exception e) {
                    // Ignore
                }
            } else {
                buffer.add(content);
                if (buffer.size() >= 100) {
                    flush();
                }
            }
        }

        void flush() {
            if (buffer.isEmpty()) return;

            try {
                Files.createDirectories(Paths.get(path).getParent());
                StringBuilder sb = new StringBuilder();
                for (String line : buffer) {
                    sb.append(line);
                }
                Files.write(Paths.get(path), sb.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                buffer.clear();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}