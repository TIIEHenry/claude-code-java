/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/claudeAiLimits.ts
 */
package com.anthropic.claudecode.services.session;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Claude AI limits and quota tracking.
 */
public final class ClaudeAiLimits {
    private ClaudeAiLimits() {}

    /**
     * Quota status.
     */
    public enum QuotaStatus {
        ALLOWED,
        ALLOWED_WARNING,
        REJECTED
    }

    /**
     * Rate limit type.
     */
    public enum RateLimitType {
        FIVE_HOUR("session limit"),
        SEVEN_DAY("weekly limit"),
        SEVEN_DAY_OPUS("Opus limit"),
        SEVEN_DAY_SONNET("Sonnet limit"),
        OVERAGE("extra usage limit");

        private final String displayName;

        RateLimitType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Overage disabled reason.
     */
    public enum OverageDisabledReason {
        OVERAGE_NOT_PROVISIONED,
        ORG_LEVEL_DISABLED,
        ORG_LEVEL_DISABLED_UNTIL,
        OUT_OF_CREDITS,
        SEAT_TIER_LEVEL_DISABLED,
        MEMBER_LEVEL_DISABLED,
        SEAT_TIER_ZERO_CREDIT_LIMIT,
        GROUP_ZERO_CREDIT_LIMIT,
        MEMBER_ZERO_CREDIT_LIMIT,
        ORG_SERVICE_LEVEL_DISABLED,
        ORG_SERVICE_ZERO_CREDIT_LIMIT,
        NO_LIMITS_CONFIGURED,
        UNKNOWN
    }

    /**
     * Claude AI limits state.
     */
    public record Limits(
        QuotaStatus status,
        boolean unifiedRateLimitFallbackAvailable,
        Long resetsAt,
        RateLimitType rateLimitType,
        Double utilization,
        QuotaStatus overageStatus,
        Long overageResetsAt,
        OverageDisabledReason overageDisabledReason,
        boolean isUsingOverage,
        Integer surpassedThreshold
    ) {
        public Limits() {
            this(QuotaStatus.ALLOWED, false, null, null, null,
                 null, null, null, false, null);
        }
    }

    // Current limits state
    private static volatile Limits currentLimits = new Limits();

    // Status listeners
    private static final Set<Consumer<Limits>> statusListeners = new CopyOnWriteArraySet<>();

    /**
     * Get current limits.
     */
    public static Limits getCurrentLimits() {
        return currentLimits;
    }

    /**
     * Add status change listener.
     */
    public static void addStatusListener(Consumer<Limits> listener) {
        statusListeners.add(listener);
    }

    /**
     * Remove status change listener.
     */
    public static void removeStatusListener(Consumer<Limits> listener) {
        statusListeners.remove(listener);
    }

    /**
     * Emit status change.
     */
    public static void emitStatusChange(Limits limits) {
        currentLimits = limits;
        for (Consumer<Limits> listener : statusListeners) {
            listener.accept(limits);
        }
    }

    /**
     * Check if quota is available.
     */
    public static boolean isQuotaAvailable() {
        return currentLimits.status() != QuotaStatus.REJECTED;
    }

    /**
     * Check if warning should be shown.
     */
    public static boolean shouldShowWarning() {
        return currentLimits.status() == QuotaStatus.ALLOWED_WARNING;
    }

    /**
     * Get rate limit display name.
     */
    public static String getRateLimitDisplayName(RateLimitType type) {
        return type != null ? type.getDisplayName() : "usage limit";
    }

