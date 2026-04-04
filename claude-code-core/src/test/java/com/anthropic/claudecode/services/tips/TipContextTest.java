/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.tips;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TipContext.
 */
class TipContextTest {

    @Test
    @DisplayName("TipContext record with all fields")
    void tipContextRecord() {
        Set<String> bashTools = Set.of("Bash", "Read", "Edit");
        TipContext context = new TipContext("dark", bashTools, "state");

        assertEquals("dark", context.theme());
        assertEquals(bashTools, context.bashTools());
        assertEquals("state", context.readFileState());
    }

    @Test
    @DisplayName("TipContext default constructor creates empty context")
    void tipContextDefault() {
        TipContext context = new TipContext();

        assertNull(context.theme());
        assertNull(context.bashTools());
        assertNull(context.readFileState());
    }

    @Test
    @DisplayName("TipContext with null values")
    void tipContextWithNulls() {
        TipContext context = new TipContext(null, null, null);

        assertNull(context.theme());
        assertNull(context.bashTools());
        assertNull(context.readFileState());
    }

    @Test
    @DisplayName("TipContext with empty bash tools set")
    void tipContextEmptyBashTools() {
        TipContext context = new TipContext("light", Set.of(), null);

        assertEquals("light", context.theme());
        assertTrue(context.bashTools().isEmpty());
        assertNull(context.readFileState());
    }

    @Test
    @DisplayName("TipContext with various theme values")
    void tipContextThemes() {
        TipContext dark = new TipContext("dark", null, null);
        TipContext light = new TipContext("light", null, null);
        TipContext custom = new TipContext("custom-theme", null, null);

        assertEquals("dark", dark.theme());
        assertEquals("light", light.theme());
        assertEquals("custom-theme", custom.theme());
    }

    @Test
    @DisplayName("TipContext readFileState can be any object")
    void tipContextReadFileState() {
        TipContext withString = new TipContext(null, null, "some-state");
        TipContext withObject = new TipContext(null, null, new Object());

        assertEquals("some-state", withString.readFileState());
        assertNotNull(withObject.readFileState());
    }
}