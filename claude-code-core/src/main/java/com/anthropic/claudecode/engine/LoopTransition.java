/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code query transitions
 */
package com.anthropic.claudecode.engine;

import java.util.List;

/**
 * Transition types for agentic loop state machine.
 *
 * <p>Corresponds to query/transitions.ts in TypeScript.
 */
public sealed interface LoopTransition permits
    LoopTransition.Continue,
    LoopTransition.Terminal {

    /**
     * Check if this is a terminal transition (loop should stop).
     */
    boolean isTerminal();

    /**
     * Continue transition - loop should continue with modified state.
     */
    record Continue(
        String reason,
        List<Object> toolResults
    ) implements LoopTransition {

        public static Continue toolResults(List<Object> toolResults) {
            return new Continue("tool_results", toolResults);
        }

        public static Continue maxOutputTokensRecovery(List<Object> messages) {
            return new Continue("max_output_tokens_recovery", messages);
        }

        public static Continue compactRecovery(List<Object> messages) {
            return new Continue("compact_recovery", messages);
        }

        public static Continue userInterrupt(String message) {
            return new Continue("user_interrupt", List.of());
        }

        @Override
        public boolean isTerminal() {
            return false;
        }

        /**
         * Get the reason for continuation.
         */
        public Reason getReason() {
            if ("tool_results".equals(this.reason)) return Reason.TOOL_RESULTS;
            if ("max_output_tokens_recovery".equals(this.reason)) return Reason.MAX_OUTPUT_TOKENS_RECOVERY;
            if ("compact_recovery".equals(this.reason)) return Reason.COMPACT_RECOVERY;
            if ("user_interrupt".equals(this.reason)) return Reason.USER_INTERRUPT;
            return Reason.UNKNOWN;
        }

        public enum Reason {
            TOOL_RESULTS,
            MAX_OUTPUT_TOKENS_RECOVERY,
            COMPACT_RECOVERY,
            USER_INTERRUPT,
            UNKNOWN
        }
    }

    /**
     * Terminal transition - loop should stop.
     */
    record Terminal(
        String reason,
        Object result,
        boolean isError
    ) implements LoopTransition {

        public static Terminal complete(Object result) {
            return new Terminal("complete", result, false);
        }

        public static Terminal error(String message) {
            return new Terminal("error", message, true);
        }

        public static Terminal maxTurnsReached(int turnCount) {
            return new Terminal("max_turns_reached", "Reached maximum turns: " + turnCount, false);
        }

        public static Terminal userExit() {
            return new Terminal("user_exit", "User requested exit", false);
        }

        public static Terminal budgetExceeded() {
            return new Terminal("budget_exceeded", "Token budget exceeded", true);
        }

        @Override
        public boolean isTerminal() {
            return true;
        }

        /**
         * Get the reason for termination.
         */
        public Reason getReason() {
            if ("complete".equals(this.reason)) return Reason.COMPLETE;
            if ("error".equals(this.reason)) return Reason.ERROR;
            if ("max_turns_reached".equals(this.reason)) return Reason.MAX_TURNS_REACHED;
            if ("user_exit".equals(this.reason)) return Reason.USER_EXIT;
            if ("budget_exceeded".equals(this.reason)) return Reason.BUDGET_EXCEEDED;
            return Reason.UNKNOWN;
        }

        public enum Reason {
            COMPLETE,
            ERROR,
            MAX_TURNS_REACHED,
            USER_EXIT,
            BUDGET_EXCEEDED,
            UNKNOWN
        }
    }
}