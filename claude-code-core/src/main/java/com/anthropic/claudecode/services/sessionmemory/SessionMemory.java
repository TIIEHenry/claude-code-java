/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/SessionMemory/sessionMemory
 */
package com.anthropic.claudecode.services.sessionmemory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.time.*;

/**
 * Session memory - Automatically maintains a markdown file with notes.
 *
 * Runs periodically in the background to extract key information
 * without interrupting the main conversation flow.
 */
public final class SessionMemory {
    private volatile boolean initialized = false;
    private volatile boolean autoCompactEnabled = true;
    private volatile boolean gateEnabled = false;
    private volatile boolean hasLoggedGateFailure = false;

    private final SessionMemoryUtils utils;
    private final List<ExtractionListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String lastMemoryMessageUuid;

    /**
     * Create session memory.
     */
    public SessionMemory() {
        this.utils = new SessionMemoryUtils();
    }

    /**
     * Extraction listener interface.
     */
    public interface ExtractionListener {
        void onExtractionStarted();
        void onExtractionCompleted(String memoryPath);
        void onExtractionFailed(String error);
    }

    /**
     * Manual extraction result record.
     */
    public record ManualExtractionResult(
        boolean success,
        String memoryPath,
        String error
    ) {
        public static ManualExtractionResult success(String path) {
            return new ManualExtractionResult(true, path, null);
        }

        public static ManualExtractionResult failure(String error) {
            return new ManualExtractionResult(false, null, error);
        }
    }

    /**
     * Extraction context record.
     */
    public record ExtractionContext(
        List<Object> messages,
        Object toolUseContext,
        String querySource
    ) {}

    /**
     * Initialize session memory.
     */
    public void init() {
        if (initialized) return;

        if (!autoCompactEnabled) {
            return;
        }

        initialized = true;
    }

    /**
     * Should extract memory.
     */
    public boolean shouldExtractMemory(List<Object> messages) {
        int currentTokenCount = estimateTokenCount(messages);

        if (!SessionMemoryUtils.isSessionMemoryInitialized()) {
            if (!SessionMemoryUtils.hasMetInitializationThreshold(currentTokenCount)) {
                return false;
            }
            SessionMemoryUtils.markSessionMemoryInitialized();
        }

        boolean hasMetTokenThreshold = SessionMemoryUtils.hasMetUpdateThreshold(currentTokenCount);
        int toolCallsSinceLastUpdate = countToolCallsSince(messages, lastMemoryMessageUuid);
        boolean hasMetToolCallThreshold = toolCallsSinceLastUpdate >= SessionMemoryUtils.getToolCallsBetweenUpdates();
        boolean hasToolCallsInLastTurn = hasToolCallsInLastAssistantTurn(messages);

        boolean shouldExtract =
            (hasMetTokenThreshold && hasMetToolCallThreshold) ||
            (hasMetTokenThreshold && !hasToolCallsInLastTurn);

        if (shouldExtract) {
            if (!messages.isEmpty()) {
                Object lastMessage = messages.get(messages.size() - 1);
                lastMemoryMessageUuid = getMessageUuid(lastMessage);
            }
            return true;
        }

        return false;
    }

    /**
     * Extract session memory.
     */
    public CompletableFuture<Void> extractSessionMemory(ExtractionContext context) {
        return CompletableFuture.runAsync(() -> {
            // Check if main REPL thread
            if (!"repl_main_thread".equals(context.querySource())) {
                return;
            }

            // Check gate
            if (!gateEnabled) {
                if ("ant".equals(System.getenv("USER_TYPE")) && !hasLoggedGateFailure) {
                    hasLoggedGateFailure = true;
                }
                return;
            }

            if (!shouldExtractMemory(context.messages())) {
                return;
            }

            SessionMemoryUtils.markExtractionStarted();
            notifyExtractionStarted();

            try {
                // Perform extraction
                String memoryPath = performExtraction(context);

                SessionMemoryUtils.markExtractionCompleted();
                notifyExtractionCompleted(memoryPath);

                // Update last summarized message ID
                updateLastSummarizedMessageIdIfSafe(context.messages());
            } catch (Exception e) {
                SessionMemoryUtils.markExtractionCompleted();
                notifyExtractionFailed(e.getMessage());
            }
        }, executor);
    }

