/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/claudeAiLimits
 */
package com.anthropic.claudecode.services.limits;

import java.util.*;
import java.time.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Claude AI limits - Manage Claude.ai usage limits.
 */
public final class ClaudeAiLimitsService {
    private volatile UsageQuota currentQuota;
    private volatile Instant quotaResetTime;
    private final List<QuotaListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Usage quota record.
     */
    public record UsageQuota(
        long maxTokens,
        long usedTokens,
        long remainingTokens,
        long maxMessages,
        long usedMessages,
        long remainingMessages,
        String tier,
        Instant resetTime,
        boolean isUnlimited
    ) {
        public double getTokenUsagePercent() {
            if (maxTokens == 0 || isUnlimited) return 0;
            return (double) usedTokens / maxTokens * 100;
        }

        public double getMessageUsagePercent() {
            if (maxMessages == 0 || isUnlimited) return 0;
            return (double) usedMessages / maxMessages * 100;
        }

        public boolean isExceeded() {
            return !isUnlimited && (remainingTokens <= 0 || remainingMessages <= 0);
        }

        public boolean isLow() {
            return !isUnlimited && (getTokenUsagePercent() > 80 || getMessageUsagePercent() > 80);
        }

        public String format() {
            if (isUnlimited) {
                return "Unlimited";
            }
            return String.format("Tokens: %d/%d (%.1f%%) | Messages: %d/%d (%.1f%%)",
                usedTokens, maxTokens, getTokenUsagePercent(),
                usedMessages, maxMessages, getMessageUsagePercent());
        }
    }

    /**
     * Tier enum.
     */
    public enum Tier {
        FREE("free", 0, 100),
        PRO("pro", 45000, 1000),
        TEAM("team", 200000, 5000),
        ENTERPRISE("enterprise", Long.MAX_VALUE, Integer.MAX_VALUE);

        private final String name;
        private final long monthlyTokens;
        private final int monthlyMessages;

        Tier(String name, long monthlyTokens, int monthlyMessages) {
            this.name = name;
            this.monthlyTokens = monthlyTokens;
            this.monthlyMessages = monthlyMessages;
        }

        public String getName() { return name; }
        public long getMonthlyTokens() { return monthlyTokens; }
        public int getMonthlyMessages() { return monthlyMessages; }
    }

    /**
     * Create service.
     */
    public ClaudeAiLimitsService() {
        this.currentQuota = new UsageQuota(
            0, 0, 0,
            0, 0, 0,
            "unknown",
            null,
            false
        );
    }

    /**
     * Update quota.
     */
    public void updateQuota(UsageQuota quota) {
        this.currentQuota = quota;
        this.quotaResetTime = quota.resetTime();

        if (quota.isExceeded() || quota.isLow()) {
            notifyListeners(quota);
        }
    }

    /**
     * Record usage.
     */
    public void recordUsage(long tokens, int messages) {
        if (currentQuota.isUnlimited()) return;

        UsageQuota updated = new UsageQuota(
            currentQuota.maxTokens(),
            currentQuota.usedTokens() + tokens,
            currentQuota.remainingTokens() - tokens,
            currentQuota.maxMessages(),
            currentQuota.usedMessages() + messages,
            currentQuota.remainingMessages() - messages,
            currentQuota.tier(),
            currentQuota.resetTime(),
            currentQuota.isUnlimited()
        );

        updateQuota(updated);
    }

    /**
     * Get current quota.
     */
    public UsageQuota getCurrentQuota() {
        return currentQuota;
    }

    /**
     * Check if can proceed.
     */
    public boolean canProceed(long estimatedTokens) {
        if (currentQuota.isUnlimited()) return true;
        return currentQuota.remainingTokens() >= estimatedTokens;
    }

    /**
     * Check if limit exceeded.
     */
    public boolean isLimitExceeded() {
        return currentQuota.isExceeded();
    }

    /**
     * Get time until reset.
     */
    public Duration getTimeUntilReset() {
        if (quotaResetTime == null) return Duration.ZERO;
        Duration remaining = Duration.between(Instant.now(), quotaResetTime);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /**
     * Add listener.
     */
    public void addListener(QuotaListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener.
     */
    public void removeListener(QuotaListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(UsageQuota quota) {
        for (QuotaListener listener : listeners) {
            listener.onQuotaUpdate(quota);
        }
    }

    /**
     * Quota listener interface.
     */
    public interface QuotaListener {
        void onQuotaUpdate(UsageQuota quota);
    }

    /**
     * Limit warning record.
     */
    public record LimitWarning(
        LimitType type,
        long current,
        long limit,
        double percentage,
        String message
    ) {
        public static LimitWarning tokens(long current, long limit) {
            double pct = (double) current / limit * 100;
            return new LimitWarning(
                LimitType.TOKENS,
                current,
                limit,
                pct,
                String.format("Token usage at %.1f%% (%d/%d)", pct, current, limit)
            );
        }

        public static LimitWarning messages(long current, long limit) {
            double pct = (double) current / limit * 100;
            return new LimitWarning(
                LimitType.MESSAGES,
                current,
                limit,
                pct,
                String.format("Message usage at %.1f%% (%d/%d)", pct, current, limit)
            );
        }
    }

    /**
     * Limit type enum.
     */
    public enum LimitType {
        TOKENS,
        MESSAGES,
        COST
    }

    /**
     * Get warnings.
     */
    public List<LimitWarning> getWarnings() {
        List<LimitWarning> warnings = new ArrayList<>();

        if (!currentQuota.isUnlimited()) {
            if (currentQuota.getTokenUsagePercent() > 80) {
                warnings.add(LimitWarning.tokens(currentQuota.usedTokens(), currentQuota.maxTokens()));
            }
            if (currentQuota.getMessageUsagePercent() > 80) {
                warnings.add(LimitWarning.messages(currentQuota.usedMessages(), currentQuota.maxMessages()));
            }
        }

        return warnings;
    }
}