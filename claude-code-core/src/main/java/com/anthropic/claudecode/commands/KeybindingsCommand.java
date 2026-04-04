/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/keybindings
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Keybindings command - Open or create keybindings configuration file.
 */
public final class KeybindingsCommand implements Command {
    @Override
    public String name() {
        return "keybindings";
    }

    @Override
    public String description() {
        return "Open or create your keybindings configuration file";
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
            return CompletableFuture.completedFuture(openKeybindingsConfig(context));
        }

        String action = args.trim().toLowerCase();

        return switch (action) {
            case "list", "ls" -> CompletableFuture.completedFuture(listKeybindings(context));
            case "edit", "open" -> CompletableFuture.completedFuture(openKeybindingsConfig(context));
            case "reset", "default" -> CompletableFuture.completedFuture(resetKeybindings(context));
            default -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Unknown action: ").append(action).append("\n\n");
                sb.append("Usage: keybindings [list|edit|reset]\n");
                yield CompletableFuture.completedFuture(CommandResult.failure(sb.toString()));
            }
        };
    }

    private CommandResult listKeybindings(CommandContext context) {
        Map<String, String> keybindings = context.getKeybindings();

        StringBuilder sb = new StringBuilder();
        sb.append("Keybindings Configuration\n");
        sb.append("=========================\n\n");

        sb.append("Custom keybindings:\n");
        if (keybindings.isEmpty()) {
            sb.append("  No custom keybindings configured.\n");
            sb.append("  Use 'keybindings edit' to create custom bindings.\n");
        } else {
            for (Map.Entry<String, String> kb : keybindings.entrySet()) {
                sb.append("  ").append(kb.getKey()).append(" -> ").append(kb.getValue()).append("\n");
            }
        }

        sb.append("\nDefault keybindings:\n");
        sb.append("  Ctrl+C - Cancel current operation\n");
        sb.append("  Ctrl+D - Exit session\n");
        sb.append("  Ctrl+L - Clear screen\n");
        sb.append("  Tab - Autocomplete\n");
        sb.append("  Up/Down - History navigation\n");

        sb.append("\nConfig file: ").append(context.getKeybindingsConfigPath()).append("\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult openKeybindingsConfig(CommandContext context) {
        String configPath = context.getKeybindingsConfigPath().toString();
        context.openKeybindingsEditor();

        StringBuilder sb = new StringBuilder();
        sb.append("Keybindings configuration opened.\n");
        sb.append("Path: ").append(configPath).append("\n\n");
        sb.append("Example keybindings.json:\n");
        sb.append("{\n");
        sb.append("  \"Ctrl+Shift+N\": \"new-session\",\n");
        sb.append("  \"Ctrl+Shift+S\": \"save-session\",\n");
        sb.append("  \"Ctrl+K\": \"clear-input\"\n");
        sb.append("}\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult resetKeybindings(CommandContext context) {
        context.resetKeybindings();

        StringBuilder sb = new StringBuilder();
        sb.append("Keybindings reset to defaults.\n");
        sb.append("Custom keybindings have been removed.\n");
        sb.append("\nUse 'keybindings edit' to create new custom bindings.\n");

        return CommandResult.success(sb.toString());
    }
}