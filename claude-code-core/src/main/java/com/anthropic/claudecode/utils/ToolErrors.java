/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tool error utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.util.*;

/**
 * Utilities for formatting tool errors.
 */
public final class ToolErrors {
    private ToolErrors() {}

    private static final int MAX_ERROR_LENGTH = 10000;
    private static final int HALF_LENGTH = 5000;

    /**
     * Format an error for display.
     */
    public static String formatError(Throwable error) {
        if (error == null) {
            return "Unknown error";
        }

        String fullMessage = getFullErrorMessage(error);

        if (fullMessage.length() <= MAX_ERROR_LENGTH) {
            return fullMessage;
        }

        // Truncate if too long
        String start = fullMessage.substring(0, HALF_LENGTH);
        String end = fullMessage.substring(fullMessage.length() - HALF_LENGTH);
        int truncated = fullMessage.length() - MAX_ERROR_LENGTH;

        return String.format("%s\n\n... [%d characters truncated] ...\n\n%s",
                start, truncated, end);
    }

    /**
     * Get full error message from throwable.
     */
    private static String getFullErrorMessage(Throwable error) {
        List<String> parts = getErrorParts(error);
        String joined = String.join("\n", parts).trim();
        return joined.isEmpty() ? "Command failed with no output" : joined;
    }

    /**
     * Get error parts from throwable.
     */
    public static List<String> getErrorParts(Throwable error) {
        List<String> parts = new ArrayList<>();

        // Add main message
        if (error.getMessage() != null) {
            parts.add(error.getMessage());
        }

        // Add stack trace (first few lines)
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        error.printStackTrace(pw);
        String stackTrace = sw.toString();

        // Only add relevant parts of stack trace
        String[] lines = stackTrace.split("\n");
        int maxLines = Math.min(10, lines.length);
        for (int i = 0; i < maxLines; i++) {
            if (lines[i] != null && !lines[i].isEmpty()) {
                parts.add(lines[i]);
            }
        }

        return parts;
    }

    /**
     * Format validation error for missing parameters.
     */
    public static String formatMissingParameter(String paramName) {
        return String.format("The required parameter `%s` is missing", paramName);
    }

    /**
     * Format validation error for unexpected parameters.
     */
    public static String formatUnexpectedParameter(String paramName) {
        return String.format("An unexpected parameter `%s` was provided", paramName);
    }

    /**
     * Format validation error for type mismatch.
     */
    public static String formatTypeMismatch(String paramName, String expected, String received) {
        return String.format(
                "The parameter `%s` type is expected as `%s` but provided as `%s`",
                paramName, expected, received
        );
    }

    /**
     * Build validation error message.
     */
    public static String buildValidationErrorMessage(String toolName, List<String> issues) {
        if (issues.isEmpty()) {
            return toolName + " failed due to unknown validation error";
        }

        String issueWord = issues.size() > 1 ? "issues" : "issue";
        return String.format("%s failed due to the following %s:\n%s",
                toolName, issueWord, String.join("\n", issues));
    }

    /**
     * Truncate a string if too long.
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        int halfLength = maxLength / 2;
        String start = text.substring(0, halfLength);
        String end = text.substring(text.length() - halfLength);
        int truncated = text.length() - maxLength;

        return String.format("%s\n\n... [%d characters truncated] ...\n\n%s",
                start, truncated, end);
    }

    /**
     * Format error with context.
     */
    public static String formatWithContext(String context, Throwable error) {
        return String.format("%s: %s", context, formatError(error));
    }
}