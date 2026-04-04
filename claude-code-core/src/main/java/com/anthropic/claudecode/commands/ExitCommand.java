/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/exit
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Exit command - Exit Claude Code.
 */
public final class ExitCommand implements Command {
    @Override
    public String name() {
        return "exit";
    }

    @Override
    public String description() {
        return "Exit Claude Code";
    }

    @Override
    public List<String> aliases() {
        return List.of("quit", "q");
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
        return CompletableFuture.completedFuture(CommandResult.success("Goodbye!"));
    }
}