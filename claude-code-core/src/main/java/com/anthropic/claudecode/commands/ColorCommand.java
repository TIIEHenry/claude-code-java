/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/color
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Color command - Set the session color for agent identification.
 */
public final class ColorCommand implements Command {
    @Override
    public String name() {
        return "color";
    }

    @Override
    public String description() {
        return "Set the session color for agent identification";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supportsNonInteractive() {
        return true;
    }

    // Available agent colors
    private static final Set<String> AGENT_COLORS = Set.of(
        "red", "green", "blue", "yellow", "purple", "cyan", "magenta", "orange"
    );

    // Reset aliases
    private static final Set<String> RESET_ALIASES = Set.of(
        "default", "reset", "none", "gray", "grey"
    );

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        if (args == null || args.trim().isEmpty()) {
            return CompletableFuture.completedFuture(showAvailableColors(context));
        }

        String colorArg = args.trim().toLowerCase().split("\\s+")[0];

        if (RESET_ALIASES.contains(colorArg)) {
            return CompletableFuture.completedFuture(resetColor(context));
        }

        return CompletableFuture.completedFuture(setColor(context, colorArg));
    }

    private CommandResult showAvailableColors(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Please provide a color.\n\n");
        sb.append("Available colors: ").append(String.join(", ", AGENT_COLORS)).append(", default\n");
        sb.append("\nUsage: color <color>\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult resetColor(CommandContext context) {
        return CommandResult.success("Session color reset to default\n");
    }

    private CommandResult setColor(CommandContext context, String colorArg) {
        if (!AGENT_COLORS.contains(colorArg)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid color '").append(colorArg).append("'.\n\n");
            sb.append("Available colors: ").append(String.join(", ", AGENT_COLORS)).append(", default\n");
            return CommandResult.failure(sb.toString());
        }

        return CommandResult.success("Session color set to: " + colorArg + "\n");
    }
}