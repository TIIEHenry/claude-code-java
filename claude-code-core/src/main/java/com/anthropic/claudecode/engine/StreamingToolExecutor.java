/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/tools/StreamingToolExecutor.ts
 */
package com.anthropic.claudecode.engine;

import com.anthropic.claudecode.message.Message;
import com.anthropic.claudecode.message.ContentBlock;

import com.anthropic.claudecode.CanUseToolFn;
import com.anthropic.claudecode.Tool;
import com.anthropic.claudecode.ToolResult;
import com.anthropic.claudecode.ToolUseContext;
import com.anthropic.claudecode.AssistantMessage;
import com.anthropic.claudecode.permission.PermissionResult;
import com.anthropic.claudecode.utils.AbortController;
import com.anthropic.claudecode.types.MessageTypes;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * StreamingToolExecutor - Executes tools as they stream in with concurrency control.
 *
 * <p>Concurrent-safe tools can execute in parallel with other concurrent-safe tools.
 * Non-concurrent tools must execute alone (exclusive access).
 * Results are buffered and emitted in the order tools were received.
 */
public class StreamingToolExecutor {

    private final List<TrackedTool> tools = new CopyOnWriteArrayList<>();
    private final ToolUseContext toolUseContext;
    private final List<Tool> toolDefinitions;
    private final CanUseToolFn canUseTool;
    private final AbortController siblingAbortController;

    private volatile boolean hasErrored = false;
    private volatile String erroredToolDescription = "";
    private volatile boolean discarded = false;

    private final CompletableFuture<Void> progressAvailable = new CompletableFuture<>();
    private volatile Runnable progressAvailableResolve = null;

    /**
     * Create a new StreamingToolExecutor.
     */
    public StreamingToolExecutor(
            List<Tool> toolDefinitions,
            CanUseToolFn canUseTool,
            ToolUseContext toolUseContext
    ) {
        this.toolDefinitions = toolDefinitions;
        this.canUseTool = canUseTool;
        this.toolUseContext = toolUseContext != null ? toolUseContext : ToolUseContext.empty();
        this.siblingAbortController = new AbortController();
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
            // Tool not found - create error result
            TrackedTool tool = new TrackedTool(
                    block.id(),
                    block,
                    assistantMessage,
                    ToolStatus.COMPLETED,
                    true,
                    null,
                    List.of(createErrorMessage(block.id(), "Error: No such tool available: " + block.name(), assistantMessage)),
                    new CopyOnWriteArrayList<>()
            );
            tools.add(tool);
            return;
        }

        // Parse input and check concurrency safety
        boolean isConcurrencySafe = false;
        try {
            Object parsedInput = toolDefinition.getInputSchema().parse(block.input());
            isConcurrencySafe = toolDefinition.isConcurrencySafeUntyped(parsedInput);
        } catch (Exception e) {
            // Parse failed, default to non-concurrent
        }

