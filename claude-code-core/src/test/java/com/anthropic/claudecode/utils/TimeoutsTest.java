/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Timeouts.
 */
class TimeoutsTest {

    @Test
    @DisplayName("Timeouts DEFAULT_TIMEOUT_MS constant")
    void defaultTimeoutConstant() {
        assertEquals(120_000, Timeouts.DEFAULT_TIMEOUT_MS);
    }

    @Test
    @DisplayName("Timeouts MAX_TIMEOUT_MS constant")
    void maxTimeoutConstant() {
        assertEquals(600_000, Timeouts.MAX_TIMEOUT_MS);
    }

    @Test
    @DisplayName("Timeouts getDefaultBashTimeoutMs returns value")
    void getDefaultBashTimeoutMs() {
        int result = Timeouts.getDefaultBashTimeoutMs();

        assertTrue(result > 0);
    }

    @Test
    @DisplayName("Timeouts getMaxBashTimeoutMs returns value")
    void getMaxBashTimeoutMs() {
        int result = Timeouts.getMaxBashTimeoutMs();

        assertTrue(result > 0);
    }

    @Test
    @DisplayName("Timeouts clampTimeout clamps high values")
    void clampTimeoutHigh() {
        int result = Timeouts.clampTimeout(Integer.MAX_VALUE);

        assertTrue(result <= Timeouts.getMaxBashTimeoutMs());
    }

    @Test
    @DisplayName("Timeouts clampTimeout clamps low values")
    void clampTimeoutLow() {
        int result = Timeouts.clampTimeout(0);

        assertEquals(1000, result);
    }

    @Test
    @DisplayName("Timeouts clampTimeout keeps valid values")
    void clampTimeoutValid() {
        int result = Timeouts.clampTimeout(5000);

        assertEquals(5000, result);
    }

    @Test
    @DisplayName("Timeouts parseTimeout returns value for valid input")
    void parseTimeoutValid() {
        int result = Timeouts.parseTimeout("5000", 10000);

        assertEquals(5000, result);
    }

    @Test
    @DisplayName("Timeouts parseTimeout returns default for null")
    void parseTimeoutNull() {
        int result = Timeouts.parseTimeout(null, 10000);

        assertEquals(10000, result);
    }

    @Test
    @DisplayName("Timeouts parseTimeout returns default for empty")
    void parseTimeoutEmpty() {
        int result = Timeouts.parseTimeout("", 10000);

        assertEquals(10000, result);
    }

    @Test
    @DisplayName("Timeouts parseTimeout returns default for invalid")
    void parseTimeoutInvalid() {
        int result = Timeouts.parseTimeout("abc", 10000);

        assertEquals(10000, result);
    }

    @Test
    @DisplayName("Timeouts parseTimeout returns default for negative")
    void parseTimeoutNegative() {
        int result = Timeouts.parseTimeout("-1000", 10000);

        assertEquals(10000, result);
    }

    @Test
    @DisplayName("Timeouts parseTimeout clamps value")
    void parseTimeoutClamps() {
        int result = Timeouts.parseTimeout("100", 10000);

        assertEquals(1000, result); // Clamped to minimum
    }

    @Test
    @DisplayName("Timeouts getEnvTimeout returns default for missing env")
    void getEnvTimeoutMissing() {
        int result = Timeouts.getEnvTimeout("NONEXISTENT_VAR_12345", 15000);

        assertEquals(15000, result);
    }
}