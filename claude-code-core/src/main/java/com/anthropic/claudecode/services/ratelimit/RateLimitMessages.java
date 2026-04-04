/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/rateLimitMessages
 */
package com.anthropic.claudecode.services.ratelimit;

import java.time.*;
import java.time.format.*;
import java.util.*;

/**
 * Rate limit messages - Centralized rate limit message generation.
 *
 * Single source of truth for all rate limit-related messages.
 */
public final class RateLimitMessages {
    private static final String FEEDBACK_CHANNEL_ANT = "#briarpatch-cc";

    /**
     * Rate limit error prefixes.
     */
    public static final List<String> RATE_LIMIT_ERROR_PREFIXES = List.of(
        "You've hit your",
        "You've used",
        "You're now using extra usage",
        "You're close to",
        "You're out of extra usage"
    );

    /**
     * Severity enum.
     */
    public enum Severity {
        ERROR,
        WARNING
    }

    /**
     * Rate limit message record.
     */
    public record RateLimitMessage(
        String message,
        Severity severity
    ) {}

    /**
     * Check if message is rate limit error.
     */
    public boolean isRateLimitErrorMessage(String text) {
        if (text == null) return false;
        return RATE_LIMIT_ERROR_PREFIXES.stream().anyMatch(text::startsWith);
    }

    /**
     * Get rate limit message.
     */
    public RateLimitMessage getRateLimitMessage(ClaudeAILimits limits, String model) {
        // Check overage scenarios
        if (limits.isUsingOverage()) {
            if (limits.overageStatus() == ClaudeAILimits.Status.ALLOWED_WARNING) {
                return new RateLimitMessage(
                    "You're close to your extra usage spending limit",
                    Severity.WARNING
                );
            }
            return null;
        }

        // Error states
        if (limits.status() == ClaudeAILimits.Status.REJECTED) {
            return new RateLimitMessage(
                getLimitReachedText(limits, model),
                Severity.ERROR
            );
        }

        // Warning states
        if (limits.status() == ClaudeAILimits.Status.ALLOWED_WARNING) {
            // Check utilization threshold
            double WARNING_THRESHOLD = 0.7;
            if (limits.utilization() != null && limits.utilization() < WARNING_THRESHOLD) {
                return null;
            }

            String text = getEarlyWarningText(limits);
            if (text != null) {
                return new RateLimitMessage(text, Severity.WARNING);
            }
        }

        return null;
    }

    /**
     * Get rate limit error message.
     */
    public String getRateLimitErrorMessage(ClaudeAILimits limits, String model) {
        RateLimitMessage message = getRateLimitMessage(limits, model);
        return message != null && message.severity() == Severity.ERROR
            ? message.message()
            : null;
    }

    /**
     * Get rate limit warning.
     */
    public String getRateLimitWarning(ClaudeAILimits limits, String model) {
        RateLimitMessage message = getRateLimitMessage(limits, model);
        return message != null && message.severity() == Severity.WARNING
            ? message.message()
            : null;
    }

    /**
     * Get limit reached text.
     */
    private String getLimitReachedText(ClaudeAILimits limits, String model) {
        String resetTime = limits.resetsAt() != null
            ? formatResetTime(limits.resetsAt(), true)
            : null;
        String resetMessage = resetTime != null ? " · resets " + resetTime : "";

        // Both subscription and overage exhausted
        if (limits.overageStatus() == ClaudeAILimits.Status.REJECTED) {
            String overageResetMessage = getOverageResetMessage(limits);

            if (limits.overageDisabledReason() == ClaudeAILimits.OverageDisabledReason.OUT_OF_CREDITS) {
                return "You're out of extra usage" + overageResetMessage;
            }

            return formatLimitReachedText("limit", overageResetMessage, model);
        }

        String limitName = getLimitName(limits);
        return formatLimitReachedText(limitName, resetMessage, model);
    }

    /**
     * Get overage reset message.
     */
    private String getOverageResetMessage(ClaudeAILimits limits) {
        if (limits.resetsAt() == null && limits.overageResetsAt() == null) {
            return "";
        }

        String resetTime = formatResetTime(limits.resetsAt(), true);
        String overageResetTime = limits.overageResetsAt() != null
            ? formatResetTime(limits.overageResetsAt(), true)
            : null;

        if (limits.resetsAt() != null && limits.overageResetsAt() != null) {
            if (limits.resetsAt().isBefore(limits.overageResetsAt())) {
                return " · resets " + resetTime;
            } else {
                return " · resets " + overageResetTime;
            }
        } else if (resetTime != null) {
            return " · resets " + resetTime;
        } else if (overageResetTime != null) {
            return " · resets " + overageResetTime;
        }
        return "";
    }

