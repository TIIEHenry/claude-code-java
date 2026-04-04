/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.tools;

import com.anthropic.claudecode.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileEditTool.
 */
class FileEditToolTest {

    @Test
    @DisplayName("FileEditTool has correct name")
    void nameWorks() {
        FileEditTool tool = new FileEditTool();
        assertEquals("Edit", tool.name());
    }

    @Test
    @DisplayName("FileEditTool has correct aliases")
    void aliasesWork() {
        FileEditTool tool = new FileEditTool();
        List<String> aliases = tool.aliases();
        assertNotNull(aliases);
    }

    @Test
    @DisplayName("FileEditTool input schema is valid")
    void inputSchemaWorks() {
        FileEditTool tool = new FileEditTool();
        var schema = tool.inputSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));
    }

    @Test
    @DisplayName("FileEditTool is NOT read-only")
    void isNotReadOnly() {
        FileEditTool tool = new FileEditTool();
        FileEditTool.Input input = new FileEditTool.Input(
            "/tmp/test.txt",
            "old",
            "new",
            false
        );
        assertFalse(tool.isReadOnly(input));
    }

    @Test
    @DisplayName("FileEditTool is destructive")
    void isDestructive() {
        FileEditTool tool = new FileEditTool();
        FileEditTool.Input input = new FileEditTool.Input(
            "/tmp/test.txt",
            "old",
            "new",
            false
        );
        assertTrue(tool.isDestructive(input));
    }

    @Test
    @DisplayName("FileEditTool matches name correctly")
    void matchesNameWorks() {
        FileEditTool tool = new FileEditTool();
        assertTrue(tool.matchesName("Edit"));
        assertTrue(tool.matchesName("edit"));
        assertFalse(tool.matchesName("Write"));
    }

    @Test
    @DisplayName("FileEditTool Input record works")
    void inputRecordWorks() {
        FileEditTool.Input input = new FileEditTool.Input(
            "/path/to/file.txt",
            "old text",
            "new text",
            true
        );

        assertEquals("/path/to/file.txt", input.filePath());
        assertEquals("old text", input.oldString());
        assertEquals("new text", input.newString());
        assertTrue(input.replaceAll());
    }

    @Test
    @DisplayName("FileEditTool isSearchOrReadCommand returns false for all")
    void isSearchOrReadCommandWorks() {
        FileEditTool tool = new FileEditTool();
        FileEditTool.Input input = new FileEditTool.Input(
            "/tmp/test.txt",
            "old",
            "new",
            false
        );
        Tool.SearchOrReadCommand cmd = tool.isSearchOrReadCommand(input);

        assertFalse(cmd.isSearch());
        assertFalse(cmd.isRead());
        assertFalse(cmd.isList());
    }
}