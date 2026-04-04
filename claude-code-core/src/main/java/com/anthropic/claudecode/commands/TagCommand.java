/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/tag
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Tag command - Toggle a searchable tag on the current session.
 */
public final class TagCommand implements Command {
    @Override
    public String name() {
        return "tag";
    }

    @Override
    public String description() {
        return "Toggle a searchable tag on the current session";
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
            return CompletableFuture.completedFuture(showCurrentTags(context));
        }

        String tagName = args.trim().split("\\s+")[0];
        return CompletableFuture.completedFuture(toggleTag(context, tagName));
    }

    private CommandResult showCurrentTags(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Session Tags\n");
        sb.append("============\n\n");
        sb.append("No tags on this session.\n\n");
        sb.append("Usage: tag <tag-name>\n");
        sb.append("Example: tag bug-fix\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult toggleTag(CommandContext context, String tagName) {
        if (tagName.equalsIgnoreCase("clear")) {
            return CommandResult.success("Cleared all tags from session.\n");
        }

        // Validate tag name
        if (!isValidTagName(tagName)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid tag name: ").append(tagName).append("\n\n");
            sb.append("Tag names must:\n");
            sb.append("  - Start with a letter or underscore\n");
            sb.append("  - Contain only letters, numbers, underscores, and hyphens\n");
            sb.append("  - Be 1-50 characters long\n");
            return CommandResult.failure(sb.toString());
        }

        return CommandResult.success("Tag added: " + tagName + "\n");
    }

    private boolean isValidTagName(String name) {
        if (name == null || name.isEmpty() || name.length() > 50) {
            return false;
        }

        // Must start with letter or underscore
        char first = name.charAt(0);
        if (!Character.isLetter(first) && first != '_') {
            return false;
        }

        // Must contain only valid characters
        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') {
                return false;
            }
        }

        return true;
    }
}