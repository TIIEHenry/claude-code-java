/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RateLimitMessages.
 */
class RateLimitMessagesTest {

    private final RateLimitMessages messages = new RateLimitMessages();

    @Test
    @DisplayName("RateLimitMessages RATE_LIMIT_ERROR_PREFIXES contains expected values")
    void rateLimitErrorPrefixes() {
        List<String> prefixes = RateLimitMessages.RATE_LIMIT_ERROR_PREFIXES;
        assertEquals(5, prefixes.size());
        assertTrue(prefixes.contains("You've hit your"));
        assertTrue(prefixes.contains("You've used"));
        assertTrue(prefixes.contains("You're close to"));
    }

    @Test
    @DisplayName("RateLimitMessages Severity enum values")
    void severityEnum() {
        RateLimitMessages.Severity[] severities = RateLimitMessages.Severity.values();
        assertEquals(2, severities.length);
        assertEquals(RateLimitMessages.Severity.ERROR, RateLimitMessages.Severity.valueOf("ERROR"));
        assertEquals(RateLimitMessages.Severity.WARNING, RateLimitMessages.Severity.valueOf("WARNING"));
    }

    @Test
    @DisplayName("RateLimitMessages RateLimitMessage record")
    void rateLimitMessageRecord() {
        RateLimitMessages.RateLimitMessage msg = new RateLimitMessages.RateLimitMessage(
            "Test message", RateLimitMessages.Severity.ERROR
        );

        assertEquals("Test message", msg.message());
        assertEquals(RateLimitMessages.Severity.ERROR, msg.severity());
    }

    @Test
    @DisplayName("RateLimitMessages isRateLimitErrorMessage true for matching prefix")
    void isRateLimitErrorMessageTrue() {
        assertTrue(messages.isRateLimitErrorMessage("You've hit your limit"));
        assertTrue(messages.isRateLimitErrorMessage("You've used 100% of your limit"));
        assertTrue(messages.isRateLimitErrorMessage("You're close to your limit"));
    }

    @Test
    @DisplayName("RateLimitMessages isRateLimitErrorMessage false for non-matching")
    void isRateLimitErrorMessageFalse() {
        assertFalse(messages.isRateLimitErrorMessage("Some other error"));
        assertFalse(messages.isRateLimitErrorMessage("Error: something went wrong"));
    }

    @Test
    @DisplayName("RateLimitMessages isRateLimitErrorMessage false for null")
    void isRateLimitErrorMessageNull() {
        assertFalse(messages.isRateLimitErrorMessage(null));
    }

    @Test
    @DisplayName("RateLimitMessages ClaudeAILimits Status enum")
    void claudeAiLimitsStatusEnum() {
        RateLimitMessages.ClaudeAILimits.Status[] statuses = RateLimitMessages.ClaudeAILimits.Status.values();
        assertEquals(3, statuses.length);
    }

    @Test
    @DisplayName("RateLimitMessages ClaudeAILimits RateLimitType enum")
    void claudeAiLimitsRateLimitTypeEnum() {
        RateLimitMessages.ClaudeAILimits.RateLimitType[] types = RateLimitMessages.ClaudeAILimits.RateLimitType.values();
        assertEquals(5, types.length);
    }

    @Test
    @DisplayName("RateLimitMessages ClaudeAILimits OverageDisabledReason enum")
    void claudeAiLimitsOverageDisabledReasonEnum() {
        RateLimitMessages.ClaudeAILimits.OverageDisabledReason[] reasons =
            RateLimitMessages.ClaudeAILimits.OverageDisabledReason.values();
        assertEquals(3, reasons.length);
    }

    @Test
    @DisplayName("RateLimitMessages ClaudeAILimits record")
    void claudeAiLimitsRecord() {
        Instant resetTime = Instant.now().plus(Duration.ofHours(1));
        RateLimitMessages.ClaudeAILimits limits = new RateLimitMessages.ClaudeAILimits(
            RateLimitMessages.ClaudeAILimits.Status.ALLOWED,
            RateLimitMessages.ClaudeAILimits.RateLimitType.SEVEN_DAY,
            0.5,
            resetTime,
            null,
            false,
            null,
            null
        );

        assertEquals(RateLimitMessages.ClaudeAILimits.Status.ALLOWED, limits.status());
        assertEquals(RateLimitMessages.ClaudeAILimits.RateLimitType.SEVEN_DAY, limits.rateLimitType());
        assertEquals(0.5, limits.utilization());
        assertEquals(resetTime, limits.resetsAt());
        assertFalse(limits.isUsingOverage());
    }

