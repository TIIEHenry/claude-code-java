/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/tools/StreamingToolExecutor.ts
 */
package com.anthropic.claudecode.services.tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import com.anthropic.claudecode.*;
import com.anthropic.claudecode.permission.PermissionResult;
import com.anthropic.claudecode.types.MessageTypes;
import com.anthropic.claudecode.utils.MessageUtils;

/**
 * Executes tools as they stream in with concurrency control.
 * - Concurrent-safe tools can execute in parallel with other concurrent-safe tools
 * - Non-concurrent tools must execute alone (exclusive access)
 * - Results are buffered and emitted in the order tools were received
 */
public final class StreamingToolExecutor {
    private final List<TrackedTool> tools = new CopyOnWriteArrayList<>();
    private final List<Tool> toolDefinitions;
    private final CanUseToolFn canUseTool;
    private volatile ToolUseContext toolUseContext;
    private volatile boolean hasErrored = false;
    private volatile String erroredToolDescription = "";
    private volatile boolean discarded = false;
    private CompletableFuture<Void> progressAvailable = null;

    /**
     * Tool status enum.
     */
    public enum ToolStatus {
        QUEUED, EXECUTING, COMPLETED, YIELDED
    }

    /**
     * Tracked tool record.
     */
    public record TrackedTool(
        String id,
        ToolUseBlock block,
        MessageTypes.AssistantMessage assistantMessage,
        ToolStatus status,
        boolean isConcurrencySafe,
        CompletableFuture<Void> promise,
        List<MessageTypes.Message> results,
        List<MessageTypes.Message> pendingProgress,
        List<Function<ToolUseContext, ToolUseContext>> contextModifiers
    ) {
        public TrackedTool withStatus(ToolStatus newStatus) {
            return new TrackedTool(id, block, assistantMessage, newStatus,
                isConcurrencySafe, promise, results, pendingProgress, contextModifiers);
        }

        public TrackedTool withResults(List<MessageTypes.Message> newResults) {
            return new TrackedTool(id, block, assistantMessage, status,
                isConcurrencySafe, promise, newResults, pendingProgress, contextModifiers);
        }

        public TrackedTool withPromise(CompletableFuture<Void> newPromise) {
            return new TrackedTool(id, block, assistantMessage, status,
                isConcurrencySafe, newPromise, results, pendingProgress, contextModifiers);
        }
    }

    /**
     * Message update record.
     */
    public record MessageUpdate(
        MessageTypes.Message message,
        ToolUseContext newContext
    ) {}

    /**
     * Tool use block (simplified from SDK).
     */
    public record ToolUseBlock(
        String id,
        String name,
        Map<String, Object> input
    ) {}

    /**
     * Create streaming tool executor.
     */
    public StreamingToolExecutor(
        List<Tool> toolDefinitions,
        CanUseToolFn canUseTool,
        ToolUseContext toolUseContext
    ) {
        this.toolDefinitions = toolDefinitions;
        this.canUseTool = canUseTool;
        this.toolUseContext = toolUseContext;
    }

    /**
     * Discard all pending and in-progress tools.
     */
    public void discard() {
        this.discarded = true;
    }

    /**
     * Add a tool to the execution queue.
     */
    public void addTool(ToolUseBlock block, MessageTypes.AssistantMessage assistantMessage) {
        Tool toolDefinition = findToolByName(block.name());
        if (toolDefinition == null) {
            // Unknown tool - create error result
            TrackedTool errorTool = new TrackedTool(
                block.id(),
                block,
                assistantMessage,
                ToolStatus.COMPLETED,
                true,
                null,
                List.of(createNoSuchToolError(block, assistantMessage)),
                new ArrayList<>(),
                new ArrayList<>()
            );
            tools.add(errorTool);
            return;
        }

        boolean isConcurrencySafe = false;
        try {
            // Use input directly since we don't have schema parsing
            isConcurrencySafe = toolDefinition.isConcurrencySafeUntyped(block.input());
        } catch (Exception e) {
            // Parse failed - treat as not concurrency safe
        }

        TrackedTool tool = new TrackedTool(
            block.id(),
            block,
            assistantMessage,
            ToolStatus.QUEUED,
            isConcurrencySafe,
            null,
            new ArrayList<>(),
            new CopyOnWriteArrayList<>(),
            new ArrayList<>()
        );
        tools.add(tool);

        processQueue();
    }

