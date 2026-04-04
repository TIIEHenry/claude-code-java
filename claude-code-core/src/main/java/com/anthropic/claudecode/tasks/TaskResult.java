/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code Task result
 */
package com.anthropic.claudecode.tasks;

/**
 * Task execution result.
 */
public final class TaskResult {
    private final boolean success;
    private final String output;
    private final String error;
    private final long durationMs;

    private TaskResult(boolean success, String output, String error, long durationMs) {
        this.success = success;
        this.output = output;
        this.error = error;
        this.durationMs = durationMs;
    }

    /**
     * Create a successful result.
     */
    public static TaskResult success(String output) {
        return new TaskResult(true, output, null, 0);
    }

    /**
     * Create a successful result with duration.
     */
    public static TaskResult success(String output, long durationMs) {
        return new TaskResult(true, output, null, durationMs);
    }

    /**
     * Create a failed result.
     */
    public static TaskResult failure(String error) {
        return new TaskResult(false, null, error, 0);
    }

    /**
     * Create a failed result with duration.
     */
    public static TaskResult failure(String error, long durationMs) {
        return new TaskResult(false, null, error, durationMs);
    }

    /**
     * Check if successful.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Get the output.
     */
    public String getOutput() {
        return output;
    }

    /**
     * Get the error.
     */
    public String getError() {
        return error;
    }

    /**
     * Get the duration in milliseconds.
     */
    public long getDurationMs() {
        return durationMs;
    }

    @Override
    public String toString() {
        if (success) {
            return "TaskResult[success, output=" + (output != null ? output.length() + " chars" : "null") + "]";
        } else {
            return "TaskResult[failure, error=" + error + "]";
        }
    }
}