/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bash/shellQuote.ts
 */
package com.anthropic.claudecode.utils.bash;

import java.util.*;

/**
 * Shell quoting utilities for safe command execution.
 */
public final class ShellQuoteUtils {
    private ShellQuoteUtils() {}

    /**
     * Quote a string for shell execution.
     */
    public static String quote(String arg) {
        if (arg == null || arg.isEmpty()) {
            return "''";
        }

        // If the string is safe (only alphanumeric and safe chars), no quoting needed
        if (isSafe(arg)) {
            return arg;
        }

        // Use single quotes, escaping any single quotes within
        return "'" + arg.replace("'", "'\"'\"'") + "'";
    }

    /**
     * Quote multiple arguments for shell execution.
     */
    public static String quote(List<String> args) {
        if (args == null || args.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(quote(args.get(i)));
        }
        return sb.toString();
    }

    /**
     * Quote an array of arguments.
     */
    public static String quote(String... args) {
        return quote(Arrays.asList(args));
    }

    /**
     * Check if a string is safe to use without quoting.
     */
    public static boolean isSafe(String arg) {
        if (arg == null || arg.isEmpty()) {
            return false;
        }

        // Safe characters: alphanumeric, dash, underscore, dot, slash, colon, at sign
        for (char c : arg.toCharArray()) {
            if (!Character.isLetterOrDigit(c) &&
                c != '-' && c != '_' && c != '.' && c != '/' && c != ':' && c != '@') {
                return false;
            }
        }
        return true;
    }

    /**
     * Escape a string for use in double quotes.
     */
    public static String escapeForDoubleQuotes(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("$", "\\$")
                .replace("`", "\\`");
    }

    /**
     * Escape a string for use in single quotes.
     * Note: Single quotes cannot contain single quotes in bash.
     */
    public static String escapeForSingleQuotes(String s) {
        if (s == null) return "";
        // In single quotes, only ' needs special handling
        // We terminate the quote, add an escaped ', and start a new quote
        return s.replace("'", "'\"'\"'");
    }

    /**
     * Unquote a shell-quoted string.
     */
    public static String unquote(String quoted) {
        if (quoted == null || quoted.isEmpty()) {
            return quoted;
        }

        // Handle single quotes
        if (quoted.startsWith("'") && quoted.endsWith("'")) {
            return quoted.substring(1, quoted.length() - 1)
                    .replace("'\"'\"'", "'");
        }

        // Handle double quotes (basic unescaping)
        if (quoted.startsWith("\"") && quoted.endsWith("\"")) {
            String inner = quoted.substring(1, quoted.length() - 1);
            return unescapeDoubleQuoted(inner);
        }

        return quoted;
    }

