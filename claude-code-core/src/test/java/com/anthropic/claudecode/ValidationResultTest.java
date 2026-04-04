/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValidationResult sealed interface.
 */
class ValidationResultTest {

    @Test
    @DisplayName("Success result is success")
    void successResultWorks() {
        ValidationResult success = ValidationResult.success();

        assertTrue(success.isSuccess());
        assertNull(success.message());
        assertEquals(0, success.errorCode());

        assertTrue(success instanceof ValidationResult.Success);
    }

    @Test
    @DisplayName("Failure result is not success")
    void failureResultWorks() {
        ValidationResult failure = ValidationResult.failure("Invalid input");

        assertFalse(failure.isSuccess());
        assertEquals("Invalid input", failure.message());
        assertEquals(1, failure.errorCode());

        assertTrue(failure instanceof ValidationResult.Failure);
    }

    @Test
    @DisplayName("Failure with custom error code works")
    void failureWithErrorCodeWorks() {
        ValidationResult failure = ValidationResult.failure("Not found", 404);

        assertFalse(failure.isSuccess());
        assertEquals("Not found", failure.message());
        assertEquals(404, failure.errorCode());
    }

    @Test
    @DisplayName("Failure.of static method works")
    void failureOfWorks() {
        ValidationResult.Failure failure = ValidationResult.Failure.of("Test error");

        assertEquals("Test error", failure.message());
        assertEquals(1, failure.errorCode());
    }

    @Test
    @DisplayName("Pattern matching on ValidationResult works")
    void patternMatchingWorks() {
        ValidationResult success = ValidationResult.success();
        ValidationResult failure = ValidationResult.failure("Error");

        String successMsg;
        if (success instanceof ValidationResult.Success) {
            successMsg = "OK";
        } else if (success instanceof ValidationResult.Failure f) {
            successMsg = "FAIL: " + f.message();
        } else {
            successMsg = "UNKNOWN";
        }

        String failureMsg;
        if (failure instanceof ValidationResult.Success) {
            failureMsg = "OK";
        } else if (failure instanceof ValidationResult.Failure f) {
            failureMsg = "FAIL: " + f.message();
        } else {
            failureMsg = "UNKNOWN";
        }

        assertEquals("OK", successMsg);
        assertEquals("FAIL: Error", failureMsg);
    }
}