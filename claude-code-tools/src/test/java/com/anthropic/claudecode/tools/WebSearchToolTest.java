/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.tools;

import com.anthropic.claudecode.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WebSearchTool.
 */
class WebSearchToolTest {

    @Test
    @DisplayName("WebSearchTool has correct name")
    void nameWorks() {
        WebSearchTool tool = new WebSearchTool();
        assertEquals("WebSearch", tool.name());
    }

    @Test
    @DisplayName("WebSearchTool input schema is valid")
    void inputSchemaWorks() {
        WebSearchTool tool = new WebSearchTool();
        var schema = tool.inputSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));
    }

    @Test
    @DisplayName("WebSearchTool is read-only")
    void isReadOnlyWorks() {
        WebSearchTool tool = new WebSearchTool();
        WebSearchTool.Input input = new WebSearchTool.Input("test query", null, null);
        assertTrue(tool.isReadOnly(input));
    }

    @Test
    @DisplayName("WebSearchTool is concurrency-safe")
    void isConcurrencySafeWorks() {
        WebSearchTool tool = new WebSearchTool();
        WebSearchTool.Input input = new WebSearchTool.Input("test query", null, null);
        assertTrue(tool.isConcurrencySafe(input));
    }

    @Test
    @DisplayName("WebSearchTool matches name correctly")
    void matchesNameWorks() {
        WebSearchTool tool = new WebSearchTool();
        assertTrue(tool.matchesName("WebSearch"));
        assertFalse(tool.matchesName("WebFetch"));
    }

    @Test
    @DisplayName("WebSearchTool Input record works")
    void inputRecordWorks() {
        WebSearchTool.Input input = new WebSearchTool.Input(
            "Java 21 features",
            null,
            null
        );

        assertEquals("Java 21 features", input.query());
    }

    @Test
    @DisplayName("WebSearchTool isOpenWorld returns true")
    void isOpenWorldWorks() {
        WebSearchTool tool = new WebSearchTool();
        WebSearchTool.Input input = new WebSearchTool.Input("test", null, null);
        assertTrue(tool.isOpenWorld(input));
    }
}