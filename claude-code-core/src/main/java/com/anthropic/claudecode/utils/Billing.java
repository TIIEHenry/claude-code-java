/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/billing
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Billing - Billing and usage tracking.
 */
public final class Billing {
    private static volatile UsageStats currentUsage = UsageStats.empty();
    private static volatile BillingPlan currentPlan = BillingPlan.FREE;

    /**
     * Billing plan enum.
     */
    public enum BillingPlan {
        FREE("free", 0, 0),
        PRO("pro", 100, 1000000),
        TEAM("team", 500, 10000000),
        ENTERPRISE("enterprise", -1, -1);

        private final String name;
        private final int maxUsers;
        private final long maxTokens;

        BillingPlan(String name, int maxUsers, long maxTokens) {
            this.name = name;
            this.maxUsers = maxUsers;
            this.maxTokens = maxTokens;
        }

        public String getName() { return name; }
        public int getMaxUsers() { return maxUsers; }
        public long getMaxTokens() { return maxTokens; }
        public boolean isUnlimited() { return maxTokens < 0; }
    }

    /**
     * Usage stats record.
     */
    public record UsageStats(
        long inputTokens,
        long outputTokens,
        long totalTokens,
        int apiCalls,
        long startTime,
        long endTime
    ) {
        public static UsageStats empty() {
            return new UsageStats(0, 0, 0, 0, System.currentTimeMillis(), 0);
        }

        public UsageStats addInput(long tokens) {
            return new UsageStats(
                inputTokens + tokens,
                outputTokens,
                totalTokens + tokens,
                apiCalls + 1,
                startTime,
                endTime
            );
        }

        public UsageStats addOutput(long tokens) {
            return new UsageStats(
                inputTokens,
                outputTokens + tokens,
                totalTokens + tokens,
                apiCalls,
                startTime,
                endTime
            );
        }

        public double getCostEstimate() {
            // Simplified cost estimation
            double inputCost = inputTokens * 0.003 / 1000; // $3 per million input
            double outputCost = outputTokens * 0.015 / 1000; // $15 per million output
            return inputCost + outputCost;
        }
    }

    /**
     * Get current plan.
     */
    public static BillingPlan getPlan() {
        return currentPlan;
    }

    /**
     * Set plan.
     */
    public static void setPlan(BillingPlan plan) {
        currentPlan = plan;
    }

    /**
     * Get current usage.
     */
    public static UsageStats getUsage() {
        return currentUsage;
    }

    /**
     * Record usage.
     */
    public static void recordUsage(long inputTokens, long outputTokens) {
        currentUsage = currentUsage.addInput(inputTokens).addOutput(outputTokens);
    }

    /**
     * Check if within limits.
     */
    public static boolean isWithinLimits() {
        if (currentPlan.isUnlimited()) return true;
        return currentUsage.totalTokens() < currentPlan.getMaxTokens();
    }

    /**
     * Get remaining tokens.
     */
    public static long getRemainingTokens() {
        if (currentPlan.isUnlimited()) return Long.MAX_VALUE;
        return currentPlan.getMaxTokens() - currentUsage.totalTokens();
    }

    /**
     * Reset usage.
     */
    public static void resetUsage() {
        currentUsage = UsageStats.empty();
    }

    /**
     * Billing summary record.
     */
    public record BillingSummary(
        BillingPlan plan,
        UsageStats usage,
        double costEstimate,
        long remaining,
        boolean withinLimits
    ) {
        public String format() {
            return String.format(
                "Plan: %s | Tokens: %d/%s | Cost: $%.2f | Remaining: %s",
                plan.getName(),
                usage.totalTokens(),
                plan.isUnlimited() ? "∞" : String.valueOf(plan.getMaxTokens()),
                costEstimate,
                plan.isUnlimited() ? "∞" : String.valueOf(remaining)
            );
        }
    }

    /**
     * Get billing summary.
     */
    public static BillingSummary getSummary() {
        return new BillingSummary(
            currentPlan,
            currentUsage,
            currentUsage.getCostEstimate(),
            getRemainingTokens(),
            isWithinLimits()
        );
    }
}