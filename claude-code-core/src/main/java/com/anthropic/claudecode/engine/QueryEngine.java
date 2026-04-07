/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code QueryEngine.ts
 */
package com.anthropic.claudecode.engine;

import com.anthropic.claudecode.*;
import com.anthropic.claudecode.permission.PermissionResult;
import com.anthropic.claudecode.services.tools.StreamingToolExecutor;
import com.anthropic.claudecode.types.MessageTypes;
import com.anthropic.claudecode.session.ClaudeSession;
import com.anthropic.claudecode.message.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * QueryEngine - Core agentic loop processing engine.
 *
 * <p>Corresponds to query.ts in the original TypeScript codebase.
 * Implements the full agentic loop with TRUE streaming:
 * - Messages are processed as they arrive (not after collectList)
 * - Tools are added to executor DURING API streaming
 * - Completed results are yielded while streaming continues
 *
 * <p>This matches TypeScript's "for await (const message of apiStream)" pattern.
 */
public class QueryEngine {

    private final QueryEngineConfig config;
    private final List<Object> mutableMessages = new CopyOnWriteArrayList<>();
    private volatile boolean interrupted = false;
    private final StreamingToolExecutor streamingToolExecutor;
    private final ClaudeSession session;
    private volatile QueryState currentState;

    // Track tool use blocks during streaming
    private final List<StreamingToolExecutor.ToolUseBlock> streamingToolBlocks = new CopyOnWriteArrayList<>();
    private final AtomicInteger toolCount = new AtomicInteger(0);
    private final AtomicBoolean hasToolCalls = new AtomicBoolean(false);

    @SuppressWarnings("unchecked")
    public QueryEngine(QueryEngineConfig config) {
        this.config = config;
        if (config.initialMessages() != null) {
            this.mutableMessages.addAll(config.initialMessages());
        }

        ToolUseContext emptyContext = ToolUseContext.empty();
        List<Tool> tools = config.tools() != null ? config.tools() : Collections.emptyList();
        CanUseToolFn canUseTool = config.canUseTool() != null ? config.canUseTool() :
            (tool, input, context, assistantMessage, toolUseId) ->
                CompletableFuture.completedFuture(PermissionResult.allow(input));

        this.streamingToolExecutor = new StreamingToolExecutor(tools, canUseTool, emptyContext);

        // Create API session
        String apiKey = config.apiKey() != null ? config.apiKey() : System.getenv("ANTHROPIC_API_KEY");
        @SuppressWarnings("unchecked")
        List<Tool<?, ?, ?>> typedTools = (List<Tool<?, ?, ?>>) (List<?>) tools;
        this.session = new ClaudeSession(
            UUID.randomUUID().toString(),
            apiKey,
            config.model() != null ? config.model() : "glm-5",
            typedTools,
            config.systemPrompt()
        );

        // Initialize state
        this.currentState = QueryState.initial(new ArrayList<>(mutableMessages), emptyContext);
    }

    /**
     * Execute the agentic loop with TRUE streaming.
     *
     * <p>Key difference from previous implementation:
     * - Uses handle() to process each message as it arrives
     * - Adds tools to executor DURING streaming (not after collectList)
     * - Yields completed results while streaming continues
     */
    public Flux<QueryEvent> executeAgenticLoop(String prompt) {
        return Flux.defer(() -> {
            interrupted = false;
            streamingToolBlocks.clear();
            toolCount.set(0);
            hasToolCalls.set(false);
            return doExecuteLoop(prompt, 0);
        });
    }

    /**
     * Internal loop execution with iteration tracking.
     */
    private Flux<QueryEvent> doExecuteLoop(String prompt, int iteration) {
        int maxIterations = config.maxTurns() != null ? config.maxTurns() : 20;

        if (interrupted || iteration >= maxIterations) {
            LoopTransition.Terminal terminal = iteration >= maxIterations
                ? LoopTransition.Terminal.maxTurnsReached(iteration)
                : LoopTransition.Terminal.userExit();
            return Flux.just(new QueryEvent.Terminal(terminal));
        }

        // Execute one turn with TRUE streaming
        return executeOneTurnStreaming(prompt)
            .expand(event -> {
                // Continue loop after tools complete
                if (event instanceof QueryEvent.ToolsComplete complete) {
                    // If there were tool calls, continue the loop
                    if (complete.resultCount() > 0 || hasToolCalls.get()) {
                        return doExecuteLoop(null, iteration + 1);
                    }
                }
                return Flux.empty();
            })
            .takeUntil(event -> event instanceof QueryEvent.Terminal);
    }

