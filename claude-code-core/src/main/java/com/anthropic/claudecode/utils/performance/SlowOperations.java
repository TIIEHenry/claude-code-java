/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code slow operation logging
 */
package com.anthropic.claudecode.utils.performance;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Slow operation logging infrastructure.
 *
 * Threshold in milliseconds for logging slow JSON/clone operations.
 * Operations taking longer than this will be logged for debugging.
 */
public final class SlowOperations {
    private SlowOperations() {}

    private static final AtomicLong SLOW_OPERATION_THRESHOLD_MS = new AtomicLong(computeThreshold());
    private static final AtomicBoolean isLogging = new AtomicBoolean(false);
    private static final List<SlowOperationRecord> slowOperations = new CopyOnWriteArrayList<>();

    /**
     * Compute the slow operation threshold from environment.
     */
    private static long computeThreshold() {
        String envValue = System.getenv("CLAUDE_CODE_SLOW_OPERATION_THRESHOLD_MS");
        if (envValue != null) {
            try {
                long parsed = Long.parseLong(envValue);
                if (parsed >= 0) {
                    return parsed;
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        String nodeEnv = System.getenv("NODE_ENV");
        if ("development".equals(nodeEnv)) {
            return 20;
        }

        String userType = System.getenv("USER_TYPE");
        if ("ant".equals(userType)) {
            return 300;
        }

        return Long.MAX_VALUE; // Infinity in Java
    }

    /**
     * Get the current threshold.
     */
    public static long getThresholdMs() {
        return SLOW_OPERATION_THRESHOLD_MS.get();
    }

    /**
     * Set a custom threshold.
     */
    public static void setThresholdMs(long threshold) {
        SLOW_OPERATION_THRESHOLD_MS.set(threshold);
    }

    /**
     * Record of a slow operation.
     */
    public record SlowOperationRecord(
            String description,
            long durationMs,
            Instant timestamp,
            String callerFrame
    ) {}

    /**
     * Log a slow operation.
     */
    public static void addSlowOperation(String description, long durationMs) {
        String callerFrame = extractCallerFrame();
        slowOperations.add(new SlowOperationRecord(
                description,
                durationMs,
                Instant.now(),
                callerFrame
        ));
    }

    /**
     * Extract the caller stack frame.
     */
    private static String extractCallerFrame() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            if (element.getClassName().contains("SlowOperations")) continue;
            String fileName = element.getFileName();
            int lineNumber = element.getLineNumber();
            if (fileName != null && lineNumber > 0) {
                return " @ " + fileName + ":" + lineNumber;
            }
        }
        return "";
    }

    /**
     * Build a description from a value.
     */
    public static String buildDescription(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof List) {
            return "Array[" + ((List<?>) value).size() + "]";
        }

        if (value instanceof Map) {
            return "Object{" + ((Map<?, ?>) value).keySet().size() + " keys}";
        }

        if (value instanceof String) {
            String str = (String) value;
            return str.length() > 80 ? str.substring(0, 80) + "…" : str;
        }

        return value.toString();
    }

    /**
     * Time an operation and log if slow.
     */
    public static <T> T timeOperation(String description, Supplier<T> operation) {
        long startTime = System.nanoTime();
        try {
            return operation.get();
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            if (durationMs > SLOW_OPERATION_THRESHOLD_MS.get() && !isLogging.get()) {
                isLogging.set(true);
                try {
                    addSlowOperation(description, durationMs);
                } finally {
                    isLogging.set(false);
                }
            }
        }
    }

    /**
     * Time a void operation and log if slow.
     */
    public static void timeOperationVoid(String description, Runnable operation) {
        long startTime = System.nanoTime();
        try {
            operation.run();
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            if (durationMs > SLOW_OPERATION_THRESHOLD_MS.get() && !isLogging.get()) {
                isLogging.set(true);
                try {
                    addSlowOperation(description, durationMs);
                } finally {
                    isLogging.set(false);
                }
            }
        }
    }

    /**
     * Wrapped JSON stringify with slow operation logging.
     */
    public static String jsonStringify(Object value) {
        return timeOperation("JSON.stringify(" + buildDescription(value) + ")", () -> {
            return JsonUtils.toJson(value);
        });
    }

    /**
     * Wrapped JSON stringify with pretty printing.
     */
    public static String jsonStringify(Object value, boolean pretty) {
        return timeOperation("JSON.stringify(" + buildDescription(value) + ")", () -> {
            return pretty ? JsonUtils.toPrettyJson(value) : JsonUtils.toJson(value);
        });
    }

    /**
     * Wrapped JSON parse with slow operation logging.
     */
    public static <T> T jsonParse(String text, Class<T> type) {
        return timeOperation("JSON.parse(" + buildDescription(text) + ")", () -> {
            return JsonUtils.fromJson(text, type);
        });
    }

