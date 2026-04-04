/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bash/commands
 */
package com.anthropic.claudecode.utils.bash;

import java.util.*;

/**
 * Shell commands - Shell command definitions and utilities.
 */
public final class ShellCommands {
    private static final Map<String, CommandInfo> COMMAND_REGISTRY = new HashMap<>();

    /**
     * Command info record.
     */
    public record CommandInfo(
        String name,
        String description,
        List<String> options,
        CommandCategory category,
        boolean isSafe,
        boolean isDestructive
    ) {
        public boolean isReadOnly() {
            return !isDestructive;
        }
    }

    /**
     * Command category enum.
     */
    public enum CommandCategory {
        FILE_OPERATION,
        PROCESS_MANAGEMENT,
        NETWORK,
        SYSTEM_INFO,
        TEXT_PROCESSING,
        PACKAGE_MANAGEMENT,
        VERSION_CONTROL,
        DEVELOPMENT,
        OTHER
    }

    static {
        // Register common commands
        register(new CommandInfo("ls", "List directory contents",
            List.of("-l", "-a", "-h", "-R", "-t", "-S"),
            CommandCategory.FILE_OPERATION, true, false));

        register(new CommandInfo("cat", "Concatenate and print files",
            List.of("-n", "-b", "-s"),
            CommandCategory.FILE_OPERATION, true, false));

        register(new CommandInfo("rm", "Remove files or directories",
            List.of("-r", "-f", "-i", "-v"),
            CommandCategory.FILE_OPERATION, false, true));

        register(new CommandInfo("mv", "Move/rename files",
            List.of("-i", "-f", "-v"),
            CommandCategory.FILE_OPERATION, false, true));

        register(new CommandInfo("cp", "Copy files",
            List.of("-r", "-i", "-f", "-v", "-p"),
            CommandCategory.FILE_OPERATION, false, true));

        register(new CommandInfo("mkdir", "Create directories",
            List.of("-p", "-v"),
            CommandCategory.FILE_OPERATION, false, true));

        register(new CommandInfo("rmdir", "Remove empty directories",
            List.of("-p", "-v"),
            CommandCategory.FILE_OPERATION, false, true));

        register(new CommandInfo("grep", "Search text patterns",
            List.of("-i", "-v", "-r", "-n", "-c", "-l", "-E"),
            CommandCategory.TEXT_PROCESSING, true, false));

        register(new CommandInfo("find", "Search for files",
            List.of("-name", "-type", "-size", "-mtime", "-exec"),
            CommandCategory.FILE_OPERATION, true, false));

        register(new CommandInfo("sed", "Stream editor",
            List.of("-i", "-n", "-e"),
            CommandCategory.TEXT_PROCESSING, false, true));

        register(new CommandInfo("awk", "Pattern scanning and processing",
            List.of("-F", "-v", "-f"),
            CommandCategory.TEXT_PROCESSING, true, false));

        register(new CommandInfo("head", "Output first lines",
            List.of("-n", "-c"),
            CommandCategory.TEXT_PROCESSING, true, false));

        register(new CommandInfo("tail", "Output last lines",
            List.of("-n", "-c", "-f"),
            CommandCategory.TEXT_PROCESSING, true, false));

        register(new CommandInfo("echo", "Display message",
            List.of("-n", "-e"),
            CommandCategory.TEXT_PROCESSING, true, false));

        register(new CommandInfo("pwd", "Print working directory",
            List.of(),
            CommandCategory.SYSTEM_INFO, true, false));

        register(new CommandInfo("cd", "Change directory",
            List.of(),
            CommandCategory.FILE_OPERATION, true, false));

        register(new CommandInfo("git", "Version control",
            List.of("add", "commit", "push", "pull", "status", "log", "diff", "branch"),
            CommandCategory.VERSION_CONTROL, false, false));

        register(new CommandInfo("npm", "Node package manager",
            List.of("install", "update", "remove", "run", "test", "build"),
            CommandCategory.PACKAGE_MANAGEMENT, false, true));

        register(new CommandInfo("pip", "Python package installer",
            List.of("install", "uninstall", "list", "show", "freeze"),
            CommandCategory.PACKAGE_MANAGEMENT, false, true));

        register(new CommandInfo("docker", "Container management",
            List.of("run", "build", "ps", "stop", "rm", "exec", "logs"),
            CommandCategory.DEVELOPMENT, false, true));
    }

    /**
     * Register command.
     */
    public static void register(CommandInfo info) {
        COMMAND_REGISTRY.put(info.name(), info);
    }

    /**
     * Get command info.
     */
    public static CommandInfo getCommandInfo(String name) {
        return COMMAND_REGISTRY.get(name);
    }

    /**
     * Check if command is registered.
     */
    public static boolean isRegistered(String name) {
        return COMMAND_REGISTRY.containsKey(name);
    }

    /**
     * Check if command is safe.
     */
    public static boolean isSafe(String name) {
        CommandInfo info = COMMAND_REGISTRY.get(name);
        return info != null && info.isSafe();
    }

    /**
     * Check if command is destructive.
     */
    public static boolean isDestructive(String name) {
        CommandInfo info = COMMAND_REGISTRY.get(name);
        return info != null && info.isDestructive();
    }

    /**
     * Get all commands.
     */
    public static Map<String, CommandInfo> getAllCommands() {
        return Collections.unmodifiableMap(COMMAND_REGISTRY);
    }

    /**
     * Get commands by category.
     */
    public static List<CommandInfo> getCommandsByCategory(CommandCategory category) {
        return COMMAND_REGISTRY.values()
            .stream()
            .filter(c -> c.category() == category)
            .toList();
    }

    /**
     * Get destructive commands.
     */
    public static List<String> getDestructiveCommands() {
        return COMMAND_REGISTRY.values()
            .stream()
            .filter(CommandInfo::isDestructive)
            .map(CommandInfo::name)
            .toList();
    }

    /**
     * Get safe commands.
     */
    public static List<String> getSafeCommands() {
        return COMMAND_REGISTRY.values()
            .stream()
            .filter(CommandInfo::isSafe)
            .map(CommandInfo::name)
            .toList();
    }
}