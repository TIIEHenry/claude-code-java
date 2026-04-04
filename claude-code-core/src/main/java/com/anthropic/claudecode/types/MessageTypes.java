/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code types/message.ts
 */
package com.anthropic.claudecode.types;

import java.util.*;

/**
 * Message types for conversation handling.
 */
public final class MessageTypes {
    private MessageTypes() {}

    // Common message constants
    public static final String INTERRUPT_MESSAGE = "[Request interrupted by user]";
    public static final String CANCEL_MESSAGE = "The user doesn't want to take this action right now.";
    public static final String REJECT_MESSAGE = "The user doesn't want to proceed with this tool use.";
    public static final String NO_RESPONSE_REQUESTED = "No response requested.";
    public static final String SYNTHETIC_TOOL_RESULT_PLACEHOLDER = "[Tool result missing due to internal error]";

    /**
     * Message role types.
     */
    public enum MessageRole {
        USER("user"),
        ASSISTANT("assistant"),
        SYSTEM("system");

        private final String value;
        MessageRole(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    /**
     * Content block types.
     */
    public enum ContentBlockType {
        TEXT("text"),
        IMAGE("image"),
        TOOL_USE("tool_use"),
        TOOL_RESULT("tool_result"),
        THINKING("thinking"),
        REDACTED_THINKING("redacted_thinking");

        private final String value;
        ContentBlockType(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    /**
     * Base message interface.
     */
    public sealed interface Message permits
            UserMessage,
            AssistantMessage,
            SystemMessage,
            TombstoneMessage,
            AttachmentMessage,
            ProgressMessage {
        MessageRole role();

        /**
         * Get unique identifier for this message.
         * Returns null if not applicable.
         */
        default String uuid() { return null; }
    }

    /**
     * User message.
     */
    public record UserMessage(
            List<Map<String, Object>> content,
            Map<String, Object> metadata
    ) implements Message {
        @Override
        public MessageRole role() { return MessageRole.USER; }

        /**
         * Get content blocks from this message.
         */
        public List<Map<String, Object>> getContentBlocks() {
            return content != null ? content : List.of();
        }

        public static UserMessage of(String text) {
            return new UserMessage(
                List.of(Map.of("type", "text", "text", text)),
                new HashMap<>()
            );
        }

        // Check if message contains error content
        public boolean hasErrorContent() {
            for (Map<String, Object> block : content) {
                String text = (String) block.get("text");
                if (text != null && (text.contains("<tool_use_error>") || text.contains("Error:"))) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Assistant message.
     */
    public record AssistantMessage(
            String id,
            List<Map<String, Object>> content,
            Map<String, Object> usage,
            String model,
            String stopReason,
            Map<String, Object> metadata
    ) implements Message {
        @Override
        public MessageRole role() { return MessageRole.ASSISTANT; }

        // Alias for id() - used in some places
        @Override
        public String uuid() { return id; }

        /**
         * Count tool use blocks in this message.
         */
        public int getToolUseCount() {
            if (content == null) return 0;
            int count = 0;
            for (Map<String, Object> block : content) {
                Object type = block.get("type");
                if ("tool_use".equals(type)) {
                    count++;
                }
            }
            return count;
        }

        /**
         * Check if message has a tool use with the given ID.
         */
        public boolean hasToolUse(String toolUseId) {
            if (content == null || toolUseId == null) return false;
            for (Map<String, Object> block : content) {
                if ("tool_use".equals(block.get("type"))) {
                    String id = (String) block.get("id");
                    if (toolUseId.equals(id)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public static AssistantMessage of(String text) {
            return new AssistantMessage(
                UUID.randomUUID().toString(),
                List.of(Map.of("type", "text", "text", text)),
                null,
                null,
                null,
                new HashMap<>()
            );
        }
    }

    /**
     * System message.
     */
    public record SystemMessage(
            String content,
            String level,
            Map<String, Object> metadata
    ) implements Message {
        @Override
        public MessageRole role() { return MessageRole.SYSTEM; }

        public static SystemMessage of(String content) {
            return new SystemMessage(content, "info", new HashMap<>());
        }

        public static SystemMessage error(String content) {
            return new SystemMessage(content, "error", new HashMap<>());
        }

        public static SystemMessage warning(String content) {
            return new SystemMessage(content, "warning", new HashMap<>());
        }
    }

    /**
     * Tombstone message (deleted/removed).
     */
    public record TombstoneMessage(String reason) implements Message {
        @Override
        public MessageRole role() { return MessageRole.SYSTEM; }
    }

    /**
     * Attachment message.
     */
    public record AttachmentMessage(
            List<Map<String, Object>> attachments,
            Map<String, Object> metadata
    ) implements Message {
        @Override
        public MessageRole role() { return MessageRole.USER; }
    }

    /**
     * Progress message.
     */
    public record ProgressMessage(
            String progressId,
            String status,
            String message
    ) implements Message {
        @Override
        public MessageRole role() { return MessageRole.SYSTEM; }
    }

    /**
     * Tool use block.
     */
    public record ToolUseBlock(
            String id,
            String type,
            String name,
            Map<String, Object> input
    ) {
        public static ToolUseBlock of(String name, Map<String, Object> input) {
            return new ToolUseBlock(
                UUID.randomUUID().toString(),
                "tool_use",
                name,
                input
            );
        }
    }

    /**
     * Tool result block.
     */
    public record ToolResultBlock(
            String toolUseId,
            String type,
            Object content,
            boolean isError
    ) {
        public static ToolResultBlock success(String toolUseId, String content) {
            return new ToolResultBlock(toolUseId, "tool_result", content, false);
        }

        public static ToolResultBlock error(String toolUseId, String error) {
            return new ToolResultBlock(toolUseId, "tool_result", error, true);
        }
    }

    /**
     * Text block.
     */
    public record TextBlock(String type, String text) {
        public static TextBlock of(String text) {
            return new TextBlock("text", text);
        }
    }

    /**
     * Thinking block.
     */
    public record ThinkingBlock(String type, String thinking) {
        public static ThinkingBlock of(String thinking) {
            return new ThinkingBlock("thinking", thinking);
        }
    }

    /**
     * Token usage record.
     */
    public record TokenUsage(
            int inputTokens,
            int outputTokens,
            int cacheCreationInputTokens,
            int cacheReadInputTokens
    ) {
        public int totalTokens() {
            return inputTokens + outputTokens + cacheCreationInputTokens + cacheReadInputTokens;
        }
    }

    /**
     * Build a rejection message.
     */
    public static String buildRejectMessage(String reason) {
        if (reason != null && !reason.isEmpty()) {
            return REJECT_MESSAGE + " Reason: " + reason;
        }
        return REJECT_MESSAGE;
    }

    /**
     * Build an auto-rejection message for a tool.
     */
    public static String buildAutoRejectMessage(String toolName) {
        return "Permission to use " + toolName + " has been denied. " +
               "You may attempt to accomplish this action using other tools.";
    }

    /**
     * Check if content is a classifier denial.
     */
    public static boolean isClassifierDenial(String content) {
        return content != null && content.startsWith("Permission for this action has been denied. Reason: ");
    }

    /**
     * Create a user message map.
     */
    public static Map<String, Object> createUserMessageMap(Object content) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "user");
        msg.put("content", content);
        return msg;
    }

    /**
     * Create an assistant message map.
     */
    public static Map<String, Object> createAssistantMessageMap(Object content) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "assistant");
        msg.put("content", content);
        return msg;
    }
}