    /**
     * Get time until reset.
     */
    public static String getTimeUntilReset(Long resetsAt) {
        if (resetsAt == null) {
            return "unknown";
        }

        long now = System.currentTimeMillis() / 1000;
        long secondsUntilReset = resetsAt - now;

        if (secondsUntilReset <= 0) {
            return "now";
        }

        long hours = secondsUntilReset / 3600;
        long minutes = (secondsUntilReset % 3600) / 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    /**
     * Extract quota status from response headers.
     */
    public static void extractQuotaStatusFromHeaders(Map<String, String> headers) {
        // Parse headers
        String statusStr = headers.get("anthropic-ratelimit-unified-status");
        QuotaStatus status = parseQuotaStatus(statusStr);

        String resetsAtStr = headers.get("anthropic-ratelimit-unified-reset");
        Long resetsAt = resetsAtStr != null ? Long.parseLong(resetsAtStr) : null;

        boolean fallbackAvailable = "available".equals(
            headers.get("anthropic-ratelimit-unified-fallback")
        );

        String rateLimitTypeStr = headers.get(
            "anthropic-ratelimit-unified-representative-claim"
        );
        RateLimitType rateLimitType = parseRateLimitType(rateLimitTypeStr);

        String overageStatusStr = headers.get(
            "anthropic-ratelimit-unified-overage-status"
        );
        QuotaStatus overageStatus = parseQuotaStatus(overageStatusStr);

        String overageResetsAtStr = headers.get(
            "anthropic-ratelimit-unified-overage-reset"
        );
        Long overageResetsAt = overageResetsAtStr != null ?
            Long.parseLong(overageResetsAtStr) : null;

        String overageDisabledReasonStr = headers.get(
            "anthropic-ratelimit-unified-overage-disabled-reason"
        );
        OverageDisabledReason overageDisabledReason = parseOverageDisabledReason(
            overageDisabledReasonStr
        );

        boolean isUsingOverage = status == QuotaStatus.REJECTED &&
            (overageStatus == QuotaStatus.ALLOWED ||
             overageStatus == QuotaStatus.ALLOWED_WARNING);

        String utilizationStr = headers.get(
            "anthropic-ratelimit-unified-5h-utilization"
        );
        Double utilization = utilizationStr != null ?
            Double.parseDouble(utilizationStr) : null;

        String surpassedThresholdStr = headers.get(
            "anthropic-ratelimit-unified-5h-surpassed-threshold"
        );
        Integer surpassedThreshold = surpassedThresholdStr != null ?
            Integer.parseInt(surpassedThresholdStr) : null;

        // Create new limits
        Limits newLimits = new Limits(
            status,
            fallbackAvailable,
            resetsAt,
            rateLimitType,
            utilization,
            overageStatus,
            overageResetsAt,
            overageDisabledReason,
            isUsingOverage,
            surpassedThreshold
        );

        // Update if changed
        if (!newLimits.equals(currentLimits)) {
            emitStatusChange(newLimits);
        }
    }

    private static QuotaStatus parseQuotaStatus(String status) {
        if (status == null) return QuotaStatus.ALLOWED;
        return switch (status.toLowerCase()) {
            case "allowed" -> QuotaStatus.ALLOWED;
            case "allowed_warning" -> QuotaStatus.ALLOWED_WARNING;
            case "rejected" -> QuotaStatus.REJECTED;
            default -> QuotaStatus.ALLOWED;
        };
    }

    private static RateLimitType parseRateLimitType(String type) {
        if (type == null) return null;
        return switch (type.toLowerCase()) {
            case "five_hour" -> RateLimitType.FIVE_HOUR;
            case "seven_day" -> RateLimitType.SEVEN_DAY;
            case "seven_day_opus" -> RateLimitType.SEVEN_DAY_OPUS;
            case "seven_day_sonnet" -> RateLimitType.SEVEN_DAY_SONNET;
            case "overage" -> RateLimitType.OVERAGE;
            default -> null;
        };
    }

    private static OverageDisabledReason parseOverageDisabledReason(String reason) {
        if (reason == null) return null;
        try {
            return OverageDisabledReason.valueOf(reason.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OverageDisabledReason.UNKNOWN;
        }
    }

    /**
     * Consumer interface for listeners.
     */
    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T t);
    }
}