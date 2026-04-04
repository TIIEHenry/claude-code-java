/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bash/shellQuoting.ts
 */
package com.anthropic.claudecode.utils.bash;

import java.util.regex.*;

/**
 * Shell command quoting utilities with heredoc and multiline support.
 */
public final class ShellQuotingUtils {
    private ShellQuotingUtils() {}

    // Regex for heredoc patterns: <<EOF, <<'EOF', <<"EOF", <<-EOF, <<-'EOF', <<\EOF
    private static final Pattern HEREDOC_PATTERN = Pattern.compile("<<-?\\s*(?:(['\"]?)(\\w+)\\1|\\\\(\\w+))");

    // Regex for bit-shift operators (to exclude from heredoc detection)
    private static final Pattern BIT_SHIFT_PATTERN = Pattern.compile("\\d\\s*<<\\s*\\d");
    private static final Pattern ARITH_BIT_SHIFT = Pattern.compile("\\[\\[\\s*\\d+\\s*<<\\s*\\d+\\s*\\]\\]");
    private static final Pattern ARITH_EXPR_BIT_SHIFT = Pattern.compile("\\$\\(\\(.*<<.*\\)\\)");

    // Regex for multiline strings with actual newlines
    private static final Pattern SINGLE_QUOTE_MULTILINE = Pattern.compile("'(?:[^'\\\\]|\\\\.)*\\n(?:[^'\\\\]|\\\\.)*'");
    private static final Pattern DOUBLE_QUOTE_MULTILINE = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\\n(?:[^\"\\\\]|\\\\.)*\"");

    // Regex for stdin redirect
    private static final Pattern STDIN_REDIRECT_PATTERN = Pattern.compile("(?:^|[\\s;&|])<(?![<(])\\s*\\S+");

    // Regex for Windows null redirect
    private static final Pattern NUL_REDIRECT_PATTERN = Pattern.compile("(\\d?&?>+\\s*)[Nn][Uu][Ll](?=\\s|$|[|&;\\)\\n])");

    /**
     * Detect if a command contains a heredoc pattern.
     */
    public static boolean containsHeredoc(String command) {
        if (command == null || !command.contains("<<")) {
            return false;
        }

        // Exclude bit-shift operators
        if (BIT_SHIFT_PATTERN.matcher(command).find() ||
            ARITH_BIT_SHIFT.matcher(command).find() ||
            ARITH_EXPR_BIT_SHIFT.matcher(command).find()) {
            return false;
        }

        return HEREDOC_PATTERN.matcher(command).find();
    }

    /**
     * Detect if a command contains multiline strings in quotes.
     */
    public static boolean containsMultilineString(String command) {
        if (command == null) {
            return false;
        }
        return SINGLE_QUOTE_MULTILINE.matcher(command).find() ||
               DOUBLE_QUOTE_MULTILINE.matcher(command).find();
    }

    /**
     * Quote a shell command appropriately, preserving heredocs and multiline strings.
     */
    public static String quoteShellCommand(String command) {
        return quoteShellCommand(command, true);
    }

    /**
     * Quote a shell command appropriately.
     * @param command The command to quote
     * @param addStdinRedirect Whether to add < /dev/null
     */
    public static String quoteShellCommand(String command, boolean addStdinRedirect) {
        if (command == null || command.isEmpty()) {
            return addStdinRedirect ? "'' < /dev/null" : "''";
        }

        // Handle heredocs and multiline strings specially
        if (containsHeredoc(command) || containsMultilineString(command)) {
            // Use single quotes and escape only single quotes
            String escaped = command.replace("'", "'\"'\"'");
            String quoted = "'" + escaped + "'";

            // Don't add stdin redirect for heredocs
            if (containsHeredoc(command)) {
                return quoted;
            }

            return addStdinRedirect ? quoted + " < /dev/null" : quoted;
        }

        // For regular commands, use shell-quote style quoting
        if (addStdinRedirect) {
            return ShellQuoteUtils.quote(command, "<", "/dev/null");
        }

        return ShellQuoteUtils.quote(command);
    }

    /**
     * Check if a command has stdin redirect.
     */
    public static boolean hasStdinRedirect(String command) {
        if (command == null) {
            return false;
        }
        return STDIN_REDIRECT_PATTERN.matcher(command).find();
    }

    /**
     * Check if stdin redirect should be added.
     */
    public static boolean shouldAddStdinRedirect(String command) {
        if (command == null) {
            return true;
        }

        // Don't add for heredocs
        if (containsHeredoc(command)) {
            return false;
        }

        // Don't add if already has one
        if (hasStdinRedirect(command)) {
            return false;
        }

        return true;
    }

    /**
     * Rewrite Windows CMD-style >nul redirects to POSIX /dev/null.
     */
    public static String rewriteWindowsNullRedirect(String command) {
        if (command == null) {
            return null;
        }
        return NUL_REDIRECT_PATTERN.matcher(command).replaceAll("$1/dev/null");
    }

    /**
     * Check if a command contains shell-quote single quote bug pattern.
     * This detects patterns like '\' that exploit shell-quote library's
     * incorrect handling of backslashes inside single quotes.
     */
    public static boolean hasShellQuoteSingleQuoteBug(String command) {
        if (command == null || !command.contains("'")) {
            return false;
        }

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            // Handle backslash escaping outside single quotes
            if (c == '\\' && !inSingleQuote) {
                i++; // Skip next char
                continue;
            }

            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;

                // Check trailing backslashes when closing single quote
                if (!inSingleQuote) {
                    int backslashCount = 0;
                    int j = i - 1;
                    while (j >= 0 && command.charAt(j) == '\\') {
                        backslashCount++;
                        j--;
                    }

                    // Odd trailing backslashes = always a bug
                    if (backslashCount > 0 && backslashCount % 2 == 1) {
                        return true;
                    }

                    // Even trailing backslashes with later ' = bug
                    if (backslashCount > 0 && backslashCount % 2 == 0) {
                        if (command.indexOf("'", i + 1) != -1) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}