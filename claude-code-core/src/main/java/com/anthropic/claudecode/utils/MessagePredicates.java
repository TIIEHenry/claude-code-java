/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code message predicates
 */
package com.anthropic.claudecode.utils;

/**
 * Message predicate utilities for filtering messages.
 */
public final class MessagePredicates {
    private MessagePredicates() {}

    /**
     * Check if a message is a human turn (not tool result, not meta).
     */
    public static boolean isHumanTurn(Message message) {
        return message != null &&
                "user".equals(message.type()) &&
                !message.isMeta() &&
                message.toolUseResult() == null;
    }

    /**
     * Check if a message is a tool result.
     */
    public static boolean isToolResult(Message message) {
        return message != null &&
                "user".equals(message.type()) &&
                message.toolUseResult() != null;
    }

    /**
     * Check if a message is an assistant turn.
     */
    public static boolean isAssistantTurn(Message message) {
        return message != null && "assistant".equals(message.type());
    }

    /**
     * Check if a message is a system message.
     */
    public static boolean isSystemMessage(Message message) {
        return message != null && "system".equals(message.type());
    }

    /**
     * Check if a message is meta (not shown to user).
     */
    public static boolean isMetaMessage(Message message) {
        return message != null && message.isMeta();
    }

    /**
     * Message interface for predicates.
     */
    public interface Message {
        String type();
        boolean isMeta();
        Object toolUseResult();
    }

    /**
     * Simple message implementation.
     */
    public record SimpleMessage(String type, boolean isMeta, Object toolUseResult) implements Message {}
}