/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code background bash notification collapsing
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Collapses consecutive completed-background-bash task-notifications
 * into a single synthetic "N background commands completed" notification.
 */
public final class CollapseBackgroundBashNotifications {
    private CollapseBackgroundBashNotifications() {}

    private static final String TASK_NOTIFICATION_TAG = "task_notification";
    private static final String STATUS_TAG = "status";
    private static final String SUMMARY_TAG = "summary";
    private static final String BACKGROUND_BASH_SUMMARY_PREFIX = "Background command";

    /**
     * Collapse background bash notifications.
     *
     * @param messages The messages to collapse
     * @param verbose Whether verbose mode is enabled (pass-through in verbose)
     * @return Collapsed messages
     */
    public static List<RenderableMessage> collapseBackgroundBashNotifications(
            List<RenderableMessage> messages, boolean verbose) {

        if (!FullscreenUtils.isFullscreenEnvEnabled()) return messages;
        if (verbose) return messages;

        List<RenderableMessage> result = new ArrayList<>();
        int i = 0;

        while (i < messages.size()) {
            RenderableMessage msg = messages.get(i);
            if (isCompletedBackgroundBash(msg)) {
                int count = 0;
                while (i < messages.size() && isCompletedBackgroundBash(messages.get(i))) {
                    count++;
                    i++;
                }
                if (count == 1) {
                    result.add(msg);
                } else {
                    // Synthesize a task-notification
                    result.add(createCollapsedNotification(count));
                }
            } else {
                result.add(msg);
                i++;
            }
        }

        return result;
    }

    /**
     * Check if message is a completed background bash notification.
     */
    private static boolean isCompletedBackgroundBash(RenderableMessage msg) {
        if (msg.type() != RenderableMessage.MessageType.USER) return false;

        String content = msg.textContent();
        if (content == null || !content.contains("<" + TASK_NOTIFICATION_TAG)) return false;

        // Only collapse successful completions
        String status = extractTag(content, STATUS_TAG);
        if (!"completed".equals(status)) return false;

        // Check for background bash prefix
        String summary = extractTag(content, SUMMARY_TAG);
        return summary != null && summary.startsWith(BACKGROUND_BASH_SUMMARY_PREFIX);
    }

    /**
     * Create collapsed notification for multiple completions.
     */
    private static RenderableMessage createCollapsedNotification(int count) {
        String text = String.format(
                "<%s><%s>completed</%s><%s>%d background commands completed</%s></%s>",
                TASK_NOTIFICATION_TAG, STATUS_TAG, STATUS_TAG,
                SUMMARY_TAG, count, SUMMARY_TAG, TASK_NOTIFICATION_TAG);

        return new RenderableMessage(
                RenderableMessage.MessageType.USER,
                "user",
                List.of(new TextContent(text)));
    }

    /**
     * Extract tag content from XML-like text.
     */
    private static String extractTag(String text, String tag) {
        int start = text.indexOf("<" + tag + ">");
        if (start < 0) return null;
        start += tag.length() + 2;

        int end = text.indexOf("</" + tag + ">", start);
        if (end < 0) return null;

        return text.substring(start, end);
    }

    // Helper classes for message representation
    public record RenderableMessage(
            MessageType type,
            String role,
            List<ContentPart> content
    ) {
        public enum MessageType { USER, ASSISTANT, SYSTEM, PROGRESS }

        public String textContent() {
            for (ContentPart part : content) {
                if (part instanceof TextContent tc) {
                    return tc.text();
                }
            }
            return null;
        }
    }

    public sealed interface ContentPart permits TextContent, AttachmentContent {}
    public record TextContent(String text) implements ContentPart {}
    public record AttachmentContent(String path) implements ContentPart {}
}