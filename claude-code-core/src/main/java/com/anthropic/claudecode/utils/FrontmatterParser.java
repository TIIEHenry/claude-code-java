/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code frontmatter parser
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;

/**
 * Frontmatter parser for markdown files.
 * Extracts and parses YAML frontmatter between --- delimiters.
 */
public final class FrontmatterParser {
    private FrontmatterParser() {}

    // Regex for frontmatter
    private static final Pattern FRONTMATTER_REGEX = Pattern.compile("^---\\s*\\n([\\s\\S]*?)---\\s*\\n?");

    // Characters that require quoting in YAML values
    private static final Pattern YAML_SPECIAL_CHARS = Pattern.compile("[{}\\[\\]*&#!|>%@`]|: ");

    /**
     * Parsed markdown result.
     */
    public record ParsedMarkdown(Map<String, Object> frontmatter, String content) {
        /**
         * Get a string value from frontmatter.
         */
        public String getString(String key) {
            Object value = frontmatter.get(key);
            return value != null ? value.toString() : null;
        }

        /**
         * Get a list value from frontmatter.
         */
        @SuppressWarnings("unchecked")
        public List<String> getList(String key) {
            Object value = frontmatter.get(key);
            if (value instanceof List) {
                return (List<String>) value;
            }
            if (value instanceof String) {
                return Arrays.asList(((String) value).split(","));
            }
            return List.of();
        }

        /**
         * Get a boolean value from frontmatter.
         */
        public boolean getBoolean(String key) {
            Object value = frontmatter.get(key);
            return value != null && ("true".equalsIgnoreCase(value.toString()) || Boolean.TRUE.equals(value));
        }
    }

    /**
     * Parse markdown content to extract frontmatter and content.
     */
    public static ParsedMarkdown parseFrontmatter(String markdown) {
        return parseFrontmatter(markdown, null);
    }

    /**
     * Parse markdown content to extract frontmatter and content.
     */
    public static ParsedMarkdown parseFrontmatter(String markdown, String sourcePath) {
        if (markdown == null || markdown.isEmpty()) {
            return new ParsedMarkdown(new LinkedHashMap<>(), "");
        }

        Matcher matcher = FRONTMATTER_REGEX.matcher(markdown);

        if (!matcher.find()) {
            return new ParsedMarkdown(new LinkedHashMap<>(), markdown);
        }

        String frontmatterText = matcher.group(1) != null ? matcher.group(1) : "";
        String content = markdown.substring(matcher.end());

        Map<String, Object> frontmatter = new LinkedHashMap<>();

        try {
            Map<String, Object> parsed = YamlUtils.parseMap(frontmatterText);
            if (parsed != null) {
                frontmatter = parsed;
            }
        } catch (Exception e) {
            // Try again after quoting problematic values
            try {
                String quotedText = quoteProblematicValues(frontmatterText);
                Map<String, Object> parsed = YamlUtils.parseMap(quotedText);
                if (parsed != null) {
                    frontmatter = parsed;
                }
            } catch (Exception retryError) {
                String location = sourcePath != null ? " in " + sourcePath : "";
                Debug.log("Failed to parse YAML frontmatter" + location + ": " + retryError.getMessage(), "warn");
            }
        }

        return new ParsedMarkdown(frontmatter, content);
    }

    /**
     * Pre-process frontmatter text to quote values with special YAML characters.
     */
    private static String quoteProblematicValues(String frontmatterText) {
        String[] lines = frontmatterText.split("\n");
        List<String> result = new ArrayList<>();

        Pattern keyValuePattern = Pattern.compile("^([a-zA-Z_-]+):\\s+(.+)$");

        for (String line : lines) {
            Matcher matcher = keyValuePattern.matcher(line);
            if (matcher.matches()) {
                String key = matcher.group(1);
                String value = matcher.group(2);

                // Skip if already quoted
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                    result.add(line);
                    continue;
                }

                // Quote if contains special YAML characters
                if (YAML_SPECIAL_CHARS.matcher(value).find()) {
                    String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
                    result.add(key + ": \"" + escaped + "\"");
                    continue;
                }
            }
            result.add(line);
        }

        return String.join("\n", result);
    }

    /**
     * Split a comma-separated string and expand brace patterns.
     */
    public static List<String> splitPathInFrontmatter(String input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }

        // Split by comma while respecting braces
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceDepth = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '{') {
                braceDepth++;
                current.append(c);
            } else if (c == '}') {
                braceDepth--;
                current.append(c);
            } else if (c == ',' && braceDepth == 0) {
                String trimmed = current.toString().trim();
                if (!trimmed.isEmpty()) {
                    parts.add(trimmed);
                }
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // Add last part
        String trimmed = current.toString().trim();
        if (!trimmed.isEmpty()) {
            parts.add(trimmed);
        }

        // Expand brace patterns
        List<String> expanded = new ArrayList<>();
        for (String part : parts) {
            expanded.addAll(expandBraces(part));
        }

        return expanded;
    }

    /**
     * Expand brace patterns in a glob string.
     */
    private static List<String> expandBraces(String pattern) {
        Pattern bracePattern = Pattern.compile("^([^{]*)\\{([^}]+)\\}(.*)$");
        Matcher matcher = bracePattern.matcher(pattern);

        if (!matcher.find()) {
            return List.of(pattern);
        }

        String prefix = matcher.group(1) != null ? matcher.group(1) : "";
        String alternatives = matcher.group(2) != null ? matcher.group(2) : "";
        String suffix = matcher.group(3) != null ? matcher.group(3) : "";

        String[] parts = alternatives.split(",");
        List<String> expanded = new ArrayList<>();

        for (String part : parts) {
            String combined = prefix + part.trim() + suffix;
            expanded.addAll(expandBraces(combined));
        }

        return expanded;
    }

    /**
     * Parse a positive integer value from frontmatter.
     */
    public static Integer parsePositiveInt(Object value) {
        if (value == null) return null;

        try {
            int parsed;
            if (value instanceof Number) {
                parsed = ((Number) value).intValue();
            } else {
                parsed = Integer.parseInt(value.toString());
            }

            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Coerce description to string.
     */
    public static String coerceDescriptionToString(Object value, String componentName) {
        if (value == null) return null;

        if (value instanceof String) {
            String trimmed = ((String) value).trim();
            return trimmed.isEmpty() ? null : trimmed;
        }

        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }

        Debug.log("Description invalid for " + (componentName != null ? componentName : "unknown") + " - omitting", "warn");
        return null;
    }
}