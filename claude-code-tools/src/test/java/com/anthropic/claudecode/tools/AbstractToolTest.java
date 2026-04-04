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
 * Tests for AbstractTool base class.
 */
class AbstractToolTest {

    /**
     * Concrete implementation for testing.
     */
    static class TestTool extends AbstractTool<String, String, ToolProgressData> {
        TestTool() {
            super("Test", List.of("t", "test"), null);
        }

        @Override
        public java.util.concurrent.CompletableFuture<ToolResult<String>> call(
                String input,
                ToolUseContext context,
                CanUseToolFn canUseTool,
                AssistantMessage parentMessage,
                java.util.function.Consumer<ToolProgress<ToolProgressData>> onProgress) {
            return java.util.concurrent.CompletableFuture.completedFuture(ToolResult.of("result"));
        }
    }

    @Test
    @DisplayName("AbstractTool name returns correct value")
    void nameWorks() {
        TestTool tool = new TestTool();
        assertEquals("Test", tool.name());
    }

    @Test
    @DisplayName("AbstractTool aliases returns correct values")
    void aliasesWorks() {
        TestTool tool = new TestTool();
        assertEquals(List.of("t", "test"), tool.aliases());
    }

    @Test
    @DisplayName("AbstractTool isEnabled returns true by default")
    void isEnabledWorks() {
        TestTool tool = new TestTool();
        assertTrue(tool.isEnabled());
    }

    @Test
    @DisplayName("AbstractTool isReadOnly returns false by default")
    void isReadOnlyWorks() {
        TestTool tool = new TestTool();
        assertFalse(tool.isReadOnly("input"));
    }

    @Test
    @DisplayName("AbstractTool isDestructive returns true when not read-only")
    void isDestructiveWorks() {
        TestTool tool = new TestTool();
        assertTrue(tool.isDestructive("input"));
    }

    @Test
    @DisplayName("AbstractTool matchesName works correctly")
    void matchesNameWorks() {
        TestTool tool = new TestTool();
        assertTrue(tool.matchesName("Test"));
        assertTrue(tool.matchesName("t"));
        assertTrue(tool.matchesName("test"));
        assertFalse(tool.matchesName("Other"));
    }

    @Test
    @DisplayName("AbstractTool strict returns true by default")
    void strictWorks() {
        TestTool tool = new TestTool();
        assertTrue(tool.strict());
    }

    @Test
    @DisplayName("AbstractTool alwaysLoad returns true by default")
    void alwaysLoadWorks() {
        TestTool tool = new TestTool();
        assertTrue(tool.alwaysLoad());
    }

    @Test
    @DisplayName("AbstractTool shouldDefer returns false by default")
    void shouldDeferWorks() {
        TestTool tool = new TestTool();
        assertFalse(tool.shouldDefer());
    }

    @Test
    @DisplayName("AbstractTool requiresUserInteraction returns false by default")
    void requiresUserInteractionWorks() {
        TestTool tool = new TestTool();
        assertFalse(tool.requiresUserInteraction());
    }

    @Test
    @DisplayName("AbstractTool interruptBehavior returns block by default")
    void interruptBehaviorWorks() {
        TestTool tool = new TestTool();
        assertEquals("block", tool.interruptBehavior());
    }

    @Test
    @DisplayName("AbstractTool maxResultSizeChars returns default")
    void maxResultSizeCharsWorks() {
        TestTool tool = new TestTool();
        assertEquals(100000, tool.maxResultSizeChars());
    }

    @Test
    @DisplayName("AbstractTool isOpenWorld returns false by default")
    void isOpenWorldWorks() {
        TestTool tool = new TestTool();
        assertFalse(tool.isOpenWorld("input"));
    }

    @Test
    @DisplayName("AbstractTool mcpInfo returns null by default")
    void mcpInfoWorks() {
        TestTool tool = new TestTool();
        assertNull(tool.mcpInfo());
    }

    @Test
    @DisplayName("AbstractTool getToolUseSummary returns name by default")
    void getToolUseSummaryWorks() {
        TestTool tool = new TestTool();
        assertEquals("Test", tool.getToolUseSummary("input"));
    }

    @Test
    @DisplayName("AbstractTool getActivityDescription returns description")
    void getActivityDescriptionWorks() {
        TestTool tool = new TestTool();
        assertEquals("Running Test", tool.getActivityDescription("input"));
    }

    @Test
    @DisplayName("AbstractTool validateInput returns success by default")
    void validateInputWorks() throws Exception {
        TestTool tool = new TestTool();
        var result = tool.validateInput("input", null).get();
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("AbstractTool describe returns default description")
    void describeWorks() throws Exception {
        TestTool tool = new TestTool();
        var desc = tool.describe("input", ToolDescribeOptions.empty()).get();
        assertTrue(desc.contains("Test"));
    }
}