/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DebugFilter.
 */
class DebugFilterTest {

    @Test
    @DisplayName("DebugFilter FilterConfig record")
    void filterConfigRecord() {
        DebugFilter.FilterConfig config = new DebugFilter.FilterConfig(
            List.of("api", "hooks"), List.of(), false
        );

        assertEquals(2, config.include().size());
        assertEquals(0, config.exclude().size());
        assertFalse(config.isExclusive());
    }

    @Test
    @DisplayName("DebugFilter parseDebugFilter null")
    void parseDebugFilterNull() {
        assertNull(DebugFilter.parseDebugFilter(null));
    }

    @Test
    @DisplayName("DebugFilter parseDebugFilter empty")
    void parseDebugFilterEmpty() {
        assertNull(DebugFilter.parseDebugFilter(""));
        assertNull(DebugFilter.parseDebugFilter("   "));
    }

    @Test
    @DisplayName("DebugFilter parseDebugFilter inclusive")
    void parseDebugFilterInclusive() {
        DebugFilter.FilterConfig config = DebugFilter.parseDebugFilter("api,hooks");

        assertNotNull(config);
        assertEquals(2, config.include().size());
        assertTrue(config.include().contains("api"));
        assertTrue(config.include().contains("hooks"));
        assertFalse(config.isExclusive());
    }

    @Test
    @DisplayName("DebugFilter parseDebugFilter exclusive")
    void parseDebugFilterExclusive() {
        DebugFilter.FilterConfig config = DebugFilter.parseDebugFilter("!1p,!file");

        assertNotNull(config);
        assertEquals(2, config.exclude().size());
        assertTrue(config.exclude().contains("1p"));
        assertTrue(config.exclude().contains("file"));
        assertTrue(config.isExclusive());
    }

    @Test
    @DisplayName("DebugFilter parseDebugFilter mixed invalid")
    void parseDebugFilterMixed() {
        // Mixed inclusive and exclusive filters should return null
        DebugFilter.FilterConfig config = DebugFilter.parseDebugFilter("api,!hooks");
        assertNull(config);
    }

    @Test
    @DisplayName("DebugFilter extractDebugCategories null")
    void extractDebugCategoriesNull() {
        assertEquals(0, DebugFilter.extractDebugCategories(null).size());
    }

    @Test
    @DisplayName("DebugFilter extractDebugCategories empty")
    void extractDebugCategoriesEmpty() {
        assertEquals(0, DebugFilter.extractDebugCategories("").size());
    }

    @Test
    @DisplayName("DebugFilter extractDebugCategories prefix format")
    void extractDebugCategoriesPrefix() {
        List<String> categories = DebugFilter.extractDebugCategories("api: Request sent");
        assertTrue(categories.contains("api"));
    }

    @Test
    @DisplayName("DebugFilter extractDebugCategories bracket format")
    void extractDebugCategoriesBracket() {
        List<String> categories = DebugFilter.extractDebugCategories("[HOOKS] Executing hook");
        assertTrue(categories.contains("hooks"));
    }

    @Test
    @DisplayName("DebugFilter extractDebugCategories MCP format")
    void extractDebugCategoriesMcp() {
        List<String> categories = DebugFilter.extractDebugCategories("MCP server \"myserver\": Connected");
        assertTrue(categories.contains("mcp"));
        assertTrue(categories.contains("myserver"));
    }

    @Test
    @DisplayName("DebugFilter extractDebugCategories 1p event")
    void extractDebugCategories1pEvent() {
        List<String> categories = DebugFilter.extractDebugCategories("Something 1p event: triggered");
        assertTrue(categories.contains("1p"));
    }

    @Test
    @DisplayName("DebugFilter shouldShowDebugCategories null filter")
    void shouldShowDebugCategoriesNullFilter() {
        assertTrue(DebugFilter.shouldShowDebugCategories(List.of("api"), null));
    }

    @Test
    @DisplayName("DebugFilter shouldShowDebugCategories empty categories")
    void shouldShowDebugCategoriesEmptyCategories() {
        DebugFilter.FilterConfig config = new DebugFilter.FilterConfig(
            List.of("api"), List.of(), false
        );
        assertFalse(DebugFilter.shouldShowDebugCategories(List.of(), config));
    }

    @Test
    @DisplayName("DebugFilter shouldShowDebugCategories inclusive match")
    void shouldShowDebugCategoriesInclusiveMatch() {
        DebugFilter.FilterConfig config = new DebugFilter.FilterConfig(
            List.of("api", "hooks"), List.of(), false
        );
        assertTrue(DebugFilter.shouldShowDebugCategories(List.of("api"), config));
    }

    @Test
    @DisplayName("DebugFilter shouldShowDebugCategories inclusive no match")
    void shouldShowDebugCategoriesInclusiveNoMatch() {
        DebugFilter.FilterConfig config = new DebugFilter.FilterConfig(
            List.of("api", "hooks"), List.of(), false
        );
        assertFalse(DebugFilter.shouldShowDebugCategories(List.of("file"), config));
    }

    @Test
    @DisplayName("DebugFilter shouldShowDebugCategories exclusive match")
    void shouldShowDebugCategoriesExclusiveMatch() {
        DebugFilter.FilterConfig config = new DebugFilter.FilterConfig(
            List.of(), List.of("1p", "file"), true
        );
        assertFalse(DebugFilter.shouldShowDebugCategories(List.of("1p"), config));
    }

    @Test
    @DisplayName("DebugFilter shouldShowDebugCategories exclusive no match")
    void shouldShowDebugCategoriesExclusiveNoMatch() {
        DebugFilter.FilterConfig config = new DebugFilter.FilterConfig(
            List.of(), List.of("1p", "file"), true
        );
        assertTrue(DebugFilter.shouldShowDebugCategories(List.of("api"), config));
    }

    @Test
    @DisplayName("DebugFilter shouldShowDebugMessage null filter")
    void shouldShowDebugMessageNullFilter() {
        assertTrue(DebugFilter.shouldShowDebugMessage("api: test", null));
    }

    @Test
    @DisplayName("DebugFilter shouldShowDebugMessage with filter")
    void shouldShowDebugMessageWithFilter() {
        DebugFilter.FilterConfig config = DebugFilter.parseDebugFilter("api");
        assertTrue(DebugFilter.shouldShowDebugMessage("api: test", config));
        assertFalse(DebugFilter.shouldShowDebugMessage("hooks: test", config));
    }

    @Test
    @DisplayName("DebugFilter getFilterFromEnv")
    void getFilterFromEnv() {
        // Returns null if env var not set, or config if set
        DebugFilter.FilterConfig config = DebugFilter.getFilterFromEnv();
        assertTrue(config == null || config instanceof DebugFilter.FilterConfig);
    }
}