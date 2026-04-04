/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code cost-tracker.ts
 */
package com.anthropic.claudecode.services.cost;

import java.util.*;
import java.util.concurrent.*;

/**
 * Cost tracking service.
 *
 * Tracks API costs, token usage, and code changes throughout a session.
 */
public final class CostTracker {
    private CostTracker() {}

    // State
    private static volatile double totalCostUSD = 0.0;
    private static volatile long totalAPIDuration = 0;
    private static volatile long totalAPIDurationWithoutRetries = 0;
    private static volatile long totalToolDuration = 0;
    private static volatile long totalLinesAdded = 0;
    private static volatile long totalLinesRemoved = 0;
    private static volatile long totalInputTokens = 0;
    private static volatile long totalOutputTokens = 0;
    private static volatile long totalCacheReadInputTokens = 0;
    private static volatile long totalCacheCreationInputTokens = 0;
    private static volatile long totalWebSearchRequests = 0;
    private static volatile boolean hasUnknownModelCost = false;

    // Model usage tracking
    private static final ConcurrentHashMap<String, ModelUsage> modelUsage = new ConcurrentHashMap<>();

    // Session info
    private static volatile String sessionId = UUID.randomUUID().toString();
    private static volatile long sessionStartTime = System.currentTimeMillis();

    /**
     * Model usage data.
     */
    public record ModelUsage(
        long inputTokens,
        long outputTokens,
        long cacheReadInputTokens,
        long cacheCreationInputTokens,
        long webSearchRequests,
        double costUSD,
        int contextWindow,
        int maxOutputTokens
    ) {
        public ModelUsage() {
            this(0, 0, 0, 0, 0, 0.0, 0, 0);
        }

        public ModelUsage add(InputOutputTokens tokens, double cost) {
            return new ModelUsage(
                inputTokens + tokens.inputTokens(),
                outputTokens + tokens.outputTokens(),
                cacheReadInputTokens + tokens.cacheReadInputTokens(),
                cacheCreationInputTokens + tokens.cacheCreationInputTokens(),
                webSearchRequests + tokens.webSearchRequests(),
                costUSD + cost,
                tokens.contextWindow(),
                tokens.maxOutputTokens()
            );
        }
    }

    public record InputOutputTokens(
        long inputTokens,
        long outputTokens,
        long cacheReadInputTokens,
        long cacheCreationInputTokens,
        long webSearchRequests,
        int contextWindow,
        int maxOutputTokens
    ) {}

    /**
     * Stored cost state for session persistence.
     */
    public record StoredCostState(
        double totalCostUSD,
        long totalAPIDuration,
        long totalAPIDurationWithoutRetries,
        long totalToolDuration,
        long totalLinesAdded,
        long totalLinesRemoved,
        Long lastDuration,
        Map<String, ModelUsage> modelUsage
    ) {}

    /**
     * Add to total session cost.
     */
    public static synchronized double addToTotalSessionCost(
        double cost,
        InputOutputTokens tokens,
        String model
    ) {
        totalCostUSD += cost;
        totalInputTokens += tokens.inputTokens();
        totalOutputTokens += tokens.outputTokens();
        totalCacheReadInputTokens += tokens.cacheReadInputTokens();
        totalCacheCreationInputTokens += tokens.cacheCreationInputTokens();
        totalWebSearchRequests += tokens.webSearchRequests();

        // Update model usage
        modelUsage.compute(model, (k, v) ->
            v == null ? new ModelUsage().add(tokens, cost) : v.add(tokens, cost));

        return cost;
    }

    /**
     * Add to total API duration.
     */
    public static void addToTotalAPIDuration(long duration) {
        totalAPIDuration += duration;
    }

    /**
     * Add to total API duration without retries.
     */
    public static void addToTotalAPIDurationWithoutRetries(long duration) {
        totalAPIDurationWithoutRetries += duration;
    }

    /**
     * Add to total tool duration.
     */
    public static void addToTotalToolDuration(long duration) {
        totalToolDuration += duration;
    }

    /**
     * Add to total lines changed.
     */
    public static void addToTotalLinesChanged(int added, int removed) {
        totalLinesAdded += added;
        totalLinesRemoved += removed;
    }

    // ─── Getters ──────────────────────────────────────────────────────────

    public static double getTotalCostUSD() {
        return totalCostUSD;
    }

    public static long getTotalAPIDuration() {
        return totalAPIDuration;
    }

    public static long getTotalAPIDurationWithoutRetries() {
        return totalAPIDurationWithoutRetries;
    }

    public static long getTotalToolDuration() {
        return totalToolDuration;
    }

    public static long getTotalDuration() {
        return System.currentTimeMillis() - sessionStartTime;
    }

    public static long getTotalLinesAdded() {
        return totalLinesAdded;
    }

    public static long getTotalLinesRemoved() {
        return totalLinesRemoved;
    }

    public static long getTotalInputTokens() {
        return totalInputTokens;
    }

    public static long getTotalOutputTokens() {
        return totalOutputTokens;
    }

    public static long getTotalCacheReadInputTokens() {
        return totalCacheReadInputTokens;
    }

    public static long getTotalCacheCreationInputTokens() {
        return totalCacheCreationInputTokens;
    }

