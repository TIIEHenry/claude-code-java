/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClaudeError.
 */
class ClaudeErrorTest {

    @Test
    @DisplayName("ClaudeError constructor with message")
    void claudeErrorMessage() {
        ClaudeError error = new ClaudeError("Test error");

        assertEquals("Test error", error.getMessage());
    }

    @Test
    @DisplayName("ClaudeError constructor with message and cause")
    void claudeErrorMessageAndCause() {
        Exception cause = new RuntimeException("Cause");
        ClaudeError error = new ClaudeError("Test error", cause);

        assertEquals("Test error", error.getMessage());
        assertEquals(cause, error.getCause());
    }

    @Test
    @DisplayName("ClaudeError extends RuntimeException")
    void claudeErrorExceptionType() {
        ClaudeError error = new ClaudeError("Test");

        assertTrue(error instanceof RuntimeException);
    }

    @Test
    @DisplayName("ClaudeError can be thrown and caught")
    void claudeErrorThrowCatch() {
        assertThrows(ClaudeError.class, () -> {
            throw new ClaudeError("Test error");
        });
    }
}