/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TimeFormatUtils.
 */
class TimeFormatUtilsTest {

    @Test
    @DisplayName("TimeFormatUtils toIsoString formats instant")
    void toIsoString() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        String result = TimeFormatUtils.toIsoString(instant);

        assertTrue(result.contains("2024"));
        assertTrue(result.contains("01"));
        assertTrue(result.contains("15"));
    }

    @Test
    @DisplayName("TimeFormatUtils toIsoString null returns null")
    void toIsoStringNull() {
        assertNull(TimeFormatUtils.toIsoString(null));
    }

    @Test
    @DisplayName("TimeFormatUtils toReadableString formats")
    void toReadableString() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        String result = TimeFormatUtils.toReadableString(instant);

        assertTrue(result.contains("2024"));
        assertTrue(result.contains("01"));
        assertTrue(result.contains("15"));
    }

    @Test
    @DisplayName("TimeFormatUtils toReadableString null returns null")
    void toReadableStringNull() {
        assertNull(TimeFormatUtils.toReadableString(null));
    }

    @Test
    @DisplayName("TimeFormatUtils toShortString formats")
    void toShortString() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        String result = TimeFormatUtils.toShortString(instant);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("TimeFormatUtils toShortString null returns null")
    void toShortStringNull() {
        assertNull(TimeFormatUtils.toShortString(null));
    }

    @Test
    @DisplayName("TimeFormatUtils toTimeString formats")
    void toTimeString() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        String result = TimeFormatUtils.toTimeString(instant);

        assertTrue(result.contains(":"));
    }

    @Test
    @DisplayName("TimeFormatUtils fromIsoString parses")
    void fromIsoString() {
        Instant result = TimeFormatUtils.fromIsoString("2024-01-15T10:30:00Z");

        assertNotNull(result);
        assertEquals(Instant.parse("2024-01-15T10:30:00Z"), result);
    }

    @Test
    @DisplayName("TimeFormatUtils fromIsoString null returns null")
    void fromIsoStringNull() {
        assertNull(TimeFormatUtils.fromIsoString(null));
    }

    @Test
    @DisplayName("TimeFormatUtils fromIsoString invalid returns null")
    void fromIsoStringInvalid() {
        assertNull(TimeFormatUtils.fromIsoString("not a date"));
    }

    @Test
    @DisplayName("TimeFormatUtils currentMillis returns value")
    void currentMillis() {
        long result = TimeFormatUtils.currentMillis();

        assertTrue(result > 0);
    }

    @Test
    @DisplayName("TimeFormatUtils currentSeconds returns value")
    void currentSeconds() {
        long result = TimeFormatUtils.currentSeconds();

        assertTrue(result > 0);
    }

    @Test
    @DisplayName("TimeFormatUtils now returns instant")
    void now() {
        Instant result = TimeFormatUtils.now();

        assertNotNull(result);
        assertTrue(result.isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("TimeFormatUtils formatDuration seconds")
    void formatDurationSeconds() {
        Duration duration = Duration.ofSeconds(30);
        String result = TimeFormatUtils.formatDuration(duration);

        assertTrue(result.contains("30"));
        assertTrue(result.contains("s"));
    }

    @Test
    @DisplayName("TimeFormatUtils formatDuration minutes")
    void formatDurationMinutes() {
        Duration duration = Duration.ofMinutes(5);
        String result = TimeFormatUtils.formatDuration(duration);

        assertTrue(result.contains("5"));
        assertTrue(result.contains("m"));
    }

    @Test
    @DisplayName("TimeFormatUtils formatDuration hours")
    void formatDurationHours() {
        Duration duration = Duration.ofHours(2);
        String result = TimeFormatUtils.formatDuration(duration);

        assertTrue(result.contains("2"));
        assertTrue(result.contains("h"));
    }

    @Test
    @DisplayName("TimeFormatUtils formatDuration days")
    void formatDurationDays() {
        Duration duration = Duration.ofDays(3);
        String result = TimeFormatUtils.formatDuration(duration);

        assertTrue(result.contains("3"));
        assertTrue(result.contains("d"));
    }

    @Test
    @DisplayName("TimeFormatUtils formatDuration null returns empty")
    void formatDurationNull() {
        assertEquals("", TimeFormatUtils.formatDuration(null));
    }

    @Test
    @DisplayName("TimeFormatUtils formatDurationCompact milliseconds")
    void formatDurationCompactMs() {
        Duration duration = Duration.ofMillis(500);
        String result = TimeFormatUtils.formatDurationCompact(duration);

        assertEquals("500ms", result);
    }

    @Test
    @DisplayName("TimeFormatUtils formatDurationCompact seconds")
    void formatDurationCompactSeconds() {
        Duration duration = Duration.ofSeconds(30);
        String result = TimeFormatUtils.formatDurationCompact(duration);

        assertEquals("30s", result);
    }

    @Test
    @DisplayName("TimeFormatUtils formatDurationCompact null returns empty")
    void formatDurationCompactNull() {
        assertEquals("", TimeFormatUtils.formatDurationCompact(null));
    }

    @Test
    @DisplayName("TimeFormatUtils formatRelativeTime past")
    void formatRelativeTimePast() {
        Instant instant = Instant.now().minusSeconds(60);
        String result = TimeFormatUtils.formatRelativeTime(instant);

        assertTrue(result.contains("ago"));
    }

    @Test
    @DisplayName("TimeFormatUtils formatRelativeTime future")
    void formatRelativeTimeFuture() {
        Instant instant = Instant.now().plusSeconds(60);
        String result = TimeFormatUtils.formatRelativeTime(instant);

        assertTrue(result.contains("from now"));
    }

    @Test
    @DisplayName("TimeFormatUtils formatRelativeTime just now")
    void formatRelativeTimeJustNow() {
        Instant instant = Instant.now().minusSeconds(1);
        String result = TimeFormatUtils.formatRelativeTime(instant);

        assertTrue(result.contains("just now") || result.contains("1 second"));
    }

    @Test
    @DisplayName("TimeFormatUtils formatRelativeTime null returns empty")
    void formatRelativeTimeNull() {
        assertEquals("", TimeFormatUtils.formatRelativeTime(null));
    }

    @Test
    @DisplayName("TimeFormatUtils formatTimestamp formats")
    void formatTimestamp() {
        long timestamp = System.currentTimeMillis();
        String result = TimeFormatUtils.formatTimestamp(timestamp);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("TimeFormatUtils formatLogTimestamp formats")
    void formatLogTimestamp() {
        long timestamp = System.currentTimeMillis();
        String result = TimeFormatUtils.formatLogTimestamp(timestamp);

        assertTrue(result.contains("."));
    }

    @Test
    @DisplayName("TimeFormatUtils getAge returns duration")
    void getAge() {
        long timestamp = System.currentTimeMillis() - 60000;
        Duration result = TimeFormatUtils.getAge(timestamp);

        assertTrue(result.getSeconds() >= 60);
    }

    @Test
    @DisplayName("TimeFormatUtils isRecent true")
    void isRecentTrue() {
        long timestamp = System.currentTimeMillis();
        assertTrue(TimeFormatUtils.isRecent(timestamp, Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("TimeFormatUtils isRecent false")
    void isRecentFalse() {
        long timestamp = System.currentTimeMillis() - 600000;
        assertFalse(TimeFormatUtils.isRecent(timestamp, Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("TimeFormatUtils isExpired true")
    void isExpiredTrue() {
        long timestamp = System.currentTimeMillis() - 600000;
        assertTrue(TimeFormatUtils.isExpired(timestamp, Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("TimeFormatUtils isExpired false")
    void isExpiredFalse() {
        long timestamp = System.currentTimeMillis();
        assertFalse(TimeFormatUtils.isExpired(timestamp, Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("TimeFormatUtils startOfDay returns start")
    void startOfDay() {
        Instant instant = Instant.now();
        Instant result = TimeFormatUtils.startOfDay(instant);

        assertTrue(result.isBefore(instant));
    }

    @Test
    @DisplayName("TimeFormatUtils endOfDay returns end")
    void endOfDay() {
        Instant instant = Instant.now();
        Instant result = TimeFormatUtils.endOfDay(instant);

        assertTrue(result.isAfter(instant));
    }

    @Test
    @DisplayName("TimeFormatUtils isToday true")
    void isTodayTrue() {
        assertTrue(TimeFormatUtils.isToday(Instant.now()));
    }

    @Test
    @DisplayName("TimeFormatUtils isToday false")
    void isTodayFalse() {
        assertFalse(TimeFormatUtils.isToday(Instant.now().minusSeconds(86400 * 2)));
    }

    @Test
    @DisplayName("TimeFormatUtils isYesterday true")
    void isYesterdayTrue() {
        assertTrue(TimeFormatUtils.isYesterday(Instant.now().minusSeconds(86400)));
    }

    @Test
    @DisplayName("TimeFormatUtils isYesterday false")
    void isYesterdayFalse() {
        assertFalse(TimeFormatUtils.isYesterday(Instant.now()));
    }

    @Test
    @DisplayName("TimeFormatUtils parseDuration simple")
    void parseDurationSimple() {
        Duration result = TimeFormatUtils.parseDuration("30s");

        assertEquals(30, result.getSeconds());
    }

    @Test
    @DisplayName("TimeFormatUtils parseDuration minutes")
    void parseDurationMinutes() {
        Duration result = TimeFormatUtils.parseDuration("5m");

        assertEquals(5 * 60, result.getSeconds());
    }

    @Test
    @DisplayName("TimeFormatUtils parseDuration hours")
    void parseDurationHours() {
        Duration result = TimeFormatUtils.parseDuration("2h");

        assertEquals(2 * 3600, result.getSeconds());
    }

    @Test
    @DisplayName("TimeFormatUtils parseDuration days")
    void parseDurationDays() {
        Duration result = TimeFormatUtils.parseDuration("1d");

        assertEquals(86400, result.getSeconds());
    }

    @Test
    @DisplayName("TimeFormatUtils parseDuration complex")
    void parseDurationComplex() {
        Duration result = TimeFormatUtils.parseDuration("1h30m");

        assertEquals(90 * 60, result.getSeconds());
    }

    @Test
    @DisplayName("TimeFormatUtils parseDuration null returns zero")
    void parseDurationNull() {
        assertEquals(Duration.ZERO, TimeFormatUtils.parseDuration(null));
    }

    @Test
    @DisplayName("TimeFormatUtils parseDuration empty returns zero")
    void parseDurationEmpty() {
        assertEquals(Duration.ZERO, TimeFormatUtils.parseDuration(""));
    }

    @Test
    @DisplayName("TimeFormatUtils sleep does not throw")
    void sleepDuration() {
        assertDoesNotThrow(() -> TimeFormatUtils.sleep(Duration.ofMillis(10)));
    }

    @Test
    @DisplayName("TimeFormatUtils sleep millis does not throw")
    void sleepMillis() {
        assertDoesNotThrow(() -> TimeFormatUtils.sleep(10));
    }

    @Test
    @DisplayName("TimeFormatUtils deadlineFromNow returns future")
    void deadlineFromNow() {
        Instant result = TimeFormatUtils.deadlineFromNow(Duration.ofMinutes(5));

        assertTrue(result.isAfter(Instant.now()));
    }

    @Test
    @DisplayName("TimeFormatUtils isDeadlinePassed true")
    void isDeadlinePassedTrue() {
        Instant deadline = Instant.now().minusSeconds(1);
        assertTrue(TimeFormatUtils.isDeadlinePassed(deadline));
    }

    @Test
    @DisplayName("TimeFormatUtils isDeadlinePassed false")
    void isDeadlinePassedFalse() {
        Instant deadline = Instant.now().plusSeconds(60);
        assertFalse(TimeFormatUtils.isDeadlinePassed(deadline));
    }

    @Test
    @DisplayName("TimeFormatUtils timeRemaining returns duration")
    void timeRemaining() {
        Instant deadline = Instant.now().plusSeconds(60);
        Duration result = TimeFormatUtils.timeRemaining(deadline);

        assertTrue(result.getSeconds() <= 60);
        assertTrue(result.getSeconds() > 0);
    }

    @Test
    @DisplayName("TimeFormatUtils timeRemaining past returns zero")
    void timeRemainingPast() {
        Instant deadline = Instant.now().minusSeconds(60);
        Duration result = TimeFormatUtils.timeRemaining(deadline);

        assertEquals(Duration.ZERO, result);
    }

    @Test
    @DisplayName("TimeFormatUtils measureTime measures runnable")
    void measureTimeRunnable() {
        Duration result = TimeFormatUtils.measureTime(() -> {
            try { Thread.sleep(50); } catch (InterruptedException e) {}
        });

        assertTrue(result.toMillis() >= 50);
    }

    @Test
    @DisplayName("TimeFormatUtils measureTime measures supplier")
    void measureTimeSupplier() {
        TimeFormatUtils.TimedResult<String> result = TimeFormatUtils.measureTime(() -> "test");

        assertEquals("test", result.value());
        assertNotNull(result.duration());
    }

    @Test
    @DisplayName("TimeFormatUtils TimedResult record")
    void timedResultRecord() {
        TimeFormatUtils.TimedResult<Integer> result = new TimeFormatUtils.TimedResult<>(42, Duration.ofMillis(100));

        assertEquals(42, result.value());
        assertEquals(Duration.ofMillis(100), result.duration());
    }

    @Test
    @DisplayName("TimeFormatUtils format with pattern")
    void formatWithPattern() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        String result = TimeFormatUtils.format(instant, "yyyy-MM-dd");

        assertTrue(result.contains("2024"));
    }

    @Test
    @DisplayName("TimeFormatUtils format null returns null")
    void formatNull() {
        assertNull(TimeFormatUtils.format(null, "yyyy-MM-dd"));
    }

    @Test
    @DisplayName("TimeFormatUtils parse with pattern")
    void parseWithPattern() {
        Instant result = TimeFormatUtils.parse("2024-01-15 10:30", "yyyy-MM-dd HH:mm");

        assertNotNull(result);
    }

    @Test
    @DisplayName("TimeFormatUtils parse null returns null")
    void parseNull() {
        assertNull(TimeFormatUtils.parse(null, "yyyy-MM-dd"));
    }

    @Test
    @DisplayName("TimeFormatUtils parse invalid returns null")
    void parseInvalid() {
        assertNull(TimeFormatUtils.parse("not a date", "yyyy-MM-dd"));
    }
}