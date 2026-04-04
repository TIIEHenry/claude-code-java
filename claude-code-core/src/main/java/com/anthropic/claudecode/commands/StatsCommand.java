/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/stats
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Stats command - Show usage statistics.
 */
public final class StatsCommand implements Command {
    @Override
    public String name() {
        return "stats";
    }

    @Override
    public String description() {
        return "Show your Claude Code usage statistics and activity";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supportsNonInteractive() {
        return true;
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        StringBuilder sb = new StringBuilder();

        sb.append("Claude Code Statistics\n");
        sb.append("======================\n\n");

        // Session stats
        sb.append("Session Statistics\n");
        sb.append("------------------\n");
        sb.append(String.format("Messages sent:     %d\n", context.getMessageCount()));
        sb.append(String.format("Tool calls:        %d\n", context.getToolCallCount()));
        sb.append(String.format("Files modified:    %d\n", context.getModifiedFileCount()));
        sb.append(String.format("Commands executed: %d\n", context.getCommandCount()));
        sb.append("\n");

        // Token stats
        sb.append("Token Usage\n");
        sb.append("-----------\n");
        sb.append(String.format("Input tokens:  %,d\n", context.getTotalInputTokens()));
        sb.append(String.format("Output tokens: %,d\n", context.getTotalOutputTokens()));
        sb.append(String.format("Cache read:    %,d\n", context.getCacheReadTokens()));
        sb.append(String.format("Cache write:   %,d\n", context.getCacheWriteTokens()));
        sb.append("\n");

        // Cost
        sb.append(String.format("Estimated cost: $%.4f\n", context.getTotalCost()));

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }
}