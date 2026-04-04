/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AnsiUtils.
 */
class AnsiUtilsTest {

    @Test
    @DisplayName("AnsiUtils stripAnsi removes escape codes")
    void stripAnsiWorks() {
        String text = "\u001B[31mRed\u001B[0m text";

        String stripped = AnsiUtils.stripAnsi(text);

        assertEquals("Red text", stripped);
    }

    @Test
    @DisplayName("AnsiUtils stripAnsi handles null")
    void stripAnsiNull() {
        assertNull(AnsiUtils.stripAnsi(null));
    }

    @Test
    @DisplayName("AnsiUtils stripAnsi handles no codes")
    void stripAnsiNoCodes() {
        String text = "plain text";

        assertEquals("plain text", AnsiUtils.stripAnsi(text));
    }

    @Test
    @DisplayName("AnsiUtils stripControlCodes removes cursor codes")
    void stripControlCodesCursor() {
        String text = "hello\u001B[Aworld";

        String stripped = AnsiUtils.stripControlCodes(text);

        assertEquals("helloworld", stripped);
    }

    @Test
    @DisplayName("AnsiUtils stripControlCodes handles null")
    void stripControlCodesNull() {
        assertNull(AnsiUtils.stripControlCodes(null));
    }

    @Test
    @DisplayName("AnsiUtils containsAnsi detects codes")
    void containsAnsiWorks() {
        assertTrue(AnsiUtils.containsAnsi("\u001B[31mRed"));
        assertFalse(AnsiUtils.containsAnsi("plain text"));
        assertFalse(AnsiUtils.containsAnsi(null));
    }

    @Test
    @DisplayName("AnsiUtils parseColors parses colored text")
    void parseColorsWorks() {
        String text = "\u001B[31mRed\u001B[0m Normal";

        List<AnsiUtils.ColoredSegment> segments = AnsiUtils.parseColors(text);

        assertFalse(segments.isEmpty());
    }

    @Test
    @DisplayName("AnsiUtils parseColors handles null")
    void parseColorsNull() {
        List<AnsiUtils.ColoredSegment> segments = AnsiUtils.parseColors(null);

        assertTrue(segments.isEmpty());
    }

    @Test
    @DisplayName("AnsiUtils ansiToHtml converts to HTML")
    void ansiToHtmlWorks() {
        String text = "\u001B[31mRed\u001B[0m";

        String html = AnsiUtils.ansiToHtml(text);

        assertTrue(html.contains("color:"));
    }

    @Test
    @DisplayName("AnsiUtils ansiToHtml handles null")
    void ansiToHtmlNull() {
        assertEquals("", AnsiUtils.ansiToHtml(null));
    }

    @Test
    @DisplayName("AnsiUtils ansiToHtml handles plain text")
    void ansiToHtmlPlain() {
        String html = AnsiUtils.ansiToHtml("plain text");

        assertEquals("plain text", html);
    }

    @Test
    @DisplayName("AnsiUtils ansiToHtml escapes HTML chars")
    void ansiToHtmlEscapes() {
        String html = AnsiUtils.ansiToHtml("<script>");

        assertEquals("&lt;script&gt;", html);
    }

    @Test
    @DisplayName("AnsiUtils ColoredSegment record works")
    void coloredSegmentRecord() {
        AnsiUtils.ColoredSegment segment = new AnsiUtils.ColoredSegment("text", "#FF0000", "#000000");

        assertEquals("text", segment.text());
        assertEquals("#FF0000", segment.fgColor());
        assertEquals("#000000", segment.bgColor());
    }

    @Test
    @DisplayName("AnsiUtils parseColors with multiple colors")
    void parseColorsMultiple() {
        String text = "\u001B[31mRed\u001B[32mGreen\u001B[0m";

        List<AnsiUtils.ColoredSegment> segments = AnsiUtils.parseColors(text);

        assertFalse(segments.isEmpty());
    }

    @Test
    @DisplayName("AnsiUtils handles bright colors")
    void brightColors() {
        String text = "\u001B[91mBright Red\u001B[0m";

        String stripped = AnsiUtils.stripAnsi(text);
        List<AnsiUtils.ColoredSegment> segments = AnsiUtils.parseColors(text);

        assertEquals("Bright Red", stripped);
        assertFalse(segments.isEmpty());
    }
}