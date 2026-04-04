/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ContentArray.
 */
class ContentArrayTest {

    @Test
    @DisplayName("ContentArray TextBlock record")
    void textBlockRecord() {
        ContentArray.TextBlock block = new ContentArray.TextBlock("Hello, World!");
        assertEquals("text", block.type());
        assertEquals("Hello, World!", block.text());
    }

    @Test
    @DisplayName("ContentArray TextBlock with explicit type")
    void textBlockWithExplicitType() {
        ContentArray.TextBlock block = new ContentArray.TextBlock("text", "Content");
        assertEquals("text", block.type());
        assertEquals("Content", block.text());
    }

    @Test
    @DisplayName("ContentArray ToolResultBlock record")
    void toolResultBlockRecord() {
        ContentArray.ToolResultBlock block = new ContentArray.ToolResultBlock("tool-123", "Result content");
        assertEquals("tool_result", block.type());
        assertEquals("tool-123", block.toolUseId());
        assertEquals("Result content", block.content());
    }

    @Test
    @DisplayName("ContentArray ContentBlock interface")
    void contentBlockInterface() {
        ContentArray.ContentBlock textBlock = new ContentArray.TextBlock("text");
        ContentArray.ContentBlock toolBlock = new ContentArray.ToolResultBlock("id", "content");

        assertEquals("text", textBlock.type());
        assertEquals("tool_result", toolBlock.type());
    }

    @Test
    @DisplayName("ContentArray insertBlockAfterToolResults with tool results")
    void insertBlockAfterToolResultsWithToolResults() {
        List<ContentArray.ContentBlock> content = new ArrayList<>();
        content.add(new ContentArray.TextBlock("Start"));
        content.add(new ContentArray.ToolResultBlock("tool-1", "Result"));
        content.add(new ContentArray.TextBlock("End"));

        ContentArray.TextBlock newBlock = new ContentArray.TextBlock("Inserted");
        ContentArray.insertBlockAfterToolResults(content, newBlock);

        // After tool_result at index 1, insert new block at index 2
        // Result: [Start, tool_result, Inserted, End] - 4 elements
        assertEquals(4, content.size());
        assertEquals("Inserted", ((ContentArray.TextBlock) content.get(2)).text());
    }

    @Test
    @DisplayName("ContentArray insertBlockAfterToolResults no tool results")
    void insertBlockAfterToolResultsNoToolResults() {
        List<ContentArray.ContentBlock> content = new ArrayList<>();
        content.add(new ContentArray.TextBlock("Start"));
        content.add(new ContentArray.TextBlock("End"));

        ContentArray.TextBlock newBlock = new ContentArray.TextBlock("Inserted");
        ContentArray.insertBlockAfterToolResults(content, newBlock);

        assertEquals(3, content.size());
        // Should be inserted before the last element
        assertEquals("Inserted", ((ContentArray.TextBlock) content.get(1)).text());
    }

    @Test
    @DisplayName("ContentArray insertBlockAfterToolResults null content")
    void insertBlockAfterToolResultsNullContent() {
        ContentArray.TextBlock block = new ContentArray.TextBlock("Test");
        assertDoesNotThrow(() -> ContentArray.insertBlockAfterToolResults((List<ContentArray.ContentBlock>) null, block));
    }

    @Test
    @DisplayName("ContentArray insertBlockAfterToolResults null block")
    void insertBlockAfterToolResultsNullBlock() {
        List<ContentArray.ContentBlock> content = new ArrayList<>();
        content.add(new ContentArray.TextBlock("Test"));
        assertDoesNotThrow(() -> ContentArray.insertBlockAfterToolResults(content, null));
    }

    @Test
    @DisplayName("ContentArray insertBlockAfterToolResults with maps")
    void insertBlockAfterToolResultsWithMaps() {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(ContentArray.textBlock("Start"));
        content.add(ContentArray.toolResultBlock("tool-1", "Result"));
        content.add(ContentArray.textBlock("End"));

        Map<String, Object> newBlock = ContentArray.textBlock("Inserted");
        ContentArray.insertBlockAfterToolResults(content, newBlock);

        // After tool_result at index 1, insert new block at index 2
        // Result: [Start, tool_result, Inserted, End] - 4 elements
        assertEquals(4, content.size());
    }

    @Test
    @DisplayName("ContentArray findToolResults")
    void findToolResults() {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(ContentArray.textBlock("Start"));
        content.add(ContentArray.toolResultBlock("tool-1", "Result 1"));
        content.add(ContentArray.toolResultBlock("tool-2", "Result 2"));
        content.add(ContentArray.textBlock("End"));

        List<Map<String, Object>> results = ContentArray.findToolResults(content);
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("ContentArray findToolResults empty")
    void findToolResultsEmpty() {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(ContentArray.textBlock("Start"));
        content.add(ContentArray.textBlock("End"));

        List<Map<String, Object>> results = ContentArray.findToolResults(content);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("ContentArray findToolResults null")
    void findToolResultsNull() {
        List<Map<String, Object>> results = ContentArray.findToolResults(null);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("ContentArray hasToolResults true")
    void hasToolResultsTrue() {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(ContentArray.toolResultBlock("tool-1", "Result"));

        assertTrue(ContentArray.hasToolResults(content));
    }

    @Test
    @DisplayName("ContentArray hasToolResults false")
    void hasToolResultsFalse() {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(ContentArray.textBlock("Text"));

        assertFalse(ContentArray.hasToolResults(content));
    }

    @Test
    @DisplayName("ContentArray hasToolResults null")
    void hasToolResultsNull() {
        assertFalse(ContentArray.hasToolResults(null));
    }

    @Test
    @DisplayName("ContentArray textBlock utility")
    void textBlockUtility() {
        Map<String, Object> block = ContentArray.textBlock("Hello");
        assertEquals("text", block.get("type"));
        assertEquals("Hello", block.get("text"));
    }

    @Test
    @DisplayName("ContentArray toolResultBlock utility")
    void toolResultBlockUtility() {
        Map<String, Object> block = ContentArray.toolResultBlock("tool-123", "Result");
        assertEquals("tool_result", block.get("type"));
        assertEquals("tool-123", block.get("tool_use_id"));
        assertEquals("Result", block.get("content"));
    }

    @Test
    @DisplayName("ContentArray imageBlock utility")
    void imageBlockUtility() {
        Map<String, Object> block = ContentArray.imageBlock("image/png", "base64data");
        assertEquals("image", block.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> source = (Map<String, Object>) block.get("source");
        assertEquals("base64", source.get("type"));
        assertEquals("image/png", source.get("media_type"));
        assertEquals("base64data", source.get("data"));
    }
}