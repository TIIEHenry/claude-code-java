/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode;

import com.anthropic.claudecode.message.ContentBlock;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Assistant message type.
 *
 * <p>Represents a message from the assistant.
 */
public record AssistantMessage(
        String uuid,
        Instant timestamp,
        String name,
        List<ContentBlock> content,
        String stopReason,
        Usage usage
) {
    public record Usage(
            long inputTokens,
            long outputTokens,
            long cacheCreationInputTokens,
            long cacheReadInputTokens
    ) {
        public static Usage empty() {
            return new Usage(0, 0, 0, 0);
        }
    }

    public static AssistantMessage of(List<ContentBlock> content) {
        return new AssistantMessage(
                UUID.randomUUID().toString(),
                Instant.now(),
                "assistant",
                content,
                null,
                Usage.empty()
        );
    }

    public boolean hasToolUse() {
        return content.stream().anyMatch(c -> c instanceof ContentBlock.ToolUse);
    }

    public List<ContentBlock.ToolUse> getToolUses() {
        return content.stream()
                .filter(c -> c instanceof ContentBlock.ToolUse)
                .map(c -> (ContentBlock.ToolUse) c)
                .toList();
    }
}