    /**
     * Manually extract session memory.
     */
    public CompletableFuture<ManualExtractionResult> manuallyExtractSessionMemory(
        List<Object> messages,
        Object toolUseContext
    ) {
        return CompletableFuture.supplyAsync(() -> {
            if (messages.isEmpty()) {
                return ManualExtractionResult.failure("No messages to summarize");
            }

            SessionMemoryUtils.markExtractionStarted();

            try {
                String memoryPath = performExtraction(new ExtractionContext(
                    messages, toolUseContext, "session_memory_manual"
                ));

                SessionMemoryUtils.markExtractionCompleted();
                SessionMemoryUtils.recordExtractionTokenCount(estimateTokenCount(messages));
                updateLastSummarizedMessageIdIfSafe(messages);

                return ManualExtractionResult.success(memoryPath);
            } catch (Exception e) {
                SessionMemoryUtils.markExtractionCompleted();
                return ManualExtractionResult.failure(e.getMessage());
            }
        }, executor);
    }

    /**
     * Perform extraction.
     */
    private String performExtraction(ExtractionContext context) {
        // Implementation would run forked agent for extraction
        return getSessionMemoryPath();
    }

    /**
     * Get session memory path.
     */
    private String getSessionMemoryPath() {
        String home = System.getProperty("user.home");
        return home + "/.claude/session_memory.md";
    }

    /**
     * Count tool calls since.
     */
    private int countToolCallsSince(List<Object> messages, String sinceUuid) {
        int count = 0;
        boolean foundStart = sinceUuid == null;

        for (Object message : messages) {
            if (!foundStart) {
                if (getMessageUuid(message).equals(sinceUuid)) {
                    foundStart = true;
                }
                continue;
            }

            // Count tool calls in assistant messages
            if (isAssistantMessage(message)) {
                count += countToolCalls(message);
            }
        }

        return count;
    }

    /**
     * Check if has tool calls in last assistant turn.
     */
    private boolean hasToolCallsInLastAssistantTurn(List<Object> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Object msg = messages.get(i);
            if (isAssistantMessage(msg)) {
                return countToolCalls(msg) > 0;
            }
        }
        return false;
    }

    /**
     * Estimate token count.
     */
    private int estimateTokenCount(List<Object> messages) {
        // Simple estimation
        return messages.size() * 100;
    }

    /**
     * Get message UUID.
     */
    private String getMessageUuid(Object message) {
        // Implementation would extract UUID from message
        return UUID.randomUUID().toString();
    }

    /**
     * Check if assistant message.
     */
    private boolean isAssistantMessage(Object message) {
        // Implementation would check message type
        return true;
    }

    /**
     * Count tool calls in message.
     */
    private int countToolCalls(Object message) {
        // Implementation would count tool_use blocks
        return 0;
    }

    /**
     * Update last summarized message ID if safe.
     */
    private void updateLastSummarizedMessageIdIfSafe(List<Object> messages) {
        if (!hasToolCallsInLastAssistantTurn(messages)) {
            if (!messages.isEmpty()) {
                String uuid = getMessageUuid(messages.get(messages.size() - 1));
                SessionMemoryUtils.setLastSummarizedMessageId(uuid);
            }
        }
    }

    /**
     * Add listener.
     */
    public void addListener(ExtractionListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener.
     */
    public void removeListener(ExtractionListener listener) {
        listeners.remove(listener);
    }

    private void notifyExtractionStarted() {
        for (ExtractionListener listener : listeners) {
            listener.onExtractionStarted();
        }
    }

    private void notifyExtractionCompleted(String memoryPath) {
        for (ExtractionListener listener : listeners) {
            listener.onExtractionCompleted(memoryPath);
        }
    }

    private void notifyExtractionFailed(String error) {
        for (ExtractionListener listener : listeners) {
            listener.onExtractionFailed(error);
        }
    }

    /**
     * Set auto compact enabled.
     */
    public void setAutoCompactEnabled(boolean enabled) {
        this.autoCompactEnabled = enabled;
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