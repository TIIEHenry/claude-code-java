/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * String utilities.
 */
public final class StringUtils {
    private StringUtils() {}

    /**
     * Check if string is null or empty.
     */
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Check if string is null or blank.
     */
    public static boolean isNullOrBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Truncate string.
     */
    public static String truncate(String s, int maxLength) {
        if (s == null || s.length() <= maxLength) {
            return s;
        }
        return s.substring(0, maxLength) + "...";
    }

    /**
     * Truncate with ellipsis in middle.
     */
    public static String truncateMiddle(String s, int maxLength) {
        if (s == null || s.length() <= maxLength) {
            return s;
        }
        int half = maxLength / 2 - 2;
        return s.substring(0, half) + "..." + s.substring(s.length() - half);
    }

    /**
     * Repeat string.
     */
    public static String repeat(String s, int count) {
        if (s == null || count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Capitalize first letter.
     */
    public static String capitalize(String s) {
        if (isNullOrEmpty(s)) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Escape for JSON.
     */
    public static String escapeJson(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Pad left.
     */
    public static String padLeft(String s, int length, char padChar) {
        if (s == null) s = "";
        if (s.length() >= length) return s;
        return repeat(String.valueOf(padChar), length - s.length()) + s;
    }

    /**
     * Pad right.
     */
    public static String padRight(String s, int length, char padChar) {
        if (s == null) s = "";
        if (s.length() >= length) return s;
        return s + repeat(String.valueOf(padChar), length - s.length());
    }
}