/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code JSON path utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;
import java.util.regex.*;

/**
 * JSONPath utilities for navigating JSON-like structures.
 */
public final class JsonPathUtils {
    private JsonPathUtils() {}

    private static final Pattern PATH_PATTERN = Pattern.compile(
        "\\[(\\d+)\\]|\\['([^']+)'\\]|\\.([^.\\[]+)"
    );

    /**
     * Get value at path.
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> get(Object root, String path) {
        if (path == null || path.isEmpty() || path.equals("$")) {
            return Optional.ofNullable((T) root);
        }

        Object current = root;
        Matcher matcher = PATH_PATTERN.matcher(path);

        while (matcher.find()) {
            if (current == null) return Optional.empty();

            String indexStr = matcher.group(1);
            String keyQuote = matcher.group(2);
            String keyDot = matcher.group(3);

            if (indexStr != null) {
                // Array index
                if (!(current instanceof List)) return Optional.empty();
                int index = Integer.parseInt(indexStr);
                List<?> list = (List<?>) current;
                if (index < 0 || index >= list.size()) return Optional.empty();
                current = list.get(index);
            } else {
                // Object key
                String key = keyQuote != null ? keyQuote : keyDot;
                if (!(current instanceof Map)) return Optional.empty();
                current = ((Map<String, Object>) current).get(key);
            }
        }

        return Optional.ofNullable((T) current);
    }

    /**
     * Set value at path.
     */
    @SuppressWarnings("unchecked")
    public static void set(Object root, String path, Object value) {
        if (path == null || path.isEmpty()) return;

        // Find parent path
        int lastSep = Math.max(
            path.lastIndexOf('.'),
            Math.max(path.lastIndexOf('['), path.lastIndexOf(']'))
        );

        if (lastSep < 0) return;

        String parentPath = path.substring(0, lastSep);
        if (parentPath.endsWith(".")) {
            parentPath = parentPath.substring(0, parentPath.length() - 1);
        }
        if (parentPath.endsWith("]")) {
            // Keep bracket notation for parent
        }

        String keyPart = path.substring(lastSep);
        if (keyPart.startsWith(".")) keyPart = keyPart.substring(1);

        Object parent = get(root, parentPath).orElse(null);
        if (parent == null) return;

        if (keyPart.startsWith("[") && keyPart.endsWith("]")) {
            // Array index
            String indexStr = keyPart.substring(1, keyPart.length() - 1);
            if (indexStr.startsWith("'") && indexStr.endsWith("'")) {
                // Key in brackets
                String key = indexStr.substring(1, indexStr.length() - 1);
                if (parent instanceof Map) {
                    ((Map<String, Object>) parent).put(key, value);
                }
            } else {
                int index = Integer.parseInt(indexStr);
                if (parent instanceof List) {
                    List<Object> list = (List<Object>) parent;
                    while (list.size() <= index) {
                        list.add(null);
                    }
                    list.set(index, value);
                }
            }
        } else {
            if (parent instanceof Map) {
                ((Map<String, Object>) parent).put(keyPart, value);
            }
        }
    }

    /**
     * Check if path exists.
     */
    public static boolean exists(Object root, String path) {
        return get(root, path).isPresent();
    }

    /**
     * Get all matching paths.
     */
    public static List<String> findPaths(Object root, String pattern) {
        List<String> result = new ArrayList<>();
        findPathsRecursive(root, "$", pattern, result);
        return result;
    }

    private static void findPathsRecursive(Object obj, String currentPath, String pattern, List<String> result) {
        if (matchesPattern(currentPath, pattern)) {
            result.add(currentPath);
        }

        if (obj instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                String childPath = currentPath + "." + entry.getKey();
                findPathsRecursive(entry.getValue(), childPath, pattern, result);
            }
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            for (int i = 0; i < list.size(); i++) {
                String childPath = currentPath + "[" + i + "]";
                findPathsRecursive(list.get(i), childPath, pattern, result);
            }
        }
    }

    private static boolean matchesPattern(String path, String pattern) {
        if (pattern.equals("$") || pattern.equals("$.*")) {
            return true;
        }
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return path.startsWith(prefix);
        }
        return path.equals(pattern);
    }

    /**
     * Delete value at path.
     */
    @SuppressWarnings("unchecked")
    public static boolean delete(Object root, String path) {
        int lastDot = path.lastIndexOf('.');
        int lastBracket = path.lastIndexOf('[');

        if (lastDot < 0 && lastBracket < 0) {
            return false;
        }

        int lastSep = Math.max(lastDot, lastBracket);
        String parentPath = path.substring(0, lastSep);
        if (parentPath.endsWith(".")) {
            parentPath = parentPath.substring(0, parentPath.length() - 1);
        }

        String keyPart = path.substring(lastSep);
        if (keyPart.startsWith(".")) keyPart = keyPart.substring(1);

        Object parent = get(root, parentPath).orElse(null);
        if (parent == null) return false;

        if (keyPart.startsWith("[") && keyPart.endsWith("]")) {
            String inner = keyPart.substring(1, keyPart.length() - 1);
            if (inner.startsWith("'") && inner.endsWith("'")) {
                String key = inner.substring(1, inner.length() - 1);
                if (parent instanceof Map) {
                    ((Map<String, Object>) parent).remove(key);
                    return true;
                }
            } else {
                int index = Integer.parseInt(inner);
                if (parent instanceof List && index < ((List<?>) parent).size()) {
                    ((List<Object>) parent).remove(index);
                    return true;
                }
            }
        } else {
            if (parent instanceof Map) {
                ((Map<String, Object>) parent).remove(keyPart);
                return true;
            }
        }

        return false;
    }

    /**
     * Get all values matching predicate.
     */
    public static <T> List<T> findAll(Object root, Class<T> type, Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        findAllRecursive(root, type, predicate, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> void findAllRecursive(Object obj, Class<T> type, Predicate<T> predicate, List<T> result) {
        if (obj != null && type.isInstance(obj)) {
            T value = (T) obj;
            if (predicate == null || predicate.test(value)) {
                result.add(value);
            }
        }

        if (obj instanceof Map) {
            for (Object value : ((Map<?, ?>) obj).values()) {
                findAllRecursive(value, type, predicate, result);
            }
        } else if (obj instanceof List) {
            for (Object value : (List<?>) obj) {
                findAllRecursive(value, type, predicate, result);
            }
        }
    }

    /**
     * Parse JSONPath expression.
     */
    public static List<PathSegment> parsePath(String path) {
        List<PathSegment> segments = new ArrayList<>();
        Matcher matcher = PATH_PATTERN.matcher(path);

        while (matcher.find()) {
            String indexStr = matcher.group(1);
            String keyQuote = matcher.group(2);
            String keyDot = matcher.group(3);

            if (indexStr != null) {
                segments.add(new PathSegment.Index(Integer.parseInt(indexStr)));
            } else {
                String key = keyQuote != null ? keyQuote : keyDot;
                segments.add(new PathSegment.Key(key));
            }
        }

        return segments;
    }

    /**
     * Path segment.
     */
    public sealed interface PathSegment permits PathSegment.Key, PathSegment.Index {
        record Key(String value) implements PathSegment {}
        record Index(int value) implements PathSegment {}
    }
}