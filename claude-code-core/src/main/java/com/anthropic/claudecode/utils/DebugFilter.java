/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code debug filter utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Debug filter utilities for filtering debug messages by category.
 */
public final class DebugFilter {
    private DebugFilter() {}

    /**
     * Debug filter configuration.
     */
    public record FilterConfig(List<String> include, List<String> exclude, boolean isExclusive) {}

    // Cache for parsed filters
    private static FilterConfig cachedFilter = null;
    private static String cachedFilterString = null;

    /**
     * Parse debug filter string into a filter configuration.
     *
     * Examples:
     * - "api,hooks" -> include only api and hooks categories
     * - "!1p,!file" -> exclude logging and file categories
     * - undefined/empty -> no filtering (show all)
     */
    public static FilterConfig parseDebugFilter(String filterString) {
        if (filterString == null || filterString.trim().isEmpty()) {
            return null;
        }

        // Check cache
        if (filterString.equals(cachedFilterString)) {
            return cachedFilter;
        }

        String[] filters = Arrays.stream(filterString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        if (filters.length == 0) {
            cachedFilter = null;
            cachedFilterString = filterString;
            return null;
        }

        // Check for mixed inclusive/exclusive filters
        boolean hasExclusive = Arrays.stream(filters).anyMatch(f -> f.startsWith("!"));
        boolean hasInclusive = Arrays.stream(filters).anyMatch(f -> !f.startsWith("!"));

        if (hasExclusive && hasInclusive) {
            // Mixed filters are invalid
            cachedFilter = null;
            cachedFilterString = filterString;
            return null;
        }

        // Clean up filters (remove ! prefix) and normalize
        List<String> cleanFilters = Arrays.stream(filters)
                .map(f -> f.replaceFirst("^!", "").toLowerCase())
                .toList();

        FilterConfig config = new FilterConfig(
                hasExclusive ? List.of() : cleanFilters,
                hasExclusive ? cleanFilters : List.of(),
                hasExclusive
        );

        cachedFilter = config;
        cachedFilterString = filterString;
        return config;
    }

    /**
     * Extract debug categories from a message.
     *
     * Supports multiple patterns:
     * - "category: message" -> ["category"]
     * - "[CATEGORY] message" -> ["category"]
     * - "MCP server \"name\": message" -> ["mcp", "name"]
     */
    public static List<String> extractDebugCategories(String message) {
        if (message == null || message.isEmpty()) {
            return List.of();
        }

        Set<String> categories = new LinkedHashSet<>();

        // Pattern 3: MCP server "servername" - Check this first
        Pattern mcpPattern = Pattern.compile("^MCP server [\"']([^\"']+)[\"']");
        Matcher mcpMatcher = mcpPattern.matcher(message);
        if (mcpMatcher.find()) {
            categories.add("mcp");
            categories.add(mcpMatcher.group(1).toLowerCase());
        } else {
            // Pattern 1: "category: message" (simple prefix)
            Pattern prefixPattern = Pattern.compile("^([^:\\[]+):");
            Matcher prefixMatcher = prefixPattern.matcher(message);
            if (prefixMatcher.find()) {
                categories.add(prefixMatcher.group(1).trim().toLowerCase());
            }
        }

        // Pattern 2: [CATEGORY] at the start
        Pattern bracketPattern = Pattern.compile("^\\[([^\\]]+)\\]");
        Matcher bracketMatcher = bracketPattern.matcher(message);
        if (bracketMatcher.find()) {
            categories.add(bracketMatcher.group(1).trim().toLowerCase());
        }

        // Pattern 4: Check for "1p event:"
        if (message.toLowerCase().contains("1p event:")) {
            categories.add("1p");
        }

        // Pattern 5: Look for secondary categories
        Pattern secondaryPattern = Pattern.compile(":\\s*([^:]+?)(?:\\s+(?:type|mode|status|event))?:");
        Matcher secondaryMatcher = secondaryPattern.matcher(message);
        if (secondaryMatcher.find()) {
            String secondary = secondaryMatcher.group(1).trim().toLowerCase();
            if (secondary.length() < 30 && !secondary.contains(" ")) {
                categories.add(secondary);
            }
        }

        return new ArrayList<>(categories);
    }

    /**
     * Check if debug message should be shown based on filter.
     */
    public static boolean shouldShowDebugCategories(List<String> categories, FilterConfig filter) {
        // No filter means show everything
        if (filter == null) {
            return true;
        }

        // If no categories found, exclude
        if (categories.isEmpty()) {
            return false;
        }

        if (filter.isExclusive()) {
            // Exclusive mode: show if none of the categories are in the exclude list
            return categories.stream().noneMatch(cat -> filter.exclude().contains(cat));
        } else {
            // Inclusive mode: show if any of the categories are in the include list
            return categories.stream().anyMatch(cat -> filter.include().contains(cat));
        }
    }

    /**
     * Main function to check if a debug message should be shown.
     */
    public static boolean shouldShowDebugMessage(String message, FilterConfig filter) {
        // Fast path: no filter means show everything
        if (filter == null) {
            return true;
        }

        List<String> categories = extractDebugCategories(message);
        return shouldShowDebugCategories(categories, filter);
    }

    /**
     * Get filter from environment variable.
     */
    public static FilterConfig getFilterFromEnv() {
        String filterString = System.getenv("CLAUDE_DEBUG_FILTER");
        return parseDebugFilter(filterString);
    }
}