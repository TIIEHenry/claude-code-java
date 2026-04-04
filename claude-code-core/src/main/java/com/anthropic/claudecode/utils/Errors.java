/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/errors.ts
 */
package com.anthropic.claudecode.utils;

/**
 * Error types and utilities.
 */
public final class Errors {
    private Errors() {}

    /**
     * Base Claude error class.
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
     * Abort error.
     */
    public static class AbortError extends RuntimeException {
        public AbortError() {
            super("Aborted");
        }

        public AbortError(String message) {
            super(message);
        }
    }

    /**
     * Check if error is an abort error.
     */
    public static boolean isAbortError(Throwable e) {
        return e instanceof AbortError ||
               (e instanceof RuntimeException && "AbortError".equals(e.getClass().getSimpleName())) ||
               (e instanceof Exception && "AbortError".equals(e.getClass().getName()));
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

        public String getFilePath() { return filePath; }
        public Object getDefaultConfig() { return defaultConfig; }
    }

    /**
     * Shell error.
     */
    public static class ShellError extends RuntimeException {
        private final String stdout;
        private final String stderr;
        private final int code;
        private final boolean interrupted;

        public ShellError(String stdout, String stderr, int code, boolean interrupted) {
            super("Shell command failed");
            this.stdout = stdout;
            this.stderr = stderr;
            this.code = code;
            this.interrupted = interrupted;
        }

        public String getStdout() { return stdout; }
        public String getStderr() { return stderr; }
        public int getCode() { return code; }
        public boolean isInterrupted() { return interrupted; }
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

        public String getFormattedMessage() { return formattedMessage; }
    }

    /**
     * Telemetry-safe error.
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

        public String getTelemetryMessage() { return telemetryMessage; }
    }

    /**
     * Check if error has exact message.
     */
    public static boolean hasExactErrorMessage(Throwable error, String message) {
        return error != null && error.getMessage() != null && error.getMessage().equals(message);
    }

    /**
     * Convert to error.
     */
    public static RuntimeException toError(Object e) {
        if (e instanceof RuntimeException re) return re;
        if (e instanceof Exception ex) return new RuntimeException(ex);
        return new RuntimeException(String.valueOf(e));
    }

    /**
     * Extract error message.
     */
    public static String errorMessage(Object e) {
        if (e instanceof Throwable t) {
            return t.getMessage() != null ? t.getMessage() : t.toString();
        }
        return String.valueOf(e);
    }

    /**
     * Get errno code from error.
     */
    public static String getErrnoCode(Throwable e) {
        // Java doesn't have errno codes directly, but we can check for common patterns
        if (e instanceof java.nio.file.NoSuchFileException) return "ENOENT";
        if (e instanceof java.nio.file.AccessDeniedException) return "EACCES";
        if (e instanceof java.nio.file.FileSystemException) {
            String msg = e.getMessage();
            if (msg != null) {
                if (msg.contains("Not a directory")) return "ENOTDIR";
                if (msg.contains("Too many levels of symbolic links")) return "ELOOP";
            }
        }
        return null;
    }

    /**
     * Check if error is ENOENT.
     */
    public static boolean isENOENT(Throwable e) {
        return "ENOENT".equals(getErrnoCode(e));
    }

    /**
     * Get errno path from error.
     */
    public static String getErrnoPath(Throwable e) {
        if (e instanceof java.nio.file.FileSystemException fse) {
            return fse.getFile();
        }
        return null;
    }

    /**
     * Short error stack trace.
     */
    public static String shortErrorStack(Throwable e, int maxFrames) {
        if (e == null) return "null";

        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName());
        if (e.getMessage() != null) {
            sb.append(": ").append(e.getMessage());
        }

        StackTraceElement[] stack = e.getStackTrace();
        int count = 0;
        for (StackTraceElement frame : stack) {
            if (count >= maxFrames) break;
            sb.append("\n\tat ").append(frame.toString());
            count++;
        }

        if (stack.length > maxFrames) {
            sb.append("\n\t... ").append(stack.length - maxFrames).append(" more");
        }

        return sb.toString();
    }

    /**
     * Short error stack trace with default 5 frames.
     */
    public static String shortErrorStack(Throwable e) {
        return shortErrorStack(e, 5);
    }

    /**
     * Check if filesystem is inaccessible.
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
     * Axios error kind.
     */
    public enum AxiosErrorKind {
        AUTH, TIMEOUT, NETWORK, HTTP, OTHER
    }

    /**
     * Axios error classification.
     */
    public record AxiosErrorClassification(
        AxiosErrorKind kind,
        Integer status,
        String message
    ) {}

    /**
     * Classify an HTTP error.
     */
    public static AxiosErrorClassification classifyHttpError(Throwable e) {
        String message = errorMessage(e);

        // Check for common HTTP error patterns
        if (e instanceof java.net.SocketTimeoutException) {
            return new AxiosErrorClassification(AxiosErrorKind.TIMEOUT, null, message);
        }
        if (e instanceof java.net.ConnectException) {
            return new AxiosErrorClassification(AxiosErrorKind.NETWORK, null, message);
        }
        if (e instanceof java.net.UnknownHostException) {
            return new AxiosErrorClassification(AxiosErrorKind.NETWORK, null, message);
        }

        // Check for status code in message
        if (message.contains("401") || message.contains("403")) {
            int status = message.contains("401") ? 401 : 403;
            return new AxiosErrorClassification(AxiosErrorKind.AUTH, status, message);
        }

        return new AxiosErrorClassification(AxiosErrorKind.OTHER, null, message);
    }
}