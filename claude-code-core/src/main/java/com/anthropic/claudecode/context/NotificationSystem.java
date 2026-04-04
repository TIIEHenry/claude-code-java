/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code context
 */
package com.anthropic.claudecode.context;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Notification system.
 */
public class NotificationSystem {
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();
    private final List<Consumer<Notification>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Add a notification.
     */
    public void add(Notification notification) {
        notifications.add(notification);
        listeners.forEach(l -> l.accept(notification));
    }

    /**
     * Remove a notification.
     */
    public void remove(String id) {
        notifications.removeIf(n -> n.id().equals(id));
    }

    /**
     * Get all notifications.
     */
    public List<Notification> getAll() {
        return new ArrayList<>(notifications);
    }

    /**
     * Clear all notifications.
     */
    public void clear() {
        notifications.clear();
    }

    /**
     * Subscribe to notifications.
     */
    public void subscribe(Consumer<Notification> listener) {
        listeners.add(listener);
    }

    /**
     * Unsubscribe.
     */
    public void unsubscribe(Consumer<Notification> listener) {
        listeners.remove(listener);
    }

    /**
     * Notification record.
     */
    public record Notification(
        String id,
        String type,
        String title,
        String message,
        long timestamp,
        Map<String, Object> data
    ) {
        public Notification(String type, String title, String message) {
            this(UUID.randomUUID().toString(), type, title, message, System.currentTimeMillis(), Map.of());
        }
    }
}