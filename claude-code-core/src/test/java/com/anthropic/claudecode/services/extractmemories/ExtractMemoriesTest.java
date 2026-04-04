/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.extractmemories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExtractMemories.
 */
class ExtractMemoriesTest {

    private ExtractMemories extractMemories;

    @BeforeEach
    void setUp() {
        extractMemories = new ExtractMemories();
    }

    @Test
    @DisplayName("ExtractMemories ExtractionContext record")
    void extractionContextRecord() {
        ExtractMemories.ExtractionContext context = new ExtractMemories.ExtractionContext(
            List.of("message1", "message2"),
            "toolContext",
            "agent-123"
        );

        assertEquals(2, context.messages().size());
        assertEquals("toolContext", context.toolUseContext());
        assertEquals("agent-123", context.agentId());
    }

    @Test
    @DisplayName("ExtractMemories ExtractionResult record")
    void extractionResultRecord() {
        ExtractMemories.ExtractionResult result = new ExtractMemories.ExtractionResult(
            true,
            List.of("/path/to/memory1.md", "/path/to/memory2.md"),
            5,
            1000L
        );

        assertTrue(result.success());
        assertEquals(2, result.writtenPaths().size());
        assertEquals(5, result.turnCount());
        assertEquals(1000L, result.durationMs());
    }

    @Test
    @DisplayName("ExtractMemories ExtractionResult empty")
    void extractionResultEmpty() {
        ExtractMemories.ExtractionResult result = ExtractMemories.ExtractionResult.empty();

        assertTrue(result.success());
        assertTrue(result.writtenPaths().isEmpty());
        assertEquals(0, result.turnCount());
        assertEquals(0, result.durationMs());
    }

    @Test
    @DisplayName("ExtractMemories init does not throw")
    void init() {
        extractMemories.init();
    }

    @Test
    @DisplayName("ExtractMemories setGateEnabled")
    void setGateEnabled() {
        extractMemories.setGateEnabled(true);
        extractMemories.setGateEnabled(false);
    }

    @Test
    @DisplayName("ExtractMemories executeExtractMemories without init returns completed")
    void executeWithoutInit() throws Exception {
        ExtractMemories.ExtractionContext context = new ExtractMemories.ExtractionContext(
            Collections.emptyList(), null, null
        );

        extractMemories.executeExtractMemories(context, null).get();
        // Should complete without error
    }

    @Test
    @DisplayName("ExtractMemories executeExtractMemories with init")
    void executeWithInit() throws Exception {
        extractMemories.init();
        extractMemories.setGateEnabled(true);

        ExtractMemories.ExtractionContext context = new ExtractMemories.ExtractionContext(
            Collections.emptyList(), null, null
        );

        extractMemories.executeExtractMemories(context, null).get();
    }

    @Test
    @DisplayName("ExtractMemories drainPendingExtraction returns completed")
    void drainPendingExtraction() throws Exception {
        extractMemories.drainPendingExtraction(1000).get();
    }

    @Test
    @DisplayName("ExtractMemories shutdown does not throw")
    void shutdown() {
        extractMemories.shutdown();
    }
}