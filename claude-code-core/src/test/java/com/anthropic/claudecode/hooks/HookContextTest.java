/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.hooks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HookContext.
 */
class HookContextTest {

    @Test
    @DisplayName("HookContext simple constructor")
    void simpleConstructor() {
        HookContext ctx = new HookContext("PreToolUse");

        assertEquals("PreToolUse", ctx.event());
        assertNull(ctx.toolName());
        assertNull(ctx.input());
        assertNull(ctx.output());
        assertNull(ctx.error());
        assertTrue(ctx.timestamp() > 0);
        assertTrue(ctx.metadata().isEmpty());
    }

    @Test
    @DisplayName("HookContext constructor with tool info")
    void constructorWithToolInfo() {
        HookContext ctx = new HookContext("PostToolUse", "bash", "ls -la");

        assertEquals("PostToolUse", ctx.event());
        assertEquals("bash", ctx.toolName());
        assertEquals("ls -la", ctx.input());
        assertNull(ctx.output());
        assertNull(ctx.error());
        assertTrue(ctx.timestamp() > 0);
    }

    @Test
    @DisplayName("HookContext full record")
    void fullRecord() {
        RuntimeException err = new RuntimeException("test error");
        Map<String, Object> meta = Map.of("key", "value");

        HookContext ctx = new HookContext(
            "OnError", "read", "file.txt", "content", err, 12345L, meta
        );

        assertEquals("OnError", ctx.event());
        assertEquals("read", ctx.toolName());
        assertEquals("file.txt", ctx.input());
        assertEquals("content", ctx.output());
        assertEquals(err, ctx.error());
        assertEquals(12345L, ctx.timestamp());
        assertEquals("value", ctx.metadata().get("key"));
    }

    @Test
    @DisplayName("HookContext builder")
    void builder() {
        RuntimeException err = new RuntimeException("error");

        HookContext ctx = HookContext.builder()
            .event("PreToolUse")
            .toolName("write")
            .input("path/to/file")
            .output("written")
            .error(err)
            .timestamp(99999L)
            .metadata("custom", "data")
            .build();

        assertEquals("PreToolUse", ctx.event());
        assertEquals("write", ctx.toolName());
        assertEquals("path/to/file", ctx.input());
        assertEquals("written", ctx.output());
        assertEquals(err, ctx.error());
        assertEquals(99999L, ctx.timestamp());
        assertEquals("data", ctx.metadata().get("custom"));
    }

    @Test
    @DisplayName("HookContext builder default timestamp")
    void builderDefaultTimestamp() {
        HookContext ctx = HookContext.builder()
            .event("test")
            .build();

        assertTrue(ctx.timestamp() > 0);
    }

    @Test
    @DisplayName("HookContext builder empty metadata")
    void builderEmptyMetadata() {
        HookContext ctx = HookContext.builder()
            .event("test")
            .build();

        assertTrue(ctx.metadata().isEmpty());
    }

    @Test
    @DisplayName("HookContext builder multiple metadata")
    void builderMultipleMetadata() {
        HookContext ctx = HookContext.builder()
            .event("test")
            .metadata("key1", "val1")
            .metadata("key2", "val2")
            .build();

        assertEquals(2, ctx.metadata().size());
        assertEquals("val1", ctx.metadata().get("key1"));
        assertEquals("val2", ctx.metadata().get("key2"));
    }
}