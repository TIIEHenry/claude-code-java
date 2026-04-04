/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code SDK message types
 */
package com.anthropic.claudecode.types;

import com.anthropic.claudecode.message.*;

import java.util.*;

/**
 * SDK message types for API communication.
 *
 * <p>These types represent messages as they flow through the SDK layer.
 */
public sealed interface SDKMessage permits
    SDKMessage.User,
    SDKMessage.Assistant,
    SDKMessage.Result,
    SDKMessage.Progress {

    /**
     * Convert from a Message to SDKMessage.
     */
    public static SDKMessage fromMessage(Message msg) {
        if (msg instanceof Message.User user) {
            return new User(user.content());
        } else if (msg instanceof Message.Assistant assistant) {
            return new Assistant(assistant.content());
        } else if (msg instanceof Message.System system) {
            return new User(List.of(new ContentBlock.Text(system.content())));
        }
        return new User(List.of());
    }

    /**
     * User message from SDK.
     */
    public record User(List<ContentBlock> content) implements SDKMessage {
        public User(String text) {
            this(List.of(new ContentBlock.Text(text)));
        }
    }

    /**
     * Assistant message from SDK.
     */
    public record Assistant(List<ContentBlock> content) implements SDKMessage {
        public Assistant(String text) {
            this(List.of(new ContentBlock.Text(text)));
        }
    }

    /**
     * Result message from tool execution.
     */
    public record Result(Object result, boolean isError) implements SDKMessage {
        public Result(Object result) {
            this(result, false);
        }

        public static Result error(Object error) {
            return new Result(error, true);
        }
    }

    /**
     * Progress message during execution.
     */
    public record Progress(String status, double percentage) implements SDKMessage {
        public Progress(String status) {
            this(status, 0.0);
        }
    }
}