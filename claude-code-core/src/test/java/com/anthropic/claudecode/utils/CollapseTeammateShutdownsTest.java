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
 * Tests for CollapseTeammateShutdowns.
 */
class CollapseTeammateShutdownsTest {

    @Test
    @DisplayName("CollapseTeammateShutdowns AttachmentMessage MessageType enum")
    void messageTypeEnum() {
        assertEquals(4, CollapseTeammateShutdowns.AttachmentMessage.MessageType.values().length);
    }

    @Test
    @DisplayName("CollapseTeammateShutdowns AttachmentInfo AttachmentType enum")
    void attachmentTypeEnum() {
        assertEquals(5, CollapseTeammateShutdowns.AttachmentInfo.AttachmentType.values().length);
    }

    @Test
    @DisplayName("CollapseTeammateShutdowns AttachmentMessage record")
    void attachmentMessageRecord() {
        CollapseTeammateShutdowns.AttachmentInfo info = new CollapseTeammateShutdowns.AttachmentInfo(
            CollapseTeammateShutdowns.AttachmentInfo.AttachmentType.TASK_STATUS,
            "in_process_teammate", "completed", null, null, null
        );

        CollapseTeammateShutdowns.AttachmentMessage msg = new CollapseTeammateShutdowns.AttachmentMessage(
            CollapseTeammateShutdowns.AttachmentMessage.MessageType.ATTACHMENT,
            "uuid-123", 1000L, info
        );

        assertEquals(CollapseTeammateShutdowns.AttachmentMessage.MessageType.ATTACHMENT, msg.type());
        assertEquals("uuid-123", msg.uuid());
        assertEquals(1000L, msg.timestamp());
        assertEquals(info, msg.attachment());
    }

    @Test
    @DisplayName("CollapseTeammateShutdowns AttachmentInfo record")
    void attachmentInfoRecord() {
        CollapseTeammateShutdowns.AttachmentInfo info = new CollapseTeammateShutdowns.AttachmentInfo(
            CollapseTeammateShutdowns.AttachmentInfo.AttachmentType.FILE,
            null, null, null, "/path/to/file", "content"
        );

        assertEquals(CollapseTeammateShutdowns.AttachmentInfo.AttachmentType.FILE, info.type());
        assertEquals("/path/to/file", info.path());
        assertEquals("content", info.content());
    }

    @Test
    @DisplayName("CollapseTeammateShutdowns collapseTeammateShutdowns empty list")
    void collapseEmptyList() {
        List<CollapseTeammateShutdowns.AttachmentMessage> messages = new ArrayList<>();
        List<CollapseTeammateShutdowns.AttachmentMessage> result =
            CollapseTeammateShutdowns.collapseTeammateShutdowns(messages);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("CollapseTeammateShutdowns collapseTeammateShutdowns non-attachment message")
    void collapseNonAttachmentMessage() {
        CollapseTeammateShutdowns.AttachmentMessage msg = new CollapseTeammateShutdowns.AttachmentMessage(
            CollapseTeammateShutdowns.AttachmentMessage.MessageType.USER,
            "uuid", 1000L, null
        );

        List<CollapseTeammateShutdowns.AttachmentMessage> messages = List.of(msg);
        List<CollapseTeammateShutdowns.AttachmentMessage> result =
            CollapseTeammateShutdowns.collapseTeammateShutdowns(messages);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("CollapseTeammateShutdowns collapseTeammateShutdowns file attachment")
    void collapseFileAttachment() {
        CollapseTeammateShutdowns.AttachmentInfo info = new CollapseTeammateShutdowns.AttachmentInfo(
            CollapseTeammateShutdowns.AttachmentInfo.AttachmentType.FILE,
            null, null, null, "/path", null
        );

        CollapseTeammateShutdowns.AttachmentMessage msg = new CollapseTeammateShutdowns.AttachmentMessage(
            CollapseTeammateShutdowns.AttachmentMessage.MessageType.ATTACHMENT,
            "uuid", 1000L, info
        );

        List<CollapseTeammateShutdowns.AttachmentMessage> messages = List.of(msg);
        List<CollapseTeammateShutdowns.AttachmentMessage> result =
            CollapseTeammateShutdowns.collapseTeammateShutdowns(messages);

        assertEquals(1, result.size());
    }
}