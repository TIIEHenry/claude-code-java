/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/rename
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Rename command - Rename the current session.
 */
public final class RenameCommand implements Command {
    @Override
    public String name() {
        return "rename";
    }

    @Override
    public String description() {
        return "Rename the current session";
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
            return CompletableFuture.completedFuture(showCurrentName(context));
        }

        String arg = args.trim();

        if ("--auto".equals(arg) || "-a".equals(arg)) {
            return CompletableFuture.completedFuture(autoGenerateName(context));
        }

        return CompletableFuture.completedFuture(renameSession(context, arg));
    }

    private CommandResult showCurrentName(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Session Name\n");
        sb.append("============\n\n");

        String currentName = context.getSessionName();
        sb.append("Current: ").append(currentName != null ? currentName : "(unnamed)").append("\n\n");

        sb.append("Usage:\n");
        sb.append("  rename <name>    - Set a custom name\n");
        sb.append("  rename --auto    - Auto-generate a name\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult autoGenerateName(CommandContext context) {
        String generatedName = context.generateSessionName();

        if (generatedName == null || generatedName.isEmpty()) {
            generatedName = "Session " + new java.text.SimpleDateFormat("MMM d HH:mm").format(new java.util.Date());
        }

        context.setSessionName(generatedName);

        StringBuilder sb = new StringBuilder();
        sb.append("Session renamed to: ").append(generatedName).append("\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult renameSession(CommandContext context, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            return CommandResult.failure("Session name cannot be empty");
        }

        String trimmedName = newName.trim();
        if (trimmedName.length() > 100) {
            return CommandResult.failure("Session name too long (max 100 characters)");
        }

        if (trimmedName.contains("\n") || trimmedName.contains("\r")) {
            return CommandResult.failure("Session name cannot contain newlines");
        }

        context.setSessionName(trimmedName);

        StringBuilder sb = new StringBuilder();
        sb.append("Session renamed to: ").append(trimmedName).append("\n");
        return CommandResult.success(sb.toString());
    }
}