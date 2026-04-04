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
 * Tests for GrepTool.
 */
class GrepToolTest {

    @Test
    @DisplayName("GrepTool has correct name")
    void nameWorks() {
        GrepTool tool = new GrepTool();
        assertEquals("Grep", tool.name());
    }

    @Test
    @DisplayName("GrepTool has correct aliases")
    void aliasesWork() {
        GrepTool tool = new GrepTool();
        List<String> aliases = tool.aliases();
        assertTrue(aliases.contains("grep"));
        assertTrue(aliases.contains("rg"));
        assertTrue(aliases.contains("search"));
    }

    @Test
    @DisplayName("GrepTool input schema is valid")
    void inputSchemaWorks() {
        GrepTool tool = new GrepTool();
        var schema = tool.inputSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));
    }

    @Test
    @DisplayName("GrepTool is read-only")
    void isReadOnlyWorks() {
        GrepTool tool = new GrepTool();
        GrepTool.Input input = new GrepTool.Input("test", null);
        assertTrue(tool.isReadOnly(input));
    }

    @Test
    @DisplayName("GrepTool is concurrency-safe")
    void isConcurrencySafeWorks() {
        GrepTool tool = new GrepTool();
        GrepTool.Input input = new GrepTool.Input("test", null);
        assertTrue(tool.isConcurrencySafe(input));
    }

    @Test
    @DisplayName("GrepTool isSearchOrReadCommand returns search=true")
    void isSearchOrReadCommandWorks() {
        GrepTool tool = new GrepTool();
        GrepTool.Input input = new GrepTool.Input("test", null);
        Tool.SearchOrReadCommand cmd = tool.isSearchOrReadCommand(input);

        assertTrue(cmd.isSearch());
        assertFalse(cmd.isRead());
        assertFalse(cmd.isList());
    }

    @Test
    @DisplayName("GrepTool matches name correctly")
    void matchesNameWorks() {
        GrepTool tool = new GrepTool();
        assertTrue(tool.matchesName("Grep"));
        assertTrue(tool.matchesName("grep"));
        assertTrue(tool.matchesName("rg"));
        assertTrue(tool.matchesName("search"));
        assertFalse(tool.matchesName("Glob"));
    }

    @Test
    @DisplayName("GrepTool Input record works")
    void inputRecordWorks() {
        GrepTool.Input input = new GrepTool.Input(
            "test.*pattern",
            "/tmp",
            "*.java",
            "java",
            "content",
            true,
            false,
            100,
            2
        );

        assertEquals("test.*pattern", input.pattern());
        assertEquals("/tmp", input.path());
        assertEquals("*.java", input.glob());
        assertEquals("java", input.type());
        assertEquals("content", input.outputMode());
        assertTrue(input.caseInsensitive());
        assertFalse(input.multiline());
        assertEquals(100, input.headLimit());
        assertEquals(2, input.context());
    }

    @Test
    @DisplayName("GrepTool Input convenience constructor works")
    void inputConvenienceConstructorWorks() {
        GrepTool.Input input = new GrepTool.Input("test", "/home");

        assertEquals("test", input.pattern());
        assertEquals("/home", input.path());
        assertNull(input.glob());
        assertEquals("files_with_matches", input.outputMode());
        assertFalse(input.caseInsensitive());
        assertEquals(250, input.headLimit());
    }

    @Test
    @DisplayName("GrepTool Output record works")
    void outputRecordWorks() {
        GrepTool.Output output = new GrepTool.Output(
            List.of("file1.java", "file2.java"),
            List.of(),
            10,
            "",
            false
        );

        assertEquals(2, output.files().size());
        assertEquals(10, output.totalMatches());
        assertFalse(output.isError());
        assertTrue(output.toResultString().contains("Found matches in 2 files"));
    }

    @Test
    @DisplayName("GrepTool Output error case")
    void outputErrorWorks() {
        GrepTool.Output output = new GrepTool.Output(
            List.of(),
            List.of(),
            0,
            "Invalid regex",
            true
        );

        assertTrue(output.isError());
        assertEquals("Invalid regex", output.error());
        assertEquals("Invalid regex", output.toResultString());
    }

    @Test
    @DisplayName("GrepTool Output content mode")
    void outputContentModeWorks() {
        GrepTool.Output output = new GrepTool.Output(
            List.of(),
            List.of("file1.java:10:match here", "file1.java:20:another match"),
            2,
            "",
            false
        );

        assertTrue(output.toResultString().contains("Matched lines"));
        assertTrue(output.toResultString().contains("file1.java:10"));
    }

    @Test
    @DisplayName("GrepTool describe returns description")
    void describeWorks() throws Exception {
        GrepTool tool = new GrepTool();
        GrepTool.Input input = new GrepTool.Input("pattern", null);

        String desc = tool.describe(input, ToolDescribeOptions.empty()).get();

        assertTrue(desc.contains("Grep"));
        assertTrue(desc.contains("pattern"));
    }

    @Test
    @DisplayName("GrepTool getActivityDescription works")
    void getActivityDescriptionWorks() {
        GrepTool tool = new GrepTool();
        GrepTool.Input input = new GrepTool.Input("TODO", null);

        assertEquals("Searching for: TODO", tool.getActivityDescription(input));
    }

    @Test
    @DisplayName("GrepTool getToolUseSummary works")
    void getToolUseSummaryWorks() {
        GrepTool tool = new GrepTool();
        GrepTool.Input input = new GrepTool.Input("pattern", null);

        assertEquals("pattern", tool.getToolUseSummary(input));
    }
}