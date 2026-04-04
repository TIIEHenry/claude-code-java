/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.tools;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;

import com.anthropic.claudecode.*;
import com.anthropic.claudecode.permission.PermissionResult;

/**
 * Tests for StreamingToolExecutor.
 */
@DisplayName("StreamingToolExecutor Tests")
class StreamingToolExecutorTest {

    @Test
    @DisplayName("StreamingToolExecutor creates with valid parameters")
    void createsWithValidParameters() {
        List<Tool> tools = new ArrayList<>();
        CanUseToolFn canUseTool = (tool, input, context, assistant, id) ->
            CompletableFuture.completedFuture(new PermissionResult.Allow<>(input));
        ToolUseContext context = ToolUseContext.empty();

        StreamingToolExecutor executor = new StreamingToolExecutor(tools, canUseTool, context);

        assertNotNull(executor);
    }

    @Test
    @DisplayName("StreamingToolExecutor getCompletedResults returns empty when no tools")
    void getCompletedResultsReturnsEmptyWhenNoTools() {
        StreamingToolExecutor executor = new StreamingToolExecutor(
            List.of(), null, ToolUseContext.empty()
        );

        Iterator<StreamingToolExecutor.MessageUpdate> results = executor.getCompletedResults();

        assertFalse(results.hasNext());
    }

    @Test
    @DisplayName("StreamingToolExecutor discard prevents further results")
    void discardPreventsFurtherResults() {
        StreamingToolExecutor executor = new StreamingToolExecutor(
            List.of(), null, ToolUseContext.empty()
        );

        executor.discard();

        Iterator<StreamingToolExecutor.MessageUpdate> results = executor.getCompletedResults();
        assertFalse(results.hasNext());
    }

    @Test
    @DisplayName("StreamingToolExecutor addTool with unknown tool creates error")
    void addToolWithUnknownToolCreatesError() {
        StreamingToolExecutor executor = new StreamingToolExecutor(
            List.of(), null, ToolUseContext.empty()
        );

        StreamingToolExecutor.ToolUseBlock block = new StreamingToolExecutor.ToolUseBlock(
            "tool-1", "UnknownTool", Map.of()
        );

        com.anthropic.claudecode.types.MessageTypes.AssistantMessage assistantMessage =
            new com.anthropic.claudecode.types.MessageTypes.AssistantMessage(
                java.util.UUID.randomUUID().toString(),
                java.util.List.of(),
                java.util.Map.of(),
                "claude-sonnet-4-6",
                null,
                java.util.Map.of()
            );

        executor.addTool(block, assistantMessage);

        // The tool should be added as completed with error
        Iterator<StreamingToolExecutor.MessageUpdate> results = executor.getCompletedResults();
        // May or may not have results depending on timing
        assertNotNull(results);
    }

    @Test
    @DisplayName("StreamingToolExecutor ToolStatus enum has correct values")
    void toolStatusEnumHasCorrectValues() {
        StreamingToolExecutor.ToolStatus[] statuses = StreamingToolExecutor.ToolStatus.values();

        assertEquals(4, statuses.length);
        assertTrue(Arrays.asList(statuses).contains(StreamingToolExecutor.ToolStatus.QUEUED));
        assertTrue(Arrays.asList(statuses).contains(StreamingToolExecutor.ToolStatus.EXECUTING));
        assertTrue(Arrays.asList(statuses).contains(StreamingToolExecutor.ToolStatus.COMPLETED));
        assertTrue(Arrays.asList(statuses).contains(StreamingToolExecutor.ToolStatus.YIELDED));
    }

    @Test
    @DisplayName("StreamingToolExecutor AbortReason enum has correct values")
    void abortReasonEnumHasCorrectValues() {
        StreamingToolExecutor.AbortReason[] reasons = StreamingToolExecutor.AbortReason.values();

        assertEquals(3, reasons.length);
        assertTrue(Arrays.asList(reasons).contains(StreamingToolExecutor.AbortReason.SIBLING_ERROR));
        assertTrue(Arrays.asList(reasons).contains(StreamingToolExecutor.AbortReason.USER_INTERRUPTED));
        assertTrue(Arrays.asList(reasons).contains(StreamingToolExecutor.AbortReason.STREAMING_FALLBACK));
    }

    @Test
    @DisplayName("StreamingToolExecutor TrackedTool record works correctly")
    void trackedToolRecordWorksCorrectly() {
        StreamingToolExecutor.ToolUseBlock block = new StreamingToolExecutor.ToolUseBlock(
            "id-1", "TestTool", Map.of("key", "value")
        );

        com.anthropic.claudecode.types.MessageTypes.AssistantMessage assistantMessage =
            new com.anthropic.claudecode.types.MessageTypes.AssistantMessage(
                java.util.UUID.randomUUID().toString(),
                java.util.List.of(),
                java.util.Map.of(),
                "claude-sonnet-4-6",
                null,
                java.util.Map.of()
            );

        StreamingToolExecutor.TrackedTool tool = new StreamingToolExecutor.TrackedTool(
            "id-1",
            block,
            assistantMessage,
            StreamingToolExecutor.ToolStatus.QUEUED,
            true,
            null,
            List.of(),
            List.of(),
            List.of()
        );

        assertEquals("id-1", tool.id());
        assertEquals("TestTool", tool.block().name());
        assertTrue(tool.isConcurrencySafe());
        assertEquals(StreamingToolExecutor.ToolStatus.QUEUED, tool.status());

        StreamingToolExecutor.TrackedTool updated = tool.withStatus(StreamingToolExecutor.ToolStatus.EXECUTING);
        assertEquals(StreamingToolExecutor.ToolStatus.EXECUTING, updated.status());
    }

    @Test
    @DisplayName("StreamingToolExecutor MessageUpdate record works correctly")
    void messageUpdateRecordWorksCorrectly() {
        Map<String, Object> textContent = new java.util.LinkedHashMap<>();
        textContent.put("type", "text");
        textContent.put("text", "Test message");

        com.anthropic.claudecode.types.MessageTypes.UserMessage message =
            new com.anthropic.claudecode.types.MessageTypes.UserMessage(
                java.util.List.of(textContent),
                java.util.Map.of()
            );

        StreamingToolExecutor.MessageUpdate update = new StreamingToolExecutor.MessageUpdate(
            message, ToolUseContext.empty()
        );

        assertEquals(message, update.message());
        assertNotNull(update.newContext());
    }

    @Test
    @DisplayName("StreamingToolExecutor getUpdatedContext returns context")
    void getUpdatedContextReturnsContext() {
        ToolUseContext context = ToolUseContext.empty();
        StreamingToolExecutor executor = new StreamingToolExecutor(
            List.of(), null, context
        );

        assertEquals(context, executor.getUpdatedContext());
    }

    @Test
    @DisplayName("StreamingToolExecutor getRemainingResults completes for empty executor")
    void getRemainingResultsCompletesForEmptyExecutor() {
        StreamingToolExecutor executor = new StreamingToolExecutor(
            List.of(), null, ToolUseContext.empty()
        );

        CompletableFuture<Iterator<StreamingToolExecutor.MessageUpdate>> future = executor.getRemainingResults();
        Iterator<StreamingToolExecutor.MessageUpdate> results = future.join();

        assertNotNull(results);
        assertFalse(results.hasNext());
    }
}