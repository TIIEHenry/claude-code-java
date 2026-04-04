/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code YAML utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

/**
 * YAML parsing and dumping utilities.
 *
 * Uses SnakeYAML library for YAML processing.
 */
public final class YamlUtils {
    private YamlUtils() {}

    private static final Yaml YAML_INSTANCE = createDefaultYaml();

    /**
     * Create a default YAML instance with standard options.
     */
    private static Yaml createDefaultYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        return new Yaml(options);
    }

    /**
     * Parse YAML string to Object.
     */
    public static Object parse(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        return YAML_INSTANCE.load(input);
    }

    /**
     * Parse YAML string to Map.
     */
    public static Map<String, Object> parseMap(String input) {
        if (input == null || input.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Object result = YAML_INSTANCE.load(input);
        if (result instanceof Map) {
            return (Map<String, Object>) result;
        }
        return new LinkedHashMap<>();
    }

    /**
     * Parse YAML string to List.
     */
    public static List<Object> parseList(String input) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>();
        }
        Object result = YAML_INSTANCE.load(input);
        if (result instanceof List) {
            return (List<Object>) result;
        }
        return new ArrayList<>();
    }

    /**
     * Dump object to YAML string.
     */
    public static String dump(Object obj) {
        if (obj == null) {
            return "";
        }
        return YAML_INSTANCE.dump(obj);
    }

    /**
     * Dump object to YAML string with custom options.
     */
    public static String dump(Object obj, DumperOptions options) {
        if (obj == null) {
            return "";
        }
        Yaml yaml = new Yaml(options);
        return yaml.dump(obj);
    }

    /**
     * Dump multiple documents to YAML string.
     */
    public static String dumpAll(Iterable<Object> documents) {
        if (documents == null) {
            return "";
        }
        return YAML_INSTANCE.dumpAll(documents.iterator());
    }

    /**
     * Parse multiple YAML documents from a string.
     */
    public static Iterable<Object> loadAll(String input) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>();
        }
        return YAML_INSTANCE.loadAll(input);
    }

    /**
     * Create YAML options for pretty output.
     */
    public static DumperOptions prettyOptions() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setIndicatorIndent(2);
        options.setIndentWithIndicator(true);
        return options;
    }

    /**
     * Create YAML options for compact output.
     */
    public static DumperOptions compactOptions() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
        options.setPrettyFlow(false);
        return options;
    }

    /**
     * Parse YAML file from path.
     */
    public static Object parseFile(String path) {
        try {
            return YAML_INSTANCE.load(new java.io.FileReader(path));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse YAML file: " + path, e);
        }
    }

    /**
     * Parse YAML file to Map.
     */
    public static Map<String, Object> parseFileToMap(String path) {
        Object result = parseFile(path);
        if (result instanceof Map) {
            return (Map<String, Object>) result;
        }
        return new LinkedHashMap<>();
    }

    /**
     * Write object to YAML file.
     */
    public static void writeToFile(Object obj, String path) {
        try (java.io.FileWriter writer = new java.io.FileWriter(path)) {
            YAML_INSTANCE.dump(obj, writer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write YAML file: " + path, e);
        }
    }

    /**
     * Check if string is valid YAML.
     */
    public static boolean isValidYaml(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }
        try {
            YAML_INSTANCE.load(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}