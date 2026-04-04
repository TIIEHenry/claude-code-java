/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code context analysis utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Context analysis utilities.
 */
public final class ContextAnalysis {
    private ContextAnalysis() {}

    /**
     * Token statistics.
     */
    public record TokenStats(
            int inputTokens,
            int outputTokens,
            int totalTokens,
            Map<String, Integer> toolRequests,
            Map<String, Integer> toolResults
    ) {}

    /**
     * Analyze context from messages.
     */
    public static TokenStats analyzeContext(List<Map<String, Object>> messages) {
        int inputTokens = 0;
        int outputTokens = 0;
        Map<String, Integer> toolRequests = new LinkedHashMap<>();
        Map<String, Integer> toolResults = new LinkedHashMap<>();

        for (Map<String, Object> message : messages) {
            String role = (String) message.get("role");
            Object content = message.get("content");

            if ("user".equals(role)) {
                inputTokens += estimateTokens(content);
                // Count tool results
                if (content instanceof List) {
                    for (Object block : (List<?>) content) {
                        if (block instanceof Map) {
                            Map<?, ?> blockMap = (Map<?, ?>) block;
                            if ("tool_result".equals(blockMap.get("type"))) {
                                String toolUseId = (String) blockMap.get("tool_use_id");
                                toolResults.merge(toolUseId, 1, Integer::sum);
                            }
                        }
                    }
                }
            } else if ("assistant".equals(role)) {
                outputTokens += estimateTokens(content);
                // Count tool uses
                if (content instanceof List) {
                    for (Object block : (List<?>) content) {
                        if (block instanceof Map) {
                            Map<?, ?> blockMap = (Map<?, ?>) block;
                            if ("tool_use".equals(blockMap.get("type"))) {
                                String toolName = (String) blockMap.get("name");
                                toolRequests.merge(toolName, 1, Integer::sum);
                            }
                        }
                    }
                }
            }
        }

        return new TokenStats(
                inputTokens,
                outputTokens,
                inputTokens + outputTokens,
                toolRequests,
                toolResults
        );
    }

    /**
     * Estimate tokens from content.
     */
    public static int estimateTokens(Object content) {
        if (content == null) return 0;

        if (content instanceof String) {
            return estimateTokensFromString((String) content);
        }

        if (content instanceof List) {
            int total = 0;
            for (Object block : (List<?>) content) {
                if (block instanceof Map) {
                    Map<?, ?> blockMap = (Map<?, ?>) block;
                    Object text = blockMap.get("text");
                    if (text instanceof String) {
                        total += estimateTokensFromString((String) text);
                    }
                }
            }
            return total;
        }

        return 0;
    }

    /**
     * Estimate tokens from string (rough approximation).
     * Uses ~4 characters per token as a rough estimate.
     */
    public static int estimateTokensFromString(String text) {
        if (text == null || text.isEmpty()) return 0;
        return text.length() / 4;
    }

    /**
     * Get context window usage percentage.
     */
    public static double getContextUsagePercentage(int usedTokens, int maxTokens) {
        if (maxTokens <= 0) return 0;
        return (double) usedTokens / maxTokens * 100;
    }

    /**
     * Check if context is near limit.
     */
    public static boolean isContextNearLimit(int usedTokens, int maxTokens, double threshold) {
        return getContextUsagePercentage(usedTokens, maxTokens) >= threshold;
    }
}