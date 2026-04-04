/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/tools/toolOrchestration.ts
 */
package com.anthropic.claudecode.services.tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import com.anthropic.claudecode.*;
import com.anthropic.claudecode.types.MessageTypes;
import com.anthropic.claudecode.services.tools.ToolExecutionService.ToolUseBlock;

/**
 * Tool orchestration - run tools with concurrency control.
 */
public final class ToolOrchestrationService {
    private ToolOrchestrationService() {}

    private static final int DEFAULT_MAX_TOOL_USE_CONCURRENCY = 10;

    /**
     * Message update record.
     */
    public record MessageUpdate(
        MessageTypes.Message message,
        ToolUseContext newContext
    ) {}

    /**
     * Get max tool use concurrency.
     */
    public static int getMaxToolUseConcurrency() {
        String envValue = System.getenv("CLAUDE_CODE_MAX_TOOL_USE_CONCURRENCY");
        if (envValue != null) {
            try {
                return Integer.parseInt(envValue);
            } catch (NumberFormatException e) {
                // Use default
            }
        }
        return DEFAULT_MAX_TOOL_USE_CONCURRENCY;
    }

    /**
     * Run tools with concurrency control.
     */
    public static Iterator<MessageUpdate> runTools(
        List<ToolUseBlock> toolUseMessages,
        List<MessageTypes.AssistantMessage> assistantMessages,
        CanUseToolFn canUseTool,
        ToolUseContext toolUseContext
    ) {
        List<MessageUpdate> results = new ArrayList<>();
        ToolUseContext currentContext = toolUseContext;

        for (Batch batch : partitionToolCalls(toolUseMessages, currentContext)) {
            if (batch.isConcurrencySafe()) {
                // Run read-only batch concurrently
                Map<String, List<Function<ToolUseContext, ToolUseContext>>> queuedContextModifiers = new HashMap<>();

                for (ToolExecutionService.MessageUpdate update : runToolsConcurrently(
                    batch.blocks(),
                    assistantMessages,
                    canUseTool,
                    currentContext
                )) {
                    if (update.contextModifier() != null) {
                        String toolUseID = update.contextModifier().toolUseID();
                        queuedContextModifiers.computeIfAbsent(toolUseID, k -> new ArrayList<>())
                            .add(update.contextModifier().modifyContext());
                    }
                    results.add(new MessageUpdate(
                        update.message(),
                        currentContext
                    ));
                }

                // Apply context modifiers
                for (ToolUseBlock block : batch.blocks()) {
                    List<Function<ToolUseContext, ToolUseContext>> modifiers = queuedContextModifiers.get(block.id());
                    if (modifiers != null) {
                        for (Function<ToolUseContext, ToolUseContext> modifier : modifiers) {
                            currentContext = modifier.apply(currentContext);
                        }
                    }
                }
                results.add(new MessageUpdate(null, currentContext));
            } else {
                // Run non-read-only batch serially
                for (ToolExecutionService.MessageUpdate update : runToolsSerially(
                    batch.blocks(),
                    assistantMessages,
                    canUseTool,
                    currentContext
                )) {
                    if (update.contextModifier() != null) {
                        currentContext = update.contextModifier().modifyContext().apply(currentContext);
                    }
                    results.add(new MessageUpdate(
                        update.message(),
                        currentContext
                    ));
                }
            }
        }

        return results.iterator();
    }

    /**
     * Partition tool calls into batches.
     */
    private static List<Batch> partitionToolCalls(
        List<ToolUseBlock> toolUseMessages,
        ToolUseContext toolUseContext
    ) {
        List<Batch> batches = new ArrayList<>();

        for (ToolUseBlock toolUse : toolUseMessages) {
            Tool<?, ?, ?> tool = findToolByName(toolUseContext.options().tools(), toolUse.name());
            boolean isConcurrencySafe = false;

            if (tool != null) {
                try {
                    Object parsedInput = tool.getInputSchema().parse(toolUse.input());
                    isConcurrencySafe = tool.isConcurrencySafeUntyped(parsedInput);
                } catch (Exception e) {
                    // Treat as not concurrency safe
                }
            }

            if (isConcurrencySafe && !batches.isEmpty() && batches.get(batches.size() - 1).isConcurrencySafe()) {
                // Add to existing concurrent batch
                List<ToolUseBlock> blocks = new ArrayList<>(batches.get(batches.size() - 1).blocks());
                blocks.add(toolUse);
                batches.set(batches.size() - 1, new Batch(true, blocks));
            } else {
                // Create new batch
                batches.add(new Batch(isConcurrencySafe, List.of(toolUse)));
            }
        }

        return batches;
    }

