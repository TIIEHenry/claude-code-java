/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ErrorsNew.
 */
class ErrorsNewTest {

    @Test
    @DisplayName("ErrorsNew ClaudeError")
    void claudeError() {
        ErrorsNew.ClaudeError error = new ErrorsNew.ClaudeError("Test error");
        assertEquals("Test error", error.getMessage());
    }

    @Test
    @DisplayName("ErrorsNew MalformedCommandError")
    void malformedCommandError() {
        ErrorsNew.MalformedCommandError error = new ErrorsNew.MalformedCommandError("Bad command");
        assertEquals("Bad command", error.getMessage());
    }

    @Test
    @DisplayName("ErrorsNew AbortError default message")
    void abortErrorDefault() {
        ErrorsNew.AbortError error = new ErrorsNew.AbortError();
        assertEquals("Operation aborted", error.getMessage());
    }

    @Test
    @DisplayName("ErrorsNew AbortError custom message")
    void abortErrorCustom() {
        ErrorsNew.AbortError error = new ErrorsNew.AbortError("Custom abort");
        assertEquals("Custom abort", error.getMessage());
    }

    @Test
    @DisplayName("ErrorsNew isAbortError with AbortError")
    void isAbortErrorWithAbortError() {
        ErrorsNew.AbortError error = new ErrorsNew.AbortError();
        assertTrue(ErrorsNew.isAbortError(error));
    }

    @Test
    @DisplayName("ErrorsNew isAbortError with other exception")
    void isAbortErrorWithOther() {
        RuntimeException error = new RuntimeException("Not abort");
        assertFalse(ErrorsNew.isAbortError(error));
    }

    @Test
    @DisplayName("ErrorsNew ConfigParseError")
    void configParseError() {
        ErrorsNew.ConfigParseError error = new ErrorsNew.ConfigParseError(
            "Parse failed", "/path/to/config", "default"
        );
        assertEquals("Parse failed", error.getMessage());
        assertEquals("/path/to/config", error.getFilePath());
        assertEquals("default", error.getDefaultConfig());
    }

    @Test
    @DisplayName("ErrorsNew ShellError")
    void shellError() {
        ErrorsNew.ShellError error = new ErrorsNew.ShellError(
            "stdout", "stderr", 1, false
        );
        assertEquals("stdout", error.getStdout());
        assertEquals("stderr", error.getStderr());
        assertEquals(1, error.getExitCode());
        assertFalse(error.isInterrupted());
    }

    @Test
    @DisplayName("ErrorsNew TeleportOperationError")
    void teleportOperationError() {
        ErrorsNew.TeleportOperationError error = new ErrorsNew.TeleportOperationError(
            "message", "formatted message"
        );
        assertEquals("message", error.getMessage());
        assertEquals("formatted message", error.getFormattedMessage());
    }

    @Test
    @DisplayName("ErrorsNew TelemetrySafeError")
    void telemetrySafeError() {
        ErrorsNew.TelemetrySafeError error = new ErrorsNew.TelemetrySafeError("Safe message");
        assertEquals("Safe message", error.getMessage());
        assertEquals("Safe message", error.getTelemetryMessage());
    }

    @Test
    @DisplayName("ErrorsNew TelemetrySafeError with separate telemetry message")
    void telemetrySafeErrorSeparate() {
        ErrorsNew.TelemetrySafeError error = new ErrorsNew.TelemetrySafeError(
            "Full message", "Telemetry message"
        );
        assertEquals("Full message", error.getMessage());
        assertEquals("Telemetry message", error.getTelemetryMessage());
    }

    @Test
    @DisplayName("ErrorsNew hasExactErrorMessage true")
    void hasExactErrorMessageTrue() {
        Exception e = new Exception("exact message");
        assertTrue(ErrorsNew.hasExactErrorMessage(e, "exact message"));
    }

    @Test
    @DisplayName("ErrorsNew hasExactErrorMessage false")
    void hasExactErrorMessageFalse() {
        Exception e = new Exception("different message");
        assertFalse(ErrorsNew.hasExactErrorMessage(e, "exact message"));
    }

    @Test
    @DisplayName("ErrorsNew toError with RuntimeException")
    void toErrorWithRuntime() {
        RuntimeException e = new RuntimeException("test");
        assertSame(e, ErrorsNew.toError(e));
    }

