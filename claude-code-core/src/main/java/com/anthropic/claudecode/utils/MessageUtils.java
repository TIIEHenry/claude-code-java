/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/messages.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.Pattern;
import com.anthropic.claudecode.types.MessageTypes;

/**
 * Message handling utilities.
 */
public final class MessageUtils {
    private MessageUtils() {}

    // Patterns for message processing
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```", Pattern.MULTILINE);
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * Message role types.
     */
    public enum MessageRole {
        USER,
        ASSISTANT,
        SYSTEM
    }

    /**
     * Content block types.
     */
    public enum ContentBlockType {
        TEXT,
        IMAGE,
        TOOL_USE,
        TOOL_RESULT,
        THINKING
    }

    /**
     * Message record.
     */
    public record Message(
            MessageRole role,
            List<Map<String, Object>> content,
            Map<String, Object> metadata
    ) {
        public static Message user(String text) {
            return new Message(
                MessageRole.USER,
                List.of(Map.of("type", "text", "text", text)),
                new HashMap<>()
            );
        }

        public static Message assistant(String text) {
            return new Message(
                MessageRole.ASSISTANT,
                List.of(Map.of("type", "text", "text", text)),
                new HashMap<>()
            );
        }

        public static Message system(String text) {
            return new Message(
                MessageRole.SYSTEM,
                List.of(Map.of("type", "text", "text", text)),
                new HashMap<>()
            );
        }
    }

    /**
     * Extract text content from a message.
     */
    public static String extractText(Message message) {
        if (message == null || message.content() == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> block : message.content()) {
            String type = (String) block.get("type");
            if ("text".equals(type)) {
                String text = (String) block.get("text");
                if (text != null) {
                    sb.append(text);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Get the last assistant message from a list.
     */
    public static Message getLastAssistantMessage(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg.role() == MessageRole.ASSISTANT) {
                return msg;
            }
        }
        return null;
    }

    /**
     * Check if message contains tool use.
     */
    public static boolean hasToolUse(Message message) {
        if (message == null || message.content() == null) {
            return false;
        }

        for (Map<String, Object> block : message.content()) {
            String type = (String) block.get("type");
            if ("tool_use".equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract tool names from a message.
     */
    public static List<String> extractToolNames(Message message) {
        List<String> toolNames = new ArrayList<>();

        if (message == null || message.content() == null) {
            return toolNames;
        }

        for (Map<String, Object> block : message.content()) {
            String type = (String) block.get("type");
            if ("tool_use".equals(type)) {
                String name = (String) block.get("name");
                if (name != null) {
                    toolNames.add(name);
                }
            }
        }
        return toolNames;
    }

    /**
     * Normalize whitespace in text.
     */
    public static String normalizeWhitespace(String text) {
        if (text == null) return null;
        return WHITESPACE_PATTERN.matcher(text.trim()).replaceAll(" ");
    }

    /**
     * Truncate text to a maximum length.
     */
    public static String truncate(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Count words in text.
     */
    public static int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        String[] words = WHITESPACE_PATTERN.split(text.trim());
        return words.length;
    }

    /**
     * Check if text contains code blocks.
     */
    public static boolean hasCodeBlocks(String text) {
        if (text == null) return false;
        return CODE_BLOCK_PATTERN.matcher(text).find();
    }

    /**
     * Extract code blocks from text.
     */
    public static List<String> extractCodeBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        if (text == null) return blocks;

        var matcher = CODE_BLOCK_PATTERN.matcher(text);
        while (matcher.find()) {
            blocks.add(matcher.group());
        }
        return blocks;
    }

    /**
     * Create a user message with content.
     */
    public static Map<String, Object> createUserMessage(Object content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", content);
        return message;
    }

    /**
     * Create a user message as a tool result.
     */
    public static MessageTypes.Message createUserMessage(String content, String toolUseId, String assistantUuid) {
        Map<String, Object> toolResult = new LinkedHashMap<>();
        toolResult.put("type", "tool_result");
        toolResult.put("tool_use_id", toolUseId);
        toolResult.put("content", content);

        return new MessageTypes.UserMessage(
            List.of(toolResult),
            Map.of("assistant_uuid", assistantUuid != null ? assistantUuid : "")
        );
    }

    /**
     * Create an assistant message with content.
     */
    public static Map<String, Object> createAssistantMessage(Object content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant");
        message.put("content", content);
        return message;
    }

    /**
     * Create a system message with content.
     */
    public static Map<String, Object> createSystemMessage(String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "system");
        message.put("content", content);
        return message;
    }

    /**
     * Create an attachment message for tool results.
     */
    public static MessageTypes.Message createAttachmentMessage(String type, String content, String toolUseId) {
        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("type", type);
        attachment.put("content", content);
        attachment.put("tool_use_id", toolUseId);

        return new MessageTypes.UserMessage(
            List.of(attachment),
            Map.of()
        );
    }
}