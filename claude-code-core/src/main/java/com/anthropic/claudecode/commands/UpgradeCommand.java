/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/upgrade
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Upgrade command - Upgrade to Max for higher rate limits.
 */
public final class UpgradeCommand implements Command {
    @Override
    public String name() {
        return "upgrade";
    }

    @Override
    public String description() {
        return "Upgrade to Max for higher rate limits and more Opus";
    }

    @Override
    public boolean isEnabled() {
        String disabled = System.getenv("DISABLE_UPGRADE_COMMAND");
        if ("true".equalsIgnoreCase(disabled)) {
            return false;
        }
        // Would check subscription type - not available for enterprise
        return true;
    }

    @Override
    public boolean supportsNonInteractive() {
        return true;
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Upgrade to Claude Max\n");
        sb.append("=====================\n\n");

        sb.append("Claude Max provides:\n");
        sb.append("  • Higher rate limits\n");
        sb.append("  • More Opus usage\n");
        sb.append("  • Priority access during peak times\n");
        sb.append("  • Advanced features\n\n");

        sb.append("To upgrade:\n");
        sb.append("  1. Visit: https://claude.ai/settings/subscription\n");
        sb.append("  2. Select 'Max' plan\n");
        sb.append("  3. Complete payment\n\n");

        sb.append("After upgrading, run /logout and /login for changes to take effect.\n");

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }
}