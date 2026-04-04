/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/tokenEstimation.ts
 */
package com.anthropic.claudecode.services;

import java.util.*;

/**
 * Token estimation service.
 */
public final class TokenEstimation {
    private TokenEstimation() {}

    // Approximation constants
    private static final int CHARS_PER_TOKEN = 4;
    private static final int WORDS_PER_TOKEN = 3;
    private static final int MESSAGE_OVERHEAD = 4;
    private static final int TOOL_CALL_OVERHEAD = 10;

    /**
     * Estimate tokens from a string.
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Count by characters
        int charTokens = text.length() / CHARS_PER_TOKEN;

        // Count by words
        String[] words = text.split("\\s+");
        int wordTokens = words.length / WORDS_PER_TOKEN;

        // Use the larger estimate
        return Math.max(charTokens, wordTokens) + MESSAGE_OVERHEAD;
    }

    /**
     * Estimate tokens for a list of messages.
     */
    public static int estimateTokensForMessages(List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }

        int total = 0;
        for (Map<String, Object> message : messages) {
            total += estimateTokensForMessage(message);
        }

        return total;
    }

    /**
     * Estimate tokens for a single message.
     */
    public static int estimateTokensForMessage(Map<String, Object> message) {
        if (message == null) {
            return 0;
        }

        int tokens = MESSAGE_OVERHEAD;

        // Role overhead
        String role = (String) message.get("role");
        if (role != null) {
            tokens += 1;
        }

        // Content
        Object content = message.get("content");
        if (content instanceof String) {
            tokens += estimateTokens((String) content);
        } else if (content instanceof List) {
            tokens += estimateTokensForContent((List<?>) content);
        }

        return tokens;
    }

    /**
     * Estimate tokens for content blocks.
     */
    private static int estimateTokensForContent(List<?> content) {
        int tokens = 0;

        for (Object block : content) {
            if (block instanceof Map) {
                Map<?, ?> blockMap = (Map<?, ?>) block;
                String type = (String) blockMap.get("type");

                if ("text".equals(type)) {
                    String text = (String) blockMap.get("text");
                    tokens += estimateTokens(text);
                } else if ("image".equals(type)) {
                    // Images are counted based on size
                    tokens += 85; // Minimum for small images
                } else if ("tool_use".equals(type)) {
                    tokens += TOOL_CALL_OVERHEAD;
                    String name = (String) blockMap.get("name");
                    if (name != null) {
                        tokens += estimateTokens(name);
                    }
                    Object input = blockMap.get("input");
                    if (input instanceof Map) {
                        tokens += estimateTokensForMap((Map<?, ?>) input);
                    }
                } else if ("tool_result".equals(type)) {
                    tokens += TOOL_CALL_OVERHEAD;
                    Object resultContent = blockMap.get("content");
                    if (resultContent instanceof String) {
                        tokens += estimateTokens((String) resultContent);
                    } else if (resultContent instanceof List) {
                        tokens += estimateTokensForContent((List<?>) resultContent);
                    }
                } else if ("thinking".equals(type)) {
                    String thinking = (String) blockMap.get("thinking");
                    tokens += estimateTokens(thinking);
                }
            }
        }

        return tokens;
    }

    /**
     * Estimate tokens for a map/object.
     */
    private static int estimateTokensForMap(Map<?, ?> map) {
        int tokens = 2; // {} overhead

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                tokens += estimateTokens(String.valueOf(entry.getKey()));
            }
            if (entry.getValue() != null) {
                if (entry.getValue() instanceof String) {
                    tokens += estimateTokens((String) entry.getValue());
                } else if (entry.getValue() instanceof Map) {
                    tokens += estimateTokensForMap((Map<?, ?>) entry.getValue());
                }
            }
            tokens += 1; // : and , overhead
        }

        return tokens;
    }

    /**
     * Rough token count estimation for quick calculations.
     */
    public static int roughTokenCountEstimation(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / CHARS_PER_TOKEN;
    }

    /**
     * Estimate tokens for a system prompt.
     */
    public static int estimateSystemPromptTokens(String systemPrompt) {
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            return 0;
        }
        // System prompts have additional overhead
        return estimateTokens(systemPrompt) + 10;
    }

    /**
     * Estimate tokens for tools definition.
     */
    public static int estimateToolsTokens(List<Map<String, Object>> tools) {
        if (tools == null || tools.isEmpty()) {
            return 0;
        }

        int tokens = 0;
        for (Map<String, Object> tool : tools) {
            tokens += TOOL_CALL_OVERHEAD;
            String name = (String) tool.get("name");
            if (name != null) {
                tokens += estimateTokens(name);
            }
            String description = (String) tool.get("description");
            if (description != null) {
                tokens += estimateTokens(description);
            }
            // Schema estimation
            Object schema = tool.get("input_schema");
            if (schema instanceof Map) {
                tokens += estimateTokensForMap((Map<?, ?>) schema);
            }
        }

        return tokens;
    }
}