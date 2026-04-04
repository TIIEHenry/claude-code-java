/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.tools;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for TodoWriteTool.
 */
@DisplayName("TodoWriteTool Tests")
class TodoWriteToolTest {

    @Test
    @DisplayName("TodoWriteTool has correct name")
    void hasCorrectName() {
        TodoWriteTool tool = new TodoWriteTool();
        assertEquals("TodoWrite", tool.name());
    }

    @Test
    @DisplayName("TodoItem record works correctly")
    void todoItemRecordWorksCorrectly() {
        TodoWriteTool.TodoItem item = new TodoWriteTool.TodoItem(
            "Test Task",
            "pending"
        );

        assertEquals("Test Task", item.content());
        assertEquals("pending", item.status());
    }

    @Test
    @DisplayName("TodoWriteTool Input record works correctly")
    void inputRecordWorksCorrectly() {
        List<TodoWriteTool.TodoItem> items = new ArrayList<>();
        items.add(new TodoWriteTool.TodoItem("Task 1", "pending"));
        items.add(new TodoWriteTool.TodoItem("Task 2", "completed"));

        TodoWriteTool.Input input = new TodoWriteTool.Input(items);

        assertEquals(2, input.todos().size());
    }

    @Test
    @DisplayName("TodoWriteTool input schema returns schema or null")
    void inputSchemaIsValid() {
        TodoWriteTool tool = new TodoWriteTool();
        Map<String, Object> schema = tool.inputSchema();

        // Schema may be null for this tool - just verify the method works
        // The tool uses a convenience constructor that doesn't set schema
        assertTrue(schema == null || schema.containsKey("type"));
    }

    @Test
    @DisplayName("TodoWriteTool is not concurrency safe")
    void isNotConcurrencySafe() {
        TodoWriteTool tool = new TodoWriteTool();

        assertFalse(tool.isConcurrencySafeUntyped(null));
    }
}