    /**
     * Check if a tool can execute based on current concurrency state.
     */
    private boolean canExecuteTool(boolean isConcurrencySafe) {
        List<TrackedTool> executingTools = tools.stream()
            .filter(t -> t.status() == ToolStatus.EXECUTING)
            .toList();
        return executingTools.isEmpty() ||
            (isConcurrencySafe && executingTools.stream().allMatch(t -> t.isConcurrencySafe()));
    }

    /**
     * Process the queue, starting tools when concurrency conditions allow.
     */
    private void processQueue() {
        for (TrackedTool tool : tools) {
            if (tool.status() != ToolStatus.QUEUED) continue;

            if (canExecuteTool(tool.isConcurrencySafe())) {
                executeTool(tool);
            } else {
                // Can't execute this tool yet, stop for non-concurrent tools
                if (!tool.isConcurrencySafe()) break;
            }
        }
    }

    /**
     * Execute a tool and collect its results.
     */
    private void executeTool(TrackedTool tool) {
        // Update status to executing
        updateTool(tool.withStatus(ToolStatus.EXECUTING));

        List<MessageTypes.Message> messages = new ArrayList<>();
        List<Function<ToolUseContext, ToolUseContext>> contextModifiers = new ArrayList<>();

        CompletableFuture<Void> promise = CompletableFuture.runAsync(() -> {
            // Check for abort reasons
            AbortReason initialAbortReason = getAbortReason(tool);
            if (initialAbortReason != null) {
                messages.add(createSyntheticErrorMessage(tool.id(), initialAbortReason, tool.assistantMessage()));
                updateTool(tool.withResults(messages).withStatus(ToolStatus.COMPLETED));
                return;
            }

            // Run the tool
            try {
                Iterator<MessageUpdate> generator = runToolUse(tool.block(), tool.assistantMessage());

                boolean thisToolErrored = false;
                while (generator.hasNext()) {
                    MessageUpdate update = generator.next();

                    AbortReason abortReason = getAbortReason(tool);
                    if (abortReason != null && !thisToolErrored) {
                        messages.add(createSyntheticErrorMessage(tool.id(), abortReason, tool.assistantMessage()));
                        break;
                    }

                    boolean isErrorResult = isErrorResult(update.message());
                    if (isErrorResult) {
                        thisToolErrored = true;
                        // Only Bash errors cancel siblings
                        if ("Bash".equals(tool.block().name())) {
                            hasErrored = true;
                            erroredToolDescription = getToolDescription(tool);
                        }
                    }

                    // Progress messages go to pendingProgress
                    if (update.message() instanceof MessageTypes.ProgressMessage) {
                        tool.pendingProgress().add(update.message());
                        signalProgressAvailable();
                    } else {
                        messages.add(update.message());
                    }
                }

                updateTool(tool.withResults(messages).withStatus(ToolStatus.COMPLETED));
            } catch (Exception e) {
                messages.add(createErrorResult(tool.id(), e.getMessage(), tool.assistantMessage()));
                updateTool(tool.withResults(messages).withStatus(ToolStatus.COMPLETED));
            }
        });

        updateTool(tool.withPromise(promise));
        promise.whenComplete((v, ex) -> processQueue());
    }

    /**
     * Get completed results (non-blocking).
     */
    public Iterator<MessageUpdate> getCompletedResults() {
        if (discarded) {
            return Collections.emptyIterator();
        }

        List<MessageUpdate> results = new ArrayList<>();
        for (TrackedTool tool : tools) {
            // Yield pending progress messages first
            for (MessageTypes.Message progress : tool.pendingProgress()) {
                results.add(new MessageUpdate(progress, toolUseContext));
            }
            tool.pendingProgress().clear();

            if (tool.status() == ToolStatus.YIELDED) continue;

            if (tool.status() == ToolStatus.COMPLETED && tool.results() != null) {
                updateTool(tool.withStatus(ToolStatus.YIELDED));

                for (MessageTypes.Message message : tool.results()) {
                    results.add(new MessageUpdate(message, toolUseContext));
                }

                markToolUseAsComplete(tool.id());
            } else if (tool.status() == ToolStatus.EXECUTING && !tool.isConcurrencySafe()) {
                break;
            }
        }

        return results.iterator();
    }

