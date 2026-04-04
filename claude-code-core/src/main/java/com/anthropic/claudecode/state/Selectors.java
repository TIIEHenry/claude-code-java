/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code state/selectors.ts
 */
package com.anthropic.claudecode.state;

import java.util.Set;
import java.util.function.*;

/**
 * State selectors for derived data.
 */
public final class Selectors {
    private Selectors() {}

    /**
     * Select session ID.
     */
    public static Function<AppState, String> sessionId() {
        return AppState::getSessionId;
    }

    /**
     * Select conversation ID.
     */
    public static Function<AppState, String> conversationId() {
        return AppState::getConversationId;
    }

    /**
     * Select model.
     */
    public static Function<AppState, String> model() {
        return AppState::getModel;
    }

    /**
     * Select query state.
     */
    public static Function<AppState, AppState.QueryState> queryState() {
        return AppState::getQueryState;
    }

    /**
     * Select if query is running.
     */
    public static Function<AppState, Boolean> isQueryRunning() {
        return state -> {
            AppState.QueryState qs = state.getQueryState();
            return qs == AppState.QueryState.RUNNING || qs == AppState.QueryState.STREAMING;
        };
    }

    /**
     * Select current turn.
     */
    public static Function<AppState, Integer> currentTurn() {
        return AppState::getCurrentTurn;
    }

    /**
     * Select permission mode.
     */
    public static Function<AppState, AppState.PermissionMode> permissionMode() {
        return AppState::getPermissionMode;
    }

    /**
     * Select input tokens.
     */
    public static Function<AppState, Long> inputTokens() {
        return AppState::getInputTokens;
    }

    /**
     * Select output tokens.
     */
    public static Function<AppState, Long> outputTokens() {
        return AppState::getOutputTokens;
    }

    /**
     * Select total tokens.
     */
    public static Function<AppState, Long> totalTokens() {
        return state -> state.getInputTokens() + state.getOutputTokens();
    }

    /**
     * Select total cost.
     */
    public static Function<AppState, Double> totalCost() {
        return AppState::getTotalCost;
    }

    /**
     * Select in-progress tools.
     */
    public static Function<AppState, Set<String>> inProgressTools() {
        return AppState::getInProgressToolIds;
    }

    /**
     * Select has in-progress tools.
     */
    public static Function<AppState, Boolean> hasInProgressTools() {
        return state -> !state.getInProgressToolIds().isEmpty();
    }
}