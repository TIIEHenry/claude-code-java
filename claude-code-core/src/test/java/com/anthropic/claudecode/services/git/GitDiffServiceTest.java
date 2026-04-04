/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.git;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GitDiffService.
 */
class GitDiffServiceTest {

    @Test
    @DisplayName("GitDiffService DiffType enum values")
    void diffTypeEnum() {
        GitDiffService.DiffType[] types = GitDiffService.DiffType.values();
        assertEquals(4, types.length);
        assertEquals(GitDiffService.DiffType.WORKING_DIRECTORY, GitDiffService.DiffType.valueOf("WORKING_DIRECTORY"));
        assertEquals(GitDiffService.DiffType.STAGED, GitDiffService.DiffType.valueOf("STAGED"));
        assertEquals(GitDiffService.DiffType.COMMIT_TO_COMMIT, GitDiffService.DiffType.valueOf("COMMIT_TO_COMMIT"));
        assertEquals(GitDiffService.DiffType.BRANCH_TO_BRANCH, GitDiffService.DiffType.valueOf("BRANCH_TO_BRANCH"));
    }

    @Test
    @DisplayName("GitDiffService DiffLineType enum values")
    void diffLineTypeEnum() {
        GitDiffService.DiffLineType[] types = GitDiffService.DiffLineType.values();
        assertEquals(3, types.length);
        assertEquals("+", GitDiffService.DiffLineType.ADD.getPrefix());
        assertEquals("-", GitDiffService.DiffLineType.REMOVE.getPrefix());
        assertEquals(" ", GitDiffService.DiffLineType.CONTEXT.getPrefix());
    }

    @Test
    @DisplayName("GitDiffService DiffLine record")
    void diffLineRecord() {
        GitDiffService.DiffLine addLine = new GitDiffService.DiffLine(GitDiffService.DiffLineType.ADD, "new content");
        GitDiffService.DiffLine removeLine = new GitDiffService.DiffLine(GitDiffService.DiffLineType.REMOVE, "old content");
        GitDiffService.DiffLine contextLine = new GitDiffService.DiffLine(GitDiffService.DiffLineType.CONTEXT, "context");

        assertEquals(GitDiffService.DiffLineType.ADD, addLine.type());
        assertEquals("new content", addLine.content());
        assertEquals("+new content", addLine.format());
        assertEquals("-old content", removeLine.format());
        assertEquals(" context", contextLine.format());
    }

    @Test
    @DisplayName("GitDiffService HunkDiff record")
    void hunkDiffRecord() {
        List<GitDiffService.DiffLine> lines = List.of(
            new GitDiffService.DiffLine(GitDiffService.DiffLineType.CONTEXT, "line 1"),
            new GitDiffService.DiffLine(GitDiffService.DiffLineType.REMOVE, "old line"),
            new GitDiffService.DiffLine(GitDiffService.DiffLineType.ADD, "new line"),
            new GitDiffService.DiffLine(GitDiffService.DiffLineType.ADD, "another new")
        );

        GitDiffService.HunkDiff hunk = new GitDiffService.HunkDiff(10, 3, 10, 4, lines);

        assertEquals(10, hunk.oldStart());
        assertEquals(3, hunk.oldCount());
        assertEquals(10, hunk.newStart());
        assertEquals(4, hunk.newCount());
        assertEquals(4, hunk.lines().size());
        assertEquals(2, hunk.getAdditions());
        assertEquals(1, hunk.getDeletions());
        assertEquals("@@ -10,3 +10,4 @@", hunk.formatHeader());
    }

    @Test
    @DisplayName("GitDiffService FileDiff record")
    void fileDiffRecord() {
        GitDiffService.FileDiff normalDiff = new GitDiffService.FileDiff(
            "old.txt", "new.txt", List.of(), 5, 3
        );
        GitDiffService.FileDiff newFile = new GitDiffService.FileDiff(
            "/dev/null", "newfile.txt", List.of(), 10, 0
        );
        GitDiffService.FileDiff deletedFile = new GitDiffService.FileDiff(
            "deleted.txt", "/dev/null", List.of(), 0, 15
        );

        assertTrue(normalDiff.isModified());
        assertFalse(normalDiff.isNewFile());
        assertFalse(normalDiff.isDeletedFile());

        assertTrue(newFile.isNewFile());
        assertFalse(newFile.isModified());

        assertTrue(deletedFile.isDeletedFile());
        assertFalse(deletedFile.isModified());
    }

