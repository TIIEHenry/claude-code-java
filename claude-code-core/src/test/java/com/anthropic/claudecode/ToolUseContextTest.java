/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for ToolUseContext.
 */
@DisplayName("ToolUseContext Tests")
class ToolUseContextTest {

    @Test
    @DisplayName("ToolUseContext empty creates valid context")
    void emptyCreatesValidContext() {
        ToolUseContext ctx = ToolUseContext.empty();

        assertNotNull(ctx);
        assertNotNull(ctx.options());
        assertNotNull(ctx.abortController());
        assertNotNull(ctx.readFileState());
        assertNotNull(ctx.messages());
    }

    @Test
    @DisplayName("ToolUseContext withToolUseId creates new context")
    void withToolUseIdCreatesNewContext() {
        ToolUseContext ctx = ToolUseContext.empty();
        ToolUseContext newCtx = ctx.withToolUseId("tool-123");

        assertEquals("tool-123", newCtx.toolUseId());
        // Original should be unchanged
        assertNull(ctx.toolUseId());
    }

    @Test
    @DisplayName("ToolUseContext addInProgressToolUseId does not throw")
    void addInProgressToolUseIdDoesNotThrow() {
        ToolUseContext ctx = ToolUseContext.empty();

        // Should not throw even with null consumer
        assertDoesNotThrow(() -> ctx.addInProgressToolUseId("tool-123"));
    }

    @Test
    @DisplayName("ToolUseContext removeInProgressToolUseId does not throw")
    void removeInProgressToolUseIdDoesNotThrow() {
        ToolUseContext ctx = ToolUseContext.empty();

        // Should not throw even with null consumer
        assertDoesNotThrow(() -> ctx.removeInProgressToolUseId("tool-123"));
    }

    @Test
    @DisplayName("ToolUseContext with consumers tracks tool use IDs")
    void withConsumersTracksToolUseIds() {
        Set<String> trackedIds = new HashSet<>();

        ToolUseContext ctx = new ToolUseContext(
            ToolUseContext.ToolUseOptions.empty(),
            new ToolUseContext.AbortController(),
            ToolUseContext.FileStateCache.empty(),
            v -> Map.of("inProgressToolUseIDs", trackedIds),
            state -> null,
            ids -> {
                trackedIds.clear();
                trackedIds.addAll(ids);
            },
            len -> {},
            mode -> {},
            state -> state,
            state -> state,
            List.of(),
            null,
            null,
            Map.of(),
            null,
            null,
            null,
            null,
            false,
            null,
            null,
            null
        );

        ctx.addInProgressToolUseId("tool-1");
        assertTrue(trackedIds.contains("tool-1"));

        ctx.addInProgressToolUseId("tool-2");
        assertTrue(trackedIds.contains("tool-1"));
        assertTrue(trackedIds.contains("tool-2"));

        ctx.removeInProgressToolUseId("tool-1");
        assertFalse(trackedIds.contains("tool-1"));
        assertTrue(trackedIds.contains("tool-2"));
    }

    @Test
    @DisplayName("ToolUseOptions empty creates valid options")
    void toolUseOptionsEmptyCreatesValidOptions() {
        ToolUseContext.ToolUseOptions options = ToolUseContext.ToolUseOptions.empty();

        assertNotNull(options);
        assertTrue(options.commands().isEmpty());
        assertFalse(options.debug());
        assertTrue(options.tools().isEmpty());
    }

    @Test
    @DisplayName("AbortController record works correctly")
    void abortControllerWorksCorrectly() {
        // Test with explicit values
        Runnable abortRunnable = () -> {};
        ToolUseContext.AbortController controller = new ToolUseContext.AbortController(true, abortRunnable);

        assertTrue(controller.aborted());
        assertEquals(abortRunnable, controller.abort());
    }

    @Test
    @DisplayName("AbortController default constructor creates not-aborted controller")
    void abortControllerDefaultConstructor() {
        ToolUseContext.AbortController controller = new ToolUseContext.AbortController();

        assertFalse(controller.aborted());
        assertNotNull(controller.abort());
        // abort() does nothing by default
        assertDoesNotThrow(() -> controller.abort().run());
    }

