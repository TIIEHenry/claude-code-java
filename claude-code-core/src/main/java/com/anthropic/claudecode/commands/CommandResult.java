/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands
 */
package com.anthropic.claudecode.commands;

import java.util.Optional;

/**
 * Command execution result.
 */
public sealed interface CommandResult permits
        CommandResult.Text,
        CommandResult.Error,
        CommandResult.Skip {

    /**
     * Text result.
     */
    record Text(String value) implements CommandResult {
        public static Text of(String value) {
            return new Text(value);
        }
    }

    /**
     * Error result.
     */
    record Error(String message, Throwable cause) implements CommandResult {
        public static Error of(String message) {
            return new Error(message, null);
        }

        public static Error of(String message, Throwable cause) {
            return new Error(message, cause);
        }
    }

    /**
     * Skip result (no output).
     */
    record Skip() implements CommandResult {
        public static Skip INSTANCE = new Skip();
    }

    /**
     * Check if result is success.
     */
    default boolean isSuccess() {
        return this instanceof Text;
    }

    /**
     * Get text value if present.
     */
    default Optional<String> getText() {
        if (this instanceof Text t) {
            return Optional.of(t.value());
        }
        return Optional.empty();
    }

    // Static factory methods for convenience
    static Text success(String value) {
        return new Text(value);
    }

    static Error failure(String message) {
        return new Error(message, null);
    }

    static Error failure(String message, Throwable cause) {
        return new Error(message, cause);
    }
}