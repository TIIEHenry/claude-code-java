/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code frontmatter parser
 */
package com.anthropic.claudecode.utils.parser;

import java.util.*;
import java.util.regex.*;

/**
 * Frontmatter parser for markdown files.
 * Extracts and parses YAML frontmatter between --- delimiters.
 */
public final class FrontmatterParser {
    private FrontmatterParser() {}

    // Frontmatter regex pattern
    private static final Pattern FRONTMATTER_REGEX = Pattern.compile(
            "^---\\s*\\n([\\s\\S]*?)---\\s*\\n?"
    );

    // Characters that require quoting in YAML values
    private static final Pattern YAML_SPECIAL_CHARS = Pattern.compile(
            "[{}\\[\\]*&#!|>%@`]|: "
    );

    /**
     * Frontmatter data parsed from markdown.
     */
    public record FrontmatterData(
            String allowedTools,
            List<String> allowedToolsList,
            String description,
            String type,
            String argumentHint,
            String whenToUse,
            String version,
            String hideFromSlashCommandTool,
            String model,
            String skills,
            String userInvocable,
            Object hooks,
            String effort,
            String context,
            String agent,
            List<String> paths,
            String shell,
            Map<String, Object> additionalFields
    ) {
        public static FrontmatterData empty() {
            return new FrontmatterData(
                    null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null,
                    null, null, null, null
            );
        }
    }

    /**
     * Parsed markdown result.
     */
    public record ParsedMarkdown(
            FrontmatterData frontmatter,
            String content
    ) {}

    /**
     * Parse markdown content to extract frontmatter and content.
     */
    public static ParsedMarkdown parseFrontmatter(String markdown) {
        return parseFrontmatter(markdown, null);
    }

    /**
     * Parse markdown content to extract frontmatter and content.
     *
     * @param markdown The raw markdown content
     * @param sourcePath Optional source path for error messages
     * @return ParsedMarkdown with frontmatter and content
     */
    public static ParsedMarkdown parseFrontmatter(String markdown, String sourcePath) {
        if (markdown == null || markdown.isEmpty()) {
            return new ParsedMarkdown(FrontmatterData.empty(), markdown);
        }

        Matcher matcher = FRONTMATTER_REGEX.matcher(markdown);

        if (!matcher.find()) {
            return new ParsedMarkdown(FrontmatterData.empty(), markdown);
        }

        String frontmatterText = matcher.group(1) != null ? matcher.group(1) : "";
        String content = markdown.substring(matcher.end());

        FrontmatterData frontmatter = parseYamlFrontmatter(frontmatterText, sourcePath);

        return new ParsedMarkdown(frontmatter, content);
    }

    /**
     * Parse YAML frontmatter text.
     */
    private static FrontmatterData parseYamlFrontmatter(String text, String sourcePath) {
        Map<String, Object> fields = new LinkedHashMap<>();

        try {
            fields = parseYaml(text);
        } catch (Exception e) {
            // Try again after quoting problematic values
            try {
                String quotedText = quoteProblematicValues(text);
                fields = parseYaml(quotedText);
            } catch (Exception retryError) {
                // Failed to parse - return empty frontmatter
                return FrontmatterData.empty();
            }
        }

        return buildFrontmatterData(fields);
    }

    /**
     * Build FrontmatterData from parsed fields.
     */
    private static FrontmatterData buildFrontmatterData(Map<String, Object> fields) {
        String allowedTools = getString(fields, "allowed-tools");
        List<String> allowedToolsList = parseToolList(fields.get("allowed-tools"));

        return new FrontmatterData(
                allowedTools,
                allowedToolsList,
                getString(fields, "description"),
                getString(fields, "type"),
                getString(fields, "argument-hint"),
                getString(fields, "when_to_use"),
                getString(fields, "version"),
                getString(fields, "hide-from-slash-command-tool"),
                getString(fields, "model"),
                getString(fields, "skills"),
                getString(fields, "user-invocable"),
                fields.get("hooks"),
                getString(fields, "effort"),
                getString(fields, "context"),
                getString(fields, "agent"),
                parsePaths(fields.get("paths")),
                getString(fields, "shell"),
                fields
        );
    }

    /**
     * Get string value from fields map.
     */
    private static String getString(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        if (value == null) return null;
        if (value instanceof String) return ((String) value).trim().isEmpty() ? null : (String) value;
        return value.toString();
    }

    /**
     * Parse tool list from frontmatter value.
     */
    public static List<String> parseToolList(Object value) {
        if (value == null) return null;

        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty()) return List.of();

            // Split by comma
            return Arrays.stream(str.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            return list.stream()
                    .filter(item -> item instanceof String)
                    .map(item -> (String) item)
                    .filter(s -> !s.trim().isEmpty())
                    .map(String::trim)
                    .toList();
        }

