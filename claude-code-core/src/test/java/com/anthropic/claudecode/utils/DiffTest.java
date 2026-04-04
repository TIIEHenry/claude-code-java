/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Diff.
 */
class DiffTest {

    @Test
    @DisplayName("Diff CONTEXT_LINES is 3")
    void contextLines() {
        assertEquals(3, Diff.CONTEXT_LINES);
    }

    @Test
    @DisplayName("Diff DIFF_TIMEOUT_MS is 5000")
    void diffTimeout() {
        assertEquals(5000, Diff.DIFF_TIMEOUT_MS);
    }

    @Test
    @DisplayName("Diff DiffHunk record works")
    void diffHunkRecord() {
        List<String> lines = List.of("-old", "+new");
        Diff.DiffHunk hunk = new Diff.DiffHunk(1, 1, 1, 1, lines);

        assertEquals(1, hunk.oldStart());
        assertEquals(1, hunk.oldLines());
        assertEquals(1, hunk.newStart());
        assertEquals(1, hunk.newLines());
        assertEquals(2, hunk.lines().size());
    }

    @Test
    @DisplayName("Diff countLinesChanged counts additions and removals")
    void countLinesChanged() {
        List<String> lines = List.of("--- old", "+++ new", "@@ -1,1 +1,1 @@", "-removed", "+added");
        Diff.DiffHunk hunk = new Diff.DiffHunk(1, 1, 1, 1, lines);
        List<Diff.DiffHunk> hunks = List.of(hunk);

        int[] counts = Diff.countLinesChanged(hunks);

        assertEquals(1, counts[0]); // additions
        assertEquals(1, counts[1]); // removals
    }

    @Test
    @DisplayName("Diff countLinesChanged ignores file headers")
    void countLinesChangedIgnoresHeaders() {
        List<String> lines = List.of("--- old", "+++ new");
        Diff.DiffHunk hunk = new Diff.DiffHunk(1, 0, 1, 0, lines);
        List<Diff.DiffHunk> hunks = List.of(hunk);

        int[] counts = Diff.countLinesChanged(hunks);

        assertEquals(0, counts[0]);
        assertEquals(0, counts[1]);
    }

    @Test
    @DisplayName("Diff countNewFileLines counts lines")
    void countNewFileLines() {
        assertEquals(3, Diff.countNewFileLines("line1\nline2\nline3"));
    }

    @Test
    @DisplayName("Diff countNewFileLines handles null")
    void countNewFileLinesNull() {
        assertEquals(0, Diff.countNewFileLines(null));
    }

    @Test
    @DisplayName("Diff countNewFileLines handles empty")
    void countNewFileLinesEmpty() {
        assertEquals(0, Diff.countNewFileLines(""));
    }

    @Test
    @DisplayName("Diff adjustHunkLineNumbers shifts by offset")
    void adjustHunkLineNumbers() {
        List<String> lines = List.of("-old", "+new");
        Diff.DiffHunk hunk = new Diff.DiffHunk(1, 1, 1, 1, lines);
        List<Diff.DiffHunk> hunks = List.of(hunk);

        List<Diff.DiffHunk> adjusted = Diff.adjustHunkLineNumbers(hunks, 10);

        assertEquals(11, adjusted.get(0).oldStart());
        assertEquals(11, adjusted.get(0).newStart());
    }

    @Test
    @DisplayName("Diff adjustHunkLineNumbers no change for offset 0")
    void adjustHunkLineNumbersZero() {
        List<String> lines = List.of("-old", "+new");
        Diff.DiffHunk hunk = new Diff.DiffHunk(1, 1, 1, 1, lines);
        List<Diff.DiffHunk> hunks = List.of(hunk);

        List<Diff.DiffHunk> adjusted = Diff.adjustHunkLineNumbers(hunks, 0);

        assertEquals(1, adjusted.get(0).oldStart());
        assertEquals(1, adjusted.get(0).newStart());
    }

    @Test
    @DisplayName("Diff createDiff finds changes")
    void createDiffFindsChanges() {
        String oldContent = "line1\nline2\nline3";
        String newContent = "line1\nmodified\nline3";

        List<Diff.DiffHunk> hunks = Diff.createDiff(oldContent, newContent);

        assertFalse(hunks.isEmpty());
        assertTrue(hunks.get(0).lines().stream().anyMatch(l -> l.startsWith("-")));
        assertTrue(hunks.get(0).lines().stream().anyMatch(l -> l.startsWith("+")));
    }

    @Test
    @DisplayName("Diff createDiff returns empty for identical content")
    void createDiffIdentical() {
        String content = "line1\nline2\nline3";

        List<Diff.DiffHunk> hunks = Diff.createDiff(content, content);

        assertTrue(hunks.isEmpty());
    }

    @Test
    @DisplayName("Diff createDiff handles null old content")
    void createDiffNullOld() {
        List<Diff.DiffHunk> hunks = Diff.createDiff(null, "new content");

        assertFalse(hunks.isEmpty());
    }

    @Test
    @DisplayName("Diff createDiff handles null new content")
    void createDiffNullNew() {
        List<Diff.DiffHunk> hunks = Diff.createDiff("old content", null);

        assertFalse(hunks.isEmpty());
    }

    @Test
    @DisplayName("Diff formatHunk produces correct format")
    void formatHunkFormat() {
        List<String> lines = List.of("-removed", "+added");
        Diff.DiffHunk hunk = new Diff.DiffHunk(1, 1, 1, 1, lines);

        String formatted = Diff.formatHunk(hunk);

        assertTrue(formatted.startsWith("@@ -1,1 +1,1 @@"));
        assertTrue(formatted.contains("-removed"));
        assertTrue(formatted.contains("+added"));
    }

    @Test
    @DisplayName("Diff formatDiff produces unified diff")
    void formatDiffUnified() {
        List<String> lines = List.of("-old", "+new");
        Diff.DiffHunk hunk = new Diff.DiffHunk(1, 1, 1, 1, lines);
        List<Diff.DiffHunk> hunks = List.of(hunk);

        String formatted = Diff.formatDiff(hunks, "old.txt", "new.txt");

        assertTrue(formatted.contains("--- old.txt"));
        assertTrue(formatted.contains("+++ new.txt"));
        assertTrue(formatted.contains("@@"));
    }

    @Test
    @DisplayName("Diff applyEdit replaces single occurrence")
    void applyEditSingle() {
        String content = "Hello world, hello again";
        String result = Diff.applyEdit(content, "hello", "Hi", false);

        assertEquals("Hello world, Hi again", result);
    }

    @Test
    @DisplayName("Diff applyEdit replaces all occurrences")
    void applyEditAll() {
        String content = "hello hello hello";
        String result = Diff.applyEdit(content, "hello", "Hi", true);

        assertEquals("Hi Hi Hi", result);
    }

    @Test
    @DisplayName("Diff applyEdit handles null content")
    void applyEditNullContent() {
        assertNull(Diff.applyEdit(null, "old", "new", false));
    }

    @Test
    @DisplayName("Diff applyEdit handles empty old string")
    void applyEditEmptyOld() {
        String content = "unchanged";
        String result = Diff.applyEdit(content, "", "new", false);

        assertEquals("unchanged", result);
    }

    @Test
    @DisplayName("Diff applyEdit handles null new string")
    void applyEditNullNew() {
        String content = "Hello world";
        String result = Diff.applyEdit(content, "Hello ", null, false);

        assertEquals("world", result);
    }
}