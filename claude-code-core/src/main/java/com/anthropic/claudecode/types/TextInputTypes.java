/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code types/textInputTypes
 */
package com.anthropic.claudecode.types;

import java.time.Instant;

import java.util.*;

/**
 * Text input types - Text input type definitions.
 */
public final class TextInputTypes {

    /**
     * Text input state record.
     */
    public record TextInputState(
        String value,
        int cursorPosition,
        int selectionStart,
        int selectionEnd,
        InputMode mode,
        boolean focused,
        boolean disabled,
        boolean readonly
    ) {
        public static TextInputState empty() {
            return new TextInputState("", 0, 0, 0, InputMode.NORMAL, false, false, false);
        }

        public TextInputState withValue(String newValue) {
            return new TextInputState(
                newValue,
                Math.min(cursorPosition, newValue.length()),
                selectionStart,
                selectionEnd,
                mode,
                focused,
                disabled,
                readonly
            );
        }

        public TextInputState withCursorPosition(int position) {
            int newPos = Math.max(0, Math.min(position, value.length()));
            return new TextInputState(value, newPos, newPos, newPos, mode, focused, disabled, readonly);
        }

        public TextInputState withSelection(int start, int end) {
            return new TextInputState(
                value,
                cursorPosition,
                Math.max(0, Math.min(start, value.length())),
                Math.max(0, Math.min(end, value.length())),
                mode,
                focused,
                disabled,
                readonly
            );
        }

        public boolean hasSelection() {
            return selectionStart != selectionEnd;
        }

        public String getSelectedText() {
            if (!hasSelection()) return "";
            int start = Math.min(selectionStart, selectionEnd);
            int end = Math.max(selectionStart, selectionEnd);
            return value.substring(start, end);
        }

        public TextInputState insertText(String text) {
            if (disabled || readonly) return this;

            int start = Math.min(selectionStart, selectionEnd);
            int end = Math.max(selectionStart, selectionEnd);

            String newValue = value.substring(0, start) + text + value.substring(end);
            int newPos = start + text.length();

            return new TextInputState(newValue, newPos, newPos, newPos, mode, focused, disabled, readonly);
        }

        public TextInputState deleteSelection() {
            if (!hasSelection()) return this;
            return insertText("");
        }
    }

    /**
     * Input mode enum.
     */
    public enum InputMode {
        NORMAL,
        INSERT,
        REPLACE,
        OVERWRITE
    }

    /**
     * Text input config record.
     */
    public record TextInputConfig(
        String placeholder,
        int maxLength,
        int minLength,
        String pattern,
        boolean multiline,
        int rows,
        int cols,
        boolean required,
        boolean autoComplete,
        List<String> suggestions,
        ValidationRule validation
    ) {
        public static TextInputConfig defaults() {
            return new TextInputConfig(
                "",
                Integer.MAX_VALUE,
                0,
                null,
                false,
                1,
                40,
                false,
                false,
                Collections.emptyList(),
                null
            );
        }
    }

    /**
     * Validation rule interface.
     */
    public interface ValidationRule {
        ValidationResult validate(String value);
    }

    /**
     * Validation result record.
     */
    public record ValidationResult(
        boolean valid,
        String errorMessage,
        String warningMessage
    ) {
        public static ValidationResult createValid() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message, null);
        }

        public static ValidationResult warning(String message) {
            return new ValidationResult(true, null, message);
        }
    }

    /**
     * Text selection record.
     */
    public record TextSelection(
        int start,
        int end,
        String text
    ) {
        public static TextSelection empty() {
            return new TextSelection(0, 0, "");
        }

        public boolean isEmpty() {
            return start == end;
        }

        public int getLength() {
            return Math.abs(end - start);
        }
    }

    /**
     * Text range record.
     */
    public record TextRange(
        int startLine,
        int startColumn,
        int endLine,
        int endColumn
    ) {
        public static TextRange of(int line, int column) {
            return new TextRange(line, column, line, column);
        }

        public boolean isSingleLine() {
            return startLine == endLine;
        }

        public boolean isSinglePoint() {
            return startLine == endLine && startColumn == endColumn;
        }
    }

    /**
     * Text edit record.
     */
    public record TextEdit(
        TextRange range,
        String newText,
        EditType type
    ) {
        public static TextEdit insert(int line, int column, String text) {
            return new TextEdit(TextRange.of(line, column), text, EditType.INSERT);
        }

        public static TextEdit delete(TextRange range) {
            return new TextEdit(range, "", EditType.DELETE);
        }

        public static TextEdit replace(TextRange range, String text) {
            return new TextEdit(range, text, EditType.REPLACE);
        }
    }

    /**
     * Edit type enum.
     */
    public enum EditType {
        INSERT,
        DELETE,
        REPLACE
    }

    /**
     * Cursor direction enum.
     */
    public enum CursorDirection {
        LEFT,
        RIGHT,
        UP,
        DOWN,
        HOME,
        END,
        WORD_LEFT,
        WORD_RIGHT,
        LINE_START,
        LINE_END,
        DOCUMENT_START,
        DOCUMENT_END
    }

    /**
     * Text input event record.
     */
    public record TextInputEvent(
        String type,
        String value,
        int cursorPosition,
        TextSelection selection,
        String inputMethod,
        boolean isComposing,
        Instant timestamp
    ) {
        public static TextInputEvent of(String type, String value) {
            return new TextInputEvent(
                type,
                value,
                value.length(),
                TextSelection.empty(),
                "keyboard",
                false,
                Instant.now()
            );
        }
    }

    /**
     * Clipboard data record.
     */
    public record ClipboardData(
        String text,
        String html,
        List<FileReference> files,
        Map<String, String> customData
    ) {
        public static ClipboardData text(String text) {
            return new ClipboardData(text, null, Collections.emptyList(), new HashMap<>());
        }

        public boolean hasText() {
            return text != null && !text.isEmpty();
        }

        public boolean hasFiles() {
            return files != null && !files.isEmpty();
        }
    }

    /**
     * File reference record.
     */
    public record FileReference(
        String path,
        String name,
        String mimeType,
        long size
    ) {}
}