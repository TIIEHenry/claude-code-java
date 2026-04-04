/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code text utilities
 */
package com.anthropic.claudecode.utils.text;

import java.util.*;
import java.util.regex.*;

/**
 * Text processing utilities.
 */
public final class TextUtils {
    private TextUtils() {}

    /**
     * Truncate text to a maximum length.
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Truncate with ellipsis in the middle.
     */
    public static String truncateMiddle(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        int half = (maxLength - 3) / 2;
        return text.substring(0, half) + "..." + text.substring(text.length() - half);
    }

    /**
     * Word wrap text to a maximum line width.
     */
    public static List<String> wordWrap(String text, int width) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else if (currentLine.length() + 1 + word.length() <= width) {
                currentLine.append(" ").append(word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    /**
     * Indent text with a prefix.
     */
    public static String indent(String text, String prefix) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder sb = new StringBuilder();
        for (String line : text.split("\n")) {
            sb.append(prefix).append(line).append("\n");
        }

        // Remove trailing newline
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * Strip ANSI escape codes from text.
     */
    public static String stripAnsi(String text) {
        if (text == null) return null;

        // Pattern for ANSI escape sequences
        Pattern ansiPattern = Pattern.compile("\u001B\\[[;\\d]*m");
        return ansiPattern.matcher(text).replaceAll("");
    }

    /**
     * Strip leading and trailing blank lines.
     */
    public static String stripBlankLines(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String[] lines = text.split("\n");
        int start = 0;
        int end = lines.length;

        while (start < end && lines[start].isBlank()) {
            start++;
        }

        while (end > start && lines[end - 1].isBlank()) {
            end--;
        }

        if (start >= end) {
            return "";
        }

        return String.join("\n", Arrays.copyOfRange(lines, start, end));
    }

    /**
     * Normalize line endings.
     */
    public static String normalizeLineEndings(String text) {
        if (text == null) return null;
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    /**
     * Count lines in text.
     */
    public static int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int count = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                count++;
            }
        }

        return count;
    }

    /**
     * Get first N lines.
     */
    public static String firstLines(String text, int n) {
        if (text == null || n <= 0) {
            return "";
        }

        String[] lines = text.split("\n", n + 1);
        if (lines.length <= n) {
            return text;
        }

        return String.join("\n", Arrays.copyOf(lines, n)) + "\n...";
    }

    /**
     * Get last N lines.
     */
    public static String lastLines(String text, int n) {
        if (text == null || n <= 0) {
            return "";
        }

        String[] lines = text.split("\n");
        if (lines.length <= n) {
            return text;
        }

        return "...\n" + String.join("\n", Arrays.copyOfRange(lines, lines.length - n, lines.length));
    }

    /**
     * Check if text matches a pattern.
     */
    public static boolean matches(String text, String pattern) {
        if (text == null || pattern == null) {
            return false;
        }

        // Simple glob-like pattern matching
        String regex = pattern.replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");

        return Pattern.matches(regex, text);
    }

    /**
     * Find all matches of a pattern.
     */
    public static List<String> findAll(String text, String pattern) {
        if (text == null || pattern == null) {
            return List.of();
        }

        List<String> matches = new ArrayList<>();
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);

        while (m.find()) {
            matches.add(m.group());
        }

        return matches;
    }

    /**
     * Pluralize a word.
     */
    public static String pluralize(int count, String singular) {
        return pluralize(count, singular, singular + "s");
    }

    /**
     * Pluralize a word with explicit plural form.
     */
    public static String pluralize(int count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }

    /**
     * Format a list of items.
     */
    public static String formatList(List<String> items, String conjunction) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        if (items.size() == 1) {
            return items.get(0);
        }

        if (items.size() == 2) {
            return items.get(0) + " " + conjunction + " " + items.get(1);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size() - 1; i++) {
            if (i > 0) sb.append(", ");
            sb.append(items.get(i));
        }
        sb.append(", ").append(conjunction).append(" ").append(items.get(items.size() - 1));

        return sb.toString();
    }

    /**
     * Capitalize first letter.
     */
    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /**
     * Generate a random string.
     */
    public static String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }

    /**
     * Check if text is blank (null, empty, or whitespace only).
     */
    public static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    /**
     * Check if text is not blank.
     */
    public static boolean isNotBlank(String text) {
        return text != null && !text.isBlank();
    }

    /**
     * Default if blank.
     */
    public static String defaultIfBlank(String text, String defaultValue) {
        return isBlank(text) ? defaultValue : text;
    }

    /**
     * Join with separator.
     */
    public static String join(String separator, String... parts) {
        return String.join(separator, Arrays.stream(parts)
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .toList());
    }
}