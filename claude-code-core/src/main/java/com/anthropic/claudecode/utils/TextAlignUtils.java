/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code text align utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Text alignment utilities.
 */
public final class TextAlignUtils {
    private TextAlignUtils() {}

    /**
     * Align left.
     */
    public static String left(String text, int width) {
        if (text.length() >= width) return text.substring(0, width);
        return text + " ".repeat(width - text.length());
    }

    /**
     * Align right.
     */
    public static String right(String text, int width) {
        if (text.length() >= width) return text.substring(text.length() - width);
        return " ".repeat(width - text.length()) + text;
    }

    /**
     * Align center.
     */
    public static String center(String text, int width) {
        if (text.length() >= width) return text.substring(0, width);
        int leftPad = (width - text.length()) / 2;
        int rightPad = width - text.length() - leftPad;
        return " ".repeat(leftPad) + text + " ".repeat(rightPad);
    }

    /**
     * Justify text (align both sides).
     */
    public static String justify(String text, int width) {
        if (text.length() >= width) return text.substring(0, width);

        String[] words = text.split(" ");
        if (words.length == 1) return left(text, width);

        int totalChars = Arrays.stream(words).mapToInt(String::length).sum();
        int totalSpaces = width - totalChars;
        int gaps = words.length - 1;

        if (gaps == 0) return left(text, width);

        int baseSpaces = totalSpaces / gaps;
        int extraSpaces = totalSpaces % gaps;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            sb.append(words[i]);
            if (i < gaps) {
                int spaces = baseSpaces + (i < extraSpaces ? 1 : 0);
                sb.append(" ".repeat(spaces));
            }
        }
        return sb.toString();
    }

    /**
     * Wrap text to width.
     */
    public static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() + word.length() + 1 > width) {
                if (current.length() > 0) {
                    lines.add(current.toString().trim());
                    current = new StringBuilder();
                }
                if (word.length() > width) {
                    // Word too long, split it
                    for (int i = 0; i < word.length(); i += width) {
                        lines.add(word.substring(i, Math.min(i + width, word.length())));
                    }
                } else {
                    current.append(word).append(" ");
                }
            } else {
                current.append(word).append(" ");
            }
        }

        if (current.length() > 0) {
            lines.add(current.toString().trim());
        }

        return lines;
    }

    /**
     * Wrap and justify.
     */
    public static List<String> wrapJustify(String text, int width) {
        List<String> lines = wrap(text, width);
        if (lines.isEmpty()) return lines;

        // Don't justify last line
        for (int i = 0; i < lines.size() - 1; i++) {
            lines.set(i, justify(lines.get(i), width));
        }
        return lines;
    }

    /**
     * Indent lines.
     */
    public static String indent(String text, int spaces) {
        String indentStr = " ".repeat(spaces);
        return Arrays.stream(text.split("\n"))
            .map(line -> indentStr + line)
            .reduce((a, b) -> a + "\n" + b)
            .orElse("");
    }

    /**
     * Dedent lines (remove common leading whitespace).
     */
    public static String dedent(String text) {
        String[] lines = text.split("\n");

        // Find minimum common indentation
        int minIndent = Arrays.stream(lines)
            .filter(line -> !line.isBlank())
            .mapToInt(line -> {
                int count = 0;
                while (count < line.length() && Character.isWhitespace(line.charAt(count))) {
                    count++;
                }
                return count;
            })
            .min()
            .orElse(0);

        // Remove common indentation
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].length() >= minIndent) {
                sb.append(lines[i].substring(minIndent));
            } else {
                sb.append(lines[i]);
            }
            if (i < lines.length - 1) sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Truncate with ellipsis.
     */
    public static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        if (maxLength <= 3) return ".".repeat(maxLength);
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Truncate middle with ellipsis.
     */
    public static String truncateMiddle(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        if (maxLength <= 3) return ".".repeat(maxLength);
        int half = (maxLength - 3) / 2;
        return text.substring(0, half) + "..." + text.substring(text.length() - half);
    }

    /**
     * Pad both sides.
     */
    public static String pad(String text, int leftPad, int rightPad) {
        return " ".repeat(leftPad) + text + " ".repeat(rightPad);
    }

    /**
     * Repeat pattern.
     */
    public static String repeat(String pattern, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(pattern);
        }
        return sb.toString();
    }

    /**
     * Fit text to width (truncate or pad).
     */
    public static String fit(String text, int width) {
        if (text.length() > width) return truncate(text, width);
        return left(text, width);
    }

    /**
     * Align text block.
     */
    public static String alignBlock(String text, int width, Alignment alignment) {
        return switch (alignment) {
            case LEFT -> left(text, width);
            case RIGHT -> right(text, width);
            case CENTER -> center(text, width);
            case JUSTIFY -> justify(text, width);
        };
    }

    /**
     * Alignment enum.
     */
    public enum Alignment {
        LEFT, RIGHT, CENTER, JUSTIFY
    }

    /**
     * Column align.
     */
    public static List<String> alignColumns(List<List<String>> rows, List<Alignment> alignments, List<Integer> widths) {
        List<String> result = new ArrayList<>();

        for (List<String> row : rows) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < row.size(); i++) {
                String cell = row.get(i);
                int width = i < widths.size() ? widths.get(i) : 10;
                Alignment align = i < alignments.size() ? alignments.get(i) : Alignment.LEFT;

                sb.append(alignBlock(cell, width, align));
                if (i < row.size() - 1) sb.append(" ");
            }
            result.add(sb.toString());
        }

        return result;
    }
}