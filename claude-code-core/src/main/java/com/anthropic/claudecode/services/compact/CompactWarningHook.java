/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/compact/compactWarningHook
 */
package com.anthropic.claudecode.services.compact;

import java.time.Instant;

import java.util.*;
import java.util.concurrent.*;

/**
 * Compact warning hook - Warnings before compaction.
 */
public final class CompactWarningHook {
    private final List<WarningListener> listeners = new CopyOnWriteArrayList<>();
    private final CompactWarningState state = new CompactWarningState();

    /**
     * Warning type enum.
     */
    public enum WarningType {
        APPROACHING_LIMIT,
        LIMIT_EXCEEDED,
        COMPACTION_RECOMMENDED,
        COMPACTION_REQUIRED,
        TOKEN_BUDGET_LOW
    }

    /**
     * Warning record.
     */
    public record CompactWarning(
        WarningType type,
        String message,
        int currentTokens,
        int threshold,
        int limit,
        double percentage,
        List<String> suggestions,
        Instant timestamp
    ) {
        public static CompactWarning approachingLimit(int current, int threshold, int limit) {
            double pct = (double) current / limit * 100;
            return new CompactWarning(
                WarningType.APPROACHING_LIMIT,
                String.format("Token usage at %.0f%% (%d/%d tokens)", pct, current, limit),
                current,
                threshold,
                limit,
                pct,
                List.of("Consider compacting to free up space"),
                Instant.now()
            );
        }

        public static CompactWarning limitExceeded(int current, int limit) {
            return new CompactWarning(
                WarningType.LIMIT_EXCEEDED,
                String.format("Token limit exceeded: %d/%d tokens", current, limit),
                current,
                limit,
                limit,
                100,
                List.of("Compaction required to continue"),
                Instant.now()
            );
        }

        public boolean requiresAction() {
            return type == WarningType.LIMIT_EXCEEDED || type == WarningType.COMPACTION_REQUIRED;
        }
    }

    /**
     * Check and generate warning.
     */
    public Optional<CompactWarning> checkWarning(int currentTokens, int threshold, int limit) {
        if (currentTokens > limit) {
            CompactWarning warning = CompactWarning.limitExceeded(currentTokens, limit);
            state.recordWarning(warning);
            notifyListeners(warning);
            return Optional.of(warning);
        }

        if (currentTokens > threshold) {
            CompactWarning warning = CompactWarning.approachingLimit(currentTokens, threshold, limit);
            state.recordWarning(warning);
            notifyListeners(warning);
            return Optional.of(warning);
        }

        return Optional.empty();
    }

    /**
     * Get warning state.
     */
    public CompactWarningState getState() {
        return state;
    }

    /**
     * Add listener.
     */
    public void addListener(WarningListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener.
     */
    public void removeListener(WarningListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(CompactWarning warning) {
        for (WarningListener listener : listeners) {
            listener.onWarning(warning);
        }
    }

    /**
     * Warning listener interface.
     */
    public interface WarningListener {
        void onWarning(CompactWarning warning);
    }
}