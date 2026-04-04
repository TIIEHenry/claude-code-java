/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bash/commands.ts
 */
package com.anthropic.claudecode.utils.bash;

import java.util.*;
import java.util.regex.*;

/**
 * Command parsing and analysis utilities.
 */
public final class CommandUtils {
    private CommandUtils() {}

    // File descriptors for standard input/output/error
    private static final Set<String> ALLOWED_FILE_DESCRIPTORS = Set.of("0", "1", "2");

    // Control operators
    private static final Set<String> COMMAND_LIST_SEPARATORS = Set.of("&&", "||", ";", ";;", "|");
    private static final Set<String> ALL_CONTROL_OPERATORS = Set.of("&&", "||", ";", ";;", "|", ">&", ">", ">>");

    // Regex for valid alphanumeric tokens
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    /**
     * Split command with operators preserved.
     */
    public static List<String> splitCommandWithOperators(String command) {
        if (command == null || command.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Generate unique placeholders
        Placeholders placeholders = generatePlaceholders();

        // Extract heredocs
        HeredocUtils.HeredocExtractionResult heredocResult = HeredocUtils.extractHeredocs(command);
        String processedCommand = heredocResult.processedCommand();

        // Join continuation lines
        String commandWithContinuations = joinContinuationLines(processedCommand);
        String originalJoined = joinContinuationLines(command);

        // Add placeholders for quotes
        String commandWithPlaceholders = addPlaceholders(commandWithContinuations, placeholders);

        // Parse the command
        ShellQuoteUtils.ShellParseResult parseResult = ShellQuoteUtils.tryParseShellCommand(commandWithPlaceholders);

        if (!parseResult.success) {
            return Collections.singletonList(originalJoined);
        }

        List<String> parsed = parseResult.tokens;
        if (parsed.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // Process parts
            List<Object> parts = new ArrayList<>();
            for (String part : parsed) {
                if (part.equals(placeholders.NEW_LINE)) {
                    parts.add(null);
                } else if (parts.size() > 0 && parts.get(parts.size() - 1) instanceof String) {
                    String last = (String) parts.get(parts.size() - 1);
                    parts.set(parts.size() - 1, last + " " + part);
                } else {
                    parts.add(part);
                }
            }

            // Remove nulls and map placeholders
            List<String> result = new ArrayList<>();
            for (Object part : parts) {
                if (part != null && part instanceof String) {
                    String s = (String) part;
                    s = removePlaceholders(s, placeholders);
                    result.add(s);
                }
            }

            // Restore heredocs
            return HeredocUtils.restoreHeredocs(result, heredocResult.heredocs());
        } catch (Exception e) {
            return Collections.singletonList(originalJoined);
        }
    }

    /**
     * Filter out control operators.
     */
    public static List<String> filterControlOperators(List<String> parts) {
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (!ALL_CONTROL_OPERATORS.contains(part)) {
                result.add(part);
            }
        }
        return result;
    }

    /**
     * Split command deprecated - legacy regex path.
     * @deprecated Use splitCommandWithOperators instead
     */
    @Deprecated
    public static List<String> splitCommandDeprecated(String command) {
        List<String> parts = splitCommandWithOperators(command);

        // Handle redirections
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            if (part == null) continue;

            if (">&".equals(part) || ">".equals(part) || ">>".equals(part)) {
                String prevPart = i > 0 ? parts.get(i - 1) : null;
                String nextPart = i < parts.size() - 1 ? parts.get(i + 1) : null;
                String afterNext = i < parts.size() - 2 ? parts.get(i + 2) : null;

                if (nextPart == null) continue;

                boolean shouldStrip = false;
                boolean stripThird = false;

                if (">&".equals(part) && ALLOWED_FILE_DESCRIPTORS.contains(nextPart.trim())) {
                    shouldStrip = true;
                } else if (">".equals(part) && "&".equals(nextPart) &&
                           afterNext != null && ALLOWED_FILE_DESCRIPTORS.contains(afterNext.trim())) {
                    shouldStrip = true;
                    stripThird = true;
                } else if (">".equals(part) && nextPart.startsWith("&") &&
                           nextPart.length() > 1 && ALLOWED_FILE_DESCRIPTORS.contains(nextPart.substring(1))) {
                    shouldStrip = true;
                } else if ((">".equals(part) || ">>".equals(part)) &&
                           isStaticRedirectTarget(nextPart)) {
                    shouldStrip = true;
                }

                if (shouldStrip) {
                    // Handle file descriptor prefix
                    if (prevPart != null && prevPart.length() >= 3) {
                        char lastChar = prevPart.charAt(prevPart.length() - 1);
                        if (ALLOWED_FILE_DESCRIPTORS.contains(String.valueOf(lastChar)) &&
                            prevPart.charAt(prevPart.length() - 2) == ' ') {
                            parts.set(i - 1, prevPart.substring(0, prevPart.length() - 2));
                        }
                    }

                    parts.set(i, null);
                    parts.set(i + 1, null);
                    if (stripThird && i + 2 < parts.size()) {
                        parts.set(i + 2, null);
                    }
                }
            }
        }

