/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EnvValidation.
 */
class EnvValidationTest {

    @Test
    @DisplayName("EnvValidation EnvVarValidationResult.valid")
    void validResult() {
        EnvValidation.EnvVarValidationResult result = EnvValidation.EnvVarValidationResult.valid(100);
        assertEquals(100, result.effective());
        assertEquals("valid", result.status());
        assertNull(result.message());
    }

    @Test
    @DisplayName("EnvValidation EnvVarValidationResult.invalid")
    void invalidResult() {
        EnvValidation.EnvVarValidationResult result = EnvValidation.EnvVarValidationResult.invalid(50, "Bad value");
        assertEquals(50, result.effective());
        assertEquals("invalid", result.status());
        assertEquals("Bad value", result.message());
    }

    @Test
    @DisplayName("EnvValidation EnvVarValidationResult.capped")
    void cappedResult() {
        EnvValidation.EnvVarValidationResult result = EnvValidation.EnvVarValidationResult.capped(1000, "Capped");
        assertEquals(1000, result.effective());
        assertEquals("capped", result.status());
        assertEquals("Capped", result.message());
    }

    @Test
    @DisplayName("EnvValidation validateBoundedIntEnvVar null value")
    void validateBoundedIntEnvVarNull() {
        EnvValidation.EnvVarValidationResult result = EnvValidation.validateBoundedIntEnvVar(
            "TEST_VAR", null, 10, 100
        );
        assertEquals(10, result.effective());
        assertEquals("valid", result.status());
    }

    @Test
    @DisplayName("EnvValidation validateBoundedIntEnvVar empty value")
    void validateBoundedIntEnvVarEmpty() {
        EnvValidation.EnvVarValidationResult result = EnvValidation.validateBoundedIntEnvVar(
            "TEST_VAR", "", 10, 100
        );
        assertEquals(10, result.effective());
        assertEquals("valid", result.status());
    }

    @Test
    @DisplayName("EnvValidation validateBoundedIntEnvVar valid value")
    void validateBoundedIntEnvVarValid() {
        EnvValidation.EnvVarValidationResult result = EnvValidation.validateBoundedIntEnvVar(
            "TEST_VAR", "50", 10, 100
        );
        assertEquals(50, result.effective());
        assertEquals("valid", result.status());
    }

    @Test
    @DisplayName("EnvValidation validateBoundedIntEnvVar capped value")
    void validateBoundedIntEnvVarCapped() {
        EnvValidation.EnvVarValidationResult result = EnvValidation.validateBoundedIntEnvVar(
            "TEST_VAR", "200", 10, 100
        );
        assertEquals(100, result.effective());
        assertEquals("capped", result.status());
    }

    @Test
    @DisplayName("EnvValidation validateBoundedIntEnvVar zero value")
    void validateBoundedIntEnvVarZero() {
        EnvValidation.EnvVarValidationResult result = EnvValidation.validateBoundedIntEnvVar(
            "TEST_VAR", "0", 10, 100
        );
        assertEquals(10, result.effective());
        assertEquals("invalid", result.status());
    }

    @Test
    @DisplayName("EnvValidation validateBoundedIntEnvVar negative value")
    void validateBoundedIntEnvVarNegative() {
        EnvValidation.EnvVarValidationResult result = EnvValidation.validateBoundedIntEnvVar(
            "TEST_VAR", "-5", 10, 100
        );
        assertEquals(10, result.effective());
        assertEquals("invalid", result.status());
    }

    @Test
    @DisplayName("EnvValidation validateBoundedIntEnvVar non-numeric")
    void validateBoundedIntEnvVarNonNumeric() {
        EnvValidation.EnvVarValidationResult result = EnvValidation.validateBoundedIntEnvVar(
            "TEST_VAR", "abc", 10, 100
        );
        assertEquals(10, result.effective());
        assertEquals("invalid", result.status());
    }

    @Test
    @DisplayName("EnvValidation getBoundedIntEnvVar returns effective value")
    void getBoundedIntEnvVar() {
        // Value depends on environment, but should return an int
        int result = EnvValidation.getBoundedIntEnvVar("NONEXISTENT_VAR", 50, 100);
        assertEquals(50, result);
    }
}