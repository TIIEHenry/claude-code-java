/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/effort
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Effort command - Set effort level for model usage.
 */
public final class EffortCommand implements Command {
    @Override
    public String name() {
        return "effort";
    }

    @Override
    public String description() {
        return "Set effort level for model usage";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supportsNonInteractive() {
        return true;
    }

    private static final Map<String, String> EFFORT_LEVELS = Map.of(
        "low", "Minimal effort - fastest responses",
        "medium", "Balanced effort - default level",
        "high", "High effort - more thorough analysis",
        "max", "Maximum effort - deepest reasoning",
        "auto", "Automatic effort based on task complexity"
    );

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        if (args == null || args.trim().isEmpty()) {
            return CompletableFuture.completedFuture(showCurrentEffort(context));
        }

        String levelArg = args.trim().toLowerCase();
        return CompletableFuture.completedFuture(setEffort(context, levelArg));
    }

    private CommandResult showCurrentEffort(CommandContext context) {
        String currentEffort = context.getEffortLevel() != null ? context.getEffortLevel() : "auto";

        StringBuilder sb = new StringBuilder();
        sb.append("Effort Level Configuration\n");
        sb.append("==========================\n\n");

        sb.append("Current: ").append(currentEffort).append("\n\n");

        sb.append("Available levels:\n");
        for (Map.Entry<String, String> entry : EFFORT_LEVELS.entrySet()) {
            String name = entry.getKey();
            String marker = name.equals(currentEffort) ? "* " : "  ";
            sb.append(marker).append(name).append(" - ").append(entry.getValue()).append("\n");
        }

        sb.append("\nUsage: effort <level>\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult setEffort(CommandContext context, String levelArg) {
        if (!EFFORT_LEVELS.containsKey(levelArg)) {
            return CommandResult.failure("Invalid effort level: " + levelArg + "\n\nAvailable levels: " + String.join(", ", EFFORT_LEVELS.keySet()) + "\n");
        }

        context.setEffortLevel(levelArg);
        return CommandResult.success("Effort set to: " + levelArg + "\n" + EFFORT_LEVELS.get(levelArg) + "\n");
    }
}