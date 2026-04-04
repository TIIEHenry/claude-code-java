/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code sanitization utilities
 */
package com.anthropic.claudecode.utils.security;

import java.text.*;
import java.util.*;
import java.util.regex.*;

/**
 * Unicode Sanitization for Hidden Character Attack Mitigation.
 *
 * This module implements security measures against Unicode-based hidden character attacks,
 * specifically targeting ASCII Smuggling and Hidden Prompt Injection vulnerabilities.
 * These attacks use invisible Unicode characters (such as Tag characters, format controls,
 * private use areas, and noncharacters) to hide malicious instructions that are invisible
 * to users but processed by AI models.
 *
 * Reference: https://embracethered.com/blog/posts/2024/hiding-and-finding-text-with-unicode-tags/
 */
public final class Sanitization {
    private Sanitization() {}

    private static final int MAX_ITERATIONS = 10;

    // Regex patterns for dangerous Unicode categories
    // Format characters (Cf), Private use (Co), Unassigned (Cn)
    private static final Pattern DANGEROUS_CATEGORIES = Pattern.compile(
            "[\\p{Cf}\\p{Co}\\p{Cn}]",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    // Zero-width spaces and directional marks
    private static final Pattern ZERO_WIDTH_DIRECTIONAL = Pattern.compile(
            "[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF]"
    );

    // Basic Multilingual Plane private use area
    private static final Pattern PRIVATE_USE_BMP = Pattern.compile(
            "[\\uE000-\\uF8FF]"
    );

    /**
     * Partially sanitize Unicode from a string.
     * Applies NFKC normalization and removes dangerous Unicode characters.
     */
    public static String partiallySanitizeUnicode(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return prompt;
        }

        String current = prompt;
        String previous = "";
        int iterations = 0;

        // Iteratively sanitize until no more changes occur or max iterations reached
        while (!current.equals(previous) && iterations < MAX_ITERATIONS) {
            previous = current;

            // Apply NFKC normalization to handle composed character sequences
            current = Normalizer.normalize(current, Normalizer.Form.NFKC);

            // Method 1: Strip dangerous Unicode property classes
            current = DANGEROUS_CATEGORIES.matcher(current).replaceAll("");

            // Method 2: Explicit character ranges fallback
            current = ZERO_WIDTH_DIRECTIONAL.matcher(current).replaceAll("");
            current = PRIVATE_USE_BMP.matcher(current).replaceAll("");

            iterations++;
        }

        // If we hit max iterations, throw an error
        if (iterations >= MAX_ITERATIONS) {
            throw new RuntimeException(
                    "Unicode sanitization reached maximum iterations (" + MAX_ITERATIONS +
                    ") for input: " + prompt.substring(0, Math.min(100, prompt.length()))
            );
        }

        return current;
    }

    /**
     * Recursively sanitize Unicode from strings in any data structure.
     */
    public static String recursivelySanitizeUnicode(String value) {
        return partiallySanitizeUnicode(value);
    }

    /**
     * Recursively sanitize Unicode from strings in a list.
     */
    public static <T> List<T> recursivelySanitizeUnicode(List<T> list) {
        if (list == null) return null;

        List<T> result = new ArrayList<>(list.size());
        for (T item : list) {
            result.add(sanitizeValue(item));
        }
        return result;
    }

    /**
     * Recursively sanitize Unicode from strings in a map.
     */
    public static Map<String, Object> recursivelySanitizeUnicode(Map<String, Object> map) {
        if (map == null) return null;

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String sanitizedKey = partiallySanitizeUnicode(entry.getKey());
            Object sanitizedValue = sanitizeValue(entry.getValue());
            result.put(sanitizedKey, sanitizedValue);
        }
        return result;
    }

    /**
     * Sanitize any value based on its type.
     */
    @SuppressWarnings("unchecked")
    private static <T> T sanitizeValue(T value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return (T) partiallySanitizeUnicode((String) value);
        }

        if (value instanceof List) {
            return (T) recursivelySanitizeUnicode((List<Object>) value);
        }

        if (value instanceof Map) {
            return (T) recursivelySanitizeUnicode((Map<String, Object>) value);
        }

        // Return other primitive values unchanged
        return value;
    }

    /**
     * Check if a string contains potentially dangerous Unicode characters.
     */
    public static boolean containsDangerousUnicode(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        return DANGEROUS_CATEGORIES.matcher(text).find() ||
               ZERO_WIDTH_DIRECTIONAL.matcher(text).find() ||
               PRIVATE_USE_BMP.matcher(text).find();
    }

    /**
     * Get count of dangerous Unicode characters removed during sanitization.
     */
    public static int countDangerousCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int count = 0;

        // Count matches in each pattern
        Matcher m1 = DANGEROUS_CATEGORIES.matcher(text);
        while (m1.find()) count++;

        Matcher m2 = ZERO_WIDTH_DIRECTIONAL.matcher(text);
        while (m2.find()) count++;

        Matcher m3 = PRIVATE_USE_BMP.matcher(text);
        while (m3.find()) count++;

        return count;
    }
}