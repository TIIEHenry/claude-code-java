/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/stickers
 */
package com.anthropic.claudecode.commands;

import java.util.concurrent.CompletableFuture;

/**
 * Stickers command - Order Claude Code stickers.
 */
public final class StickersCommand implements Command {
    @Override
    public String name() {
        return "stickers";
    }

    @Override
    public String description() {
        return "Order Claude Code stickers";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supportsNonInteractive() {
        return false;
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Claude Code Stickers\n");
        sb.append("====================\n\n");

        sb.append("Want Claude Code stickers? We'd love to send you some!\n\n");

        sb.append("Available sticker packs:\n");
        sb.append("  1. Classic Pack (5 stickers)\n");
        sb.append("     - Claude logo\n");
        sb.append("     - Claude Code logo\n");
        sb.append("     - 'Hello, Claude!' quote\n");
        sb.append("     - Anthropic logo\n");
        sb.append("     - AI Assistant themed\n");

        sb.append("\n  2. Developer Pack (8 stickers)\n");
        sb.append("     - All Classic Pack stickers\n");
        sb.append("     - 'Just Code It'\n");
        sb.append("     - 'Prompt Engineer'\n");
        sb.append("     - 'Tool User'\n");

        sb.append("\nTo order:\n");
        sb.append("  1. Visit: https://anthropic.com/stickers\n");
        sb.append("  2. Fill out the form with your shipping address\n");
        sb.append("  3. Choose your sticker pack\n");
        sb.append("  4. We'll ship within 2-3 weeks\n");

        sb.append("\nNote: Stickers are free for Claude Code users!\n");
        sb.append("      Limited to one order per user.\n");

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }
}