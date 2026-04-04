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
 * Tests for BashTool.
 */
class BashToolTest {

    @Test
    @DisplayName("BashTool has correct name")
    void nameWorks() {
        BashTool tool = new BashTool();
        assertEquals("Bash", tool.name());
    }

    @Test
    @DisplayName("BashTool has correct aliases")
    void aliasesWork() {
        BashTool tool = new BashTool();
        List<String> aliases = tool.aliases();
        // Check that aliases list exists
        assertNotNull(aliases);
    }

    @Test
    @DisplayName("BashTool input schema is valid")
    void inputSchemaWorks() {
        BashTool tool = new BashTool();
        var schema = tool.inputSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));
    }

    @Test
    @DisplayName("BashTool is NOT read-only")
    void isNotReadOnly() {
        BashTool tool = new BashTool();
        BashTool.Input input = new BashTool.Input("ls -la", null, null, null, false);
        assertFalse(tool.isReadOnly(input));
    }

    @Test
    @DisplayName("BashTool is destructive by default")
    void isDestructiveByDefault() {
        BashTool tool = new BashTool();
        BashTool.Input input = new BashTool.Input("rm -rf /", null, null, null, false);
        assertTrue(tool.isDestructive(input));
    }

    @Test
    @DisplayName("BashTool matches name correctly")
    void matchesNameWorks() {
        BashTool tool = new BashTool();
        assertTrue(tool.matchesName("Bash"));
        assertTrue(tool.matchesName("bash"));
        assertTrue(tool.matchesName("shell"));
        assertFalse(tool.matchesName("Read"));
    }

    @Test
    @DisplayName("BashTool Input record works")
    void inputRecordWorks() {
        BashTool.Input input = new BashTool.Input(
            "ls -la",
            30000L,
            "List files",
            "/tmp",
            false
        );

        assertEquals("ls -la", input.command());
        assertEquals(30000L, input.timeout());
        assertEquals("List files", input.description());
        assertEquals("/tmp", input.cwd());
        assertFalse(input.runInBackground());
    }

    @Test
    @DisplayName("BashTool describe returns description")
    void describeWorks() throws Exception {
        BashTool tool = new BashTool();
        BashTool.Input input = new BashTool.Input("echo hello", null, "Say hello", null, false);

        String desc = tool.describe(input, ToolDescribeOptions.empty()).get();

        // Returns the description if set, otherwise the command
        assertEquals("Say hello", desc);
    }

    @Test
    @DisplayName("BashTool getActivityDescription works")
    void getActivityDescriptionWorks() {
        BashTool tool = new BashTool();
        BashTool.Input input = new BashTool.Input("npm test", null, null, null, false);

        // When no description is set, returns generic message
        assertEquals("Running shell command", tool.getActivityDescription(input));
    }

    @Test
    @DisplayName("BashTool getActivityDescription with description")
    void getActivityDescriptionWithDescription() {
        BashTool tool = new BashTool();
        BashTool.Input input = new BashTool.Input("npm test", null, "Run tests", null, false);

        assertEquals("Run tests", tool.getActivityDescription(input));
    }

    @Test
    @DisplayName("BashTool getToolUseSummary works")
    void getToolUseSummaryWorks() {
        BashTool tool = new BashTool();
        BashTool.Input input = new BashTool.Input("git status", null, null, null, false);

        // Default implementation returns tool name
        assertNotNull(tool.getToolUseSummary(input));
    }

    @Test
    @DisplayName("BashTool isSearchOrReadCommand returns false for all")
    void isSearchOrReadCommandWorks() {
        BashTool tool = new BashTool();
        BashTool.Input input = new BashTool.Input("echo test", null, null, null, false);
        Tool.SearchOrReadCommand cmd = tool.isSearchOrReadCommand(input);

        assertFalse(cmd.isSearch());
        assertFalse(cmd.isRead());
        assertFalse(cmd.isList());
    }
}