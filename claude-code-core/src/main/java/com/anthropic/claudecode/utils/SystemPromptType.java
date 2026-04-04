/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code system prompt type
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Branded type for system prompt arrays.
 * This module is intentionally dependency-free so it can be imported
 * from anywhere without risking circular initialization issues.
 */
public final class SystemPromptType {
    private SystemPromptType() {}

    /**
     * System prompt - an immutable list of strings.
     */
    public record SystemPrompt(List<String> parts) {
        public SystemPrompt {
            parts = Collections.unmodifiableList(new ArrayList<>(parts));
        }

        /**
         * Create from varargs.
         */
        public static SystemPrompt of(String... parts) {
            return new SystemPrompt(Arrays.asList(parts));
        }

        /**
         * Create from list.
         */
        public static SystemPrompt from(List<String> parts) {
            return new SystemPrompt(parts);
        }

        /**
         * Create an empty system prompt.
         */
        public static SystemPrompt empty() {
            return new SystemPrompt(Collections.emptyList());
        }

        /**
         * Join all parts into a single string.
         */
        public String join() {
            return String.join("\n", parts);
        }

        /**
         * Join with custom delimiter.
         */
        public String join(String delimiter) {
            return String.join(delimiter, parts);
        }

        /**
         * Get the number of parts.
         */
        public int size() {
            return parts.size();
        }

        /**
         * Check if empty.
         */
        public boolean isEmpty() {
            return parts.isEmpty();
        }

        /**
         * Get a part by index.
         */
        public String get(int index) {
            return parts.get(index);
        }

        /**
         * Append a part and return a new SystemPrompt.
         */
        public SystemPrompt append(String part) {
            List<String> newParts = new ArrayList<>(parts);
            newParts.add(part);
            return new SystemPrompt(newParts);
        }

        /**
         * Append multiple parts and return a new SystemPrompt.
         */
        public SystemPrompt appendAll(List<String> moreParts) {
            List<String> newParts = new ArrayList<>(parts);
            newParts.addAll(moreParts);
            return new SystemPrompt(newParts);
        }

        /**
         * Concat with another SystemPrompt.
         */
        public SystemPrompt concat(SystemPrompt other) {
            List<String> newParts = new ArrayList<>(parts);
            newParts.addAll(other.parts);
            return new SystemPrompt(newParts);
        }

        @Override
        public String toString() {
            return join();
        }
    }

    /**
     * Convert a list of strings to a SystemPrompt.
     */
    public static SystemPrompt asSystemPrompt(List<String> parts) {
        return SystemPrompt.from(parts);
    }

    /**
     * Convert varargs to a SystemPrompt.
     */
    public static SystemPrompt asSystemPrompt(String... parts) {
        return SystemPrompt.of(parts);
    }
}