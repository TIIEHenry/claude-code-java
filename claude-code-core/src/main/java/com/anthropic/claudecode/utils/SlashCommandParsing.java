/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code slash command parsing utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Centralized utilities for parsing slash commands.
 */
public final class SlashCommandParsing {
    private SlashCommandParsing() {}

    /**
     * Parsed slash command result.
     */
    public record ParsedSlashCommand(
            String commandName,
            String args,
            boolean isMcp
    ) {}

    /**
     * Parse a slash command input string into its component parts.
     *
     * @param input The raw input string (should start with '/')
     * @return Parsed command name, args, and MCP flag, or null if invalid
     */
    public static ParsedSlashCommand parseSlashCommand(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        String trimmedInput = input.trim();

        // Check if input starts with '/'
        if (!trimmedInput.startsWith("/")) {
            return null;
        }

        // Remove the leading '/' and split by spaces
        String withoutSlash = trimmedInput.substring(1);
        String[] words = withoutSlash.split("\\s+");

        if (words.length == 0 || words[0].isEmpty()) {
            return null;
        }

        String commandName = words[0];
        boolean isMcp = false;
        int argsStartIndex = 1;

        // Check for MCP commands (second word is '(MCP)')
        if (words.length > 1 && "(MCP)".equals(words[1])) {
            commandName = commandName + " (MCP)";
            isMcp = true;
            argsStartIndex = 2;
        }

        // Extract arguments (everything after command name)
        String args = "";
        if (words.length > argsStartIndex) {
            args = String.join(" ", Arrays.copyOfRange(words, argsStartIndex, words.length));
        }

        return new ParsedSlashCommand(commandName, args, isMcp);
    }

    /**
     * Check if input is a slash command.
     */
    public static boolean isSlashCommand(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return input.trim().startsWith("/");
    }

    /**
     * Get command name from slash command input.
     */
    public static String getCommandName(String input) {
        ParsedSlashCommand parsed = parseSlashCommand(input);
        return parsed != null ? parsed.commandName() : null;
    }

    /**
     * Get arguments from slash command input.
     */
    public static String getArgs(String input) {
        ParsedSlashCommand parsed = parseSlashCommand(input);
        return parsed != null ? parsed.args() : null;
    }
}