/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/compact/postCompactCleanup
 */
package com.anthropic.claudecode.services.compact;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Post compact cleanup - Cleanup after compaction.
 *
 * Run cleanup of caches and tracking state after compaction.
 * Call this after both auto-compact and manual /compact to free memory
 * held by tracking structures that are invalidated by compaction.
 */
public final class PostCompactCleanup {
    private final List<CleanupAction> cleanupActions = new CopyOnWriteArrayList<>();

    /**
     * Cleanup action interface.
     */
    public interface CleanupAction {
        void run(boolean isMainThread);
        String name();
    }

    /**
     * Query source enum.
     */
    public enum QuerySource {
        REPL_MAIN_THREAD,
        SDK,
        AGENT,
        SUBAGENT,
        MANUAL_COMPACT,
        CLEAR
    }

    /**
     * Cleanup options record.
     */
    public record CleanupOptions(
        boolean clearSystemPromptSections,
        boolean clearClassifierApprovals,
        boolean clearSpeculativeChecks,
        boolean clearSessionMessagesCache,
        boolean resetMicrocompactState,
        boolean clearBetaTracingState
    ) {
        public static CleanupOptions defaults() {
            return new CleanupOptions(true, true, true, true, true, true);
        }

        public static CleanupOptions minimal() {
            return new CleanupOptions(false, false, false, false, true, false);
        }
    }

    /**
     * Add cleanup action.
     */
    public void addCleanupAction(CleanupAction action) {
        cleanupActions.add(action);
    }

    /**
     * Remove cleanup action.
     */
    public void removeCleanupAction(CleanupAction action) {
        cleanupActions.remove(action);
    }

    /**
     * Run post compact cleanup.
     */
    public void runCleanup(QuerySource querySource, CleanupOptions options) {
        // Determine if this is main thread compact
        boolean isMainThread = isMainThreadCompact(querySource);

        // Reset microcompact state (always runs)
        if (options.resetMicrocompactState()) {
            resetMicrocompactState();
        }

        // Main thread only cleanups
        if (isMainThread) {
            if (options.clearSystemPromptSections()) {
                clearSystemPromptSections();
            }
            if (options.clearClassifierApprovals()) {
                clearClassifierApprovals();
            }
            if (options.clearSpeculativeChecks()) {
                clearSpeculativeChecks();
            }
            if (options.clearSessionMessagesCache()) {
                clearSessionMessagesCache();
            }
        }

        // Always clear beta tracing state
        if (options.clearBetaTracingState()) {
            clearBetaTracingState();
        }

        // Run custom cleanup actions
        for (CleanupAction action : cleanupActions) {
            action.run(isMainThread);
        }
    }

    /**
     * Run cleanup with defaults.
     */
    public void runCleanup(QuerySource querySource) {
        runCleanup(querySource, CleanupOptions.defaults());
    }

    /**
     * Check if main thread compact.
     */
    private boolean isMainThreadCompact(QuerySource querySource) {
        return querySource == null ||
               querySource == QuerySource.REPL_MAIN_THREAD ||
               querySource == QuerySource.SDK ||
               querySource == QuerySource.MANUAL_COMPACT ||
               querySource == QuerySource.CLEAR;
    }

    /**
     * Reset microcompact state.
     */
    private void resetMicrocompactState() {
        // Implementation would reset microcompact service state
    }

    /**
     * Clear system prompt sections.
     */
    private void clearSystemPromptSections() {
        // Implementation would clear cached system prompt sections
    }

    /**
     * Clear classifier approvals.
     */
    private void clearClassifierApprovals() {
        // Implementation would clear classifier approval cache
    }

    /**
     * Clear speculative checks.
     */
    private void clearSpeculativeChecks() {
        // Implementation would clear speculative check cache
    }

    /**
     * Clear session messages cache.
     */
    private void clearSessionMessagesCache() {
        // Implementation would clear session messages cache
    }

    /**
     * Clear beta tracing state.
     */
    private void clearBetaTracingState() {
        // Implementation would clear beta tracing state
    }

    /**
     * Clear all caches.
     */
    public void clearAll() {
        runCleanup(QuerySource.MANUAL_COMPACT, CleanupOptions.defaults());
    }

    /**
     * Cleanup result record.
     */
    public record CleanupResult(
        boolean success,
        int actionsRun,
        List<String> actionNames,
        Duration duration
    ) {
        public static CleanupResult empty() {
            return new CleanupResult(true, 0, Collections.emptyList(), Duration.ZERO);
        }
    }
}