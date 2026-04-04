/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code git diff utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Git diff utilities for parsing diff output.
 */
public final class GitDiff {
    private GitDiff() {}

    private static final int GIT_TIMEOUT_MS = 5000;
    private static final int MAX_FILES = 50;
    private static final int MAX_DIFF_SIZE_BYTES = 1_000_000; // 1 MB
    private static final int MAX_LINES_PER_FILE = 400;
    private static final int MAX_FILES_FOR_DETAILS = 500;

    /**
     * Git diff stats.
     */
    public record GitDiffStats(int filesCount, int linesAdded, int linesRemoved) {}

    /**
     * Per-file stats.
     */
    public record PerFileStats(int added, int removed, boolean isBinary, boolean isUntracked) {}

    /**
     * Git diff result.
     */
    public record GitDiffResult(GitDiffStats stats, Map<String, PerFileStats> perFileStats, Map<String, List<DiffHunk>> hunks) {}

    /**
     * Diff hunk structure.
     */
    public record DiffHunk(int oldStart, int oldLines, int newStart, int newLines, List<String> lines) {}

    /**
     * Tool use diff structure.
     */
    public record ToolUseDiff(String filename, String status, int additions, int deletions, int changes, String patch, String repository) {}

    /**
     * Parse git diff --numstat output into stats.
     * Format: <added>\t<removed>\t<filename>
     */
    public static NumstatResult parseGitNumstat(String stdout) {
        String[] lines = stdout.trim().split("\n");
        int added = 0;
        int removed = 0;
        int validFileCount = 0;
        Map<String, PerFileStats> perFileStats = new LinkedHashMap<>();

        for (String line : lines) {
            if (line.isEmpty()) continue;

            String[] parts = line.split("\t");
            if (parts.length < 3) continue;

            validFileCount++;
            String addStr = parts[0];
            String remStr = parts[1];
            String filePath = Arrays.stream(parts).skip(2).reduce((a, b) -> a + "\t" + b).orElse("");

            boolean isBinary = addStr.equals("-") || remStr.equals("-");
            int fileAdded = isBinary ? 0 : parseIntSafe(addStr);
            int fileRemoved = isBinary ? 0 : parseIntSafe(remStr);

            added += fileAdded;
            removed += fileRemoved;

            if (perFileStats.size() < MAX_FILES) {
                perFileStats.put(filePath, new PerFileStats(fileAdded, fileRemoved, isBinary, false));
            }
        }

        return new NumstatResult(
                new GitDiffStats(validFileCount, added, removed),
                perFileStats
        );
    }

    /**
     * Numstat result.
     */
    public record NumstatResult(GitDiffStats stats, Map<String, PerFileStats> perFileStats) {}

    /**
     * Parse git diff --shortstat output into stats.
     * Format: " 1648 files changed, 52341 insertions(+), 8123 deletions(-)"
     */
    public static GitDiffStats parseShortstat(String stdout) {
        Pattern pattern = Pattern.compile(
                "(\\d+)\\s+files?\\s+changed(?:,\\s+(\\d+)\\s+insertions?\\(\\+\\))?(?:,\\s+(\\d+)\\s+deletions?\\(-\\))?"
        );
        Matcher matcher = pattern.matcher(stdout);
        if (!matcher.find()) return null;

        return new GitDiffStats(
                parseIntSafe(matcher.group(1)),
                parseIntSafe(matcher.group(2)),
                parseIntSafe(matcher.group(3))
        );
    }

    /**
     * Parse unified diff output into per-file hunks.
     */
    public static Map<String, List<DiffHunk>> parseGitDiff(String stdout) {
        Map<String, List<DiffHunk>> result = new LinkedHashMap<>();
        if (stdout == null || stdout.trim().isEmpty()) return result;

        // Split by file diffs
        String[] fileDiffs = stdout.split("^diff --git ", Pattern.MULTILINE);

        for (String fileDiff : fileDiffs) {
            if (fileDiff.isEmpty()) continue;

            // Stop after MAX_FILES
            if (result.size() >= MAX_FILES) break;

            // Skip files larger than 1MB
            if (fileDiff.length() > MAX_DIFF_SIZE_BYTES) continue;

            String[] lines = fileDiff.split("\n");

            // Extract filename from first line: "a/path/to/file b/path/to/file"
            Pattern headerPattern = Pattern.compile("^a/(.+?) b/(.+)$");
            if (lines.length == 0 || !headerPattern.matcher(lines[0]).find()) continue;

            Matcher headerMatcher = headerPattern.matcher(lines[0]);
            if (!headerMatcher.find()) continue;
            String filePath = headerMatcher.group(2);

            List<DiffHunk> fileHunks = new ArrayList<>();
            DiffHunk currentHunk = null;
            int lineCount = 0;

            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];

                // Hunk header: @@ -oldStart,oldLines +newStart,newLines @@
                Pattern hunkPattern = Pattern.compile("^@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@");
                Matcher hunkMatcher = hunkPattern.matcher(line);
                if (hunkMatcher.find()) {
                    if (currentHunk != null) {
                        fileHunks.add(currentHunk);
                    }
                    currentHunk = new DiffHunk(
                            parseIntSafe(hunkMatcher.group(1)),
                            parseIntSafeOrDefault(hunkMatcher.group(2), 1),
                            parseIntSafe(hunkMatcher.group(3)),
                            parseIntSafeOrDefault(hunkMatcher.group(4), 1),
                            new ArrayList<>()
                    );
                    continue;
                }

                // Skip metadata lines
                if (line.startsWith("index ") ||
                    line.startsWith("---") ||
                    line.startsWith("+++") ||
                    line.startsWith("new file") ||
                    line.startsWith("deleted file") ||
                    line.startsWith("old mode") ||
                    line.startsWith("new mode") ||
                    line.startsWith("Binary files")) {
                    continue;
                }

                // Add diff lines to current hunk
                if (currentHunk != null &&
                    (line.startsWith("+") ||
                     line.startsWith("-") ||
                     line.startsWith(" ") ||
                     line.isEmpty())) {

                    if (lineCount >= MAX_LINES_PER_FILE) continue;

                    currentHunk.lines().add(line);
                    lineCount++;
                }
            }

            if (currentHunk != null) {
                fileHunks.add(currentHunk);
            }

            if (!fileHunks.isEmpty()) {
                result.put(filePath, fileHunks);
            }
        }

        return result;
    }

    /**
     * Parse raw unified diff output into the structured ToolUseDiff format.
     */
    public static ToolUseDiff parseRawDiffToToolUseDiff(String filename, String rawDiff, String status, String repository) {
        String[] lines = rawDiff.split("\n");
        List<String> patchLines = new ArrayList<>();
        boolean inHunks = false;
        int additions = 0;
        int deletions = 0;

        for (String line : lines) {
            if (line.startsWith("@@")) {
                inHunks = true;
            }
            if (inHunks) {
                patchLines.add(line);
                if (line.startsWith("+") && !line.startsWith("+++")) {
                    additions++;
                } else if (line.startsWith("-") && !line.startsWith("---")) {
                    deletions++;
                }
            }
        }

        return new ToolUseDiff(
                filename,
                status,
                additions,
                deletions,
                additions + deletions,
                String.join("\n", patchLines),
                repository
        );
    }

    private static int parseIntSafe(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseIntSafeOrDefault(String value, int defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}