    /**
     * Wait for remaining tools and yield their results.
     */
    public CompletableFuture<Iterator<MessageUpdate>> getRemainingResults() {
        return CompletableFuture.supplyAsync(() -> {
            List<MessageUpdate> allResults = new ArrayList<>();

            while (hasUnfinishedTools()) {
                processQueue();

                Iterator<MessageUpdate> completed = getCompletedResults();
                while (completed.hasNext()) {
                    allResults.add(completed.next());
                }

                if (hasExecutingTools() && !hasCompletedResults() && !hasPendingProgress()) {
                    // Wait for any tool to complete or progress
                    List<CompletableFuture<Void>> executingPromises = tools.stream()
                        .filter(t -> t.status() == ToolStatus.EXECUTING && t.promise() != null)
                        .map(TrackedTool::promise)
                        .toList();

                    if (!executingPromises.isEmpty()) {
                        CompletableFuture.anyOf(executingPromises.toArray(new CompletableFuture[0])).join();
                    }
                }
            }

            // Get remaining results
            Iterator<MessageUpdate> finalResults = getCompletedResults();
            while (finalResults.hasNext()) {
                allResults.add(finalResults.next());
            }

            return allResults.iterator();
        });
    }

    /**
     * Get the current tool use context.
     */
    public ToolUseContext getUpdatedContext() {
        return toolUseContext;
    }

    // Helper methods

    private Tool findToolByName(String name) {
        return toolDefinitions.stream()
            .filter(t -> t.name().equals(name) || t.aliases().contains(name))
            .findFirst()
            .orElse(null);
    }

    private void updateTool(TrackedTool updatedTool) {
        for (int i = 0; i < tools.size(); i++) {
            if (tools.get(i).id().equals(updatedTool.id())) {
                tools.set(i, updatedTool);
                break;
            }
        }
    }

    private AbortReason getAbortReason(TrackedTool tool) {
        if (discarded) return AbortReason.STREAMING_FALLBACK;
        if (hasErrored) return AbortReason.SIBLING_ERROR;
        // Check abort signal
        return null;
    }

    private MessageTypes.Message createNoSuchToolError(ToolUseBlock block, MessageTypes.AssistantMessage assistantMessage) {
        return MessageUtils.createUserMessage(
            "<tool_use_error>Error: No such tool available: " + block.name() + "</tool_use_error>",
            block.id(),
            assistantMessage.uuid()
        );
    }

    private MessageTypes.Message createSyntheticErrorMessage(String toolUseId, AbortReason reason, MessageTypes.AssistantMessage assistantMessage) {
        String content = switch (reason) {
            case USER_INTERRUPTED -> "User rejected tool use";
            case STREAMING_FALLBACK -> "<tool_use_error>Error: Streaming fallback - tool execution discarded</tool_use_error>";
            case SIBLING_ERROR -> {
                String desc = erroredToolDescription;
                yield desc != null && !desc.isEmpty()
                    ? "<tool_use_error>Cancelled: parallel tool call " + desc + " errored</tool_use_error>"
                    : "<tool_use_error>Cancelled: parallel tool call errored</tool_use_error>";
            }
        };
        return MessageUtils.createUserMessage(content, toolUseId, assistantMessage.uuid());
    }

    private MessageTypes.Message createErrorResult(String toolUseId, String errorMessage, MessageTypes.AssistantMessage assistantMessage) {
        return MessageUtils.createUserMessage(
            "<tool_use_error>" + errorMessage + "</tool_use_error>",
            toolUseId,
            assistantMessage.uuid()
        );
    }

    private String getToolDescription(TrackedTool tool) {
        Object summary = tool.block().input().get("command");
        if (summary == null) summary = tool.block().input().get("file_path");
        if (summary == null) summary = tool.block().input().get("pattern");
        if (summary instanceof String s && !s.isEmpty()) {
            String truncated = s.length() > 40 ? s.substring(0, 40) + "…" : s;
            return tool.block().name() + "(" + truncated + ")";
        }
        return tool.block().name();
    }

    private boolean isErrorResult(MessageTypes.Message message) {
        if (message instanceof MessageTypes.UserMessage user) {
            return user.hasErrorContent();
        }
        return false;
    }

