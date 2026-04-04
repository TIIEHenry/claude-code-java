/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/notifier.ts
 */
package com.anthropic.claudecode.services;

import com.anthropic.claudecode.services.analytics.AnalyticsMetadata;
import java.util.concurrent.*;
import java.util.*;
import com.anthropic.claudecode.utils.EnvUtils;

/**
 * Notification service for sending user notifications.
 */
public final class Notifier {
    private Notifier() {}

    private static final String DEFAULT_TITLE = "Claude Code";

    /**
     * Notification options.
     */
    public record NotificationOptions(
        String message,
        String title,
        String notificationType
    ) {}

    /**
     * Send a notification.
     */
    public static CompletableFuture<String> sendNotification(NotificationOptions notif) {
        return CompletableFuture.supplyAsync(() -> {
            // Log analytics
            AnalyticsMetadata.logEvent("tengu_notification_method_used", Map.of(
                "configured_channel", "auto",
                "method_used", "auto",
                "term", EnvUtils.getTerminal() != null ? EnvUtils.getTerminal() : "unknown"
            ));
            return "auto";
        });
    }

    /**
     * Send notification with message only.
     */
    public static CompletableFuture<String> sendNotification(String message) {
        return sendNotification(new NotificationOptions(message, DEFAULT_TITLE, "info"));
    }

    /**
     * Check if notifications are supported.
     */
    public static boolean isNotificationSupported() {
        return true;
    }

    /**
     * Get default notification title.
     */
    public static String getDefaultTitle() {
        return DEFAULT_TITLE;
    }
}