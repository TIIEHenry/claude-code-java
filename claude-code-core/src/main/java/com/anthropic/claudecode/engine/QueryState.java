/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code query.ts State type
 */
package com.anthropic.claudecode.engine;

import com.anthropic.claudecode.ToolUseContext;
import com.anthropic.claudecode.services.compact.AutoCompactTrackingState;

import java.util.List;

/**
 * Mutable state carried between agentic loop iterations.
 *
 * <p>Corresponds to State type in query.ts:204-217
 */
public record QueryState(
    List<Object> messages,
    ToolUseContext toolUseContext,
    AutoCompactTrackingState autoCompactTracking,
    int maxOutputTokensRecoveryCount,
    boolean hasAttemptedReactiveCompact,
    Integer maxOutputTokensOverride,
    Object pendingToolUseSummary,
    boolean stopHookActive,
    int turnCount,
    LoopTransition.Continue transition
) {
    /**
     * Create initial state.
     */
    public static QueryState initial(List<Object> messages, ToolUseContext toolUseContext) {
        return new QueryState(
            messages,
            toolUseContext,
            null,
            0,
            false,
            null,
            null,
            false,
            1,
            null
        );
    }

    /**
     * Builder for creating modified state (for continue sites).
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder(null);
    }

    public static class Builder {
        private List<Object> messages;
        private ToolUseContext toolUseContext;
        private AutoCompactTrackingState autoCompactTracking;
        private int maxOutputTokensRecoveryCount;
        private boolean hasAttemptedReactiveCompact;
        private Integer maxOutputTokensOverride;
        private Object pendingToolUseSummary;
        private boolean stopHookActive;
        private int turnCount;
        private LoopTransition.Continue transition;

        public Builder(QueryState state) {
            if (state != null) {
                this.messages = state.messages();
                this.toolUseContext = state.toolUseContext();
                this.autoCompactTracking = state.autoCompactTracking();
                this.maxOutputTokensRecoveryCount = state.maxOutputTokensRecoveryCount();
                this.hasAttemptedReactiveCompact = state.hasAttemptedReactiveCompact();
                this.maxOutputTokensOverride = state.maxOutputTokensOverride();
                this.pendingToolUseSummary = state.pendingToolUseSummary();
                this.stopHookActive = state.stopHookActive();
                this.turnCount = state.turnCount();
                this.transition = state.transition();
            }
        }

        public Builder messages(List<Object> messages) { this.messages = messages; return this; }
        public Builder toolUseContext(ToolUseContext ctx) { this.toolUseContext = ctx; return this; }
        public Builder autoCompactTracking(AutoCompactTrackingState tracking) { this.autoCompactTracking = tracking; return this; }
        public Builder maxOutputTokensRecoveryCount(int count) { this.maxOutputTokensRecoveryCount = count; return this; }
        public Builder hasAttemptedReactiveCompact(boolean attempted) { this.hasAttemptedReactiveCompact = attempted; return this; }
        public Builder maxOutputTokensOverride(Integer override) { this.maxOutputTokensOverride = override; return this; }
        public Builder pendingToolUseSummary(Object summary) { this.pendingToolUseSummary = summary; return this; }
        public Builder stopHookActive(boolean active) { this.stopHookActive = active; return this; }
        public Builder turnCount(int count) { this.turnCount = count; return this; }
        public Builder transition(LoopTransition.Continue transition) { this.transition = transition; return this; }

        public QueryState build() {
            return new QueryState(
                messages,
                toolUseContext,
                autoCompactTracking,
                maxOutputTokensRecoveryCount,
                hasAttemptedReactiveCompact,
                maxOutputTokensOverride,
                pendingToolUseSummary,
                stopHookActive,
                turnCount,
                transition
            );
        }
    }
}