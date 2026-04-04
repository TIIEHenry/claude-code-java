/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/version.ts
 */
package com.anthropic.claudecode.commands;

import java.util.concurrent.CompletableFuture;

/**
 * Version command - Print the version.
 */
public final class VersionCommand implements Command {
    private static final String VERSION = "1.0.0";
    private static final String BUILD_TIME = null; // Set during build

    @Override
    public String name() {
        return "version";
    }

    @Override
    public String description() {
        return "Print the version of Claude Code Java";
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
        String version = BUILD_TIME != null
            ? VERSION + " (built " + BUILD_TIME + ")"
            : VERSION;
        return CompletableFuture.completedFuture(CommandResult.success(version));
    }
}