/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CollapseReadSearch.
 */
class CollapseReadSearchTest {

    @Test
    @DisplayName("CollapseReadSearch SearchOrReadResult record")
    void searchOrReadResultRecord() {
        CollapseReadSearch.SearchOrReadResult result = new CollapseReadSearch.SearchOrReadResult(
            true, true, false, false, false, false, false, null, false
        );

        assertTrue(result.isCollapsible());
        assertTrue(result.isSearch());
        assertFalse(result.isRead());
    }

    @Test
    @DisplayName("CollapseReadSearch GroupAccumulator createEmpty")
    void groupAccumulatorCreateEmpty() {
        CollapseReadSearch.GroupAccumulator acc = CollapseReadSearch.GroupAccumulator.createEmpty();

        assertEquals(0, acc.messages().size());
        assertEquals(0, acc.searchCount());
        assertEquals(0, acc.readOperationCount());
        assertEquals(0, acc.listCount());
    }

    @Test
    @DisplayName("CollapseReadSearch CollapsedReadSearchGroup record")
    void collapsedReadSearchGroupRecord() {
        CollapseReadSearch.CollapsedReadSearchGroup group = new CollapseReadSearch.CollapsedReadSearchGroup(
            "collapsed_read_search",
            1, 2, 3, 4, 5, 6, 7,
            null, null, null, null, null, "uuid", 1000L,
            8, null, 9, 10, 11
        );

        assertEquals("collapsed_read_search", group.type());
        assertEquals(1, group.searchCount());
        assertEquals(2, group.readCount());
        assertEquals("uuid", group.uuid());
    }

    @Test
    @DisplayName("CollapseReadSearch CollapsibleMessage.AssistantMessage")
    void assistantMessage() {
        CollapseReadSearch.ToolUseContent toolUse = new CollapseReadSearch.ToolUseContent(
            "tool_use", "Read", "id-123", null
        );

        CollapseReadSearch.CollapsibleMessage.AssistantMessage msg =
            new CollapseReadSearch.CollapsibleMessage.AssistantMessage(
                "uuid", 1000L, toolUse
            );

        assertEquals("uuid", msg.uuid());
        assertEquals(1000L, msg.timestamp());
        assertEquals("assistant", msg.type());
        assertEquals("Read", msg.toolUse().name());
    }

    @Test
    @DisplayName("CollapseReadSearch CollapsibleMessage.UserMessage")
    void userMessage() {
        CollapseReadSearch.ToolResultContent toolResult = new CollapseReadSearch.ToolResultContent(
            "tool_result", "id-123", "output"
        );

        CollapseReadSearch.CollapsibleMessage.UserMessage msg =
            new CollapseReadSearch.CollapsibleMessage.UserMessage(
                "uuid", 1000L, toolResult
            );

        assertEquals("uuid", msg.uuid());
        assertEquals(1000L, msg.timestamp());
        assertEquals("user", msg.type());
        assertEquals("id-123", msg.toolResult().toolUseId());
    }

    @Test
    @DisplayName("CollapseReadSearch CollapsibleMessage.GroupedMessage")
    void groupedMessage() {
        CollapseReadSearch.CollapsibleMessage.AssistantMessage inner =
            new CollapseReadSearch.CollapsibleMessage.AssistantMessage(
                "uuid1", 1000L, null
            );

        CollapseReadSearch.CollapsibleMessage.GroupedMessage msg =
            new CollapseReadSearch.CollapsibleMessage.GroupedMessage(
                "uuid", 1000L, "Grep", List.of(inner)
            );

        assertEquals("uuid", msg.uuid());
        assertEquals("grouped_tool_use", msg.type());
        assertEquals("Grep", msg.toolName());
        assertEquals(1, msg.messages().size());
    }

    @Test
    @DisplayName("CollapseReadSearch ToolUseContent record")
    void toolUseContentRecord() {
        CollapseReadSearch.ToolUseContent content = new CollapseReadSearch.ToolUseContent(
            "tool_use", "Bash", "id", Map.of("command", "ls")
        );

        assertEquals("tool_use", content.type());
        assertEquals("Bash", content.name());
        assertEquals("id", content.id());
    }

    @Test
    @DisplayName("CollapseReadSearch ToolResultContent record")
    void toolResultContentRecord() {
        CollapseReadSearch.ToolResultContent content = new CollapseReadSearch.ToolResultContent(
            "tool_result", "id", "output data"
        );

        assertEquals("tool_result", content.type());
        assertEquals("id", content.toolUseId());
        assertEquals("output data", content.output());
    }

    @Test
    @DisplayName("CollapseReadSearch commandAsHint basic")
    void commandAsHintBasic() {
        String hint = CollapseReadSearch.commandAsHint("ls -la");
        assertTrue(hint.startsWith("$ "));
        assertTrue(hint.contains("ls"));
    }

    @Test
    @DisplayName("CollapseReadSearch commandAsHint multiline")
    void commandAsHintMultiline() {
        String hint = CollapseReadSearch.commandAsHint("ls -la\ngrep test");
        assertTrue(hint.contains("\n"));
    }

    @Test
    @DisplayName("CollapseReadSearch commandAsHint truncates long commands")
    void commandAsHintTruncates() {
        String longCommand = "a".repeat(400);
        String hint = CollapseReadSearch.commandAsHint(longCommand);
        assertTrue(hint.length() <= 302); // MAX_HINT_CHARS + prefix + ellipsis
    }

