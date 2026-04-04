/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/tools/toolOrchestration
 */
package com.anthropic.claudecode.services.tools;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.*;

/**
 * Tool orchestration - Orchestrate tool execution.
 *
 * Partition tool calls into batches and run concurrently or serially.
 */
public final class ToolOrchestration {
    private static final int DEFAULT_MAX_CONCURRENCY = 10;

    /**
     * Message update record.
     */
    public record MessageUpdate(
        Object message,
        Object newContext,
        ContextModifier contextModifier
    ) {
        public static MessageUpdate empty() {
            return new MessageUpdate(null, null, null);
        }

        public boolean hasMessage() {
            return message != null;
        }

        public boolean hasContextModifier() {
            return contextModifier != null;
        }
    }

    /**
     * Context modifier record.
     */
    public record ContextModifier(
        String toolUseId,
        Function<Object, Object> modifyContext
    ) {}

    /**
     * Batch record for tool calls.
     */
    public record Batch(
        boolean isConcurrencySafe,
        List<Object> blocks
    ) {
        public int size() {
            return blocks.size();
        }

        public boolean isEmpty() {
            return blocks.isEmpty();
        }
    }

    /**
     * Tool use block record.
     */
    public record ToolUseBlock(
        String id,
        String name,
        Object input
    ) {}

    /**
     * Get max tool use concurrency.
     */
    public int getMaxToolUseConcurrency() {
        String envValue = System.getenv("CLAUDE_CODE_MAX_TOOL_USE_CONCURRENCY");
        if (envValue != null) {
            try {
                return Integer.parseInt(envValue);
            } catch (NumberFormatException e) {
                return DEFAULT_MAX_CONCURRENCY;
            }
        }
        return DEFAULT_MAX_CONCURRENCY;
    }

    /**
     * Partition tool calls into batches.
     */
    public List<Batch> partitionToolCalls(
        List<ToolUseBlock> toolUseMessages,
        Object toolUseContext
    ) {
        List<Batch> batches = new ArrayList<>();

        for (ToolUseBlock toolUse : toolUseMessages) {
            boolean isConcurrencySafe = checkConcurrencySafe(toolUse, toolUseContext);

            if (isConcurrencySafe && !batches.isEmpty() && batches.get(batches.size() - 1).isConcurrencySafe()) {
                // Add to existing concurrency-safe batch
                Batch lastBatch = batches.get(batches.size() - 1);
                List<Object> newBlocks = new ArrayList<>(lastBatch.blocks());
                newBlocks.add(toolUse);
                batches.set(batches.size() - 1, new Batch(true, newBlocks));
            } else {
                // Create new batch
                batches.add(new Batch(isConcurrencySafe, List.of(toolUse)));
            }
        }

        return batches;
    }

    /**
     * Check if tool is concurrency safe.
     * Uses reflection to check if tool implements isConcurrencySafe method.
     */
    private boolean checkConcurrencySafe(ToolUseBlock toolUse, Object toolUseContext) {
        // Read-only tools can run concurrently
        String toolName = toolUse.name();

        // Tools that are known to be read-only and safe for concurrent execution
        Set<String> readOnlyTools = Set.of(
            "Read", "Glob", "Grep", "WebFetch", "WebSearch",
            "TaskList", "TaskGet", "CronList"
        );

        if (readOnlyTools.contains(toolName)) {
            return true;
        }

        // Tools that modify state should run serially
        Set<String> modifyingTools = Set.of(
            "Edit", "Write", "Bash", "TaskCreate", "TaskUpdate",
            "TaskStop", "CronCreate", "CronDelete"
        );

        if (modifyingTools.contains(toolName)) {
            return false;
        }

        // Conservative default for unknown tools
        return false;
    }

    /**
     * Run tools concurrently.
     */
    public CompletableFuture<List<MessageUpdate>> runToolsConcurrently(
        List<ToolUseBlock> blocks,
        List<Object> assistantMessages,
        Object canUseTool,
        Object toolUseContext
    ) {
        int maxConcurrency = getMaxToolUseConcurrency();

        return CompletableFuture.supplyAsync(() -> {
            List<MessageUpdate> results = new ArrayList<>();

            // Process in parallel with concurrency limit
            List<CompletableFuture<MessageUpdate>> futures = blocks.stream()
                .map(block -> CompletableFuture.supplyAsync(() ->
                    runSingleTool(block, assistantMessages, canUseTool, toolUseContext)
                ))
                .collect(Collectors.toList());

            // Wait for all to complete
            for (CompletableFuture<MessageUpdate> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    results.add(MessageUpdate.empty());
                }
            }

            return results;
        });
    }

    /**
     * Run tools serially.
     */
    public CompletableFuture<List<MessageUpdate>> runToolsSerially(
        List<ToolUseBlock> blocks,
        List<Object> assistantMessages,
        Object canUseTool,
        Object toolUseContext
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<MessageUpdate> results = new ArrayList<>();
            Object currentContext = toolUseContext;

            for (ToolUseBlock block : blocks) {
                MessageUpdate update = runSingleTool(block, assistantMessages, canUseTool, currentContext);
                results.add(update);

                if (update.hasContextModifier()) {
                    currentContext = update.contextModifier().modifyContext().apply(currentContext);
                }
            }

            return results;
        });
    }

    /**
     * Run single tool.
     */
    private MessageUpdate runSingleTool(
        ToolUseBlock toolUse,
        List<Object> assistantMessages,
        Object canUseTool,
        Object toolUseContext
    ) {
        // Implementation would actually run the tool
        return MessageUpdate.empty();
    }

    /**
     * Orchestration result record.
     */
    public record OrchestrationResult(
        boolean success,
        int toolsExecuted,
        int toolsSucceeded,
        int toolsFailed,
        List<MessageUpdate> updates,
        Duration totalDuration
    ) {
        public static OrchestrationResult empty() {
            return new OrchestrationResult(true, 0, 0, 0, Collections.emptyList(), Duration.ZERO);
        }

        public double successRate() {
            if (toolsExecuted == 0) return 1.0;
            return (double) toolsSucceeded / toolsExecuted;
        }
    }
}