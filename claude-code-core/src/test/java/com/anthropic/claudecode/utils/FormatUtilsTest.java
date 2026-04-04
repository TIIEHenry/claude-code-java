/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FormatUtils.
 */
class FormatUtilsTest {

    @Test
    @DisplayName("formatNumber adds thousands separator")
    void formatNumberWorks() {
        assertEquals("1,000", FormatUtils.formatNumber(1000));
        assertEquals("1,000,000", FormatUtils.formatNumber(1000000));
        assertEquals("0", FormatUtils.formatNumber(0));
        assertEquals("123", FormatUtils.formatNumber(123));
    }

    @Test
    @DisplayName("formatDecimal adds separators and decimals")
    void formatDecimalWorks() {
        assertTrue(FormatUtils.formatDecimal(1000.5).contains("1,000"));
        assertTrue(FormatUtils.formatDecimal(0.5).contains("0.50"));
    }

    @Test
    @DisplayName("formatTokens uses K and M abbreviations")
    void formatTokensWorks() {
        assertEquals("500", FormatUtils.formatTokens(500));
        assertTrue(FormatUtils.formatTokens(1500).contains("K"));
        assertTrue(FormatUtils.formatTokens(1500000).contains("M"));
    }

    @Test
    @DisplayName("formatCurrency adds dollar sign")
    void formatCurrencyWorks() {
        assertTrue(FormatUtils.formatCurrency(10.50).startsWith("$"));
        assertTrue(FormatUtils.formatCurrency(0).startsWith("$"));
    }

    @Test
    @DisplayName("formatDuration formats correctly")
    void formatDurationWorks() {
        assertEquals("500ms", FormatUtils.formatDuration(500));
        assertTrue(FormatUtils.formatDuration(5000).contains("s"));
        assertTrue(FormatUtils.formatDuration(90000).contains("m"));
    }

    @Test
    @DisplayName("formatSize uses KB and MB")
    void formatSizeWorks() {
        assertEquals("500 B", FormatUtils.formatSize(500));
        assertTrue(FormatUtils.formatSize(2048).contains("KB"));
        assertTrue(FormatUtils.formatSize(2_000_000).contains("MB"));
        assertTrue(FormatUtils.formatSize(2_000_000_000).contains("GB"));
    }

    @Test
    @DisplayName("formatPercent formats percentage")
    void formatPercentWorks() {
        assertTrue(FormatUtils.formatPercent(0.5).contains("50"));
        assertTrue(FormatUtils.formatPercent(1.0).contains("100"));
    }
}