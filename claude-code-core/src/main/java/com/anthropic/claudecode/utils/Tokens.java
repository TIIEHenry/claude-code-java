/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/tokens.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import com.anthropic.claudecode.types.MessageTypes;

/**
 * Token counting utilities.
 */
public final class Tokens {
    private Tokens() {}

    // Model context window sizes
    public static final int CLAUDE_3_5_SONNET_CONTEXT = 200_000;
    public static final int CLAUDE_3_5_HAIKU_CONTEXT = 200_000;
    public static final int CLAUDE_3_OPUS_CONTEXT = 200_000;
    public static final int CLAUDE_SONNET_4_6_CONTEXT = 200_000;
    public static final int CLAUDE_OPUS_4_6_CONTEXT = 200_000;

    // Token estimation constants
    public static final int CHARS_PER_TOKEN = 4; // Rough estimate
    public static final int MESSAGE_OVERHEAD_TOKENS = 10;

    /**
     * Token usage record.
     */
    public record TokenUsage(
            int inputTokens,
            int outputTokens,
            int cacheCreationInputTokens,
            int cacheReadInputTokens
    ) {
        public int total() {
            return inputTokens + outputTokens + cacheCreationInputTokens + cacheReadInputTokens;
        }

        public static TokenUsage empty() {
            return new TokenUsage(0, 0, 0, 0);
        }
    }

    /**
     * Calculate total context window tokens from usage data.
     */
    public static int getTokenCountFromUsage(TokenUsage usage) {
        return usage.inputTokens() +
               usage.cacheCreationInputTokens() +
               usage.cacheReadInputTokens() +
               usage.outputTokens();
    }

    /**
     * Estimate tokens from string content.
     * Uses rough 4 chars per token estimation.
     */
    public static int estimateTokens(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.length() / CHARS_PER_TOKEN + MESSAGE_OVERHEAD_TOKENS;
    }

    /**
     * Estimate tokens for a list of messages.
     */
    public static int estimateTokensForMessages(List<Map<String, Object>> messages) {
        int total = 0;
        for (Map<String, Object> message : messages) {
            total += estimateMessageTokens(message);
        }
        return total;
    }

    private static int estimateMessageTokens(Map<String, Object> message) {
        Object content = message.get("content");
        if (content instanceof String) {
            return estimateTokens((String) content);
        } else if (content instanceof List) {
            int tokens = 0;
            for (Object block : (List<?>) content) {
                if (block instanceof Map) {
                    Object text = ((Map<?, ?>) block).get("text");
                    if (text instanceof String) {
                        tokens += estimateTokens((String) text);
                    }
                }
            }
            return tokens + MESSAGE_OVERHEAD_TOKENS;
        }
        return MESSAGE_OVERHEAD_TOKENS;
    }

    /**
     * Get context window size for a model.
     */
    public static int getContextWindowSize(String modelId) {
        if (modelId == null) {
            return CLAUDE_3_5_SONNET_CONTEXT;
        }

        String canonical = modelId.toLowerCase();
        if (canonical.contains("opus-4-6")) {
            return CLAUDE_OPUS_4_6_CONTEXT;
        } else if (canonical.contains("sonnet-4-6")) {
            return CLAUDE_SONNET_4_6_CONTEXT;
        } else if (canonical.contains("opus")) {
            return CLAUDE_3_OPUS_CONTEXT;
        } else if (canonical.contains("haiku")) {
            return CLAUDE_3_5_HAIKU_CONTEXT;
        }

        return CLAUDE_3_5_SONNET_CONTEXT;
    }

    /**
     * Check if token count is approaching context limit.
     */
    public static boolean isApproachingLimit(int currentTokens, int maxTokens) {
        return currentTokens > maxTokens * 0.9; // 90% threshold
    }

    /**
     * Calculate remaining tokens in context window.
     */
    public static int getRemainingTokens(int currentTokens, String modelId) {
        int maxTokens = getContextWindowSize(modelId);
        return Math.max(0, maxTokens - currentTokens);
    }

    /**
     * Estimate token count for a list of messages.
     * This is an approximation based on message content.
     */
    public static int tokenCountWithEstimation(List<MessageTypes.Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }

        int total = 0;
        for (MessageTypes.Message message : messages) {
            total += estimateMessageTokensFromType(message);
        }
        return total;
    }

    private static int estimateMessageTokensFromType(MessageTypes.Message message) {
        if (message instanceof MessageTypes.UserMessage user) {
            List<Map<String, Object>> content = user.content();
            if (content == null) return MESSAGE_OVERHEAD_TOKENS;
            int tokens = 0;
            for (Map<String, Object> block : content) {
                Object text = block.get("text");
                if (text instanceof String) {
                    tokens += estimateTokens((String) text);
                }
            }
            return tokens + MESSAGE_OVERHEAD_TOKENS;
        } else if (message instanceof MessageTypes.AssistantMessage assistant) {
            List<Map<String, Object>> content = assistant.content();
            if (content == null) return MESSAGE_OVERHEAD_TOKENS;
            int tokens = 0;
            for (Map<String, Object> block : content) {
                Object text = block.get("text");
                if (text instanceof String) {
                    tokens += estimateTokens((String) text);
                }
                Object thinking = block.get("thinking");
                if (thinking instanceof String) {
                    tokens += estimateTokens((String) thinking);
                }
            }
            return tokens + MESSAGE_OVERHEAD_TOKENS;
        } else if (message instanceof MessageTypes.SystemMessage sys) {
            return estimateTokens(sys.content()) + MESSAGE_OVERHEAD_TOKENS;
        }
        return MESSAGE_OVERHEAD_TOKENS;
    }
}