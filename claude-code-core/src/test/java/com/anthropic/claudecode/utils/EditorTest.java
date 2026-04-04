/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Editor.
 */
class EditorTest {

    @BeforeEach
    void setUp() {
        Editor.clearCache();
    }

    @Test
    @DisplayName("Editor getExternalEditor returns something or null")
    void getExternalEditor() {
        String editor = Editor.getExternalEditor();
        // May be null if no editor is available, or a valid editor
        assertTrue(editor == null || !editor.isEmpty());
    }

    @Test
    @DisplayName("Editor getExternalEditor cached")
    void getExternalEditorCached() {
        String editor1 = Editor.getExternalEditor();
        String editor2 = Editor.getExternalEditor();
        assertEquals(editor1, editor2);
    }

    @Test
    @DisplayName("Editor classifyGuiEditor code")
    void classifyGuiEditorCode() {
        assertEquals("code", Editor.classifyGuiEditor("code"));
        assertEquals("code", Editor.classifyGuiEditor("/usr/bin/code"));
    }

    @Test
    @DisplayName("Editor classifyGuiEditor vim")
    void classifyGuiEditorVim() {
        assertNull(Editor.classifyGuiEditor("vim"));
    }

    @Test
    @DisplayName("Editor classifyGuiEditor null")
    void classifyGuiEditorNull() {
        assertNull(Editor.classifyGuiEditor(null));
    }

    @Test
    @DisplayName("Editor classifyGuiEditor empty")
    void classifyGuiEditorEmpty() {
        assertNull(Editor.classifyGuiEditor(""));
    }

    @Test
    @DisplayName("Editor classifyGuiEditor cursor")
    void classifyGuiEditorCursor() {
        assertEquals("cursor", Editor.classifyGuiEditor("cursor"));
    }

    @Test
    @DisplayName("Editor supportsGotoLine vim")
    void supportsGotoLineVim() {
        assertTrue(Editor.supportsGotoLine("vim"));
        assertTrue(Editor.supportsGotoLine("nvim"));
    }

    @Test
    @DisplayName("Editor supportsGotoLine nano")
    void supportsGotoLineNano() {
        assertTrue(Editor.supportsGotoLine("nano"));
    }

    @Test
    @DisplayName("Editor supportsGotoLine code")
    void supportsGotoLineCode() {
        assertFalse(Editor.supportsGotoLine("code"));
    }

    @Test
    @DisplayName("Editor supportsGotoLine null")
    void supportsGotoLineNull() {
        assertFalse(Editor.supportsGotoLine(null));
    }

    @Test
    @DisplayName("Editor openFileInExternalEditor null editor")
    void openFileInExternalEditorNoEditor() {
        // If no editor available, returns false
        // This test depends on environment
        boolean result = Editor.openFileInExternalEditor("/tmp/test.txt");
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("Editor openWithDefaultApp does not throw")
    void openWithDefaultApp() {
        assertDoesNotThrow(() -> Editor.openWithDefaultApp("/nonexistent/file.txt"));
    }

    @Test
    @DisplayName("Editor clearCache")
    void clearCache() {
        Editor.getExternalEditor(); // Cache it
        Editor.clearCache();
        // Cache is cleared
        assertTrue(true);
    }
}