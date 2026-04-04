/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/PromptInput
 */
package com.anthropic.claudecode.components.promptinput;

import java.util.*;

/**
 * Prompt input types - Types for prompt input component.
 */
public final class PromptInputTypes {

    /**
     * Input mode enum.
     */
    public enum InputMode {
        NORMAL,
        MULTILINE,
        FILE,
        EDITOR
    }

    /**
     * Prompt input state.
     */
    public record PromptInputState(
        String text,
        InputMode mode,
        int cursorPosition,
        List<String> history,
        int historyIndex,
        boolean isSubmitting
    ) {
        public PromptInputState() {
            this("", InputMode.NORMAL, 0, new ArrayList<>(), -1, false);
        }

        public PromptInputState withText(String newText) {
            return new PromptInputState(newText, mode, newText.length(), history, historyIndex, isSubmitting);
        }

        public PromptInputState withMode(InputMode newMode) {
            return new PromptInputState(text, newMode, cursorPosition, history, historyIndex, isSubmitting);
        }

        public boolean isEmpty() {
            return text == null || text.trim().isEmpty();
        }

        public int length() {
            return text != null ? text.length() : 0;
        }
    }

    /**
     * Input paste result.
     */
    public record InputPasteResult(
        String text,
        boolean wasTruncated,
        int originalLength,
        int finalLength
    ) {
        public static InputPasteResult of(String text) {
            return new InputPasteResult(text, false, text.length(), text.length());
        }

        public static InputPasteResult truncated(String text, int originalLength) {
            return new InputPasteResult(text, true, originalLength, text.length());
        }
    }

    /**
     * Input validation result.
     */
    public record InputValidationResult(
        boolean isValid,
        String errorMessage,
        String warningMessage
    ) {
        public static InputValidationResult valid() {
            return new InputValidationResult(true, null, null);
        }

        public static InputValidationResult invalid(String error) {
            return new InputValidationResult(false, error, null);
        }

        public static InputValidationResult withWarning(String warning) {
            return new InputValidationResult(true, null, warning);
        }
    }

    /**
     * Placeholder config.
     */
    public record PlaceholderConfig(
        String defaultPlaceholder,
        String multilinePlaceholder,
        String fastModeHint
    ) {
        public String getPlaceholder(InputMode mode, boolean isFastMode) {
            if (isFastMode && fastModeHint != null) {
                return fastModeHint;
            }

            return mode == InputMode.MULTILINE ? multilinePlaceholder : defaultPlaceholder;
        }
    }
}