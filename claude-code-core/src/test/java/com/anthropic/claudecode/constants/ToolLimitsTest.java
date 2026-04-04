/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ToolLimits.
 */
class ToolLimitsTest {

    @Test
    @DisplayName("ToolLimits BASH_MAX_OUTPUT_CHARS is positive")
    void bashMaxOutputChars() {
        assertTrue(ToolLimits.BASH_MAX_OUTPUT_CHARS > 0);
        assertEquals(30000, ToolLimits.BASH_MAX_OUTPUT_CHARS);
    }

    @Test
    @DisplayName("ToolLimits BASH_DEFAULT_TIMEOUT_MS is 2 minutes")
    void bashDefaultTimeout() {
        assertEquals(120000, ToolLimits.BASH_DEFAULT_TIMEOUT_MS);
    }

    @Test
    @DisplayName("ToolLimits BASH_MAX_TIMEOUT_MS is 10 minutes")
    void bashMaxTimeout() {
        assertEquals(600000, ToolLimits.BASH_MAX_TIMEOUT_MS);
    }

    @Test
    @DisplayName("ToolLimits FILE_READ_MAX_LINES is positive")
    void fileReadMaxLines() {
        assertTrue(ToolLimits.FILE_READ_MAX_LINES > 0);
        assertEquals(2000, ToolLimits.FILE_READ_MAX_LINES);
    }

    @Test
    @DisplayName("ToolLimits FILE_READ_MAX_BYTES is 10MB")
    void fileReadMaxBytes() {
        assertEquals(10 * 1024 * 1024, ToolLimits.FILE_READ_MAX_BYTES);
    }

    @Test
    @DisplayName("ToolLimits FILE_WRITE_MAX_BYTES is 10MB")
    void fileWriteMaxBytes() {
        assertEquals(10 * 1024 * 1024, ToolLimits.FILE_WRITE_MAX_BYTES);
    }

    @Test
    @DisplayName("ToolLimits GLOB_MAX_RESULTS is positive")
    void globMaxResults() {
        assertTrue(ToolLimits.GLOB_MAX_RESULTS > 0);
        assertEquals(1000, ToolLimits.GLOB_MAX_RESULTS);
    }

    @Test
    @DisplayName("ToolLimits GREP_MAX_RESULTS is positive")
    void grepMaxResults() {
        assertTrue(ToolLimits.GREP_MAX_RESULTS > 0);
        assertEquals(100, ToolLimits.GREP_MAX_RESULTS);
    }

    @Test
    @DisplayName("ToolLimits GREP_MAX_CONTEXT_LINES is positive")
    void grepMaxContextLines() {
        assertTrue(ToolLimits.GREP_MAX_CONTEXT_LINES > 0);
        assertEquals(10, ToolLimits.GREP_MAX_CONTEXT_LINES);
    }

    @Test
    @DisplayName("ToolLimits WEB_FETCH_MAX_BYTES is 5MB")
    void webFetchMaxBytes() {
        assertEquals(5 * 1024 * 1024, ToolLimits.WEB_FETCH_MAX_BYTES);
    }

    @Test
    @DisplayName("ToolLimits WEB_FETCH_TIMEOUT_MS is 30 seconds")
    void webFetchTimeout() {
        assertEquals(30000, ToolLimits.WEB_FETCH_TIMEOUT_MS);
    }

    @Test
    @DisplayName("ToolLimits WEB_SEARCH_MAX_RESULTS is positive")
    void webSearchMaxResults() {
        assertTrue(ToolLimits.WEB_SEARCH_MAX_RESULTS > 0);
        assertEquals(10, ToolLimits.WEB_SEARCH_MAX_RESULTS);
    }

    @Test
    @DisplayName("ToolLimits AGENT_MAX_TURNS is positive")
    void agentMaxTurns() {
        assertTrue(ToolLimits.AGENT_MAX_TURNS > 0);
        assertEquals(50, ToolLimits.AGENT_MAX_TURNS);
    }

    @Test
    @DisplayName("ToolLimits AGENT_MAX_NESTING_DEPTH is positive")
    void agentMaxNestingDepth() {
        assertTrue(ToolLimits.AGENT_MAX_NESTING_DEPTH > 0);
        assertEquals(3, ToolLimits.AGENT_MAX_NESTING_DEPTH);
    }

    @Test
    @DisplayName("ToolLimits ASK_QUESTION_MAX_OPTIONS is 4")
    void askQuestionMaxOptions() {
        assertEquals(4, ToolLimits.ASK_QUESTION_MAX_OPTIONS);
    }

    @Test
    @DisplayName("ToolLimits IMAGE_MAX_DIMENSION is positive")
    void imageMaxDimension() {
        assertTrue(ToolLimits.IMAGE_MAX_DIMENSION > 0);
        assertEquals(768, ToolLimits.IMAGE_MAX_DIMENSION);
    }

    @Test
    @DisplayName("ToolLimits IMAGE_MAX_BYTES is 5MB")
    void imageMaxBytes() {
        assertEquals(5 * 1024 * 1024, ToolLimits.IMAGE_MAX_BYTES);
    }

    @Test
    @DisplayName("ToolLimits IMAGE_SUPPORTED_TYPES contains expected types")
    void imageSupportedTypes() {
        String[] types = ToolLimits.IMAGE_SUPPORTED_TYPES;
        assertEquals(4, types.length);
        assertArrayEquals(new String[]{"image/png", "image/jpeg", "image/gif", "image/webp"}, types);
    }

    @Test
    @DisplayName("ToolLimits NOTEBOOK_MAX_CELL_SIZE is positive")
    void notebookMaxCellSize() {
        assertTrue(ToolLimits.NOTEBOOK_MAX_CELL_SIZE > 0);
        assertEquals(100000, ToolLimits.NOTEBOOK_MAX_CELL_SIZE);
    }

    @Test
    @DisplayName("ToolLimits TASK_OUTPUT_MAX_LINES is positive")
    void taskOutputMaxLines() {
        assertTrue(ToolLimits.TASK_OUTPUT_MAX_LINES > 0);
        assertEquals(1000, ToolLimits.TASK_OUTPUT_MAX_LINES);
    }

    @Test
    @DisplayName("ToolLimits timeout constraints are consistent")
    void timeoutConstraints() {
        assertTrue(ToolLimits.BASH_DEFAULT_TIMEOUT_MS <= ToolLimits.BASH_MAX_TIMEOUT_MS);
    }

    @Test
    @DisplayName("ToolLimits all limits are reasonable values")
    void allLimitsAreReasonable() {
        // All limits should be positive
        assertTrue(ToolLimits.BASH_MAX_OUTPUT_CHARS > 0);
        assertTrue(ToolLimits.FILE_READ_MAX_LINES > 0);
        assertTrue(ToolLimits.FILE_READ_MAX_BYTES > 0);
        assertTrue(ToolLimits.GLOB_MAX_RESULTS > 0);
        assertTrue(ToolLimits.GREP_MAX_RESULTS > 0);
    }
}