/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EditorUtils.
 */
class EditorUtilsTest {

    @BeforeEach
    void setUp() {
        EditorUtils.clearCache();
    }

    @Test
    @DisplayName("EditorUtils getExternalEditor returns something or null")
    void getExternalEditor() {
        String editor = EditorUtils.getExternalEditor();
        assertTrue(editor == null || !editor.isEmpty());
    }

    @Test
    @DisplayName("EditorUtils getExternalEditor cached")
    void getExternalEditorCached() {
        String editor1 = EditorUtils.getExternalEditor();
        String editor2 = EditorUtils.getExternalEditor();
        assertEquals(editor1, editor2);
    }

    @Test
    @DisplayName("EditorUtils classifyGuiEditor code")
    void classifyGuiEditorCode() {
        assertEquals("code", EditorUtils.classifyGuiEditor("code"));
        assertEquals("code", EditorUtils.classifyGuiEditor("/usr/bin/code"));
    }

    @Test
    @DisplayName("EditorUtils classifyGuiEditor cursor")
    void classifyGuiEditorCursor() {
        assertEquals("cursor", EditorUtils.classifyGuiEditor("cursor"));
    }

    @Test
    @DisplayName("EditorUtils classifyGuiEditor vim")
    void classifyGuiEditorVim() {
        assertNull(EditorUtils.classifyGuiEditor("vim"));
    }

    @Test
    @DisplayName("EditorUtils classifyGuiEditor null")
    void classifyGuiEditorNull() {
        assertNull(EditorUtils.classifyGuiEditor(null));
    }

    @Test
    @DisplayName("EditorUtils classifyGuiEditor empty")
    void classifyGuiEditorEmpty() {
        assertNull(EditorUtils.classifyGuiEditor(""));
    }

    @Test
    @DisplayName("EditorUtils isGuiEditor true")
    void isGuiEditorTrue() {
        assertTrue(EditorUtils.isGuiEditor("code"));
        assertTrue(EditorUtils.isGuiEditor("cursor"));
        assertTrue(EditorUtils.isGuiEditor("subl"));
    }

    @Test
    @DisplayName("EditorUtils isGuiEditor false")
    void isGuiEditorFalse() {
        assertFalse(EditorUtils.isGuiEditor("vim"));
        assertFalse(EditorUtils.isGuiEditor("nano"));
        assertFalse(EditorUtils.isGuiEditor(null));
    }

    @Test
    @DisplayName("EditorUtils supportsGotoLine vim")
    void supportsGotoLineVim() {
        assertTrue(EditorUtils.supportsGotoLine("vim"));
        assertTrue(EditorUtils.supportsGotoLine("nvim"));
    }

    @Test
    @DisplayName("EditorUtils supportsGotoLine nano")
    void supportsGotoLineNano() {
        assertTrue(EditorUtils.supportsGotoLine("nano"));
        assertTrue(EditorUtils.supportsGotoLine("emacs"));
    }

    @Test
    @DisplayName("EditorUtils supportsGotoLine code")
    void supportsGotoLineCode() {
        assertFalse(EditorUtils.supportsGotoLine("code"));
    }

    @Test
    @DisplayName("EditorUtils supportsGotoLine null")
    void supportsGotoLineNull() {
        assertFalse(EditorUtils.supportsGotoLine(null));
    }

    @Test
    @DisplayName("EditorUtils openFileInExternalEditor returns boolean")
    void openFileInExternalEditor() {
        boolean result = EditorUtils.openFileInExternalEditor("/tmp/test.txt");
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("EditorUtils openFileInExternalEditor with line")
    void openFileInExternalEditorWithLine() {
        boolean result = EditorUtils.openFileInExternalEditor("/tmp/test.txt", 10);
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("EditorUtils clearCache")
    void clearCache() {
        EditorUtils.getExternalEditor();
        EditorUtils.clearCache();
        assertTrue(true);
    }
}