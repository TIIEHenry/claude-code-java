/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/hooks
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Hooks command - View hook configurations for tool events.
 */
public final class HooksCommand implements Command {
    @Override
    public String name() {
        return "hooks";
    }

    @Override
    public String description() {
        return "View hook configurations for tool events";
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
        sb.append("Hook Configurations\n");
        sb.append("===================\n\n");

        sb.append("No hooks configured.\n\n");
        sb.append("Hooks allow you to run custom scripts when tool events occur.\n");
        sb.append("Configure hooks in your .claude/settings.json or project CLAUDE.md.\n\n");

        sb.append("Hook Types:\n");
        sb.append("  PreToolUse    - Run before a tool executes\n");
        sb.append("  PostToolUse   - Run after a tool completes\n");
        sb.append("  Notification  - Run on notifications\n");
        sb.append("  Stop          - Run when conversation ends\n");

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }
}