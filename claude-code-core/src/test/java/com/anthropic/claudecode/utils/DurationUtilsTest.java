/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DurationUtils.
 */
class DurationUtilsTest {

    @Test
    @DisplayName("DurationUtils parse parses milliseconds")
    void parseMillis() {
        Duration d = DurationUtils.parse("100ms");
        assertEquals(100, d.toMillis());
    }

    @Test
    @DisplayName("DurationUtils parse parses seconds")
    void parseSeconds() {
        Duration d = DurationUtils.parse("5s");
        assertEquals(5, d.toSeconds());
    }

    @Test
    @DisplayName("DurationUtils parse parses minutes")
    void parseMinutes() {
        Duration d = DurationUtils.parse("2m");
        assertEquals(2, d.toMinutes());
    }

    @Test
    @DisplayName("DurationUtils parse parses hours")
    void parseHours() {
        Duration d = DurationUtils.parse("3h");
        assertEquals(3, d.toHours());
    }

    @Test
    @DisplayName("DurationUtils parse parses days")
    void parseDays() {
        Duration d = DurationUtils.parse("2d");
        assertEquals(2, d.toDays());
    }

    @Test
    @DisplayName("DurationUtils parse parses weeks")
    void parseWeeks() {
        Duration d = DurationUtils.parse("1w");
        assertEquals(7, d.toDays());
    }

    @Test
    @DisplayName("DurationUtils parse parses long form")
    void parseLongForm() {
        assertEquals(1000, DurationUtils.parse("1seconds").toMillis());
        assertEquals(60000, DurationUtils.parse("1minutes").toMillis());
        assertEquals(3600000, DurationUtils.parse("1hours").toMillis());
    }

    @Test
    @DisplayName("DurationUtils parse throws on invalid")
    void parseInvalid() {
        assertThrows(IllegalArgumentException.class, () -> DurationUtils.parse(""));
        assertThrows(IllegalArgumentException.class, () -> DurationUtils.parse("invalid"));
    }

    @Test
    @DisplayName("DurationUtils parseOrDefault returns default on error")
    void parseOrDefault() {
        Duration d = DurationUtils.parseOrDefault("invalid", Duration.ofSeconds(10));
        assertEquals(10, d.toSeconds());
    }

    @Test
    @DisplayName("DurationUtils format formats milliseconds")
    void formatMillis() {
        assertEquals("100ms", DurationUtils.format(Duration.ofMillis(100)));
    }

    @Test
    @DisplayName("DurationUtils format formats seconds")
    void formatSeconds() {
        assertEquals("5s", DurationUtils.format(Duration.ofSeconds(5)));
    }

    @Test
    @DisplayName("DurationUtils format formats minutes")
    void formatMinutes() {
        assertEquals("2m", DurationUtils.format(Duration.ofMinutes(2)));
    }

    @Test
    @DisplayName("DurationUtils format formats hours")
    void formatHours() {
        assertEquals("3h", DurationUtils.format(Duration.ofHours(3)));
    }

    @Test
    @DisplayName("DurationUtils format formats days")
    void formatDays() {
        assertEquals("2d", DurationUtils.format(Duration.ofDays(2)));
    }

    @Test
    @DisplayName("DurationUtils format formats zero")
    void formatZero() {
        assertEquals("0ms", DurationUtils.format(Duration.ZERO));
    }

    @Test
    @DisplayName("DurationUtils formatCompact formats compactly")
    void formatCompact() {
        assertEquals("1h30m", DurationUtils.formatCompact(Duration.ofMinutes(90)));
        assertEquals("1h30m5s", DurationUtils.formatCompact(Duration.ofMinutes(90).plusSeconds(5)));
    }

    @Test
    @DisplayName("DurationUtils formatIso returns ISO format")
    void formatIso() {
        String iso = DurationUtils.formatIso(Duration.ofHours(2));
        assertTrue(iso.startsWith("PT"));
    }

    @Test
    @DisplayName("DurationUtils min returns smaller")
    void min() {
        Duration a = Duration.ofSeconds(5);
        Duration b = Duration.ofSeconds(10);
        assertEquals(a, DurationUtils.min(a, b));
    }

    @Test
    @DisplayName("DurationUtils max returns larger")
    void max() {
        Duration a = Duration.ofSeconds(5);
        Duration b = Duration.ofSeconds(10);
        assertEquals(b, DurationUtils.max(a, b));
    }

    @Test
    @DisplayName("DurationUtils sum sums durations")
    void sum() {
        List<Duration> durations = List.of(
            Duration.ofSeconds(1),
            Duration.ofSeconds(2),
            Duration.ofSeconds(3)
        );
        assertEquals(6, DurationUtils.sum(durations).toSeconds());
    }

