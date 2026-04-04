/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.hooks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OutputResult.
 */
class OutputResultTest {

    @Test
    @DisplayName("OutputResult record creation")
    void recordCreation() {
        OutputResult result = new OutputResult("stdout content", "stderr content", "combined output");
        assertEquals("stdout content", result.stdout());
        assertEquals("stderr content", result.stderr());
        assertEquals("combined output", result.combined());
    }

    @Test
    @DisplayName("OutputResult empty creates empty result")
    void empty() {
        OutputResult result = OutputResult.empty();
        assertEquals("", result.stdout());
        assertEquals("", result.stderr());
        assertEquals("", result.combined());
    }

    @Test
    @DisplayName("OutputResult with null values")
    void withNullValues() {
        OutputResult result = new OutputResult(null, null, null);
        assertNull(result.stdout());
        assertNull(result.stderr());
        assertNull(result.combined());
    }

    @Test
    @DisplayName("OutputResult with empty strings")
    void withEmptyStrings() {
        OutputResult result = new OutputResult("", "", "");
        assertEquals("", result.stdout());
        assertEquals("", result.stderr());
        assertEquals("", result.combined());
    }

    @Test
    @DisplayName("OutputResult with partial content")
    void withPartialContent() {
        OutputResult result = new OutputResult("output", "", "");
        assertEquals("output", result.stdout());
        assertEquals("", result.stderr());
        assertEquals("", result.combined());
    }

    @Test
    @DisplayName("OutputResult multiline content")
    void withMultilineContent() {
        String multiline = "line1\nline2\nline3";
        OutputResult result = new OutputResult(multiline, "", multiline);
        assertEquals(multiline, result.stdout());
        assertEquals(multiline, result.combined());
    }
}