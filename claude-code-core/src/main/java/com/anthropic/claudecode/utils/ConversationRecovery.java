/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code conversation recovery utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Utilities for loading and deserializing conversations for resume.
 */
public final class ConversationRecovery {
    private ConversationRecovery() {}

    /**
     * Turn interruption state.
     */
    public sealed interface TurnInterruptionState permits
            None,
            InterruptedPrompt,
            InterruptedTurn {

        String kind();
    }

    public static final class None implements TurnInterruptionState {
        @Override public String kind() { return "none"; }
    }

    public static final class InterruptedPrompt implements TurnInterruptionState {
        private final Map<String, Object> message;

        public InterruptedPrompt(Map<String, Object> message) {
            this.message = message;
        }

        public Map<String, Object> message() { return message; }
        @Override public String kind() { return "interrupted_prompt"; }
    }

    public static final class InterruptedTurn implements TurnInterruptionState {
        @Override public String kind() { return "interrupted_turn"; }
    }

    /**
     * Deserialize result.
     */
    public record DeserializeResult(
            List<Map<String, Object>> messages,
            TurnInterruptionState turnInterruptionState
    ) {}

    /**
     * Deserialize messages from a log file.
     */
    public static List<Map<String, Object>> deserializeMessages(
            List<Map<String, Object>> serializedMessages) {
        return deserializeMessagesWithInterruptDetection(serializedMessages).messages();
    }

    /**
     * Deserialize messages with interrupt detection.
     */
    public static DeserializeResult deserializeMessagesWithInterruptDetection(
            List<Map<String, Object>> serializedMessages) {
        try {
            // Migrate legacy attachment types
            List<Map<String, Object>> migratedMessages = new ArrayList<>();
            for (Map<String, Object> msg : serializedMessages) {
                migratedMessages.add(migrateLegacyAttachmentTypes(msg));
            }

            // Filter unresolved tool uses
            List<Map<String, Object>> filteredToolUses = filterUnresolvedToolUses(migratedMessages);

            // Filter orphaned thinking-only messages
            List<Map<String, Object>> filteredThinking = filterOrphanedThinkingOnlyMessages(filteredToolUses);

            // Filter whitespace-only assistant messages
            List<Map<String, Object>> filteredMessages = filterWhitespaceOnlyAssistantMessages(filteredThinking);

            // Detect turn interruption
            TurnInterruptionState internalState = detectTurnInterruption(filteredMessages);

            TurnInterruptionState turnInterruptionState;
            if (internalState instanceof InterruptedTurn) {
                // Transform interrupted_turn into interrupted_prompt with synthetic message
                Map<String, Object> continuationMessage = new HashMap<>();
                continuationMessage.put("type", "user");
                continuationMessage.put("content", "Continue from where you left off.");
                continuationMessage.put("isMeta", true);
                filteredMessages.add(continuationMessage);
                turnInterruptionState = new InterruptedPrompt(continuationMessage);
            } else {
                turnInterruptionState = internalState;
            }

            // Append synthetic assistant sentinel after last user message
            int lastRelevantIdx = -1;
            for (int i = filteredMessages.size() - 1; i >= 0; i--) {
                Map<String, Object> msg = filteredMessages.get(i);
                String type = (String) msg.get("type");
                if (!"system".equals(type) && !"progress".equals(type)) {
                    lastRelevantIdx = i;
                    break;
                }
            }

            if (lastRelevantIdx != -1 && "user".equals(filteredMessages.get(lastRelevantIdx).get("type"))) {
                Map<String, Object> sentinel = new HashMap<>();
                sentinel.put("type", "assistant");
                sentinel.put("content", "NO_RESPONSE_REQUESTED");
                filteredMessages.add(lastRelevantIdx + 1, sentinel);
            }

            return new DeserializeResult(filteredMessages, turnInterruptionState);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize messages", e);
        }
    }

