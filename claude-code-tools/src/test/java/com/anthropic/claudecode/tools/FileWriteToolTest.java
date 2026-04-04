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
 * Tests for FileWriteTool.
 */
class FileWriteToolTest {

    @Test
    @DisplayName("FileWriteTool has correct name")
    void nameWorks() {
        FileWriteTool tool = new FileWriteTool();
        assertEquals("Write", tool.name());
    }

    @Test
    @DisplayName("FileWriteTool has correct aliases")
    void aliasesWork() {
        FileWriteTool tool = new FileWriteTool();
        List<String> aliases = tool.aliases();
        assertNotNull(aliases);
    }

    @Test
    @DisplayName("FileWriteTool input schema is valid")
    void inputSchemaWorks() {
        FileWriteTool tool = new FileWriteTool();
        var schema = tool.inputSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));
    }

    @Test
    @DisplayName("FileWriteTool is NOT read-only")
    void isNotReadOnly() {
        FileWriteTool tool = new FileWriteTool();
        FileWriteTool.Input input = new FileWriteTool.Input("/tmp/test.txt", "content");
        assertFalse(tool.isReadOnly(input));
    }

    @Test
    @DisplayName("FileWriteTool is destructive only if file exists")
    void isDestructive() {
        FileWriteTool tool = new FileWriteTool();
        FileWriteTool.Input input = new FileWriteTool.Input("/nonexistent/path/test.txt", "content");
        // Only destructive if the file already exists (overwriting)
        assertFalse(tool.isDestructive(input));
    }

    @Test
    @DisplayName("FileWriteTool matches name correctly")
    void matchesNameWorks() {
        FileWriteTool tool = new FileWriteTool();
        assertTrue(tool.matchesName("Write"));
        assertTrue(tool.matchesName("write"));
        assertFalse(tool.matchesName("Read"));
    }

    @Test
    @DisplayName("FileWriteTool Input record works")
    void inputRecordWorks() {
        FileWriteTool.Input input = new FileWriteTool.Input(
            "/path/to/file.txt",
            "Hello, World!"
        );

        assertEquals("/path/to/file.txt", input.filePath());
        assertEquals("Hello, World!", input.content());
    }

    @Test
    @DisplayName("FileWriteTool isSearchOrReadCommand returns false for all")
    void isSearchOrReadCommandWorks() {
        FileWriteTool tool = new FileWriteTool();
        FileWriteTool.Input input = new FileWriteTool.Input("/tmp/test.txt", "content");
        Tool.SearchOrReadCommand cmd = tool.isSearchOrReadCommand(input);

        assertFalse(cmd.isSearch());
        assertFalse(cmd.isRead());
        assertFalse(cmd.isList());
    }
}