    @Test
    @DisplayName("CollapseReadSearch getSearchReadSummaryText search only")
    void getSearchReadSummaryTextSearch() {
        String summary = CollapseReadSearch.getSearchReadSummaryText(3, 0, false, 0, 0);
        assertTrue(summary.contains("Searched for"));
        assertTrue(summary.contains("3"));
        assertTrue(summary.contains("patterns"));
    }

    @Test
    @DisplayName("CollapseReadSearch getSearchReadSummaryText read only")
    void getSearchReadSummaryTextRead() {
        String summary = CollapseReadSearch.getSearchReadSummaryText(0, 2, false, 0, 0);
        assertTrue(summary.contains("Read"));
        assertTrue(summary.contains("2"));
        assertTrue(summary.contains("files"));
    }

    @Test
    @DisplayName("CollapseReadSearch getSearchReadSummaryText active")
    void getSearchReadSummaryTextActive() {
        String summary = CollapseReadSearch.getSearchReadSummaryText(1, 1, true, 0, 0);
        assertTrue(summary.contains("Searching"));
        assertTrue(summary.endsWith("…"));
    }

    @Test
    @DisplayName("CollapseReadSearch getSearchReadSummaryText list")
    void getSearchReadSummaryTextList() {
        String summary = CollapseReadSearch.getSearchReadSummaryText(0, 0, false, 0, 5);
        assertTrue(summary.contains("Listed"));
        assertTrue(summary.contains("5"));
    }

    @Test
    @DisplayName("CollapseReadSearch getSearchReadSummaryText empty")
    void getSearchReadSummaryTextEmpty() {
        String summary = CollapseReadSearch.getSearchReadSummaryText(0, 0, false, 0, 0);
        assertEquals("", summary);
    }

    @Test
    @DisplayName("CollapseReadSearch getToolSearchOrReadInfo Grep")
    void getToolSearchOrReadInfoGrep() {
        CollapseReadSearch.SearchOrReadResult result =
            CollapseReadSearch.getToolSearchOrReadInfo("Grep", null);
        assertTrue(result.isSearch());
        assertTrue(result.isCollapsible());
    }

    @Test
    @DisplayName("CollapseReadSearch getToolSearchOrReadInfo Glob")
    void getToolSearchOrReadInfoGlob() {
        CollapseReadSearch.SearchOrReadResult result =
            CollapseReadSearch.getToolSearchOrReadInfo("Glob", null);
        assertTrue(result.isSearch());
        assertTrue(result.isCollapsible());
    }

    @Test
    @DisplayName("CollapseReadSearch getToolSearchOrReadInfo Read")
    void getToolSearchOrReadInfoRead() {
        CollapseReadSearch.SearchOrReadResult result =
            CollapseReadSearch.getToolSearchOrReadInfo("Read", null);
        assertTrue(result.isRead());
        assertTrue(result.isCollapsible());
    }

    @Test
    @DisplayName("CollapseReadSearch getToolSearchOrReadInfo Ls")
    void getToolSearchOrReadInfoLs() {
        CollapseReadSearch.SearchOrReadResult result =
            CollapseReadSearch.getToolSearchOrReadInfo("Ls", null);
        assertTrue(result.isList());
        assertTrue(result.isCollapsible());
    }

    @Test
    @DisplayName("CollapseReadSearch getToolSearchOrReadInfo Bash with grep")
    void getToolSearchOrReadInfoBashGrep() {
        Map<String, Object> input = new HashMap<>();
        input.put("command", "grep pattern file.txt");

        CollapseReadSearch.SearchOrReadResult result =
            CollapseReadSearch.getToolSearchOrReadInfo("Bash", input);
        assertTrue(result.isSearch());
    }

    @Test
    @DisplayName("CollapseReadSearch getToolSearchOrReadInfo Bash with cat")
    void getToolSearchOrReadInfoBashCat() {
        Map<String, Object> input = new HashMap<>();
        input.put("command", "cat file.txt");

        CollapseReadSearch.SearchOrReadResult result =
            CollapseReadSearch.getToolSearchOrReadInfo("Bash", input);
        assertTrue(result.isRead());
    }

    @Test
    @DisplayName("CollapseReadSearch getToolSearchOrReadInfo Bash with ls")
    void getToolSearchOrReadInfoBashLs() {
        Map<String, Object> input = new HashMap<>();
        input.put("command", "ls -la");

        CollapseReadSearch.SearchOrReadResult result =
            CollapseReadSearch.getToolSearchOrReadInfo("Bash", input);
        assertTrue(result.isList());
    }

    @Test
    @DisplayName("CollapseReadSearch getFilePathFromToolInput with file_path")
    void getFilePathFromToolInputFilePath() {
        Map<String, Object> input = new HashMap<>();
        input.put("file_path", "/path/to/file");

        String path = CollapseReadSearch.getFilePathFromToolInput(input);
        assertEquals("/path/to/file", path);
    }

    @Test
    @DisplayName("CollapseReadSearch getFilePathFromToolInput with path")
    void getFilePathFromToolInputPath() {
        Map<String, Object> input = new HashMap<>();
        input.put("path", "/path/to/file");

        String path = CollapseReadSearch.getFilePathFromToolInput(input);
        assertEquals("/path/to/file", path);
    }

    @Test
    @DisplayName("CollapseReadSearch getFilePathFromToolInput null")
    void getFilePathFromToolInputNull() {
        String path = CollapseReadSearch.getFilePathFromToolInput(null);
        assertNull(path);
    }
}