    @Test
    @DisplayName("RateLimitMessages formatResetTime hours and minutes")
    void formatResetTimeHoursMinutes() {
        Instant resetTime = Instant.now().plus(Duration.ofHours(2).plusMinutes(30));
        String formatted = messages.formatResetTime(resetTime, true);

        assertTrue(formatted.contains("2h"));
        assertTrue(formatted.contains("30m"));
    }

    @Test
    @DisplayName("RateLimitMessages formatResetTime minutes only")
    void formatResetTimeMinutesOnly() {
        Instant resetTime = Instant.now().plus(Duration.ofMinutes(45));
        String formatted = messages.formatResetTime(resetTime, true);

        assertTrue(formatted.contains("45m"));
        assertFalse(formatted.contains("h"));
    }

    @Test
    @DisplayName("RateLimitMessages formatResetTime soon")
    void formatResetTimeSoon() {
        Instant resetTime = Instant.now().plus(Duration.ofSeconds(30));
        String formatted = messages.formatResetTime(resetTime, true);

        assertEquals("soon", formatted);
    }

    @Test
    @DisplayName("RateLimitMessages formatResetTime null")
    void formatResetTimeNull() {
        String formatted = messages.formatResetTime(null, true);
        assertNull(formatted);
    }

    @Test
    @DisplayName("RateLimitMessages getRateLimitMessage allowed status returns null")
    void getRateLimitMessageAllowed() {
        RateLimitMessages.ClaudeAILimits limits = new RateLimitMessages.ClaudeAILimits(
            RateLimitMessages.ClaudeAILimits.Status.ALLOWED,
            null,
            null,
            null,
            null,
            false,
            null,
            null
        );

        assertNull(messages.getRateLimitMessage(limits, "claude-sonnet"));
    }

    @Test
    @DisplayName("RateLimitMessages getRateLimitMessage rejected status returns error")
    void getRateLimitMessageRejected() {
        RateLimitMessages.ClaudeAILimits limits = new RateLimitMessages.ClaudeAILimits(
            RateLimitMessages.ClaudeAILimits.Status.REJECTED,
            RateLimitMessages.ClaudeAILimits.RateLimitType.SEVEN_DAY,
            1.0,
            Instant.now().plus(Duration.ofHours(1)),
            null,
            false,
            null,
            null
        );

        RateLimitMessages.RateLimitMessage msg = messages.getRateLimitMessage(limits, "claude-sonnet");
        assertNotNull(msg);
        assertEquals(RateLimitMessages.Severity.ERROR, msg.severity());
        assertTrue(msg.message().contains("limit"));
    }

    @Test
    @DisplayName("RateLimitMessages getRateLimitErrorMessage returns error message")
    void getRateLimitErrorMessage() {
        RateLimitMessages.ClaudeAILimits limits = new RateLimitMessages.ClaudeAILimits(
            RateLimitMessages.ClaudeAILimits.Status.REJECTED,
            RateLimitMessages.ClaudeAILimits.RateLimitType.FIVE_HOUR,
            1.0,
            null,
            null,
            false,
            null,
            null
        );

        String errorMsg = messages.getRateLimitErrorMessage(limits, "claude-sonnet");
        assertNotNull(errorMsg);
    }

    @Test
    @DisplayName("RateLimitMessages getRateLimitWarning returns warning")
    void getRateLimitWarning() {
        RateLimitMessages.ClaudeAILimits limits = new RateLimitMessages.ClaudeAILimits(
            RateLimitMessages.ClaudeAILimits.Status.ALLOWED_WARNING,
            RateLimitMessages.ClaudeAILimits.RateLimitType.SEVEN_DAY,
            0.8,
            Instant.now().plus(Duration.ofHours(24)),
            null,
            false,
            null,
            null
        );

        String warning = messages.getRateLimitWarning(limits, "claude-sonnet");
        assertNotNull(warning);
        assertTrue(warning.contains("used"));
    }

    @Test
    @DisplayName("RateLimitMessages getUsingOverageText")
    void getUsingOverageText() {
        RateLimitMessages.ClaudeAILimits limits = new RateLimitMessages.ClaudeAILimits(
            RateLimitMessages.ClaudeAILimits.Status.ALLOWED,
            RateLimitMessages.ClaudeAILimits.RateLimitType.SEVEN_DAY,
            null,
            Instant.now().plus(Duration.ofHours(24)),
            null,
            true,
            null,
            null
        );

        String text = messages.getUsingOverageText(limits);
        assertTrue(text.contains("extra usage"));
    }
}