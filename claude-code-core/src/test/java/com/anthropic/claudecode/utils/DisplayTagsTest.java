/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DisplayTags.
 */
class DisplayTagsTest {

    @Test
    @DisplayName("DisplayTags stripDisplayTags null input")
    void stripDisplayTagsNull() {
        assertNull(DisplayTags.stripDisplayTags(null));
    }

    @Test
    @DisplayName("DisplayTags stripDisplayTags empty input")
    void stripDisplayTagsEmpty() {
        assertEquals("", DisplayTags.stripDisplayTags(""));
    }

    @Test
    @DisplayName("DisplayTags stripDisplayTags plain text")
    void stripDisplayTagsPlainText() {
        assertEquals("Hello World", DisplayTags.stripDisplayTags("Hello World"));
    }

    @Test
    @DisplayName("DisplayTags stripDisplayTags single tag")
    void stripDisplayTagsSingleTag() {
        String input = "<tag>content</tag>Hello";
        assertEquals("Hello", DisplayTags.stripDisplayTags(input));
    }

    @Test
    @DisplayName("DisplayTags stripDisplayTags multiple tags")
    void stripDisplayTagsMultipleTags() {
        String input = "<tag1>a</tag1><tag2>b</tag2>Text";
        assertEquals("Text", DisplayTags.stripDisplayTags(input));
    }

    @Test
    @DisplayName("DisplayTags stripDisplayTags tag with attributes")
    void stripDisplayTagsTagWithAttributes() {
        String input = "<tag attr=\"value\">content</tag>Remaining";
        assertEquals("Remaining", DisplayTags.stripDisplayTags(input));
    }

    @Test
    @DisplayName("DisplayTags stripDisplayTags multiline tag")
    void stripDisplayTagsMultilineTag() {
        String input = "<tag>\nline1\nline2\n</tag>After";
        assertEquals("After", DisplayTags.stripDisplayTags(input));
    }

    @Test
    @DisplayName("DisplayTags stripDisplayTags only tags returns original")
    void stripDisplayTagsOnlyTags() {
        String input = "<tag>content</tag>";
        assertEquals(input, DisplayTags.stripDisplayTags(input));
    }

    @Test
    @DisplayName("DisplayTags stripDisplayTagsAllowEmpty null input")
    void stripDisplayTagsAllowEmptyNull() {
        assertEquals("", DisplayTags.stripDisplayTagsAllowEmpty(null));
    }

    @Test
    @DisplayName("DisplayTags stripDisplayTagsAllowEmpty empty input")
    void stripDisplayTagsAllowEmptyEmpty() {
        assertEquals("", DisplayTags.stripDisplayTagsAllowEmpty(""));
    }

    @Test
    @DisplayName("DisplayTags stripDisplayTagsAllowEmpty only tags")
    void stripDisplayTagsAllowEmptyOnlyTags() {
        String input = "<tag>content</tag>";
        assertEquals("", DisplayTags.stripDisplayTagsAllowEmpty(input));
    }

    @Test
    @DisplayName("DisplayTags stripDisplayTagsAllowEmpty mixed content")
    void stripDisplayTagsAllowEmptyMixed() {
        String input = "<tag>content</tag>Text";
        assertEquals("Text", DisplayTags.stripDisplayTagsAllowEmpty(input));
    }

    @Test
    @DisplayName("DisplayTags stripIdeContextTags null input")
    void stripIdeContextTagsNull() {
        assertNull(DisplayTags.stripIdeContextTags(null));
    }

    @Test
    @DisplayName("DisplayTags stripIdeContextTags empty input")
    void stripIdeContextTagsEmpty() {
        assertEquals("", DisplayTags.stripIdeContextTags(""));
    }

    @Test
    @DisplayName("DisplayTags stripIdeContextTags ide_opened_file")
    void stripIdeContextTagsOpenedFile() {
        String input = "<ide_opened_file>file.txt</ide_opened_file>User text";
        assertEquals("User text", DisplayTags.stripIdeContextTags(input));
    }

    @Test
    @DisplayName("DisplayTags stripIdeContextTags ide_selection")
    void stripIdeContextTagsSelection() {
        String input = "<ide_selection>selected</ide_selection>Query";
        assertEquals("Query", DisplayTags.stripIdeContextTags(input));
    }

    @Test
    @DisplayName("DisplayTags stripIdeContextTags preserves other tags")
    void stripIdeContextTagsPreservesOtherTags() {
        String input = "<code>example</code>Text";
        assertEquals("<code>example</code>Text", DisplayTags.stripIdeContextTags(input));
    }

    @Test
    @DisplayName("DisplayTags stripIdeContextTags with attributes")
    void stripIdeContextTagsWithAttributes() {
        String input = "<ide_opened_file path=\"/tmp\">content</ide_opened_file>After";
        assertEquals("After", DisplayTags.stripIdeContextTags(input));
    }

    @Test
    @DisplayName("DisplayTags stripDisplayTags uppercase tags preserved")
    void stripDisplayTagsUppercaseTags() {
        String input = "<TAG>content</TAG>Text";
        // Uppercase tags shouldn't be stripped (pattern only matches lowercase)
        assertTrue(DisplayTags.stripDisplayTags(input).contains("TAG"));
    }
}