    public static long getTotalWebSearchRequests() {
        return totalWebSearchRequests;
    }

    public static boolean hasUnknownModelCost() {
        return hasUnknownModelCost;
    }

    public static Map<String, ModelUsage> getModelUsage() {
        return new HashMap<>(modelUsage);
    }

    public static ModelUsage getUsageForModel(String model) {
        return modelUsage.get(model);
    }

    public static String getSessionId() {
        return sessionId;
    }

    // ─── State management ──────────────────────────────────────────────────

    /**
     * Reset cost state.
     */
    public static synchronized void resetCostState() {
        totalCostUSD = 0.0;
        totalAPIDuration = 0;
        totalAPIDurationWithoutRetries = 0;
        totalToolDuration = 0;
        totalLinesAdded = 0;
        totalLinesRemoved = 0;
        totalInputTokens = 0;
        totalOutputTokens = 0;
        totalCacheReadInputTokens = 0;
        totalCacheCreationInputTokens = 0;
        totalWebSearchRequests = 0;
        hasUnknownModelCost = false;
        modelUsage.clear();
        sessionId = UUID.randomUUID().toString();
        sessionStartTime = System.currentTimeMillis();
    }

    /**
     * Reset state for tests.
     */
    public static synchronized void resetStateForTests() {
        resetCostState();
    }

    /**
     * Set has unknown model cost.
     */
    public static void setHasUnknownModelCost(boolean value) {
        hasUnknownModelCost = value;
    }

    /**
     * Set cost state for restore.
     */
    public static synchronized void setCostStateForRestore(StoredCostState state) {
        totalCostUSD = state.totalCostUSD();
        totalAPIDuration = state.totalAPIDuration();
        totalAPIDurationWithoutRetries = state.totalAPIDurationWithoutRetries();
        totalToolDuration = state.totalToolDuration();
        totalLinesAdded = state.totalLinesAdded();
        totalLinesRemoved = state.totalLinesRemoved();
        if (state.modelUsage() != null) {
            modelUsage.clear();
            modelUsage.putAll(state.modelUsage());
        }
    }

    /**
     * Get stored cost state for current session.
     */
    public static StoredCostState getStoredCostState() {
        return new StoredCostState(
            totalCostUSD,
            totalAPIDuration,
            totalAPIDurationWithoutRetries,
            totalToolDuration,
            totalLinesAdded,
            totalLinesRemoved,
            getTotalDuration(),
            new HashMap<>(modelUsage)
        );
    }

    // ─── Formatting ────────────────────────────────────────────────────────

    /**
     * Format cost as string.
     */
    public static String formatCost(double cost) {
        return formatCost(cost, 4);
    }

    /**
     * Format cost with max decimal places.
     */
    public static String formatCost(double cost, int maxDecimalPlaces) {
        if (cost > 0.5) {
            return String.format("$%.2f", Math.round(cost * 100) / 100.0);
        }
        return String.format("$%." + maxDecimalPlaces + "f", cost);
    }

    /**
     * Format duration as string.
     */
    public static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Format number with thousands separator.
     */
    public static String formatNumber(long num) {
        return String.format("%,d", num);
    }

    /**
     * Format total cost summary.
     */
    public static String formatTotalCost() {
        StringBuilder sb = new StringBuilder();
        sb.append("Total cost:            ")
          .append(formatCost(totalCostUSD));
        if (hasUnknownModelCost) {
            sb.append(" (costs may be inaccurate due to usage of unknown models)");
        }
        sb.append("\n");
        sb.append("Total duration (API):  ").append(formatDuration(totalAPIDuration)).append("\n");
        sb.append("Total duration (wall): ").append(formatDuration(getTotalDuration())).append("\n");
        sb.append("Total code changes:    ")
          .append(totalLinesAdded)
          .append(totalLinesAdded == 1 ? " line" : " lines")
          .append(" added, ")
          .append(totalLinesRemoved)
          .append(totalLinesRemoved == 1 ? " line" : " lines")
          .append(" removed\n");

        sb.append(formatModelUsage());

        return sb.toString();
    }

    /**
     * Format model usage.
     */
    private static String formatModelUsage() {
        if (modelUsage.isEmpty()) {
            return "Usage:                 0 input, 0 output, 0 cache read, 0 cache write";
        }

        StringBuilder sb = new StringBuilder("Usage by model:");
        for (Map.Entry<String, ModelUsage> entry : modelUsage.entrySet()) {
            ModelUsage usage = entry.getValue();
            sb.append("\n");
            sb.append(String.format("%21s: ", entry.getKey()));
            sb.append(formatNumber(usage.inputTokens())).append(" input, ");
            sb.append(formatNumber(usage.outputTokens())).append(" output, ");
            sb.append(formatNumber(usage.cacheReadInputTokens())).append(" cache read, ");
            sb.append(formatNumber(usage.cacheCreationInputTokens())).append(" cache write");
            if (usage.webSearchRequests() > 0) {
                sb.append(", ").append(formatNumber(usage.webSearchRequests())).append(" web search");
            }
            sb.append(" (").append(formatCost(usage.costUSD())).append(")");
        }

        return sb.toString();
    }
}