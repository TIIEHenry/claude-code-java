/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code JSON read utilities
 */
package com.anthropic.claudecode.utils.json;

/**
 * JSON read utilities including BOM stripping.
 */
public final class JsonRead {
    private JsonRead() {}

    private static final char UTF8_BOM = '\uFEFF';

    /**
     * Strip UTF-8 BOM from content.
     * PowerShell 5.x writes UTF-8 with BOM by default.
     */
    public static String stripBOM(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        return content.charAt(0) == UTF8_BOM ? content.substring(1) : content;
    }

    /**
     * Check if content starts with BOM.
     */
    public static boolean hasBOM(String content) {
        return content != null && !content.isEmpty() && content.charAt(0) == UTF8_BOM;
    }

    /**
     * Safe JSON parse with BOM stripping.
     */
    public static Object parseSafe(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        String stripped = stripBOM(content);
        return parseJson(stripped);
    }

    /**
     * Simple JSON parser (placeholder - would use Jackson/Gson).
     */
    private static Object parseJson(String json) {
        json = json.trim();

        if (json.isEmpty()) return null;
        if ("null".equals(json)) return null;
        if ("true".equals(json)) return true;
        if ("false".equals(json)) return false;

        // String
        if (json.startsWith("\"") && json.endsWith("\"")) {
            return unescapeJsonString(json.substring(1, json.length() - 1));
        }

        // Number
        try {
            if (json.contains(".")) {
                return Double.parseDouble(json);
            }
            return Long.parseLong(json);
        } catch (NumberFormatException e) {
            // Not a number
        }

        // Array
        if (json.startsWith("[") && json.endsWith("]")) {
            return parseJsonArray(json);
        }

        // Object
        if (json.startsWith("{") && json.endsWith("}")) {
            return parseJsonObject(json);
        }

        return json;
    }

    /**
     * Parse JSON array.
     */
    private static java.util.List<Object> parseJsonArray(String json) {
        java.util.List<Object> result = new java.util.ArrayList<>();
        String inner = json.substring(1, json.length() - 1).trim();

        if (inner.isEmpty()) {
            return result;
        }

        java.util.List<String> items = splitJsonItems(inner);
        for (String item : items) {
            result.add(parseJson(item.trim()));
        }

        return result;
    }

    /**
     * Parse JSON object.
     */
    private static java.util.Map<String, Object> parseJsonObject(String json) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        String inner = json.substring(1, json.length() - 1).trim();

        if (inner.isEmpty()) {
            return result;
        }

        java.util.List<String> items = splitJsonItems(inner);
        for (String item : items) {
            int colonIndex = findJsonColon(item);
            if (colonIndex > 0) {
                String key = item.substring(0, colonIndex).trim();
                String value = item.substring(colonIndex + 1).trim();

                // Remove quotes from key
                if (key.startsWith("\"") && key.endsWith("\"")) {
                    key = key.substring(1, key.length() - 1);
                }

                result.put(key, parseJson(value));
            }
        }

        return result;
    }

    /**
     * Split JSON items by comma respecting nesting.
     */
    private static java.util.List<String> splitJsonItems(String json) {
        java.util.List<String> items = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inString = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (inString) {
                current.append(c);
                if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
                current.append(c);
            } else if (c == '[' || c == '{') {
                depth++;
                current.append(c);
            } else if (c == ']' || c == '}') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                items.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        String last = current.toString().trim();
        if (!last.isEmpty()) {
            items.add(last);
        }

        return items;
    }

    /**
     * Find colon in JSON key-value pair.
     */
    private static int findJsonColon(String s) {
        boolean inString = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == ':') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Unescape JSON string.
     */
    private static String unescapeJsonString(String s) {
        StringBuilder result = new StringBuilder();
        boolean escape = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (escape) {
                switch (c) {
                    case 'n' -> result.append('\n');
                    case 't' -> result.append('\t');
                    case 'r' -> result.append('\r');
                    case '\\' -> result.append('\\');
                    case '"' -> result.append('"');
                    case '/' -> result.append('/');
                    case 'u' -> {
                        if (i + 4 < s.length()) {
                            String hex = s.substring(i + 1, i + 5);
                            try {
                                result.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException e) {
                                result.append(c);
                            }
                        } else {
                            result.append(c);
                        }
                    }
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
}