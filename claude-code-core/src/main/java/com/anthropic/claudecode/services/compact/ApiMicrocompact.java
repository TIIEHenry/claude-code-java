/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/compact/apiMicrocompact
 */
package com.anthropic.claudecode.services.compact;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API microcompact - API-based context management for microcompact.
 *
 * API-based microcompact implementation that uses native context management.
 * Uses context edit strategies to clear tool results and thinking blocks.
 */
public final class ApiMicrocompact {
    private static final int DEFAULT_MAX_INPUT_TOKENS = 180_000;
    private static final int DEFAULT_TARGET_INPUT_TOKENS = 40_000;

    private static final AtomicBoolean useClearToolResults = new AtomicBoolean(false);
    private static final AtomicBoolean useClearToolUses = new AtomicBoolean(false);
    private static final AtomicInteger maxInputTokens = new AtomicInteger(DEFAULT_MAX_INPUT_TOKENS);
    private static final AtomicInteger targetInputTokens = new AtomicInteger(DEFAULT_TARGET_INPUT_TOKENS);

    // Tools clearable results
    private static final Set<String> TOOLS_CLEARABLE_RESULTS = Set.of(
        "Bash", "Glob", "Grep", "Read", "WebFetch", "WebSearch"
    );

    // Tools clearable uses
    private static final Set<String> TOOLS_CLEARABLE_USES = Set.of(
        "Edit", "Write", "NotebookEdit"
    );

    /**
     * Context edit strategy type enum.
     */
    public enum StrategyType {
        CLEAR_TOOL_USES_20250919,
        CLEAR_THINKING_20251015
    }

    /**
     * Trigger type enum.
     */
    public enum TriggerType {
        INPUT_TOKENS
    }

    /**
     * Keep type enum.
     */
    public enum KeepType {
        TOOL_USES,
        THINKING_TURNS
    }

    /**
     * Context edit strategy sealed interface.
     */
    public sealed interface ContextEditStrategy permits ClearToolUses, ClearThinking {}

    /**
     * Clear tool uses strategy.
     */
    public static final class ClearToolUses implements ContextEditStrategy {
        private final StrategyType type = StrategyType.CLEAR_TOOL_USES_20250919;
        private final Trigger trigger;
        private final Keep keep;
        private final ClearAtLeast clearAtLeast;
        private final List<String> clearToolInputs;
        private final List<String> excludeTools;

        public ClearToolUses(Trigger trigger, Keep keep, ClearAtLeast clearAtLeast,
                             List<String> clearToolInputs, List<String> excludeTools) {
            this.trigger = trigger;
            this.keep = keep;
            this.clearAtLeast = clearAtLeast;
            this.clearToolInputs = clearToolInputs;
            this.excludeTools = excludeTools;
        }

        public StrategyType getType() { return type; }
        public Trigger getTrigger() { return trigger; }
        public Keep getKeep() { return keep; }
        public ClearAtLeast getClearAtLeast() { return clearAtLeast; }
        public List<String> getClearToolInputs() { return clearToolInputs; }
        public List<String> getExcludeTools() { return excludeTools; }
    }

    /**
     * Clear thinking strategy.
     */
    public static final class ClearThinking implements ContextEditStrategy {
        private final StrategyType type = StrategyType.CLEAR_THINKING_20251015;
        private final ThinkingKeep keep;

        public ClearThinking(ThinkingKeep keep) {
            this.keep = keep;
        }

        public StrategyType getType() { return type; }
        public ThinkingKeep getKeep() { return keep; }
    }

    /**
     * Trigger record.
     */
    public record Trigger(TriggerType type, int value) {}

    /**
     * Keep record.
     */
    public record Keep(KeepType type, int value) {}

    /**
     * Clear at least record.
     */
    public record ClearAtLeast(TriggerType type, int value) {}

    /**
     * Thinking keep sealed interface.
     */
    public sealed interface ThinkingKeep permits KeepAll, KeepTurns {}

    public static final class KeepAll implements ThinkingKeep {
        public static final KeepAll INSTANCE = new KeepAll();
    }