    @Test
    @DisplayName("GitDiffService FileDiffSummary record")
    void fileDiffSummaryRecord() {
        GitDiffService.FileDiffSummary summary = new GitDiffService.FileDiffSummary(
            "src/Main.java", 10, 5, false
        );

        assertEquals("src/Main.java", summary.path());
        assertEquals(10, summary.additions());
        assertEquals(5, summary.deletions());
        assertFalse(summary.isBinary());
        assertEquals("src/Main.java | 10 +++, 5 ---", summary.format());
    }

    @Test
    @DisplayName("GitDiffService DiffSummary record")
    void diffSummaryRecord() {
        List<GitDiffService.FileDiffSummary> files = List.of(
            new GitDiffService.FileDiffSummary("file1.java", 10, 5, false),
            new GitDiffService.FileDiffSummary("file2.java", 20, 10, false)
        );

        GitDiffService.DiffSummary summary = new GitDiffService.DiffSummary(files, 30, 15, 2);

        assertEquals(2, summary.filesChanged());
        assertEquals(30, summary.totalAdditions());
        assertEquals(15, summary.totalDeletions());
        assertEquals("2 files changed, 30 insertions(+), 15 deletions(-)", summary.format());
    }

    @Test
    @DisplayName("GitDiffService parseDiff parses simple diff")
    void parseDiffSimple() {
        // Simple diff with one hunk
        String diffOutput = """
            @@ -1,3 +1,4 @@
             line 1
            -line 2
            +line 2 modified
            +line 3
             line 4
            """;

        GitDiffService service = new GitDiffService(null);
        List<GitDiffService.HunkDiff> hunks = service.parseDiff(diffOutput);

        assertEquals(1, hunks.size());

        GitDiffService.HunkDiff hunk = hunks.get(0);
        assertEquals(1, hunk.oldStart());
        assertEquals(3, hunk.oldCount());
        assertEquals(1, hunk.newStart());
        assertEquals(4, hunk.newCount());

        assertEquals(2, hunk.getAdditions());
        assertEquals(1, hunk.getDeletions());
    }

    @Test
    @DisplayName("GitDiffService parseDiff parses multiple hunks")
    void parseDiffMultipleHunks() {
        String diffOutput = """
            @@ -1,5 +1,5 @@
             context
            -removed
            +added
             context
            @@ -10,3 +10,4 @@
             context
            +new line
            """;

        GitDiffService service = new GitDiffService(null);
        List<GitDiffService.HunkDiff> hunks = service.parseDiff(diffOutput);

        assertEquals(2, hunks.size());

        assertEquals(1, hunks.get(0).oldStart());
        assertEquals(1, hunks.get(0).getAdditions());
        assertEquals(1, hunks.get(0).getDeletions());

        assertEquals(10, hunks.get(1).oldStart());
        assertEquals(1, hunks.get(1).getAdditions());
        assertEquals(0, hunks.get(1).getDeletions());
    }

    @Test
    @DisplayName("GitDiffService parseDiff handles empty input")
    void parseDiffEmpty() {
        GitDiffService service = new GitDiffService(null);
        List<GitDiffService.HunkDiff> hunks = service.parseDiff("");

        assertTrue(hunks.isEmpty());
    }

    @Test
    @DisplayName("GitDiffService getFileDiff returns placeholder")
    void getFileDiff() {
        GitDiffService service = new GitDiffService(null);
        GitDiffService.FileDiff diff = service.getFileDiff(
            java.nio.file.Paths.get("test.txt"),
            GitDiffService.DiffType.WORKING_DIRECTORY
        );

        assertNotNull(diff);
        assertEquals("test.txt", diff.newPath());
    }

    @Test
    @DisplayName("GitDiffService getDiffSummary returns placeholder")
    void getDiffSummary() {
        GitDiffService service = new GitDiffService(null);
        GitDiffService.DiffSummary summary = service.getDiffSummary(GitDiffService.DiffType.STAGED);

        assertNotNull(summary);
        assertEquals(0, summary.filesChanged());
        assertEquals(0, summary.totalAdditions());
        assertEquals(0, summary.totalDeletions());
    }
}