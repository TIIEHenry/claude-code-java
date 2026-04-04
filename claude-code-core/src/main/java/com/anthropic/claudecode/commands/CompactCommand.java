/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/compact
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Compact command - Compact conversation history.
 */
public final class CompactCommand implements Command {
    @Override
    public String name() {
        return "compact";
    }

    @Override
    public String description() {
        return "Compact conversation history to reduce token usage";
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
        boolean verbose = args != null && (args.contains("--verbose") || args.contains("-v"));

        int beforeTokens = context.getMessageCount();
        context.compactHistory();
        int afterTokens = context.getMessageCount();

        StringBuilder sb = new StringBuilder();
        sb.append("Conversation history compacted.\n");

        if (verbose) {
            sb.append(String.format("Messages before: %d\n", beforeTokens));
            sb.append(String.format("Messages after:  %d\n", afterTokens));
            sb.append(String.format("Reduction:       %d%%\n",
                (beforeTokens - afterTokens) * 100 / Math.max(1, beforeTokens)));
        }

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }
}