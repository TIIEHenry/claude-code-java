/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.message;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Message types for conversation handling.
 */
public sealed interface Message permits
    Message.User,
    Message.Assistant,
    Message.System {

    String role();

    /**
     * User message.
     */
    record User(String role, List<ContentBlock> content, Map<String, Object> metadata) implements Message {
        public User(List<ContentBlock> content) {
            this("user", content, Map.of());
        }

        public User(String text) {
            this("user", List.of(new ContentBlock.Text(text)), Map.of());
        }
    }

    /**
     * Assistant message.
     */
    record Assistant(
        String role,
        String id,
        List<ContentBlock> content,
        Map<String, Object> usage,
        String model,
        String stopReason,
        Instant timestamp
    ) implements Message {
        public Assistant(List<ContentBlock> content) {
            this("assistant", null, content, null, null, null, Instant.now());
        }

        public Assistant(String text) {
            this("assistant", null, List.of(new ContentBlock.Text(text)), null, null, null, Instant.now());
        }
    }

    /**
     * System message.
     */
    record System(String role, String content, String level) implements Message {
        public System(String content) {
            this("system", content, "info");
        }

        public static System error(String content) {
            return new System("system", content, "error");
        }

        public static System warning(String content) {
            return new System("system", content, "warning");
        }
    }
}