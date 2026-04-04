/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/awaySummary
 */
package com.anthropic.claudecode.services.away;

import java.util.*;
import java.time.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Away summary - Summary for when user returns.
 */
public final class AwaySummaryService {

    /**
     * Away summary record.
     */
    public record AwaySummary(
        Instant awaySince,
        Instant returnedAt,
        Duration awayDuration,
        List<EventDuringAway> events,
        List<CompletedTask> completedTasks,
        List<PendingAction> pendingActions,
        int newMessages,
        int newNotifications
    ) {
        public boolean hasActivity() {
            return !events.isEmpty() || !completedTasks.isEmpty();
        }

        public String formatGreeting() {
            StringBuilder sb = new StringBuilder();
            sb.append("Welcome back! You were away for ");
            sb.append(formatDuration(awayDuration));
            sb.append(".\n\n");

            if (hasActivity()) {
                sb.append("While you were away:\n");
                if (!completedTasks.isEmpty()) {
                    sb.append("- ").append(completedTasks.size()).append(" tasks completed\n");
                }
                if (newMessages > 0) {
                    sb.append("- ").append(newMessages).append(" new messages\n");
                }
                if (newNotifications > 0) {
                    sb.append("- ").append(newNotifications).append(" notifications\n");
                }
            } else {
                sb.append("No new activity.\n");
            }

            if (!pendingActions.isEmpty()) {
                sb.append("\nPending actions:\n");
                for (PendingAction action : pendingActions) {
                    sb.append("- ").append(action.description()).append("\n");
                }
            }

            return sb.toString();
        }

        private String formatDuration(Duration d) {
            long hours = d.toHours();
            long minutes = d.toMinutesPart();
            if (hours > 0) {
                return String.format("%d hours %d minutes", hours, minutes);
            }
            return String.format("%d minutes", minutes);
        }
    }

    /**
     * Event during away record.
     */
    public record EventDuringAway(
        String type,
        String description,
        Instant timestamp,
        String source,
        Map<String, Object> data
    ) {
        public String format() {
            return String.format("[%s] %s: %s", timestamp, type, description);
        }
    }

    /**
     * Completed task record.
     */
    public record CompletedTask(
        String taskId,
        String description,
        Instant completedAt,
        boolean success,
        String result
    ) {}

    /**
     * Pending action record.
     */
    public record PendingAction(
        String actionId,
        String type,
        String description,
        Instant createdAt,
        int priority,
        boolean requiresUserInput
    ) {
        public static PendingAction of(String type, String description) {
            return new PendingAction(
                UUID.randomUUID().toString(),
                type,
                description,
                Instant.now(),
                0,
                false
            );
        }

        public PendingAction withPriority(int priority) {
            return new PendingAction(actionId, type, description, createdAt, priority, requiresUserInput);
        }

        public PendingAction requiresInput() {
            return new PendingAction(actionId, type, description, createdAt, priority, true);
        }
    }

    private volatile Instant awaySince = null;
    private final List<EventDuringAway> events = new ArrayList<>();
    private final List<CompletedTask> completedTasks = new ArrayList<>();
    private final List<PendingAction> pendingActions = new ArrayList<>();
    private final List<AwayListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Mark user as away.
     */
    public void markAway() {
        awaySince = Instant.now();
        events.clear();
        completedTasks.clear();
    }

    /**
     * Mark user as returned.
     */
    public AwaySummary markReturned() {
        if (awaySince == null) {
            return new AwaySummary(
                Instant.now(),
                Instant.now(),
                Duration.ZERO,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                0, 0
            );
        }

        AwaySummary summary = new AwaySummary(
            awaySince,
            Instant.now(),
            Duration.between(awaySince, Instant.now()),
            new ArrayList<>(events),
            new ArrayList<>(completedTasks),
            new ArrayList<>(pendingActions),
            countNewMessages(),
            countNewNotifications()
        );

        awaySince = null;
        notifyListeners(summary);

        return summary;
    }

    /**
     * Record event.
     */
    public void recordEvent(String type, String description, Map<String, Object> data) {
        if (awaySince == null) return;

        events.add(new EventDuringAway(
            type,
            description,
            Instant.now(),
            "system",
            data != null ? data : Collections.emptyMap()
        ));
    }

    /**
     * Record completed task.
     */
    public void recordCompletedTask(CompletedTask task) {
        if (awaySince == null) return;
        completedTasks.add(task);
    }

    /**
     * Add pending action.
     */
    public void addPendingAction(PendingAction action) {
        pendingActions.add(action);
    }

    /**
     * Remove pending action.
     */
    public void removePendingAction(String actionId) {
        pendingActions.removeIf(a -> a.actionId().equals(actionId));
    }

    /**
     * Check if user is away.
     */
    public boolean isAway() {
        return awaySince != null;
    }

    /**
     * Get away duration.
     */
    public Duration getAwayDuration() {
        if (awaySince == null) return Duration.ZERO;
        return Duration.between(awaySince, Instant.now());
    }

    private int countNewMessages() {
        return (int) events.stream()
            .filter(e -> e.type().equals("message"))
            .count();
    }

    private int countNewNotifications() {
        return (int) events.stream()
            .filter(e -> e.type().equals("notification"))
            .count();
    }

    /**
     * Add listener.
     */
    public void addListener(AwayListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener.
     */
    public void removeListener(AwayListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(AwaySummary summary) {
        for (AwayListener listener : listeners) {
            listener.onReturn(summary);
        }
    }

    /**
     * Away listener interface.
     */
    public interface AwayListener {
        void onReturn(AwaySummary summary);
    }
}