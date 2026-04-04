/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/StructuredDiff/colorDiff
 */
package com.anthropic.claudecode.components.diff;

import java.util.*;
import java.util.regex.*;

/**
 * Color diff - Color utilities for diff output.
 */
public final class ColorDiff {
    private static final Pattern ANSI_PATTERN = Pattern.compile("\033\\[[0-9;]*m");
    private static final Pattern WORD_BOUNDARY = Pattern.compile("\\b");

    /**
     * Diff color enum.
     */
    public enum DiffColor {
        ADD("\033[32m", "green"),
        ADD_BG("\033[42m", "green-bg"),
        REMOVE("\033[31m", "red"),
        REMOVE_BG("\033[41m", "red-bg"),
        CONTEXT("\033[0m", "default"),
        HEADER("\033[36m", "cyan"),
        HIGHLIGHT("\033[7m", "reverse"),
        RESET("\033[0m", "reset");

        private final String ansi;
        private final String name;

        DiffColor(String ansi, String name) {
            this.ansi = ansi;
            this.name = name;
        }

        public String getAnsi() { return ansi; }
        public String getName() { return name; }
    }

    /**
     * Color added line.
     */
    public static String colorAdd(String line) {
        return DiffColor.ADD.getAnsi() + line + DiffColor.RESET.getAnsi();
    }

    /**
     * Color removed line.
     */
    public static String colorRemove(String line) {
        return DiffColor.REMOVE.getAnsi() + line + DiffColor.RESET.getAnsi();
    }

    /**
     * Color context line.
     */
    public static String colorContext(String line) {
        return line; // No coloring for context
    }

    /**
     * Color header line.
     */
    public static String colorHeader(String line) {
        return DiffColor.HEADER.getAnsi() + line + DiffColor.RESET.getAnsi();
    }

    /**
     * Highlight word differences.
     */
    public static String highlightWordDiff(String oldLine, String newLine) {
        List<String> oldWords = splitWords(oldLine);
        List<String> newWords = splitWords(newLine);

        StringBuilder oldResult = new StringBuilder();
        StringBuilder newResult = new StringBuilder();

        int maxLen = Math.max(oldWords.size(), newWords.size());
        for (int i = 0; i < maxLen; i++) {
            String oldWord = i < oldWords.size() ? oldWords.get(i) : "";
            String newWord = i < newWords.size() ? newWords.get(i) : "";

            if (!oldWord.equals(newWord)) {
                if (!oldWord.isEmpty()) {
                    oldResult.append(DiffColor.REMOVE_BG.getAnsi())
                             .append(oldWord)
                             .append(DiffColor.RESET.getAnsi());
                }
                if (!newWord.isEmpty()) {
                    newResult.append(DiffColor.ADD_BG.getAnsi())
                             .append(newWord)
                             .append(DiffColor.RESET.getAnsi());
                }
            } else {
                oldResult.append(oldWord);
                newResult.append(newWord);
            }
        }

        return oldResult + " -> " + newResult;
    }

    /**
     * Split into words.
     */
    private static List<String> splitWords(String line) {
        List<String> words = new ArrayList<>();
        Matcher matcher = WORD_BOUNDARY.matcher(line);

        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                words.add(line.substring(lastEnd, matcher.start()));
            }
            lastEnd = matcher.start();
        }

        if (lastEnd < line.length()) {
            words.add(line.substring(lastEnd));
        }

        return words;
    }

    /**
     * Strip ANSI codes.
     */
    public static String stripAnsi(String text) {
        return ANSI_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Get line length without ANSI.
     */
    public static int getVisibleLength(String text) {
        return stripAnsi(text).length();
    }

    /**
     * Apply side-by-side coloring.
     */
    public static String[] sideBySide(String oldLine, String newLine) {
        String coloredOld = DiffColor.REMOVE.getAnsi() + oldLine + DiffColor.RESET.getAnsi();
        String coloredNew = DiffColor.ADD.getAnsi() + newLine + DiffColor.RESET.getAnsi();

        return new String[] { coloredOld, coloredNew };
    }

    /**
     * Color scheme config.
     */
    public record ColorScheme(
        DiffColor addColor,
        DiffColor removeColor,
        DiffColor addHighlight,
        DiffColor removeHighlight,
        boolean useBackgroundHighlight
    ) {
        public static ColorScheme defaultScheme() {
            return new ColorScheme(
                DiffColor.ADD,
                DiffColor.REMOVE,
                DiffColor.ADD_BG,
                DiffColor.REMOVE_BG,
                true
            );
        }

        public static ColorScheme minimalScheme() {
            return new ColorScheme(
                DiffColor.ADD,
                DiffColor.REMOVE,
                DiffColor.HIGHLIGHT,
                DiffColor.HIGHLIGHT,
                false
            );
        }
    }
}