    @Test
    @DisplayName("DurationUtils average calculates average")
    void average() {
        List<Duration> durations = List.of(
            Duration.ofSeconds(2),
            Duration.ofSeconds(4),
            Duration.ofSeconds(6)
        );
        assertEquals(4, DurationUtils.average(durations).toSeconds());
    }

    @Test
    @DisplayName("DurationUtils clamp clamps to range")
    void clamp() {
        Duration min = Duration.ofSeconds(1);
        Duration max = Duration.ofSeconds(10);

        assertEquals(min, DurationUtils.clamp(Duration.ZERO, min, max));
        assertEquals(max, DurationUtils.clamp(Duration.ofSeconds(20), min, max));
        assertEquals(Duration.ofSeconds(5), DurationUtils.clamp(Duration.ofSeconds(5), min, max));
    }

    @Test
    @DisplayName("DurationUtils scale scales duration")
    void scale() {
        Duration d = Duration.ofSeconds(10);
        Duration scaled = DurationUtils.scale(d, 0.5);
        assertEquals(5, scaled.toSeconds());
    }

    @Test
    @DisplayName("DurationUtils between calculates duration")
    void between() {
        Instant start = Instant.now().minusSeconds(10);
        Instant end = Instant.now();

        Duration d = DurationUtils.between(start, end);
        assertTrue(d.toSeconds() >= 9);
    }

    @Test
    @DisplayName("DurationUtils isExpired checks expiration")
    void isExpired() {
        Instant past = Instant.now().minusSeconds(10);
        assertTrue(DurationUtils.isExpired(past, Duration.ofSeconds(5)));
        assertFalse(DurationUtils.isExpired(past, Duration.ofSeconds(20)));
    }

    @Test
    @DisplayName("DurationUtils clamp negative remaining to zero")
    void remaining() {
        Instant past = Instant.now().minusSeconds(10);
        assertEquals(Duration.ZERO, DurationUtils.remaining(past));
    }

    @Test
    @DisplayName("DurationUtils toMillis converts")
    void toMillis() {
        assertEquals(5000, DurationUtils.toMillis(Duration.ofSeconds(5)));
    }

    @Test
    @DisplayName("DurationUtils toSeconds converts")
    void toSeconds() {
        assertEquals(300, DurationUtils.toSeconds(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("DurationUtils factory methods work")
    void factoryMethods() {
        assertEquals(0, DurationUtils.zero().toMillis());
        assertEquals(1, DurationUtils.oneMillisecond().toMillis());
        assertEquals(1, DurationUtils.oneSecond().toSeconds());
        assertEquals(1, DurationUtils.oneMinute().toMinutes());
        assertEquals(1, DurationUtils.oneHour().toHours());
        assertEquals(1, DurationUtils.oneDay().toDays());
    }

    @Test
    @DisplayName("DurationUtils isZero checks")
    void isZero() {
        assertTrue(DurationUtils.isZero(Duration.ZERO));
        assertFalse(DurationUtils.isZero(Duration.ofSeconds(1)));
    }

    @Test
    @DisplayName("DurationUtils isNegative checks")
    void isNegative() {
        assertTrue(DurationUtils.isNegative(Duration.ofSeconds(-1)));
        assertFalse(DurationUtils.isNegative(Duration.ofSeconds(1)));
    }

    @Test
    @DisplayName("DurationUtils isPositive checks")
    void isPositive() {
        assertTrue(DurationUtils.isPositive(Duration.ofSeconds(1)));
        assertFalse(DurationUtils.isPositive(Duration.ZERO));
        assertFalse(DurationUtils.isPositive(Duration.ofSeconds(-1)));
    }

    @Test
    @DisplayName("DurationUtils abs returns absolute")
    void abs() {
        assertEquals(Duration.ofSeconds(5), DurationUtils.abs(Duration.ofSeconds(-5)));
        assertEquals(Duration.ofSeconds(5), DurationUtils.abs(Duration.ofSeconds(5)));
    }

    @Test
    @DisplayName("DurationUtils negate negates")
    void negate() {
        assertEquals(Duration.ofSeconds(-5), DurationUtils.negate(Duration.ofSeconds(5)));
    }

    @Test
    @DisplayName("DurationUtils divide divides")
    void divide() {
        assertEquals(Duration.ofSeconds(5), DurationUtils.divide(Duration.ofSeconds(10), 2));
    }

    @Test
    @DisplayName("DurationUtils divideBy calculates ratio")
    void divideBy() {
        double ratio = DurationUtils.divideBy(Duration.ofSeconds(10), Duration.ofSeconds(5));
        assertEquals(2.0, ratio);
    }
}