    /**
     * Unescape a double-quoted string.
     */
    private static String unescapeDoubleQuoted(String s) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (escaped) {
                switch (c) {
                    case 'n': result.append('\n'); break;
                    case 't': result.append('\t'); break;
                    case 'r': result.append('\r'); break;
                    case '\\': result.append('\\'); break;
                    case '"': result.append('"'); break;
                    case '$': result.append('$'); break;
                    case '`': result.append('`'); break;
                    default: result.append(c); break;
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Result of shell parse operation.
     */
    public static class ShellParseResult {
        public final boolean success;
        public final List<String> tokens;
        public final String error;

        private ShellParseResult(boolean success, List<String> tokens, String error) {
            this.success = success;
            this.tokens = tokens;
            this.error = error;
        }

        public static ShellParseResult success(List<String> tokens) {
            return new ShellParseResult(true, tokens, null);
        }

        public static ShellParseResult failure(String error) {
            return new ShellParseResult(false, Collections.emptyList(), error);
        }
    }

    /**
     * Result of shell quote operation.
     */
    public static class ShellQuoteResult {
        public final boolean success;
        public final String quoted;
        public final String error;

        private ShellQuoteResult(boolean success, String quoted, String error) {
            this.success = success;
            this.quoted = quoted;
            this.error = error;
        }

        public static ShellQuoteResult success(String quoted) {
            return new ShellQuoteResult(true, quoted, null);
        }

        public static ShellQuoteResult failure(String error) {
            return new ShellQuoteResult(false, null, error);
        }
    }

    /**
     * Try to parse a shell command.
     */
    public static ShellParseResult tryParseShellCommand(String cmd) {
        if (cmd == null || cmd.isEmpty()) {
            return ShellParseResult.success(Collections.emptyList());
        }

        try {
            List<String> tokens = parseCommand(cmd);
            return ShellParseResult.success(tokens);
        } catch (Exception e) {
            return ShellParseResult.failure(e.getMessage());
        }
    }

    /**
     * Parse a shell command into tokens.
     * Basic implementation - handles quotes and escapes.
     */
    public static List<String> parseCommand(String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaped = false;

        for (int i = 0; i < cmd.length(); i++) {
            char c = cmd.charAt(i);

            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\' && !inSingleQuote) {
                escaped = true;
                continue;
            }

            if (c == '\'' && !inDoubleQuote) {
                if (inSingleQuote) {
                    // End single quote
                    tokens.add(current.toString());
                    current = new StringBuilder();
                    inSingleQuote = false;
                } else {
                    // Start single quote
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                        current = new StringBuilder();
                    }
                    inSingleQuote = true;
                }
                continue;
            }

            if (c == '"' && !inSingleQuote) {
                if (inDoubleQuote) {
                    // End double quote
                    tokens.add(current.toString());
                    current = new StringBuilder();
                    inDoubleQuote = false;
                } else {
                    // Start double quote
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                        current = new StringBuilder();
                    }
                    inDoubleQuote = true;
                }
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
                continue;
            }

            current.append(c);
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    /**
     * Try to quote shell arguments with validation.
     */
    public static ShellQuoteResult tryQuoteShellArgs(List<Object> args) {
        if (args == null || args.isEmpty()) {
            return ShellQuoteResult.success("");
        }

        try {
            List<String> stringArgs = new ArrayList<>();
            for (int i = 0; i < args.size(); i++) {
                Object arg = args.get(i);
                if (arg == null) {
                    stringArgs.add("null");
                } else if (arg instanceof String) {
                    stringArgs.add((String) arg);
                } else if (arg instanceof Number || arg instanceof Boolean) {
                    stringArgs.add(arg.toString());
                } else {
                    throw new IllegalArgumentException(
                        "Cannot quote argument at index " + i +
                        ": unsupported type " + arg.getClass().getName());
                }
            }
            return ShellQuoteResult.success(quote(stringArgs));
        } catch (Exception e) {
            return ShellQuoteResult.failure(e.getMessage());
        }
    }

    /**
     * Check if parsed tokens contain malformed entries.
     */
    public static boolean hasMalformedTokens(String command, List<String> parsed) {
        if (command == null || parsed == null) {
            return false;
        }

        // Check for unterminated quotes in original command
        int singleCount = 0;
        int doubleCount = 0;
        boolean escaped = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (c == '\\' && !escaped) {
                escaped = true;
                continue;
            }
            if (c == '\'' && !escaped) singleCount++;
            if (c == '"' && !escaped) doubleCount++;
            escaped = false;
        }

        if (singleCount % 2 != 0 || doubleCount % 2 != 0) {
            return true;
        }

        // Check each token for balanced brackets/braces
        for (String token : parsed) {
            if (!isBalanced(token, '{', '}')) return true;
            if (!isBalanced(token, '(', ')')) return true;
            if (!isBalanced(token, '[', ']')) return true;
        }

        return false;
    }

    private static boolean isBalanced(String s, char open, char close) {
        int count = 0;
        for (char c : s.toCharArray()) {
            if (c == open) count++;
            if (c == close) count--;
        }
        return count == 0;
    }
}