/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.components.diff;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for StructuredDiffTypes.
 */
@DisplayName("StructuredDiffTypes Tests")
class StructuredDiffTypesTest {

    @Test
    @DisplayName("DiffLineType enum has correct values")
    void diffLineTypeEnumHasCorrectValues() {
        StructuredDiffTypes.DiffLineType[] types = StructuredDiffTypes.DiffLineType.values();

        assertEquals(5, types.length);
        assertTrue(Arrays.asList(types).contains(StructuredDiffTypes.DiffLineType.CONTEXT));
        assertTrue(Arrays.asList(types).contains(StructuredDiffTypes.DiffLineType.ADD));
        assertTrue(Arrays.asList(types).contains(StructuredDiffTypes.DiffLineType.REMOVE));
        assertTrue(Arrays.asList(types).contains(StructuredDiffTypes.DiffLineType.HEADER));
        assertTrue(Arrays.asList(types).contains(StructuredDiffTypes.DiffLineType.NO_NEWLINE));
    }

    @Test
    @DisplayName("DiffLineType getPrefix returns correct prefix")
    void diffLineTypeGetPrefixReturnsCorrectPrefix() {
        assertEquals(" ", StructuredDiffTypes.DiffLineType.CONTEXT.getPrefix());
        assertEquals("+", StructuredDiffTypes.DiffLineType.ADD.getPrefix());
        assertEquals("-", StructuredDiffTypes.DiffLineType.REMOVE.getPrefix());
        assertEquals("@", StructuredDiffTypes.DiffLineType.HEADER.getPrefix());
        assertEquals("\\", StructuredDiffTypes.DiffLineType.NO_NEWLINE.getPrefix());
    }

    @Test
    @DisplayName("DiffLineType fromPrefix returns correct type")
    void diffLineTypeFromPrefixReturnsCorrectType() {
        assertEquals(StructuredDiffTypes.DiffLineType.CONTEXT, StructuredDiffTypes.DiffLineType.fromPrefix(' '));
        assertEquals(StructuredDiffTypes.DiffLineType.ADD, StructuredDiffTypes.DiffLineType.fromPrefix('+'));
        assertEquals(StructuredDiffTypes.DiffLineType.REMOVE, StructuredDiffTypes.DiffLineType.fromPrefix('-'));
        assertEquals(StructuredDiffTypes.DiffLineType.HEADER, StructuredDiffTypes.DiffLineType.fromPrefix('@'));
        assertEquals(StructuredDiffTypes.DiffLineType.NO_NEWLINE, StructuredDiffTypes.DiffLineType.fromPrefix('\\'));
        assertEquals(StructuredDiffTypes.DiffLineType.CONTEXT, StructuredDiffTypes.DiffLineType.fromPrefix('x'));
    }

    @Test
    @DisplayName("DiffLine record works correctly")
    void diffLineRecordWorksCorrectly() {
        StructuredDiffTypes.DiffLine line = new StructuredDiffTypes.DiffLine(
            StructuredDiffTypes.DiffLineType.ADD,
            "new line content",
            -1,
            10,
            "added"
        );

        assertEquals(StructuredDiffTypes.DiffLineType.ADD, line.type());
        assertEquals("new line content", line.content());
        assertEquals(-1, line.oldLineNumber());
        assertEquals(10, line.newLineNumber());
        assertEquals("added", line.annotation());
    }

    @Test
    @DisplayName("DiffLine type check methods work correctly")
    void diffLineTypeCheckMethodsWorkCorrectly() {
        StructuredDiffTypes.DiffLine addLine = new StructuredDiffTypes.DiffLine(
            StructuredDiffTypes.DiffLineType.ADD, "content", -1, 1, null);
        StructuredDiffTypes.DiffLine removeLine = new StructuredDiffTypes.DiffLine(
            StructuredDiffTypes.DiffLineType.REMOVE, "content", 1, -1, null);
        StructuredDiffTypes.DiffLine contextLine = new StructuredDiffTypes.DiffLine(
            StructuredDiffTypes.DiffLineType.CONTEXT, "content", 1, 1, null);
        StructuredDiffTypes.DiffLine headerLine = new StructuredDiffTypes.DiffLine(
            StructuredDiffTypes.DiffLineType.HEADER, "@@ -1,3 +1,4 @@", -1, -1, null);

        assertTrue(addLine.isAdd());
        assertFalse(addLine.isRemove());
        assertTrue(removeLine.isRemove());
        assertFalse(removeLine.isAdd());
        assertTrue(contextLine.isContext());
        assertTrue(headerLine.isHeader());
    }

