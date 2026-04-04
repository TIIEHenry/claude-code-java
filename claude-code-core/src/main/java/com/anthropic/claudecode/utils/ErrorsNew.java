/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code error utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Error handling utilities and custom error types.
 */
public final class ErrorsNew {
    private ErrorsNew() {}

    /**
     * Base class for Claude errors.
     */
    public static class ClaudeError extends RuntimeException {
        public ClaudeError(String message) {
            super(message);
        }
    }

    /**
     * Malformed command error.
     */
    public static class MalformedCommandError extends RuntimeException {
        public MalformedCommandError(String message) {
            super(message);
        }
    }

    /**
     * Abort error for cancelled operations.
     */
    public static class AbortError extends RuntimeException {
        public AbortError() {
            super("Operation aborted");
        }

        public AbortError(String message) {
            super(message);
        }
    }

    /**
     * Check if an exception is an abort error.
     */
    public static boolean isAbortError(Throwable e) {
        return e instanceof AbortError ||
               (e instanceof InterruptedException) ||
               "AbortError".equals(e.getClass().getSimpleName());
    }

    /**
     * Configuration parse error.
     */
    public static class ConfigParseError extends RuntimeException {
        private final String filePath;
        private final Object defaultConfig;

        public ConfigParseError(String message, String filePath, Object defaultConfig) {
            super(message);
            this.filePath = filePath;
            this.defaultConfig = defaultConfig;
        }

        public String getFilePath() {
            return filePath;
        }

        public Object getDefaultConfig() {
            return defaultConfig;
        }
    }

    /**
     * Shell command execution error.
     */
    public static class ShellError extends RuntimeException {
        private final String stdout;
        private final String stderr;
        private final int exitCode;
        private final boolean interrupted;

        public ShellError(String stdout, String stderr, int exitCode, boolean interrupted) {
            super("Shell command failed with exit code " + exitCode);
            this.stdout = stdout;
            this.stderr = stderr;
            this.exitCode = exitCode;
            this.interrupted = interrupted;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public int getExitCode() {
            return exitCode;
        }

        public boolean isInterrupted() {
            return interrupted;
        }
    }

    /**
     * Teleport operation error.
     */
    public static class TeleportOperationError extends RuntimeException {
        private final String formattedMessage;

        public TeleportOperationError(String message, String formattedMessage) {
            super(message);
            this.formattedMessage = formattedMessage;
        }

        public String getFormattedMessage() {
            return formattedMessage;
        }
    }

    /**
     * Telemetry-safe error with message verified to contain no sensitive data.
     */
    public static class TelemetrySafeError extends RuntimeException {
        private final String telemetryMessage;

        public TelemetrySafeError(String message) {
            super(message);
            this.telemetryMessage = message;
        }

        public TelemetrySafeError(String message, String telemetryMessage) {
            super(message);
            this.telemetryMessage = telemetryMessage;
        }

        public String getTelemetryMessage() {
            return telemetryMessage;
        }
    }

    /**
     * Check if error has exact message.
     */
    public static boolean hasExactErrorMessage(Throwable error, String message) {
        return error != null && error.getMessage() != null && error.getMessage().equals(message);
    }

    /**
     * Convert any throwable to an Error.
     */
    public static RuntimeException toError(Throwable e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(e);
    }

    /**
     * Extract message from any throwable.
     */
    public static String errorMessage(Throwable e) {
        return e != null && e.getMessage() != null ? e.getMessage() : String.valueOf(e);
    }

    /**
     * Extract errno-style code from exception.
     */
    public static String getErrnoCode(Throwable e) {
        if (e == null) return null;
        // Check for common filesystem error codes
        String message = e.getMessage();
        if (message == null) return null;

        // Common errno patterns
        if (message.contains("ENOENT")) return "ENOENT";
        if (message.contains("EACCES")) return "EACCES";
        if (message.contains("EPERM")) return "EPERM";
        if (message.contains("ENOTDIR")) return "ENOTDIR";
        if (message.contains("ELOOP")) return "ELOOP";
        if (message.contains("EEXIST")) return "EEXIST";

        return null;
    }

    /**
     * Check if error is ENOENT (file not found).
     */
    public static boolean isENOENT(Throwable e) {
        return "ENOENT".equals(getErrnoCode(e));
    }

    /**
     * Extract path from errno exception.
     */
    public static String getErrnoPath(Throwable e) {
        if (e == null) return null;
        String message = e.getMessage();
        if (message == null) return null;

        // Try to extract path from common error message patterns
        // "File not found: /path/to/file" or "No such file or directory: /path"
        int lastColon = message.lastIndexOf(':');
        if (lastColon >= 0 && lastColon < message.length() - 1) {
            String path = message.substring(lastColon + 1).trim();
            if (!path.isEmpty()) {
                return path;
            }
        }
        return null;
    }

    /**
     * Get shortened error stack trace.
     */
    public static String shortErrorStack(Throwable e, int maxFrames) {
        if (e == null) return "null";

        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName());
        if (e.getMessage() != null) {
            sb.append(": ").append(e.getMessage());
        }

        StackTraceElement[] stack = e.getStackTrace();
        int frames = Math.min(maxFrames, stack.length);
        for (int i = 0; i < frames; i++) {
            sb.append("\n\tat ").append(stack[i]);
        }

        if (stack.length > maxFrames) {
            sb.append("\n\t... ").append(stack.length - maxFrames).append(" more");
        }

        return sb.toString();
    }

    /**
     * Check if error indicates filesystem inaccessibility.
     */
    public static boolean isFsInaccessible(Throwable e) {
        String code = getErrnoCode(e);
        return "ENOENT".equals(code) ||
               "EACCES".equals(code) ||
               "EPERM".equals(code) ||
               "ENOTDIR".equals(code) ||
               "ELOOP".equals(code);
    }

    /**
     * HTTP error classification.
     */
    public enum HttpErrorKind {
        AUTH, TIMEOUT, NETWORK, HTTP, OTHER
    }

    /**
     * Classify HTTP error.
     */
    public static HttpErrorClassification classifyHttpError(Throwable e) {
        if (e == null) {
            return new HttpErrorClassification(HttpErrorKind.OTHER, null, "null");
        }

        String message = errorMessage(e);
        Integer status = null;

        // Try to extract status code
        if (message.contains("401")) status = 401;
        else if (message.contains("403")) status = 403;
        else if (message.contains("404")) status = 404;
        else if (message.contains("500")) status = 500;
        else if (message.contains("502")) status = 502;
        else if (message.contains("503")) status = 503;

        // Classify by status or message
        if (status != null && (status == 401 || status == 403)) {
            return new HttpErrorClassification(HttpErrorKind.AUTH, status, message);
        }
        if (message.contains("timeout") || message.contains("Timeout") || message.contains("ECONNABORTED")) {
            return new HttpErrorClassification(HttpErrorKind.TIMEOUT, status, message);
        }
        if (message.contains("ECONNREFUSED") || message.contains("ENOTFOUND") || message.contains("network")) {
            return new HttpErrorClassification(HttpErrorKind.NETWORK, status, message);
        }
        if (status != null) {
            return new HttpErrorClassification(HttpErrorKind.HTTP, status, message);
        }

        return new HttpErrorClassification(HttpErrorKind.OTHER, null, message);
    }

    /**
     * HTTP error classification result.
     */
    public record HttpErrorClassification(HttpErrorKind kind, Integer status, String message) {}
}