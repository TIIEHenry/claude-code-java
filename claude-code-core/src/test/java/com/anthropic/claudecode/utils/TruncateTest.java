/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Truncate.
 */
class TruncateTest {

    @Test
    @DisplayName("Truncate truncatePathMiddle null input")
    void truncatePathMiddleNull() {
        String result = Truncate.truncatePathMiddle(null, 10);
        assertNull(result);
    }

    @Test
    @DisplayName("Truncate truncatePathMiddle empty input")
    void truncatePathMiddleEmpty() {
        String result = Truncate.truncatePathMiddle("", 10);
        assertEquals("", result);
    }

    @Test
    @DisplayName("Truncate truncatePathMiddle short path unchanged")
    void truncatePathMiddleShort() {
        String path = "/short/path.txt";
        String result = Truncate.truncatePathMiddle(path, 50);
        assertEquals(path, result);
    }

    @Test
    @DisplayName("Truncate truncatePathMiddle truncates long path")
    void truncatePathMiddleLong() {
        String path = "/very/long/directory/structure/that/needs/truncating/filename.txt";
        String result = Truncate.truncatePathMiddle(path, 30);

        assertTrue(result.contains("…"));
        assertTrue(result.endsWith("filename.txt"));
        assertTrue(Truncate.stringWidth(result) <= 30);
    }

    @Test
    @DisplayName("Truncate truncatePathMiddle maxLength zero")
    void truncatePathMiddleZero() {
        String result = Truncate.truncatePathMiddle("/path/to/file.txt", 0);
        assertEquals("…", result);
    }

    @Test
    @DisplayName("Truncate truncatePathMiddle maxLength negative")
    void truncatePathMiddleNegative() {
        String result = Truncate.truncatePathMiddle("/path/to/file.txt", -5);
        assertEquals("…", result);
    }

    @Test
    @DisplayName("Truncate truncatePathMiddle very small maxLength")
    void truncatePathMiddleSmall() {
        String result = Truncate.truncatePathMiddle("/path/to/file.txt", 3);
        assertTrue(Truncate.stringWidth(result) <= 3);
    }

    @Test
    @DisplayName("Truncate truncatePathMiddle preserves filename")
    void truncatePathMiddlePreservesFilename() {
        String path = "/some/dir/file.txt";
        String result = Truncate.truncatePathMiddle(path, 20);
        assertTrue(result.contains("file.txt"));
    }

    @Test
    @DisplayName("Truncate truncateToWidth null input")
    void truncateToWidthNull() {
        String result = Truncate.truncateToWidth(null, 10);
        assertNull(result);
    }

    @Test
    @DisplayName("Truncate truncateToWidth empty input")
    void truncateToWidthEmpty() {
        String result = Truncate.truncateToWidth("", 10);
        assertEquals("", result);
    }

    @Test
    @DisplayName("Truncate truncateToWidth short string unchanged")
    void truncateToWidthShort() {
        String result = Truncate.truncateToWidth("hello", 10);
        assertEquals("hello", result);
    }

    @Test
    @DisplayName("Truncate truncateToWidth truncates long string")
    void truncateToWidthLong() {
        String result = Truncate.truncateToWidth("hello world this is long", 10);
        assertTrue(result.endsWith("…"));
        assertTrue(Truncate.stringWidth(result) <= 10);
    }

    @Test
    @DisplayName("Truncate truncateToWidth maxLength one")
    void truncateToWidthOne() {
        String result = Truncate.truncateToWidth("hello", 1);
        assertEquals("…", result);
    }

    @Test
    @DisplayName("Truncate truncateToWidth maxLength zero")
    void truncateToWidthZero() {
        String result = Truncate.truncateToWidth("hello", 0);
        assertEquals("…", result);
    }

    @Test
    @DisplayName("Truncate truncateToWidth with unicode")
    void truncateToWidthUnicode() {
        String result = Truncate.truncateToWidth("你好世界hello", 8);
        assertTrue(result.endsWith("…"));
        assertTrue(Truncate.stringWidth(result) <= 8);
    }

    @Test
    @DisplayName("Truncate truncateStartToWidth null input")
    void truncateStartToWidthNull() {
        String result = Truncate.truncateStartToWidth(null, 10);
        assertNull(result);
    }

    @Test
    @DisplayName("Truncate truncateStartToWidth empty input")
    void truncateStartToWidthEmpty() {
        String result = Truncate.truncateStartToWidth("", 10);
        assertEquals("", result);
    }

    @Test
    @DisplayName("Truncate truncateStartToWidth short string unchanged")
    void truncateStartToWidthShort() {
        String result = Truncate.truncateStartToWidth("hello", 10);
        assertEquals("hello", result);
    }

    @Test
    @DisplayName("Truncate truncateStartToWidth truncates from start")
    void truncateStartToWidthLong() {
        String result = Truncate.truncateStartToWidth("hello world", 8);
        assertTrue(result.startsWith("…"));
        assertTrue(result.endsWith("world") || result.contains("ld"));
        assertTrue(Truncate.stringWidth(result) <= 8);
    }

    @Test
    @DisplayName("Truncate truncateStartToWidth maxLength one")
    void truncateStartToWidthOne() {
        String result = Truncate.truncateStartToWidth("hello", 1);
        assertEquals("…", result);
    }

