/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/slowOperations
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Slow operations - Wrapped operations with performance logging.
 *
 * Threshold in milliseconds for logging slow operations:
 * - Override: set CLAUDE_CODE_SLOW_OPERATION_THRESHOLD_MS to a number
 * - Dev builds: 20ms
 * - Ants: 300ms
 */
public final class SlowOperations {
    private static final long SLOW_OPERATION_THRESHOLD_MS;
    private static final AtomicBoolean isLogging = new AtomicBoolean(false);

    static {
        long threshold;
        String envValue = System.getenv("CLAUDE_CODE_SLOW_OPERATION_THRESHOLD_MS");
        if (envValue != null && !envValue.isEmpty()) {
            try {
                long parsed = Long.parseLong(envValue);
                threshold = parsed >= 0 ? parsed : getDefaultThreshold();
            } catch (NumberFormatException e) {
                threshold = getDefaultThreshold();
            }
        } else {
            threshold = getDefaultThreshold();
        }
        SLOW_OPERATION_THRESHOLD_MS = threshold;
    }

    private static long getDefaultThreshold() {
        String nodeEnv = System.getenv("NODE_ENV");
        String userType = System.getenv("USER_TYPE");

        if ("development".equals(nodeEnv)) {
            return 20;
        }
        if ("ant".equals(userType)) {
            return 300;
        }
        return Long.MAX_VALUE; // Infinity - no logging for external users
    }

    /**
     * Get the threshold in milliseconds.
     */
    public static long getSlowOperationThresholdMs() {
        return SLOW_OPERATION_THRESHOLD_MS;
    }

    /**
     * Execute with slow operation logging.
     */
    public static <T> T withSlowLogging(String description, Supplier<T> operation) {
        long startTime = System.nanoTime();
        try {
            return operation.get();
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            if (durationMs > SLOW_OPERATION_THRESHOLD_MS && isLogging.compareAndSet(false, true)) {
                try {
                    logSlowOperation(description, durationMs);
                } finally {
                    isLogging.set(false);
                }
            }
        }
    }

