/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/rateLimitMessages.ts
 */
package com.anthropic.claudecode.services;

import java.util.*;

/**
 * Rate limit message generation utilities.
 */
public final class RateLimitMessages {
    private RateLimitMessages() {}

    /**
     * All possible rate limit error message prefixes.
     */
    public static final List<String> RATE_LIMIT_ERROR_PREFIXES = List.of(
        "You've hit your",
        "You've used",
        "You're now using extra usage",
        "You're close to",
        "You're out of extra usage"
    );

    /**
     * Rate limit message record.
     */
    public record RateLimitMessage(String message, Severity severity) {
        public enum Severity {
            ERROR, WARNING
        }
    }

    /**
     * Rate limit types.
     */
    public enum RateLimitType {
        SEVEN_DAY,
        SEVEN_DAY_SONNET,
        SEVEN_DAY_OPUS,
        FIVE_HOUR,
        OVERAGE
    }

    /**
     * Check if a message is a rate limit error.
     */
    public static boolean isRateLimitErrorMessage(String text) {
        if (text == null) return false;
        return RATE_LIMIT_ERROR_PREFIXES.stream().anyMatch(text::startsWith);
    }

    /**
     * Get rate limit message based on limit state.
     */
    public static RateLimitMessage getRateLimitMessage(
            ClaudeAILimits limits, String model) {
        if (limits == null) return null;

        // Check overage scenarios
        if (limits.isUsingOverage()) {
            if ("allowed_warning".equals(limits.getOverageStatus())) {
                return new RateLimitMessage(
                    "You're close to your extra usage spending limit",
                    RateLimitMessage.Severity.WARNING
                );
            }
            return null;
        }

        // Error states
        if ("rejected".equals(limits.getStatus())) {
            return new RateLimitMessage(
                getLimitReachedText(limits, model),
                RateLimitMessage.Severity.ERROR
            );
        }

        // Warning states
        if ("allowed_warning".equals(limits.getStatus())) {
            double utilization = limits.getUtilization();
            if (utilization < 0.7) {
                return null;
            }

            String text = getEarlyWarningText(limits);
            if (text != null) {
                return new RateLimitMessage(text, RateLimitMessage.Severity.WARNING);
            }
        }

        return null;
    }

    private static String getLimitReachedText(ClaudeAILimits limits, String model) {
        String resetTime = formatResetTime(limits.getResetsAt(), true);
        String resetMessage = resetTime != null ? " · resets " + resetTime : "";

        if ("rejected".equals(limits.getOverageStatus())) {
            String overageResetTime = formatResetTime(limits.getOverageResetsAt(), true);
            String overageResetMsg = chooseEarlierReset(resetTime, overageResetTime);

            if ("out_of_credits".equals(limits.getOverageDisabledReason())) {
                return "You're out of extra usage" + overageResetMsg;
            }

            return formatLimitReachedText("limit", overageResetMsg, model);
        }

        RateLimitType type = limits.getRateLimitType();
        if (type == null) {
            return formatLimitReachedText("usage limit", resetMessage, model);
        }

        String limitName = switch (type) {
            case SEVEN_DAY_SONNET -> "Sonnet limit";
            case SEVEN_DAY_OPUS -> "Opus limit";
            case SEVEN_DAY -> "weekly limit";
            case FIVE_HOUR -> "session limit";
            default -> "usage limit";
        };

        return formatLimitReachedText(limitName, resetMessage, model);
    }

    private static String getEarlyWarningText(ClaudeAILimits limits) {
        RateLimitType type = limits.getRateLimitType();
        if (type == null) return null;

        String limitName = switch (type) {
            case SEVEN_DAY -> "weekly limit";
            case FIVE_HOUR -> "session limit";
            case SEVEN_DAY_OPUS -> "Opus limit";
            case SEVEN_DAY_SONNET -> "Sonnet limit";
            case OVERAGE -> "extra usage";
        };

        Integer used = limits.getUtilization() != null
            ? (int) Math.floor(limits.getUtilization() * 100)
            : null;
        String resetTime = formatResetTime(limits.getResetsAt(), true);

        if (used != null && resetTime != null) {
            return String.format("You've used %d%% of your %s · resets %s",
                used, limitName, resetTime);
        }

        if (used != null) {
            return String.format("You've used %d%% of your %s", used, limitName);
        }

        if (type == RateLimitType.OVERAGE) {
            limitName += " limit";
        }

        if (resetTime != null) {
            return String.format("Approaching %s · resets %s", limitName, resetTime);
        }

        return String.format("Approaching %s", limitName);
    }

    private static String chooseEarlierReset(String reset1, String reset2) {
        if (reset1 == null && reset2 == null) return "";
        if (reset1 == null) return " · resets " + reset2;
        if (reset2 == null) return " · resets " + reset1;
        // In real implementation, would compare timestamps
        return " · resets " + reset1;
    }

    private static String formatLimitReachedText(
            String limit, String resetMessage, String model) {
        return String.format("You've hit your %s%s", limit, resetMessage);
    }

    private static String formatResetTime(Long timestamp, boolean relative) {
        if (timestamp == null) return null;
        // Simplified - would use proper time formatting
        return "in " + ((timestamp - System.currentTimeMillis()) / 60000) + " minutes";
    }

    /**
     * Claude AI Limits record.
     */
    public record ClaudeAILimits(
        String status,
        RateLimitType rateLimitType,
        boolean isUsingOverage,
        String overageStatus,
        String overageDisabledReason,
        Double utilization,
        Long resetsAt,
        Long overageResetsAt
    ) {
        // Explicit getter methods for compatibility
        public String getStatus() { return status; }
        public RateLimitType getRateLimitType() { return rateLimitType; }
        public boolean isUsingOverage() { return isUsingOverage; }
        public String getOverageStatus() { return overageStatus; }
        public String getOverageDisabledReason() { return overageDisabledReason; }
        public Double getUtilization() { return utilization; }
        public Long getResetsAt() { return resetsAt; }
        public Long getOverageResetsAt() { return overageResetsAt; }
    }

    /**
     * Format overage reset message.
     */
    public static String overageResetMessage(String resetTime) {
        if (resetTime == null) return "";
        return " · resets " + resetTime;
    }
}