/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/clear
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Clear command - Clear conversation or caches.
 */
public final class ClearCommand implements Command {
    @Override
    public String name() {
        return "clear";
    }

    @Override
    public String description() {
        return "Clear the conversation history";
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
        boolean clearCache = args != null && (args.contains("--cache") || args.contains("-c"));
        boolean clearAll = args != null && (args.contains("--all") || args.contains("-a"));

        StringBuilder message = new StringBuilder();

        if (clearAll) {
            context.clearConversation();
            context.clearCaches();
            message.append("Cleared conversation and all caches.");
        } else if (clearCache) {
            context.clearCaches();
            message.append("Cleared caches.");
        } else {
            context.clearConversation();
            message.append("Cleared conversation history.");
        }

        return CompletableFuture.completedFuture(CommandResult.success(message.toString()));
    }
}