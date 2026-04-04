/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/envExpansion
 */
package com.anthropic.claudecode.services.mcp;

import java.util.*;
import java.util.regex.*;

/**
 * Environment expansion - Expand environment variables in MCP config.
 */
public final class McpEnvExpansion {
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern SIMPLE_ENV_PATTERN = Pattern.compile("\\$([A-Za-z_][A-Za-z0-9_]*)");

    /**
     * Expand environment variables in string.
     */
    public static String expand(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;

        // Expand ${VAR} format
        Matcher matcher = ENV_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = getEnv(varName);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        result = sb.toString();

        // Expand $VAR format
        matcher = SIMPLE_ENV_PATTERN.matcher(result);
        sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = getEnv(varName);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Get environment variable with defaults.
     */
    private static String getEnv(String varSpec) {
        // Handle ${VAR:-default} format
        if (varSpec.contains(":-")) {
            String[] parts = varSpec.split(":-", 2);
            String varName = parts[0];
            String defaultValue = parts[1];
            String value = System.getenv(varName);
            return value != null ? value : defaultValue;
        }

        // Handle ${VAR:=default} format
        if (varSpec.contains(":=")) {
            String[] parts = varSpec.split(":=", 2);
            String varName = parts[0];
            String defaultValue = parts[1];
            String value = System.getenv(varName);
            if (value == null) {
                // Would set env in real implementation
                return defaultValue;
            }
            return value;
        }

        // Handle ${VAR:?error} format
        if (varSpec.contains(":?")) {
            String[] parts = varSpec.split(":\\?", 2);
            String varName = parts[0];
            String errorMessage = parts[1];
            String value = System.getenv(varName);
            if (value == null) {
                throw new IllegalArgumentException(varName + ": " + errorMessage);
            }
            return value;
        }

        // Simple variable
        String value = System.getenv(varSpec);
        return value != null ? value : "";
    }

    /**
     * Expand in map.
     */
    public static Map<String, String> expand(Map<String, String> input) {
        if (input == null) return Collections.emptyMap();

        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            result.put(entry.getKey(), expand(entry.getValue()));
        }
        return result;
    }

    /**
     * Expand in list.
     */
    public static List<String> expand(List<String> input) {
        if (input == null) return Collections.emptyList();

        List<String> result = new ArrayList<>();
        for (String item : input) {
            result.add(expand(item));
        }
        return result;
    }

    /**
     * Expand config object.
     */
    public static McpServerConfig expandConfig(McpServerConfig config) {
        if (config == null) return null;

        return new McpServerConfig(
            config.name(),
            expand(config.command()),
            expand(config.args()),
            expand(config.env()),
            config.cwd(),
            config.autoStart(),
            config.capabilities()
        );
    }

    /**
     * Check if string contains env variables.
     */
    public static boolean containsEnvVars(String input) {
        if (input == null) return false;
        return ENV_PATTERN.matcher(input).find() || SIMPLE_ENV_PATTERN.matcher(input).find();
    }

    /**
     * Extract env variable names.
     */
    public static Set<String> extractEnvVars(String input) {
        Set<String> vars = new HashSet<>();
        if (input == null) return vars;

        Matcher matcher = ENV_PATTERN.matcher(input);
        while (matcher.find()) {
            String varSpec = matcher.group(1);
            // Remove any modifiers
            String varName = varSpec.split("[:-]")[0];
            vars.add(varName);
        }

        matcher = SIMPLE_ENV_PATTERN.matcher(input);
        while (matcher.find()) {
            vars.add(matcher.group(1));
        }

        return vars;
    }

    /**
     * MCP server config record.
     */
    public record McpServerConfig(
        String name,
        String command,
        List<String> args,
        Map<String, String> env,
        String cwd,
        boolean autoStart,
        Set<String> capabilities
    ) {}
}