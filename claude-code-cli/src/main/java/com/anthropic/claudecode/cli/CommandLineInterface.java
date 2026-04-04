/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code CLI module
 */
package com.anthropic.claudecode.cli;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;

/**
 * Command line interface for Claude Code.
 */
public class CommandLineInterface {

    private final Map<String, Command> commands = new ConcurrentHashMap<>();
    private final List<CliListener> listeners = new CopyOnWriteArrayList<>();
    private final CommandRegistry registry;
    private final InputReader inputReader;
    private final OutputWriter outputWriter;
    private volatile boolean running = false;
    private final CliConfig config;

    public CommandLineInterface() {
        this(new CliConfig());
    }

    public CommandLineInterface(CliConfig config) {
        this.config = config;
        this.registry = new CommandRegistry();
        this.inputReader = new InputReader.ConsoleInputReader();
        this.outputWriter = new OutputWriter.ConsoleOutputWriter();
        registerDefaultCommands();
    }

    private void registerDefaultCommands() {
        commands.put("help", new Command.HelpCommand(this));
        commands.put("exit", new Command.ExitCommand(this));
        commands.put("quit", new Command.ExitCommand(this));
        commands.put("clear", new Command.ClearCommand());
        commands.put("version", new Command.VersionCommand());
        commands.put("status", new Command.StatusCommand());
    }

    /**
     * Register a command.
     */
    public void register(String name, Command command) {
        commands.put(name, command);
        registry.register(name, command);
    }

    /**
     * Register with aliases.
     */
    public void register(String name, Command command, String... aliases) {
        commands.put(name, command);
        for (String alias : aliases) {
            commands.put(alias, command);
        }
        registry.register(name, command, aliases);
    }

    /**
     * Unregister a command.
     */
    public void unregister(String name) {
        commands.remove(name);
        registry.unregister(name);
    }

    /**
     * Start interactive mode.
     */
    public void startInteractive() {
        running = true;
        outputWriter.writeWelcome();

        while (running) {
            try {
                String line = inputReader.readLine(config.prompt());

                if (line == null || line.trim().isEmpty()) {
                    continue;
                }

                // Parse command
                String[] parts = parseLine(line);
                String commandName = parts[0];
                String[] args = Arrays.copyOfRange(parts, 1, parts.length);

                // Execute command
                CommandResult result = execute(commandName, args);

                // Output result
                outputWriter.writeResult(result);

                // Notify listeners
                notifyListeners(commandName, args, result);

            } catch (Exception e) {
                outputWriter.writeError(e.getMessage());
            }
        }
    }

    /**
     * Execute a command.
     */
    public CommandResult execute(String name, String[] args) {
        Command command = commands.get(name);

        if (command == null) {
            return CommandResult.error("Unknown command: " + name);
        }

        try {
            CommandContext context = new CommandContext(
                name, args, this, config
            );
            return command.execute(context);
        } catch (Exception e) {
            return CommandResult.error("Command failed: " + e.getMessage());
        }
    }

    /**
     * Execute command line.
     */
    public CommandResult executeLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return CommandResult.empty();
        }

        String[] parts = parseLine(line);
        String commandName = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        return execute(commandName, args);
    }

    /**
     * Run single command and exit.
     */
    public void runSingle(String[] args) {
        if (args.length == 0) {
            outputWriter.writeHelp();
            return;
        }

        String commandName = args[0];
        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

        CommandResult result = execute(commandName, commandArgs);
        outputWriter.writeResult(result);
    }

    /**
     * Stop the CLI.
     */
    public void stop() {
        running = false;
    }

    /**
     * Check if running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Get registered commands.
     */
    public Map<String, Command> getCommands() {
        return new HashMap<>(commands);
    }

    /**
     * Get command names.
     */
    public Set<String> getCommandNames() {
        return new HashSet<>(commands.keySet());
    }

    /**
     * Check if command exists.
     */
    public boolean hasCommand(String name) {
        return commands.containsKey(name);
    }

    /**
     * Get config.
     */
    public CliConfig config() {
        return config;
    }

    /**
     * Get registry.
     */
    public CommandRegistry registry() {
        return registry;
    }

    /**
     * Add listener.
     */
    public void addListener(CliListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener.
     */
    public void removeListener(CliListener listener) {
        listeners.remove(listener);
    }

    private String[] parseLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
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

    private void notifyListeners(String command, String[] args, CommandResult result) {
        listeners.forEach(l -> l.onCommand(command, args, result));
    }

    /**
     * CLI configuration.
     */
    public record CliConfig(
        String prompt,
        boolean colorOutput,
        boolean showTimestamp,
        boolean debugMode
    ) {
        public CliConfig() {
            this("claude> ", true, false, false);
        }
    }

    /**
     * CLI listener interface.
     */
    public interface CliListener {
        void onCommand(String command, String[] args, CommandResult result);
    }
}