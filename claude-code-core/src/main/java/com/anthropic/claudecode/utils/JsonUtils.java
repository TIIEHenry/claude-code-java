/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/json.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;

/**
 * JSON parsing and manipulation utilities.
 */
public final class JsonUtils {
    private JsonUtils() {}

    /**
     * Parse JSON string to map.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }

        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return new HashMap<>();
        }

        return parseObject(json);
    }

    /**
     * Convert object to JSON string (alias for stringify).
     */
    public static String toJson(Object obj) {
        return stringify(obj);
    }

    /**
     * Parse JSON string to list.
     */
    @SuppressWarnings("unchecked")
    public static List<Object> parseArray(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }

        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) {
            return new ArrayList<>();
        }

        return parseArrayContent(json);
    }

    /**
     * Convert object to JSON string.
     */
    public static String stringify(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Boolean) return obj.toString();
        if (obj instanceof Number) return obj.toString();
        if (obj instanceof String) return quoteString((String) obj);
        if (obj instanceof Map) return stringifyMap((Map<?, ?>) obj);
        if (obj instanceof List) return stringifyList((List<?>) obj);
        return quoteString(obj.toString());
    }

    /**
     * Try to parse JSON, return null on failure.
     */
    public static Map<String, Object> tryParse(String json) {
        try {
            return parse(json);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if string is valid JSON.
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }

        json = json.trim();
        try {
            if (json.startsWith("{")) {
                parse(json);
                return true;
            } else if (json.startsWith("[")) {
                parseArray(json);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // Private helper methods

    private static Map<String, Object> parseObject(String json) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Remove outer braces
        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) return result;

        int depth = 0;
        boolean inString = false;
        StringBuilder current = new StringBuilder();
        String currentKey = null;
        char prevChar = '\0';

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && prevChar != '\\') {
                inString = !inString;
            }

            if (!inString) {
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
            }

            if (!inString && depth == 0) {
                if (c == ':') {
                    currentKey = unquoteString(current.toString().trim());
                    current = new StringBuilder();
                } else if (c == ',') {
                    if (currentKey != null) {
                        result.put(currentKey, parseValue(current.toString().trim()));
                    }
                    currentKey = null;
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            } else {
                current.append(c);
            }

            prevChar = c;
        }

        // Last key-value
        if (currentKey != null) {
            result.put(currentKey, parseValue(current.toString().trim()));
        }

        return result;
    }

    private static List<Object> parseArrayContent(String json) {
        List<Object> result = new ArrayList<>();

        // Remove outer brackets
        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) return result;

        int depth = 0;
        boolean inString = false;
        StringBuilder current = new StringBuilder();
        char prevChar = '\0';

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && prevChar != '\\') {
                inString = !inString;
            }

            if (!inString) {
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
            }

            if (!inString && depth == 0 && c == ',') {
                result.add(parseValue(current.toString().trim()));
                current = new StringBuilder();
            } else {
                current.append(c);
            }

            prevChar = c;
        }

        // Last element
        if (current.length() > 0) {
            result.add(parseValue(current.toString().trim()));
        }

        return result;
    }

    private static Object parseValue(String value) {
        if (value == null || value.isEmpty()) return null;

        value = value.trim();

        // String
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return unquoteString(value);
        }

        // Object
        if (value.startsWith("{")) {
            return parseObject(value);
        }

        // Array
        if (value.startsWith("[")) {
            return parseArrayContent(value);
        }

        // Boolean
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;

        // Null
        if ("null".equals(value)) return null;

        // Number
        try {
            if (value.contains(".") || value.contains("e") || value.contains("E")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private static String unquoteString(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        // Unescape basic sequences
        s = s.replace("\\\"", "\"")
             .replace("\\\\", "\\")
             .replace("\\n", "\n")
             .replace("\\t", "\t");
        return s;
    }

    private static String quoteString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                      .replace("\"", "\\\"")
                      .replace("\n", "\\n")
                      .replace("\t", "\\t") + "\"";
    }

    private static String stringifyMap(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append(quoteString(String.valueOf(entry.getKey())));
            sb.append(":");
            sb.append(stringify(entry.getValue()));
        }

        sb.append("}");
        return sb.toString();
    }

    private static String stringifyList(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;

        for (Object item : list) {
            if (!first) sb.append(",");
            first = false;
            sb.append(stringify(item));
        }

        sb.append("]");
        return sb.toString();
    }
}