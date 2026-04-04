/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/brief
 */
package com.anthropic.claudecode.commands;

import java.util.concurrent.CompletableFuture;

/**
 * Brief command - Toggle brief-only mode.
 */
public final class BriefCommand implements Command {
    @Override
    public String name() {
        return "brief";
    }

    @Override
    public String description() {
        return "Toggle brief-only mode";
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
        boolean currentBriefMode = context.isBriefOnlyMode();
        boolean newBriefMode = !currentBriefMode;

        context.setBriefOnlyMode(newBriefMode);

        StringBuilder sb = new StringBuilder();
        sb.append("Brief Mode\n");
        sb.append("==========\n\n");

        sb.append("Status: ").append(newBriefMode ? "enabled" : "disabled").append("\n\n");

        if (newBriefMode) {
            sb.append("Brief-only mode is now enabled.\n\n");
            sb.append("In this mode:\n");
            sb.append("  - All responses are concise and focused\n");
            sb.append("  - Extra explanations are minimized\n");
            sb.append("  - Direct answers are prioritized\n");
        } else {
            sb.append("Brief-only mode is now disabled.\n\n");
            sb.append("Responses will return to normal verbosity.\n");
        }

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }
}