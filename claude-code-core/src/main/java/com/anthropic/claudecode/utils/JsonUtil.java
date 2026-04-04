/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/json.ts
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/**
 * JSON parsing utilities.
 */
public final class JsonUtil {
    private JsonUtil() {}

    private static final int PARSE_CACHE_MAX_KEY_BYTES = 8 * 1024;
    private static final int MAX_CACHE_SIZE = 50;
    private static final LinkedHashMap<String, Object> parseCache =
        new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };

    /**
     * Safely parse JSON string.
     */
    public static Object safeParseJSON(String json) {
        return safeParseJSON(json, true);
    }

    public static Object safeParseJSON(String json, boolean shouldLogError) {
        if (json == null || json.isEmpty()) return null;

        String stripped = stripBOM(json);

        // Use cache for small inputs
        if (stripped.length() <= PARSE_CACHE_MAX_KEY_BYTES) {
            synchronized (parseCache) {
                Object cached = parseCache.get(stripped);
                if (cached != null) {
                    return cached;
                }
            }
        }

        try {
            Object result = new JSONObject(stripped);
            if (stripped.length() <= PARSE_CACHE_MAX_KEY_BYTES) {
                synchronized (parseCache) {
                    parseCache.put(stripped, result);
                }
            }
            return result;
        } catch (JSONException e) {
            // Try parsing as array
            try {
                Object result = new JSONArray(stripped);
                if (stripped.length() <= PARSE_CACHE_MAX_KEY_BYTES) {
                    synchronized (parseCache) {
                        parseCache.put(stripped, result);
                    }
                }
                return result;
            } catch (JSONException e2) {
                // Try parsing as primitive
                try {
                    return parsePrimitive(stripped);
                } catch (Exception e3) {
                    if (shouldLogError) {
                        System.err.println("JSON parse error: " + e.getMessage());
                    }
                    return null;
                }
            }
        }
    }

    private static Object parsePrimitive(String json) {
        String trimmed = json.trim();
        if ("null".equals(trimmed)) return null;
        if ("true".equals(trimmed)) return true;
        if ("false".equals(trimmed)) return false;
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        // Try number
        try {
            if (trimmed.contains(".")) {
                return Double.parseDouble(trimmed);
            } else {
                return Long.parseLong(trimmed);
            }
        } catch (NumberFormatException e) {
            throw new JSONException("Cannot parse: " + json);
        }
    }

    /**
     * Strip BOM from string.
     */
    public static String stripBOM(String s) {
        if (s == null || s.isEmpty()) return s;
        // UTF-8 BOM
        if (s.length() >= 3 && s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s;
    }

    /**
     * Parse JSONL data from string.
     */
    public static List<Object> parseJSONL(String data) {
        List<Object> results = new ArrayList<>();
        if (data == null || data.isEmpty()) return results;

        String stripped = stripBOM(data);
        int start = 0;
        int len = stripped.length();

        while (start < len) {
            int end = stripped.indexOf('\n', start);
            if (end == -1) end = len;

            String line = stripped.substring(start, end).trim();
            start = end + 1;

            if (line.isEmpty()) continue;

            try {
                results.add(safeParseJSON(line, false));
            } catch (Exception e) {
                // Skip malformed lines
            }
        }

        return results;
    }

    /**
     * Read JSONL file.
     */
    public static List<Object> readJSONLFile(String filePath) throws IOException {
        String content = Files.readString(Paths.get(filePath));
        return parseJSONL(content);
    }

    /**
     * Read JSONL file with size limit.
     */
    public static List<Object> readJSONLFile(String filePath, long maxBytes) throws IOException {
        Path path = Paths.get(filePath);
        long size = Files.size(path);

        if (size <= maxBytes) {
            return parseJSONL(Files.readString(path));
        }

        // Read tail of file
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            raf.seek(size - maxBytes);
            byte[] buf = new byte[(int) maxBytes];
            int bytesRead = raf.read(buf);

            // Skip first partial line
            int start = 0;
            for (int i = 0; i < bytesRead; i++) {
                if (buf[i] == '\n') {
                    start = i + 1;
                    break;
                }
            }

            String data = new String(buf, start, bytesRead - start);
            return parseJSONL(data);
        }
    }

    /**
     * Add item to JSON array.
     */
    public static String addItemToJSONArray(String content, Object newItem) {
        if (content == null || content.trim().isEmpty()) {
            return "[" + jsonStringify(newItem) + "]";
        }

        try {
            JSONArray arr = new JSONArray(stripBOM(content));
            arr.put(newItem);
            return arr.toString(4);
        } catch (JSONException e) {
            // Not an array, create new
            return "[" + jsonStringify(newItem) + "]";
        }
    }

    /**
     * JSON stringify with pretty printing.
     */
    public static String jsonStringify(Object obj) {
        return jsonStringify(obj, 0);
    }

    public static String jsonStringify(Object obj, int indent) {
        if (obj == null) return "null";
        if (obj instanceof String s) return quoteString(s);
        if (obj instanceof Boolean b) return b.toString();
        if (obj instanceof Number n) return n.toString();

        try {
            if (obj instanceof JSONObject jo) {
                return indent > 0 ? jo.toString(indent) : jo.toString();
            }
            if (obj instanceof JSONArray ja) {
                return indent > 0 ? ja.toString(indent) : ja.toString();
            }
            if (obj instanceof Map<?, ?> map) {
                JSONObject jo = new JSONObject(map);
                return indent > 0 ? jo.toString(indent) : jo.toString();
            }
            if (obj instanceof List<?> list) {
                JSONArray ja = new JSONArray(list);
                return indent > 0 ? ja.toString(indent) : ja.toString();
            }

            // Use reflection for other objects
            return new JSONObject(obj).toString(indent);
        } catch (Exception e) {
            return quoteString(obj.toString());
        }
    }

    private static String quoteString(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
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
        sb.append('"');
        return sb.toString();
    }

    /**
     * Clear parse cache.
     */
    public static void clearCache() {
        synchronized (parseCache) {
            parseCache.clear();
        }
    }
}