/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/stringUtils.ts
 */
package com.anthropic.claudecode.utils;

import java.util.regex.*;

/**
 * Extended string utilities from stringUtils.ts.
 */
public final class StringUtil {
    private StringUtil() {}

    /** Max string length to avoid memory issues */
    public static final int MAX_STRING_LENGTH = 1 << 25; // 2^25 = ~33MB

    /**
     * Escapes special regex characters in a string so it can be used as a literal
     * pattern in a Pattern constructor.
     */
    public static String escapeRegExp(String str) {
        // Escape special regex characters: . * + ? ^ $ { } ( ) | [ ] \
        return str.replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0");
    }

    /**
     * Returns the singular or plural form of a word based on count.
     *
     * @param n the count
     * @param word the singular word
     * @param pluralWord the plural word (defaults to word + "s")
     * @return the appropriate word form
     */
    public static String plural(long n, String word, String pluralWord) {
        if (pluralWord == null) {
            pluralWord = word + "s";
        }
        return n == 1 ? word : pluralWord;
    }

    /**
     * Returns the singular or plural form with default plural.
     */
    public static String plural(long n, String word) {
        return plural(n, word, word + "s");
    }

    /**
     * Returns the first line of a string.
     * Used for shebang detection in diff rendering.
     */
    public static String firstLineOf(String s) {
        if (s == null) return null;
        int nl = s.indexOf('\n');
        return nl == -1 ? s : s.substring(0, nl);
    }

    /**
     * Counts occurrences of char in string.
     */
    public static int countCharInString(String str, char ch) {
        if (str == null) return 0;
        int count = 0;
        int i = str.indexOf(ch);
        while (i != -1) {
            count++;
            i = str.indexOf(ch, i + 1);
        }
        return count;
    }

    /**
     * Normalize full-width (zenkaku) digits to half-width digits.
     * Useful for accepting input from Japanese/CJK IMEs.
     */
    public static String normalizeFullWidthDigits(String input) {
        if (input == null) return null;
        StringBuilder result = new StringBuilder();
        for (char ch : input.toCharArray()) {
            // Full-width digits: U+FF10-U+FF19 (０-９)
            if (ch >= '\uFF10' && ch <= '\uFF19') {
                result.append((char)(ch - 0xFEE0));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    /**
     * Normalize full-width (zenkaku) space to half-width space.
     * U+3000 → U+0020.
     */
    public static String normalizeFullWidthSpace(String input) {
        if (input == null) return null;
        return input.replace('\u3000', ' ');
    }

    /**
     * Safely joins an array of strings with a delimiter, truncating if the result exceeds maxSize.
     */
    public static String safeJoinLines(String[] lines, String delimiter, int maxSize) {
        String truncationMarker = "...[truncated]";
        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            String delimiterToAdd = result.length() > 0 ? delimiter : "";
            int fullAdditionLen = delimiterToAdd.length() + line.length();

            if (result.length() + fullAdditionLen <= maxSize) {
                result.append(delimiterToAdd).append(line);
            } else {
                int remainingSpace = maxSize - result.length() - delimiterToAdd.length() - truncationMarker.length();

                if (remainingSpace > 0) {
                    result.append(delimiterToAdd).append(line.substring(0, remainingSpace)).append(truncationMarker);
                } else {
                    result.append(truncationMarker);
                }
                return result.toString();
            }
        }
        return result.toString();
    }

    /**
     * Safe join with default delimiter and max size.
     */
    public static String safeJoinLines(String[] lines) {
        return safeJoinLines(lines, ",", MAX_STRING_LENGTH);
    }

    /**
     * Truncates text to a maximum number of lines, adding an ellipsis if truncated.
     */
    public static String truncateToLines(String text, int maxLines) {
        if (text == null) return null;
        String[] lines = text.split("\n", -1);
        if (lines.length <= maxLines) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            if (i > 0) result.append("\n");
            result.append(lines[i]);
        }
        result.append("…");
        return result.toString();
    }
}