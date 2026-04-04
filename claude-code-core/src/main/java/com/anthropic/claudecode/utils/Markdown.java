/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code markdown utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;

/**
 * Markdown formatting utilities.
 */
public final class Markdown {
    private Markdown() {}

    // Issue reference pattern: owner/repo#NNN
    private static final Pattern ISSUE_REF_PATTERN = Pattern.compile(
            "(^|[^\\w./-])([A-Za-z0-9][\\w-]*/[A-Za-z0-9][\\w.-]*)#(\\d+)\\b"
    );

    // Roman numeral values
    private static final int[][] ROMAN_VALUES = {
            {1000, 'm'}, {900, 'c'}, {500, 'd'}, {400, 'c'},
            {100, 'c'}, {90, 'x'}, {50, 'l'}, {40, 'x'},
            {10, 'x'}, {9, 'i'}, {5, 'v'}, {4, 'i'}, {1, 'i'}
    };

    /**
     * Apply markdown formatting.
     */
    public static String applyMarkdown(String content) {
        // Simplified implementation - full version would parse markdown
        return content == null ? "" : content.trim();
    }

    /**
     * Linkify GitHub issue references.
     */
    public static String linkifyIssueReferences(String text) {
        if (text == null) return null;

        if (!Hyperlink.supportsHyperlinks()) {
            return text;
        }

        Matcher matcher = ISSUE_REF_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String prefix = matcher.group(1);
            String repo = matcher.group(2);
            String num = matcher.group(3);

            String url = "https://github.com/" + repo + "/issues/" + num;
            String linkText = repo + "#" + num;
            String hyperlink = Hyperlink.createHyperlink(url, linkText);

            matcher.appendReplacement(sb, prefix + hyperlink);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Convert number to letter (a, b, c...).
     */
    public static String numberToLetter(int n) {
        StringBuilder result = new StringBuilder();
        while (n > 0) {
            n--;
            result.insert(0, (char) ('a' + (n % 26)));
            n = n / 26;
        }
        return result.toString();
    }

    /**
     * Convert number to Roman numerals.
     */
    public static String numberToRoman(int n) {
        StringBuilder result = new StringBuilder();
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i"};

        for (int i = 0; i < values.length; i++) {
            while (n >= values[i]) {
                result.append(numerals[i]);
                n -= values[i];
            }
        }
        return result.toString();
    }

    /**
     * Get list number based on depth.
     */
    public static String getListNumber(int listDepth, int orderedListNumber) {
        switch (listDepth) {
            case 0:
            case 1:
                return String.valueOf(orderedListNumber);
            case 2:
                return numberToLetter(orderedListNumber);
            case 3:
                return numberToRoman(orderedListNumber);
            default:
                return String.valueOf(orderedListNumber);
        }
    }

    /**
     * Pad content according to alignment.
     */
    public static String padAligned(String content, int displayWidth, int targetWidth, String align) {
        int padding = Math.max(0, targetWidth - displayWidth);

        if ("center".equals(align)) {
            int leftPad = padding / 2;
            return " ".repeat(leftPad) + content + " ".repeat(padding - leftPad);
        }
        if ("right".equals(align)) {
            return " ".repeat(padding) + content;
        }
        return content + " ".repeat(padding);
    }

    /**
     * Strip ANSI codes from text.
     */
    public static String stripAnsi(String text) {
        if (text == null) return null;
        return text.replaceAll("\u001b\\[[;\\d]*[ -/]*[@-~]", "");
    }

    /**
     * Escape markdown special characters.
     */
    public static String escapeMarkdown(String text) {
        if (text == null) return null;
        return text.replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("`", "\\`")
                .replace("#", "\\#");
    }
}