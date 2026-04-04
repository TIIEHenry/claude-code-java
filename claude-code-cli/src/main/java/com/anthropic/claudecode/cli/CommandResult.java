/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code CLI module
 */
package com.anthropic.claudecode.cli;

import java.util.*;

/**
 * Command execution result.
 */
public class CommandResult {

    private final boolean success;
    private final String message;
    private final Object data;
    private final String error;
    private final ResultType type;
    private final Map<String, Object> metadata;

    public CommandResult(boolean success, String message, Object data,
                          String error, ResultType type,
                          Map<String, Object> metadata) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
        this.type = type;
        this.metadata = new HashMap<>(metadata);
    }

    /**
     * Create success result.
     */
    public static CommandResult success() {
        return new CommandResult(true, null, null, null, ResultType.SUCCESS, Map.of());
    }

    /**
     * Create success with message.
     */
    public static CommandResult success(String message) {
        return new CommandResult(true, message, null, null, ResultType.SUCCESS, Map.of());
    }

    /**
     * Create success with data.
     */
    public static CommandResult success(Object data) {
        return new CommandResult(true, null, data, null, ResultType.SUCCESS, Map.of());
    }

    /**
     * Create success with message and data.
     */
    public static CommandResult success(String message, Object data) {
        return new CommandResult(true, message, data, null, ResultType.SUCCESS, Map.of());
    }

    /**
     * Create error result.
     */
    public static CommandResult error(String error) {
        return new CommandResult(false, null, null, error, ResultType.ERROR, Map.of());
    }

    /**
     * Create error with message.
     */
    public static CommandResult error(String error, String message) {
        return new CommandResult(false, message, null, error, ResultType.ERROR, Map.of());
    }

    /**
     * Create empty result.
     */
    public static CommandResult empty() {
        return new CommandResult(true, null, null, null, ResultType.EMPTY, Map.of());
    }

    /**
     * Create info result.
     */
    public static CommandResult info(String message) {
        return new CommandResult(true, message, null, null, ResultType.INFO, Map.of());
    }

    /**
     * Create warning result.
     */
    public static CommandResult warning(String message) {
        return new CommandResult(true, message, null, null, ResultType.WARNING, Map.of());
    }

    /**
     * Create data result.
     */
    public static CommandResult data(Object data) {
        return new CommandResult(true, null, data, null, ResultType.DATA, Map.of());
    }

    /**
     * Create table result.
     */
    public static CommandResult table(List<Map<String, Object>> rows) {
        return new CommandResult(true, null, rows, null, ResultType.TABLE, Map.of());
    }

    /**
     * Check if successful.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Check if error.
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * Check if has message.
     */
    public boolean hasMessage() {
        return message != null;
    }

    /**
     * Check if has data.
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * Get message.
     */
    public String message() {
        return message;
    }

    /**
     * Get data.
     */
    public Object data() {
        return data;
    }

    /**
     * Get data with type.
     */
    public <T> T data(Class<T> type) {
        return data != null && type.isInstance(data) ? type.cast(data) : null;
    }

    /**
     * Get error.
     */
    public String error() {
        return error;
    }

    /**
     * Get result type.
     */
    public ResultType type() {
        return type;
    }

    /**
     * Get metadata.
     */
    public Map<String, Object> metadata() {
        return new HashMap<>(metadata);
    }

    /**
     * Add metadata.
     */
    public CommandResult withMetadata(String key, Object value) {
        Map<String, Object> newMetadata = new HashMap<>(metadata);
        newMetadata.put(key, value);
        return new CommandResult(success, message, data, error, type, newMetadata);
    }

    /**
     * Get exit code.
     */
    public int exitCode() {
        return success ? 0 : 1;
    }

    /**
     * Format as string.
     */
    public String format() {
        if (hasError()) {
            return "Error: " + error;
        }
        if (hasMessage()) {
            return message;
        }
        if (hasData()) {
            return formatData(data);
        }
        return "";
    }

    private String formatData(Object data) {
        if (data instanceof Map) {
            StringBuilder sb = new StringBuilder();
            ((Map<?, ?>) data).forEach((k, v) ->
                sb.append(k).append(": ").append(v).append("\n"));
            return sb.toString();
        }
        if (data instanceof List) {
            StringBuilder sb = new StringBuilder();
            int i = 1;
            for (Object item : (List<?>) data) {
                sb.append(i++).append(". ").append(item).append("\n");
            }
            return sb.toString();
        }
        return data.toString();
    }

    /**
     * Result type enum.
     */
    public enum ResultType {
        SUCCESS,
        ERROR,
        EMPTY,
        INFO,
        WARNING,
        DATA,
        TABLE
    }
}