    @Test
    @DisplayName("DiffLine format returns correct string")
    void diffLineFormatReturnsCorrectString() {
        StructuredDiffTypes.DiffLine line = new StructuredDiffTypes.DiffLine(
            StructuredDiffTypes.DiffLineType.ADD,
            "new code",
            -1,
            5,
            null
        );

        assertEquals("+new code", line.format());
    }

    @Test
    @DisplayName("DiffLine format with annotation")
    void diffLineFormatWithAnnotation() {
        StructuredDiffTypes.DiffLine line = new StructuredDiffTypes.DiffLine(
            StructuredDiffTypes.DiffLineType.ADD,
            "new code",
            -1,
            5,
            "important"
        );

        assertEquals("+new code [important]", line.format());
    }

    @Test
    @DisplayName("DiffHunk record works correctly")
    void diffHunkRecordWorksCorrectly() {
        List<StructuredDiffTypes.DiffLine> lines = List.of(
            new StructuredDiffTypes.DiffLine(StructuredDiffTypes.DiffLineType.CONTEXT, "ctx", 1, 1, null),
            new StructuredDiffTypes.DiffLine(StructuredDiffTypes.DiffLineType.ADD, "add", -1, 2, null),
            new StructuredDiffTypes.DiffLine(StructuredDiffTypes.DiffLineType.REMOVE, "remove", 2, -1, null)
        );

        StructuredDiffTypes.DiffHunk hunk = new StructuredDiffTypes.DiffHunk(
            1, 3, 1, 4, " function", lines
        );

        assertEquals(1, hunk.oldStart());
        assertEquals(3, hunk.oldCount());
        assertEquals(1, hunk.newStart());
        assertEquals(4, hunk.newCount());
        assertEquals(" function", hunk.header());
        assertEquals(3, hunk.lines().size());
    }

    @Test
    @DisplayName("DiffHunk getOldEnd and getNewEnd work correctly")
    void diffHunkGetEndsWorkCorrectly() {
        StructuredDiffTypes.DiffHunk hunk = new StructuredDiffTypes.DiffHunk(
            10, 5, 15, 6, "", List.of()
        );

        assertEquals(14, hunk.getOldEnd());
        assertEquals(20, hunk.getNewEnd());
    }

    @Test
    @DisplayName("DiffHunk getAdds and getRemoves work correctly")
    void diffHunkGetAddsAndGetRemovesWorkCorrectly() {
        List<StructuredDiffTypes.DiffLine> lines = List.of(
            new StructuredDiffTypes.DiffLine(StructuredDiffTypes.DiffLineType.CONTEXT, "ctx", 1, 1, null),
            new StructuredDiffTypes.DiffLine(StructuredDiffTypes.DiffLineType.ADD, "add1", -1, 2, null),
            new StructuredDiffTypes.DiffLine(StructuredDiffTypes.DiffLineType.REMOVE, "rm1", 2, -1, null),
            new StructuredDiffTypes.DiffLine(StructuredDiffTypes.DiffLineType.ADD, "add2", -1, 3, null)
        );

        StructuredDiffTypes.DiffHunk hunk = new StructuredDiffTypes.DiffHunk(1, 3, 1, 4, "", lines);

        assertEquals(2, hunk.getAdds().size());
        assertEquals(1, hunk.getRemoves().size());
        assertEquals(2, hunk.getAddCount());
        assertEquals(1, hunk.getRemoveCount());
    }

