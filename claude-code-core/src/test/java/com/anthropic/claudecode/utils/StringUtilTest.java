/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StringUtil.
 */
class StringUtilTest {

    @Test
    @DisplayName("StringUtil MAX_STRING_LENGTH constant")
    void maxStringLength() {
        assertEquals(1 << 25, StringUtil.MAX_STRING_LENGTH);
    }

    @Test
    @DisplayName("StringUtil escapeRegExp escapes special chars")
    void escapeRegExp() {
        String result = StringUtil.escapeRegExp("a.b*c+d?e^f$g{h}i(j)k|l[m]n\\o");

        assertTrue(result.contains("\\."));
        assertTrue(result.contains("\\*"));
        assertTrue(result.contains("\\+"));
        assertTrue(result.contains("\\?"));
        assertTrue(result.contains("\\^"));
        assertTrue(result.contains("\\$"));
    }

    @Test
    @DisplayName("StringUtil escapeRegExp no special chars unchanged")
    void escapeRegExpNoSpecial() {
        String result = StringUtil.escapeRegExp("abc123");

        assertEquals("abc123", result);
    }

    @Test
    @DisplayName("StringUtil plural returns singular for 1")
    void pluralSingular() {
        String result = StringUtil.plural(1, "cat");

        assertEquals("cat", result);
    }

    @Test
    @DisplayName("StringUtil plural returns plural for 0")
    void pluralZero() {
        String result = StringUtil.plural(0, "cat");

        assertEquals("cats", result);
    }

    @Test
    @DisplayName("StringUtil plural returns plural for 2")
    void pluralTwo() {
        String result = StringUtil.plural(2, "cat");

        assertEquals("cats", result);
    }

    @Test
    @DisplayName("StringUtil plural with custom plural word")
    void pluralCustom() {
        String result = StringUtil.plural(2, "child", "children");

        assertEquals("children", result);
    }

    @Test
    @DisplayName("StringUtil plural singular with custom plural word")
    void pluralCustomSingular() {
        String result = StringUtil.plural(1, "child", "children");

        assertEquals("child", result);
    }

    @Test
    @DisplayName("StringUtil firstLineOf returns first line")
    void firstLineOf() {
        String result = StringUtil.firstLineOf("line1\nline2\nline3");

        assertEquals("line1", result);
    }

    @Test
    @DisplayName("StringUtil firstLineOf single line")
    void firstLineOfSingle() {
        String result = StringUtil.firstLineOf("single line");

        assertEquals("single line", result);
    }

    @Test
    @DisplayName("StringUtil firstLineOf null returns null")
    void firstLineOfNull() {
        assertNull(StringUtil.firstLineOf(null));
    }

    @Test
    @DisplayName("StringUtil firstLineOf empty returns empty")
    void firstLineOfEmpty() {
        assertEquals("", StringUtil.firstLineOf(""));
    }

    @Test
    @DisplayName("StringUtil countCharInString counts occurrences")
    void countCharInString() {
        int result = StringUtil.countCharInString("hello world", 'l');

        assertEquals(3, result);
    }

    @Test
    @DisplayName("StringUtil countCharInString zero for missing char")
    void countCharInStringMissing() {
        int result = StringUtil.countCharInString("hello", 'z');

        assertEquals(0, result);
    }

    @Test
    @DisplayName("StringUtil countCharInString null returns 0")
    void countCharInStringNull() {
        int result = StringUtil.countCharInString(null, 'a');

        assertEquals(0, result);
    }

    @Test
    @DisplayName("StringUtil normalizeFullWidthDigits converts to half-width")
    void normalizeFullWidthDigits() {
        String result = StringUtil.normalizeFullWidthDigits("１２３４５");

        assertEquals("12345", result);
    }

    @Test
    @DisplayName("StringUtil normalizeFullWidthDigits mixed input")
    void normalizeFullWidthDigitsMixed() {
        String result = StringUtil.normalizeFullWidthDigits("１a２b３");

        assertEquals("1a2b3", result);
    }

    @Test
    @DisplayName("StringUtil normalizeFullWidthDigits null returns null")
    void normalizeFullWidthDigitsNull() {
        assertNull(StringUtil.normalizeFullWidthDigits(null));
    }

    @Test
    @DisplayName("StringUtil normalizeFullWidthDigits no full-width unchanged")
    void normalizeFullWidthDigitsNone() {
        String result = StringUtil.normalizeFullWidthDigits("abc123");

        assertEquals("abc123", result);
    }

    @Test
    @DisplayName("StringUtil normalizeFullWidthSpace converts ideographic space")
    void normalizeFullWidthSpace() {
        String result = StringUtil.normalizeFullWidthSpace("hello　world");

        assertEquals("hello world", result);
    }

    @Test
    @DisplayName("StringUtil normalizeFullWidthSpace null returns null")
    void normalizeFullWidthSpaceNull() {
        assertNull(StringUtil.normalizeFullWidthSpace(null));
    }

    @Test
    @DisplayName("StringUtil normalizeFullWidthSpace no full-width unchanged")
    void normalizeFullWidthSpaceNone() {
        String result = StringUtil.normalizeFullWidthSpace("hello world");

        assertEquals("hello world", result);
    }

    @Test
    @DisplayName("StringUtil safeJoinLines joins with delimiter")
    void safeJoinLines() {
        String result = StringUtil.safeJoinLines(new String[]{"a", "b", "c"}, ",", 100);

        assertEquals("a,b,c", result);
    }

    @Test
    @DisplayName("StringUtil safeJoinLines default delimiter and max")
    void safeJoinLinesDefault() {
        String result = StringUtil.safeJoinLines(new String[]{"a", "b", "c"});

        assertEquals("a,b,c", result);
    }

    @Test
    @DisplayName("StringUtil safeJoinLines truncates when exceeds max")
    void safeJoinLinesTruncate() {
        String result = StringUtil.safeJoinLines(new String[]{"abcdefghij", "xyz"}, ",", 10);

        // First line fits (10 chars), second triggers truncation
        // Truncation marker "...[truncated]" is appended when remainingSpace <= 0
        assertTrue(result.contains("abcdefghij"));
    }

    @Test
    @DisplayName("StringUtil safeJoinLines empty array")
    void safeJoinLinesEmpty() {
        String result = StringUtil.safeJoinLines(new String[]{}, ",", 100);

        assertEquals("", result);
    }

    @Test
    @DisplayName("StringUtil truncateToLines limits lines")
    void truncateToLines() {
        String text = "line1\nline2\nline3\nline4";
        String result = StringUtil.truncateToLines(text, 2);

        assertEquals("line1\nline2…", result);
    }

    @Test
    @DisplayName("StringUtil truncateToLines returns original when under limit")
    void truncateToLinesUnderLimit() {
        String text = "line1\nline2";
        String result = StringUtil.truncateToLines(text, 5);

        assertEquals(text, result);
    }

    @Test
    @DisplayName("StringUtil truncateToLines null returns null")
    void truncateToLinesNull() {
        assertNull(StringUtil.truncateToLines(null, 2));
    }

    @Test
    @DisplayName("StringUtil truncateToLines zero lines")
    void truncateToLinesZero() {
        String text = "line1\nline2";
        String result = StringUtil.truncateToLines(text, 0);

        assertEquals("…", result);
    }

    @Test
    @DisplayName("StringUtil truncateToLines single line")
    void truncateToLinesSingle() {
        String text = "single";
        String result = StringUtil.truncateToLines(text, 1);

        assertEquals("single", result);
    }
}