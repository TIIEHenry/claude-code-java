/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.components.promptinput;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for PromptInputTypes.
 */
@DisplayName("PromptInputTypes Tests")
class PromptInputTypesTest {

    @Test
    @DisplayName("InputMode enum has correct values")
    void inputModeEnumHasCorrectValues() {
        PromptInputTypes.InputMode[] modes = PromptInputTypes.InputMode.values();

        assertEquals(4, modes.length);
        assertTrue(Arrays.asList(modes).contains(PromptInputTypes.InputMode.NORMAL));
        assertTrue(Arrays.asList(modes).contains(PromptInputTypes.InputMode.MULTILINE));
        assertTrue(Arrays.asList(modes).contains(PromptInputTypes.InputMode.FILE));
        assertTrue(Arrays.asList(modes).contains(PromptInputTypes.InputMode.EDITOR));
    }

    @Test
    @DisplayName("PromptInputState default constructor works correctly")
    void promptInputStateDefaultConstructorWorksCorrectly() {
        PromptInputTypes.PromptInputState state = new PromptInputTypes.PromptInputState();

        assertEquals("", state.text());
        assertEquals(PromptInputTypes.InputMode.NORMAL, state.mode());
        assertEquals(0, state.cursorPosition());
        assertTrue(state.history().isEmpty());
        assertEquals(-1, state.historyIndex());
        assertFalse(state.isSubmitting());
    }

    @Test
    @DisplayName("PromptInputState record works correctly")
    void promptInputStateRecordWorksCorrectly() {
        List<String> history = List.of("cmd1", "cmd2");

        PromptInputTypes.PromptInputState state = new PromptInputTypes.PromptInputState(
            "hello",
            PromptInputTypes.InputMode.MULTILINE,
            3,
            history,
            0,
            true
        );

        assertEquals("hello", state.text());
        assertEquals(PromptInputTypes.InputMode.MULTILINE, state.mode());
        assertEquals(3, state.cursorPosition());
        assertEquals(2, state.history().size());
        assertEquals(0, state.historyIndex());
        assertTrue(state.isSubmitting());
    }

    @Test
    @DisplayName("PromptInputState withText works correctly")
    void promptInputStateWithTextWorksCorrectly() {
        PromptInputTypes.PromptInputState state = new PromptInputTypes.PromptInputState();

        PromptInputTypes.PromptInputState newState = state.withText("new text");

        assertEquals("new text", newState.text());
        assertEquals(8, newState.cursorPosition()); // cursor at end
        assertEquals("", state.text()); // original unchanged
    }

    @Test
    @DisplayName("PromptInputState withMode works correctly")
    void promptInputStateWithModeWorksCorrectly() {
        PromptInputTypes.PromptInputState state = new PromptInputTypes.PromptInputState();

        PromptInputTypes.PromptInputState newState = state.withMode(PromptInputTypes.InputMode.MULTILINE);

        assertEquals(PromptInputTypes.InputMode.MULTILINE, newState.mode());
        assertEquals(PromptInputTypes.InputMode.NORMAL, state.mode());
    }

    @Test
    @DisplayName("PromptInputState isEmpty works correctly")
    void promptInputStateIsEmptyWorksCorrectly() {
        PromptInputTypes.PromptInputState empty = new PromptInputTypes.PromptInputState();
        PromptInputTypes.PromptInputState whitespace = new PromptInputTypes.PromptInputState("   ", PromptInputTypes.InputMode.NORMAL, 0, List.of(), -1, false);
        PromptInputTypes.PromptInputState hasText = new PromptInputTypes.PromptInputState("text", PromptInputTypes.InputMode.NORMAL, 0, List.of(), -1, false);

        assertTrue(empty.isEmpty());
        assertTrue(whitespace.isEmpty());
        assertFalse(hasText.isEmpty());
    }

    @Test
    @DisplayName("PromptInputState length works correctly")
    void promptInputStateLengthWorksCorrectly() {
        PromptInputTypes.PromptInputState state = new PromptInputTypes.PromptInputState("hello", PromptInputTypes.InputMode.NORMAL, 0, List.of(), -1, false);

        assertEquals(5, state.length());
    }

    @Test
    @DisplayName("InputPasteResult of factory works correctly")
    void inputPasteResultOfFactoryWorksCorrectly() {
        PromptInputTypes.InputPasteResult result = PromptInputTypes.InputPasteResult.of("pasted text");

        assertEquals("pasted text", result.text());
        assertFalse(result.wasTruncated());
        assertEquals(11, result.originalLength());
        assertEquals(11, result.finalLength());
    }

    @Test
    @DisplayName("InputPasteResult truncated factory works correctly")
    void inputPasteResultTruncatedFactoryWorksCorrectly() {
        PromptInputTypes.InputPasteResult result = PromptInputTypes.InputPasteResult.truncated("short", 100);

        assertEquals("short", result.text());
        assertTrue(result.wasTruncated());
        assertEquals(100, result.originalLength());
        assertEquals(5, result.finalLength());
    }

    @Test
    @DisplayName("InputValidationResult valid factory works correctly")
    void inputValidationResultValidFactoryWorksCorrectly() {
        PromptInputTypes.InputValidationResult result = PromptInputTypes.InputValidationResult.valid();

        assertTrue(result.isValid());
        assertNull(result.errorMessage());
        assertNull(result.warningMessage());
    }

    @Test
    @DisplayName("InputValidationResult invalid factory works correctly")
    void inputValidationResultInvalidFactoryWorksCorrectly() {
        PromptInputTypes.InputValidationResult result = PromptInputTypes.InputValidationResult.invalid("Error message");

        assertFalse(result.isValid());
        assertEquals("Error message", result.errorMessage());
        assertNull(result.warningMessage());
    }

    @Test
    @DisplayName("InputValidationResult withWarning factory works correctly")
    void inputValidationResultWithWarningFactoryWorksCorrectly() {
        PromptInputTypes.InputValidationResult result = PromptInputTypes.InputValidationResult.withWarning("Warning message");

        assertTrue(result.isValid());
        assertNull(result.errorMessage());
        assertEquals("Warning message", result.warningMessage());
    }

    @Test
    @DisplayName("PlaceholderConfig getPlaceholder works correctly")
    void placeholderConfigGetPlaceholderWorksCorrectly() {
        PromptInputTypes.PlaceholderConfig config = new PromptInputTypes.PlaceholderConfig(
            "Enter prompt",
            "Enter multiline",
            "Fast mode"
        );

        assertEquals("Enter prompt", config.getPlaceholder(PromptInputTypes.InputMode.NORMAL, false));
        assertEquals("Enter multiline", config.getPlaceholder(PromptInputTypes.InputMode.MULTILINE, false));
        assertEquals("Fast mode", config.getPlaceholder(PromptInputTypes.InputMode.NORMAL, true));
        assertEquals("Fast mode", config.getPlaceholder(PromptInputTypes.InputMode.MULTILINE, true));
    }

    @Test
    @DisplayName("PlaceholderConfig getPlaceholder with null fastModeHint")
    void placeholderConfigGetPlaceholderWithNullFastModeHint() {
        PromptInputTypes.PlaceholderConfig config = new PromptInputTypes.PlaceholderConfig(
            "Enter prompt",
            "Enter multiline",
            null
        );

        assertEquals("Enter prompt", config.getPlaceholder(PromptInputTypes.InputMode.NORMAL, true));
    }
}