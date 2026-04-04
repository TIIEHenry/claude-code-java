/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClaudeMd.
 */
class ClaudeMdTest {

    @Test
    @DisplayName("ClaudeMd MAX_MEMORY_CHARACTER_COUNT")
    void maxMemoryCharacterCount() {
        assertEquals(40000, ClaudeMd.MAX_MEMORY_CHARACTER_COUNT);
    }

    @Test
    @DisplayName("ClaudeMd MemoryType enum values")
    void memoryTypeEnum() {
        ClaudeMd.MemoryType[] types = ClaudeMd.MemoryType.values();
        assertEquals(6, types.length);
        assertEquals(ClaudeMd.MemoryType.MANAGED, ClaudeMd.MemoryType.valueOf("MANAGED"));
        assertEquals(ClaudeMd.MemoryType.USER, ClaudeMd.MemoryType.valueOf("USER"));
        assertEquals(ClaudeMd.MemoryType.PROJECT, ClaudeMd.MemoryType.valueOf("PROJECT"));
        assertEquals(ClaudeMd.MemoryType.LOCAL, ClaudeMd.MemoryType.valueOf("LOCAL"));
        assertEquals(ClaudeMd.MemoryType.AUTO_MEM, ClaudeMd.MemoryType.valueOf("AUTO_MEM"));
        assertEquals(ClaudeMd.MemoryType.TEAM_MEM, ClaudeMd.MemoryType.valueOf("TEAM_MEM"));
    }

    @Test
    @DisplayName("ClaudeMd MemoryFileInfo record")
    void memoryFileInfoRecord() {
        ClaudeMd.MemoryFileInfo info = new ClaudeMd.MemoryFileInfo(
            "/path/to/CLAUDE.md",
            ClaudeMd.MemoryType.PROJECT,
            "Test content",
            "/parent",
            null,
            false,
            null
        );

        assertEquals("/path/to/CLAUDE.md", info.path());
        assertEquals(ClaudeMd.MemoryType.PROJECT, info.type());
        assertEquals("Test content", info.content());
        assertEquals("/parent", info.parent());
        assertFalse(info.contentDiffersFromDisk());
        assertNull(info.rawContent());
    }

    @Test
    @DisplayName("ClaudeMd isMemoryFilePath CLAUDE.md")
    void isMemoryFilePathClaudeMd() {
        assertTrue(ClaudeMd.isMemoryFilePath("/project/CLAUDE.md"));
        assertTrue(ClaudeMd.isMemoryFilePath("/home/user/project/CLAUDE.md"));
    }

    @Test
    @DisplayName("ClaudeMd isMemoryFilePath CLAUDE.local.md")
    void isMemoryFilePathClaudeLocalMd() {
        assertTrue(ClaudeMd.isMemoryFilePath("/project/CLAUDE.local.md"));
    }

    @Test
    @DisplayName("ClaudeMd isMemoryFilePath rules")
    void isMemoryFilePathRules() {
        assertTrue(ClaudeMd.isMemoryFilePath("/project/.claude/rules/custom.md"));
    }

    @Test
    @DisplayName("ClaudeMd isMemoryFilePath regular md")
    void isMemoryFilePathRegularMd() {
        assertFalse(ClaudeMd.isMemoryFilePath("/project/README.md"));
        assertFalse(ClaudeMd.isMemoryFilePath("/project/docs/guide.md"));
    }

    @Test
    @DisplayName("ClaudeMd isMemoryFilePath non md")
    void isMemoryFilePathNonMd() {
        assertFalse(ClaudeMd.isMemoryFilePath("/project/test.txt"));
        assertFalse(ClaudeMd.isMemoryFilePath("/project/config.json"));
    }

    @Test
    @DisplayName("ClaudeMd stripHtmlComments no comments")
    void stripHtmlCommentsNoComments() {
        String content = "No comments here";
        assertEquals(content, ClaudeMd.stripHtmlComments(content));
    }

    @Test
    @DisplayName("ClaudeMd stripHtmlComments single comment")
    void stripHtmlCommentsSingle() {
        String content = "Before <!-- comment --> After";
        String result = ClaudeMd.stripHtmlComments(content);
        assertEquals("Before  After", result);
    }

    @Test
    @DisplayName("ClaudeMd stripHtmlComments multiline comment")
    void stripHtmlCommentsMultiline() {
        String content = "Before <!-- \nmultiline\ncomment --> After";
        String result = ClaudeMd.stripHtmlComments(content);
        assertEquals("Before  After", result);
    }

    @Test
    @DisplayName("ClaudeMd stripHtmlComments multiple comments")
    void stripHtmlCommentsMultiple() {
        String content = "A <!-- c1 --> B <!-- c2 --> C";
        String result = ClaudeMd.stripHtmlComments(content);
        assertEquals("A  B  C", result);
    }

    @Test
    @DisplayName("ClaudeMd stripHtmlComments empty comment")
    void stripHtmlCommentsEmpty() {
        String content = "Before <!----> After";
        String result = ClaudeMd.stripHtmlComments(content);
        assertEquals("Before  After", result);
    }

    @Test
    @DisplayName("ClaudeMd truncateContent no truncate")
    void truncateContentNoTruncate() {
        String content = "Short content";
        String result = ClaudeMd.truncateContent(content, 100);
        assertEquals(content, result);
    }

    @Test
    @DisplayName("ClaudeMd truncateContent exact length")
    void truncateContentExactLength() {
        String content = "12345";
        String result = ClaudeMd.truncateContent(content, 5);
        assertEquals(content, result);
    }

    @Test
    @DisplayName("ClaudeMd truncateContent needs truncation")
    void truncateContentNeedsTruncation() {
        String content = "1234567890";
        String result = ClaudeMd.truncateContent(content, 5);
        assertEquals("12345\n... (truncated)", result);
    }

    @Test
    @DisplayName("ClaudeMd truncateContent zero max chars")
    void truncateContentZeroMaxChars() {
        String content = "Content";
        String result = ClaudeMd.truncateContent(content, 0);
        assertEquals("\n... (truncated)", result);
    }

    @Test
    @DisplayName("ClaudeMd getMemoryFiles does not throw")
    void getMemoryFiles() {
        // This method reads from filesystem, may return empty list
        assertDoesNotThrow(() -> ClaudeMd.getMemoryFiles(false));
    }

    @Test
    @DisplayName("ClaudeMd getMemoryFiles with forceInclude")
    void getMemoryFilesForceInclude() {
        assertDoesNotThrow(() -> ClaudeMd.getMemoryFiles(true));
    }
}