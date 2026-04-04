/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FormatUtil.
 */
class FormatUtilTest {

    @Test
    @DisplayName("FormatUtil formatFileSize bytes")
    void formatFileSizeBytes() {
        assertEquals("500 bytes", FormatUtil.formatFileSize(500));
        assertEquals("0 bytes", FormatUtil.formatFileSize(0));
    }

    @Test
    @DisplayName("FormatUtil formatFileSize KB")
    void formatFileSizeKB() {
        assertEquals("1KB", FormatUtil.formatFileSize(1024));
        assertEquals("1.5KB", FormatUtil.formatFileSize(1536));
    }

    @Test
    @DisplayName("FormatUtil formatFileSize MB")
    void formatFileSizeMB() {
        assertEquals("1MB", FormatUtil.formatFileSize(1024 * 1024));
        assertEquals("1.5MB", FormatUtil.formatFileSize((long)(1.5 * 1024 * 1024)));
    }

    @Test
    @DisplayName("FormatUtil formatFileSize GB")
    void formatFileSizeGB() {
        assertEquals("1GB", FormatUtil.formatFileSize(1024L * 1024 * 1024));
    }

    @Test
    @DisplayName("FormatUtil formatSecondsShort")
    void formatSecondsShort() {
        assertTrue(FormatUtil.formatSecondsShort(1000).contains("1.0"));
        assertTrue(FormatUtil.formatSecondsShort(500).contains("0.5"));
    }

    @Test
    @DisplayName("FormatUtil formatDuration zero")
    void formatDurationZero() {
        assertEquals("0s", FormatUtil.formatDuration(0));
    }

    @Test
    @DisplayName("FormatUtil formatDuration milliseconds")
    void formatDurationMilliseconds() {
        assertTrue(FormatUtil.formatDuration(500).contains("0.5"));
    }

    @Test
    @DisplayName("FormatUtil formatDuration seconds")
    void formatDurationSeconds() {
        assertEquals("5s", FormatUtil.formatDuration(5000));
    }

    @Test
    @DisplayName("FormatUtil formatDuration minutes")
    void formatDurationMinutes() {
        String result = FormatUtil.formatDuration(90000); // 1m 30s
        assertTrue(result.contains("1m"));
        assertTrue(result.contains("30s"));
    }

    @Test
    @DisplayName("FormatUtil formatDuration hours")
    void formatDurationHours() {
        String result = FormatUtil.formatDuration(3661000); // 1h 1m 1s
        assertTrue(result.contains("1h"));
        assertTrue(result.contains("1m"));
        assertTrue(result.contains("1s"));
    }

    @Test
    @DisplayName("FormatUtil formatDuration days")
    void formatDurationDays() {
        String result = FormatUtil.formatDuration(90061000); // 1d 1h 1m
        assertTrue(result.contains("1d"));
        assertTrue(result.contains("1h"));
        assertTrue(result.contains("1m"));
    }

    @Test
    @DisplayName("FormatUtil FormatDurationOptions record")
    void formatDurationOptionsRecord() {
        FormatUtil.FormatDurationOptions opts = new FormatUtil.FormatDurationOptions(true, true);
        assertTrue(opts.hideTrailingZeros());
        assertTrue(opts.mostSignificantOnly());
    }

    @Test
    @DisplayName("FormatUtil formatDuration with hideTrailingZeros")
    void formatDurationHideTrailingZeros() {
        FormatUtil.FormatDurationOptions opts = new FormatUtil.FormatDurationOptions(true, false);
        String result = FormatUtil.formatDuration(3600000, opts); // 1h
        assertTrue(result.contains("1h"));
    }

    @Test
    @DisplayName("FormatUtil formatDuration with mostSignificantOnly")
    void formatDurationMostSignificantOnly() {
        FormatUtil.FormatDurationOptions opts = new FormatUtil.FormatDurationOptions(false, true);
        String result = FormatUtil.formatDuration(3661000, opts); // 1h 1m 1s
        assertEquals("1h", result);
    }

    @Test
    @DisplayName("FormatUtil formatNumber small")
    void formatNumberSmall() {
        assertEquals("100", FormatUtil.formatNumber(100));
        assertEquals("999", FormatUtil.formatNumber(999));
    }

    @Test
    @DisplayName("FormatUtil formatNumber thousands")
    void formatNumberThousands() {
        assertTrue(FormatUtil.formatNumber(1000).contains("1"));
        assertTrue(FormatUtil.formatNumber(1000).contains("k"));
    }

    @Test
    @DisplayName("FormatUtil formatNumber millions")
    void formatNumberMillions() {
        assertTrue(FormatUtil.formatNumber(1000000).contains("m"));
    }

    @Test
    @DisplayName("FormatUtil formatTokens")
    void formatTokens() {
        assertNotNull(FormatUtil.formatTokens(1000));
        assertNotNull(FormatUtil.formatTokens(1000000));
    }

    @Test
    @DisplayName("FormatUtil formatRelativeTime past")
    void formatRelativeTimePast() {
        Instant past = Instant.now().minusSeconds(3600); // 1 hour ago
        String result = FormatUtil.formatRelativeTime(past);
        assertTrue(result.contains("1h"));
        assertTrue(result.contains("ago"));
    }

    @Test
    @DisplayName("FormatUtil formatRelativeTime future")
    void formatRelativeTimeFuture() {
        Instant future = Instant.now().plusSeconds(3600); // 1 hour in future
        String result = FormatUtil.formatRelativeTime(future);
        assertTrue(result.contains("1h"));
    }

    @Test
    @DisplayName("FormatUtil LogMetadata record")
    void logMetadataRecord() {
        FormatUtil.LogMetadata metadata = new FormatUtil.LogMetadata(
            Instant.now(), 10, 1024L, "main", "tag", "agent", 123, "repo"
        );
        assertEquals(10, metadata.messageCount());
        assertEquals("main", metadata.gitBranch());
        assertEquals("tag", metadata.tag());
    }

    @Test
    @DisplayName("FormatUtil formatLogMetadata")
    void formatLogMetadata() {
        FormatUtil.LogMetadata metadata = new FormatUtil.LogMetadata(
            Instant.now().minusSeconds(60), 5, null, "main", null, null, null, null
        );
        String result = FormatUtil.formatLogMetadata(metadata);
        assertTrue(result.contains("main"));
    }

    @Test
    @DisplayName("FormatUtil truncate")
    void truncate() {
        assertEquals("short", FormatUtil.truncate("short", 10));
        assertEquals("abc...", FormatUtil.truncate("abcdefg", 6));
        assertNull(FormatUtil.truncate(null, 10));
    }

    @Test
    @DisplayName("FormatUtil truncatePathMiddle")
    void truncatePathMiddle() {
        String path = "/very/long/path/to/some/file.txt";
        String truncated = FormatUtil.truncatePathMiddle(path, 20);
        assertTrue(truncated.contains("..."));
        assertTrue(truncated.length() <= 20);
    }

    @Test
    @DisplayName("FormatUtil formatResetTime returns string")
    void formatResetTime() {
        long futureTime = Instant.now().plusSeconds(3600).getEpochSecond();
        String result = FormatUtil.formatResetTime(futureTime);
        assertNotNull(result);
    }
}