/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code user prompt keyword utilities
 */
package com.anthropic.claudecode.utils;

import java.util.regex.Pattern;

/**
 * User prompt keyword pattern matching utilities.
 */
public final class UserPromptKeywords {
    private UserPromptKeywords() {}

    // Negative keyword pattern
    private static final Pattern NEGATIVE_PATTERN = Pattern.compile(
            "\\b(wtf|wth|ffs|omfg|shit(ty|tiest)?|dumbass|horrible|awful|" +
            "piss(ed|ing)? off|piece of (shit|crap|junk)|what the (fuck|hell)|" +
            "fucking? (broken|useless|terrible|awful|horrible)|fuck you|" +
            "screw (this|you)|so frustrating|this sucks|damn it)\\b"
    );

    // Keep going pattern
    private static final Pattern KEEP_GOING_PATTERN = Pattern.compile("\\b(keep going|go on)\\b");

    /**
     * Checks if input matches negative keyword patterns.
     */
    public static boolean matchesNegativeKeyword(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        String lowerInput = input.toLowerCase();
        return NEGATIVE_PATTERN.matcher(lowerInput).find();
    }

    /**
     * Checks if input matches keep going/continuation patterns.
     */
    public static boolean matchesKeepGoingKeyword(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        String lowerInput = input.toLowerCase().trim();

        // Match "continue" only if it's the entire prompt
        if (lowerInput.equals("continue")) {
            return true;
        }

        // Match "keep going" or "go on" anywhere in the input
        return KEEP_GOING_PATTERN.matcher(lowerInput).find();
    }

    /**
     * Check if input indicates frustration or negative sentiment.
     */
    public static boolean isFrustration(String input) {
        return matchesNegativeKeyword(input);
    }

    /**
     * Check if input is a continuation request.
     */
    public static boolean isContinuationRequest(String input) {
        return matchesKeepGoingKeyword(input);
    }

    /**
     * Check if input needs special handling.
     */
    public static boolean needsSpecialHandling(String input) {
        return matchesNegativeKeyword(input) || matchesKeepGoingKeyword(input);
    }

    /**
     * Get the matched negative keyword from input.
     */
    public static String findNegativeKeyword(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        String lowerInput = input.toLowerCase();
        java.util.regex.Matcher matcher = NEGATIVE_PATTERN.matcher(lowerInput);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /**
     * Check if the entire prompt is just "continue".
     */
    public static boolean isJustContinue(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return input.toLowerCase().trim().equals("continue");
    }
}