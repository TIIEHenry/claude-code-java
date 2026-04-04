/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FormatBriefTimestamp.
 */
class FormatBriefTimestampTest {

    @Test
    @DisplayName("FormatBriefTimestamp same day")
    void formatBriefTimestampSameDay() {
        Instant now = Instant.now();
        String iso = now.toString();

        String result = FormatBriefTimestamp.formatBriefTimestamp(iso, now);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("FormatBriefTimestamp within week")
    void formatBriefTimestampWithinWeek() {
        Instant now = Instant.now();
        Instant past = now.minusSeconds(86400 * 3); // 3 days ago

        String result = FormatBriefTimestamp.formatBriefTimestamp(past.toString(), now);
        assertNotNull(result);
        assertTrue(result.contains(","));
    }

    @Test
    @DisplayName("FormatBriefTimestamp older than week")
    void formatBriefTimestampOlder() {
        Instant now = Instant.now();
        Instant past = now.minusSeconds(86400 * 10); // 10 days ago

        String result = FormatBriefTimestamp.formatBriefTimestamp(past.toString(), now);
        assertNotNull(result);
    }

    @Test
    @DisplayName("FormatBriefTimestamp invalid input returns empty")
    void formatBriefTimestampInvalid() {
        String result = FormatBriefTimestamp.formatBriefTimestamp("invalid");
        assertEquals("", result);
    }

    @Test
    @DisplayName("FormatBriefTimestamp null throws exception")
    void formatBriefTimestampNull() {
        // The method doesn't handle null input - will throw NPE
        assertThrows(NullPointerException.class, () -> FormatBriefTimestamp.formatBriefTimestamp(null));
    }

    @Test
    @DisplayName("FormatBriefTimestamp with Z suffix")
    void formatBriefTimestampZSuffix() {
        Instant now = Instant.now();
        String iso = now.toString(); // Already has Z suffix

        String result = FormatBriefTimestamp.formatBriefTimestamp(iso, now);
        assertNotNull(result);
    }

    @Test
    @DisplayName("FormatBriefTimestamp with timezone offset")
    void formatBriefTimestampTimezoneOffset() {
        Instant now = Instant.now();
        // ISO format with timezone
        String iso = now.atZone(java.time.ZoneId.systemDefault()).toString();

        String result = FormatBriefTimestamp.formatBriefTimestamp(iso, now);
        assertNotNull(result);
    }
}