/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/errors.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Error handling utilities.
 */
public final class ErrorUtils {
    private ErrorUtils() {}

    // Common error patterns
    private static final Pattern API_ERROR_PATTERN = Pattern.compile("^(\\d{3})\\s+(.+)$");
    private static final Pattern RATE_LIMIT_PATTERN = Pattern.compile("rate.?limit", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIMEOUT_PATTERN = Pattern.compile("timeout|timed?\\s*out", Pattern.CASE_INSENSITIVE);

    /**
     * Error types.
     */
    public enum ErrorType {
        API_ERROR,
        RATE_LIMIT,
        TIMEOUT,
        PERMISSION_DENIED,
        FILE_NOT_FOUND,
        INVALID_INPUT,
        NETWORK_ERROR,
        UNKNOWN
    }

    /**
     * Classified error result.
     */
    public record ClassifiedError(
            ErrorType type,
            String message,
            String code,
            boolean retryable
    ) {
        public static ClassifiedError unknown(String message) {
            return new ClassifiedError(ErrorType.UNKNOWN, message, null, false);
        }
    }

    /**
     * Classify an error based on its message.
     */
    public static ClassifiedError classifyError(Throwable error) {
        if (error == null) {
            return ClassifiedError.unknown(null);
        }

        String message = error.getMessage();
        if (message == null) {
            message = error.getClass().getSimpleName();
        }

        // Check for rate limit
        if (RATE_LIMIT_PATTERN.matcher(message).find()) {
            return new ClassifiedError(ErrorType.RATE_LIMIT, message, "rate_limit", true);
        }

        // Check for timeout
        if (TIMEOUT_PATTERN.matcher(message).find()) {
            return new ClassifiedError(ErrorType.TIMEOUT, message, "timeout", true);
        }

        // Check for API error format
        var matcher = API_ERROR_PATTERN.matcher(message);
        if (matcher.find()) {
            String code = matcher.group(1);
            String detail = matcher.group(2);

            switch (code) {
                case "401":
                case "403":
                    return new ClassifiedError(ErrorType.PERMISSION_DENIED, detail, code, false);
                case "404":
                    return new ClassifiedError(ErrorType.FILE_NOT_FOUND, detail, code, false);
                case "400":
                    return new ClassifiedError(ErrorType.INVALID_INPUT, detail, code, false);
                case "429":
                    return new ClassifiedError(ErrorType.RATE_LIMIT, detail, code, true);
                case "500":
                case "502":
                case "503":
                case "504":
                    return new ClassifiedError(ErrorType.API_ERROR, detail, code, true);
                default:
                    return new ClassifiedError(ErrorType.API_ERROR, detail, code, false);
            }
        }

        // Check for network errors
        if (message.contains("Connection refused") ||
            message.contains("Network is unreachable") ||
            message.contains("No route to host")) {
            return new ClassifiedError(ErrorType.NETWORK_ERROR, message, "network", true);
        }

        // Check exception type
        String className = error.getClass().getName();
        if (className.contains("FileNotFoundException") || className.contains("NoSuchFile")) {
            return new ClassifiedError(ErrorType.FILE_NOT_FOUND, message, "file_not_found", false);
        }

        if (className.contains("SecurityException") || className.contains("AccessDenied")) {
            return new ClassifiedError(ErrorType.PERMISSION_DENIED, message, "permission_denied", false);
        }

        if (className.contains("IllegalArgumentException") || className.contains("ValidationException")) {
            return new ClassifiedError(ErrorType.INVALID_INPUT, message, "invalid_input", false);
        }

        return ClassifiedError.unknown(message);
    }

    /**
     * Check if error has a specific message.
     */
    public static boolean hasExactErrorMessage(Throwable error, String expectedMessage) {
        if (error == null || expectedMessage == null) {
            return false;
        }
        return expectedMessage.equals(error.getMessage());
    }

    /**
     * Check if error message starts with a prefix.
     */
    public static boolean startsWithErrorMessage(Throwable error, String prefix) {
        if (error == null || prefix == null) {
            return false;
        }
        String message = error.getMessage();
        return message != null && message.startsWith(prefix);
    }

    /**
     * Get a user-friendly error message.
     */
    public static String getUserFriendlyMessage(Throwable error) {
        ClassifiedError classified = classifyError(error);

        return switch (classified.type()) {
            case RATE_LIMIT -> "Rate limited. Please wait a moment and try again.";
            case TIMEOUT -> "The operation timed out. Please try again.";
            case PERMISSION_DENIED -> "Permission denied: " + classified.message();
            case FILE_NOT_FOUND -> "File not found: " + classified.message();
            case INVALID_INPUT -> "Invalid input: " + classified.message();
            case NETWORK_ERROR -> "Network error. Please check your connection.";
            case API_ERROR -> "API error: " + classified.message();
            default -> "An error occurred: " + classified.message();
        };
    }

    /**
     * Get retry delay for an error (in milliseconds).
     */
    public static long getRetryDelay(Throwable error, int attempt) {
        ClassifiedError classified = classifyError(error);

        if (!classified.retryable()) {
            return -1; // Don't retry
        }

        // Exponential backoff with jitter
        long baseDelay = switch (classified.type()) {
            case RATE_LIMIT -> 60_000; // 60 seconds for rate limits
            case TIMEOUT -> 5_000;     // 5 seconds for timeouts
            case NETWORK_ERROR -> 1_000; // 1 second for network errors
            case API_ERROR -> 2_000;   // 2 seconds for other API errors
            default -> 1_000;
        };

        // Exponential backoff: base * 2^attempt, capped at 60 seconds
        long delay = baseDelay * (1L << Math.min(attempt, 6));
        delay = Math.min(delay, 60_000);

        // Add jitter (±25%)
        Random random = new Random();
        double jitter = 0.75 + (random.nextDouble() * 0.5);
        delay = (long) (delay * jitter);

        return delay;
    }
}