    /**
     * Run tools serially.
     */
    private static Iterable<ToolExecutionService.MessageUpdate> runToolsSerially(
        List<ToolUseBlock> toolUseMessages,
        List<MessageTypes.AssistantMessage> assistantMessages,
        CanUseToolFn canUseTool,
        ToolUseContext toolUseContext
    ) {
        List<ToolExecutionService.MessageUpdate> results = new ArrayList<>();
        ToolUseContext currentContext = toolUseContext;

        for (ToolUseBlock toolUse : toolUseMessages) {
            toolUseContext.addInProgressToolUseId(toolUse.id());

            MessageTypes.AssistantMessage assistantMessage = findAssistantMessage(assistantMessages, toolUse.id());
            Iterator<ToolExecutionService.MessageUpdate> updates = ToolExecutionService.runToolUse(
                toolUse,
                assistantMessage,
                canUseTool,
                currentContext
            );

            while (updates.hasNext()) {
                ToolExecutionService.MessageUpdate update = updates.next();
                if (update.contextModifier() != null) {
                    currentContext = update.contextModifier().modifyContext().apply(currentContext);
                }
                results.add(update);
            }

            markToolUseAsComplete(toolUseContext, toolUse.id());
        }

        return results;
    }

    /**
     * Run tools concurrently.
     */
    private static Iterable<ToolExecutionService.MessageUpdate> runToolsConcurrently(
        List<ToolUseBlock> toolUseMessages,
        List<MessageTypes.AssistantMessage> assistantMessages,
        CanUseToolFn canUseTool,
        ToolUseContext toolUseContext
    ) {
        List<ToolExecutionService.MessageUpdate> results = new ArrayList<>();

        // Run with concurrency limit
        ExecutorService executor = Executors.newFixedThreadPool(getMaxToolUseConcurrency());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (ToolUseBlock toolUse : toolUseMessages) {
            futures.add(CompletableFuture.runAsync(() -> {
                toolUseContext.addInProgressToolUseId(toolUse.id());

                MessageTypes.AssistantMessage assistantMessage = findAssistantMessage(assistantMessages, toolUse.id());
                Iterator<ToolExecutionService.MessageUpdate> updates = ToolExecutionService.runToolUse(
                    toolUse,
                    assistantMessage,
                    canUseTool,
                    toolUseContext
                );

                while (updates.hasNext()) {
                    synchronized (results) {
                        results.add(updates.next());
                    }
                }

                markToolUseAsComplete(toolUseContext, toolUse.id());
            }, executor));
        }

        // Wait for all to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } catch (Exception e) {
            // Handle error
        } finally {
            executor.shutdown();
        }

        return results;
    }

    private static MessageTypes.AssistantMessage findAssistantMessage(
        List<MessageTypes.AssistantMessage> messages,
        String toolUseId
    ) {
        return messages.stream()
            .filter(m -> m.hasToolUse(toolUseId))
            .findFirst()
            .orElse(null);
    }

    private static void markToolUseAsComplete(ToolUseContext toolUseContext, String toolUseID) {
        toolUseContext.removeInProgressToolUseId(toolUseID);
    }

    private static Tool<?, ?, ?> findToolByName(List<Tool<?, ?, ?>> tools, String name) {
        return tools.stream()
            .filter(t -> t.name().equals(name) || t.aliases().contains(name))
            .findFirst()
            .orElse(null);
    }

    /**
     * Batch record.
     */
    public record Batch(
        boolean isConcurrencySafe,
        List<ToolUseBlock> blocks
    ) {}
}