        TrackedTool tool = new TrackedTool(
                block.id(),
                block,
                assistantMessage,
                ToolStatus.QUEUED,
                isConcurrencySafe,
                null,
                null,
                new CopyOnWriteArrayList<>()
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
                (isConcurrencySafe && executingTools.stream().allMatch(TrackedTool::isConcurrencySafe));
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
     * Execute a single tool.
     */
    @SuppressWarnings("unchecked")
    private void executeTool(TrackedTool tool) {
        if (discarded) {
            tool.setStatus(ToolStatus.COMPLETED);
            tool.setResults(List.of(createErrorMessage(
                    tool.id(),
                    "Tool execution discarded",
                    tool.assistantMessage()
            )));
            return;
        }

        tool.setStatus(ToolStatus.EXECUTING);

        CompletableFuture.supplyAsync(() -> {
            try {
                Tool toolDef = findToolByName(tool.block().name());
                if (toolDef == null) {
                    return createErrorMessage(tool.id(), "Tool not found: " + tool.block().name(), tool.assistantMessage());
                }

                // Parse the input using the tool's schema
                Map<String, Object> inputMap = tool.block().input();
                Object parsedInput = toolDef.getInputSchema().parse(inputMap);

                // Create context if not provided
                ToolUseContext ctx = toolUseContext != null ? toolUseContext : ToolUseContext.empty();

                // Create a simple canUseTool function that always allows
                CanUseToolFn canUse = canUseTool != null ? canUseTool : (t, i, c, a, id) ->
                    CompletableFuture.completedFuture(PermissionResult.allow(i));

                // Get tool use ID
                String toolUseId = tool.id();

                // Invoke the tool using reflection to handle generic types
                java.lang.reflect.Method callMethod = toolDef.getClass().getMethod(
                    "call",
                    Object.class,
                    ToolUseContext.class,
                    CanUseToolFn.class,
                    AssistantMessage.class,
                    java.util.function.Consumer.class
                );

                CompletableFuture<ToolResult<?>> resultFuture;
                try {
                    resultFuture = (CompletableFuture<ToolResult<?>>) callMethod.invoke(
                        toolDef,
                        parsedInput,
                        ctx,
                        canUse,
                        tool.assistantMessage(),
                        null
                    );
                } catch (Exception e) {
                    return createErrorMessage(tool.id(), "Tool invocation error: " + e.getMessage(), tool.assistantMessage());
                }

                // Wait for result with timeout
                ToolResult<?> result = resultFuture.get(120, TimeUnit.SECONDS);

                // Format result
                String resultContent;
                if (result != null && result.data() != null) {
                    resultContent = formatToolResult(result.data());
                } else {
                    resultContent = "Tool completed with no output";
                }

                return createToolResult(tool.id(), resultContent, false, tool.assistantMessage());

            } catch (TimeoutException e) {
                hasErrored = true;
                erroredToolDescription = tool.block().name();
                siblingAbortController.abort();
                return createErrorMessage(tool.id(), "Tool execution timed out (120s)", tool.assistantMessage());
            } catch (Exception e) {
                hasErrored = true;
                erroredToolDescription = tool.block().name();
                siblingAbortController.abort();
                return createErrorMessage(tool.id(), "Error: " + e.getMessage(), tool.assistantMessage());
            }
        }).thenAccept(result -> {
            tool.setStatus(ToolStatus.COMPLETED);
            tool.setResults(List.of(result));
        });
    }

    /**
     * Format tool result for display.
     */
    private String formatToolResult(Object data) {
        if (data == null) return "";

        // Check if it has a toResultString method
        try {
            java.lang.reflect.Method method = data.getClass().getMethod("toResultString");
            Object result = method.invoke(data);
            if (result instanceof String s) return s;
        } catch (Exception ignored) {}

        // Check if it's a record with output
        if (data instanceof Record) {
            try {
                java.lang.reflect.Method outputMethod = data.getClass().getMethod("output");
                Object output = outputMethod.invoke(data);
                if (output != null) return output.toString();
            } catch (Exception ignored) {}
        }

        // Default: use toString
        return data.toString();
    }

    /**
     * Get remaining results in order.
     */
    public List<Message> getRemainingResults() {
        List<Message> results = new ArrayList<>();

        for (TrackedTool tool : tools) {
            if (tool.status() == ToolStatus.COMPLETED && tool.results() != null) {
                results.addAll(tool.results());
            }
        }

        return results;
    }

    /**
     * Check if all tools are complete.
     */
    public boolean isComplete() {
        return tools.stream().allMatch(t ->
                t.status() == ToolStatus.COMPLETED ||
                t.status() == ToolStatus.YIELDED
        );
    }

    /**
     * Check if any tool has errored.
     */
    public boolean hasErrored() {
        return hasErrored;
    }

    /**
     * Get the number of tools.
     */
    public int getToolCount() {
        return tools.size();
    }

    /**
     * Find a tool by name.
     */
    private Tool findToolByName(String name) {
        for (Tool tool : toolDefinitions) {
            if (tool.name().equals(name)) {
                return tool;
            }
        }
        return null;
    }

    /**
     * Create an error message.
     */
    private Message createErrorMessage(String toolUseId, String content, MessageTypes.AssistantMessage assistantMessage) {
        return new Message.User(List.of(new ContentBlock.ToolResult(toolUseId, "<tool_use_error>" + content + "</tool_use_error>", true)));
    }

    /**
     * Create a tool result message.
     */
    private Message createToolResult(String toolUseId, Object content, boolean isError, MessageTypes.AssistantMessage assistantMessage) {
        return new Message.User(List.of(new ContentBlock.ToolResult(toolUseId, content != null ? content.toString() : "", isError)));
    }

    // ==================== Inner Classes ====================

    /**
     * Tool status enum.
     */
    public enum ToolStatus {
        QUEUED,
        EXECUTING,
        COMPLETED,
        YIELDED
    }

    /**
     * Tracked tool record.
     */
    public static class TrackedTool {
        private final String id;
        private final ToolUseBlock block;
        private final MessageTypes.AssistantMessage assistantMessage;
        private final AtomicReference<ToolStatus> status;
        private final boolean isConcurrencySafe;
        private final CompletableFuture<Void> promise;
        private volatile List<Message> results;
        private final List<Message> pendingProgress;

        public TrackedTool(
                String id,
                ToolUseBlock block,
                MessageTypes.AssistantMessage assistantMessage,
                ToolStatus status,
                boolean isConcurrencySafe,
                CompletableFuture<Void> promise,
                List<Message> results,
                List<Message> pendingProgress
        ) {
            this.id = id;
            this.block = block;
            this.assistantMessage = assistantMessage;
            this.status = new AtomicReference<>(status);
            this.isConcurrencySafe = isConcurrencySafe;
            this.promise = promise;
            this.results = results;
            this.pendingProgress = pendingProgress;
        }

        public String id() { return id; }
        public ToolUseBlock block() { return block; }
        public MessageTypes.AssistantMessage assistantMessage() { return assistantMessage; }
        public ToolStatus status() { return status.get(); }
        public void setStatus(ToolStatus s) { status.set(s); }
        public boolean isConcurrencySafe() { return isConcurrencySafe; }
        public List<Message> results() { return results; }
        public void setResults(List<Message> r) { results = r; }
        public List<Message> pendingProgress() { return pendingProgress; }
    }

    /**
     * Tool use block record.
     */
    public record ToolUseBlock(
            String id,
            String name,
            Map<String, Object> input
    ) {}

    /**
     * Permission decision.
     */
    public record PermissionDecision(
            Behavior behavior,
            String message
    ) {
        public enum Behavior {
            ALLOW,
            DENY,
            ASK
        }
    }
}