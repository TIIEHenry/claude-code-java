/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/attribution
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Attribution utilities - Code attribution tracking.
 */
public final class AttributionUtils {
    /**
     * Create an attribution for a code change.
     */
    public static Attribution create(String source, String author) {
        return new Attribution(
            UUID.randomUUID().toString(),
            source,
            author,
            System.currentTimeMillis()
        );
    }

    /**
     * Create an attribution for AI-generated code.
     */
    public static Attribution forAI(String modelId, String sessionId) {
        return new Attribution(
            UUID.randomUUID().toString(),
            "ai:" + modelId,
            sessionId,
            System.currentTimeMillis()
        );
    }

    /**
     * Create an attribution for user code.
     */
    public static Attribution forUser(String userId) {
        return new Attribution(
            UUID.randomUUID().toString(),
            "user",
            userId,
            System.currentTimeMillis()
        );
    }

    /**
     * Parse attribution from string.
     */
    public static Attribution parse(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }

        String[] parts = str.split("\\|");
        if (parts.length < 3) {
            return null;
        }

        try {
            return new Attribution(
                parts[0],
                parts[1],
                parts[2],
                parts.length > 3 ? Long.parseLong(parts[3]) : System.currentTimeMillis()
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if source is AI.
     */
    public static boolean isAI(String source) {
        return source != null && source.startsWith("ai:");
    }

    /**
     * Check if source is user.
     */
    public static boolean isUser(String source) {
        return "user".equals(source);
    }

    /**
     * Get model ID from AI source.
     */
    public static String getModelId(String source) {
        if (!isAI(source)) return null;
        return source.substring(3);
    }

    /**
     * Merge multiple attributions.
     */
    public static Attribution merge(List<Attribution> attributions) {
        if (attributions == null || attributions.isEmpty()) {
            return null;
        }

        if (attributions.size() == 1) {
            return attributions.get(0);
        }

        // Return the most recent attribution
        return attributions.stream()
            .max(Comparator.comparingLong(Attribution::timestamp))
            .orElse(null);
    }

    /**
     * Attribution record.
     */
    public record Attribution(
        String id,
        String source,
        String author,
        long timestamp
    ) {
        /**
         * Serialize to string.
         */
        public String serialize() {
            return id + "|" + source + "|" + author + "|" + timestamp;
        }

        /**
         * Check if this is AI attribution.
         */
        public boolean isAI() {
            return AttributionUtils.isAI(source);
        }

        /**
         * Check if this is user attribution.
         */
        public boolean isUser() {
            return AttributionUtils.isUser(source);
        }

        /**
         * Get model ID if AI attribution.
         */
        public String getModelId() {
            return AttributionUtils.getModelId(source);
        }
    }

    /**
     * Attribution block for code.
     */
    public record AttributionBlock(
        String filePath,
        int startLine,
        int endLine,
        Attribution attribution
    ) {
        public int lineCount() {
            return endLine - startLine + 1;
        }
    }
}