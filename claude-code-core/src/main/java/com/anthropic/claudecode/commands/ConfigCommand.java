/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/config
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Config command - Manage configuration settings.
 */
public final class ConfigCommand implements Command {
    @Override
    public String name() {
        return "config";
    }

    @Override
    public String description() {
        return "View or modify Claude Code configuration";
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
        if (args == null || args.trim().isEmpty()) {
            return CompletableFuture.completedFuture(listConfig(context));
        }

        String[] parts = args.trim().split("\\s+");
        String subcommand = parts[0].toLowerCase();

        return CompletableFuture.completedFuture(switch (subcommand) {
            case "list", "ls" -> listConfig(context);
            case "get" -> parts.length > 1 ? getConfig(context, parts[1]) :
                CommandResult.failure("Usage: /config get <key>");
            case "set" -> parts.length > 2 ? setConfig(context, parts[1], parts[2]) :
                CommandResult.failure("Usage: /config set <key> <value>");
            default -> CommandResult.failure("Unknown subcommand: " + subcommand +
                "\nAvailable: list, get, set");
        });
    }

    private CommandResult listConfig(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Claude Code Configuration\n");
        sb.append("=========================\n\n");

        Map<String, Object> config = context.getConfiguration();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            sb.append(String.format("  %-20s = %s\n", entry.getKey(), entry.getValue()));
        }

        return CommandResult.success(sb.toString());
    }

    private CommandResult getConfig(CommandContext context, String key) {
        Map<String, Object> config = context.getConfiguration();
        Object value = config.get(key);
        if (value == null) {
            return CommandResult.failure("Configuration key not found: " + key);
        }
        return CommandResult.success(key + " = " + value);
    }

    private CommandResult setConfig(CommandContext context, String key, String value) {
        Map<String, Object> config = new HashMap<>(context.getConfiguration());
        config.put(key, value);
        return CommandResult.success("Set " + key + " = " + value);
    }
}