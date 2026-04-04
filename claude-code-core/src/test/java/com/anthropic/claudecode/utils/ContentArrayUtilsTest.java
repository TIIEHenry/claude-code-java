/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ContentArrayUtils.
 */
class ContentArrayUtilsTest {

    @Test
    @DisplayName("ContentArrayUtils insertBlockAfterToolResults with tool_result")
    void insertBlockAfterToolResults() {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(ContentArrayUtils.textBlock("text1"));
        content.add(ContentArrayUtils.toolResultBlock("id1", "result1"));
        content.add(ContentArrayUtils.textBlock("text2"));

        Map<String, Object> block = ContentArrayUtils.textBlock("inserted");
        ContentArrayUtils.insertBlockAfterToolResults(content, block);

        // Inserted after tool_result at index 1, so goes to index 2
        assertEquals(4, content.size()); // 3 original + 1 inserted
        assertEquals("inserted", content.get(2).get("text"));
    }

    @Test
    @DisplayName("ContentArrayUtils insertBlockAfterToolResults no tool_result")
    void insertBlockAfterToolResultsNoToolResult() {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(ContentArrayUtils.textBlock("text1"));
        content.add(ContentArrayUtils.textBlock("text2"));

        Map<String, Object> block = ContentArrayUtils.textBlock("inserted");
        ContentArrayUtils.insertBlockAfterToolResults(content, block);

        assertEquals(3, content.size());
        assertEquals("inserted", content.get(1).get("text"));
    }

    @Test
    @DisplayName("ContentArrayUtils insertBlockAfterToolResults empty list")
    void insertBlockAfterToolResultsEmpty() {
        List<Map<String, Object>> content = new ArrayList<>();

        Map<String, Object> block = ContentArrayUtils.textBlock("inserted");
        ContentArrayUtils.insertBlockAfterToolResults(content, block);

        assertEquals(1, content.size());
    }

    @Test
    @DisplayName("ContentArrayUtils findLastToolResultIndex returns index")
    void findLastToolResultIndex() {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(ContentArrayUtils.toolResultBlock("id1", "result1"));
        content.add(ContentArrayUtils.textBlock("text"));
        content.add(ContentArrayUtils.toolResultBlock("id2", "result2"));

        int index = ContentArrayUtils.findLastToolResultIndex(content);
        assertEquals(2, index);
    }

    @Test
    @DisplayName("ContentArrayUtils findLastToolResultIndex returns -1 if none")
    void findLastToolResultIndexNone() {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(ContentArrayUtils.textBlock("text1"));
        content.add(ContentArrayUtils.textBlock("text2"));

        int index = ContentArrayUtils.findLastToolResultIndex(content);
        assertEquals(-1, index);
    }

    @Test
    @DisplayName("ContentArrayUtils findLastToolResultIndex empty list")
    void findLastToolResultIndexEmpty() {
        List<Map<String, Object>> content = new ArrayList<>();
        int index = ContentArrayUtils.findLastToolResultIndex(content);
        assertEquals(-1, index);
    }

    @Test
    @DisplayName("ContentArrayUtils hasToolResults true")
    void hasToolResultsTrue() {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(ContentArrayUtils.textBlock("text"));
        content.add(ContentArrayUtils.toolResultBlock("id", "result"));

        assertTrue(ContentArrayUtils.hasToolResults(content));
    }

    @Test
    @DisplayName("ContentArrayUtils hasToolResults false")
    void hasToolResultsFalse() {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(ContentArrayUtils.textBlock("text1"));
        content.add(ContentArrayUtils.textBlock("text2"));

        assertFalse(ContentArrayUtils.hasToolResults(content));
    }

    @Test
    @DisplayName("ContentArrayUtils hasToolResults empty")
    void hasToolResultsEmpty() {
        List<Map<String, Object>> content = new ArrayList<>();
        assertFalse(ContentArrayUtils.hasToolResults(content));
    }

    @Test
    @DisplayName("ContentArrayUtils textBlock creates correct block")
    void textBlock() {
        Map<String, Object> block = ContentArrayUtils.textBlock("hello world");

        assertEquals("text", block.get("type"));
        assertEquals("hello world", block.get("text"));
        assertEquals(2, block.size());
    }

    @Test
    @DisplayName("ContentArrayUtils toolResultBlock creates correct block")
    void toolResultBlock() {
        Map<String, Object> block = ContentArrayUtils.toolResultBlock("tool-123", "result content");

        assertEquals("tool_result", block.get("type"));
        assertEquals("tool-123", block.get("tool_use_id"));
        assertEquals("result content", block.get("content"));
        assertEquals(3, block.size());
    }

    @Test
    @DisplayName("ContentArrayUtils handles null items")
    void handlesNullItems() {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(null);
        content.add(ContentArrayUtils.toolResultBlock("id", "result"));
        content.add(null);

        assertTrue(ContentArrayUtils.hasToolResults(content));
        assertEquals(1, ContentArrayUtils.findLastToolResultIndex(content));
    }

    @Test
    @DisplayName("ContentArrayUtils multiple tool_results")
    void multipleToolResults() {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(ContentArrayUtils.toolResultBlock("id1", "result1"));
        content.add(ContentArrayUtils.toolResultBlock("id2", "result2"));
        content.add(ContentArrayUtils.toolResultBlock("id3", "result3"));

        Map<String, Object> block = ContentArrayUtils.textBlock("inserted");
        ContentArrayUtils.insertBlockAfterToolResults(content, block);

        // Should insert after id3 (last tool_result)
        assertEquals("inserted", content.get(3).get("text"));
    }

    @Test
    @DisplayName("ContentArrayUtils adds continuation when inserted at end")
    void addsContinuation() {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(ContentArrayUtils.toolResultBlock("id", "result"));

        Map<String, Object> block = ContentArrayUtils.textBlock("inserted");
        ContentArrayUtils.insertBlockAfterToolResults(content, block);

        // Should add continuation text block
        assertEquals(3, content.size());
        assertEquals("text", content.get(2).get("type"));
    }
}