/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code collapse notifications utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Utilities for collapsing notification messages.
 */
public final class CollapseNotifications {
    private CollapseNotifications() {}

    private static final String STATUS_TAG = "status";
    private static final String SUMMARY_TAG = "summary";
    private static final String TASK_NOTIFICATION_TAG = "task-notification";

    /**
     * Collapse consecutive background bash notifications.
     */
    public static List<Map<String, Object>> collapseBackgroundBashNotifications(
            List<Map<String, Object>> messages,
            boolean verbose) {

        if (verbose) return messages;

        List<Map<String, Object>> result = new ArrayList<>();
        int i = 0;

        while (i < messages.size()) {
            Map<String, Object> msg = messages.get(i);

            if (isCompletedBackgroundBash(msg)) {
                int count = 0;
                while (i < messages.size() && isCompletedBackgroundBash(messages.get(i))) {
                    count++;
                    i++;
                }

                if (count == 1) {
                    result.add(msg);
                } else {
                    // Synthesize collapsed notification
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
    private static boolean isCompletedBackgroundBash(Map<String, Object> msg) {
        if (!"user".equals(msg.get("type"))) return false;

        Object content = msg.get("content");
        if (!(content instanceof List)) return false;

        List<?> contentList = (List<?>) content;
        if (contentList.isEmpty()) return false;

        Object first = contentList.get(0);
        if (!(first instanceof Map)) return false;

        Map<?, ?> block = (Map<?, ?>) first;
        if (!"text".equals(block.get("type"))) return false;

        String text = (String) block.get("text");
        if (text == null || !text.contains("<" + TASK_NOTIFICATION_TAG)) return false;

        // Only collapse successful completions
        String status = extractTag(text, STATUS_TAG);
        if (!"completed".equals(status)) return false;

        // Check for background bash prefix
        String summary = extractTag(text, SUMMARY_TAG);
        return summary != null && summary.startsWith("Background command");
    }

    /**
     * Extract tag value from XML-like string.
     */
    private static String extractTag(String text, String tag) {
        String startTag = "<" + tag + ">";
        String endTag = "</" + tag + ">";

        int start = text.indexOf(startTag);
        if (start == -1) return null;

        int end = text.indexOf(endTag, start + startTag.length());
        if (end == -1) return null;

        return text.substring(start + startTag.length(), end);
    }

    /**
     * Create a collapsed notification message.
     */
    private static Map<String, Object> createCollapsedNotification(int count) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "user");

        Map<String, Object> contentBlock = new LinkedHashMap<>();
        contentBlock.put("type", "text");
        contentBlock.put("text", String.format(
                "<%s><%s>completed</%s><%s>%d background commands completed</%s></%s>",
                TASK_NOTIFICATION_TAG, STATUS_TAG, STATUS_TAG, SUMMARY_TAG, count, SUMMARY_TAG, TASK_NOTIFICATION_TAG));

        msg.put("content", List.of(contentBlock));
        return msg;
    }
}