    @Test
    @DisplayName("DiffHunk formatHeader returns correct string")
    void diffHunkFormatHeaderReturnsCorrectString() {
        StructuredDiffTypes.DiffHunk hunk = new StructuredDiffTypes.DiffHunk(
            10, 5, 15, 8, " function", List.of()
        );

        assertEquals("@@ -10,5 +15,8 @@ function", hunk.formatHeader());
    }

    @Test
    @DisplayName("DiffFile record works correctly")
    void diffFileRecordWorksCorrectly() {
        StructuredDiffTypes.DiffFile file = new StructuredDiffTypes.DiffFile(
            "old/path.txt",
            "new/path.txt",
            List.of(),
            StructuredDiffTypes.DiffFileType.MODIFIED
        );

        assertEquals("old/path.txt", file.oldPath());
        assertEquals("new/path.txt", file.newPath());
        assertEquals(StructuredDiffTypes.DiffFileType.MODIFIED, file.fileType());
    }

    @Test
    @DisplayName("DiffFile isNewFile works correctly")
    void diffFileIsNewFileWorksCorrectly() {
        StructuredDiffTypes.DiffFile newFile = new StructuredDiffTypes.DiffFile(
            "/dev/null", "new.txt", List.of(), StructuredDiffTypes.DiffFileType.ADDED
        );
        StructuredDiffTypes.DiffFile normalFile = new StructuredDiffTypes.DiffFile(
            "old.txt", "new.txt", List.of(), StructuredDiffTypes.DiffFileType.MODIFIED
        );

        assertTrue(newFile.isNewFile());
        assertFalse(normalFile.isNewFile());
    }

    @Test
    @DisplayName("DiffFile isDeletedFile works correctly")
    void diffFileIsDeletedFileWorksCorrectly() {
        StructuredDiffTypes.DiffFile deletedFile = new StructuredDiffTypes.DiffFile(
            "deleted.txt", "/dev/null", List.of(), StructuredDiffTypes.DiffFileType.DELETED
        );
        StructuredDiffTypes.DiffFile normalFile = new StructuredDiffTypes.DiffFile(
            "old.txt", "new.txt", List.of(), StructuredDiffTypes.DiffFileType.MODIFIED
        );

        assertTrue(deletedFile.isDeletedFile());
        assertFalse(normalFile.isDeletedFile());
    }

    @Test
    @DisplayName("DiffFile isRenamed works correctly")
    void diffFileIsRenamedWorksCorrectly() {
        StructuredDiffTypes.DiffFile renamedFile = new StructuredDiffTypes.DiffFile(
            "old.txt", "new.txt", List.of(), StructuredDiffTypes.DiffFileType.RENAMED
        );
        StructuredDiffTypes.DiffFile normalFile = new StructuredDiffTypes.DiffFile(
            "same.txt", "same.txt", List.of(), StructuredDiffTypes.DiffFileType.MODIFIED
        );

        assertTrue(renamedFile.isRenamed());
        assertFalse(normalFile.isRenamed());
    }

    @Test
    @DisplayName("DiffFile getTotalAddLines and getTotalRemoveLines work correctly")
    void diffFileGetTotalLinesWorkCorrectly() {
        List<StructuredDiffTypes.DiffLine> lines1 = List.of(
            new StructuredDiffTypes.DiffLine(StructuredDiffTypes.DiffLineType.ADD, "a", -1, 1, null),
            new StructuredDiffTypes.DiffLine(StructuredDiffTypes.DiffLineType.ADD, "b", -1, 2, null)
        );
        List<StructuredDiffTypes.DiffLine> lines2 = List.of(
            new StructuredDiffTypes.DiffLine(StructuredDiffTypes.DiffLineType.REMOVE, "c", 1, -1, null)
        );

        StructuredDiffTypes.DiffHunk hunk1 = new StructuredDiffTypes.DiffHunk(1, 0, 1, 2, "", lines1);
        StructuredDiffTypes.DiffHunk hunk2 = new StructuredDiffTypes.DiffHunk(1, 1, 1, 0, "", lines2);

        StructuredDiffTypes.DiffFile file = new StructuredDiffTypes.DiffFile(
            "old.txt", "new.txt", List.of(hunk1, hunk2), StructuredDiffTypes.DiffFileType.MODIFIED
        );

        assertEquals(2, file.getTotalAddLines());
        assertEquals(1, file.getTotalRemoveLines());
    }

