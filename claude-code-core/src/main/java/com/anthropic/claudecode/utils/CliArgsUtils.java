/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/cliArgs
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * CLI args utilities - Command line argument parsing.
 */
public final class CliArgsUtils {
    private final Map<String, String> options = new HashMap<>();
    private final List<String> positionalArgs = new ArrayList<>();
    private final Set<String> flags = new HashSet<>();

    /**
     * Parse command line arguments.
     */
    public static CliArgsUtils parse(String[] args) {
        CliArgsUtils result = new CliArgsUtils();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.startsWith("--")) {
                // Long option
                String option = arg.substring(2);
                if (option.contains("=")) {
                    int eqIndex = option.indexOf('=');
                    String key = option.substring(0, eqIndex);
                    String value = option.substring(eqIndex + 1);
                    result.options.put(key, value);
                } else if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    result.options.put(option, args[++i]);
                } else {
                    result.flags.add(option);
                }
            } else if (arg.startsWith("-")) {
                // Short option(s)
                String option = arg.substring(1);
                if (option.length() == 1) {
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        result.options.put(option, args[++i]);
                    } else {
                        result.flags.add(option);
                    }
                } else {
                    // Multiple short flags
                    for (char c : option.toCharArray()) {
                        result.flags.add(String.valueOf(c));
                    }
                }
            } else {
                // Positional argument
                result.positionalArgs.add(arg);
            }
        }

        return result;
    }

    /**
     * Get an option value.
     */
    public String getOption(String name) {
        return options.get(name);
    }

    /**
     * Get an option value with default.
     */
    public String getOption(String name, String defaultValue) {
        return options.getOrDefault(name, defaultValue);
    }

    /**
     * Get an option as integer.
     */
    public int getOptionAsInt(String name, int defaultValue) {
        String value = options.get(name);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get an option as boolean.
     */
    public boolean getOptionAsBool(String name, boolean defaultValue) {
        String value = options.get(name);
        if (value == null) return defaultValue;
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    /**
     * Check if a flag is set.
     */
    public boolean hasFlag(String name) {
        return flags.contains(name);
    }

    /**
     * Check if an option is present.
     */
    public boolean hasOption(String name) {
        return options.containsKey(name);
    }

    /**
     * Get positional arguments.
     */
    public List<String> getPositionalArgs() {
        return Collections.unmodifiableList(positionalArgs);
    }

    /**
     * Get positional argument at index.
     */
    public String getPositionalArg(int index) {
        if (index < 0 || index >= positionalArgs.size()) {
            return null;
        }
        return positionalArgs.get(index);
    }

    /**
     * Get positional argument at index with default.
     */
    public String getPositionalArg(int index, String defaultValue) {
        String value = getPositionalArg(index);
        return value != null ? value : defaultValue;
    }

    /**
     * Get all flags.
     */
    public Set<String> getFlags() {
        return Collections.unmodifiableSet(flags);
    }

    /**
     * Get all options.
     */
    public Map<String, String> getOptions() {
        return Collections.unmodifiableMap(options);
    }

    /**
     * Get argument count.
     */
    public int getArgCount() {
        return positionalArgs.size();
    }

    /**
     * Check if no arguments provided.
     */
    public boolean isEmpty() {
        return positionalArgs.isEmpty() && options.isEmpty() && flags.isEmpty();
    }

    @Override
    public String toString() {
        return "CliArgs{options=" + options + ", flags=" + flags + ", args=" + positionalArgs + "}";
    }
}