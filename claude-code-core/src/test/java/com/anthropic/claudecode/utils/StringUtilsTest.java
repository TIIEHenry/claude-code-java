/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StringUtils.
 */
class StringUtilsTest {

    @Test
    @DisplayName("isNullOrEmpty works correctly")
    void isNullOrEmptyWorks() {
        assertTrue(StringUtils.isNullOrEmpty(null));
        assertTrue(StringUtils.isNullOrEmpty(""));
        assertFalse(StringUtils.isNullOrEmpty("test"));
        assertFalse(StringUtils.isNullOrEmpty(" "));
    }

    @Test
    @DisplayName("isNullOrBlank works correctly")
    void isNullOrBlankWorks() {
        assertTrue(StringUtils.isNullOrBlank(null));
        assertTrue(StringUtils.isNullOrBlank(""));
        assertTrue(StringUtils.isNullOrBlank("   "));
        assertFalse(StringUtils.isNullOrBlank("test"));
    }

    @Test
    @DisplayName("truncate works correctly")
    void truncateWorks() {
        assertNull(StringUtils.truncate(null, 10));
        assertEquals("short", StringUtils.truncate("short", 10));
        // truncate takes first 10 chars and adds "...", so "very long " + "..."
        assertEquals("very long ...", StringUtils.truncate("very long string", 10));
    }

    @Test
    @DisplayName("truncateMiddle works correctly")
    void truncateMiddleWorks() {
        assertNull(StringUtils.truncateMiddle(null, 10));
        assertEquals("short", StringUtils.truncateMiddle("short", 10));
        // maxLength=10, half = 10/2 - 2 = 3, so first 3 + "..." + last 3 = "sup...ing"
        assertEquals("sup...ing", StringUtils.truncateMiddle("super long string", 10));
    }

    @Test
    @DisplayName("repeat works correctly")
    void repeatWorks() {
        assertEquals("", StringUtils.repeat(null, 3));
        assertEquals("", StringUtils.repeat("a", 0));
        assertEquals("aaa", StringUtils.repeat("a", 3));
        assertEquals("ababab", StringUtils.repeat("ab", 3));
    }

    @Test
    @DisplayName("capitalize works correctly")
    void capitalizeWorks() {
        assertNull(StringUtils.capitalize(null));
        assertEquals("", StringUtils.capitalize(""));
        assertEquals("Test", StringUtils.capitalize("test"));
        assertEquals("TEST", StringUtils.capitalize("TEST"));
    }

    @Test
    @DisplayName("escapeJson works correctly")
    void escapeJsonWorks() {
        assertEquals("null", StringUtils.escapeJson(null));
        assertEquals("\"test\"", StringUtils.escapeJson("test"));
        assertEquals("\"test\\nline\"", StringUtils.escapeJson("test\nline"));
        assertEquals("\"test\\ttab\"", StringUtils.escapeJson("test\ttab"));
        assertEquals("\"test\\\"quote\"", StringUtils.escapeJson("test\"quote"));
        assertEquals("\"test\\\\slash\"", StringUtils.escapeJson("test\\slash"));
    }

    @Test
    @DisplayName("padLeft works correctly")
    void padLeftWorks() {
        assertEquals("   test", StringUtils.padLeft("test", 7, ' '));
        assertEquals("test", StringUtils.padLeft("test", 3, ' '));
        assertEquals("00042", StringUtils.padLeft("42", 5, '0'));
        assertEquals("    ", StringUtils.padLeft(null, 4, ' '));
    }

    @Test
    @DisplayName("padRight works correctly")
    void padRightWorks() {
        assertEquals("test   ", StringUtils.padRight("test", 7, ' '));
        assertEquals("test", StringUtils.padRight("test", 3, ' '));
        assertEquals("42   ", StringUtils.padRight("42", 5, ' '));
        assertEquals("    ", StringUtils.padRight(null, 4, ' '));
    }
}