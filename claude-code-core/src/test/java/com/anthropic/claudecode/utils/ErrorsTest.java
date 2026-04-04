/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.file.NoSuchFileException;
import java.nio.file.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Errors.
 */
class ErrorsTest {

    @Test
    @DisplayName("Errors ClaudeError constructor")
    void claudeError() {
        Errors.ClaudeError error = new Errors.ClaudeError("test message");
        assertEquals("test message", error.getMessage());
    }

    @Test
    @DisplayName("Errors MalformedCommandError constructor")
    void malformedCommandError() {
        Errors.MalformedCommandError error = new Errors.MalformedCommandError("bad command");
        assertEquals("bad command", error.getMessage());
    }

    @Test
    @DisplayName("Errors AbortError default message")
    void abortErrorDefault() {
        Errors.AbortError error = new Errors.AbortError();
        assertEquals("Aborted", error.getMessage());
    }

    @Test
    @DisplayName("Errors AbortError custom message")
    void abortErrorCustom() {
        Errors.AbortError error = new Errors.AbortError("custom abort");
        assertEquals("custom abort", error.getMessage());
    }

    @Test
    @DisplayName("Errors isAbortError true for AbortError")
    void isAbortErrorTrue() {
        Errors.AbortError error = new Errors.AbortError();
        assertTrue(Errors.isAbortError(error));
    }

    @Test
    @DisplayName("Errors isAbortError false for other")
    void isAbortErrorFalse() {
        RuntimeException error = new RuntimeException("other");
        assertFalse(Errors.isAbortError(error));
    }

    @Test
    @DisplayName("Errors ConfigParseError constructor")
    void configParseError() {
        Errors.ConfigParseError error = new Errors.ConfigParseError("parse failed", "/path/to/config", "default");
        assertEquals("parse failed", error.getMessage());
        assertEquals("/path/to/config", error.getFilePath());
        assertEquals("default", error.getDefaultConfig());
    }

    @Test
    @DisplayName("Errors ShellError constructor")
    void shellError() {
        Errors.ShellError error = new Errors.ShellError("stdout", "stderr", 1, false);
        assertEquals("stdout", error.getStdout());
        assertEquals("stderr", error.getStderr());
        assertEquals(1, error.getCode());
        assertFalse(error.isInterrupted());
    }

    @Test
    @DisplayName("Errors ShellError interrupted")
    void shellErrorInterrupted() {
        Errors.ShellError error = new Errors.ShellError("", "", -1, true);
        assertTrue(error.isInterrupted());
    }

    @Test
    @DisplayName("Errors TeleportOperationError constructor")
    void teleportOperationError() {
        Errors.TeleportOperationError error = new Errors.TeleportOperationError("message", "formatted");
        assertEquals("message", error.getMessage());
        assertEquals("formatted", error.getFormattedMessage());
    }

    @Test
    @DisplayName("Errors TelemetrySafeError single message")
    void telemetrySafeErrorSingle() {
        Errors.TelemetrySafeError error = new Errors.TelemetrySafeError("safe message");
        assertEquals("safe message", error.getMessage());
        assertEquals("safe message", error.getTelemetryMessage());
    }

    @Test
    @DisplayName("Errors TelemetrySafeError dual message")
    void telemetrySafeErrorDual() {
        Errors.TelemetrySafeError error = new Errors.TelemetrySafeError("message", "telemetry");
        assertEquals("message", error.getMessage());
        assertEquals("telemetry", error.getTelemetryMessage());
    }

    @Test
    @DisplayName("Errors hasExactErrorMessage true")
    void hasExactErrorMessageTrue() {
        RuntimeException error = new RuntimeException("exact message");
        assertTrue(Errors.hasExactErrorMessage(error, "exact message"));
    }

    @Test
    @DisplayName("Errors hasExactErrorMessage false different")
    void hasExactErrorMessageFalseDifferent() {
        RuntimeException error = new RuntimeException("different message");
        assertFalse(Errors.hasExactErrorMessage(error, "exact message"));
    }

    @Test
    @DisplayName("Errors hasExactErrorMessage false null")
    void hasExactErrorMessageFalseNull() {
        assertFalse(Errors.hasExactErrorMessage(null, "message"));
    }

    @Test
    @DisplayName("Errors toError RuntimeException unchanged")
    void toErrorRuntimeException() {
        RuntimeException error = new RuntimeException("test");
        assertSame(error, Errors.toError(error));
    }

    @Test
    @DisplayName("Errors toError Exception wrapped")
    void toErrorException() {
        Exception ex = new Exception("test");
        RuntimeException result = Errors.toError(ex);
        assertSame(ex, result.getCause());
    }

    @Test
    @DisplayName("Errors toError other wrapped")
    void toErrorOther() {
        RuntimeException result = Errors.toError("string");
        assertEquals("string", result.getMessage());
    }

    @Test
    @DisplayName("Errors errorMessage from Throwable")
    void errorMessageThrowable() {
        RuntimeException error = new RuntimeException("test message");
        assertEquals("test message", Errors.errorMessage(error));
    }

    @Test
    @DisplayName("Errors errorMessage from other")
    void errorMessageOther() {
        assertEquals("string value", Errors.errorMessage("string value"));
    }

    @Test
    @DisplayName("Errors getErrnoCode ENOENT")
    void getErrnoCodeENOENT() throws Exception {
        NoSuchFileException ex = new NoSuchFileException("file");
        assertEquals("ENOENT", Errors.getErrnoCode(ex));
    }

    @Test
    @DisplayName("Errors getErrnoCode EACCES")
    void getErrnoCodeEACCES() throws Exception {
        AccessDeniedException ex = new AccessDeniedException("file");
        assertEquals("EACCES", Errors.getErrnoCode(ex));
    }

