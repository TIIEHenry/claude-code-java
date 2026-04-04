/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/extractMemories/extractMemories
 */
package com.anthropic.claudecode.services.extractmemories;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.time.*;

/**
 * Extract memories - Extracts durable memories from session transcript.
 *
 * Writes them to the auto-memory directory (~/.claude/projects/<path>/memory/).
 * Runs once at the end of each complete query loop.
 */
public final class ExtractMemories {
    private volatile boolean initialized = false;
    private volatile boolean gateEnabled = false;

    private String lastMemoryMessageUuid;
    private boolean hasLoggedGateFailure = false;
    private boolean inProgress = false;
    private int turnsSinceLastExtraction = 0;
    private PendingContext pendingContext;

    private final Set<CompletableFuture<Void>> inFlightExtractions = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Pending context record.
     */
    private record PendingContext(
        ExtractionContext context,
        Consumer<Object> appendSystemMessage
    ) {}

    /**
     * Extraction context record.
     */
    public record ExtractionContext(
        List<Object> messages,
        Object toolUseContext,
        String agentId
    ) {}

    /**
     * Extraction result record.
     */
    public record ExtractionResult(
        boolean success,
        List<String> writtenPaths,
        int turnCount,
        long durationMs
    ) {
        public static ExtractionResult empty() {
            return new ExtractionResult(true, Collections.emptyList(), 0, 0);
        }
    }

    /**
     * Initialize extract memories.
     */
    public void init() {
        if (initialized) return;
        initialized = true;
    }

    /**
     * Execute extract memories.
     */
    public CompletableFuture<Void> executeExtractMemories(
        ExtractionContext context,
        Consumer<Object> appendSystemMessage
    ) {
        if (!initialized) {
            return CompletableFuture.completedFuture(null);
        }

        // Only run for main agent, not subagents
        if (context.agentId() != null) {
            return CompletableFuture.completedFuture(null);
        }

        // Check gate
        if (!gateEnabled) {
            if ("ant".equals(System.getenv("USER_TYPE")) && !hasLoggedGateFailure) {
                hasLoggedGateFailure = true;
            }
            return CompletableFuture.completedFuture(null);
        }

        // Check if auto memory enabled
        if (!isAutoMemoryEnabled()) {
            return CompletableFuture.completedFuture(null);
        }

        // If extraction in progress, stash for trailing run
        if (inProgress) {
            pendingContext = new PendingContext(context, appendSystemMessage);
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            runExtraction(context, appendSystemMessage, false);
        }, executor);

        inFlightExtractions.add(future);
        future.whenComplete((v, e) -> inFlightExtractions.remove(future));