    /**
     * Clone an object with slow operation logging.
     */
    public static <T> T clone(T value) {
        return timeOperation("clone(" + buildDescription(value) + ")", () -> {
            return deepClone(value);
        });
    }

    /**
     * Deep clone implementation.
     */
    @SuppressWarnings("unchecked")
    private static <T> T deepClone(T value) {
        if (value == null) return null;

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<Object> cloned = new ArrayList<>(list.size());
            for (Object item : list) {
                cloned.add(deepClone(item));
            }
            return (T) cloned;
        }

        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            Map<Object, Object> cloned = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                cloned.put(deepClone(entry.getKey()), deepClone(entry.getValue()));
            }
            return (T) cloned;
        }

        // Primitive types and immutable types don't need cloning
        if (value instanceof String || value instanceof Number ||
            value instanceof Boolean || value instanceof Character) {
            return value;
        }

        // Fallback: serialize/deserialize via JSON
        String json = JsonUtils.toJson(value);
        return JsonUtils.fromJson(json, (Class<T>) value.getClass());
    }

    /**
     * Get all recorded slow operations.
     */
    public static List<SlowOperationRecord> getSlowOperations() {
        return new ArrayList<>(slowOperations);
    }

    /**
     * Clear slow operations log.
     */
    public static void clearSlowOperations() {
        slowOperations.clear();
    }

    /**
     * Check if slow operation logging is enabled.
     */
    public static boolean isLoggingEnabled() {
        return SLOW_OPERATION_THRESHOLD_MS.get() < Long.MAX_VALUE;
    }

    /**
     * JSON utilities helper class.
     */
    private static final class JsonUtils {
        // Placeholder - would use Jackson or similar in real implementation
        static String toJson(Object value) {
            // Simplified JSON serialization
            if (value == null) return "null";
            if (value instanceof String) return "\"" + escape((String) value) + "\"";
            if (value instanceof Number || value instanceof Boolean) return value.toString();
            if (value instanceof List) {
                StringBuilder sb = new StringBuilder("[");
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(toJson(list.get(i)));
                }
                sb.append("]");
                return sb.toString();
            }
            if (value instanceof Map) {
                StringBuilder sb = new StringBuilder("{");
                Map<?, ?> map = (Map<?, ?>) value;
                boolean first = true;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("\"").append(escape(entry.getKey().toString())).append("\":");
                    sb.append(toJson(entry.getValue()));
                }
                sb.append("}");
                return sb.toString();
            }
            return "\"" + escape(value.toString()) + "\"";
        }

        static String toPrettyJson(Object value) {
            return toJson(value); // Simplified - no pretty printing
        }

        static <T> T fromJson(String text, Class<T> type) {
            // Simple JSON parsing implementation
            if (text == null || text.isEmpty()) return null;

            try {
                text = text.trim();

                // Handle primitive types
                if (type == String.class) {
                    if (text.startsWith("\"") && text.endsWith("\"")) {
                        @SuppressWarnings("unchecked")
                        T result = (T) text.substring(1, text.length() - 1);
                        return result;
                    }
                    @SuppressWarnings("unchecked")
                    T result = (T) text;
                    return result;
                }
                if (type == Integer.class || type == int.class) {
                    @SuppressWarnings("unchecked")
                    T result = (T) Integer.valueOf(text);
                    return result;
                }
                if (type == Long.class || type == long.class) {
                    @SuppressWarnings("unchecked")
                    T result = (T) Long.valueOf(text);
                    return result;
                }
                if (type == Boolean.class || type == boolean.class) {
                    @SuppressWarnings("unchecked")
                    T result = (T) Boolean.valueOf(text);
                    return result;
                }
                if (type == Double.class || type == double.class) {
                    @SuppressWarnings("unchecked")
                    T result = (T) Double.valueOf(text);
                    return result;
                }

                // Handle Map type
                if (text.startsWith("{") && text.endsWith("}")) {
                    @SuppressWarnings("unchecked")
                    T result = (T) parseJsonObject(text);
                    return result;
                }

                // Handle List type
                if (text.startsWith("[") && text.endsWith("]")) {
                    @SuppressWarnings("unchecked")
                    T result = (T) parseJsonArray(text);
                    return result;
                }

                return null;
            } catch (Exception e) {
                return null;
            }
        }

        static Map<String, Object> parseJsonObject(String text) {
            Map<String, Object> result = new LinkedHashMap<>();
            text = text.substring(1, text.length() - 1).trim();

            int i = 0;
            while (i < text.length()) {
                // Find key
                int keyStart = text.indexOf("\"", i);
                if (keyStart < 0) break;
                int keyEnd = text.indexOf("\"", keyStart + 1);
                if (keyEnd < 0) break;

                String key = text.substring(keyStart + 1, keyEnd);

                // Find value
                int colon = text.indexOf(":", keyEnd);
                if (colon < 0) break;

                // Determine value type and parse
                int valStart = colon + 1;
                while (valStart < text.length() && Character.isWhitespace(text.charAt(valStart))) valStart++;

                if (valStart >= text.length()) break;

                char firstChar = text.charAt(valStart);
                Object value;

                if (firstChar == '"') {
                    int valEnd = text.indexOf("\"", valStart + 1);
                    value = text.substring(valStart + 1, valEnd);
                    i = valEnd + 1;
                } else if (firstChar == '{') {
                    int depth = 1;
                    int valEnd = valStart + 1;
                    while (valEnd < text.length() && depth > 0) {
                        char c = text.charAt(valEnd);
                        if (c == '{') depth++;
                        else if (c == '}') depth--;
                        valEnd++;
                    }
                    value = parseJsonObject(text.substring(valStart, valEnd));
                    i = valEnd;
                } else if (firstChar == '[') {
                    int depth = 1;
                    int valEnd = valStart + 1;
                    while (valEnd < text.length() && depth > 0) {
                        char c = text.charAt(valEnd);
                        if (c == '[') depth++;
                        else if (c == ']') depth--;
                        valEnd++;
                    }
                    value = parseJsonArray(text.substring(valStart, valEnd));
                    i = valEnd;
                } else if (Character.isDigit(firstChar) || firstChar == '-') {
                    int valEnd = valStart;
                    while (valEnd < text.length() && (Character.isDigit(text.charAt(valEnd)) || text.charAt(valEnd) == '.' || text.charAt(valEnd) == '-')) {
                        valEnd++;
                    }
                    String numStr = text.substring(valStart, valEnd);
                    if (numStr.contains(".")) {
                        value = Double.parseDouble(numStr);
                    } else {
                        value = Long.parseLong(numStr);
                    }
                    i = valEnd;
                } else if (text.substring(valStart).startsWith("true")) {
                    value = true;
                    i = valStart + 4;
                } else if (text.substring(valStart).startsWith("false")) {
                    value = false;
                    i = valStart + 5;
                } else if (text.substring(valStart).startsWith("null")) {
                    value = null;
                    i = valStart + 4;
                } else {
                    break;
                }

                result.put(key, value);

                // Skip comma
                while (i < text.length() && (text.charAt(i) == ',' || Character.isWhitespace(text.charAt(i)))) i++;
            }

            return result;
        }

        static List<Object> parseJsonArray(String text) {
            List<Object> result = new ArrayList<>();
            text = text.substring(1, text.length() - 1).trim();

            int i = 0;
            while (i < text.length()) {
                // Skip whitespace
                while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
                if (i >= text.length()) break;

                char firstChar = text.charAt(i);
                Object value;

                if (firstChar == '"') {
                    int valEnd = text.indexOf("\"", i + 1);
                    value = text.substring(i + 1, valEnd);
                    i = valEnd + 1;
                } else if (firstChar == '{') {
                    int depth = 1;
                    int valEnd = i + 1;
                    while (valEnd < text.length() && depth > 0) {
                        char c = text.charAt(valEnd);
                        if (c == '{') depth++;
                        else if (c == '}') depth--;
                        valEnd++;
                    }
                    value = parseJsonObject(text.substring(i, valEnd));
                    i = valEnd;
                } else if (firstChar == '[') {
                    int depth = 1;
                    int valEnd = i + 1;
                    while (valEnd < text.length() && depth > 0) {
                        char c = text.charAt(valEnd);
                        if (c == '[') depth++;
                        else if (c == ']') depth--;
                        valEnd++;
                    }
                    value = parseJsonArray(text.substring(i, valEnd));
                    i = valEnd;
                } else if (Character.isDigit(firstChar) || firstChar == '-') {
                    int valEnd = i;
                    while (valEnd < text.length() && (Character.isDigit(text.charAt(valEnd)) || text.charAt(valEnd) == '.' || text.charAt(valEnd) == '-')) {
                        valEnd++;
                    }
                    String numStr = text.substring(i, valEnd);
                    if (numStr.contains(".")) {
                        value = Double.parseDouble(numStr);
                    } else {
                        value = Long.parseLong(numStr);
                    }
                    i = valEnd;
                } else if (text.substring(i).startsWith("true")) {
                    value = true;
                    i += 4;
                } else if (text.substring(i).startsWith("false")) {
                    value = false;
                    i += 5;
                } else if (text.substring(i).startsWith("null")) {
                    value = null;
                    i += 4;
                } else {
                    break;
                }

                result.add(value);

                // Skip comma
                while (i < text.length() && (text.charAt(i) == ',' || Character.isWhitespace(text.charAt(i)))) i++;
            }

            return result;
        }

        private static String escape(String s) {
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}