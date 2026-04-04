/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ContextSuggestions.
 */
class ContextSuggestionsTest {

    @Test
    @DisplayName("ContextSuggestions Severity enum")
    void severityEnum() {
        assertEquals(2, ContextSuggestions.Severity.values().length);
        assertEquals(ContextSuggestions.Severity.INFO, ContextSuggestions.Severity.values()[0]);
        assertEquals(ContextSuggestions.Severity.WARNING, ContextSuggestions.Severity.values()[1]);
    }

    @Test
    @DisplayName("ContextSuggestions ContextSuggestion record")
    void contextSuggestionRecord() {
        ContextSuggestions.ContextSuggestion suggestion = new ContextSuggestions.ContextSuggestion(
            ContextSuggestions.Severity.WARNING,
            "Test Title",
            "Test Detail",
            1000
        );

        assertEquals(ContextSuggestions.Severity.WARNING, suggestion.severity());
        assertEquals("Test Title", suggestion.title());
        assertEquals("Test Detail", suggestion.detail());
        assertEquals(1000, suggestion.savingsTokens());
    }

    @Test
    @DisplayName("ContextSuggestions ToolCallBreakdown record")
    void toolCallBreakdownRecord() {
        ContextSuggestions.ToolCallBreakdown breakdown = new ContextSuggestions.ToolCallBreakdown(
            "Read", 100, 500
        );

        assertEquals("Read", breakdown.name());
        assertEquals(100, breakdown.callTokens());
        assertEquals(500, breakdown.resultTokens());
    }

    @Test
    @DisplayName("ContextSuggestions MemoryFile record")
    void memoryFileRecord() {
        ContextSuggestions.MemoryFile file = new ContextSuggestions.MemoryFile("/path/to/file", 1000);
        assertEquals("/path/to/file", file.path());
        assertEquals(1000, file.tokens());
    }

    @Test
    @DisplayName("ContextSuggestions MessageBreakdown record")
    void messageBreakdownRecord() {
        ContextSuggestions.MessageBreakdown breakdown = new ContextSuggestions.MessageBreakdown(
            List.of(new ContextSuggestions.ToolCallBreakdown("Bash", 50, 200))
        );

        assertEquals(1, breakdown.toolCallsByType().size());
    }

    @Test
    @DisplayName("ContextSuggestions ContextData record")
    void contextDataRecord() {
        ContextSuggestions.ContextData data = new ContextSuggestions.ContextData(
            50, true, 100000, null, new ArrayList<>()
        );

        assertEquals(50, data.percentage());
        assertTrue(data.isAutoCompactEnabled());
        assertEquals(100000, data.rawMaxTokens());
    }

    @Test
    @DisplayName("ContextSuggestions generateContextSuggestions empty")
    void generateContextSuggestionsEmpty() {
        ContextSuggestions.ContextData data = new ContextSuggestions.ContextData(
            10, true, 100000, null, new ArrayList<>()
        );

        List<ContextSuggestions.ContextSuggestion> suggestions =
            ContextSuggestions.generateContextSuggestions(data);

        // Low usage with autocompact should have no suggestions
        assertTrue(suggestions.isEmpty() || suggestions.size() == 0);
    }

    @Test
    @DisplayName("ContextSuggestions generateContextSuggestions near capacity warning")
    void generateContextSuggestionsNearCapacity() {
        ContextSuggestions.ContextData data = new ContextSuggestions.ContextData(
            85, false, 100000, null, new ArrayList<>()
        );

        List<ContextSuggestions.ContextSuggestion> suggestions =
            ContextSuggestions.generateContextSuggestions(data);

        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.get(0).title().contains("Context"));
        assertEquals(ContextSuggestions.Severity.WARNING, suggestions.get(0).severity());
    }

    @Test
    @DisplayName("ContextSuggestions generateContextSuggestions autocompact disabled info")
    void generateContextSuggestionsAutocompactDisabled() {
        ContextSuggestions.ContextData data = new ContextSuggestions.ContextData(
            60, false, 100000, null, new ArrayList<>()
        );

        List<ContextSuggestions.ContextSuggestion> suggestions =
            ContextSuggestions.generateContextSuggestions(data);

        // Should have suggestion about autocompact
        boolean hasAutocompactSuggestion = suggestions.stream()
            .anyMatch(s -> s.title().contains("Autocompact"));

        assertTrue(hasAutocompactSuggestion);
    }

    @Test
    @DisplayName("ContextSuggestions generateContextSuggestions large tool result")
    void generateContextSuggestionsLargeToolResult() {
        ContextSuggestions.ToolCallBreakdown bashBreakdown = new ContextSuggestions.ToolCallBreakdown(
            "Bash", 100, 20000
        );

        ContextSuggestions.MessageBreakdown breakdown = new ContextSuggestions.MessageBreakdown(
            List.of(bashBreakdown)
        );

        ContextSuggestions.ContextData data = new ContextSuggestions.ContextData(
            50, true, 100000, breakdown, new ArrayList<>()
        );

        List<ContextSuggestions.ContextSuggestion> suggestions =
            ContextSuggestions.generateContextSuggestions(data);

        // Should have suggestion about Bash
        boolean hasBashSuggestion = suggestions.stream()
            .anyMatch(s -> s.title().contains("Bash"));

        assertTrue(hasBashSuggestion);
    }

    @Test
    @DisplayName("ContextSuggestions generateContextSuggestions memory bloat")
    void generateContextSuggestionsMemoryBloat() {
        List<ContextSuggestions.MemoryFile> memoryFiles = List.of(
            new ContextSuggestions.MemoryFile("/path/to/large.md", 8000)
        );

        ContextSuggestions.ContextData data = new ContextSuggestions.ContextData(
            50, true, 100000, null, memoryFiles
        );

        List<ContextSuggestions.ContextSuggestion> suggestions =
            ContextSuggestions.generateContextSuggestions(data);

        // Should have memory suggestion
        boolean hasMemorySuggestion = suggestions.stream()
            .anyMatch(s -> s.title().contains("Memory"));

        assertTrue(hasMemorySuggestion);
    }
}