/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/fast
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Fast command - Toggle fast mode for accelerated responses.
 */
public final class FastCommand implements Command {
    @Override
    public String name() {
        return "fast";
    }

    @Override
    public String description() {
        return "Toggle fast mode for accelerated responses";
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder sb = new StringBuilder();

            boolean currentFastMode = context.isFastModeEnabled();

            String[] parts = args != null ? args.trim().split("\\s+") : new String[0];

            if (parts.length == 0 || parts[0].isEmpty()) {
                // Toggle
                boolean newMode = !currentFastMode;
                context.setFastMode(newMode);

                sb.append("Fast Mode\n");
                sb.append("=========\n\n");
                sb.append(newMode ? "Enabled" : "Disabled").append("\n\n");

                if (newMode) {
                    sb.append("Fast mode provides accelerated responses with:\n");
                    sb.append("• Lower latency streaming\n");
                    sb.append("• Prioritized request handling\n");
                    sb.append("• Optimized model routing\n\n");
                    sb.append("Note: Fast mode may consume more tokens.\n");
                } else {
                    sb.append("Returned to standard mode.\n");
                }
            } else {
                String action = parts[0].toLowerCase();
                switch (action) {
                    case "on":
                    case "enable":
                    case "true":
                        context.setFastMode(true);
                        sb.append("Fast mode enabled.\n");
                        break;
                    case "off":
                    case "disable":
                    case "false":
                        context.setFastMode(false);
                        sb.append("Fast mode disabled.\n");
                        break;
                    case "status":
                        sb.append("Fast Mode Status\n");
                        sb.append("================\n\n");
                        sb.append("Status: ").append(currentFastMode ? "Enabled" : "Disabled").append("\n");

                        if (context.isFastModeCooldown()) {
                            sb.append("Cooldown: Active (");
                            sb.append(formatCooldownRemaining(context.getFastModeCooldownEnd()));
                            sb.append(" remaining)\n");
                        }
                        break;
                    default:
                        sb.append("Unknown action: ").append(action).append("\n\n");
                        sb.append("Usage: fast [on|off|status]\n");
                        return CommandResult.failure(sb.toString());
                }
            }

            return CommandResult.success(sb.toString());
        });
    }

    private String formatCooldownRemaining(Long cooldownEnd) {
        if (cooldownEnd == null) return "unknown";

        long remaining = cooldownEnd - System.currentTimeMillis();
        if (remaining <= 0) return "0s";

        long minutes = remaining / (60 * 1000);
        long seconds = (remaining % (60 * 1000)) / 1000;

        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}