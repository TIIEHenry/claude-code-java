/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AnsiToSvg.
 */
class AnsiToSvgTest {

    @Test
    @DisplayName("AnsiToSvg AnsiColor record")
    void ansiColorRecord() {
        AnsiToSvg.AnsiColor color = new AnsiToSvg.AnsiColor(100, 150, 200);
        assertEquals(100, color.r());
        assertEquals(150, color.g());
        assertEquals(200, color.b());
    }

    @Test
    @DisplayName("AnsiToSvg TextSpan record")
    void textSpanRecord() {
        AnsiToSvg.AnsiColor color = new AnsiToSvg.AnsiColor(255, 0, 0);
        AnsiToSvg.TextSpan span = new AnsiToSvg.TextSpan("test", color, true);
        assertEquals("test", span.text());
        assertEquals(color, span.color());
        assertTrue(span.bold());
    }

    @Test
    @DisplayName("AnsiToSvg DEFAULT_FG constant")
    void defaultFgConstant() {
        assertEquals(229, AnsiToSvg.DEFAULT_FG.r());
        assertEquals(229, AnsiToSvg.DEFAULT_FG.g());
        assertEquals(229, AnsiToSvg.DEFAULT_FG.b());
    }

    @Test
    @DisplayName("AnsiToSvg DEFAULT_BG constant")
    void defaultBgConstant() {
        assertEquals(30, AnsiToSvg.DEFAULT_BG.r());
        assertEquals(30, AnsiToSvg.DEFAULT_BG.g());
        assertEquals(30, AnsiToSvg.DEFAULT_BG.b());
    }

    @Test
    @DisplayName("AnsiToSvg parseAnsi plain text")
    void parseAnsiPlainText() {
        String text = "Hello World";
        List<List<AnsiToSvg.TextSpan>> lines = AnsiToSvg.parseAnsi(text);

        assertEquals(1, lines.size());
        assertEquals(1, lines.get(0).size());
        assertEquals("Hello World", lines.get(0).get(0).text());
        assertEquals(AnsiToSvg.DEFAULT_FG, lines.get(0).get(0).color());
    }

    @Test
    @DisplayName("AnsiToSvg parseAnsi with colors")
    void parseAnsiWithColors() {
        String text = "\u001b[31mRed\u001b[0mNormal";
        List<List<AnsiToSvg.TextSpan>> lines = AnsiToSvg.parseAnsi(text);

        assertEquals(1, lines.size());
        assertEquals(2, lines.get(0).size());
        assertEquals("Red", lines.get(0).get(0).text());
        assertEquals(205, lines.get(0).get(0).color().r()); // red color
    }

    @Test
    @DisplayName("AnsiToSvg parseAnsi with bold")
    void parseAnsiWithBold() {
        String text = "\u001b[1mBold\u001b[0m";
        List<List<AnsiToSvg.TextSpan>> lines = AnsiToSvg.parseAnsi(text);

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).get(0).bold());
    }

    @Test
    @DisplayName("AnsiToSvg parseAnsi multiple lines")
    void parseAnsiMultipleLines() {
        String text = "Line1\nLine2\nLine3";
        List<List<AnsiToSvg.TextSpan>> lines = AnsiToSvg.parseAnsi(text);

        assertEquals(3, lines.size());
    }

    @Test
    @DisplayName("AnsiToSvg parseAnsi empty string")
    void parseAnsiEmpty() {
        List<List<AnsiToSvg.TextSpan>> lines = AnsiToSvg.parseAnsi("");
        assertEquals(1, lines.size());
        assertEquals("", lines.get(0).get(0).text());
    }

    @Test
    @DisplayName("AnsiToSvg ansiToSvg returns SVG")
    void ansiToSvgBasic() {
        String text = "Hello";
        String svg = AnsiToSvg.ansiToSvg(text);

        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("</svg>"));
        assertTrue(svg.contains("Hello"));
    }

    @Test
    @DisplayName("AnsiToSvg ansiToSvg with colors")
    void ansiToSvgWithColors() {
        String text = "\u001b[32mGreen\u001b[0m";
        String svg = AnsiToSvg.ansiToSvg(text);

        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("Green"));
        assertTrue(svg.contains("rgb(13, 188, 121)")); // green color
    }

    @Test
    @DisplayName("AnsiToSvg AnsiToSvgOptions defaults")
    void optionsDefaults() {
        AnsiToSvg.AnsiToSvgOptions opts = AnsiToSvg.AnsiToSvgOptions.defaults();
        assertEquals("Menlo, Monaco, monospace", opts.fontFamily());
        assertEquals(14, opts.fontSize());
        assertEquals(22, opts.lineHeight());
        assertEquals(24, opts.paddingX());
        assertEquals(24, opts.paddingY());
        assertEquals("rgb(30, 30, 30)", opts.backgroundColor());
        assertEquals(8, opts.borderRadius());
    }

    @Test
    @DisplayName("AnsiToSvg ansiToSvg with options")
    void ansiToSvgWithOptions() {
        AnsiToSvg.AnsiToSvgOptions opts = new AnsiToSvg.AnsiToSvgOptions(
            "Courier", 16, 24, 10, 10, "rgb(0, 0, 0)", 4
        );
        String svg = AnsiToSvg.ansiToSvg("Test", opts);

        assertTrue(svg.contains("font-family: Courier"));
        assertTrue(svg.contains("font-size: 16px"));
        assertTrue(svg.contains("rgb(0, 0, 0)"));
    }

    @Test
    @DisplayName("AnsiToSvg escapeXml")
    void escapeXml() {
        // Test indirectly via ansiToSvg
        String text = "<tag>&attr\"";
        String svg = AnsiToSvg.ansiToSvg(text);

        assertTrue(svg.contains("&lt;"));
        assertTrue(svg.contains("&gt;"));
        assertTrue(svg.contains("&amp;"));
    }

    @Test
    @DisplayName("AnsiToSvg bright colors")
    void brightColors() {
        String text = "\u001b[91mBrightRed\u001b[0m";
        List<List<AnsiToSvg.TextSpan>> lines = AnsiToSvg.parseAnsi(text);

        assertEquals(241, lines.get(0).get(0).color().r()); // bright red
    }
}