    @Test
    @DisplayName("Truncate truncateToWidthNoEllipsis null input")
    void truncateToWidthNoEllipsisNull() {
        String result = Truncate.truncateToWidthNoEllipsis(null, 10);
        assertNull(result);
    }

    @Test
    @DisplayName("Truncate truncateToWidthNoEllipsis empty input")
    void truncateToWidthNoEllipsisEmpty() {
        String result = Truncate.truncateToWidthNoEllipsis("", 10);
        assertEquals("", result);
    }

    @Test
    @DisplayName("Truncate truncateToWidthNoEllipsis short string unchanged")
    void truncateToWidthNoEllipsisShort() {
        String result = Truncate.truncateToWidthNoEllipsis("hello", 10);
        assertEquals("hello", result);
    }

    @Test
    @DisplayName("Truncate truncateToWidthNoEllipsis truncates without ellipsis")
    void truncateToWidthNoEllipsisLong() {
        String result = Truncate.truncateToWidthNoEllipsis("hello world", 5);
        assertFalse(result.contains("…"));
        assertTrue(result.length() <= 5);
    }

    @Test
    @DisplayName("Truncate truncateToWidthNoEllipsis maxLength zero")
    void truncateToWidthNoEllipsisZero() {
        String result = Truncate.truncateToWidthNoEllipsis("hello", 0);
        assertEquals("", result);
    }

    @Test
    @DisplayName("Truncate truncate null input")
    void truncateNull() {
        String result = Truncate.truncate(null, 10, false);
        assertNull(result);
    }

    @Test
    @DisplayName("Truncate truncate empty input")
    void truncateEmpty() {
        String result = Truncate.truncate("", 10, false);
        assertEquals("", result);
    }

    @Test
    @DisplayName("Truncate truncate short string unchanged")
    void truncateShort() {
        String result = Truncate.truncate("hello", 10, false);
        assertEquals("hello", result);
    }

    @Test
    @DisplayName("Truncate truncate long string")
    void truncateLong() {
        String result = Truncate.truncate("hello world this is long", 10, false);
        assertTrue(result.endsWith("…"));
    }

    @Test
    @DisplayName("Truncate truncate singleLine cuts at newline")
    void truncateSingleLine() {
        String result = Truncate.truncate("first line\nsecond line", 20, true);
        assertTrue(result.contains("first line"));
        assertFalse(result.contains("second"));
    }

    @Test
    @DisplayName("Truncate truncate singleLine short line")
    void truncateSingleLineShort() {
        String result = Truncate.truncate("short\nline", 20, true);
        assertTrue(result.startsWith("short"));
        assertTrue(result.endsWith("…"));
    }

    @Test
    @DisplayName("Truncate truncate singleLine no newline")
    void truncateSingleLineNoNewline() {
        String result = Truncate.truncate("no newline here", 20, true);
        assertEquals("no newline here", result);
    }

    @Test
    @DisplayName("Truncate stringWidth null input")
    void stringWidthNull() {
        int result = Truncate.stringWidth(null);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Truncate stringWidth empty input")
    void stringWidthEmpty() {
        int result = Truncate.stringWidth("");
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Truncate stringWidth ascii characters")
    void stringWidthAscii() {
        int result = Truncate.stringWidth("hello");
        assertEquals(5, result);
    }

    @Test
    @DisplayName("Truncate stringWidth wide characters")
    void stringWidthWide() {
        int result = Truncate.stringWidth("你好");
        assertEquals(4, result); // Each Chinese char is width 2
    }

    @Test
    @DisplayName("Truncate stringWidth mixed characters")
    void stringWidthMixed() {
        int result = Truncate.stringWidth("hello你好");
        assertEquals(9, result); // 5 + 4
    }

    @Test
    @DisplayName("Truncate stringWidth control characters")
    void stringWidthControl() {
        int result = Truncate.stringWidth("hello\tworld");
        assertEquals(10, result); // Tab has width 0
    }

    @Test
    @DisplayName("Truncate wrapText null input")
    void wrapTextNull() {
        String[] result = Truncate.wrapText(null, 10);
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("Truncate wrapText empty input")
    void wrapTextEmpty() {
        String[] result = Truncate.wrapText("", 10);
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("Truncate wrapText short string single line")
    void wrapTextShort() {
        String[] result = Truncate.wrapText("hello", 10);
        assertEquals(1, result.length);
        assertEquals("hello", result[0]);
    }

    @Test
    @DisplayName("Truncate wrapText wraps long string")
    void wrapTextLong() {
        String[] result = Truncate.wrapText("hello world this is a test", 10);
        assertTrue(result.length > 1);
    }

    @Test
    @DisplayName("Truncate wrapText exact width")
    void wrapTextExactWidth() {
        String[] result = Truncate.wrapText("hello", 5);
        assertEquals(1, result.length);
        assertEquals("hello", result[0]);
    }

    @Test
    @DisplayName("Truncate wrapText with wide characters")
    void wrapTextWide() {
        String[] result = Truncate.wrapText("你好世界hello", 6);
        assertTrue(result.length > 1);
    }

    @Test
    @DisplayName("Truncate wrapText very small width")
    void wrapTextSmallWidth() {
        String[] result = Truncate.wrapText("hello", 1);
        assertEquals(5, result.length);
    }
}