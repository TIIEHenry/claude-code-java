/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/contentArray.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Utility for inserting a block into a content array relative to tool_result
 * blocks. Used by the API layer to position supplementary content correctly.
 */
public final class ContentArrayUtils {
    private ContentArrayUtils() {}

    /**
     * Inserts a block into the content array after the last tool_result block.
     * Mutates the list in place.
     *
     * Placement rules:
     * - If tool_result blocks exist: insert after the last one
     * - Otherwise: insert before the last block
     * - If the inserted block would be the final element, a text continuation
     *   block is appended (some APIs require the prompt not to end with non-text content)
     */
    public static void insertBlockAfterToolResults(List<Map<String, Object>> content, Map<String, Object> block) {
        // Find position after the last tool_result block
        int lastToolResultIndex = -1;
        for (int i = 0; i < content.size(); i++) {
            Map<String, Object> item = content.get(i);
            if (item != null && "tool_result".equals(item.get("type"))) {
                lastToolResultIndex = i;
            }
        }

        if (lastToolResultIndex >= 0) {
            int insertPos = lastToolResultIndex + 1;
            content.add(insertPos, block);
            // Append a text continuation if the inserted block is now last
            if (insertPos == content.size() - 1) {
                Map<String, Object> continuation = new LinkedHashMap<>();
                continuation.put("type", "text");
                continuation.put("text", ".");
                content.add(continuation);
            }
        } else {
            // No tool_result blocks — insert before the last block
            int insertIndex = Math.max(0, content.size() - 1);
            content.add(insertIndex, block);
        }
    }

    /**
     * Find the index of the last tool_result block.
     */
    public static int findLastToolResultIndex(List<Map<String, Object>> content) {
        int lastIndex = -1;
        for (int i = 0; i < content.size(); i++) {
            Map<String, Object> item = content.get(i);
            if (item != null && "tool_result".equals(item.get("type"))) {
                lastIndex = i;
            }
        }
        return lastIndex;
    }

    /**
     * Check if content contains tool_result blocks.
     */
    public static boolean hasToolResults(List<Map<String, Object>> content) {
        for (Map<String, Object> item : content) {
            if (item != null && "tool_result".equals(item.get("type"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a text block.
     */
    public static Map<String, Object> textBlock(String text) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("type", "text");
        block.put("text", text);
        return block;
    }

    /**
     * Create a tool_result block.
     */
    public static Map<String, Object> toolResultBlock(String toolUseId, String content) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("type", "tool_result");
        block.put("tool_use_id", toolUseId);
        block.put("content", content);
        return block;
    }
}