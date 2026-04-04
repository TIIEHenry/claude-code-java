/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/compact/microCompact
 */
package com.anthropic.claudecode.services.compact;

import java.time.Duration;
import java.time.Instant;

import java.util.*;

import java.util.concurrent.*;

/**
 * Micro compact service - Small-scale context compaction.
 */
public final class MicroCompactService {
    private final AutoCompactService autoCompactService;

    /**
     * Create micro compact service.
     */
    public MicroCompactService(AutoCompactService autoCompactService) {
        this.autoCompactService = autoCompactService;
    }

    /**
     * Micro compact type enum.
     */
    public enum MicroCompactType {
        REDUNDANT_PAIRS,
        OLD_CONTEXT,
        LOW_VALUE_MESSAGES,
        DUPLICATE_CONTENT
    }

    /**
     * Micro compact options record.
     */
    public record MicroCompactOptions(
        MicroCompactType type,
        int maxTokensToSave,
        boolean preserveRecent,
        boolean preservePinned,
        double valueThreshold
    ) {
        public static MicroCompactOptions defaults() {
            return new MicroCompactOptions(
                MicroCompactType.REDUNDANT_PAIRS,
                5000,
                true,
                true,
                0.3
            );
        }
    }

    /**
     * Micro compact result record.
     */
    public record MicroCompactResult(
        boolean success,
        int tokensSaved,
        int messagesAffected,
        List<MicroCompactAction> actions,
        Duration duration
    ) {
        public static MicroCompactResult empty() {
            return new MicroCompactResult(true, 0, 0, Collections.emptyList(), Duration.ZERO);
        }
    }

    /**
     * Micro compact action record.
     */
    public record MicroCompactAction(
        String messageId,
        ActionType actionType,
        String reason,
        int tokensAffected
    ) {}

    /**
     * Action type enum.
     */
    public enum ActionType {
        REMOVE,
        SUMMARIZE,
        MERGE,
        TRIM
    }

    /**
     * Perform micro compact.
     */
    public CompletableFuture<MicroCompactResult> microCompact(
        List<AutoCompactService.MessageSummary> messages,
        MicroCompactOptions options
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Instant start = Instant.now();
            List<MicroCompactAction> actions = new ArrayList<>();
            int tokensSaved = 0;
            int messagesAffected = 0;

            try {
                switch (options.type()) {
                    case REDUNDANT_PAIRS -> {
                        var result = compactRedundantPairs(messages, options);
                        actions.addAll(result.actions());
                        tokensSaved = result.tokensSaved();
                        messagesAffected = result.messagesAffected();
                    }
                    case OLD_CONTEXT -> {
                        var result = compactOldContext(messages, options);
                        actions.addAll(result.actions());
                        tokensSaved = result.tokensSaved();
                        messagesAffected = result.messagesAffected();
                    }
                    case LOW_VALUE_MESSAGES -> {
                        var result = compactLowValue(messages, options);
                        actions.addAll(result.actions());
                        tokensSaved = result.tokensSaved();
                        messagesAffected = result.messagesAffected();
                    }
                    default -> {}
                }

                Duration duration = Duration.between(start, Instant.now());
                return new MicroCompactResult(true, tokensSaved, messagesAffected, actions, duration);
            } catch (Exception e) {
                return MicroCompactResult.empty();
            }
        });
    }

    /**
     * Compact redundant pairs.
     */
    private CompactResult compactRedundantPairs(
        List<AutoCompactService.MessageSummary> messages,
        MicroCompactOptions options
    ) {
        List<MicroCompactAction> actions = new ArrayList<>();
        int tokensSaved = 0;
        int messagesAffected = 0;

        // Find and remove redundant Q&A pairs
        for (int i = 0; i < messages.size() - 1; i++) {
            AutoCompactService.MessageSummary current = messages.get(i);
            AutoCompactService.MessageSummary next = messages.get(i + 1);

            if (isRedundantPair(current, next)) {
                actions.add(new MicroCompactAction(
                    current.id(),
                    ActionType.REMOVE,
                    "Redundant Q&A pair",
                    current.tokenCount()
                ));
                tokensSaved += current.tokenCount();
                messagesAffected++;
            }
        }

        return new CompactResult(actions, tokensSaved, messagesAffected);
    }

    /**
     * Check if pair is redundant.
     */
    private boolean isRedundantPair(
        AutoCompactService.MessageSummary a,
        AutoCompactService.MessageSummary b
    ) {
        // Simple heuristic: check if user asks same question
        if (a.role().equals("user") && b.role().equals("user")) {
            return calculateSimilarity(a.content(), b.content()) > 0.8;
        }
        return false;
    }

    /**
     * Calculate text similarity.
     */
    private double calculateSimilarity(String a, String b) {
        if (a == null || b == null) return 0;
        String[] wordsA = a.toLowerCase().split("\\s+");
        String[] wordsB = b.toLowerCase().split("\\s+");

        Set<String> setA = new HashSet<>(Arrays.asList(wordsA));
        Set<String> setB = new HashSet<>(Arrays.asList(wordsB));

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    /**
     * Compact old context.
     */
    private CompactResult compactOldContext(
        List<AutoCompactService.MessageSummary> messages,
        MicroCompactOptions options
    ) {
        List<MicroCompactAction> actions = new ArrayList<>();
        int tokensSaved = 0;
        int messagesAffected = 0;

        int cutoff = Math.max(0, messages.size() - 50); // Keep last 50 messages

        for (int i = 0; i < cutoff && tokensSaved < options.maxTokensToSave(); i++) {
            AutoCompactService.MessageSummary msg = messages.get(i);
            if (msg.canCompact()) {
                actions.add(new MicroCompactAction(
                    msg.id(),
                    ActionType.SUMMARIZE,
                    "Old context message",
                    msg.tokenCount() / 2
                ));
                tokensSaved += msg.tokenCount() / 2;
                messagesAffected++;
            }
        }

        return new CompactResult(actions, tokensSaved, messagesAffected);
    }

    /**
     * Compact low value messages.
     */
    private CompactResult compactLowValue(
        List<AutoCompactService.MessageSummary> messages,
        MicroCompactOptions options
    ) {
        List<MicroCompactAction> actions = new ArrayList<>();
        int tokensSaved = 0;
        int messagesAffected = 0;

        for (AutoCompactService.MessageSummary msg : messages) {
            if (tokensSaved >= options.maxTokensToSave()) break;

            double value = calculateMessageValue(msg);
            if (value < options.valueThreshold() && msg.canCompact()) {
                actions.add(new MicroCompactAction(
                    msg.id(),
                    ActionType.REMOVE,
                    "Low value message: " + value,
                    msg.tokenCount()
                ));
                tokensSaved += msg.tokenCount();
                messagesAffected++;
            }
        }

        return new CompactResult(actions, tokensSaved, messagesAffected);
    }

    /**
     * Calculate message value.
     */
    private double calculateMessageValue(AutoCompactService.MessageSummary msg) {
        double value = 0.5;

        // Longer messages have more value
        value += Math.min(0.3, msg.content().length() / 1000.0);

        // User messages have higher value
        if (msg.role().equals("user")) value += 0.2;

        // Recent messages have higher value
        long ageMs = System.currentTimeMillis() - msg.timestamp().toEpochMilli();
        if (ageMs < 60000) value += 0.3;
        else if (ageMs < 300000) value += 0.1;

        return Math.min(1.0, value);
    }

    /**
     * Internal result record.
     */
    private record CompactResult(
        List<MicroCompactAction> actions,
        int tokensSaved,
        int messagesAffected
    ) {}
}