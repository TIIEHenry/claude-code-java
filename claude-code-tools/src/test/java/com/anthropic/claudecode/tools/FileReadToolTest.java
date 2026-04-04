/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.tools;

import com.anthropic.claudecode.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileReadTool.
 */
class FileReadToolTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("FileReadTool has correct name")
    void nameWorks() {
        FileReadTool tool = new FileReadTool();
        assertEquals("Read", tool.name());
    }

    @Test
    @DisplayName("FileReadTool has correct aliases")
    void aliasesWork() {
        FileReadTool tool = new FileReadTool();
        List<String> aliases = tool.aliases();
        assertTrue(aliases.contains("read"));
        assertTrue(aliases.contains("file_read"));
        assertTrue(aliases.contains("cat"));
    }

    @Test
    @DisplayName("FileReadTool input schema is valid")
    void inputSchemaWorks() {
        FileReadTool tool = new FileReadTool();
        var schema = tool.inputSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));
    }

    @Test
    @DisplayName("FileReadTool is read-only")
    void isReadOnlyWorks() {
        FileReadTool tool = new FileReadTool();
        FileReadTool.Input input = new FileReadTool.Input("/tmp/test.txt", null, null, null);
        assertTrue(tool.isReadOnly(input));
    }

    @Test
    @DisplayName("FileReadTool is concurrency-safe")
    void isConcurrencySafeWorks() {
        FileReadTool tool = new FileReadTool();
        FileReadTool.Input input = new FileReadTool.Input("/tmp/test.txt", null, null, null);
        assertTrue(tool.isConcurrencySafe(input));
    }

    @Test
    @DisplayName("FileReadTool isSearchOrReadCommand returns read=true")
    void isSearchOrReadCommandWorks() {
        FileReadTool tool = new FileReadTool();
        FileReadTool.Input input = new FileReadTool.Input("/tmp/test.txt", null, null, null);
        Tool.SearchOrReadCommand cmd = tool.isSearchOrReadCommand(input);

        assertFalse(cmd.isSearch());
        assertTrue(cmd.isRead());
        assertFalse(cmd.isList());
    }

    @Test
    @DisplayName("FileReadTool matches name correctly")
    void matchesNameWorks() {
        FileReadTool tool = new FileReadTool();
        assertTrue(tool.matchesName("Read"));
        assertTrue(tool.matchesName("read"));
        assertTrue(tool.matchesName("cat"));
        assertFalse(tool.matchesName("Write"));
    }

    @Test
    @DisplayName("FileReadTool Input record works")
    void inputRecordWorks() {
        FileReadTool.Input input = new FileReadTool.Input(
            "/path/to/file.txt",
            100,
            50,
            "1-5"
        );

        assertEquals("/path/to/file.txt", input.filePath());
        assertEquals(100, input.limit());
        assertEquals(50, input.offset());
        assertEquals("1-5", input.pages());
    }

    @Test
    @DisplayName("FileReadTool Output record works")
    void outputRecordWorks() {
        FileReadTool.Output output = new FileReadTool.Output(
            "file content here",
            "",
            10,
            false,
            FileReadTool.FileType.TEXT
        );

        assertEquals("file content here", output.content());
        assertEquals(10, output.lineCount());
        assertFalse(output.isError());
        assertEquals(FileReadTool.FileType.TEXT, output.fileType());
        assertEquals("file content here", output.toResultString());
    }

    @Test
    @DisplayName("FileReadTool Output error case")
    void outputErrorWorks() {
        FileReadTool.Output output = new FileReadTool.Output(
            "",
            "File not found",
            null,
            true,
            FileReadTool.FileType.TEXT
        );

        assertTrue(output.isError());
        assertEquals("File not found", output.error());
        assertEquals("File not found", output.toResultString());
    }

    @Test
    @DisplayName("FileReadTool FileType enum has all values")
    void fileTypeEnumWorks() {
        FileReadTool.FileType[] types = FileReadTool.FileType.values();
        assertEquals(5, types.length);
        assertNotNull(FileReadTool.FileType.TEXT);
        assertNotNull(FileReadTool.FileType.IMAGE);
        assertNotNull(FileReadTool.FileType.PDF);
        assertNotNull(FileReadTool.FileType.JUPYTER);
        assertNotNull(FileReadTool.FileType.DIRECTORY);
    }

    @Test
    @DisplayName("FileReadTool describe returns description")
    void describeWorks() throws Exception {
        FileReadTool tool = new FileReadTool();
        FileReadTool.Input input = new FileReadTool.Input("/tmp/test.txt", null, null, null);

        String desc = tool.describe(input, ToolDescribeOptions.empty()).get();

        assertTrue(desc.contains("Read"));
        assertTrue(desc.contains("test.txt"));
    }

    @Test
    @DisplayName("FileReadTool getActivityDescription works")
    void getActivityDescriptionWorks() {
        FileReadTool tool = new FileReadTool();
        FileReadTool.Input input = new FileReadTool.Input("/tmp/myfile.java", null, null, null);

        assertEquals("Reading myfile.java", tool.getActivityDescription(input));
    }

    @Test
    @DisplayName("FileReadTool reads text file successfully")
    void readsTextFileSuccessfully() throws Exception {
        FileReadTool tool = new FileReadTool();

        // Create test file
        Path testFile = tempDir.resolve("test.txt");
        java.nio.file.Files.writeString(testFile, "Line 1\nLine 2\nLine 3\n");

        FileReadTool.Input input = new FileReadTool.Input(testFile.toString(), null, null, null);

        ToolResult<FileReadTool.Output> result = tool.call(
            input, null, null, null, null
        ).get();

        assertNotNull(result);
        assertNotNull(result.data());
        assertFalse(result.data().isError());
        assertTrue(result.data().content().contains("Line 1"));
        assertEquals(3, result.data().lineCount());
    }

    @Test
    @DisplayName("FileReadTool handles non-existent file")
    void handlesNonExistentFile() throws Exception {
        FileReadTool tool = new FileReadTool();
        FileReadTool.Input input = new FileReadTool.Input("/nonexistent/file.txt", null, null, null);

        ToolResult<FileReadTool.Output> result = tool.call(
            input, null, null, null, null
        ).get();

        assertNotNull(result);
        assertTrue(result.data().isError());
        assertTrue(result.data().error().contains("not found"));
    }
}