    private Iterator<MessageUpdate> runToolUse(ToolUseBlock block, MessageTypes.AssistantMessage assistantMessage) {
        List<MessageUpdate> updates = new ArrayList<>();

        try {
            // Find the tool definition
            Tool toolDef = findToolByName(block.name());
            if (toolDef == null) {
                updates.add(new MessageUpdate(
                    createNoSuchToolError(block, assistantMessage),
                    toolUseContext
                ));
                return updates.iterator();
            }

            // Check permissions via canUseTool
            if (canUseTool != null) {
                // Convert MessageTypes.AssistantMessage to AssistantMessage
                com.anthropic.claudecode.AssistantMessage convertedAssistant =
                    new com.anthropic.claudecode.AssistantMessage(
                        assistantMessage.uuid(),
                        java.time.Instant.now(),
                        "assistant",
                        java.util.List.of(),
                        null,
                        com.anthropic.claudecode.AssistantMessage.Usage.empty()
                    );

                CompletableFuture<PermissionResult> permissionFuture = canUseTool.apply(
                    toolDef, block.input(), toolUseContext, convertedAssistant, block.id()
                );

                PermissionResult permissionResult = permissionFuture.get(30, TimeUnit.SECONDS);

                if (permissionResult instanceof PermissionResult.Deny denyResult) {
                    String denyMessage = denyResult.message();
                    updates.add(new MessageUpdate(
                        MessageUtils.createUserMessage(
                            "<tool_use_error>Permission denied: " + denyMessage + "</tool_use_error>",
                            block.id(),
                            assistantMessage.uuid()
                        ),
                        toolUseContext
                    ));
                    return updates.iterator();
                }
            }

            // Execute the tool - use simplified execution
            try {
                // Call the tool's call method with minimal setup
                @SuppressWarnings("unchecked")
                Tool<Object, Object, ToolProgressData> typedTool = (Tool<Object, Object, ToolProgressData>) toolDef;
                Object input = block.input();

                com.anthropic.claudecode.AssistantMessage convertedAssistant =
                    new com.anthropic.claudecode.AssistantMessage(
                        assistantMessage.uuid(),
                        java.time.Instant.now(),
                        "assistant",
                        java.util.List.of(),
                        null,
                        com.anthropic.claudecode.AssistantMessage.Usage.empty()
                    );

                CompletableFuture<ToolResult<Object>> resultFuture = typedTool.call(
                    input,
                    toolUseContext,
                    canUseTool,
                    convertedAssistant,
                    null
                );

                if (resultFuture != null) {
                    ToolResult<Object> toolResult = resultFuture.get(300, TimeUnit.SECONDS);

                    // Convert result to message
                    String content = toolResult.data() != null ? toolResult.data().toString() : "Tool completed";
                    updates.add(new MessageUpdate(
                        MessageUtils.createUserMessage(content, block.id(), assistantMessage.uuid()),
                        toolUseContext
                    ));
                } else {
                    updates.add(new MessageUpdate(
                        MessageUtils.createUserMessage("Tool completed with no output", block.id(), assistantMessage.uuid()),
                        toolUseContext
                    ));
                }
            } catch (TimeoutException e) {
                updates.add(new MessageUpdate(
                    MessageUtils.createUserMessage(
                        "<tool_use_error>Tool execution timed out after 5 minutes</tool_use_error>",
                        block.id(),
                        assistantMessage.uuid()
                    ),
                    toolUseContext
                ));
            } catch (Exception e) {
                updates.add(new MessageUpdate(
                    MessageUtils.createUserMessage(
                        "<tool_use_error>" + e.getMessage() + "</tool_use_error>",
                        block.id(),
                        assistantMessage.uuid()
                    ),
                    toolUseContext
                ));
            }
        } catch (Exception e) {
            updates.add(new MessageUpdate(
                MessageUtils.createUserMessage(
                    "<tool_use_error>Failed to execute tool: " + e.getMessage() + "</tool_use_error>",
                    block.id(),
                    assistantMessage.uuid()
                ),
                toolUseContext
            ));
        }

        return updates.iterator();
    }

    private void markToolUseAsComplete(String toolUseId) {
        // Remove tool from in-progress set via toolUseContext
        // The context has a Consumer that accepts the updated set
        if (toolUseContext != null && toolUseContext.setInProgressToolUseIDs() != null) {
            // Create a new set without this tool use ID
            // Since we don't have access to the current set, we use the helper methods
            toolUseContext.removeInProgressToolUseId(toolUseId);
        }
    }

    private void signalProgressAvailable() {
        if (progressAvailable != null) {
            progressAvailable.complete(null);
            progressAvailable = null;
        }
    }

    private boolean hasUnfinishedTools() {
        return tools.stream().anyMatch(t -> t.status() != ToolStatus.YIELDED);
    }

    private boolean hasExecutingTools() {
        return tools.stream().anyMatch(t -> t.status() == ToolStatus.EXECUTING);
    }

    private boolean hasCompletedResults() {
        return tools.stream().anyMatch(t -> t.status() == ToolStatus.COMPLETED);
    }

    private boolean hasPendingProgress() {
        return tools.stream().anyMatch(t -> !t.pendingProgress().isEmpty());
    }

    /**
     * Abort reason enum.
     */
    public enum AbortReason {
        SIBLING_ERROR, USER_INTERRUPTED, STREAMING_FALLBACK
    }
}