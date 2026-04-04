/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bash/shellQuote
 */
package com.anthropic.claudecode.utils.bash;

import java.util.*;

/**
 * Shell quote - Shell quoting utilities.
 */
public final class ShellQuote {
    private static final Set<Character> SPECIAL_CHARS = Set.of(
        Character.valueOf(' '), Character.valueOf('\t'), Character.valueOf('\n'),
        Character.valueOf('\r'), Character.valueOf('"'), Character.valueOf('\''),
        Character.valueOf('\\'), Character.valueOf('`'), Character.valueOf('$'),
        Character.valueOf('!'), Character.valueOf('&'), Character.valueOf('|'),
        Character.valueOf(';'), Character.valueOf('<'), Character.valueOf('>'),
        Character.valueOf('('), Character.valueOf(')'), Character.valueOf('{'),
        Character.valueOf('}'), Character.valueOf('*'), Character.valueOf('?'),
        Character.valueOf('['), Character.valueOf(']'), Character.valueOf('#'),
        Character.valueOf('~'), Character.valueOf('^')
    );

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

        // Use single quotes if no single quotes in string
        if (!str.contains("'")) {
            return "'" + str + "'";
        }

        // Use double quotes for strings with single quotes
        return quoteDouble(str);
    }

    /**
     * Quote with double quotes.
     */
    public static String quoteDouble(String str) {
        if (str == null) return "\"\"";
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"', '\\', '$', '`' -> sb.append('\\').append(c);
                default -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Quote with single quotes.
     */
    public static String quoteSingle(String str) {
        if (str == null) return "''";
        if (!str.contains("'")) {
            return "'" + str + "'";
        }
        // Need to escape single quotes
        return "'" + str.replace("'", "'\"'\"'") + "'";
    }

    /**
     * Check if string needs quoting.
     */
    public static boolean needsQuoting(String str) {
        if (str == null || str.isEmpty()) return false;
        for (char c : str.toCharArray()) {
            if (SPECIAL_CHARS.contains(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Unquote string.
     */
    public static String unquote(String str) {
        if (str == null) return null;

        // Single quoted
        if (str.startsWith("'") && str.endsWith("'") && str.length() >= 2) {
            return str.substring(1, str.length() - 1);
        }

        // Double quoted
        if (str.startsWith("\"") && str.endsWith("\"") && str.length() >= 2) {
            StringBuilder sb = new StringBuilder();
            String inner = str.substring(1, str.length() - 1);
            boolean escaped = false;
            for (char c : inner.toCharArray()) {
                if (escaped) {
                    sb.append(c);
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        return str;
    }

    /**
     * Quote for specific shell type.
     */
    public static String quoteForShell(String str, ShellType shellType) {
        return switch (shellType) {
            case ZSH, BASH -> quote(str);
            case FISH -> quoteFish(str);
            case SH -> quoteSingle(str);
            default -> quote(str);
        };
    }

    /**
     * Quote for fish shell.
     */
    private static String quoteFish(String str) {
        if (str == null || str.isEmpty()) return "''";
        if (!needsQuoting(str)) return str;

        StringBuilder sb = new StringBuilder();
        sb.append("'");
        for (char c : str.toCharArray()) {
            if (c == '\'') {
                sb.append("\\'");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else {
                sb.append(c);
            }
        }
        sb.append("'");
        return sb.toString();
    }

    /**
     * Shell type enum.
     */
    public enum ShellType {
        ZSH, BASH, FISH, SH, UNKNOWN
    }

    /**
     * Quote array of strings.
     */
    public static String[] quoteAll(String[] strs) {
        if (strs == null) return new String[0];
        return Arrays.stream(strs)
            .map(ShellQuote::quote)
            .toArray(String[]::new);
    }

    /**
     * Build command from arguments.
     */
    public static String buildCommand(String... args) {
        if (args == null || args.length == 0) return "";
        return String.join(" ", quoteAll(args));
    }
}