    @Test
    @DisplayName("DiffFile formatSummary returns correct string")
    void diffFileFormatSummaryReturnsCorrectString() {
        List<StructuredDiffTypes.DiffLine> lines = List.of(
            new StructuredDiffTypes.DiffLine(StructuredDiffTypes.DiffLineType.ADD, "a", -1, 1, null),
            new StructuredDiffTypes.DiffLine(StructuredDiffTypes.DiffLineType.REMOVE, "b", 1, -1, null)
        );
        StructuredDiffTypes.DiffHunk hunk = new StructuredDiffTypes.DiffHunk(1, 1, 1, 1, "", lines);

        StructuredDiffTypes.DiffFile file = new StructuredDiffTypes.DiffFile(
            "test.txt", "test.txt", List.of(hunk), StructuredDiffTypes.DiffFileType.MODIFIED
        );

        assertEquals("test.txt: +1 -1", file.formatSummary());
    }

    @Test
    @DisplayName("DiffFileType enum has correct values")
    void diffFileTypeEnumHasCorrectValues() {
        StructuredDiffTypes.DiffFileType[] types = StructuredDiffTypes.DiffFileType.values();

        assertEquals(6, types.length);
        assertTrue(Arrays.asList(types).contains(StructuredDiffTypes.DiffFileType.ADDED));
        assertTrue(Arrays.asList(types).contains(StructuredDiffTypes.DiffFileType.DELETED));
        assertTrue(Arrays.asList(types).contains(StructuredDiffTypes.DiffFileType.MODIFIED));
        assertTrue(Arrays.asList(types).contains(StructuredDiffTypes.DiffFileType.RENAMED));
        assertTrue(Arrays.asList(types).contains(StructuredDiffTypes.DiffFileType.COPIED));
        assertTrue(Arrays.asList(types).contains(StructuredDiffTypes.DiffFileType.TYPE_CHANGED));
    }

    @Test
    @DisplayName("DiffColorConfig defaultConfig returns valid config")
    void diffColorConfigDefaultConfigReturnsValidConfig() {
        StructuredDiffTypes.DiffColorConfig config = StructuredDiffTypes.DiffColorConfig.defaultConfig();

        assertNotNull(config);
        assertTrue(config.useAnsi());
        assertNotNull(config.addColor());
        assertNotNull(config.removeColor());
    }

    @Test
    @DisplayName("colorLine applies correct colors")
    void colorLineAppliesCorrectColors() {
        StructuredDiffTypes.DiffColorConfig config = StructuredDiffTypes.DiffColorConfig.defaultConfig();

        StructuredDiffTypes.DiffLine addLine = new StructuredDiffTypes.DiffLine(
            StructuredDiffTypes.DiffLineType.ADD, "content", -1, 1, null
        );
        StructuredDiffTypes.DiffLine removeLine = new StructuredDiffTypes.DiffLine(
            StructuredDiffTypes.DiffLineType.REMOVE, "content", 1, -1, null
        );

        String coloredAdd = StructuredDiffTypes.colorLine(addLine, config);
        String coloredRemove = StructuredDiffTypes.colorLine(removeLine, config);

        assertTrue(coloredAdd.contains("+content"));
        assertTrue(coloredRemove.contains("-content"));
    }

    @Test
    @DisplayName("colorLine without ANSI returns plain text")
    void colorLineWithoutAnsiReturnsPlainText() {
        StructuredDiffTypes.DiffColorConfig config = new StructuredDiffTypes.DiffColorConfig(
            null, null, null, null, false
        );

        StructuredDiffTypes.DiffLine line = new StructuredDiffTypes.DiffLine(
            StructuredDiffTypes.DiffLineType.ADD, "content", -1, 1, null
        );

        String result = StructuredDiffTypes.colorLine(line, config);

        assertEquals("+content", result);
    }
}