/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code MCP validation utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * MCP output validation and truncation utilities.
 */
public final class McpValidation {
    private McpValidation() {}

    public static final double MCP_TOKEN_COUNT_THRESHOLD_FACTOR = 0.5;
    public static final int IMAGE_TOKEN_ESTIMATE = 1600;
    private static final int DEFAULT_MAX_MCP_OUTPUT_TOKENS = 25000;

    /**
     * Get the maximum MCP output token limit.
     */
    public static int getMaxMcpOutputTokens() {
        String envValue = System.getenv("MAX_MCP_OUTPUT_TOKENS");
        if (envValue != null) {
            try {
                int parsed = Integer.parseInt(envValue);
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_MAX_MCP_OUTPUT_TOKENS;
    }

    /**
     * MCP tool result.
     */
    public sealed interface McpToolResult permits McpToolResult.StringResult, McpToolResult.BlocksResult {
        String getContentSummary();

        public record StringResult(String content) implements McpToolResult {
            @Override
            public String getContentSummary() { return content; }
        }

        public record BlocksResult(List<ContentBlock> blocks) implements McpToolResult {
            @Override
            public String getContentSummary() { return blocks.size() + " blocks"; }
        }
    }

    /**
     * Content block interface.
     */
    public sealed interface ContentBlock permits TextBlock, ImageBlock {}

    /**
     * Text content block.
     */
    public record TextBlock(String type, String text) implements ContentBlock {
        public TextBlock(String text) {
            this("text", text);
        }
    }

    /**
     * Image content block.
     */
    public record ImageBlock(String type, String mediaType, String data) implements ContentBlock {
        public ImageBlock(String mediaType, String data) {
            this("image", mediaType, data);
        }
    }

    /**
     * Get content size estimate in tokens.
     */
    public static int getContentSizeEstimate(McpToolResult content) {
        if (content == null) return 0;

        if (content instanceof McpToolResult.StringResult str) {
            return roughTokenCountEstimation(str.content());
        }

        if (content instanceof McpToolResult.BlocksResult blocks) {
            int total = 0;
            for (ContentBlock block : blocks.blocks()) {
                if (block instanceof TextBlock text) {
                    total += roughTokenCountEstimation(text.text());
                } else if (block instanceof ImageBlock) {
                    total += IMAGE_TOKEN_ESTIMATE;
                }
            }
            return total;
        }

        return 0;
    }

    /**
     * Check if MCP content needs truncation.
     */
    public static boolean mcpContentNeedsTruncation(McpToolResult content) {
        if (content == null) return false;

        int estimate = getContentSizeEstimate(content);
        return estimate > getMaxMcpOutputTokens() * MCP_TOKEN_COUNT_THRESHOLD_FACTOR;
    }

    /**
     * Truncate MCP content if needed.
     */
    public static McpToolResult truncateMcpContentIfNeeded(McpToolResult content) {
        if (!mcpContentNeedsTruncation(content)) {
            return content;
        }
        return truncateMcpContent(content);
    }

    /**
     * Truncate MCP content.
     */
    public static McpToolResult truncateMcpContent(McpToolResult content) {
        if (content == null) return null;

        int maxChars = getMaxMcpOutputTokens() * 4;
        String truncationMsg = getTruncationMessage();

        if (content instanceof McpToolResult.StringResult str) {
            String truncated = str.content();
            if (truncated.length() > maxChars) {
                truncated = truncated.substring(0, maxChars);
            }
            return new McpToolResult.StringResult(truncated + truncationMsg);
        }

        if (content instanceof McpToolResult.BlocksResult blocks) {
            List<ContentBlock> result = new ArrayList<>();
            int currentChars = 0;

            for (ContentBlock block : blocks.blocks()) {
                int remainingChars = maxChars - currentChars;
                if (remainingChars <= 0) break;

                if (block instanceof TextBlock text) {
                    if (text.text().length() <= remainingChars) {
                        result.add(text);
                        currentChars += text.text().length();
                    } else {
                        result.add(new TextBlock(text.text().substring(0, remainingChars)));
                        break;
                    }
                } else if (block instanceof ImageBlock) {
                    int imageChars = IMAGE_TOKEN_ESTIMATE * 4;
                    if (currentChars + imageChars <= maxChars) {
                        result.add(block);
                        currentChars += imageChars;
                    }
                }
            }

            result.add(new TextBlock(truncationMsg));
            return new McpToolResult.BlocksResult(result);
        }

        return content;
    }

    /**
     * Get truncation message.
     */
    private static String getTruncationMessage() {
        return String.format("\n\n[OUTPUT TRUNCATED - exceeded %d token limit]\n\n" +
                "The tool output was truncated. If this MCP server provides pagination or filtering tools, " +
                "use them to retrieve specific portions of the data.", getMaxMcpOutputTokens());
    }

    /**
     * Rough token count estimation (4 chars per token).
     */
    private static int roughTokenCountEstimation(String text) {
        return text != null ? text.length() / 4 : 0;
    }
}