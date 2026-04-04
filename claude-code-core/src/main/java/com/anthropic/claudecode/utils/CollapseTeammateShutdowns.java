/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code teammate shutdown collapsing
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Collapses consecutive in-process teammate shutdown task_status attachments
 * into a single teammate_shutdown_batch attachment with a count.
 */
public final class CollapseTeammateShutdowns {
    private CollapseTeammateShutdowns() {}

    /**
     * Collapse teammate shutdown attachments.
     *
     * @param messages The messages to collapse
     * @return Collapsed messages
     */
    public static List<AttachmentMessage> collapseTeammateShutdowns(
            List<AttachmentMessage> messages) {

        List<AttachmentMessage> result = new ArrayList<>();
        int i = 0;

        while (i < messages.size()) {
            AttachmentMessage msg = messages.get(i);
            if (isTeammateShutdownAttachment(msg)) {
                int count = 0;
                while (i < messages.size() && isTeammateShutdownAttachment(messages.get(i))) {
                    count++;
                    i++;
                }
                if (count == 1) {
                    result.add(msg);
                } else {
                    result.add(createShutdownBatch(msg, count));
                }
            } else {
                result.add(msg);
                i++;
            }
        }

        return result;
    }

    /**
     * Check if message is a teammate shutdown attachment.
     */
    private static boolean isTeammateShutdownAttachment(AttachmentMessage msg) {
        return msg.type() == AttachmentMessage.MessageType.ATTACHMENT &&
                msg.attachment().type() == AttachmentInfo.AttachmentType.TASK_STATUS &&
                msg.attachment().taskType() == "in_process_teammate" &&
                msg.attachment().status() == "completed";
    }

    /**
     * Create a shutdown batch message.
     */
    private static AttachmentMessage createShutdownBatch(AttachmentMessage template, int count) {
        AttachmentInfo batchAttachment = new AttachmentInfo(
                AttachmentInfo.AttachmentType.TEAMMATE_SHUTDOWN_BATCH,
                null, null, count, null, null);

        return new AttachmentMessage(
                AttachmentMessage.MessageType.ATTACHMENT,
                template.uuid(),
                template.timestamp(),
                batchAttachment);
    }

    /**
     * Attachment message representation.
     */
    public record AttachmentMessage(
            MessageType type,
            String uuid,
            long timestamp,
            AttachmentInfo attachment
    ) {
        public enum MessageType { ATTACHMENT, USER, ASSISTANT, SYSTEM }
    }

    /**
     * Attachment info representation.
     */
    public record AttachmentInfo(
            AttachmentType type,
            String taskType,
            String status,
            Integer count,
            String path,
            String content
    ) {
        public enum AttachmentType {
            TASK_STATUS,
            TEAMMATE_SHUTDOWN_BATCH,
            RELEVANT_MEMORIES,
            NESTED_MEMORY,
            FILE
        }
    }
}