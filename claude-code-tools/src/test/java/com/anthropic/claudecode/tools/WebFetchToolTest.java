/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.tools;

import com.anthropic.claudecode.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WebFetchTool.
 */
class WebFetchToolTest {

    @Test
    @DisplayName("WebFetchTool has correct name")
    void nameWorks() {
        WebFetchTool tool = new WebFetchTool();
        assertEquals("WebFetch", tool.name());
    }

    @Test
    @DisplayName("WebFetchTool input schema is valid")
    void inputSchemaWorks() {
        WebFetchTool tool = new WebFetchTool();
        var schema = tool.inputSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));
    }

    @Test
    @DisplayName("WebFetchTool is read-only")
    void isReadOnlyWorks() {
        WebFetchTool tool = new WebFetchTool();
        WebFetchTool.Input input = new WebFetchTool.Input("https://example.com", "Extract content");
        assertTrue(tool.isReadOnly(input));
    }

    @Test
    @DisplayName("WebFetchTool is concurrency-safe")
    void isConcurrencySafeWorks() {
        WebFetchTool tool = new WebFetchTool();
        WebFetchTool.Input input = new WebFetchTool.Input("https://example.com", null);
        assertTrue(tool.isConcurrencySafe(input));
    }

    @Test
    @DisplayName("WebFetchTool matches name correctly")
    void matchesNameWorks() {
        WebFetchTool tool = new WebFetchTool();
        assertTrue(tool.matchesName("WebFetch"));
        assertFalse(tool.matchesName("WebSearch"));
    }

    @Test
    @DisplayName("WebFetchTool Input record works")
    void inputRecordWorks() {
        WebFetchTool.Input input = new WebFetchTool.Input(
            "https://example.com/page",
            "Extract the main content"
        );

        assertEquals("https://example.com/page", input.url());
        assertEquals("Extract the main content", input.prompt());
    }

    @Test
    @DisplayName("WebFetchTool isOpenWorld returns true")
    void isOpenWorldWorks() {
        WebFetchTool tool = new WebFetchTool();
        WebFetchTool.Input input = new WebFetchTool.Input("https://example.com", null);
        assertTrue(tool.isOpenWorld(input));
    }
}