    @Test
    @DisplayName("ErrorsNew toError with Exception")
    void toErrorWithException() {
        Exception e = new Exception("test");
        RuntimeException result = ErrorsNew.toError(e);
        assertSame(e, result.getCause());
    }

    @Test
    @DisplayName("ErrorsNew errorMessage with message")
    void errorMessageWithMessage() {
        Exception e = new Exception("test message");
        assertEquals("test message", ErrorsNew.errorMessage(e));
    }

    @Test
    @DisplayName("ErrorsNew errorMessage null")
    void errorMessageNull() {
        assertEquals("null", ErrorsNew.errorMessage(null));
    }

    @Test
    @DisplayName("ErrorsNew getErrnoCode ENOENT")
    void getErrnoCodeEnoent() {
        Exception e = new Exception("Error: ENOENT: no such file");
        assertEquals("ENOENT", ErrorsNew.getErrnoCode(e));
    }

    @Test
    @DisplayName("ErrorsNew isENOENT true")
    void isEnoentTrue() {
        Exception e = new Exception("Error: ENOENT: no such file");
        assertTrue(ErrorsNew.isENOENT(e));
    }

    @Test
    @DisplayName("ErrorsNew isENOENT false")
    void isEnoentFalse() {
        Exception e = new Exception("Some other error");
        assertFalse(ErrorsNew.isENOENT(e));
    }

    @Test
    @DisplayName("ErrorsNew shortErrorStack")
    void shortErrorStack() {
        Exception e = new Exception("test");
        String stack = ErrorsNew.shortErrorStack(e, 3);
        assertTrue(stack.contains("java.lang.Exception"));
        assertTrue(stack.contains("test"));
    }

    @Test
    @DisplayName("ErrorsNew shortErrorStack null")
    void shortErrorStackNull() {
        assertEquals("null", ErrorsNew.shortErrorStack(null, 3));
    }

    @Test
    @DisplayName("ErrorsNew isFsInaccessible true")
    void isFsInaccessibleTrue() {
        Exception e = new Exception("Error: ENOENT: no such file");
        assertTrue(ErrorsNew.isFsInaccessible(e));
    }

    @Test
    @DisplayName("ErrorsNew isFsInaccessible false")
    void isFsInaccessibleFalse() {
        Exception e = new Exception("Some other error");
        assertFalse(ErrorsNew.isFsInaccessible(e));
    }

    @Test
    @DisplayName("ErrorsNew HttpErrorKind enum")
    void httpErrorKindEnum() {
        assertEquals(5, ErrorsNew.HttpErrorKind.values().length);
    }

    @Test
    @DisplayName("ErrorsNew classifyHttpError null")
    void classifyHttpErrorNull() {
        ErrorsNew.HttpErrorClassification result = ErrorsNew.classifyHttpError(null);
        assertEquals(ErrorsNew.HttpErrorKind.OTHER, result.kind());
    }

    @Test
    @DisplayName("ErrorsNew classifyHttpError auth")
    void classifyHttpErrorAuth() {
        Exception e = new Exception("Error 401: Unauthorized");
        ErrorsNew.HttpErrorClassification result = ErrorsNew.classifyHttpError(e);
        assertEquals(ErrorsNew.HttpErrorKind.AUTH, result.kind());
        assertEquals(401, result.status());
    }

    @Test
    @DisplayName("ErrorsNew classifyHttpError timeout")
    void classifyHttpErrorTimeout() {
        Exception e = new Exception("Connection timeout");
        ErrorsNew.HttpErrorClassification result = ErrorsNew.classifyHttpError(e);
        assertEquals(ErrorsNew.HttpErrorKind.TIMEOUT, result.kind());
    }

    @Test
    @DisplayName("ErrorsNew HttpErrorClassification record")
    void httpErrorClassificationRecord() {
        ErrorsNew.HttpErrorClassification classification = new ErrorsNew.HttpErrorClassification(
            ErrorsNew.HttpErrorKind.HTTP, 500, "Server error"
        );
        assertEquals(ErrorsNew.HttpErrorKind.HTTP, classification.kind());
        assertEquals(500, classification.status());
        assertEquals("Server error", classification.message());
    }
}