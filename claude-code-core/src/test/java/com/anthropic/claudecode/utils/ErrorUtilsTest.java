/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ErrorUtils.
 */
class ErrorUtilsTest {

    @Test
    @DisplayName("ErrorUtils classifyError handles null")
    void classifyErrorNull() {
        ErrorUtils.ClassifiedError error = ErrorUtils.classifyError(null);

        assertEquals(ErrorUtils.ErrorType.UNKNOWN, error.type());
    }

    @Test
    @DisplayName("ErrorUtils classifyError identifies rate limit")
    void classifyErrorRateLimit() {
        ErrorUtils.ClassifiedError error =
            ErrorUtils.classifyError(new RuntimeException("Rate limit exceeded"));

        assertEquals(ErrorUtils.ErrorType.RATE_LIMIT, error.type());
        assertTrue(error.retryable());
    }

    @Test
    @DisplayName("ErrorUtils classifyError identifies timeout")
    void classifyErrorTimeout() {
        ErrorUtils.ClassifiedError error =
            ErrorUtils.classifyError(new RuntimeException("Connection timed out"));

        assertEquals(ErrorUtils.ErrorType.TIMEOUT, error.type());
        assertTrue(error.retryable());
    }

    @Test
    @DisplayName("ErrorUtils classifyError identifies 401 error")
    void classifyError401() {
        ErrorUtils.ClassifiedError error =
            ErrorUtils.classifyError(new RuntimeException("401 Unauthorized"));

        assertEquals(ErrorUtils.ErrorType.PERMISSION_DENIED, error.type());
        assertFalse(error.retryable());
    }

    @Test
    @DisplayName("ErrorUtils classifyError identifies 404 error")
    void classifyError404() {
        ErrorUtils.ClassifiedError error =
            ErrorUtils.classifyError(new RuntimeException("404 Not Found"));

        assertEquals(ErrorUtils.ErrorType.FILE_NOT_FOUND, error.type());
    }

    @Test
    @DisplayName("ErrorUtils classifyError identifies 429 rate limit")
    void classifyError429() {
        ErrorUtils.ClassifiedError error =
            ErrorUtils.classifyError(new RuntimeException("429 Too Many Requests"));

        assertEquals(ErrorUtils.ErrorType.RATE_LIMIT, error.type());
        assertTrue(error.retryable());
    }

    @Test
    @DisplayName("ErrorUtils classifyError identifies 500 error")
    void classifyError500() {
        ErrorUtils.ClassifiedError error =
            ErrorUtils.classifyError(new RuntimeException("500 Internal Server Error"));

        assertEquals(ErrorUtils.ErrorType.API_ERROR, error.type());
        assertTrue(error.retryable());
    }

    @Test
    @DisplayName("ErrorUtils classifyError identifies network error")
    void classifyErrorNetwork() {
        ErrorUtils.ClassifiedError error =
            ErrorUtils.classifyError(new RuntimeException("Connection refused"));

        assertEquals(ErrorUtils.ErrorType.NETWORK_ERROR, error.type());
        assertTrue(error.retryable());
    }

    @Test
    @DisplayName("ErrorUtils classifyError identifies FileNotFoundException")
    void classifyErrorFileNotFoundException() {
        ErrorUtils.ClassifiedError error =
            ErrorUtils.classifyError(new FileNotFoundException("file.txt"));

        assertEquals(ErrorUtils.ErrorType.FILE_NOT_FOUND, error.type());
    }

    @Test
    @DisplayName("ErrorUtils classifyError identifies IllegalArgumentException")
    void classifyErrorIllegalArgumentException() {
        ErrorUtils.ClassifiedError error =
            ErrorUtils.classifyError(new IllegalArgumentException("Invalid argument"));

        assertEquals(ErrorUtils.ErrorType.INVALID_INPUT, error.type());
    }

    @Test
    @DisplayName("ErrorUtils ClassifiedError.unknown creates unknown")
    void classifiedErrorUnknown() {
        ErrorUtils.ClassifiedError error = ErrorUtils.ClassifiedError.unknown("test");

        assertEquals(ErrorUtils.ErrorType.UNKNOWN, error.type());
        assertEquals("test", error.message());
        assertNull(error.code());
    }

    @Test
    @DisplayName("ErrorUtils hasExactErrorMessage checks exact match")
    void hasExactErrorMessage() {
        RuntimeException ex = new RuntimeException("test message");

        assertTrue(ErrorUtils.hasExactErrorMessage(ex, "test message"));
        assertFalse(ErrorUtils.hasExactErrorMessage(ex, "other"));
        assertFalse(ErrorUtils.hasExactErrorMessage(null, "test"));
    }

    @Test
    @DisplayName("ErrorUtils startsWithErrorMessage checks prefix")
    void startsWithErrorMessage() {
        RuntimeException ex = new RuntimeException("test message");

        assertTrue(ErrorUtils.startsWithErrorMessage(ex, "test"));
        assertFalse(ErrorUtils.startsWithErrorMessage(ex, "other"));
    }

    @Test
    @DisplayName("ErrorUtils getUserFriendlyMessage returns friendly message")
    void getUserFriendlyMessage() {
        String msg = ErrorUtils.getUserFriendlyMessage(new RuntimeException("Rate limit exceeded"));

        assertTrue(msg.contains("Rate limited"));
    }

    @Test
    @DisplayName("ErrorUtils getRetryDelay returns positive for retryable")
    void getRetryDelayRetryable() {
        long delay = ErrorUtils.getRetryDelay(new RuntimeException("Rate limit exceeded"), 0);

        assertTrue(delay > 0);
    }

    @Test
    @DisplayName("ErrorUtils getRetryDelay returns -1 for non-retryable")
    void getRetryDelayNonRetryable() {
        long delay = ErrorUtils.getRetryDelay(new RuntimeException("401 Unauthorized"), 0);

        assertEquals(-1, delay);
    }

    @Test
    @DisplayName("ErrorUtils ErrorType enum values")
    void errorTypeEnum() {
        ErrorUtils.ErrorType[] types = ErrorUtils.ErrorType.values();

        assertTrue(types.length >= 8);
        assertEquals(ErrorUtils.ErrorType.API_ERROR, ErrorUtils.ErrorType.valueOf("API_ERROR"));
    }
}