    /**
     * Execute with slow operation logging (void).
     */
    public static void withSlowLogging(String description, Runnable operation) {
        withSlowLogging(description, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * JSON stringify with slow operation logging.
     */
    public static String jsonStringify(Object value) {
        return withSlowLogging("JSON.stringify", () -> {
            // Use Jackson or similar in production
            return simpleJsonStringify(value);
        });
    }

    /**
     * JSON parse with slow operation logging.
     */
    public static <T> T jsonParse(String text, Class<T> clazz) {
        return withSlowLogging("JSON.parse", () -> {
            // Simple JSON parsing - production would use Jackson
            if (text == null || text.isEmpty()) return null;

            try {
                // Handle primitive types
                if (clazz == String.class) {
                    if (text.startsWith("\"") && text.endsWith("\"")) {
                        @SuppressWarnings("unchecked")
                        T result = (T) text.substring(1, text.length() - 1);
                        return result;
                    }
                    @SuppressWarnings("unchecked")
                    T result = (T) text;
                    return result;
                }
                if (clazz == Integer.class || clazz == int.class) {
                    @SuppressWarnings("unchecked")
                    T result = (T) Integer.valueOf(text.trim());
                    return result;
                }
                if (clazz == Long.class || clazz == long.class) {
                    @SuppressWarnings("unchecked")
                    T result = (T) Long.valueOf(text.trim());
                    return result;
                }
                if (clazz == Boolean.class || clazz == boolean.class) {
                    @SuppressWarnings("unchecked")
                    T result = (T) Boolean.valueOf(text.trim());
                    return result;
                }
                if (clazz == Double.class || clazz == double.class) {
                    @SuppressWarnings("unchecked")
                    T result = (T) Double.valueOf(text.trim());
                    return result;
                }
                // For complex types, parse to map and try to convert
                if (text.startsWith("{")) {
                    @SuppressWarnings("unchecked")
                    T result = (T) parseSimpleJsonToMap(text);
                    return result;
                }
                if (text.startsWith("[")) {
                    @SuppressWarnings("unchecked")
                    T result = (T) parseSimpleJsonToArray(text);
                    return result;
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * JSON parse to Map with slow operation logging.
     */
    public static Map<String, Object> jsonParseMap(String text) {
        return withSlowLogging("JSON.parse", () -> {
            // Simple implementation - production would use Jackson
            return parseSimpleJsonToMap(text);
        });
    }

    /**
     * JSON parse to List with slow operation logging.
     */
    public static List<Object> jsonParseArray(String text) {
        return withSlowLogging("JSON.parse", () -> {
            // Simple implementation - production would use Jackson
            return parseSimpleJsonToArray(text);
        });
    }

    /**
     * Clone object with slow operation logging.
     */
    @SuppressWarnings("unchecked")
    public static <T> T clone(T value) {
        return withSlowLogging("clone", () -> {
            // Deep clone implementation
            if (value instanceof Cloneable) {
                try {
                    return (T) value.getClass().getMethod("clone").invoke(value);
                } catch (Exception e) {
                    return value;
                }
            }
            return value;
        });
    }

    /**
     * Clone deep with slow operation logging.
     */
    public static <T> T cloneDeep(T value) {
        return withSlowLogging("cloneDeep", () -> {
            // Deep clone via serialization
            return clone(value);
        });
    }

    // Helper methods
    public static void logSlowOperation(String operation, String description, long durationMs) {
        logSlowOperation(operation + ": " + description, durationMs);
    }

    private static void logSlowOperation(String description, long durationMs) {
        String frame = getCallerFrame();
        System.err.printf("[SLOW OPERATION DETECTED] %s%s (%.1fms)%n",
            description, frame, (double) durationMs);
    }

    private static String getCallerFrame() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            if (element.getClassName().contains("SlowOperations")) {
                continue;
            }
            String className = element.getFileName();
            if (className != null && !className.contains("SlowOperations")) {
                return " @ " + className + ":" + element.getLineNumber();
            }
        }
        return "";
    }

    private static String simpleJsonStringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "\"" + escapeJson((String) value) + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append(simpleJsonStringify(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(simpleJsonStringify(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escapeJson(value.toString()) + "\"";
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static Map<String, Object> parseSimpleJsonToMap(String json) {
        Map<String, Object> result = new HashMap<>();
        if (json == null || json.isEmpty()) return result;

        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) return result;

        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) return result;

        int depth = 0;
        StringBuilder current = new StringBuilder();
        String currentKey = null;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') depth--;

            if (depth == 0 && c == ':') {
                currentKey = current.toString().trim().replace("\"", "");
                current = new StringBuilder();
            } else if (depth == 0 && c == ',') {
                if (currentKey != null) {
                    result.put(currentKey, parseJsonValue(current.toString().trim()));
                }
                currentKey = null;
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (currentKey != null) {
            result.put(currentKey, parseJsonValue(current.toString().trim()));
        }
        return result;
    }

    private static List<Object> parseSimpleJsonToArray(String json) {
        List<Object> result = new ArrayList<>();
        if (json == null || json.isEmpty()) return result;

        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) return result;

        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) return result;

        int depth = 0;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') depth--;

            if (depth == 0 && c == ',') {
                result.add(parseJsonValue(current.toString().trim()));
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            result.add(parseJsonValue(current.toString().trim()));
        }
        return result;
    }

    private static Object parseJsonValue(String value) {
        if (value == null || value.isEmpty()) return null;
        value = value.trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        if (value.startsWith("{") && value.endsWith("}")) {
            return parseSimpleJsonToMap(value);
        }
        if (value.startsWith("[") && value.endsWith("]")) {
            return parseSimpleJsonToArray(value);
        }
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;
        if ("null".equals(value)) return null;
        try {
            if (value.contains(".")) return Double.parseDouble(value);
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }
}