/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code error types
 */
package com.anthropic.claudecode.utils;

/**
 * Base Claude error class.
 */
public class ClaudeError extends RuntimeException {
    public ClaudeError(String message) {
        super(message);
    }

    public ClaudeError(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Malformed command error.
 */
class MalformedCommandError extends RuntimeException {
    public MalformedCommandError(String message) {
        super(message);
    }
}

/**
 * Abort error for cancelled operations.
 */
class AbortError extends RuntimeException {
    public AbortError() {
        super();
    }

    public AbortError(String message) {
        super(message);
    }
}

/**
 * Shell command error with output details.
 */
class ShellError extends RuntimeException {
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
 * Config parse error with file path and default config.
 */
class ConfigParseError extends RuntimeException {
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
 * Teleport operation error.
 */
class TeleportOperationError extends RuntimeException {
    private final String formattedMessage;

    public TeleportOperationError(String message, String formattedMessage) {
        super(message);
        this.formattedMessage = formattedMessage;
    }

    public String getFormattedMessage() { return formattedMessage; }
}

/**
 * Telemetry-safe error for logging.
 */
class TelemetrySafeError extends RuntimeException {
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