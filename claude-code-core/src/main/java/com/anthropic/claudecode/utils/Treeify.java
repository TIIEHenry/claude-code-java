/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code treeify utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Tree visualization utility for displaying nested data structures.
 */
public final class Treeify {
    private Treeify() {}

    /**
     * Tree node representation.
     */
    public interface TreeNode {
        Map<String, Object> getChildren();
    }

    /**
     * Options for tree rendering.
     */
    public record Options(
            boolean showValues,
            boolean hideFunctions,
            String treeCharColor,
            String keyColor,
            String valueColor
    ) {
        public static Options defaults() {
            return new Options(true, false, null, null, null);
        }
    }

    // Tree characters using Unicode box-drawing characters
    private static final String BRANCH = "├";
    private static final String LAST_BRANCH = "└";
    private static final String LINE = "│";
    private static final String EMPTY = " ";

    /**
     * Convert a map to a tree string representation.
     */
    public static String treeify(Map<String, Object> obj, Options options) {
        if (options == null) {
            options = Options.defaults();
        }

        if (obj == null || obj.isEmpty()) {
            return "(empty)";
        }

        List<String> lines = new ArrayList<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        growBranch(obj, "", true, 0, lines, visited, options);

        return String.join("\n", lines);
    }

    /**
     * Convert an object to a tree string representation.
     */
    public static String treeify(Object obj) {
        return treeify(obj, Options.defaults());
    }

    /**
     * Convert an object to tree with custom options.
     */
    public static String treeify(Object obj, Options options) {
        if (obj instanceof Map) {
            return treeify((Map<String, Object>) obj, options);
        }

        if (obj == null) {
            return "(empty)";
        }

        // For non-map objects, just return string representation
        return String.valueOf(obj);
    }

    private static void growBranch(
            Object node,
            String prefix,
            boolean isLast,
            int depth,
            List<String> lines,
            Set<Object> visited,
            Options options
    ) {
        if (node instanceof String) {
            lines.add(prefix + colorize((String) node, options.valueColor));
            return;
        }

        if (!(node instanceof Map)) {
            if (options.showValues) {
                String valueStr = String.valueOf(node);
                lines.add(prefix + colorize(valueStr, options.valueColor));
            }
            return;
        }

        Map<String, Object> mapNode = (Map<String, Object>) node;

        // Check for circular references
        if (visited.contains(node)) {
            lines.add(prefix + colorize("[Circular]", options.valueColor));
            return;
        }
        visited.add(node);

        List<String> keys = new ArrayList<>(mapNode.keySet());

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Object value = mapNode.get(key);
            boolean isLastKey = i == keys.size() - 1;
            String nodePrefix = (depth == 0 && i == 0) ? "" : prefix;

            // Determine which tree character to use
            String treeChar = isLastKey ? LAST_BRANCH : BRANCH;
            String coloredTreeChar = colorize(treeChar, options.treeCharColor);
            String coloredKey = key.trim().isEmpty() ? "" : colorize(key, options.keyColor);

            String line = nodePrefix + coloredTreeChar + (coloredKey.isEmpty() ? "" : " " + coloredKey);

            boolean shouldAddColon = !key.trim().isEmpty();

            // Check for circular reference before recursing
            if (value != null && value instanceof Map && visited.contains(value)) {
                String coloredValue = colorize("[Circular]", options.valueColor);
                lines.add(line + (shouldAddColon ? ": " : (line.isEmpty() ? "" : " ")) + coloredValue);
            } else if (value != null && value instanceof Map && !(value instanceof List)) {
                lines.add(line);
                String continuationChar = isLastKey ? EMPTY : LINE;
                String coloredContinuation = colorize(continuationChar, options.treeCharColor);
                String nextPrefix = nodePrefix + coloredContinuation + " ";
                growBranch(value, nextPrefix, isLastKey, depth + 1, lines, visited, options);
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                lines.add(line + (shouldAddColon ? ": " : (line.isEmpty() ? "" : " ")) + "[Array(" + list.size() + ")]");
            } else if (options.showValues) {
                String valueStr = String.valueOf(value);
                String coloredValue = colorize(valueStr, options.valueColor);
                line += (shouldAddColon ? ": " : (line.isEmpty() ? "" : " ")) + coloredValue;
                lines.add(line);
            } else {
                lines.add(line);
            }
        }
    }

    /**
     * Apply color to text if color is specified.
     */
    private static String colorize(String text, String color) {
        if (color == null || color.isEmpty()) {
            return text;
        }
        return Theme.colorize(text, color);
    }

    /**
     * Create a simple tree from a map with default options.
     */
    public static String fromMap(Map<String, Object> map) {
        return treeify(map, Options.defaults());
    }
}