    @Test
    @DisplayName("FileStateCache empty creates valid cache")
    void fileStateCacheEmptyCreatesValidCache() {
        ToolUseContext.FileStateCache cache = ToolUseContext.FileStateCache.empty();

        assertNotNull(cache);
        assertTrue(cache.cache().isEmpty());
    }

    @Test
    @DisplayName("FileReadingLimits record works")
    void fileReadingLimitsRecordWorks() {
        ToolUseContext.FileReadingLimits limits = new ToolUseContext.FileReadingLimits(1000, 5000);

        assertEquals(1000, limits.maxTokens());
        assertEquals(5000, limits.maxSizeBytes());
    }

    @Test
    @DisplayName("GlobLimits record works")
    void globLimitsRecordWorks() {
        ToolUseContext.GlobLimits limits = new ToolUseContext.GlobLimits(100);

        assertEquals(100, limits.maxResults());
    }

    @Test
    @DisplayName("ToolDecision record works")
    void toolDecisionRecordWorks() {
        ToolUseContext.ToolDecision decision = new ToolUseContext.ToolDecision(
            "user", "allow", System.currentTimeMillis()
        );

        assertEquals("user", decision.source());
        assertEquals("allow", decision.decision());
        assertTrue(decision.timestamp() > 0);
    }

    @Test
    @DisplayName("QueryChainTracking record works")
    void queryChainTrackingRecordWorks() {
        ToolUseContext.QueryChainTracking tracking = new ToolUseContext.QueryChainTracking(
            "chain-123", 2
        );

        assertEquals("chain-123", tracking.chainId());
        assertEquals(2, tracking.depth());
    }

    @Test
    @DisplayName("PromptRequest and PromptResponse records work")
    void promptRecordsWork() {
        ToolUseContext.PromptRequest request = new ToolUseContext.PromptRequest(
            "Bash", "ls -la"
        );

        assertEquals("Bash", request.sourceName());
        assertEquals("ls -la", request.toolInputSummary());

        ToolUseContext.PromptResponse response = new ToolUseContext.PromptResponse(
            "allow", "looks safe"
        );

        assertEquals("allow", response.decision());
        assertEquals("looks safe", response.feedback());
    }

    @Test
    @DisplayName("FileHistoryState and FileSnapshot records work")
    void fileHistoryRecordsWork() {
        ToolUseContext.FileSnapshot snapshot = new ToolUseContext.FileSnapshot(
            "uuid-123", "file content", System.currentTimeMillis()
        );

        assertEquals("uuid-123", snapshot.uuid());
        assertEquals("file content", snapshot.content());

        ToolUseContext.FileHistoryState state = new ToolUseContext.FileHistoryState(
            Map.of("file.txt", snapshot)
        );

        assertNotNull(state.snapshots());
        assertEquals(1, state.snapshots().size());
    }

    @Test
    @DisplayName("AttributionState record works")
    void attributionStateRecordWorks() {
        ToolUseContext.AttributionState state = new ToolUseContext.AttributionState(
            "Claude", "Claude Code"
        );

        assertEquals("Claude", state.author());
        assertEquals("Claude Code", state.coAuthor());
    }

    @Test
    @DisplayName("DenialTrackingState record works")
    void denialTrackingStateRecordWorks() {
        ToolUseContext.DenialTrackingState state = new ToolUseContext.DenialTrackingState(
            3, System.currentTimeMillis()
        );

        assertEquals(3, state.count());
        assertTrue(state.lastDenialTime() > 0);
    }

    @Test
    @DisplayName("ContentReplacementState record works")
    void contentReplacementStateRecordWorks() {
        ToolUseContext.ReplacementEntry entry = new ToolUseContext.ReplacementEntry(
            "key", "value", System.currentTimeMillis()
        );

        ToolUseContext.ContentReplacementState state = new ToolUseContext.ContentReplacementState(
            Map.of("key", entry)
        );

        assertEquals(1, state.entries().size());
        assertEquals("value", state.entries().get("key").value());
    }
}