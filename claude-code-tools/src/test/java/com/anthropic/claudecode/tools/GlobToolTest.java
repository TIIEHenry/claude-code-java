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
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GlobTool.
 */
class GlobToolTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("GlobTool has correct name")
    void nameWorks() {
        GlobTool tool = new GlobTool();
        assertEquals("Glob", tool.name());
    }

    @Test
    @DisplayName("GlobTool has correct aliases")
    void aliasesWork() {
        GlobTool tool = new GlobTool();
        List<String> aliases = tool.aliases();
        assertTrue(aliases.contains("glob"));
        assertTrue(aliases.contains("find"));
        assertTrue(aliases.contains("ls"));
    }

    @Test
    @DisplayName("GlobTool input schema is valid")
    void inputSchemaWorks() {
        GlobTool tool = new GlobTool();
        var schema = tool.inputSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));
    }

    @Test
    @DisplayName("GlobTool is read-only")
    void isReadOnlyWorks() {
        GlobTool tool = new GlobTool();
        GlobTool.Input input = new GlobTool.Input("*.java", null);
        assertTrue(tool.isReadOnly(input));
    }

    @Test
    @DisplayName("GlobTool is concurrency-safe")
    void isConcurrencySafeWorks() {
        GlobTool tool = new GlobTool();
        GlobTool.Input input = new GlobTool.Input("*.java", null);
        assertTrue(tool.isConcurrencySafe(input));
    }

    @Test
    @DisplayName("GlobTool matches name correctly")
    void matchesNameWorks() {
        GlobTool tool = new GlobTool();
        assertTrue(tool.matchesName("Glob"));
        assertTrue(tool.matchesName("glob"));
        assertTrue(tool.matchesName("find"));
        assertTrue(tool.matchesName("ls"));
        assertFalse(tool.matchesName("Read"));
    }

    @Test
    @DisplayName("GlobTool Output record works")
    void outputRecordWorks() {
        GlobTool.Output output = new GlobTool.Output(
            List.of("file1.java", "file2.java"),
            "",
            false
        );

        assertEquals(2, output.count());
        assertFalse(output.isError());
        assertTrue(output.toResultString().contains("Found 2 files"));
    }

    @Test
    @DisplayName("GlobTool Output empty results")
    void outputEmptyWorks() {
        GlobTool.Output output = new GlobTool.Output(List.of(), "", false);

        assertEquals(0, output.count());
        assertTrue(output.toResultString().contains("No files found"));
    }

    @Test
    @DisplayName("GlobTool Output error case")
    void outputErrorWorks() {
        GlobTool.Output output = new GlobTool.Output(List.of(), "Permission denied", true);

        assertTrue(output.isError());
        assertEquals("Permission denied", output.error());
        assertEquals("Permission denied", output.toResultString());
    }

    @Test
    @DisplayName("GlobTool isSearchOrReadCommand returns correct values")
    void isSearchOrReadCommandWorks() {
        GlobTool tool = new GlobTool();
        GlobTool.Input input = new GlobTool.Input("*.java", null);
        Tool.SearchOrReadCommand cmd = tool.isSearchOrReadCommand(input);

        assertFalse(cmd.isRead());
        assertFalse(cmd.isSearch());
        assertTrue(cmd.isList());
    }

    @Test
    @DisplayName("GlobTool describe returns description")
    void describeWorks() throws Exception {
        GlobTool tool = new GlobTool();
        GlobTool.Input input = new GlobTool.Input("*.java", null);

        CompletableFuture<String> future = tool.describe(input, ToolDescribeOptions.empty());
        String desc = future.get();

        assertTrue(desc.contains("Glob"));
        assertTrue(desc.contains("*.java"));
    }
}