    /**
     * Get limit name.
     */
    private String getLimitName(ClaudeAILimits limits) {
        if (limits.rateLimitType() == null) return "usage limit";

        return switch (limits.rateLimitType()) {
            case SEVEN_DAY_SONNET -> "Sonnet limit";
            case SEVEN_DAY_OPUS -> "Opus limit";
            case SEVEN_DAY -> "weekly limit";
            case FIVE_HOUR -> "session limit";
            default -> "usage limit";
        };
    }

    /**
     * Get early warning text.
     */
    private String getEarlyWarningText(ClaudeAILimits limits) {
        String limitName = getWarningLimitName(limits);
        if (limitName == null) return null;

        Integer used = limits.utilization() != null
            ? (int) Math.floor(limits.utilization() * 100)
            : null;
        String resetTime = limits.resetsAt() != null
            ? formatResetTime(limits.resetsAt(), true)
            : null;

        String upsell = getWarningUpsellText(limits);

        if (used != null && resetTime != null) {
            String base = String.format("You've used %d%% of your %s · resets %s", used, limitName, resetTime);
            return upsell != null ? base + " · " + upsell : base;
        }

        if (used != null) {
            String base = String.format("You've used %d%% of your %s", used, limitName);
            return upsell != null ? base + " · " + upsell : base;
        }

        if (limits.rateLimitType() == ClaudeAILimits.RateLimitType.OVERAGE) {
            limitName += " limit";
        }

        if (resetTime != null) {
            String base = String.format("Approaching %s · resets %s", limitName, resetTime);
            return upsell != null ? base + " · " + upsell : base;
        }

        String base = "Approaching " + limitName;
        return upsell != null ? base + " · " + upsell : base;
    }

    /**
     * Get warning limit name.
     */
    private String getWarningLimitName(ClaudeAILimits limits) {
        if (limits.rateLimitType() == null) return null;

        return switch (limits.rateLimitType()) {
            case SEVEN_DAY -> "weekly limit";
            case FIVE_HOUR -> "session limit";
            case SEVEN_DAY_OPUS -> "Opus limit";
            case SEVEN_DAY_SONNET -> "Sonnet limit";
            case OVERAGE -> "extra usage";
            default -> null;
        };
    }

    /**
     * Get warning upsell text.
     */
    private String getWarningUpsellText(ClaudeAILimits limits) {
        // Implementation would check subscription type and extra usage settings
        return null;
    }

    /**
     * Get using overage text.
     */
    public String getUsingOverageText(ClaudeAILimits limits) {
        String resetTime = limits.resetsAt() != null
            ? formatResetTime(limits.resetsAt(), true)
            : null;

        String limitName = getOverageLimitName(limits);
        if (limitName == null) {
            return "Now using extra usage";
        }

        String resetMessage = resetTime != null
            ? " · Your " + limitName + " resets " + resetTime
            : "";

        return "You're now using extra usage" + resetMessage;
    }

    /**
     * Get overage limit name.
     */
    private String getOverageLimitName(ClaudeAILimits limits) {
        if (limits.rateLimitType() == null) return null;

        return switch (limits.rateLimitType()) {
            case FIVE_HOUR -> "session limit";
            case SEVEN_DAY -> "weekly limit";
            case SEVEN_DAY_OPUS -> "Opus limit";
            case SEVEN_DAY_SONNET -> "Sonnet limit";
            default -> null;
        };
    }

    /**
     * Format limit reached text.
     */
    private String formatLimitReachedText(String limit, String resetMessage, String model) {
        String userType = System.getenv("USER_TYPE");
        if ("ant".equals(userType)) {
            return String.format(
                "You've hit your %s%s. If you have feedback about this limit, post in %s. You can reset your limits with /reset-limits",
                limit, resetMessage, FEEDBACK_CHANNEL_ANT
            );
        }

        return "You've hit your " + limit + resetMessage;
    }

    /**
     * Format reset time.
     */
    public String formatResetTime(Instant resetsAt, boolean relative) {
        if (resetsAt == null) return null;

        Duration duration = Duration.between(Instant.now(), resetsAt);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        if (hours > 0) {
            return String.format("in %dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("in %dm", minutes);
        } else {
            return "soon";
        }
    }

    /**
     * Claude AI limits record.
     */
    public record ClaudeAILimits(
        Status status,
        RateLimitType rateLimitType,
        Double utilization,
        Instant resetsAt,
        Instant overageResetsAt,
        boolean isUsingOverage,
        Status overageStatus,
        OverageDisabledReason overageDisabledReason
    ) {
        public enum Status {
            ALLOWED,
            ALLOWED_WARNING,
            REJECTED
        }

        public enum RateLimitType {
            SEVEN_DAY,
            FIVE_HOUR,
            SEVEN_DAY_OPUS,
            SEVEN_DAY_SONNET,
            OVERAGE
        }

        public enum OverageDisabledReason {
            OUT_OF_CREDITS,
            ORG_DISABLED,
            BILLING_ISSUE
        }
    }
}