    @Test
    @DisplayName("Errors getErrnoCode null for other")
    void getErrnoCodeOther() {
        RuntimeException ex = new RuntimeException("other");
        assertNull(Errors.getErrnoCode(ex));
    }

    @Test
    @DisplayName("Errors isENOENT true")
    void isENOENTTrue() throws Exception {
        NoSuchFileException ex = new NoSuchFileException("file");
        assertTrue(Errors.isENOENT(ex));
    }

    @Test
    @DisplayName("Errors isENOENT false")
    void isENOENTFalse() {
        RuntimeException ex = new RuntimeException("other");
        assertFalse(Errors.isENOENT(ex));
    }

    @Test
    @DisplayName("Errors getErrnoPath from FileSystemException")
    void getErrnoPath() throws Exception {
        NoSuchFileException ex = new NoSuchFileException("/path/to/file");
        assertEquals("/path/to/file", Errors.getErrnoPath(ex));
    }

    @Test
    @DisplayName("Errors getErrnoPath null for other")
    void getErrnoPathOther() {
        RuntimeException ex = new RuntimeException("other");
        assertNull(Errors.getErrnoPath(ex));
    }

    @Test
    @DisplayName("Errors shortErrorStack includes message")
    void shortErrorStackMessage() {
        RuntimeException error = new RuntimeException("test error");
        String stack = Errors.shortErrorStack(error);
        assertTrue(stack.contains("test error"));
    }

    @Test
    @DisplayName("Errors shortErrorStack null returns null")
    void shortErrorStackNull() {
        assertEquals("null", Errors.shortErrorStack(null));
    }

    @Test
    @DisplayName("Errors shortErrorStack limits frames")
    void shortErrorStackLimitsFrames() {
        RuntimeException error = new RuntimeException("test");
        String stack = Errors.shortErrorStack(error, 2);
        assertTrue(stack.contains("at "));
    }

    @Test
    @DisplayName("Errors isFsInaccessible true for ENOENT")
    void isFsInaccessibleENOENT() throws Exception {
        NoSuchFileException ex = new NoSuchFileException("file");
        assertTrue(Errors.isFsInaccessible(ex));
    }

    @Test
    @DisplayName("Errors isFsInaccessible true for EACCES")
    void isFsInaccessibleEACCES() throws Exception {
        AccessDeniedException ex = new AccessDeniedException("file");
        assertTrue(Errors.isFsInaccessible(ex));
    }

    @Test
    @DisplayName("Errors isFsInaccessible false for other")
    void isFsInaccessibleOther() {
        RuntimeException ex = new RuntimeException("other");
        assertFalse(Errors.isFsInaccessible(ex));
    }

    @Test
    @DisplayName("Errors AxiosErrorKind enum values")
    void axiosErrorKindEnum() {
        Errors.AxiosErrorKind[] values = Errors.AxiosErrorKind.values();
        assertEquals(5, values.length);
    }

    @Test
    @DisplayName("Errors AxiosErrorClassification record")
    void axiosErrorClassification() {
        Errors.AxiosErrorClassification classification =
            new Errors.AxiosErrorClassification(Errors.AxiosErrorKind.NETWORK, 500, "error");
        assertEquals(Errors.AxiosErrorKind.NETWORK, classification.kind());
        assertEquals(500, classification.status());
        assertEquals("error", classification.message());
    }

    @Test
    @DisplayName("Errors classifyHttpError timeout")
    void classifyHttpErrorTimeout() {
        java.net.SocketTimeoutException ex = new java.net.SocketTimeoutException("timeout");
        Errors.AxiosErrorClassification result = Errors.classifyHttpError(ex);
        assertEquals(Errors.AxiosErrorKind.TIMEOUT, result.kind());
    }

    @Test
    @DisplayName("Errors classifyHttpError network connect")
    void classifyHttpErrorNetworkConnect() {
        java.net.ConnectException ex = new java.net.ConnectException("connection refused");
        Errors.AxiosErrorClassification result = Errors.classifyHttpError(ex);
        assertEquals(Errors.AxiosErrorKind.NETWORK, result.kind());
    }

    @Test
    @DisplayName("Errors classifyHttpError network unknown host")
    void classifyHttpErrorNetworkUnknownHost() {
        java.net.UnknownHostException ex = new java.net.UnknownHostException("unknown host");
        Errors.AxiosErrorClassification result = Errors.classifyHttpError(ex);
        assertEquals(Errors.AxiosErrorKind.NETWORK, result.kind());
    }

    @Test
    @DisplayName("Errors classifyHttpError auth 401")
    void classifyHttpErrorAuth401() {
        RuntimeException ex = new RuntimeException("Error 401 Unauthorized");
        Errors.AxiosErrorClassification result = Errors.classifyHttpError(ex);
        assertEquals(Errors.AxiosErrorKind.AUTH, result.kind());
        assertEquals(401, result.status());
    }

    @Test
    @DisplayName("Errors classifyHttpError auth 403")
    void classifyHttpErrorAuth403() {
        RuntimeException ex = new RuntimeException("Error 403 Forbidden");
        Errors.AxiosErrorClassification result = Errors.classifyHttpError(ex);
        assertEquals(Errors.AxiosErrorKind.AUTH, result.kind());
        assertEquals(403, result.status());
    }

    @Test
    @DisplayName("Errors classifyHttpError other")
    void classifyHttpErrorOther() {
        RuntimeException ex = new RuntimeException("Some other error");
        Errors.AxiosErrorClassification result = Errors.classifyHttpError(ex);
        assertEquals(Errors.AxiosErrorKind.OTHER, result.kind());
    }
}