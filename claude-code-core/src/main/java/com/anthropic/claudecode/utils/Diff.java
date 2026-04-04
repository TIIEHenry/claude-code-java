/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code diff utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Diff utilities for computing and displaying file differences.
 */
public final class Diff {
    private Diff() {}

    public static final int CONTEXT_LINES = 3;
    public static final int DIFF_TIMEOUT_MS = 5_000;

    /**
     * Represents a diff hunk.
     */
    public record DiffHunk(
            int oldStart,
            int oldLines,
            int newStart,
            int newLines,
            List<String> lines
    ) {}

    // Tokens for escaping special characters
    private static final String AMPERSAND_TOKEN = "<<:AMPERSAND_TOKEN:>>";
    private static final String DOLLAR_TOKEN = "<<:DOLLAR_TOKEN:>>";

    /**
     * Escape special characters for diff computation.
     */
    private static String escapeForDiff(String s) {
        return s.replace("&", AMPERSAND_TOKEN).replace("$", DOLLAR_TOKEN);
    }

    /**
     * Unescape special characters after diff computation.
     */
    private static String unescapeFromDiff(String s) {
        return s.replace(AMPERSAND_TOKEN, "&").replace(DOLLAR_TOKEN, "$");
    }

    /**
     * Count lines added and removed in a patch.
     */
    public static int[] countLinesChanged(List<DiffHunk> hunks) {
        int additions = 0;
        int removals = 0;

        for (DiffHunk hunk : hunks) {
            for (String line : hunk.lines()) {
                if (line.startsWith("+") && !line.startsWith("+++")) {
                    additions++;
                } else if (line.startsWith("-") && !line.startsWith("---")) {
                    removals++;
                }
            }
        }

        return new int[]{additions, removals};
    }

    /**
     * Count lines changed for a new file.
     */
    public static int countNewFileLines(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.split("\n").length;
    }

    /**
     * Shift hunk line numbers by offset.
     */
    public static List<DiffHunk> adjustHunkLineNumbers(List<DiffHunk> hunks, int offset) {
        if (offset == 0) return hunks;

        List<DiffHunk> result = new ArrayList<>();
        for (DiffHunk hunk : hunks) {
            result.add(new DiffHunk(
                    hunk.oldStart() + offset,
                    hunk.oldLines(),
                    hunk.newStart() + offset,
                    hunk.newLines(),
                    hunk.lines()
            ));
        }
        return result;
    }

    /**
     * Create a simple diff between two strings.
     */
    public static List<DiffHunk> createDiff(String oldContent, String newContent) {
        return createDiff(oldContent, newContent, CONTEXT_LINES);
    }

    /**
     * Create a diff between two strings with specified context lines.
     */
    public static List<DiffHunk> createDiff(String oldContent, String newContent, int contextLines) {
        if (oldContent == null) oldContent = "";
        if (newContent == null) newContent = "";

        String[] oldLines = oldContent.split("\n", -1);
        String[] newLines = newContent.split("\n", -1);

        List<DiffHunk> hunks = new ArrayList<>();

        // Simple line-by-line diff algorithm
        int oldIdx = 0;
        int newIdx = 0;
        int oldStart = 1;
        int newStart = 1;

        List<String> currentHunkLines = new ArrayList<>();
        int hunkOldStart = 1;
        int hunkNewStart = 1;
        int hunkOldLines = 0;
        int hunkNewLines = 0;

        while (oldIdx < oldLines.length || newIdx < newLines.length) {
            if (oldIdx < oldLines.length && newIdx < newLines.length && oldLines[oldIdx].equals(newLines[newIdx])) {
                // Lines match - context line
                if (!currentHunkLines.isEmpty()) {
                    currentHunkLines.add(" " + oldLines[oldIdx]);
                    hunkOldLines++;
                    hunkNewLines++;
                }
                oldIdx++;
                newIdx++;
            } else {
                // Lines differ
                if (currentHunkLines.isEmpty()) {
                    hunkOldStart = oldIdx + 1;
                    hunkNewStart = newIdx + 1;
                }

                if (oldIdx < oldLines.length && (newIdx >= newLines.length || !oldLines[oldIdx].equals(newLines[newIdx]))) {
                    currentHunkLines.add("-" + oldLines[oldIdx]);
                    hunkOldLines++;
                    oldIdx++;
                }

                if (newIdx < newLines.length && (oldIdx >= oldLines.length || !oldLines[Math.min(oldIdx, oldLines.length - 1)].equals(newLines[newIdx]))) {
                    currentHunkLines.add("+" + newLines[newIdx]);
                    hunkNewLines++;
                    newIdx++;
                }
            }

            // Flush hunk if we have enough context after changes
            if (!currentHunkLines.isEmpty()) {
                boolean hasChanges = currentHunkLines.stream().anyMatch(l -> l.startsWith("+") || l.startsWith("-"));
                int contextCount = 0;
                for (int i = currentHunkLines.size() - 1; i >= 0; i--) {
                    if (currentHunkLines.get(i).startsWith(" ")) {
                        contextCount++;
                    } else {
                        break;
                    }
                }

                if (hasChanges && contextCount >= contextLines && oldIdx < oldLines.length && newIdx < newLines.length && oldLines[oldIdx].equals(newLines[newIdx])) {
                    hunks.add(new DiffHunk(hunkOldStart, hunkOldLines, hunkNewStart, hunkNewLines, new ArrayList<>(currentHunkLines)));
                    currentHunkLines.clear();
                    hunkOldLines = 0;
                    hunkNewLines = 0;
                }
            }
        }

        // Add final hunk
        if (!currentHunkLines.isEmpty() && currentHunkLines.stream().anyMatch(l -> l.startsWith("+") || l.startsWith("-"))) {
            hunks.add(new DiffHunk(hunkOldStart, hunkOldLines, hunkNewStart, hunkNewLines, new ArrayList<>(currentHunkLines)));
        }

        return hunks;
    }

    /**
     * Format a diff hunk as a string.
     */
    public static String formatHunk(DiffHunk hunk) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("@@ -%d,%d +%d,%d @@\n",
                hunk.oldStart(), hunk.oldLines(),
                hunk.newStart(), hunk.newLines()));
        for (String line : hunk.lines()) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    /**
     * Format all hunks as a unified diff.
     */
    public static String formatDiff(List<DiffHunk> hunks, String oldPath, String newPath) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- ").append(oldPath).append("\n");
        sb.append("+++ ").append(newPath).append("\n");
        for (DiffHunk hunk : hunks) {
            sb.append(formatHunk(hunk));
        }
        return sb.toString();
    }

    /**
     * Apply a simple edit to content.
     */
    public static String applyEdit(String content, String oldString, String newString, boolean replaceAll) {
        if (content == null) return content;
        if (oldString == null || oldString.isEmpty()) return content;

        if (replaceAll) {
            return content.replace(oldString, newString != null ? newString : "");
        } else {
            return content.replaceFirst(Pattern.quote(oldString), newString != null ? newString : "");
        }
    }
}