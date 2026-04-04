/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import com.anthropic.claudecode.message.ContentBlock;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AssistantMessage.
 */
class AssistantMessageTest {

    @Test
    @DisplayName("AssistantMessage record creates instance")
    void recordCreatesInstance() {
        Instant timestamp = Instant.now();
        List<ContentBlock> content = Collections.emptyList();
        AssistantMessage.Usage usage = new AssistantMessage.Usage(100, 50, 0, 0);

        AssistantMessage msg = new AssistantMessage(
            "uuid-123", timestamp, "assistant", content, "end_turn", usage
        );

        assertEquals("uuid-123", msg.uuid());
        assertEquals(timestamp, msg.timestamp());
        assertEquals("assistant", msg.name());
        assertEquals(content, msg.content());
        assertEquals("end_turn", msg.stopReason());
        assertEquals(100, msg.usage().inputTokens());
        assertEquals(50, msg.usage().outputTokens());
    }

    @Test
    @DisplayName("AssistantMessage Usage record")
    void usageRecord() {
        AssistantMessage.Usage usage = new AssistantMessage.Usage(1000, 500, 100, 200);

        assertEquals(1000, usage.inputTokens());
        assertEquals(500, usage.outputTokens());
        assertEquals(100, usage.cacheCreationInputTokens());
        assertEquals(200, usage.cacheReadInputTokens());
    }

    @Test
    @DisplayName("AssistantMessage Usage empty")
    void usageEmpty() {
        AssistantMessage.Usage usage = AssistantMessage.Usage.empty();

        assertEquals(0, usage.inputTokens());
        assertEquals(0, usage.outputTokens());
        assertEquals(0, usage.cacheCreationInputTokens());
        assertEquals(0, usage.cacheReadInputTokens());
    }

    @Test
    @DisplayName("AssistantMessage of creates instance with defaults")
    void ofCreatesInstance() {
        List<ContentBlock> content = Collections.emptyList();
        AssistantMessage msg = AssistantMessage.of(content);

        assertNotNull(msg.uuid());
        assertNotNull(msg.timestamp());
        assertEquals("assistant", msg.name());
        assertEquals(content, msg.content());
        assertNull(msg.stopReason());
        assertEquals(0, msg.usage().inputTokens());
    }

    @Test
    @DisplayName("AssistantMessage hasToolUse returns false for empty content")
    void hasToolUseEmptyContent() {
        AssistantMessage msg = AssistantMessage.of(Collections.emptyList());

        assertFalse(msg.hasToolUse());
    }

    @Test
    @DisplayName("AssistantMessage hasToolUse returns false for text content")
    void hasToolUseTextContent() {
        List<ContentBlock> content = List.of(new ContentBlock.Text("Hello"));
        AssistantMessage msg = AssistantMessage.of(content);

        assertFalse(msg.hasToolUse());
    }

    @Test
    @DisplayName("AssistantMessage hasToolUse returns true for tool use content")
    void hasToolUseToolContent() {
        ContentBlock.ToolUse toolUse = new ContentBlock.ToolUse("tool-123", "bash", Map.of("command", "ls"));
        List<ContentBlock> content = List.of(toolUse);
        AssistantMessage msg = AssistantMessage.of(content);

        assertTrue(msg.hasToolUse());
    }

    @Test
    @DisplayName("AssistantMessage getToolUses returns empty for no tools")
    void getToolUsesEmpty() {
        AssistantMessage msg = AssistantMessage.of(Collections.emptyList());

        assertTrue(msg.getToolUses().isEmpty());
    }

    @Test
    @DisplayName("AssistantMessage getToolUses returns empty for text only")
    void getToolUsesTextOnly() {
        List<ContentBlock> content = List.of(new ContentBlock.Text("text"));
        AssistantMessage msg = AssistantMessage.of(content);

        assertTrue(msg.getToolUses().isEmpty());
    }

    @Test
    @DisplayName("AssistantMessage getToolUses returns tool uses")
    void getToolUsesReturnsTools() {
        ContentBlock.ToolUse tool1 = new ContentBlock.ToolUse("tool-1", "bash", Map.of());
        ContentBlock.ToolUse tool2 = new ContentBlock.ToolUse("tool-2", "read", Map.of());
        List<ContentBlock> content = List.of(tool1, tool2);
        AssistantMessage msg = AssistantMessage.of(content);

        List<ContentBlock.ToolUse> toolUses = msg.getToolUses();
        assertEquals(2, toolUses.size());
        assertEquals("tool-1", toolUses.get(0).id());
        assertEquals("tool-2", toolUses.get(1).id());
    }

    @Test
    @DisplayName("AssistantMessage getToolUses filters mixed content")
    void getToolUsesMixedContent() {
        ContentBlock.Text text = new ContentBlock.Text("some text");
        ContentBlock.ToolUse tool = new ContentBlock.ToolUse("tool-1", "bash", Map.of());
        List<ContentBlock> content = List.of(text, tool);
        AssistantMessage msg = AssistantMessage.of(content);

        List<ContentBlock.ToolUse> toolUses = msg.getToolUses();
        assertEquals(1, toolUses.size());
        assertEquals("tool-1", toolUses.get(0).id());
    }
}