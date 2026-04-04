/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/context
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Context command - Manage and view conversation context.
 */
public final class ContextCommand implements Command {
    @Override
    public String name() {
        return "context";
    }

    @Override
    public String description() {
        return "Manage and view conversation context";
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
        if (args == null || args.trim().isEmpty()) {
            return CompletableFuture.completedFuture(showContextStats(context));
        }

        String[] parts = args.trim().split("\\s+");
        String action = parts[0].toLowerCase();

        return switch (action) {
            case "stats" -> CompletableFuture.completedFuture(showContextStats(context));
            case "files" -> CompletableFuture.completedFuture(showContextFiles(context));
            case "clear" -> CompletableFuture.completedFuture(clearContext(context, parts.length > 1 ? parts[1] : null));
            case "add" -> {
                if (parts.length < 2) {
                    yield CompletableFuture.completedFuture(CommandResult.failure("Usage: context add <path>"));
                }
                yield CompletableFuture.completedFuture(addContext(context, parts[1]));
            }
            case "remove", "rm" -> {
                if (parts.length < 2) {
                    yield CompletableFuture.completedFuture(CommandResult.failure("Usage: context remove <path>"));
                }
                yield CompletableFuture.completedFuture(removeContext(context, parts[1]));
            }
            default -> CompletableFuture.completedFuture(CommandResult.failure("Unknown action: " + action + "\n\nUsage: context [stats|files|clear|add|remove]\n"));
        };
    }

    private CommandResult showContextStats(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Conversation Context\n");
        sb.append("====================\n\n");

        sb.append("Messages:\n");
        sb.append(String.format("  User messages: %d\n", context.getMessageCount()));
        sb.append(String.format("  Tool calls: %d\n\n", context.getToolCallCount()));

        sb.append("Token Usage:\n");
        sb.append(String.format("  Input tokens: %,d\n", context.getTotalInputTokens()));
        sb.append(String.format("  Output tokens: %,d\n", context.getTotalOutputTokens()));
        sb.append(String.format("  Cache read: %,d\n", context.getCacheReadTokens()));
        sb.append(String.format("  Cache write: %,d\n\n", context.getCacheWriteTokens()));

        sb.append("Context Files: ").append(context.getContextFiles().size()).append("\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult showContextFiles(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Context Files\n");
        sb.append("=============\n\n");

        List<String> files = context.getContextFiles();

        if (files.isEmpty()) {
            sb.append("No files in context.\n\n");
            sb.append("Use 'context add <path>' to add files.\n");
            return CommandResult.success(sb.toString());
        }

        for (String file : files) {
            sb.append("• ").append(file).append("\n");
        }

        sb.append("\nTotal: ").append(files.size()).append(" file(s)\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult clearContext(CommandContext context, String target) {
        if (target != null) {
            context.removeContextFile(target);
            return CommandResult.success("Removed from context: " + target + "\n");
        }

        context.clearContextFiles();
        return CommandResult.success("Context cleared.\n");
    }

    private CommandResult addContext(CommandContext context, String path) {
        context.addContextFile(path);
        return CommandResult.success("Added to context: " + path + "\n");
    }

    private CommandResult removeContext(CommandContext context, String path) {
        context.removeContextFile(path);
        return CommandResult.success("Removed from context: " + path + "\n");
    }
}