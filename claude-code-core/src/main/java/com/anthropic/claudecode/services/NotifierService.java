/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/notifier.ts
 */
package com.anthropic.claudecode.services;

import java.util.*;
import java.util.concurrent.*;

/**
 * Notification service for user alerts.
 */
public final class NotifierService {
    private NotifierService() {}

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final List<NotificationListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Notification types.
     */
    public enum NotificationType {
        INFO,
        SUCCESS,
        WARNING,
        ERROR,
        PROGRESS
    }

    /**
     * Notification record.
     */
    public record Notification(
            String id,
            NotificationType type,
            String title,
            String message,
            long timestamp,
            Map<String, Object> metadata
    ) {
        public static Notification info(String title, String message) {
            return new Notification(
                UUID.randomUUID().toString(),
                NotificationType.INFO,
                title,
                message,
                System.currentTimeMillis(),
                new HashMap<>()
            );
        }

        public static Notification success(String title, String message) {
            return new Notification(
                UUID.randomUUID().toString(),
                NotificationType.SUCCESS,
                title,
                message,
                System.currentTimeMillis(),
                new HashMap<>()
            );
        }

        public static Notification warning(String title, String message) {
            return new Notification(
                UUID.randomUUID().toString(),
                NotificationType.WARNING,
                title,
                message,
                System.currentTimeMillis(),
                new HashMap<>()
            );
        }

        public static Notification error(String title, String message) {
            return new Notification(
                UUID.randomUUID().toString(),
                NotificationType.ERROR,
                title,
                message,
                System.currentTimeMillis(),
                new HashMap<>()
            );
        }
    }

    /**
     * Notification listener interface.
     */
    @FunctionalInterface
    public interface NotificationListener {
        void onNotification(Notification notification);
    }

    /**
     * Add a notification listener.
     */
    public static void addListener(NotificationListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a notification listener.
     */
    public static void removeListener(NotificationListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all listeners.
     */
    public static void notify(Notification notification) {
        executor.submit(() -> {
            for (NotificationListener listener : listeners) {
                try {
                    listener.onNotification(notification);
                } catch (Exception e) {
                    // Ignore listener errors
                }
            }
        });
    }

    /**
     * Show info notification.
     */
    public static void info(String title, String message) {
        notify(Notification.info(title, message));
    }

    /**
     * Show success notification.
     */
    public static void success(String title, String message) {
        notify(Notification.success(title, message));
    }

    /**
     * Show warning notification.
     */
    public static void warning(String title, String message) {
        notify(Notification.warning(title, message));
    }

    /**
     * Show error notification.
     */
    public static void error(String title, String message) {
        notify(Notification.error(title, message));
    }

    /**
     * Shutdown the notifier.
     */
    public static void shutdown() {
        executor.shutdown();
        listeners.clear();
    }
}