/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/ansiToPng
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;

/**
 * ANSI utilities - ANSI escape code handling.
 */
public final class AnsiUtils {
    // ANSI escape sequence patterns
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\u001B\\[[;\\d]*m");
    private static final Pattern ANSI_COLOR = Pattern.compile("\u001B\\[(3[0-7]|4[0-7]|38;5;\\d+|48;5;\\d+)m");
    private static final Pattern ANSI_RESET = Pattern.compile("\u001B\\[0m");
    private static final Pattern ANSI_CURSOR = Pattern.compile("\u001B\\[[ABCD]\\d*|\u001B\\[\\d*;\\d*H");
    private static final Pattern ANSI_CLEAR = Pattern.compile("\u001B\\[2J|\u001B\\[K");

    // Standard ANSI colors
    private static final String[] ANSI_COLORS = {
        "#000000", // Black
        "#CD0000", // Red
        "#00CD00", // Green
        "#CDCD00", // Yellow
        "#0000EE", // Blue
        "#CD00CD", // Magenta
        "#00CDCD", // Cyan
        "#E5E5E5", // White
        "#7F7F7F", // Bright Black
        "#FF0000", // Bright Red
        "#00FF00", // Bright Green
        "#FFFF00", // Bright Yellow
        "#5C5CFF", // Bright Blue
        "#FF00FF", // Bright Magenta
        "#00FFFF", // Bright Cyan
        "#FFFFFF"  // Bright White
    };

    /**
     * Strip all ANSI escape codes from text.
     */
    public static String stripAnsi(String text) {
        if (text == null) return null;
        return ANSI_ESCAPE.matcher(text).replaceAll("");
    }

    /**
     * Strip cursor and clear codes.
     */
    public static String stripControlCodes(String text) {
        if (text == null) return null;
        String result = ANSI_CURSOR.matcher(text).replaceAll("");
        return ANSI_CLEAR.matcher(result).replaceAll("");
    }

    /**
     * Check if text contains ANSI codes.
     */
    public static boolean containsAnsi(String text) {
        if (text == null) return false;
        return text.contains("\u001B[");
    }

    /**
     * Parse ANSI colors from text.
     */
    public static List<ColoredSegment> parseColors(String text) {
        List<ColoredSegment> segments = new ArrayList<>();
        if (text == null) return segments;

        StringBuilder currentText = new StringBuilder();
        String currentFg = null;
        String currentBg = null;

        Matcher matcher = ANSI_ESCAPE.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            // Add text before this escape
            if (matcher.start() > lastEnd) {
                currentText.append(text, lastEnd, matcher.start());
            }

            String escape = matcher.group();

            if (escape.equals("\u001B[0m")) {
                // Reset
                if (currentText.length() > 0) {
                    segments.add(new ColoredSegment(currentText.toString(), currentFg, currentBg));
                    currentText = new StringBuilder();
                }
                currentFg = null;
                currentBg = null;
            } else {
                // Parse color
                if (escape.contains("38;") || escape.matches("\u001B\\[3[0-7]m")) {
                    currentFg = parseColor(escape);
                } else if (escape.contains("48;") || escape.matches("\u001B\\[4[0-7]m")) {
                    currentBg = parseColor(escape);
                }
            }

            lastEnd = matcher.end();
        }

        // Add remaining text
        if (lastEnd < text.length()) {
            currentText.append(text.substring(lastEnd));
        }

        if (currentText.length() > 0) {
            segments.add(new ColoredSegment(currentText.toString(), currentFg, currentBg));
        }

        return segments;
    }

    /**
     * Convert ANSI to HTML.
     */
    public static String ansiToHtml(String text) {
        if (text == null) return "";
        if (!containsAnsi(text)) return escapeHtml(text);

        StringBuilder html = new StringBuilder();
        List<ColoredSegment> segments = parseColors(text);

        for (ColoredSegment segment : segments) {
            if (segment.fgColor() == null && segment.bgColor() == null) {
                html.append(escapeHtml(segment.text()));
            } else {
                html.append("<span style=\"");
                if (segment.fgColor() != null) {
                    html.append("color:").append(segment.fgColor()).append(";");
                }
                if (segment.bgColor() != null) {
                    html.append("background-color:").append(segment.bgColor()).append(";");
                }
                html.append("\">").append(escapeHtml(segment.text())).append("</span>");
            }
        }

        return html.toString();
    }

    private static String parseColor(String escape) {
        // Simple color parsing (30-37 foreground, 40-47 background)
        if (escape.matches(".*\\[3([0-7])m.*")) {
            Matcher m = Pattern.compile("\\[3([0-7])m").matcher(escape);
            if (m.find()) {
                int idx = Integer.parseInt(m.group(1));
                return ANSI_COLORS[idx];
            }
        }
        if (escape.matches(".*\\[4([0-7])m.*")) {
            Matcher m = Pattern.compile("\\[4([0-7])m").matcher(escape);
            if (m.find()) {
                int idx = Integer.parseInt(m.group(1));
                return ANSI_COLORS[idx];
            }
        }
        // Bright colors (90-97, 100-107)
        if (escape.matches(".*\\[9([0-7])m.*")) {
            Matcher m = Pattern.compile("\\[9([0-7])m").matcher(escape);
            if (m.find()) {
                int idx = Integer.parseInt(m.group(1)) + 8;
                return ANSI_COLORS[idx];
            }
        }
        return null;
    }

    private static String escapeHtml(String text) {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    /**
     * Colored segment record.
     */
    public record ColoredSegment(String text, String fgColor, String bgColor) {}
}