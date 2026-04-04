/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code token budget utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Token budget parsing utilities.
 *
 * Parses shorthand (+500k) and verbose (use/spend 2M tokens) budget specifications.
 */
public final class TokenBudget {
    private TokenBudget() {}

    // Patterns for matching budget specifications
    private static final Pattern SHORTHAND_START = Pattern.compile("^\\s*\\+(\\d+(?:\\.\\d+)?)\\s*(k|m|b)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHORTHAND_END = Pattern.compile("\\s\\+(\\d+(?:\\.\\d+)?)\\s*(k|m|b)\\s*[.!?]?\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VERBOSE = Pattern.compile("\\b(?:use|spend)\\s+(\\d+(?:\\.\\d+)?)\\s*(k|m|b)\\s*tokens?\\b", Pattern.CASE_INSENSITIVE);

    // Multipliers for suffixes
    private static final Map<String, Long> MULTIPLIERS = Map.of(
            "k", 1_000L,
            "m", 1_000_000L,
            "b", 1_000_000_000L
    );

    /**
     * Budget position record.
     */
    public record BudgetPosition(int start, int end) {}

    /**
     * Parse a budget match from value and suffix.
     */
    private static long parseBudgetMatch(String value, String suffix) {
        double num = Double.parseDouble(value);
        String lowerSuffix = suffix.toLowerCase();
        Long multiplier = MULTIPLIERS.get(lowerSuffix);
        if (multiplier == null) {
            return 0;
        }
        return (long) (num * multiplier);
    }

    /**
     * Parse token budget from text.
     * Returns null if no budget specification found.
     */
    public static Long parseTokenBudget(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        Matcher startMatch = SHORTHAND_START.matcher(text);
        if (startMatch.find()) {
            return parseBudgetMatch(startMatch.group(1), startMatch.group(2));
        }

        Matcher endMatch = SHORTHAND_END.matcher(text);
        if (endMatch.find()) {
            return parseBudgetMatch(endMatch.group(1), endMatch.group(2));
        }

        Matcher verboseMatch = VERBOSE.matcher(text);
        if (verboseMatch.find()) {
            return parseBudgetMatch(verboseMatch.group(1), verboseMatch.group(2));
        }

        return null;
    }

    /**
     * Find all token budget positions in text.
     */
    public static List<BudgetPosition> findTokenBudgetPositions(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<BudgetPosition> positions = new ArrayList<>();

        Matcher startMatch = SHORTHAND_START.matcher(text);
        if (startMatch.find()) {
            int offset = startMatch.start() + (startMatch.group(0).length() - startMatch.group(0).replaceAll("^\\s+", "").length());
            positions.add(new BudgetPosition(offset, startMatch.end()));
        }

        Matcher endMatch = SHORTHAND_END.matcher(text);
        if (endMatch.find()) {
            int endStart = endMatch.start() + 1; // +1: regex includes leading space
            boolean alreadyCovered = positions.stream()
                    .anyMatch(p -> endStart >= p.start() && endStart < p.end());
            if (!alreadyCovered) {
                positions.add(new BudgetPosition(endStart, endMatch.end()));
            }
        }

        Matcher verboseMatch = VERBOSE.matcher(text);
        while (verboseMatch.find()) {
            positions.add(new BudgetPosition(verboseMatch.start(), verboseMatch.end()));
        }

        return positions;
    }

    /**
     * Get budget continuation message.
     */
    public static String getBudgetContinuationMessage(int pct, long turnTokens, long budget) {
        return String.format("Stopped at %d%% of token target (%d / %d). Keep working \u2014 do not summarize.",
                pct, turnTokens, budget);
    }

    /**
     * Check if text has a budget specification.
     */
    public static boolean hasBudgetSpecification(String text) {
        return parseTokenBudget(text) != null;
    }

    /**
     * Format a number with thousand separators.
     */
    public static String formatNumber(long n) {
        return String.format("%,d", n);
    }

    /**
     * Calculate percentage of budget used.
     */
    public static int calculatePercentage(long used, long budget) {
        if (budget <= 0) {
            return 0;
        }
        return (int) ((used * 100) / budget);
    }

    /**
     * Check if budget is exceeded.
     */
    public static boolean isBudgetExceeded(long used, long budget) {
        return used >= budget;
    }

    /**
     * Get remaining tokens in budget.
     */
    public static long getRemainingTokens(long used, long budget) {
        return Math.max(0, budget - used);
    }
}