    /**
     * Migrate legacy attachment types.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> migrateLegacyAttachmentTypes(Map<String, Object> message) {
        String type = (String) message.get("type");
        if (!"attachment".equals(type)) {
            return message;
        }

        Map<String, Object> attachment = (Map<String, Object>) message.get("attachment");
        if (attachment == null) {
            return message;
        }

        Map<String, Object> result = new HashMap<>(message);
        Map<String, Object> newAttachment = new HashMap<>(attachment);

        String attachmentType = (String) attachment.get("type");
        if ("new_file".equals(attachmentType)) {
            newAttachment.put("type", "file");
            newAttachment.put("displayPath", attachment.get("filename"));
        } else if ("new_directory".equals(attachmentType)) {
            newAttachment.put("type", "directory");
            newAttachment.put("displayPath", attachment.get("path"));
        } else if (!attachment.containsKey("displayPath")) {
            Object path = attachment.get("filename");
            if (path == null) path = attachment.get("path");
            if (path == null) path = attachment.get("skillDir");
            if (path != null) {
                newAttachment.put("displayPath", path);
            }
        }

        result.put("attachment", newAttachment);
        return result;
    }

    /**
     * Filter unresolved tool uses.
     */
    private static List<Map<String, Object>> filterUnresolvedToolUses(List<Map<String, Object>> messages) {
        // In real implementation, would filter out assistant messages with unmatched tool_uses
        return messages;
    }

    /**
     * Filter orphaned thinking-only messages.
     */
    private static List<Map<String, Object>> filterOrphanedThinkingOnlyMessages(List<Map<String, Object>> messages) {
        // In real implementation, would filter out orphaned thinking-only assistant messages
        return messages;
    }

    /**
     * Filter whitespace-only assistant messages.
     */
    private static List<Map<String, Object>> filterWhitespaceOnlyAssistantMessages(List<Map<String, Object>> messages) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> msg : messages) {
            if ("assistant".equals(msg.get("type"))) {
                Object content = msg.get("content");
                if (content instanceof String && ((String) content).trim().isEmpty()) {
                    continue;
                }
            }
            result.add(msg);
        }
        return result;
    }

    /**
     * Detect turn interruption.
     */
    private static TurnInterruptionState detectTurnInterruption(List<Map<String, Object>> messages) {
        if (messages.isEmpty()) {
            return new None();
        }

        // Find last turn-relevant message
        int lastMessageIdx = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, Object> msg = messages.get(i);
            String type = (String) msg.get("type");
            if (!"system".equals(type) && !"progress".equals(type)) {
                lastMessageIdx = i;
                break;
            }
        }

        if (lastMessageIdx == -1) {
            return new None();
        }

        Map<String, Object> lastMessage = messages.get(lastMessageIdx);
        String lastType = (String) lastMessage.get("type");

        if ("assistant".equals(lastType)) {
            return new None();
        }

        if ("user".equals(lastType)) {
            Boolean isMeta = (Boolean) lastMessage.get("isMeta");
            Boolean isCompactSummary = (Boolean) lastMessage.get("isCompactSummary");

            if (Boolean.TRUE.equals(isMeta) || Boolean.TRUE.equals(isCompactSummary)) {
                return new None();
            }

            if (isToolUseResultMessage(lastMessage)) {
                if (isTerminalToolResult(lastMessage, messages, lastMessageIdx)) {
                    return new None();
                }
                return new InterruptedTurn();
            }

            return new InterruptedPrompt(lastMessage);
        }

        if ("attachment".equals(lastType)) {
            return new InterruptedTurn();
        }

        return new None();
    }

    /**
     * Check if message is a tool use result.
     */
    @SuppressWarnings("unchecked")
    private static boolean isToolUseResultMessage(Map<String, Object> message) {
        Object content = message.get("content");
        if (content instanceof List) {
            List<Map<String, Object>> blocks = (List<Map<String, Object>>) content;
            if (!blocks.isEmpty()) {
                return "tool_result".equals(blocks.get(0).get("type"));
            }
        }
        return false;
    }

    /**
     * Check if this is a terminal tool result.
     */
    private static boolean isTerminalToolResult(Map<String, Object> result, List<Map<String, Object>> messages, int resultIdx) {
        // In real implementation, would check for terminal tool names like SendUserMessage
        return false;
    }

    /**
     * Restore skill state from invoked_skills attachments.
     */
    public static void restoreSkillStateFromMessages(List<Map<String, Object>> messages) {
        for (Map<String, Object> message : messages) {
            if (!"attachment".equals(message.get("type"))) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> attachment = (Map<String, Object>) message.get("attachment");
            if (attachment == null) continue;

            if ("invoked_skills".equals(attachment.get("type"))) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> skills = (List<Map<String, Object>>) attachment.get("skills");
                if (skills != null) {
                    for (Map<String, Object> skill : skills) {
                        // In real implementation, would add to invoked skills state
                    }
                }
            }
        }
    }
}