        // Remove nulls and empty strings
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                result.add(part);
            }
        }

        return filterControlOperators(result);
    }

    /**
     * Check if command is a help command.
     */
    public static boolean isHelpCommand(String command) {
        if (command == null) return false;

        String trimmed = command.trim();

        if (!trimmed.endsWith("--help")) return false;

        if (trimmed.contains("\"") || trimmed.contains("'")) return false;

        ShellQuoteUtils.ShellParseResult result = ShellQuoteUtils.tryParseShellCommand(trimmed);
        if (!result.success) return false;

        boolean foundHelp = false;
        for (String token : result.tokens) {
            if (token.startsWith("-")) {
                if ("--help".equals(token)) {
                    foundHelp = true;
                } else {
                    return false;
                }
            } else if (!ALPHANUMERIC_PATTERN.matcher(token).matches()) {
                return false;
            }
        }

        return foundHelp;
    }

    /**
     * Extract output redirections from a command.
     */
    public static List<OutputRedirection> extractOutputRedirections(String command) {
        return extractOutputRedirectionsInternal(command).redirections();
    }

    /**
     * Extract output redirections with full result.
     */
    public static OutputRedirectionResult extractOutputRedirectionsFull(String command) {
        return extractOutputRedirectionsInternal(command);
    }

    /**
     * Remove output redirections from a command.
     */
    public static String removeOutputRedirections(String command) {
        return extractOutputRedirectionsInternal(command).commandWithoutRedirections();
    }

    /**
     * Check if a command is an unsafe compound command.
     * @deprecated Legacy path
     */
    @Deprecated
    public static boolean isUnsafeCompoundCommand(String command) {
        if (command == null) return true;

        HeredocUtils.HeredocExtractionResult result = HeredocUtils.extractHeredocs(command);
        ShellQuoteUtils.ShellParseResult parseResult = ShellQuoteUtils.tryParseShellCommand(result.processedCommand());

        if (!parseResult.success) return true;

        return splitCommandDeprecated(command).size() > 1 && !isCommandList(command);
    }

    // Private helper methods

    private static boolean isStaticRedirectTarget(String target) {
        if (target == null || target.isEmpty()) return false;
        if (target.matches(".*[\\s'\"].*")) return false;
        if (target.startsWith("#")) return false;

        return !target.startsWith("!") &&
               !target.startsWith("=") &&
               !target.contains("$") &&
               !target.contains("`") &&
               !target.contains("*") &&
               !target.contains("?") &&
               !target.contains("[") &&
               !target.contains("{") &&
               !target.contains("~") &&
               !target.contains("(") &&
               !target.contains("<") &&
               !target.startsWith("&");
    }

    private static String joinContinuationLines(String command) {
        // Simple implementation - remove backslash-newline sequences
        return command.replaceAll("\\\\\\n", "");
    }

    private static boolean isCommandList(String command) {
        Placeholders placeholders = generatePlaceholders();
        HeredocUtils.HeredocExtractionResult result = HeredocUtils.extractHeredocs(command);

        String withPlaceholders = addPlaceholders(result.processedCommand(), placeholders);
        ShellQuoteUtils.ShellParseResult parseResult = ShellQuoteUtils.tryParseShellCommand(withPlaceholders);

        if (!parseResult.success) return false;

        for (String part : parseResult.tokens) {
            // Check for unsafe operators (not handled)
            if (!COMMAND_LIST_SEPARATORS.contains(part) &&
                !ALL_CONTROL_OPERATORS.contains(part) &&
                !isOperatorSafe(part)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isOperatorSafe(String part) {
        // Strings, numbers are safe
        if (!part.matches("^[|&;><]+$")) return true;
        return COMMAND_LIST_SEPARATORS.contains(part) ||
               ">".equals(part) || ">>".equals(part) || ">&".equals(part);
    }

    private record Placeholders(
        String SINGLE_QUOTE,
        String DOUBLE_QUOTE,
        String NEW_LINE,
        String ESCAPED_OPEN_PAREN,
        String ESCAPED_CLOSE_PAREN
    ) {}

    private static Placeholders generatePlaceholders() {
        String salt = generateSalt();
        return new Placeholders(
            "__SINGLE_QUOTE_" + salt + "__",
            "__DOUBLE_QUOTE_" + salt + "__",
            "__NEW_LINE_" + salt + "__",
            "__ESCAPED_OPEN_PAREN_" + salt + "__",
            "__ESCAPED_CLOSE_PAREN_" + salt + "__"
        );
    }

    private static String generateSalt() {
        byte[] bytes = new byte[8];
        new java.security.SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(16);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String addPlaceholders(String command, Placeholders p) {
        return command
            .replace("\"", "\"" + p.DOUBLE_QUOTE)
            .replace("'", "'" + p.SINGLE_QUOTE)
            .replace("\n", "\n" + p.NEW_LINE + "\n")
            .replace("\\(", p.ESCAPED_OPEN_PAREN)
            .replace("\\)", p.ESCAPED_CLOSE_PAREN);
    }

    private static String removePlaceholders(String s, Placeholders p) {
        return s
            .replace(p.SINGLE_QUOTE, "'")
            .replace(p.DOUBLE_QUOTE, "\"")
            .replace("\n" + p.NEW_LINE + "\n", "\n")
            .replace(p.ESCAPED_OPEN_PAREN, "\\(")
            .replace(p.ESCAPED_CLOSE_PAREN, "\\)");
    }

    /**
     * Output redirection extraction result.
     */
    public record OutputRedirectionResult(
        String commandWithoutRedirections,
        List<OutputRedirection> redirections,
        boolean hasDangerousRedirection
    ) {
        public static OutputRedirectionResult empty(String original) {
            return new OutputRedirectionResult(original, Collections.emptyList(), false);
        }
    }

    private static OutputRedirectionResult extractOutputRedirectionsInternal(String cmd) {
        if (cmd == null || !cmd.contains(">")) {
            return OutputRedirectionResult.empty(cmd != null ? cmd : "");
        }

        List<OutputRedirection> redirections = new ArrayList<>();
        boolean hasDangerous = false;

        // Extract heredocs first
        HeredocUtils.HeredocExtractionResult heredocResult = HeredocUtils.extractHeredocs(cmd);
        String processed = joinContinuationLines(heredocResult.processedCommand());

        // Parse
        ShellQuoteUtils.ShellParseResult parseResult = ShellQuoteUtils.tryParseShellCommand(processed);

        if (!parseResult.success) {
            return new OutputRedirectionResult(cmd, Collections.emptyList(), true);
        }

        // Find redirections
        List<String> tokens = parseResult.tokens;
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);

            if (">".equals(token) || ">>".equals(token)) {
                String next = i < tokens.size() - 1 ? tokens.get(i + 1) : null;

                if (next != null && isStaticRedirectTarget(next)) {
                    redirections.add(new OutputRedirection(next, token));
                } else if (next != null && hasDangerousExpansion(next)) {
                    hasDangerous = true;
                }
            }
        }

        return new OutputRedirectionResult(cmd, redirections, hasDangerous);
    }

    private static boolean hasDangerousExpansion(String target) {
        if (target == null || target.isEmpty()) return false;

        return target.contains("$") ||
               target.contains("%") ||
               target.contains("`") ||
               target.contains("*") ||
               target.contains("?") ||
               target.contains("[") ||
               target.contains("{") ||
               target.startsWith("!") ||
               target.startsWith("=") ||
               target.startsWith("~");
    }
}