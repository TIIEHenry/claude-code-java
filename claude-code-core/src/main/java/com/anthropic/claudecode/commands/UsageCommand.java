/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/usage
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Usage command - Show plan usage limits.
 */
public final class UsageCommand implements Command {
    @Override
    public String name() {
        return "usage";
    }

    @Override
    public String description() {
        return "Show plan usage limits";
    }

    @Override
    public List<String> aliases() {
        return List.of("limits");
    }

    @Override
    public boolean isEnabled() {
        // Only available for claude-ai subscribers
        return true; // Would check subscription status
    }

    @Override
    public boolean supportsNonInteractive() {
        return true;
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Claude AI Usage\n");
        sb.append("===============\n\n");

        // Get usage data from context
        Map<String, Object> usageData = context.getUsageData();

        if (usageData == null || usageData.isEmpty()) {
            sb.append("No usage data available.\n");
            sb.append("Run /login to see your usage.\n");
            return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
        }

        // Session limit
        Object fiveHour = usageData.get("fiveHour");
        if (fiveHour != null) {
            sb.append(formatLimit("Session", (Map<String, Object>) fiveHour));
        }

        // Weekly limit
        Object sevenDay = usageData.get("sevenDay");
        if (sevenDay != null) {
            sb.append(formatLimit("Weekly", (Map<String, Object>) sevenDay));
        }

        // Model-specific limits
        Object sevenDayOpus = usageData.get("sevenDayOpus");
        if (sevenDayOpus != null) {
            sb.append(formatLimit("Opus Weekly", (Map<String, Object>) sevenDayOpus));
        }

        Object sevenDaySonnet = usageData.get("sevenDaySonnet");
        if (sevenDaySonnet != null) {
            sb.append(formatLimit("Sonnet Weekly", (Map<String, Object>) sevenDaySonnet));
        }

        // Extra usage
        Object extraUsage = usageData.get("extraUsage");
        if (extraUsage != null) {
            sb.append("\nExtra Usage:\n");
            Map<String, Object> extra = (Map<String, Object>) extraUsage;
            if (Boolean.TRUE.equals(extra.get("isEnabled"))) {
                sb.append("  Status: Enabled\n");
                if (extra.get("utilization") != null) {
                    sb.append(String.format("  Utilization: %.0f%%\n", extra.get("utilization")));
                }
                if (extra.get("monthlyLimit") != null) {
                    sb.append(String.format("  Monthly Limit: $%.0f\n", extra.get("monthlyLimit")));
                }
            } else {
                sb.append("  Status: Not enabled\n");
            }
        }

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }

    private String formatLimit(String name, Map<String, Object> limit) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":\n");

        if (limit.get("utilization") != null) {
            double utilization = ((Number) limit.get("utilization")).doubleValue();
            sb.append(String.format("  Utilization: %.0f%%\n", utilization));

            // Progress bar
            int bars = (int) (utilization / 5);
            sb.append("  [");
            for (int i = 0; i < 20; i++) {
                sb.append(i < bars ? "=" : " ");
            }
            sb.append("]\n");
        }

        if (limit.get("resetsAt") != null) {
            sb.append("  Resets: ").append(limit.get("resetsAt")).append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }
}