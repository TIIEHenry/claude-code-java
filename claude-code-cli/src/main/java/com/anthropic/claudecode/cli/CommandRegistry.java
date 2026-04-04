/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code CLI command registry
 */
package com.anthropic.claudecode.cli;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Command registry for CLI commands.
 *
 * <p>Manages registration and dispatch of CLI commands.
 */
public class CommandRegistry {

    private final Map<String, CommandInfo> commands = new ConcurrentHashMap<>();
    private final Map<String, String> aliases = new ConcurrentHashMap<>();

    /**
     * Register a command.
     */
    public CommandRegistry register(String name, CommandHandler handler) {
        return register(name, handler, null, null);
    }

    /**
     * Register a Command interface object.
     */
    public CommandRegistry register(String name, Command command) {
        CommandHandler handler = args -> {
            CommandContext context = new CommandContext(name, args, null, null);
            return command.execute(context);
        };
        return register(name, handler, command.description(), command.usage());
    }

    /**
     * Register a Command with aliases.
     */
    public CommandRegistry register(String name, Command command, String[] aliases) {
        CommandHandler handler = args -> {
            CommandContext context = new CommandContext(name, args, null, null);
            return command.execute(context);
        };
        return registerWithAliases(name, handler, command.description(), command.usage(), aliases);
    }

    /**
     * Unregister a command.
     */
    public CommandRegistry unregister(String name) {
        CommandInfo info = commands.remove(name);
        if (info != null && !info.aliases().isEmpty()) {
            for (String alias : info.aliases()) {
                aliases.remove(alias);
            }
        }
        return this;
    }

    /**
     * Register a command with description.
     */
    public CommandRegistry register(String name, CommandHandler handler, 
                                    String description, String usage) {
        commands.put(name, new CommandInfo(name, handler, description, usage, new ArrayList<>()));
        return this;
    }

    /**
     * Register command with aliases.
     */
    public CommandRegistry registerWithAliases(String name, CommandHandler handler,
                                               String description, String usage,
                                               String... aliasList) {
        CommandInfo info = new CommandInfo(name, handler, description, usage, Arrays.asList(aliasList));
        commands.put(name, info);

        for (String alias : aliasList) {
            aliases.put(alias, name);
        }

        return this;
    }

    /**
     * Execute a command.
     */
    public CommandResult execute(String input) {
        String[] parts = parseInput(input);
        if (parts.length == 0) {
            return CommandResult.error("Empty command");
        }

        String commandName = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        // Resolve alias
        String resolvedName = aliases.getOrDefault(commandName, commandName);
        CommandInfo command = commands.get(resolvedName);

        if (command == null) {
            return CommandResult.error("Unknown command: " + commandName);
        }

        try {
            return command.handler().execute(args);
        } catch (Exception e) {
            return CommandResult.error("Command failed: " + e.getMessage());
        }
    }

    /**
     * Check if command exists.
     */
    public boolean hasCommand(String name) {
        return commands.containsKey(name) || aliases.containsKey(name);
    }

    /**
     * Get command info.
     */
    public CommandInfo getCommand(String name) {
        String resolved = aliases.getOrDefault(name, name);
        return commands.get(resolved);
    }

    /**
     * Get all command names.
     */
    public Set<String> getCommandNames() {
        return new TreeSet<>(commands.keySet());
    }

    /**
     * Get help text for all commands.
     */
    public String getHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available commands:\n\n");

        for (String name : new TreeSet<>(commands.keySet())) {
            CommandInfo info = commands.get(name);
            sb.append("  ").append(name);

            if (!info.aliases().isEmpty()) {
                sb.append(" (").append(String.join(", ", info.aliases())).append(")");
            }

            sb.append("\n");

            if (info.description() != null) {
                sb.append("    ").append(info.description()).append("\n");
            }

            if (info.usage() != null) {
                sb.append("    Usage: ").append(info.usage()).append("\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Parse input string into command and arguments.
     */
    private String[] parseInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new String[0];
        }

        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '\0';

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (inQuotes) {
                if (c == quoteChar) {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"' || c == '\'') {
                inQuotes = true;
                quoteChar = c;
            } else if (Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            parts.add(current.toString());
        }

        return parts.toArray(new String[0]);
    }

    /**
     * Command information.
     */
    public record CommandInfo(
        String name,
        CommandHandler handler,
        String description,
        String usage,
        List<String> aliases
    ) {}

    /**
     * Command handler interface.
     */
    @FunctionalInterface
    public interface CommandHandler {
        CommandResult execute(String[] args);
    }
}
