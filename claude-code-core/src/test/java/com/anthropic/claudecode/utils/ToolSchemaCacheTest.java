/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ToolSchemaCache.
 */
class ToolSchemaCacheTest {

    @BeforeEach
    void setUp() {
        ToolSchemaCache.clear();
    }

    @Test
    @DisplayName("ToolSchemaCache put and get")
    void putAndGet() {
        ToolSchemaCache.put("tool1", "{\"type\":\"object\"}");
        assertEquals("{\"type\":\"object\"}", ToolSchemaCache.get("tool1"));
    }

    @Test
    @DisplayName("ToolSchemaCache get missing returns null")
    void getMissing() {
        assertNull(ToolSchemaCache.get("missing-tool"));
    }

    @Test
    @DisplayName("ToolSchemaCache contains returns true for cached tool")
    void containsTrue() {
        ToolSchemaCache.put("tool1", "{}");
        assertTrue(ToolSchemaCache.contains("tool1"));
    }

    @Test
    @DisplayName("ToolSchemaCache contains returns false for missing tool")
    void containsFalse() {
        assertFalse(ToolSchemaCache.contains("missing-tool"));
    }

    @Test
    @DisplayName("ToolSchemaCache remove removes cached schema")
    void remove() {
        ToolSchemaCache.put("tool1", "{}");
        ToolSchemaCache.remove("tool1");
        assertNull(ToolSchemaCache.get("tool1"));
        assertFalse(ToolSchemaCache.contains("tool1"));
    }

    @Test
    @DisplayName("ToolSchemaCache clear removes all schemas")
    void clear() {
        ToolSchemaCache.put("tool1", "{}");
        ToolSchemaCache.put("tool2", "{}");
        ToolSchemaCache.clear();
        assertEquals(0, ToolSchemaCache.size());
    }

    @Test
    @DisplayName("ToolSchemaCache size returns correct count")
    void size() {
        assertEquals(0, ToolSchemaCache.size());
        ToolSchemaCache.put("tool1", "{}");
        assertEquals(1, ToolSchemaCache.size());
        ToolSchemaCache.put("tool2", "{}");
        assertEquals(2, ToolSchemaCache.size());
    }

    @Test
    @DisplayName("ToolSchemaCache getCachedTools returns tool names")
    void getCachedTools() {
        ToolSchemaCache.put("tool1", "{}");
        ToolSchemaCache.put("tool2", "{}");
        Set<String> tools = ToolSchemaCache.getCachedTools();
        assertTrue(tools.contains("tool1"));
        assertTrue(tools.contains("tool2"));
    }

    @Test
    @DisplayName("ToolSchemaCache getOrCompute returns cached value")
    void getOrComputeCached() {
        ToolSchemaCache.put("tool1", "{}");
        String result = ToolSchemaCache.getOrCompute("tool1", () -> "{\"computed\":true}");
        assertEquals("{}", result);
    }

    @Test
    @DisplayName("ToolSchemaCache getOrCompute computes when missing")
    void getOrComputeComputes() {
        String result = ToolSchemaCache.getOrCompute("tool1", () -> "{\"computed\":true}");
        assertEquals("{\"computed\":true}", result);
        assertEquals("{\"computed\":true}", ToolSchemaCache.get("tool1"));
    }

    @Test
    @DisplayName("ToolSchemaCache put with custom TTL")
    void putWithCustomTTL() {
        ToolSchemaCache.put("tool1", "{}", 1000); // 1 second TTL
        assertEquals("{}", ToolSchemaCache.get("tool1"));
    }

    @Test
    @DisplayName("ToolSchemaCache evictExpired removes expired entries")
    void evictExpired() throws InterruptedException {
        ToolSchemaCache.put("tool1", "{}", 50); // 50ms TTL
        Thread.sleep(100);
        ToolSchemaCache.evictExpired();
        assertNull(ToolSchemaCache.get("tool1"));
    }

    @Test
    @DisplayName("ToolSchemaCache get returns null for expired entry")
    void getExpired() throws InterruptedException {
        ToolSchemaCache.put("tool1", "{}", 50); // 50ms TTL
        Thread.sleep(100);
        assertNull(ToolSchemaCache.get("tool1"));
    }

    @Test
    @DisplayName("ToolSchemaCache contains returns false for expired entry")
    void containsExpired() throws InterruptedException {
        ToolSchemaCache.put("tool1", "{}", 50); // 50ms TTL
        Thread.sleep(100);
        assertFalse(ToolSchemaCache.contains("tool1"));
    }

    @Test
    @DisplayName("ToolSchemaCache handles null supplier result")
    void getOrComputeNullSupplier() {
        String result = ToolSchemaCache.getOrCompute("tool1", () -> null);
        assertNull(result);
        assertFalse(ToolSchemaCache.contains("tool1"));
    }

    @Test
    @DisplayName("ToolSchemaCache handles null schema")
    void putNullSchema() {
        ToolSchemaCache.put("tool1", null);
        assertNull(ToolSchemaCache.get("tool1"));
    }
}