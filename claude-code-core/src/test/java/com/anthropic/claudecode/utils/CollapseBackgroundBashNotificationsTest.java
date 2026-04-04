/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CollapseBackgroundBashNotifications.
 */
class CollapseBackgroundBashNotificationsTest {

    @Test
    @DisplayName("CollapseBackgroundBashNotifications RenderableMessage record")
    void renderableMessageRecord() {
        CollapseBackgroundBashNotifications.TextContent text =
            new CollapseBackgroundBashNotifications.TextContent("Hello");

        CollapseBackgroundBashNotifications.RenderableMessage msg =
            new CollapseBackgroundBashNotifications.RenderableMessage(
                CollapseBackgroundBashNotifications.RenderableMessage.MessageType.USER,
                "user",
                List.of(text)
            );

        assertEquals(CollapseBackgroundBashNotifications.RenderableMessage.MessageType.USER, msg.type());
        assertEquals("user", msg.role());
        assertEquals(1, msg.content().size());
        assertEquals("Hello", msg.textContent());
    }

    @Test
    @DisplayName("CollapseBackgroundBashNotifications MessageType enum")
    void messageTypeEnum() {
        assertEquals(4, CollapseBackgroundBashNotifications.RenderableMessage.MessageType.values().length);
    }

    @Test
    @DisplayName("CollapseBackgroundBashNotifications TextContent record")
    void textContentRecord() {
        CollapseBackgroundBashNotifications.TextContent text =
            new CollapseBackgroundBashNotifications.TextContent("Test content");
        assertEquals("Test content", text.text());
    }

    @Test
    @DisplayName("CollapseBackgroundBashNotifications AttachmentContent record")
    void attachmentContentRecord() {
        CollapseBackgroundBashNotifications.AttachmentContent attachment =
            new CollapseBackgroundBashNotifications.AttachmentContent("/path/to/file");
        assertEquals("/path/to/file", attachment.path());
    }

    @Test
    @DisplayName("CollapseBackgroundBashNotifications textContent returns null for attachment")
    void textContentNullForAttachment() {
        CollapseBackgroundBashNotifications.AttachmentContent attachment =
            new CollapseBackgroundBashNotifications.AttachmentContent("/path");

        CollapseBackgroundBashNotifications.RenderableMessage msg =
            new CollapseBackgroundBashNotifications.RenderableMessage(
                CollapseBackgroundBashNotifications.RenderableMessage.MessageType.USER,
                "user",
                List.of(attachment)
            );

        assertNull(msg.textContent());
    }

    @Test
    @DisplayName("CollapseBackgroundBashNotifications collapseBackgroundBashNotifications empty list")
    void collapseEmptyList() {
        List<CollapseBackgroundBashNotifications.RenderableMessage> messages = new ArrayList<>();
        List<CollapseBackgroundBashNotifications.RenderableMessage> result =
            CollapseBackgroundBashNotifications.collapseBackgroundBashNotifications(messages, false);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("CollapseBackgroundBashNotifications collapseBackgroundBashNotifications verbose mode")
    void collapseVerboseMode() {
        List<CollapseBackgroundBashNotifications.RenderableMessage> messages = new ArrayList<>();
        CollapseBackgroundBashNotifications.TextContent text =
            new CollapseBackgroundBashNotifications.TextContent("Test");
        CollapseBackgroundBashNotifications.RenderableMessage msg =
            new CollapseBackgroundBashNotifications.RenderableMessage(
                CollapseBackgroundBashNotifications.RenderableMessage.MessageType.USER,
                "user",
                List.of(text)
            );
        messages.add(msg);

        List<CollapseBackgroundBashNotifications.RenderableMessage> result =
            CollapseBackgroundBashNotifications.collapseBackgroundBashNotifications(messages, true);
        assertEquals(messages, result);
    }

    @Test
    @DisplayName("CollapseBackgroundBashNotifications collapseBackgroundBashNotifications single message")
    void collapseSingleMessage() {
        List<CollapseBackgroundBashNotifications.RenderableMessage> messages = new ArrayList<>();
        CollapseBackgroundBashNotifications.TextContent text =
            new CollapseBackgroundBashNotifications.TextContent("Test");
        CollapseBackgroundBashNotifications.RenderableMessage msg =
            new CollapseBackgroundBashNotifications.RenderableMessage(
                CollapseBackgroundBashNotifications.RenderableMessage.MessageType.ASSISTANT,
                "assistant",
                List.of(text)
            );
        messages.add(msg);

        List<CollapseBackgroundBashNotifications.RenderableMessage> result =
            CollapseBackgroundBashNotifications.collapseBackgroundBashNotifications(messages, false);
        assertEquals(1, result.size());
    }
}