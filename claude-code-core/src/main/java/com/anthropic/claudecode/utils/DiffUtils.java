/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code diff utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;

/**
 * Diff utilities for file comparison.
 */
public final class DiffUtils {
    private DiffUtils() {}

    public static final int CONTEXT_LINES = 3;
    public static final long DIFF_TIMEOUT_MS = 5_000;

    /**
     * Structured patch hunk.
     */
    public record PatchHunk(
            int oldStart,
            int oldLines,
            int newStart,
            int newLines,
            List<String> lines
    ) {}

    /**
     * Adjust hunk line numbers by offset.
     */
    public static List<PatchHunk> adjustHunkLineNumbers(List<PatchHunk> hunks, int offset) {
        if (offset == 0) return hunks;

        return hunks.stream()
                .map(h -> new PatchHunk(
                        h.oldStart() + offset,
                        h.oldLines(),
                        h.newStart() + offset,
                        h.newLines(),
                        h.lines()
                ))
                .toList();
    }

    /**
     * Count lines changed in a patch.
     */
    public static LinesChanged countLinesChanged(List<PatchHunk> patch) {
        int additions = 0;
        int removals = 0;

        for (PatchHunk hunk : patch) {
            for (String line : hunk.lines()) {
                if (line.startsWith("+") && !line.startsWith("++")) {
                    additions++;
                } else if (line.startsWith("-") && !line.startsWith("--")) {
                    removals++;
                }
            }
        }

        return new LinesChanged(additions, removals);
    }

    /**
     * Get patch from contents.
     */
    public static List<PatchHunk> getPatchFromContents(String filePath, String oldContent, String newContent) {
        return getPatchFromContents(filePath, oldContent, newContent, false);
    }

    /**
     * Get patch from contents with options.
     */
    public static List<PatchHunk> getPatchFromContents(String filePath, String oldContent, String newContent, boolean ignoreWhitespace) {
        if (oldContent.equals(newContent)) {
            return List.of();
        }

        String[] oldLines = oldContent.split("\n", -1);
        String[] newLines = newContent.split("\n", -1);

        List<PatchHunk> hunks = new ArrayList<>();
        int oldLine = 0;
        int newLine = 0;

        while (oldLine < oldLines.length || newLine < newLines.length) {
            // Find the next difference
            while (oldLine < oldLines.length && newLine < newLines.length &&
                   linesEqual(oldLines[oldLine], newLines[newLine], ignoreWhitespace)) {
                oldLine++;
                newLine++;
            }

            if (oldLine >= oldLines.length && newLine >= newLines.length) {
                break;
            }

            // Collect the diff block
            List<String> diffLines = new ArrayList<>();
            int startOld = oldLine + 1;
            int startNew = newLine + 1;

            while (oldLine < oldLines.length && newLine < newLines.length &&
                   !linesEqual(oldLines[oldLine], newLines[newLine], ignoreWhitespace)) {
                diffLines.add("-" + oldLines[oldLine]);
                oldLine++;
            }

            while (newLine < newLines.length) {
                if (oldLine < oldLines.length && linesEqual(oldLines[oldLine], newLines[newLine], ignoreWhitespace)) {
                    break;
                }
                diffLines.add("+" + newLines[newLine]);
                newLine++;
            }

            if (!diffLines.isEmpty()) {
                hunks.add(new PatchHunk(startOld, countOldLines(diffLines), startNew, countNewLines(diffLines), diffLines));
            }
        }

        return hunks;
    }

    private static boolean linesEqual(String oldLine, String newLine, boolean ignoreWhitespace) {
        if (ignoreWhitespace) {
            return oldLine.trim().equals(newLine.trim());
        }
        return oldLine.equals(newLine);
    }

    private static int countOldLines(List<String> lines) {
        return (int) lines.stream().filter(l -> l.startsWith("-")).count();
    }

    private static int countNewLines(List<String> lines) {
        return (int) lines.stream().filter(l -> l.startsWith("+")).count();
    }

    /**
     * Lines changed record.
     */
    public record LinesChanged(int additions, int removals) {}
}