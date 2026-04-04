/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code YAML utilities
 */
package com.anthropic.claudecode.utils.parser;

import java.util.*;

/**
 * Simple YAML parser for frontmatter and configuration files.
 */
public final class YamlParser {
    private YamlParser() {}

    /**
     * Parse YAML string to a Map.
     */
    public static Map<String, Object> parse(String yaml) {
        if (yaml == null || yaml.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        String[] lines = yaml.split("\n");

        Deque<Map<String, Object>> stack = new ArrayDeque<>();
        stack.push(result);

        Deque<Integer> indents = new ArrayDeque<>();
        indents.push(-1);

        Deque<String> currentKeys = new ArrayDeque<>();
        currentKeys.push(null);

        for (String line : lines) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            int indent = countLeadingSpaces(line);
            String trimmed = line.trim();

            // Pop stack until we find the right level
            while (indent <= indents.peek() && stack.size() > 1) {
                stack.pop();
                indents.pop();
                currentKeys.pop();
            }

            // List item
            if (trimmed.startsWith("- ")) {
                String value = trimmed.substring(2).trim();
                String currentKey = currentKeys.peek();

                if (currentKey != null) {
                    Map<String, Object> current = stack.peek();
                    Object existing = current.get(currentKey);
                    if (existing instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (List<Object>) existing;
                        list.add(parseValue(value));
                    } else {
                        List<Object> list = new ArrayList<>();
                        list.add(parseValue(value));
                        current.put(currentKey, list);
                    }
                }
                continue;
            }

            // Key: value
            int colonIndex = trimmed.indexOf(':');
            if (colonIndex > 0) {
                String key = trimmed.substring(0, colonIndex).trim();
                String value = colonIndex < trimmed.length() - 1 ?
                        trimmed.substring(colonIndex + 1).trim() : null;

                Map<String, Object> current = stack.peek();

                if (value == null || value.isEmpty()) {
                    // Nested object
                    Map<String, Object> nested = new LinkedHashMap<>();
                    current.put(key, nested);
                    stack.push(nested);
                    indents.push(indent);
                    currentKeys.push(key);
                } else {
                    current.put(key, parseValue(value));
                }
            }
        }

        return result;
    }

    /**
     * Parse YAML value string to appropriate type.
     */
    private static Object parseValue(String value) {
        if (value == null || value.isEmpty() || "null".equals(value) || "~".equals(value)) {
            return null;
        }

        // Remove quotes
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return unescapeYamlString(value.substring(1, value.length() - 1));
        }

        // Boolean
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;

        // Number
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            long l = Long.parseLong(value);
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                return (int) l;
            }
            return l;
        } catch (NumberFormatException e) {
            // Not a number
        }

        // Inline array
        if (value.startsWith("[") && value.endsWith("]")) {
            String inner = value.substring(1, value.length() - 1);
            return parseInlineArray(inner);
        }

        // Inline object
        if (value.startsWith("{") && value.endsWith("}")) {
            String inner = value.substring(1, value.length() - 1);
            return parseInlineObject(inner);
        }

        return value;
    }

    /**
     * Parse inline YAML array.
     */
    private static List<Object> parseInlineArray(String inner) {
        List<Object> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);

            if (inQuotes) {
                if (c == quoteChar && (i == 0 || inner.charAt(i - 1) != '\\')) {
                    inQuotes = false;
                }
                current.append(c);
            } else if (c == '"' || c == '\'') {
                inQuotes = true;
                quoteChar = c;
                current.append(c);
            } else if (c == '[' || c == '{') {
                depth++;
                current.append(c);
            } else if (c == ']' || c == '}') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                String val = current.toString().trim();
                if (!val.isEmpty()) {
                    result.add(parseValue(val));
                }
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        String val = current.toString().trim();
        if (!val.isEmpty()) {
            result.add(parseValue(val));
        }

        return result;
    }

    /**
     * Parse inline YAML object.
     */
    private static Map<String, Object> parseInlineObject(String inner) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> pairs = splitByComma(inner);

        for (String pair : pairs) {
            int colonIndex = pair.indexOf(':');
            if (colonIndex > 0) {
                String key = pair.substring(0, colonIndex).trim();
                String value = colonIndex < pair.length() - 1 ?
                        pair.substring(colonIndex + 1).trim() : null;
                result.put(key, parseValue(value));
            }
        }

        return result;
    }

    /**
     * Split by comma respecting nesting and quotes.
     */
    private static List<String> splitByComma(String str) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (inQuotes) {
                if (c == quoteChar && (i == 0 || str.charAt(i - 1) != '\\')) {
                    inQuotes = false;
                }
                current.append(c);
            } else if (c == '"' || c == '\'') {
                inQuotes = true;
                quoteChar = c;
                current.append(c);
            } else if (c == '[' || c == '{') {
                depth++;
                current.append(c);
            } else if (c == ']' || c == '}') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        String val = current.toString().trim();
        if (!val.isEmpty()) {
            result.add(val);
        }

        return result;
    }

    /**
     * Unescape YAML string escape sequences.
     */
    private static String unescapeYamlString(String str) {
        StringBuilder result = new StringBuilder();
        boolean escape = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (escape) {
                switch (c) {
                    case 'n' -> result.append('\n');
                    case 't' -> result.append('\t');
                    case 'r' -> result.append('\r');
                    case '\\' -> result.append('\\');
                    case '"' -> result.append('"');
                    case '\'' -> result.append('\'');
                    default -> result.append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Count leading spaces in a line.
     */
    private static int countLeadingSpaces(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else if (c == '\t') count += 2; // Treat tab as 2 spaces
            else break;
        }
        return count;
    }

    /**
     * Convert a Map to YAML string.
     */
    public static String stringify(Map<String, Object> map) {
        return stringify(map, 0);
    }

    /**
     * Convert a Map to YAML string with indentation.
     */
    private static String stringify(Object value, int indent) {
        StringBuilder sb = new StringBuilder();
        String indentStr = "  ".repeat(indent);

        if (value == null) {
            return "null";
        }

        if (value instanceof String) {
            String str = (String) value;
            if (needsQuoting(str)) {
                return "\"" + escapeYamlString(str) + "\"";
            }
            return str;
        }

        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }

        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            for (Object item : list) {
                sb.append("\n").append(indentStr).append("- ");
                if (item instanceof Map) {
                    sb.append(stringify(item, indent + 1));
                } else {
                    sb.append(stringify(item, indent + 1));
                }
            }
            return sb.toString();
        }

        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                sb.append("\n").append(indentStr).append(entry.getKey()).append(":");
                Object v = entry.getValue();
                if (v instanceof Map || v instanceof List) {
                    sb.append(stringify(v, indent + 1));
                } else {
                    sb.append(" ").append(stringify(v, indent + 1));
                }
            }
            return sb.toString();
        }

        return value.toString();
    }

    /**
     * Check if a string needs quoting in YAML.
     */
    private static boolean needsQuoting(String str) {
        if (str.isEmpty()) return true;
        if (str.startsWith(" ") || str.endsWith(" ")) return true;

        // Special characters that require quoting
        return str.matches(".*[:{}\\[\\],&*#?|<>=!%@`].*") ||
               str.equals("true") || str.equals("false") || str.equals("null") ||
               str.startsWith("- ") || str.startsWith(": ");
    }

    /**
     * Escape a string for YAML.
     */
    private static String escapeYamlString(String str) {
        return str
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}