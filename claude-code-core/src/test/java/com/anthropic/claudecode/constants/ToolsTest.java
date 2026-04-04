/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Tools constants.
 */
class ToolsTest {

    @Test
    @DisplayName("Tool name constants are correct")
    void toolNameConstants() {
        assertEquals("Bash", Tools.BASH);
        assertEquals("Read", Tools.READ);
        assertEquals("Write", Tools.WRITE);
        assertEquals("Edit", Tools.EDIT);
        assertEquals("Glob", Tools.GLOB);
        assertEquals("Grep", Tools.GREP);
        assertEquals("WebFetch", Tools.WEB_FETCH);
        assertEquals("WebSearch", Tools.WEB_SEARCH);
    }

    @Test
    @DisplayName("isReadOnly identifies read-only tools")
    void isReadOnlyWorks() {
        assertTrue(Tools.isReadOnly(Tools.READ));
        assertTrue(Tools.isReadOnly(Tools.GLOB));
        assertTrue(Tools.isReadOnly(Tools.GREP));
        assertFalse(Tools.isReadOnly(Tools.BASH));
        assertFalse(Tools.isReadOnly(Tools.WRITE));
        assertFalse(Tools.isReadOnly(Tools.EDIT));
    }

    @Test
    @DisplayName("isConcurrencySafe identifies concurrency-safe tools")
    void isConcurrencySafeWorks() {
        assertTrue(Tools.isConcurrencySafe(Tools.READ));
        assertTrue(Tools.isConcurrencySafe(Tools.GLOB));
        assertTrue(Tools.isConcurrencySafe(Tools.GREP));
        assertFalse(Tools.isConcurrencySafe(Tools.BASH));
        assertFalse(Tools.isConcurrencySafe(Tools.WRITE));
    }

    @Test
    @DisplayName("isDestructive identifies destructive tools")
    void isDestructiveWorks() {
        assertTrue(Tools.isDestructive(Tools.BASH));
        assertTrue(Tools.isDestructive(Tools.WRITE));
        assertTrue(Tools.isDestructive(Tools.EDIT));
        assertFalse(Tools.isDestructive(Tools.READ));
        assertFalse(Tools.isDestructive(Tools.GLOB));
    }

    @Test
    @DisplayName("READ_ONLY_TOOLS contains expected tools")
    void readOnlyToolsArray() {
        String[] tools = Tools.READ_ONLY_TOOLS;
        assertEquals(5, tools.length);
        assertArrayEquals(new String[]{Tools.READ, Tools.GLOB, Tools.GREP, Tools.WEB_FETCH, Tools.WEB_SEARCH}, tools);
    }

    @Test
    @DisplayName("DESTRUCTIVE_TOOLS contains expected tools")
    void destructiveToolsArray() {
        String[] tools = Tools.DESTRUCTIVE_TOOLS;
        assertEquals(3, tools.length);
        assertArrayEquals(new String[]{Tools.BASH, Tools.WRITE, Tools.EDIT}, tools);
    }
}