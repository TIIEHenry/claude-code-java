/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/compact/compact.ts
 */
package com.anthropic.claudecode.services.compact;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Context compaction service.
 * Reduces conversation context by summarizing older messages.
 */
public final class CompactService {
    private CompactService() {}

    // Constants
    public static final int POST_COMPACT_MAX_FILES_TO_RESTORE = 5;
    public static final int POST_COMPACT_TOKEN_BUDGET = 50_000;
    public static final int POST_COMPACT_MAX_TOKENS_PER_FILE = 5_000;
    public static final int POST_COMPACT_MAX_TOKENS_PER_SKILL = 5_000;
    public static final int POST_COMPACT_SKILLS_TOKEN_BUDGET = 25_000;
    public static final int MAX_COMPACT_STREAMING_RETRIES = 2;

    /**
     * Compaction options.
     */
    public record CompactOptions(
        boolean force,
        boolean partial,
        int maxOutputTokens,
        String customPrompt
    ) {
        public static CompactOptions defaults() {
            return new CompactOptions(false, false, 4096, null);
        }
    }

    /**
     * Compaction result.
     */
    public record CompactResult(
        boolean success,
        int tokensBefore,
        int tokensAfter,
        int messagesRemoved,
        int messagesKept,
        String summary,
        String error
    ) {
        public static CompactResult success(int before, int after, int removed, int kept, String summary) {
            return new CompactResult(true, before, after, removed, kept, summary, null);
        }

        public static CompactResult failure(String error) {
            return new CompactResult(false, 0, 0, 0, 0, null, error);
        }
    }

    /**
     * Strip images from messages for compaction.
     */
    public static List<Map<String, Object>> stripImagesFromMessages(List<Map<String, Object>> messages) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> message : messages) {
            String type = (String) message.get("type");
            if (!"user".equals(type)) {
                result.add(message);
                continue;
            }

            Object content = message.get("content");
            if (!(content instanceof List)) {
                result.add(message);
                continue;
            }

            List<?> contentList = (List<?>) content;
            List<Object> newContent = new ArrayList<>();
            boolean hasMediaBlock = false;

            for (Object block : contentList) {
                if (block instanceof Map) {
                    Map<?, ?> blockMap = (Map<?, ?>) block;
                    String blockType = (String) blockMap.get("type");

                    if ("image".equals(blockType)) {
                        hasMediaBlock = true;
                        newContent.add(Map.of("type", "text", "text", "[image]"));
                    } else if ("document".equals(blockType)) {
                        hasMediaBlock = true;
                        newContent.add(Map.of("type", "text", "text", "[document]"));
                    } else if ("tool_result".equals(blockType) && blockMap.get("content") instanceof List) {
                        // Handle nested images in tool_result
                        List<?> toolContent = (List<?>) blockMap.get("content");
                        List<Object> newToolContent = new ArrayList<>();
                        boolean toolHasMedia = false;

                        for (Object item : toolContent) {
                            if (item instanceof Map) {
                                Map<?, ?> itemMap = (Map<?, ?>) item;
                                String itemType = (String) itemMap.get("type");
                                if ("image".equals(itemType)) {
                                    toolHasMedia = true;
                                    newToolContent.add(Map.of("type", "text", "text", "[image]"));
                                } else if ("document".equals(itemType)) {
                                    toolHasMedia = true;
                                    newToolContent.add(Map.of("type", "text", "text", "[document]"));
                                } else {
                                    newToolContent.add(item);
                                }
                            } else {
                                newToolContent.add(item);
                            }
                        }

                        if (toolHasMedia) {
                            hasMediaBlock = true;
                            Map<String, Object> newBlock = new HashMap<>((Map<String, Object>) block);
                            newBlock.put("content", newToolContent);
                            newContent.add(newBlock);
                        } else {
                            newContent.add(block);
                        }
                    } else {
                        newContent.add(block);
                    }
                } else {
                    newContent.add(block);
                }
            }

            if (hasMediaBlock) {
                Map<String, Object> newMessage = new HashMap<>(message);
                newMessage.put("content", newContent);
                result.add(newMessage);
            } else {
                result.add(message);
            }
        }

        return result;
    }

    /**
     * Check if compaction is needed based on token count.
     */
    public static boolean needsCompaction(int currentTokens, int maxTokens) {
        return currentTokens > maxTokens * 0.9; // 90% threshold
    }

    /**
     * Estimate tokens for a message list.
     */
    public static int estimateTokens(List<Map<String, Object>> messages) {
        int total = 0;
        for (Map<String, Object> message : messages) {
            total += estimateMessageTokens(message);
        }
        return total;
    }

    private static int estimateMessageTokens(Map<String, Object> message) {
        // Rough estimation: 4 characters per token
        String content = String.valueOf(message.get("content"));
        return content.length() / 4 + 10; // +10 for message overhead
    }
}