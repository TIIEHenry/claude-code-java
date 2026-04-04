/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code CLI module
 */
package com.anthropic.claudecode.cli;

import java.util.*;

/**
 * Command context for execution.
 */
public class CommandContext {

    private final String commandName;
    private final String[] args;
    private final CommandLineInterface cli;
    private final Map<String, Object> options;
    private final CommandLineInterface.CliConfig config;
    private final Map<String, Object> state;

    public CommandContext(String commandName, String[] args,
                          CommandLineInterface cli,
                          CommandLineInterface.CliConfig config) {
        this.commandName = commandName;
        this.args = args != null ? args : new String[0];
        this.cli = cli;
        this.options = new HashMap<>();
        this.config = config;
        this.state = new HashMap<>();
        parseOptions();
    }

    private void parseOptions() {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                String key = arg.substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    options.put(key, args[i + 1]);
                    i++;
                } else {
                    options.put(key, true);
                }
            } else if (arg.startsWith("-") && arg.length() > 1) {
                // Short option
                String key = arg.substring(1);
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    options.put(key, args[i + 1]);
                    i++;
                } else {
                    options.put(key, true);
                }
            }
        }
    }

    /**
     * Get command name.
     */
    public String commandName() {
        return commandName;
    }

    /**
     * Get arguments.
     */
    public String[] args() {
        return args.clone();
    }

    /**
     * Get argument count.
     */
    public int argCount() {
        return args.length;
    }

    /**
     * Get argument at index.
     */
    public String arg(int index) {
        return index < args.length ? args[index] : null;
    }

    /**
     * Get argument with default.
     */
    public String arg(int index, String defaultValue) {
        return index < args.length ? args[index] : defaultValue;
    }

    /**
     * Check if has argument.
     */
    public boolean hasArg(int index) {
        return index < args.length;
    }

    /**
     * Get all arguments after index.
     */
    public String[] argsFrom(int start) {
        if (start >= args.length) return new String[0];
        return Arrays.copyOfRange(args, start, args.length);
    }

    /**
     * Get arguments as list.
     */
    public List<String> argList() {
        return List.of(args);
    }

    /**
     * Get option.
     */
    public Object option(String name) {
        return options.get(name);
    }

    /**
     * Get option with type.
     */
    public <T> T option(String name, Class<T> type) {
        Object value = options.get(name);
        return value != null && type.isInstance(value) ? type.cast(value) : null;
    }

    /**
     * Get string option.
     */
    public String optionString(String name) {
        Object value = options.get(name);
        return value != null ? value.toString() : null;
    }

    /**
     * Get string option with default.
     */
    public String optionString(String name, String defaultValue) {
        Object value = options.get(name);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Get boolean option.
     */
    public boolean optionBoolean(String name) {
        Object value = options.get(name);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        return false;
    }

    /**
     * Get boolean option with default.
     */
    public boolean optionBoolean(String name, boolean defaultValue) {
        if (!options.containsKey(name)) return defaultValue;
        Object value = options.get(name);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        return defaultValue;
    }

    /**
     * Get int option.
     */
    public int optionInt(String name, int defaultValue) {
        Object value = options.get(name);
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Check if has option.
     */
    public boolean hasOption(String name) {
        return options.containsKey(name);
    }

    /**
     * Get all options.
     */
    public Map<String, Object> options() {
        return new HashMap<>(options);
    }

    /**
     * Get CLI instance.
     */
    public CommandLineInterface cli() {
        return cli;
    }

    /**
     * Get config.
     */
    public CommandLineInterface.CliConfig config() {
        return config;
    }

    /**
     * Set state value.
     */
    public void setState(String key, Object value) {
        state.put(key, value);
    }

    /**
     * Get state value.
     */
    public Object getState(String key) {
        return state.get(key);
    }

    /**
     * Get state with type.
     */
    public <T> T getState(String key, Class<T> type) {
        Object value = state.get(key);
        return value != null && type.isInstance(value) ? type.cast(value) : null;
    }

    /**
     * Check if has state.
     */
    public boolean hasState(String key) {
        return state.containsKey(key);
    }

    /**
     * Get all state.
     */
    public Map<String, Object> state() {
        return new HashMap<>(state);
    }
}