/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/compact/autoCompact
 */
package com.anthropic.claudecode.services.compact;

import java.util.*;
import java.util.concurrent.*;
import java.time.*;

/**
 * Auto compact service - Automatic context compaction.
 */
public final class AutoCompactService {
    private volatile boolean enabled = true;
    private volatile int targetTokenCount = 50000;
    private volatile int triggerThreshold = 150000;
    private final List<CompactListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Compact trigger enum.
     */
    public enum CompactTrigger {
        MANUAL,
        THRESHOLD_EXCEEDED,
        PERIODIC,
        PRE_TOOL_CALL,
        PRE_API_CALL
    }

    /**
     * Compact result record.
     */
    public record CompactResult(
        boolean success,
        int tokensBefore,
        int tokensAfter,
        int tokensSaved,
        double compressionRatio,
        List<CompactSummary> summaries,
        Duration duration,
        String error
    ) {
        public static CompactResult success(int before, int after, List<CompactSummary> summaries, Duration duration) {
            return new CompactResult(
                true,
                before,
                after,
                before - after,
                (double) after / before,
                summaries,
                duration,
                null
            );
        }

        public static CompactResult failure(String error) {
            return new CompactResult(false, 0, 0, 0, 0, Collections.emptyList(), Duration.ZERO, error);
        }
    }

    /**
     * Compact summary record.
     */
    public record CompactSummary(
        String messageId,
        String summary,
        int originalTokens,
        int summaryTokens,
        SummaryType type
    ) {
        public int getTokensSaved() {
            return originalTokens - summaryTokens;
        }
    }

    /**
     * Summary type enum.
     */
    public enum SummaryType {
        FULL,
        PARTIAL,
        KEY_POINTS,
        ACTION_ITEMS,
        DECISIONS
    }

    /**
     * Check if compaction needed.
     */
    public boolean needsCompact(int currentTokens) {
        return enabled && currentTokens > triggerThreshold;
    }

    /**
     * Perform compaction.
     */
    public CompletableFuture<CompactResult> compact(
        List<MessageSummary> messages,
        int currentTokens,
        CompactTrigger trigger
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Instant start = Instant.now();
            List<CompactSummary> summaries = new ArrayList<>();

            try {
                int tokensToSave = currentTokens - targetTokenCount;
                if (tokensToSave <= 0) {
                    return CompactResult.success(currentTokens, currentTokens, summaries, Duration.ZERO);
                }

                // Select messages to compact
                List<MessageSummary> toCompact = selectMessagesForCompact(messages, tokensToSave);

                // Generate summaries
                for (MessageSummary msg : toCompact) {
                    CompactSummary summary = generateSummary(msg);
                    summaries.add(summary);
                }

                int tokensAfter = currentTokens - calculateTokensSaved(summaries);
                Duration duration = Duration.between(start, Instant.now());

                CompactResult result = CompactResult.success(currentTokens, tokensAfter, summaries, duration);
                notifyListeners(result, trigger);

                return result;
            } catch (Exception e) {
                return CompactResult.failure(e.getMessage());
            }
        }, scheduler);
    }

    /**
     * Select messages for compaction.
     */
    private List<MessageSummary> selectMessagesForCompact(List<MessageSummary> messages, int tokensToSave) {
        List<MessageSummary> selected = new ArrayList<>();
        int saved = 0;

        // Start from oldest, skip recent messages
        int skipRecent = Math.max(1, messages.size() / 10);

        for (int i = 0; i < messages.size() - skipRecent && saved < tokensToSave; i++) {
            MessageSummary msg = messages.get(i);
            if (msg.canCompact()) {
                selected.add(msg);
                saved += msg.tokenCount();
            }
        }

        return selected;
    }

    /**
     * Generate summary for message.
     */
    private CompactSummary generateSummary(MessageSummary msg) {
        // Would use LLM to generate summary
        return new CompactSummary(
            msg.id(),
            "Summary of: " + msg.content().substring(0, Math.min(50, msg.content().length())),
            msg.tokenCount(),
            Math.max(10, msg.tokenCount() / 10),
            SummaryType.KEY_POINTS
        );
    }

    /**
     * Calculate tokens saved.
     */
    private int calculateTokensSaved(List<CompactSummary> summaries) {
        return summaries.stream()
            .mapToInt(CompactSummary::getTokensSaved)
            .sum();
    }

    /**
     * Set enabled.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Set target token count.
     */
    public void setTargetTokenCount(int count) {
        this.targetTokenCount = count;
    }

    /**
     * Set trigger threshold.
     */
    public void setTriggerThreshold(int threshold) {
        this.triggerThreshold = threshold;
    }

    /**
     * Add listener.
     */
    public void addListener(CompactListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener.
     */
    public void removeListener(CompactListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(CompactResult result, CompactTrigger trigger) {
        for (CompactListener listener : listeners) {
            listener.onCompact(result, trigger);
        }
    }

    /**
     * Compact listener interface.
     */
    public interface CompactListener {
        void onCompact(CompactResult result, CompactTrigger trigger);
    }

    /**
     * Message summary record.
     */
    public record MessageSummary(
        String id,
        String content,
        int tokenCount,
        String role,
        boolean canCompact,
        Instant timestamp
    ) {}

    /**
     * Compact config record.
     */
    public record CompactConfig(
        boolean enabled,
        int targetTokens,
        int triggerThreshold,
        boolean preserveSystemMessages,
        boolean preserveRecentMessages,
        int minMessagesToKeep,
        SummaryType defaultSummaryType
    ) {
        public static CompactConfig defaults() {
            return new CompactConfig(
                true,
                50000,
                150000,
                true,
                true,
                10,
                SummaryType.KEY_POINTS
            );
        }
    }

    /**
     * Shutdown service.
     */
    public void shutdown() {
        scheduler.shutdown();
    }
}