/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FrontmatterParser.
 */
class FrontmatterParserTest {

    @Test
    @DisplayName("FrontmatterParser ParsedMarkdown record")
    void parsedMarkdownRecord() {
        FrontmatterParser.ParsedMarkdown parsed = new FrontmatterParser.ParsedMarkdown(
            Map.of("title", "Test"), "Content"
        );
        assertEquals("Test", parsed.getString("title"));
        assertEquals("Content", parsed.content());
    }

    @Test
    @DisplayName("FrontmatterParser ParsedMarkdown getString")
    void parsedMarkdownGetString() {
        FrontmatterParser.ParsedMarkdown parsed = new FrontmatterParser.ParsedMarkdown(
            Map.of("key", "value"), ""
        );
        assertEquals("value", parsed.getString("key"));
        assertNull(parsed.getString("nonexistent"));
    }

    @Test
    @DisplayName("FrontmatterParser ParsedMarkdown getList")
    void parsedMarkdownGetList() {
        FrontmatterParser.ParsedMarkdown parsed = new FrontmatterParser.ParsedMarkdown(
            Map.of("items", List.of("a", "b", "c")), ""
        );
        assertEquals(3, parsed.getList("items").size());
    }

    @Test
    @DisplayName("FrontmatterParser ParsedMarkdown getList from string")
    void parsedMarkdownGetListFromString() {
        FrontmatterParser.ParsedMarkdown parsed = new FrontmatterParser.ParsedMarkdown(
            Map.of("items", "a,b,c"), ""
        );
        List<String> list = parsed.getList("items");
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("FrontmatterParser ParsedMarkdown getBoolean true")
    void parsedMarkdownGetBooleanTrue() {
        FrontmatterParser.ParsedMarkdown parsed = new FrontmatterParser.ParsedMarkdown(
            Map.of("flag", "true"), ""
        );
        assertTrue(parsed.getBoolean("flag"));
    }

    @Test
    @DisplayName("FrontmatterParser ParsedMarkdown getBoolean false")
    void parsedMarkdownGetBooleanFalse() {
        FrontmatterParser.ParsedMarkdown parsed = new FrontmatterParser.ParsedMarkdown(
            Map.of("flag", "false"), ""
        );
        assertFalse(parsed.getBoolean("flag"));
    }

    @Test
    @DisplayName("FrontmatterParser parseFrontmatter null input")
    void parseFrontmatterNull() {
        FrontmatterParser.ParsedMarkdown parsed = FrontmatterParser.parseFrontmatter(null);
        assertTrue(parsed.frontmatter().isEmpty());
        assertEquals("", parsed.content());
    }

    @Test
    @DisplayName("FrontmatterParser parseFrontmatter empty input")
    void parseFrontmatterEmpty() {
        FrontmatterParser.ParsedMarkdown parsed = FrontmatterParser.parseFrontmatter("");
        assertTrue(parsed.frontmatter().isEmpty());
        assertEquals("", parsed.content());
    }

    @Test
    @DisplayName("FrontmatterParser parseFrontmatter no frontmatter")
    void parseFrontmatterNoFrontmatter() {
        String markdown = "Hello World\nThis is content.";
        FrontmatterParser.ParsedMarkdown parsed = FrontmatterParser.parseFrontmatter(markdown);
        assertTrue(parsed.frontmatter().isEmpty());
        assertEquals(markdown, parsed.content());
    }

    @Test
    @DisplayName("FrontmatterParser parseFrontmatter with frontmatter")
    void parseFrontmatterWithFrontmatter() {
        String markdown = "---\ntitle: Test\n---\nContent here";
        FrontmatterParser.ParsedMarkdown parsed = FrontmatterParser.parseFrontmatter(markdown);
        assertEquals("Content here", parsed.content());
    }

    @Test
    @DisplayName("FrontmatterParser splitPathInFrontmatter null")
    void splitPathInFrontmatterNull() {
        assertTrue(FrontmatterParser.splitPathInFrontmatter(null).isEmpty());
    }

    @Test
    @DisplayName("FrontmatterParser splitPathInFrontmatter empty")
    void splitPathInFrontmatterEmpty() {
        assertTrue(FrontmatterParser.splitPathInFrontmatter("").isEmpty());
    }

    @Test
    @DisplayName("FrontmatterParser splitPathInFrontmatter single")
    void splitPathInFrontmatterSingle() {
        List<String> result = FrontmatterParser.splitPathInFrontmatter("path/to/file");
        assertEquals(1, result.size());
        assertEquals("path/to/file", result.get(0));
    }

    @Test
    @DisplayName("FrontmatterParser splitPathInFrontmatter comma separated")
    void splitPathInFrontmatterComma() {
        List<String> result = FrontmatterParser.splitPathInFrontmatter("a, b, c");
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("FrontmatterParser splitPathInFrontmatter with braces")
    void splitPathInFrontmatterBraces() {
        List<String> result = FrontmatterParser.splitPathInFrontmatter("src/{main,test}/file");
        assertEquals(2, result.size());
        assertTrue(result.contains("src/main/file"));
        assertTrue(result.contains("src/test/file"));
    }

    @Test
    @DisplayName("FrontmatterParser parsePositiveInt null")
    void parsePositiveIntNull() {
        assertNull(FrontmatterParser.parsePositiveInt(null));
    }

    @Test
    @DisplayName("FrontmatterParser parsePositiveInt valid")
    void parsePositiveIntValid() {
        assertEquals(42, FrontmatterParser.parsePositiveInt(42));
        assertEquals(10, FrontmatterParser.parsePositiveInt("10"));
    }

    @Test
    @DisplayName("FrontmatterParser parsePositiveInt zero")
    void parsePositiveIntZero() {
        assertNull(FrontmatterParser.parsePositiveInt(0));
    }

    @Test
    @DisplayName("FrontmatterParser parsePositiveInt negative")
    void parsePositiveIntNegative() {
        assertNull(FrontmatterParser.parsePositiveInt(-5));
    }

    @Test
    @DisplayName("FrontmatterParser parsePositiveInt invalid string")
    void parsePositiveIntInvalid() {
        assertNull(FrontmatterParser.parsePositiveInt("abc"));
    }

    @Test
    @DisplayName("FrontmatterParser coerceDescriptionToString null")
    void coerceDescriptionToStringNull() {
        assertNull(FrontmatterParser.coerceDescriptionToString(null, "test"));
    }

    @Test
    @DisplayName("FrontmatterParser coerceDescriptionToString string")
    void coerceDescriptionToStringString() {
        assertEquals("test", FrontmatterParser.coerceDescriptionToString("test", "component"));
    }

    @Test
    @DisplayName("FrontmatterParser coerceDescriptionToString number")
    void coerceDescriptionToStringNumber() {
        assertEquals("123", FrontmatterParser.coerceDescriptionToString(123, "component"));
    }

    @Test
    @DisplayName("FrontmatterParser coerceDescriptionToString boolean")
    void coerceDescriptionToStringBoolean() {
        assertEquals("true", FrontmatterParser.coerceDescriptionToString(true, "component"));
    }

    @Test
    @DisplayName("FrontmatterParser coerceDescriptionToString empty string")
    void coerceDescriptionToStringEmpty() {
        assertNull(FrontmatterParser.coerceDescriptionToString("", "component"));
    }

    @Test
    @DisplayName("FrontmatterParser coerceDescriptionToString whitespace")
    void coerceDescriptionToStringWhitespace() {
        assertNull(FrontmatterParser.coerceDescriptionToString("   ", "component"));
    }
}