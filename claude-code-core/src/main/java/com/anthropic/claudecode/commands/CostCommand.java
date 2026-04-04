/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/cost
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Cost command - Show usage cost.
 */
public final class CostCommand implements Command {
    @Override
    public String name() {
        return "cost";
    }

    @Override
    public String description() {
        return "Show your Claude Code usage cost";
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

        // Get stats from context
        long inputTokens = context.getTotalInputTokens();
        long outputTokens = context.getTotalOutputTokens();
        double cost = context.getTotalCost();

        sb.append("Claude Code Usage Statistics\n");
        sb.append("============================\n\n");
        sb.append(String.format("Input tokens:  %,d\n", inputTokens));
        sb.append(String.format("Output tokens: %,d\n", outputTokens));
        sb.append(String.format("Total tokens:  %,d\n", inputTokens + outputTokens));
        sb.append(String.format("Estimated cost: $%.4f\n", cost));

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }
}