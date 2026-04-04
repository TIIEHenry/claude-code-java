/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bash/bashParser
 */
package com.anthropic.claudecode.utils.bash;

import java.util.*;
import java.util.regex.*;

/**
 * Bash parser - Parse bash commands.
 */
public final class BashParser {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}|\\$([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern COMMAND_SUB_PATTERN = Pattern.compile("\\$\\(([^)]+)\\)");
    private static final Pattern GLOB_PATTERN = Pattern.compile("[*?\\[\\]]");

    /**
     * Parse full command.
     */
    public static BashParseResult parse(String command) {
        if (command == null || command.trim().isEmpty()) {
            return BashParseResult.empty();
        }

        List<BashElement> elements = new ArrayList<>();
        String processed = command;

        // Parse variables
        processed = parseVariables(processed, elements);

        // Parse command substitutions
        processed = parseCommandSubstitutions(processed, elements);

        // Parse globs
        parseGlobs(processed, elements);

        // Parse pipeline
        boolean isPipeline = processed.contains("|");

        return new BashParseResult(
            command,
            processed,
            elements,
            isPipeline,
            detectCommandType(processed)
        );
    }

    /**
     * Parse variables.
     */
    private static String parseVariables(String command, List<BashElement> elements) {
        Matcher matcher = VARIABLE_PATTERN.matcher(command);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(command.substring(lastEnd, matcher.start()));
            String varName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            elements.add(new BashElement(
                BashElementType.VARIABLE,
                matcher.group(),
                varName,
                matcher.start()
            ));
            result.append(resolveVariable(varName));
            lastEnd = matcher.end();
        }

        result.append(command.substring(lastEnd));
        return result.toString();
    }

    /**
     * Resolve variable.
     */
    private static String resolveVariable(String varName) {
        // Check for special variables
        return switch (varName) {
            case "HOME" -> System.getProperty("user.home");
            case "PWD" -> System.getProperty("user.dir");
            case "USER" -> System.getProperty("user.name");
            case "PATH" -> System.getenv("PATH");
            default -> {
                String envValue = System.getenv(varName);
                yield envValue != null ? envValue : "";
            }
        };
    }

    /**
     * Parse command substitutions.
     */
    private static String parseCommandSubstitutions(String command, List<BashElement> elements) {
        Matcher matcher = COMMAND_SUB_PATTERN.matcher(command);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(command.substring(lastEnd, matcher.start()));
            String subCommand = matcher.group(1);
            elements.add(new BashElement(
                BashElementType.COMMAND_SUBSTITUTION,
                matcher.group(),
                subCommand,
                matcher.start()
            ));
            result.append("[substitution]");
            lastEnd = matcher.end();
        }

        result.append(command.substring(lastEnd));
        return result.toString();
    }

    /**
     * Parse globs.
     */
    private static void parseGlobs(String command, List<BashElement> elements) {
        Matcher matcher = GLOB_PATTERN.matcher(command);
        while (matcher.find()) {
            elements.add(new BashElement(
                BashElementType.GLOB,
                matcher.group(),
                null,
                matcher.start()
            ));
        }
    }

    /**
     * Detect command type.
     */
    private static CommandType detectCommandType(String command) {
        String trimmed = command.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 0) return CommandType.UNKNOWN;

        String executable = parts[0];

        if (ShellCommands.isRegistered(executable)) {
            return CommandType.STANDARD;
        }
        if (executable.startsWith("./") || executable.startsWith("/")) {
            return CommandType.LOCAL_SCRIPT;
        }
        if (executable.contains("=")) {
            return CommandType.ASSIGNMENT;
        }

        return CommandType.UNKNOWN;
    }

    /**
     * Check if command needs confirmation.
     */
    public static boolean needsConfirmation(String command) {
        IParsedCommand parsed = ParsedCommand.parseSync(command);
        String executable = ParsedCommand.getCommandName(command);

        if (ShellCommands.isDestructive(executable)) {
            return true;
        }

        // Check for rm with -rf
        if (executable.equals("rm") && command.contains("-rf")) {
            return true;
        }

        // Check for forced operations
        if (command.contains("-f") || command.contains("--force")) {
            return true;
        }

        return false;
    }

    /**
     * Bash element record.
     */
    public record BashElement(
        BashElementType type,
        String raw,
        String value,
        int position
    ) {}

    /**
     * Bash element type enum.
     */
    public enum BashElementType {
        VARIABLE,
        COMMAND_SUBSTITUTION,
        GLOB,
        REDIRECT,
        PIPE,
        HEREDOC,
        QUOTE,
        ESCAPE
    }

    /**
     * Command type enum.
     */
    public enum CommandType {
        STANDARD,
        LOCAL_SCRIPT,
        ASSIGNMENT,
        BUILTIN,
        FUNCTION,
        ALIAS,
        UNKNOWN
    }

    /**
     * Parse result record.
     */
    public record BashParseResult(
        String original,
        String processed,
        List<BashElement> elements,
        boolean isPipeline,
        CommandType commandType
    ) {
        public static BashParseResult empty() {
            return new BashParseResult("", "", Collections.emptyList(), false, CommandType.UNKNOWN);
        }

        public boolean hasVariables() {
            return elements.stream().anyMatch(e -> e.type() == BashElementType.VARIABLE);
        }

        public boolean hasCommandSubstitution() {
            return elements.stream().anyMatch(e -> e.type() == BashElementType.COMMAND_SUBSTITUTION);
        }

        public boolean hasGlob() {
            return elements.stream().anyMatch(e -> e.type() == BashElementType.GLOB);
        }
    }
}