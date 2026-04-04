/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/ShellCommand
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;

/**
 * ShellCommand - Shell command utilities.
 */
public final class ShellCommand {
    private final String command;
    private final List<String> parts;
    private final Shell.ShellType shellType;

    /**
     * Create shell command.
     */
    public ShellCommand(String command) {
        this.command = command;
        this.shellType = Shell.getShellType();
        this.parts = parseCommand(command);
    }

    /**
     * Get original command string.
     */
    public String getCommand() {
        return command;
    }

    /**
     * Get parsed parts.
     */
    public List<String> getParts() {
        return Collections.unmodifiableList(parts);
    }

    /**
     * Parse command into parts.
     */
    private List<String> parseCommand(String cmd) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(
            "([^\\s'\"`]+|'[^']*'|\"[^\"]*\"|`[^`]*`)+"
        );

        Matcher matcher = pattern.matcher(cmd);
        while (matcher.find()) {
            String part = matcher.group();
            // Remove quotes
            if (part.startsWith("'") && part.endsWith("'")) {
                part = part.substring(1, part.length() - 1);
            } else if (part.startsWith("\"") && part.endsWith("\"")) {
                part = part.substring(1, part.length() - 1);
            }
            result.add(part);
        }

        return result;
    }

    /**
     * Quote string for shell.
     */
    public static String quote(String str) {
        if (str == null || str.isEmpty()) {
            return "''";
        }

        // Check if string needs quoting
        if (!needsQuoting(str)) {
            return str;
        }

        // Use single quotes for simple escaping
        if (!str.contains("'")) {
            return "'" + str + "'";
        }

        // Use double quotes for strings with single quotes
        return "\"" + str.replace("\"", "\\\"") + "\"";
    }

    /**
     * Check if string needs quoting.
     */
    private static boolean needsQuoting(String str) {
        Pattern specialChars = Pattern.compile("[\\s'\"`$\\\\!&|;<>(){}*?\\[\\]#~]");
        return specialChars.matcher(str).find();
    }

    /**
     * Build command from parts.
     */
    public static String build(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(quote(parts.get(i)));
        }
        return sb.toString();
    }

    /**
     * Get executable name.
     */
    public String getExecutable() {
        if (parts.isEmpty()) return "";
        return parts.get(0);
    }

    /**
     * Get arguments.
     */
    public List<String> getArguments() {
        if (parts.size() <= 1) return Collections.emptyList();
        return parts.subList(1, parts.size());
    }

    /**
     * Check if has pipe.
     */
    public boolean hasPipe() {
        return command.contains("|");
    }

    /**
     * Split by pipe.
     */
    public List<ShellCommand> splitByPipe() {
        String[] segments = command.split("\\|");
        List<ShellCommand> result = new ArrayList<>();
        for (String segment : segments) {
            result.add(new ShellCommand(segment.trim()));
        }
        return result;
    }
}