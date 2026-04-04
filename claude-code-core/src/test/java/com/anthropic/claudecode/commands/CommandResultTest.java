/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CommandResult sealed interface.
 */
class CommandResultTest {

    @Test
    @DisplayName("Text result is success")
    void textResultIsSuccess() {
        CommandResult.Text text = CommandResult.Text.of("output");

        assertTrue(text.isSuccess());
        assertEquals("output", text.value());
        assertEquals(Optional.of("output"), text.getText());
    }

    @Test
    @DisplayName("Error result is not success")
    void errorResultIsNotSuccess() {
        CommandResult.Error error = CommandResult.Error.of("Error message");

        assertFalse(error.isSuccess());
        assertEquals("Error message", error.message());
        assertNull(error.cause());
        assertEquals(Optional.empty(), error.getText());
    }

    @Test
    @DisplayName("Error result with cause")
    void errorResultWithCause() {
        Exception cause = new RuntimeException("Root cause");
        CommandResult.Error error = CommandResult.Error.of("Failed", cause);

        assertFalse(error.isSuccess());
        assertEquals("Failed", error.message());
        assertEquals(cause, error.cause());
    }

    @Test
    @DisplayName("Skip result is not success")
    void skipResultIsNotSuccess() {
        CommandResult.Skip skip = CommandResult.Skip.INSTANCE;

        assertFalse(skip.isSuccess());
        assertEquals(Optional.empty(), skip.getText());
    }

    @Test
    @DisplayName("Static factory methods work")
    void staticFactoryMethods() {
        CommandResult.Text text = CommandResult.success("test");
        assertEquals("test", text.value());

        CommandResult.Error error = CommandResult.failure("failed");
        assertEquals("failed", error.message());
        assertNull(error.cause());

        CommandResult.Error errorWithCause = CommandResult.failure("failed", new Exception());
        assertNotNull(errorWithCause.cause());
    }

    @Test
    @DisplayName("Pattern matching on CommandResult works")
    void patternMatchingWorks() {
        CommandResult text = CommandResult.Text.of("output");
        CommandResult error = CommandResult.Error.of("error");
        CommandResult skip = CommandResult.Skip.INSTANCE;

        String textResult;
        if (text instanceof CommandResult.Text t) {
            textResult = t.value();
        } else if (text instanceof CommandResult.Error e) {
            textResult = "error: " + e.message();
        } else if (text instanceof CommandResult.Skip) {
            textResult = "skipped";
        } else {
            textResult = "unknown";
        }
        assertEquals("output", textResult);

        String errorResult;
        if (error instanceof CommandResult.Text t) {
            errorResult = t.value();
        } else if (error instanceof CommandResult.Error e) {
            errorResult = "error: " + e.message();
        } else if (error instanceof CommandResult.Skip) {
            errorResult = "skipped";
        } else {
            errorResult = "unknown";
        }
        assertEquals("error: error", errorResult);
    }
}