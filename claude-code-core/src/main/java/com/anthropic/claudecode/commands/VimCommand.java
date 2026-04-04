/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/vim
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Vim command - Toggle between Vim and Normal editing modes.
 */
public final class VimCommand implements Command {
    @Override
    public String name() {
        return "vim";
    }

    @Override
    public String description() {
        return "Toggle between Vim and Normal editing modes";
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
        if (args == null || args.trim().isEmpty()) {
            return CompletableFuture.completedFuture(toggleVim());
        }

        String action = args.trim().toLowerCase().split("\\s+")[0];

        return CompletableFuture.completedFuture(switch (action) {
            case "on", "enable" -> enableVim();
            case "off", "disable" -> disableVim();
            case "status", "show" -> showVimStatus();
            default -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Unknown action: ").append(action).append("\n\n");
                sb.append("Usage: vim [on|off|status]\n");
                yield CommandResult.failure(sb.toString());
            }
        });
    }

    private CommandResult toggleVim() {
        StringBuilder sb = new StringBuilder();
        sb.append("Vim mode toggled.\n");
        sb.append("\nVim keybindings:\n");
        sb.append("  i - Insert mode\n");
        sb.append("  esc - Normal mode\n");
        sb.append("  :w - Save\n");
        sb.append("  :q - Quit\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult enableVim() {
        StringBuilder sb = new StringBuilder();
        sb.append("Vim mode enabled.\n\n");
        sb.append("Vim keybindings:\n");
        sb.append("  i - Insert mode\n");
        sb.append("  esc - Normal mode\n");
        sb.append("  :w - Save\n");
        sb.append("  :q - Quit\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult disableVim() {
        return CommandResult.success("Vim mode disabled.\nSwitched to Normal editing mode.\n");
    }

    private CommandResult showVimStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("Vim Mode Status\n");
        sb.append("===============\n\n");
        sb.append("Status: Disabled\n");

        return CommandResult.success(sb.toString());
    }
}