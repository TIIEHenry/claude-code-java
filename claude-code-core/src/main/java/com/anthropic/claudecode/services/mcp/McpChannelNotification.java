/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/channelNotification
 */
package com.anthropic.claudecode.services.mcp;

import java.time.Instant;

import java.util.concurrent.CompletableFuture;

import java.util.*;
import java.util.concurrent.*;

/**
 * Channel notification - MCP channel notifications.
 */
public final class McpChannelNotification {
    private final Map<String, List<NotificationHandler>> handlers = new ConcurrentHashMap<>();
    private final List<Notification> pendingNotifications = new CopyOnWriteArrayList<>();
    private final int maxPendingNotifications = 100;

    /**
     * Notification handler interface.
     */
    public interface NotificationHandler {
        void handle(Notification notification);
    }

    /**
     * Notification record.
     */
    public record Notification(
        String id,
        String channelId,
        String type,
        String title,
        String message,
        Map<String, Object> data,
        Instant timestamp,
        NotificationPriority priority,
        boolean read
    ) {
        public static Notification of(String channelId, String type, String title, String message) {
            return new Notification(
                UUID.randomUUID().toString(),
                channelId,
                type,
                title,
                message,
                new HashMap<>(),
                Instant.now(),
                NotificationPriority.NORMAL,
                false
            );
        }

        public Notification withData(String key, Object value) {
            Map<String, Object> newData = new HashMap<>(data);
            newData.put(key, value);
            return new Notification(id, channelId, type, title, message, newData, timestamp, priority, read);
        }

        public Notification withPriority(NotificationPriority p) {
            return new Notification(id, channelId, type, title, message, data, timestamp, p, read);
        }

        public Notification markRead() {
            return new Notification(id, channelId, type, title, message, data, timestamp, priority, true);
        }
    }

    /**
     * Notification priority enum.
     */
    public enum NotificationPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    /**
     * Subscribe to channel.
     */
    public void subscribe(String channelId, NotificationHandler handler) {
        handlers.computeIfAbsent(channelId, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    /**
     * Unsubscribe from channel.
     */
    public void unsubscribe(String channelId, NotificationHandler handler) {
        List<NotificationHandler> channelHandlers = handlers.get(channelId);
        if (channelHandlers != null) {
            channelHandlers.remove(handler);
        }
    }

    /**
     * Send notification.
     */
    public void send(Notification notification) {
        // Add to pending
        pendingNotifications.add(notification);
        trimPending();

        // Notify handlers
        List<NotificationHandler> channelHandlers = handlers.get(notification.channelId());
        if (channelHandlers != null) {
            for (NotificationHandler handler : channelHandlers) {
                handler.handle(notification);
            }
        }
    }

    /**
     * Send notification to channel.
     */
    public void send(String channelId, String type, String title, String message) {
        send(Notification.of(channelId, type, title, message));
    }

    /**
     * Get pending notifications.
     */
    public List<Notification> getPendingNotifications() {
        return new ArrayList<>(pendingNotifications);
    }

    /**
     * Get pending for channel.
     */
    public List<Notification> getPendingForChannel(String channelId) {
        return pendingNotifications.stream()
            .filter(n -> n.channelId().equals(channelId))
            .toList();
    }

    /**
     * Get unread notifications.
     */
    public List<Notification> getUnreadNotifications() {
        return pendingNotifications.stream()
            .filter(n -> !n.read())
            .toList();
    }

    /**
     * Mark notification as read.
     */
    public void markRead(String notificationId) {
        for (int i = 0; i < pendingNotifications.size(); i++) {
            Notification n = pendingNotifications.get(i);
            if (n.id().equals(notificationId)) {
                pendingNotifications.set(i, n.markRead());
                break;
            }
        }
    }

    /**
     * Clear notification.
     */
    public void clearNotification(String notificationId) {
        pendingNotifications.removeIf(n -> n.id().equals(notificationId));
    }

    /**
     * Clear all for channel.
     */
    public void clearChannel(String channelId) {
        pendingNotifications.removeIf(n -> n.channelId().equals(channelId));
    }

    /**
     * Clear all notifications.
     */
    public void clearAll() {
        pendingNotifications.clear();
    }

    /**
     * Trim pending list.
     */
    private void trimPending() {
        while (pendingNotifications.size() > maxPendingNotifications) {
            pendingNotifications.remove(0);
        }
    }

    /**
     * Notification summary record.
     */
    public record NotificationSummary(
        int totalCount,
        int unreadCount,
        Map<String, Integer> countByChannel,
        Map<String, Integer> countByType
    ) {
        public String format() {
            return String.format("Total: %d, Unread: %d", totalCount, unreadCount);
        }
    }

    /**
     * Get summary.
     */
    public NotificationSummary getSummary() {
        Map<String, Integer> byChannel = new HashMap<>();
        Map<String, Integer> byType = new HashMap<>();
        int unread = 0;

        for (Notification n : pendingNotifications) {
            byChannel.merge(n.channelId(), 1, Integer::sum);
            byType.merge(n.type(), 1, Integer::sum);
            if (!n.read()) unread++;
        }

        return new NotificationSummary(pendingNotifications.size(), unread, byChannel, byType);
    }
}