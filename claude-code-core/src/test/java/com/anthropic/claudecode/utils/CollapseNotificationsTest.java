/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CollapseNotifications.
 */
class CollapseNotificationsTest {

    @Test
    @DisplayName("CollapseNotifications collapseBackgroundBashNotifications empty list")
    void collapseEmptyList() {
        List<Map<String, Object>> messages = new ArrayList<>();
        List<Map<String, Object>> result = CollapseNotifications.collapseBackgroundBashNotifications(messages, false);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("CollapseNotifications collapseBackgroundBashNotifications verbose mode")
    void collapseVerboseMode() {
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "user");
        messages.add(msg);

        List<Map<String, Object>> result = CollapseNotifications.collapseBackgroundBashNotifications(messages, true);
        assertEquals(messages, result);
    }

    @Test
    @DisplayName("CollapseNotifications collapseBackgroundBashNotifications single message")
    void collapseSingleMessage() {
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "assistant");
        msg.put("content", "Hello");
        messages.add(msg);

        List<Map<String, Object>> result = CollapseNotifications.collapseBackgroundBashNotifications(messages, false);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("CollapseNotifications collapseBackgroundBashNotifications non-user type")
    void collapseNonUserType() {
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "assistant");
        messages.add(msg);

        List<Map<String, Object>> result = CollapseNotifications.collapseBackgroundBashNotifications(messages, false);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("CollapseNotifications collapseBackgroundBashNotifications preserves non-bash")
    void collapsePreservesNonBash() {
        List<Map<String, Object>> messages = new ArrayList<>();

        // Add a regular user message
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "user");
        Map<String, Object> contentBlock = new HashMap<>();
        contentBlock.put("type", "text");
        contentBlock.put("text", "Regular message");
        msg.put("content", List.of(contentBlock));
        messages.add(msg);

        List<Map<String, Object>> result = CollapseNotifications.collapseBackgroundBashNotifications(messages, false);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("CollapseNotifications collapseBackgroundBashNotifications multiple types")
    void collapseMultipleTypes() {
        List<Map<String, Object>> messages = new ArrayList<>();

        // Add assistant message
        Map<String, Object> msg1 = new HashMap<>();
        msg1.put("type", "assistant");
        messages.add(msg1);

        // Add user message without task notification
        Map<String, Object> msg2 = new HashMap<>();
        msg2.put("type", "user");
        Map<String, Object> contentBlock = new HashMap<>();
        contentBlock.put("type", "text");
        contentBlock.put("text", "Regular user message");
        msg2.put("content", List.of(contentBlock));
        messages.add(msg2);

        List<Map<String, Object>> result = CollapseNotifications.collapseBackgroundBashNotifications(messages, false);
        assertEquals(2, result.size());
    }
}