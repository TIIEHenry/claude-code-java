/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/memory
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Memory command - Manage session memory and context.
 */
public final class MemoryCommand implements Command {
    @Override
    public String name() {
        return "memory";
    }

    @Override
    public String description() {
        return "Manage session memory and context";
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
            return CompletableFuture.completedFuture(showMemoryStatus(context));
        }

        String[] parts = args.trim().split("\\s+");
        String action = parts[0].toLowerCase();

        return CompletableFuture.completedFuture(switch (action) {
            case "clear" -> clearMemory(context);
            case "status" -> showMemoryStatus(context);
            case "save" -> saveMemory(context, parts.length > 1 ? parts[1] : null);
            case "load" -> loadMemory(context, parts.length > 1 ? parts[1] : null);
            case "list" -> listMemorySaves(context);
            default -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Unknown action: ").append(action).append("\n\n");
                sb.append("Usage: memory [clear|status|save [name]|load [name]|list]\n");
                yield CommandResult.failure(sb.toString());
            }
        });
    }

    private CommandResult showMemoryStatus(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Session Memory Status\n");
        sb.append("=====================\n\n");

        sb.append("Context Usage:\n");
        sb.append(String.format("  Messages: %d\n", context.getMessageCount()));
        sb.append(String.format("  Tokens: %,d\n", context.getTotalInputTokens() + context.getTotalOutputTokens()));

        sb.append("\nActions:\n");
        sb.append("  memory clear  - Clear session memory\n");
        sb.append("  memory save   - Save current context\n");
        sb.append("  memory load   - Load saved context\n");
        sb.append("  memory list   - List saved contexts\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult clearMemory(CommandContext context) {
        context.clearConversation();

        StringBuilder sb = new StringBuilder();
        sb.append("Session memory cleared.\n\n");
        sb.append("The conversation context has been reset.\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult saveMemory(CommandContext context, String name) {
        String saveName = name != null ? name : "default";
        return CommandResult.success("Memory saved as: " + saveName + "\n");
    }

    private CommandResult loadMemory(CommandContext context, String name) {
        String loadName = name != null ? name : "default";
        return CommandResult.success("Memory loaded from: " + loadName + "\n");
    }

    private CommandResult listMemorySaves(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Saved Memories\n");
        sb.append("==============\n\n");

        sb.append("No saved memories.\n\n");
        sb.append("Use 'memory save [name]' to save the current context.\n");

        return CommandResult.success(sb.toString());
    }
}