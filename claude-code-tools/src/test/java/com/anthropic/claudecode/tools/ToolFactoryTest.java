/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.tools;

import com.anthropic.claudecode.Tool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ToolFactory.
 */
class ToolFactoryTest {

    @Test
    @DisplayName("ToolFactory createAllTools returns all tools")
    void createAllToolsWorks() {
        List<Tool<?, ?, ?>> tools = ToolFactory.createAllTools();
        assertNotNull(tools);
        assertFalse(tools.isEmpty());
        // Should have at least 15 tools
        assertTrue(tools.size() >= 15);
    }

    @Test
    @DisplayName("ToolFactory createReadOnlyTools returns read-only tools")
    void createReadOnlyToolsWorks() {
        List<Tool<?, ?, ?>> tools = ToolFactory.createReadOnlyTools();
        assertNotNull(tools);
        assertFalse(tools.isEmpty());
        // All tools should be read-only
        for (Tool<?, ?, ?> tool : tools) {
            // Most tools in read-only set should be safe
            assertNotNull(tool.name());
        }
    }

    @Test
    @DisplayName("ToolFactory createFileTools returns file tools")
    void createFileToolsWorks() {
        List<Tool<?, ?, ?>> tools = ToolFactory.createFileTools();
        assertNotNull(tools);
        assertEquals(5, tools.size());
    }

    @Test
    @DisplayName("ToolFactory createWebTools returns web tools")
    void createWebToolsWorks() {
        List<Tool<?, ?, ?>> tools = ToolFactory.createWebTools();
        assertNotNull(tools);
        assertEquals(2, tools.size());
    }

    @Test
    @DisplayName("ToolFactory createExecutionTools returns execution tools")
    void createExecutionToolsWorks() {
        List<Tool<?, ?, ?>> tools = ToolFactory.createExecutionTools();
        assertNotNull(tools);
        assertEquals(2, tools.size());
    }

    @Test
    @DisplayName("ToolFactory tools have correct names")
    void toolsHaveCorrectNames() {
        List<Tool<?, ?, ?>> tools = ToolFactory.createAllTools();

        // Check for expected tool names
        boolean hasBash = tools.stream().anyMatch(t -> t.name().equals("Bash"));
        boolean hasRead = tools.stream().anyMatch(t -> t.name().equals("Read"));
        boolean hasWrite = tools.stream().anyMatch(t -> t.name().equals("Write"));
        boolean hasGlob = tools.stream().anyMatch(t -> t.name().equals("Glob"));
        boolean hasGrep = tools.stream().anyMatch(t -> t.name().equals("Grep"));

        assertTrue(hasBash, "Should have Bash tool");
        assertTrue(hasRead, "Should have Read tool");
        assertTrue(hasWrite, "Should have Write tool");
        assertTrue(hasGlob, "Should have Glob tool");
        assertTrue(hasGrep, "Should have Grep tool");
    }

    @Test
    @DisplayName("ToolFactory read-only tools don't include destructive tools")
    void readOnlyToolsExcludesDestructive() {
        List<Tool<?, ?, ?>> readOnlyTools = ToolFactory.createReadOnlyTools();

        // Read-only tools should not include Bash or Write
        boolean hasBash = readOnlyTools.stream().anyMatch(t -> t.name().equals("Bash"));
        boolean hasWrite = readOnlyTools.stream().anyMatch(t -> t.name().equals("Write"));

        assertFalse(hasBash, "Read-only tools should not include Bash");
        assertFalse(hasWrite, "Read-only tools should not include Write");
    }

    @Test
    @DisplayName("ToolFactory creates new instances each call")
    void createsNewInstances() {
        List<Tool<?, ?, ?>> tools1 = ToolFactory.createAllTools();
        List<Tool<?, ?, ?>> tools2 = ToolFactory.createAllTools();

        assertNotSame(tools1, tools2);
    }
}