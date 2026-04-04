/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code CLI args utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * CLI argument parsing utilities.
 */
public final class CliArgs {
    private CliArgs() {}

    /**
     * Parse a CLI flag value early, before normal argument processing.
     * Supports both space-separated (--flag value) and equals-separated (--flag=value).
     */
    public static String eagerParseCliFlag(String flagName, String[] argv) {
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];

            // Handle --flag=value syntax
            if (arg != null && arg.startsWith(flagName + "=")) {
                return arg.substring(flagName.length() + 1);
            }

            // Handle --flag value syntax
            if (arg != null && arg.equals(flagName) && i + 1 < argv.length) {
                return argv[i + 1];
            }
        }
        return null;
    }

    /**
     * Parse CLI flag from system args.
     */
    public static String eagerParseCliFlag(String flagName) {
        return eagerParseCliFlag(flagName, getSystemArgs());
    }

    /**
     * Handle the -- separator convention.
     */
    public static ExtractedArgs extractArgsAfterDoubleDash(String commandOrValue, String[] args) {
        if ("--".equals(commandOrValue) && args != null && args.length > 0) {
            String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);
            return new ExtractedArgs(args[0], remainingArgs);
        }
        return new ExtractedArgs(commandOrValue, args != null ? args : new String[0]);
    }

    /**
     * Check if a flag is present.
     */
    public static boolean hasFlag(String flagName, String[] argv) {
        for (String arg : argv) {
            if (arg != null && (arg.equals(flagName) || arg.startsWith(flagName + "="))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if flag is present in system args.
     */
    public static boolean hasFlag(String flagName) {
        return hasFlag(flagName, getSystemArgs());
    }

    /**
     * Get boolean flag value.
     */
    public static boolean getBooleanFlag(String flagName, String[] argv) {
        String value = eagerParseCliFlag(flagName, argv);
        if (value == null) return hasFlag(flagName, argv);
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    /**
     * Get integer flag value.
     */
    public static int getIntFlag(String flagName, String[] argv, int defaultValue) {
        String value = eagerParseCliFlag(flagName, argv);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Extracted args record.
     */
    public record ExtractedArgs(String command, String[] args) {}

    /**
     * Get system args (placeholder).
     */
    private static String[] getSystemArgs() {
        // In real implementation, would come from main args
        String argsEnv = System.getenv("CLAUDE_CLI_ARGS");
        if (argsEnv != null) {
            return argsEnv.split(" ");
        }
        return new String[0];
    }
}