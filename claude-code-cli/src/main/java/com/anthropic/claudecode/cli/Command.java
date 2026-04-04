/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code CLI module
 */
package com.anthropic.claudecode.cli;

import java.util.*;
import java.util.concurrent.*;

/**
 * Command interface and base implementations.
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
     * Get usage help.
     */
    String usage();

    /**
     * Execute the command.
     */
    CommandResult execute(CommandContext context);

    /**
     * Base command class.
     */
    abstract class AbstractCommand implements Command {
        protected final String name;
        protected final String description;

        protected AbstractCommand(String name, String description) {
            this.name = name;
            this.description = description;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public String usage() {
            return name + " - " + description;
        }

        protected CommandResult success(String message) {
            return CommandResult.success(message);
        }

        protected CommandResult success(Object data) {
            return CommandResult.success(data);
        }

        protected CommandResult error(String message) {
            return CommandResult.error(message);
        }

        protected String getArg(CommandContext context, int index, String defaultValue) {
            String[] args = context.args();
            return index < args.length ? args[index] : defaultValue;
        }

        protected String getArg(CommandContext context, int index) {
            return getArg(context, index, null);
        }

        protected boolean hasArg(CommandContext context, int index) {
            return index < context.args().length;
        }
    }

    /**
     * Help command.
     */
    static class HelpCommand extends AbstractCommand {
        private final CommandLineInterface cli;

        HelpCommand(CommandLineInterface cli) {
            super("help", "Show available commands");
            this.cli = cli;
        }

        @Override
        public String usage() {
            return "help [command] - Show help for command";
        }

        @Override
        public CommandResult execute(CommandContext context) {
            if (!hasArg(context, 0)) {
                // Show all commands
                StringBuilder sb = new StringBuilder();
                sb.append("Available commands:\n\n");
                for (Map.Entry<String, Command> entry : cli.getCommands().entrySet()) {
                    sb.append("  ").append(entry.getKey());
                    sb.append(" - ").append(entry.getValue().description());
                    sb.append("\n");
                }
                sb.append("\nType 'help <command>' for more details.");
                return success(sb.toString());
            }

            // Show specific command help
            String cmdName = getArg(context, 0);
            Command cmd = cli.getCommands().get(cmdName);
            if (cmd == null) {
                return error("Unknown command: " + cmdName);
            }
            return success(cmd.usage());
        }
    }

    /**
     * Exit command.
     */
    static class ExitCommand extends AbstractCommand {
        private final CommandLineInterface cli;

        ExitCommand(CommandLineInterface cli) {
            super("exit", "Exit the CLI");
            this.cli = cli;
        }

        @Override
        public String usage() {
            return "exit - Exit the program";
        }

        @Override
        public CommandResult execute(CommandContext context) {
            cli.stop();
            return success("Goodbye!");
        }
    }

    /**
     * Clear command.
     */
    static class ClearCommand extends AbstractCommand {
        ClearCommand() {
            super("clear", "Clear the screen");
        }

        @Override
        public CommandResult execute(CommandContext context) {
            // Send ANSI clear sequence
            System.out.print("\033[H\033[2J");
            System.out.flush();
            return CommandResult.empty();
        }
    }

    /**
     * Version command.
     */
    static class VersionCommand extends AbstractCommand {
        VersionCommand() {
            super("version", "Show version information");
        }

        @Override
        public CommandResult execute(CommandContext context) {
            return success("Claude Code Java v1.0.0");
        }
    }

    /**
     * Status command.
     */
    static class StatusCommand extends AbstractCommand {
        StatusCommand() {
            super("status", "Show system status");
        }

        @Override
        public CommandResult execute(CommandContext context) {
            Map<String, Object> status = new HashMap<>();
            status.put("java_version", System.getProperty("java.version"));
            status.put("os", System.getProperty("os.name"));
            status.put("memory", Runtime.getRuntime().totalMemory() / 1024 / 1024 + " MB");
            status.put("threads", Thread.activeCount());
            return success(status);
        }
    }
}