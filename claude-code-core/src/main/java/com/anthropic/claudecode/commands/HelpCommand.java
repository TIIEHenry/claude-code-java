/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/help
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Help command - Display available commands.
 */
public final class HelpCommand implements Command {
    private final CommandRegistry registry;

    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String description() {
        return "Display available commands and their descriptions";
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
        sb.append("Claude Code Java - Available Commands\n");
        sb.append("=====================================\n\n");

        for (Command cmd : registry.getAllCommands()) {
            if (cmd.isEnabled()) {
                sb.append(String.format("  /%-15s %s\n", cmd.name(), cmd.description()));
            }
        }

        sb.append("\nFor detailed help on a command, use: /<command> --help");

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }
}