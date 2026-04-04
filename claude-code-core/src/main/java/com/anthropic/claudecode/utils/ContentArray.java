/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code content array utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Utility for inserting blocks into a content array relative to tool_result
 * blocks. Used by the API layer to position supplementary content correctly
 * within user messages.
 */
public final class ContentArray {
    private ContentArray() {}

    /**
     * Content block representation.
     */
    public interface ContentBlock {
        String type();
    }

    /**
     * Text content block.
     */
    public record TextBlock(String type, String text) implements ContentBlock {
        public TextBlock(String text) {
            this("text", text);
        }
    }

    /**
     * Tool result content block.
     */
    public record ToolResultBlock(String type, String toolUseId, String content) implements ContentBlock {
        public ToolResultBlock(String toolUseId, String content) {
            this("tool_result", toolUseId, content);
        }
    }

    /**
     * Inserts a block into the content array after the last tool_result block.
     * Mutates the list in place.
     *
     * Placement rules:
     * - If tool_result blocks exist: insert after the last one
     * - Otherwise: insert before the last block
     * - If the inserted block would be the final element, a text continuation
     *   block is appended (some APIs require the prompt not to end with
     *   non-text content)
     *
     * @param content The content list to modify
     * @param block The block to insert
     */
    public static void insertBlockAfterToolResults(List<ContentBlock> content, ContentBlock block) {
        if (content == null || block == null) {
            return;
        }

        // Find position after the last tool_result block
        int lastToolResultIndex = -1;
        for (int i = 0; i < content.size(); i++) {
            ContentBlock item = content.get(i);
            if (item != null && "tool_result".equals(item.type())) {
                lastToolResultIndex = i;
            }
        }

        if (lastToolResultIndex >= 0) {
            int insertPos = lastToolResultIndex + 1;
            content.add(insertPos, block);
            // Append a text continuation if the inserted block is now last
            if (insertPos == content.size() - 1) {
                content.add(new TextBlock("."));
            }
        } else {
            // No tool_result blocks — insert before the last block
            int insertIndex = Math.max(0, content.size() - 1);
            content.add(insertIndex, block);
        }
    }

    /**
     * Insert a generic object block after tool results.
     */
    public static void insertBlockAfterToolResults(List<Map<String, Object>> content, Map<String, Object> block) {
        if (content == null || block == null) {
            return;
        }

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
                Map<String, Object> textBlock = new LinkedHashMap<>();
                textBlock.put("type", "text");
                textBlock.put("text", ".");
                content.add(textBlock);
            }
        } else {
            // No tool_result blocks — insert before the last block
            int insertIndex = Math.max(0, content.size() - 1);
            content.add(insertIndex, block);
        }
    }

    /**
     * Find all tool_result blocks in content.
     */
    public static List<Map<String, Object>> findToolResults(List<Map<String, Object>> content) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (content == null) {
            return results;
        }
        for (Map<String, Object> block : content) {
            if (block != null && "tool_result".equals(block.get("type"))) {
                results.add(block);
            }
        }
        return results;
    }

    /**
     * Check if content has tool_result blocks.
     */
    public static boolean hasToolResults(List<Map<String, Object>> content) {
        if (content == null) {
            return false;
        }
        for (Map<String, Object> block : content) {
            if (block != null && "tool_result".equals(block.get("type"))) {
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

    /**
     * Create an image block.
     */
    public static Map<String, Object> imageBlock(String mediaType, String data) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("type", "image");
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("type", "base64");
        source.put("media_type", mediaType);
        source.put("data", data);
        block.put("source", source);
        return block;
    }
}