    public static final class KeepTurns implements ThinkingKeep {
        private final int value;
        public KeepTurns(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    /**
     * Context management config record.
     */
    public record ContextManagementConfig(List<ContextEditStrategy> edits) {
        public static ContextManagementConfig empty() {
            return new ContextManagementConfig(Collections.emptyList());
        }

        public boolean isEmpty() {
            return edits.isEmpty();
        }

        public int getStrategyCount() {
            return edits.size();
        }
    }

    /**
     * Options for context management.
     */
    public record ContextManagementOptions(
        boolean hasThinking,
        boolean isRedactThinkingActive,
        boolean clearAllThinking,
        String userType
    ) {
        public static ContextManagementOptions defaults() {
            return new ContextManagementOptions(false, false, false, "user");
        }
    }

    /**
     * Get API context management.
     */
    public ContextManagementConfig getAPIContextManagement(ContextManagementOptions options) {
        List<ContextEditStrategy> strategies = new ArrayList<>();

        // Preserve thinking blocks
        if (options.hasThinking() && !options.isRedactThinkingActive()) {
            ThinkingKeep keep = options.clearAllThinking()
                ? new KeepTurns(1)
                : KeepAll.INSTANCE;
            strategies.add(new ClearThinking(keep));
        }

        // Tool clearing strategies are ant-only
        if (!"ant".equals(options.userType())) {
            return strategies.isEmpty()
                ? ContextManagementConfig.empty()
                : new ContextManagementConfig(strategies);
        }

        // Add tool clearing strategies if enabled
        if (useClearToolResults.get()) {
            int triggerThreshold = maxInputTokens.get();
            int keepTarget = targetInputTokens.get();

            strategies.add(new ClearToolUses(
                new Trigger(TriggerType.INPUT_TOKENS, triggerThreshold),
                null,
                new ClearAtLeast(TriggerType.INPUT_TOKENS, triggerThreshold - keepTarget),
                new ArrayList<>(TOOLS_CLEARABLE_RESULTS),
                null
            ));
        }

        if (useClearToolUses.get()) {
            int triggerThreshold = maxInputTokens.get();
            int keepTarget = targetInputTokens.get();

            strategies.add(new ClearToolUses(
                new Trigger(TriggerType.INPUT_TOKENS, triggerThreshold),
                null,
                new ClearAtLeast(TriggerType.INPUT_TOKENS, triggerThreshold - keepTarget),
                null,
                new ArrayList<>(TOOLS_CLEARABLE_USES)
            ));
        }

        return strategies.isEmpty()
            ? ContextManagementConfig.empty()
            : new ContextManagementConfig(strategies);
    }

    /**
     * Set use clear tool results.
     */
    public static void setUseClearToolResults(boolean value) {
        useClearToolResults.set(value);
    }

    /**
     * Set use clear tool uses.
     */
    public static void setUseClearToolUses(boolean value) {
        useClearToolUses.set(value);
    }

    /**
     * Set max input tokens.
     */
    public static void setMaxInputTokens(int value) {
        maxInputTokens.set(value);
    }

    /**
     * Set target input tokens.
     */
    public static void setTargetInputTokens(int value) {
        targetInputTokens.set(value);
    }

    /**
     * Get use clear tool results.
     */
    public static boolean getUseClearToolResults() {
        return useClearToolResults.get();
    }

    /**
     * Get use clear tool uses.
     */
    public static boolean getUseClearToolUses() {
        return useClearToolUses.get();
    }

    /**
     * Get max input tokens.
     */
    public static int getMaxInputTokens() {
        return maxInputTokens.get();
    }

    /**
     * Get target input tokens.
     */
    public static int getTargetInputTokens() {
        return targetInputTokens.get();
    }

    /**
     * Reset to defaults.
     */
    public static void reset() {
        useClearToolResults.set(false);
        useClearToolUses.set(false);
        maxInputTokens.set(DEFAULT_MAX_INPUT_TOKENS);
        targetInputTokens.set(DEFAULT_TARGET_INPUT_TOKENS);
    }
}