    /**
     * Execute one turn with TRUE streaming processing.
     *
     * <p>This is the key method that differs from the old implementation.
     * Instead of collectList(), we process each message as it arrives.
     */
    private Flux<QueryEvent> executeOneTurnStreaming(String prompt) {
        Flux<Message> responseFlux;
        if (prompt != null && !prompt.isEmpty()) {
            responseFlux = session.sendMessageStreaming(prompt);
        } else {
            responseFlux = session.sendMessageStreaming("");
        }

        // Yield RequestStart event
        Flux<QueryEvent> startEvent = Flux.just(new QueryEvent.RequestStart());

        // Process each message AS IT ARRIVES (not after collectList)
        Flux<QueryEvent> streamingEvents = responseFlux
            .handle((message, sink) -> {
                // 1. Immediately yield the message
                sink.next(new QueryEvent.Message(message));

                // 2. If it's an assistant message with tool_use, add to executor NOW
                if (message instanceof Message.Assistant assistant) {
                    List<ContentBlock> content = assistant.content();
                    if (content != null) {
                        for (ContentBlock block : content) {
                            if (block instanceof ContentBlock.ToolUse toolUse) {
                                // ★ Key: Add tool DURING streaming, not after
                                StreamingToolExecutor.ToolUseBlock tub = new StreamingToolExecutor.ToolUseBlock(
                                    toolUse.id(), toolUse.name(), toolUse.input()
                                );
                                streamingToolBlocks.add(tub);
                                toolCount.incrementAndGet();
                                hasToolCalls.set(true);

                                // Convert to MessageTypes.AssistantMessage for executor
                                MessageTypes.AssistantMessage mtAssistant = convertToMtAssistant(assistant);
                                streamingToolExecutor.addTool(tub, mtAssistant);

                                // Emit ToolsExecuting event if this is the first tool
                                if (toolCount.get() == 1) {
                                    sink.next(new QueryEvent.ToolsExecuting(streamingToolBlocks.size()));
                                }
                            }
                        }
                    }
                }

                // 3. Get completed results that may have finished while streaming
                Iterator<StreamingToolExecutor.MessageUpdate> completedResults =
                    streamingToolExecutor.getCompletedResults();
                while (completedResults.hasNext()) {
                    StreamingToolExecutor.MessageUpdate update = completedResults.next();
                    if (update.message() != null) {
                        sink.next(new QueryEvent.Message(update.message()));
                    }
                }
            });

        // After streaming completes, collect remaining tool results
        Flux<QueryEvent> remainingResults = Mono.fromFuture(streamingToolExecutor.getRemainingResults())
            .flatMapMany(resultsIterator -> {
                List<QueryEvent> events = new ArrayList<>();
                int resultCount = 0;

                while (resultsIterator.hasNext()) {
                    StreamingToolExecutor.MessageUpdate update = resultsIterator.next();
                    if (update.message() != null) {
                        events.add(new QueryEvent.Message(update.message()));
                        resultCount++;
                    }
                }

                // Determine if we need another loop iteration
                if (hasToolCalls.get()) {
                    events.add(new QueryEvent.ToolsComplete(resultCount));
                } else {
                    // No tool calls - terminal
                    events.add(new QueryEvent.Terminal(
                        LoopTransition.Terminal.complete(Collections.emptyList())
                    ));
                }

                return Flux.fromIterable(events);
            });

        // Concatenate: start → streaming → remaining results
        return Flux.concat(startEvent, streamingEvents, remainingResults);
    }

    /**
     * Convert Message.Assistant to MessageTypes.AssistantMessage.
     */
    private MessageTypes.AssistantMessage convertToMtAssistant(Message.Assistant assistant) {
        List<Map<String, Object>> content = new ArrayList<>();
        for (ContentBlock block : assistant.content()) {
            Map<String, Object> blockMap = new LinkedHashMap<>();
            if (block instanceof ContentBlock.Text textBlock) {
                blockMap.put("type", "text");
                blockMap.put("text", textBlock.text());
            } else if (block instanceof ContentBlock.ToolUse toolUse) {
                blockMap.put("type", "tool_use");
                blockMap.put("id", toolUse.id());
                blockMap.put("name", toolUse.name());
                blockMap.put("input", toolUse.input());
            }
            content.add(blockMap);
        }

        return new MessageTypes.AssistantMessage(
            assistant.id() != null ? assistant.id() : UUID.randomUUID().toString(),
            content,
            assistant.usage() != null ? new LinkedHashMap<>(assistant.usage()) : null,
            assistant.model(),
            assistant.stopReason(),
            null
        );
    }

    /**
     * Interrupt the current query.
     */
    public void interrupt() {
        interrupted = true;
        streamingToolExecutor.discard();
    }

    /**
     * Get all messages.
     */
    public List<Object> getMessages() {
        return new ArrayList<>(mutableMessages);
    }

    /**
     * Add a message.
     */
    public void addMessage(Object message) {
        mutableMessages.add(message);
    }

    /**
     * Clear all messages.
     */
    public void clearMessages() {
        mutableMessages.clear();
    }

    /**
     * Get the current working directory.
     */
    public String getCwd() {
        return config.cwd();
    }

    /**
     * Check if the engine is interrupted.
     */
    public boolean isInterrupted() {
        return interrupted;
    }

    /**
     * Get the configuration.
     */
    public QueryEngineConfig getConfig() {
        return config;
    }

    /**
     * Get the current state.
     */
    public QueryState getCurrentState() {
        return currentState;
    }

    /**
     * Create a default QueryEngine.
     */
    public static QueryEngine create(String cwd) {
        return new QueryEngine(QueryEngineConfig.builder()
            .cwd(cwd)
            .build());
    }
}