        return List.of();
    }

    /**
     * Parse paths from frontmatter value.
     */
    private static List<String> parsePaths(Object value) {
        if (value == null) return null;

        if (value instanceof String) {
            return splitPathInFrontmatter((String) value);
        }

        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            return list.stream()
                    .flatMap(item -> {
                        if (item instanceof String) {
                            return splitPathInFrontmatter((String) item).stream();
                        }
                        return java.util.stream.Stream.empty();
                    })
                    .toList();
        }

        return null;
    }

    /**
     * Split path in frontmatter, handling brace expansion.
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

        // Add the last part
        String trimmed = current.toString().trim();
        if (!trimmed.isEmpty()) {
            parts.add(trimmed);
        }

        // Expand brace patterns
        return parts.stream()
                .filter(p -> !p.isEmpty())
                .flatMap(p -> expandBraces(p).stream())
                .toList();
    }

    /**
     * Expand brace patterns in a glob string.
     */
    private static List<String> expandBraces(String pattern) {
        // Find the first brace group
        Pattern bracePattern = Pattern.compile("^([^{]*)\\{([^}]+)\\}(.*)$");
        Matcher matcher = bracePattern.matcher(pattern);

        if (!matcher.find()) {
            return List.of(pattern);
        }

        String prefix = matcher.group(1) != null ? matcher.group(1) : "";
        String alternatives = matcher.group(2) != null ? matcher.group(2) : "";
        String suffix = matcher.group(3) != null ? matcher.group(3) : "";

        // Split alternatives by comma and expand each one
        List<String> expanded = new ArrayList<>();
        for (String alt : alternatives.split(",")) {
            String combined = prefix + alt.trim() + suffix;
            expanded.addAll(expandBraces(combined));
        }

        return expanded;
    }

    /**
     * Pre-process frontmatter text to quote values with special YAML characters.
     */
    private static String quoteProblematicValues(String frontmatterText) {
        String[] lines = frontmatterText.split("\n");
        List<String> result = new ArrayList<>();

        for (String line : lines) {
            // Match simple key: value lines
            Pattern keyValuePattern = Pattern.compile("^([a-zA-Z_-]+):\\s+(.+)$");
            Matcher matcher = keyValuePattern.matcher(line);

            if (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);

                if (key != null && value != null) {
                    // Skip if already quoted
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        result.add(line);
                        continue;
                    }

                    // Quote if contains special YAML characters
                    if (YAML_SPECIAL_CHARS.matcher(value).find()) {
                        String escaped = value
                                .replace("\\", "\\\\")
                                .replace("\"", "\\\"");
                        result.add(key + ": \"" + escaped + "\"");
                        continue;
                    }
                }
            }

            result.add(line);
        }

        return String.join("\n", result);
    }

    /**
     * Simple YAML parser for frontmatter.
     */
    private static Map<String, Object> parseYaml(String yaml) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (yaml == null || yaml.trim().isEmpty()) {
            return result;
        }

        String[] lines = yaml.split("\n");
        String currentKey = null;
        List<String> currentList = null;
        int currentIndent = 0;

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            int indent = countLeadingSpaces(line);
            String trimmed = line.trim();

            // List item
            if (trimmed.startsWith("- ")) {
                if (currentList == null) {
                    currentList = new ArrayList<>();
                }
                currentList.add(trimmed.substring(2).trim());
                continue;
            }

            // Key: value
            int colonIndex = trimmed.indexOf(':');
            if (colonIndex > 0) {
                // Save previous list if any
                if (currentKey != null && currentList != null) {
                    result.put(currentKey, currentList);
                    currentList = null;
                }

                String key = trimmed.substring(0, colonIndex).trim();
                String value = colonIndex < trimmed.length() - 1 ?
                        trimmed.substring(colonIndex + 1).trim() : null;

                if (value == null || value.isEmpty()) {
                    currentKey = key;
                    currentIndent = indent;
                } else {
                    result.put(key, parseYamlValue(value));
                    currentKey = null;
                }
            }
        }

        // Save final list if any
        if (currentKey != null && currentList != null) {
            result.put(currentKey, currentList);
        }

        return result;
    }

    /**
     * Parse a YAML value.
     */
    private static Object parseYamlValue(String value) {
        if (value == null || value.isEmpty() || "null".equals(value)) {
            return null;
        }

        // Remove quotes
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }

        // Boolean
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;

        // Number
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e2) {
                // Not a number
            }
        }

        // Array (inline)
        if (value.startsWith("[") && value.endsWith("]")) {
            String inner = value.substring(1, value.length() - 1);
            return Arrays.stream(inner.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(FrontmatterParser::parseYamlValue)
                    .toList();
        }

        return value;
    }

    /**
     * Count leading spaces in a line.
     */
    private static int countLeadingSpaces(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else break;
        }
        return count;
    }

    /**
     * Parse a positive integer from frontmatter.
     */
    public static Integer parsePositiveInt(Object value) {
        if (value == null) return null;

        int parsed;
        if (value instanceof Number) {
            parsed = ((Number) value).intValue();
        } else {
            try {
                parsed = Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return parsed > 0 ? parsed : null;
    }

    /**
     * Validate and coerce description to string.
     */
    public static String coerceDescriptionToString(Object value, String componentName, String pluginName) {
        if (value == null) return null;

        if (value instanceof String) {
            String str = ((String) value).trim();
            return str.isEmpty() ? null : str;
        }

        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }

        // Non-scalar descriptions are invalid
        return null;
    }

    /**
     * Parse boolean frontmatter value.
     */
    public static boolean parseBoolean(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(value != null ? value.toString() : null);
    }

    /**
     * Parse shell frontmatter value.
     */
    public static String parseShell(Object value, String source) {
        if (value == null) return null;

        String normalized = value.toString().trim().toLowerCase();
        if (normalized.isEmpty()) return null;

        if ("bash".equals(normalized) || "powershell".equals(normalized)) {
            return normalized;
        }

        // Unrecognized - return null (falls back to bash)
        return null;
    }
}