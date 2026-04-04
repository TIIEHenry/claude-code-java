/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code CLI completer
 */
package com.anthropic.claudecode.cli;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CLI completer for command auto-completion.
 *
 * <p>Provides completion suggestions for CLI commands.
 */
public class CliCompleter {

    private final Map<String, CommandInfo> commands;
    private final Map<String, List<String>> options = new HashMap<>();

    public CliCompleter() {
        this.commands = new HashMap<>();
        registerDefaultCommands();
        registerDefaultOptions();
    }

    private void registerDefaultCommands() {
        registerCommand("help", "Show help information", "help [command]");
        registerCommand("exit", "Exit the CLI", "exit");
        registerCommand("quit", "Exit the CLI (alias)", "quit");
        registerCommand("clear", "Clear the screen", "clear");
        registerCommand("history", "Show command history", "history [n]");
        registerCommand("config", "Show or set configuration", "config [key] [value]");
        registerCommand("set", "Set configuration value", "set <key> <value>");
        registerCommand("get", "Get configuration value", "get <key>");
        registerCommand("model", "Set or show model", "model [name]");
        registerCommand("tools", "List available tools", "tools");
        registerCommand("sessions", "List sessions", "sessions");
        registerCommand("new", "Start new session", "new [name]");
        registerCommand("load", "Load a session", "load <session-id>");
        registerCommand("save", "Save current session", "save [name]");
        registerCommand("export", "Export session", "export <file>");
        registerCommand("import", "Import session", "import <file>");
    }

    private void registerDefaultOptions() {
        options.put("model", Arrays.asList("claude-sonnet-4-6", "claude-opus-4-6", "claude-haiku-4-5"));
        options.put("config", Arrays.asList("list", "get", "set", "reset"));
    }

    /**
     * Register command.
     */
    public void registerCommand(String name, String description, String usage) {
        commands.put(name, new CommandInfo(name, description, usage));
    }

    /**
     * Register command options.
     */
    public void registerOptions(String command, List<String> optionList) {
        options.put(command, new ArrayList<>(optionList));
    }

    /**
     * Complete input.
     */
    public List<Completion> complete(String input) {
        if (input == null || input.isEmpty()) {
            return completeCommand("");
        }

        String[] parts = input.split("\\s+");
        String lastPart = parts[parts.length - 1];

        if (parts.length == 1) {
            // Completing command name
            return completeCommand(lastPart);
        } else {
            // Completing command arguments
            return completeArguments(parts);
        }
    }

    /**
     * Complete and return only strings.
     */
    public List<String> completeStrings(String input) {
        return complete(input).stream()
            .map(Completion::text)
            .collect(Collectors.toList());
    }

    private List<Completion> completeCommand(String prefix) {
        List<Completion> completions = new ArrayList<>();

        for (Map.Entry<String, CommandInfo> entry : commands.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                completions.add(new Completion(
                    entry.getKey(),
                    entry.getKey(),
                    entry.getValue().description()
                ));
            }
        }

        return completions;
    }

    private List<Completion> completeArguments(String[] parts) {
        String command = parts[0];
        String lastPart = parts[parts.length - 1];

        // Get options for command
        List<String> commandOptions = options.get(command);
        if (commandOptions != null) {
            List<Completion> completions = new ArrayList<>();

            for (String option : commandOptions) {
                if (option.startsWith(lastPart)) {
                    completions.add(new Completion(option, option, null));
                }
            }

            return completions;
        }

        return Collections.emptyList();
    }

    /**
     * Get command info.
     */
    public CommandInfo getCommandInfo(String command) {
        return commands.get(command);
    }

    /**
     * Get all commands.
     */
    public Set<String> getCommands() {
        return new HashSet<>(commands.keySet());
    }

    /**
     * Check if command exists.
     */
    public boolean hasCommand(String command) {
        return commands.containsKey(command);
    }

    /**
     * Completion record.
     */
    public record Completion(
        String text,
        String displayText,
        String description
    ) {
        public boolean matches(String input) {
            return text.startsWith(input);
        }

        public String getCompletionText(String input) {
            if (text.startsWith(input)) {
                return text.substring(input.length());
            }
            return text;
        }
    }

    /**
     * Command info.
     */
    public record CommandInfo(
        String name,
        String description,
        String usage
    ) {}

    /**
     * Builder for completions.
     */
    public static class CompletionBuilder {
        private final CliCompleter completer;

        public CompletionBuilder() {
            this.completer = new CliCompleter();
        }

        public CompletionBuilder command(String name, String description, String usage) {
            completer.registerCommand(name, description, usage);
            return this;
        }

        public CompletionBuilder options(String command, String... optionList) {
            completer.registerOptions(command, Arrays.asList(optionList));
            return this;
        }

        public CliCompleter build() {
            return completer;
        }
    }

    /**
     * Create builder.
     */
    public static CompletionBuilder builder() {
        return new CompletionBuilder();
    }
}