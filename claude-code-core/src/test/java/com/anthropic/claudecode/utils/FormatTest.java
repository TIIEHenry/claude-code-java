/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Format.
 */
class FormatTest {

    @Test
    @DisplayName("Format formatFileSize bytes")
    void formatFileSizeBytes() {
        assertEquals("500 bytes", Format.formatFileSize(500));
        assertEquals("0 bytes", Format.formatFileSize(0));
        assertEquals("1 bytes", Format.formatFileSize(1));
    }

    @Test
    @DisplayName("Format formatFileSize KB")
    void formatFileSizeKB() {
        assertTrue(Format.formatFileSize(1024).contains("KB"));
        assertTrue(Format.formatFileSize(2048).contains("KB"));
    }

    @Test
    @DisplayName("Format formatFileSize MB")
    void formatFileSizeMB() {
        assertTrue(Format.formatFileSize(1024 * 1024).contains("MB"));
        assertTrue(Format.formatFileSize(5 * 1024 * 1024).contains("MB"));
    }

    @Test
    @DisplayName("Format formatFileSize GB")
    void formatFileSizeGB() {
        assertTrue(Format.formatFileSize(1024L * 1024 * 1024).contains("GB"));
        assertTrue(Format.formatFileSize(5L * 1024 * 1024 * 1024).contains("GB"));
    }

    @Test
    @DisplayName("Format formatSecondsShort")
    void formatSecondsShort() {
        assertTrue(Format.formatSecondsShort(1000).contains("s"));
        assertTrue(Format.formatSecondsShort(5000).contains("5"));
    }

    @Test
    @DisplayName("Format formatDuration zero")
    void formatDurationZero() {
        assertEquals("0s", Format.formatDuration(0));
    }

    @Test
    @DisplayName("Format formatDuration seconds")
    void formatDurationSeconds() {
        assertEquals("5s", Format.formatDuration(5000));
        assertEquals("30s", Format.formatDuration(30000));
    }

    @Test
    @DisplayName("Format formatDuration minutes")
    void formatDurationMinutes() {
        String result = Format.formatDuration(90000); // 1m 30s
        assertTrue(result.contains("m"));
    }

    @Test
    @DisplayName("Format formatDuration hours")
    void formatDurationHours() {
        String result = Format.formatDuration(3661000); // 1h 1m 1s
        assertTrue(result.contains("h"));
    }

    @Test
    @DisplayName("Format formatDuration days")
    void formatDurationDays() {
        String result = Format.formatDuration(90061000); // 1d 1h 1m
        assertTrue(result.contains("d"));
    }

    @Test
    @DisplayName("Format formatDuration mostSignificantOnly")
    void formatDurationMostSignificantOnly() {
        assertEquals("1d", Format.formatDuration(90061000, false, true));
        assertEquals("1h", Format.formatDuration(3661000, false, true));
        assertEquals("1m", Format.formatDuration(90000, false, true));
    }

    @Test
    @DisplayName("Format formatNumber small")
    void formatNumberSmall() {
        assertEquals("100", Format.formatNumber(100));
        assertEquals("999", Format.formatNumber(999));
    }

    @Test
    @DisplayName("Format formatNumber thousands")
    void formatNumberThousands() {
        assertTrue(Format.formatNumber(1000).contains("k"));
        assertTrue(Format.formatNumber(5000).contains("k"));
    }

    @Test
    @DisplayName("Format formatNumber millions")
    void formatNumberMillions() {
        assertTrue(Format.formatNumber(1_000_000).contains("m"));
    }

    @Test
    @DisplayName("Format formatNumber billions")
    void formatNumberBillions() {
        assertTrue(Format.formatNumber(1_000_000_000).contains("b"));
    }

    @Test
    @DisplayName("Format formatTokens")
    void formatTokens() {
        assertNotNull(Format.formatTokens(1000));
        assertNotNull(Format.formatTokens(1000000));
    }

    @Test
    @DisplayName("Format formatRelativeTime past")
    void formatRelativeTimePast() {
        Instant now = Instant.now();
        Instant past = now.minusSeconds(3600); // 1 hour ago

        String result = Format.formatRelativeTime(past, now);
        assertTrue(result.contains("ago"));
    }

    @Test
    @DisplayName("Format formatRelativeTime future")
    void formatRelativeTimeFuture() {
        Instant now = Instant.now();
        Instant future = now.plusSeconds(3600); // 1 hour from now

        String result = Format.formatRelativeTime(future, now);
        assertTrue(result.contains("in"));
    }

    @Test
    @DisplayName("Format formatRelativeTime minutes")
    void formatRelativeTimeMinutes() {
        Instant now = Instant.now();
        Instant past = now.minusSeconds(180); // 3 minutes ago

        String result = Format.formatRelativeTime(past, now);
        assertTrue(result.contains("m"));
    }

    @Test
    @DisplayName("Format formatRelativeTime days")
    void formatRelativeTimeDays() {
        Instant now = Instant.now();
        Instant past = now.minusSeconds(86400 * 2); // 2 days ago

        String result = Format.formatRelativeTime(past, now);
        assertTrue(result.contains("d"));
    }

    @Test
    @DisplayName("Format formatTimestamp")
    void formatTimestamp() {
        Instant now = Instant.now();
        String result = Format.formatTimestamp(now);
        assertNotNull(result);
        assertTrue(result.contains("-")); // Date separator
    }

    @Test
    @DisplayName("Format formatDate")
    void formatDate() {
        Instant now = Instant.now();
        String result = Format.formatDate(now);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Format formatTime")
    void formatTime() {
        Instant now = Instant.now();
        String result = Format.formatTime(now);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Format formatPercent")
    void formatPercent() {
        assertEquals("50.0%", Format.formatPercent(50.0));
        assertEquals("33.3%", Format.formatPercent(33.3));
    }

    @Test
    @DisplayName("Format formatCurrency")
    void formatCurrency() {
        assertEquals("$10.00", Format.formatCurrency(10.0));
        assertEquals("$99.99", Format.formatCurrency(99.99));
    }

    @Test
    @DisplayName("Format plural with singular")
    void pluralSingular() {
        assertEquals("item", Format.plural(1, "item"));
    }

    @Test
    @DisplayName("Format plural with plural")
    void pluralPlural() {
        assertEquals("items", Format.plural(2, "item"));
        assertEquals("items", Format.plural(0, "item"));
    }

    @Test
    @DisplayName("Format plural with custom forms")
    void pluralCustom() {
        assertEquals("mouse", Format.plural(1, "mouse", "mice"));
        assertEquals("mice", Format.plural(2, "mouse", "mice"));
    }

    @Test
    @DisplayName("Format formatList empty")
    void formatListEmpty() {
        assertEquals("", Format.formatList(List.of()));
        assertEquals("", Format.formatList(null));
    }

    @Test
    @DisplayName("Format formatList single")
    void formatListSingle() {
        assertEquals("one", Format.formatList(List.of("one")));
    }

    @Test
    @DisplayName("Format formatList two")
    void formatListTwo() {
        assertEquals("one and two", Format.formatList(List.of("one", "two")));
    }

    @Test
    @DisplayName("Format formatList multiple")
    void formatListMultiple() {
        String result = Format.formatList(List.of("one", "two", "three"));
        assertTrue(result.contains("one"));
        assertTrue(result.contains("two"));
        assertTrue(result.contains("three"));
        assertTrue(result.contains("and"));
    }
}