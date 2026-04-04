/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code QueryEngine.ts
 */
package com.anthropic.claudecode.engine;

import com.anthropic.claudecode.*;
import com.anthropic.claudecode.permission.PermissionMode;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * QueryEngine - Core query processing engine.
 *
 * <p>Corresponds to QueryEngine.ts in the original TypeScript codebase.
 * Manages message submission, interruption, and state.
 */
public class QueryEngine {

    private final QueryEngineConfig config;
    private final List<Object> mutableMessages = new CopyOnWriteArrayList<>();
    private volatile AbortController abortController;
    private volatile boolean interrupted = false;

    public QueryEngine(QueryEngineConfig config) {
        this.config = config;
        this.abortController = new AbortController();
        if (config.initialMessages() != null) {
            this.mutableMessages.addAll(config.initialMessages());
        }
    }

    /**
     * Submit a message for processing.
     */
    public CompletableFuture<QueryResult> submitMessage(String prompt, SubmitOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            // Reset abort controller for new query
            abortController = new AbortController();
            interrupted = false;

            // Process the prompt
            QueryResult result = new QueryResult(
                UUID.randomUUID().toString(),
                prompt,
                QueryStatus.COMPLETED,
                null
            );

            return result;
        });
    }

    /**
     * Interrupt the current query.
     */
    public void interrupt() {
        interrupted = true;
        if (abortController != null) {
            abortController.abort();
        }
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
     * Create a default QueryEngine.
     */
    public static QueryEngine create(String cwd) {
        return new QueryEngine(QueryEngineConfig.builder()
            .cwd(cwd)
            .build());
    }

    // ==================== Inner Classes ====================

    /**
     * AbortController for managing cancellation.
     */
    public static class AbortController {
        private volatile boolean aborted = false;
        private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

        public void abort() {
            aborted = true;
            listeners.forEach(Runnable::run);
        }

        public boolean isAborted() {
            return aborted;
        }

        public void addListener(Runnable listener) {
            listeners.add(listener);
        }
    }

    /**
     * Query result.
     */
    public record QueryResult(
        String queryId,
        String prompt,
        QueryStatus status,
        Object result
    ) {}

    /**
     * Query status.
     */
    public enum QueryStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        INTERRUPTED
    }

    /**
     * Submit options.
     */
    public record SubmitOptions(
        PermissionMode permissionMode,
        boolean verbose,
        Integer maxTurns
    ) {
        public SubmitOptions() {
            this(PermissionMode.DEFAULT, false, null);
        }

        public static SubmitOptions empty() {
            return new SubmitOptions();
        }
    }
}