/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Notifier.
 */
class NotifierTest {

    @Test
    @DisplayName("Notifier NotificationOptions record")
    void notificationOptionsRecord() {
        Notifier.NotificationOptions options = new Notifier.NotificationOptions(
            "Test message", "Test Title", "info"
        );

        assertEquals("Test message", options.message());
        assertEquals("Test Title", options.title());
        assertEquals("info", options.notificationType());
    }

    @Test
    @DisplayName("Notifier sendNotification returns result")
    void sendNotification() throws Exception {
        Notifier.NotificationOptions options = new Notifier.NotificationOptions(
            "Test message", "Test Title", "info"
        );

        String result = Notifier.sendNotification(options).get();
        assertNotNull(result);
    }

    @Test
    @DisplayName("Notifier sendNotification with message only")
    void sendNotificationMessageOnly() throws Exception {
        String result = Notifier.sendNotification("Test message").get();
        assertNotNull(result);
    }

    @Test
    @DisplayName("Notifier isNotificationSupported returns true")
    void isNotificationSupported() {
        assertTrue(Notifier.isNotificationSupported());
    }

    @Test
    @DisplayName("Notifier getDefaultTitle")
    void getDefaultTitle() {
        assertEquals("Claude Code", Notifier.getDefaultTitle());
    }
}