        return future;
    }

    /**
     * Drain pending extraction.
     */
    public CompletableFuture<Void> drainPendingExtraction(long timeoutMs) {
        if (inFlightExtractions.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.anyOf(
            CompletableFuture.allOf(inFlightExtractions.toArray(new CompletableFuture[0])),
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(timeoutMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            })
        ).thenApply(v -> null);
    }

    /**
     * Run extraction.
     */
    private void runExtraction(
        ExtractionContext context,
        Consumer<Object> appendSystemMessage,
        boolean isTrailingRun
    ) {
        String memoryDir = getAutoMemPath();
        int newMessageCount = countModelVisibleMessagesSince(
            context.messages(),
            lastMemoryMessageUuid
        );

        // Check for memory writes since last extraction
        if (hasMemoryWritesSince(context.messages(), lastMemoryMessageUuid)) {
            // Advance cursor
            if (!context.messages().isEmpty()) {
                lastMemoryMessageUuid = getMessageUuid(context.messages().get(context.messages().size() - 1));
            }
            return;
        }

        // Throttle: only run every N turns
        if (!isTrailingRun) {
            turnsSinceLastExtraction++;
            if (turnsSinceLastExtraction < getTurnsThreshold()) {
                return;
            }
        }
        turnsSinceLastExtraction = 0;

        inProgress = true;
        long startTime = System.currentTimeMillis();

        try {
            // Run forked agent for extraction
            ExtractionResult result = runForkedAgent(context, memoryDir);

            // Advance cursor after success
            if (!context.messages().isEmpty()) {
                lastMemoryMessageUuid = getMessageUuid(context.messages().get(context.messages().size() - 1));
            }

            // Notify of saved memories
            if (!result.writtenPaths().isEmpty() && appendSystemMessage != null) {
                appendSystemMessage.accept(createMemorySavedMessage(result.writtenPaths()));
            }
        } catch (Exception e) {
            // Best-effort - log but don't notify
        } finally {
            inProgress = false;

            // Run trailing extraction if pending
            if (pendingContext != null) {
                PendingContext trailing = pendingContext;
                pendingContext = null;
                runExtraction(trailing.context(), trailing.appendSystemMessage(), true);
            }
        }
    }

    /**
     * Run forked agent.
     */
    private ExtractionResult runForkedAgent(ExtractionContext context, String memoryDir) {
        // Implementation would run forked agent
        return ExtractionResult.empty();
    }

    /**
     * Count model visible messages since.
     */
    private int countModelVisibleMessagesSince(List<Object> messages, String sinceUuid) {
        if (sinceUuid == null) {
            return (int) messages.stream().filter(this::isModelVisibleMessage).count();
        }

        boolean foundStart = false;
        int count = 0;

        for (Object message : messages) {
            if (!foundStart) {
                if (getMessageUuid(message).equals(sinceUuid)) {
                    foundStart = true;
                }
                continue;
            }
            if (isModelVisibleMessage(message)) {
                count++;
            }
        }

        // If sinceUuid not found, count all
        if (!foundStart) {
            return (int) messages.stream().filter(this::isModelVisibleMessage).count();
        }

        return count;
    }

    /**
     * Has memory writes since.
     */
    private boolean hasMemoryWritesSince(List<Object> messages, String sinceUuid) {
        boolean foundStart = sinceUuid == null;

        for (Object message : messages) {
            if (!foundStart) {
                if (getMessageUuid(message).equals(sinceUuid)) {
                    foundStart = true;
                }
                continue;
            }

            // Check for Write/Edit tool calls to auto-memory paths
            if (isAssistantMessage(message)) {
                List<String> writtenPaths = extractWrittenPaths(message);
                for (String path : writtenPaths) {
                    if (isAutoMemPath(path)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Extract written paths from message.
     */
    private List<String> extractWrittenPaths(Object message) {
        // Implementation would extract file paths from tool_use blocks
        return Collections.emptyList();
    }

    /**
     * Check if auto memory enabled.
     */
    private boolean isAutoMemoryEnabled() {
        return true; // Default
    }

    /**
     * Get auto mem path.
     */
    private String getAutoMemPath() {
        return System.getProperty("user.home") + "/.claude/projects/memory/";
    }

    /**
     * Check if auto mem path.
     */
    private boolean isAutoMemPath(String path) {
        return path != null && path.contains("/memory/");
    }

    /**
     * Get message UUID.
     */
    private String getMessageUuid(Object message) {
        return UUID.randomUUID().toString();
    }

    /**
     * Check if model visible message.
     */
    private boolean isModelVisibleMessage(Object message) {
        return true;
    }

    /**
     * Check if assistant message.
     */
    private boolean isAssistantMessage(Object message) {
        return true;
    }

    /**
     * Get turns threshold.
     */
    private int getTurnsThreshold() {
        return 1;
    }

    /**
     * Create memory saved message.
     */
    private Object createMemorySavedMessage(List<String> paths) {
        return "Memories saved: " + String.join(", ", paths);
    }

    /**
     * Set gate enabled.
     */
    public void setGateEnabled(boolean enabled) {
        this.gateEnabled = enabled;
    }

    /**
     * Shutdown.
     */
    public void shutdown() {
        executor.shutdown();
    }
}