/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code TOML utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

/**
 * Simple TOML parsing utilities.
 */
public final class TomlUtils {
    private TomlUtils() {}

    /**
     * Parse TOML content.
     */
    public static Map<String, Object> parse(String content) {
        TomlParser parser = new TomlParser();
        return parser.parse(content);
    }

    /**
     * Parse TOML file.
     */
    public static Map<String, Object> parseFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return parse(content.toString());
    }

    /**
     * Get string value.
     */
    @SuppressWarnings("unchecked")
    public static Optional<String> getString(Map<String, Object> toml, String... path) {
        return getValue(toml, path).map(v -> (String) v);
    }

    /**
     * Get integer value.
     */
    @SuppressWarnings("unchecked")
    public static Optional<Integer> getInt(Map<String, Object> toml, String... path) {
        return getValue(toml, path).map(v -> ((Number) v).intValue());
    }

    /**
     * Get long value.
     */
    @SuppressWarnings("unchecked")
    public static Optional<Long> getLong(Map<String, Object> toml, String... path) {
        return getValue(toml, path).map(v -> ((Number) v).longValue());
    }

    /**
     * Get double value.
     */
    @SuppressWarnings("unchecked")
    public static Optional<Double> getDouble(Map<String, Object> toml, String... path) {
        return getValue(toml, path).map(v -> ((Number) v).doubleValue());
    }

    /**
     * Get boolean value.
     */
    @SuppressWarnings("unchecked")
    public static Optional<Boolean> getBoolean(Map<String, Object> toml, String... path) {
        return getValue(toml, path).map(v -> (Boolean) v);
    }

    /**
     * Get nested map.
     */
    @SuppressWarnings("unchecked")
    public static Optional<Map<String, Object>> getTable(Map<String, Object> toml, String... path) {
        return getValue(toml, path).map(v -> (Map<String, Object>) v);
    }

    /**
     * Get list.
     */
    @SuppressWarnings("unchecked")
    public static Optional<List<Object>> getList(Map<String, Object> toml, String... path) {
        return getValue(toml, path).map(v -> (List<Object>) v);
    }

    /**
     * Get value at path.
     */
    @SuppressWarnings("unchecked")
    private static Optional<Object> getValue(Map<String, Object> toml, String... path) {
        if (path.length == 0) return Optional.empty();

        Object current = toml;
        for (String key : path) {
            if (!(current instanceof Map)) return Optional.empty();
            current = ((Map<String, Object>) current).get(key);
            if (current == null) return Optional.empty();
        }
        return Optional.of(current);
    }

    /**
     * TOML parser.
     */
    private static final class TomlParser {
        private final Map<String, Object> result = new LinkedHashMap<>();
        private Map<String, Object> currentTable = result;
        private String currentTableName = "";

        public Map<String, Object> parse(String content) {
            List<String> lines = content.lines().toList();

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();

                // Skip empty and comments
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Table header
                if (line.startsWith("[")) {
                    if (line.startsWith("[[")) {
                        // Array of tables
                        parseArrayOfTables(line);
                    } else {
                        // Regular table
                        parseTableHeader(line);
                    }
                    continue;
                }

                // Key-value pair
                parseKeyValue(line);
            }

            return result;
        }

        private void parseTableHeader(String line) {
            int end = line.indexOf(']');
            if (end < 0) return;

            currentTableName = line.substring(1, end).trim();
            currentTable = getOrCreateTable(currentTableName);
        }

        private void parseArrayOfTables(String line) {
            int end = line.indexOf("]]");
            if (end < 0) return;

            String tableName = line.substring(2, end).trim();
            String[] parts = tableName.split("\\.");

            // Navigate to parent
            Map<String, Object> parent = result;
            for (int i = 0; i < parts.length - 1; i++) {
                parent = getOrCreateTable(parent, parts[i].trim());
            }

            // Get or create array
            String arrayName = parts[parts.length - 1].trim();
            List<Map<String, Object>> array = (List<Map<String, Object>>) parent.get(arrayName);
            if (array == null) {
                array = new ArrayList<>();
                parent.put(arrayName, array);
            }

            // Add new table to array
            currentTable = new LinkedHashMap<>();
            array.add(currentTable);
            currentTableName = tableName;
        }

        private Map<String, Object> getOrCreateTable(String path) {
            String[] parts = path.split("\\.");
            Map<String, Object> current = result;
            for (String part : parts) {
                current = getOrCreateTable(current, part.trim());
            }
            return current;
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> getOrCreateTable(Map<String, Object> parent, String name) {
            return (Map<String, Object>) parent.computeIfAbsent(name, k -> new LinkedHashMap<>());
        }

        private void parseKeyValue(String line) {
            int eqIndex = line.indexOf('=');
            if (eqIndex < 0) return;

            String key = line.substring(0, eqIndex).trim();
            String valueStr = line.substring(eqIndex + 1).trim();

            Object value = parseValue(valueStr);
            currentTable.put(key, value);
        }

        private Object parseValue(String valueStr) {
            // Remove comment
            int commentIndex = valueStr.indexOf('#');
            if (commentIndex >= 0) {
                // Check if # is inside string
                if (!valueStr.startsWith("\"") && !valueStr.startsWith("'")) {
                    valueStr = valueStr.substring(0, commentIndex).trim();
                }
            }

            // String
            if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
                return valueStr.substring(1, valueStr.length() - 1);
            }
            if (valueStr.startsWith("'") && valueStr.endsWith("'")) {
                return valueStr.substring(1, valueStr.length() - 1);
            }

            // Multiline string
            if (valueStr.startsWith("\"\"\"") && valueStr.endsWith("\"\"\"")) {
                return valueStr.substring(3, valueStr.length() - 3);
            }

            // Boolean
            if (valueStr.equals("true")) return true;
            if (valueStr.equals("false")) return false;

            // Array
            if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
                return parseArray(valueStr.substring(1, valueStr.length() - 1));
            }

            // Number
            try {
                if (valueStr.contains(".")) {
                    return Double.parseDouble(valueStr);
                }
                return Long.parseLong(valueStr);
            } catch (NumberFormatException e) {
                return valueStr;
            }
        }

        private List<Object> parseArray(String content) {
            List<Object> result = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inString = false;
            String stringChar = "";

            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);

                if (inString) {
                    current.append(c);
                    if (String.valueOf(c).equals(stringChar)) {
                        inString = false;
                    }
                } else if (c == '"' || c == '\'') {
                    inString = true;
                    stringChar = String.valueOf(c);
                    current.append(c);
                } else if (c == ',') {
                    String value = current.toString().trim();
                    if (!value.isEmpty()) {
                        result.add(parseValue(value));
                    }
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }

            String value = current.toString().trim();
            if (!value.isEmpty()) {
                result.add(parseValue(value));
            }

            return result;
        }
    }
}