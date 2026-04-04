/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Base command interface.
 */
public interface Command {
    /**
     * Get command name.
     */
    String name();

    /**
     * Get command description.
     */
    String description();

    /**
     * Execute the command.
     */
    CompletableFuture<CommandResult> execute(String args, CommandContext context);

    /**
     * Check if command is enabled.
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Check if command supports non-interactive mode.
     */
    default boolean supportsNonInteractive() {
        return false;
    }

    /**
     * Get command aliases.
     */
    default List<String> aliases() {
        return List.of();
    }
}