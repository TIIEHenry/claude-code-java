/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MessageTypes.
 */
class MessageTypesTest {

    @Test
    @DisplayName("MessageRole enum has correct values")
    void messageRoleWorks() {
        assertEquals("user", MessageTypes.MessageRole.USER.getValue());
        assertEquals("assistant", MessageTypes.MessageRole.ASSISTANT.getValue());
        assertEquals("system", MessageTypes.MessageRole.SYSTEM.getValue());
    }

    @Test
    @DisplayName("ContentBlockType enum has correct values")
    void contentBlockTypeWorks() {
        assertEquals("text", MessageTypes.ContentBlockType.TEXT.getValue());
        assertEquals("image", MessageTypes.ContentBlockType.IMAGE.getValue());
        assertEquals("tool_use", MessageTypes.ContentBlockType.TOOL_USE.getValue());
        assertEquals("tool_result", MessageTypes.ContentBlockType.TOOL_RESULT.getValue());
        assertEquals("thinking", MessageTypes.ContentBlockType.THINKING.getValue());
    }

    @Test
    @DisplayName("UserMessage factory method works")
    void userMessageFactoryWorks() {
        MessageTypes.UserMessage msg = MessageTypes.UserMessage.of("Hello");

        assertEquals(MessageTypes.MessageRole.USER, msg.role());
        assertEquals(1, msg.content().size());
        assertEquals("text", msg.content().get(0).get("type"));
        assertEquals("Hello", msg.content().get(0).get("text"));
    }

    @Test
    @DisplayName("AssistantMessage factory method works")
    void assistantMessageFactoryWorks() {
        MessageTypes.AssistantMessage msg = MessageTypes.AssistantMessage.of("Response");

        assertEquals(MessageTypes.MessageRole.ASSISTANT, msg.role());
        assertNotNull(msg.id());
        assertEquals(1, msg.content().size());
        assertEquals("text", msg.content().get(0).get("type"));
        assertEquals("Response", msg.content().get(0).get("text"));
    }

    @Test
    @DisplayName("SystemMessage factory methods work")
    void systemMessageFactoryWorks() {
        MessageTypes.SystemMessage info = MessageTypes.SystemMessage.of("Info message");
        MessageTypes.SystemMessage error = MessageTypes.SystemMessage.error("Error message");
        MessageTypes.SystemMessage warning = MessageTypes.SystemMessage.warning("Warning message");

        assertEquals(MessageTypes.MessageRole.SYSTEM, info.role());
        assertEquals("info", info.level());
        assertEquals("Info message", info.content());

        assertEquals("error", error.level());
        assertEquals("Error message", error.content());

        assertEquals("warning", warning.level());
        assertEquals("Warning message", warning.content());
    }

    @Test
    @DisplayName("TombstoneMessage works")
    void tombstoneMessageWorks() {
        MessageTypes.TombstoneMessage tombstone = new MessageTypes.TombstoneMessage("deleted");

        assertEquals(MessageTypes.MessageRole.SYSTEM, tombstone.role());
        assertEquals("deleted", tombstone.reason());
    }

    @Test
    @DisplayName("ToolUseBlock factory works")
    void toolUseBlockWorks() {
        MessageTypes.ToolUseBlock block = MessageTypes.ToolUseBlock.of(
            "bash",
            Map.of("command", "ls")
        );

        assertNotNull(block.id());
        assertEquals("tool_use", block.type());
        assertEquals("bash", block.name());
        assertEquals("ls", block.input().get("command"));
    }

    @Test
    @DisplayName("ToolResultBlock factory works")
    void toolResultBlockWorks() {
        MessageTypes.ToolResultBlock success = MessageTypes.ToolResultBlock.success("tool-1", "output");
        MessageTypes.ToolResultBlock error = MessageTypes.ToolResultBlock.error("tool-1", "failed");

        assertFalse(success.isError());
        assertEquals("output", success.content());

        assertTrue(error.isError());
        assertEquals("failed", error.content());
    }

    @Test
    @DisplayName("TokenUsage calculates total correctly")
    void tokenUsageWorks() {
        MessageTypes.TokenUsage usage = new MessageTypes.TokenUsage(100, 50, 10, 5);

        assertEquals(100, usage.inputTokens());
        assertEquals(50, usage.outputTokens());
        assertEquals(10, usage.cacheCreationInputTokens());
        assertEquals(5, usage.cacheReadInputTokens());
        assertEquals(165, usage.totalTokens());
    }

    @Test
    @DisplayName("buildRejectMessage adds reason")
    void buildRejectMessageWorks() {
        String withReason = MessageTypes.buildRejectMessage("User cancelled");
        String withoutReason = MessageTypes.buildRejectMessage(null);

        assertTrue(withReason.contains("Reason: User cancelled"));
        assertEquals(MessageTypes.REJECT_MESSAGE, withoutReason);
    }

    @Test
    @DisplayName("isClassifierDenial detects denial messages")
    void isClassifierDenialWorks() {
        assertTrue(MessageTypes.isClassifierDenial(
            "Permission for this action has been denied. Reason: security"
        ));
        assertFalse(MessageTypes.isClassifierDenial("Some other message"));
        assertFalse(MessageTypes.isClassifierDenial(null));
    }

    @Test
    @DisplayName("Pattern matching on Message works")
    void patternMatchingWorks() {
        MessageTypes.Message user = MessageTypes.UserMessage.of("test");
        MessageTypes.Message assistant = MessageTypes.AssistantMessage.of("response");
        MessageTypes.Message system = MessageTypes.SystemMessage.of("info");

        String userRole;
        if (user instanceof MessageTypes.UserMessage) {
            userRole = "user";
        } else if (user instanceof MessageTypes.AssistantMessage) {
            userRole = "assistant";
        } else if (user instanceof MessageTypes.SystemMessage) {
            userRole = "system";
        } else if (user instanceof MessageTypes.TombstoneMessage) {
            userRole = "tombstone";
        } else if (user instanceof MessageTypes.AttachmentMessage) {
            userRole = "attachment";
        } else if (user instanceof MessageTypes.ProgressMessage) {
            userRole = "progress";
        } else {
            userRole = "unknown";
        }

        assertEquals("user", userRole);
    }
}