/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/activityManager
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Activity manager - Track and manage background activities.
 */
public final class ActivityManager {
    private final Map<String, Activity> activities = new ConcurrentHashMap<>();
    private final List<ActivityListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Start an activity.
     */
    public Activity startActivity(String id, String description) {
        Activity activity = new Activity(id, description, ActivityState.RUNNING, System.currentTimeMillis());
        activities.put(id, activity);
        notifyListeners(activity, ActivityEvent.STARTED);
        return activity;
    }

    /**
     * Complete an activity.
     */
    public void completeActivity(String id) {
        Activity activity = activities.get(id);
        if (activity != null) {
            Activity completed = new Activity(activity.id(), activity.description(), ActivityState.COMPLETED, activity.startTime());
            activities.put(id, completed);
            notifyListeners(completed, ActivityEvent.COMPLETED);
        }
    }

    /**
     * Fail an activity.
     */
    public void failActivity(String id, String error) {
        Activity activity = activities.get(id);
        if (activity != null) {
            Activity failed = new Activity(activity.id(), activity.description(), ActivityState.FAILED, activity.startTime());
            activities.put(id, failed);
            notifyListeners(failed, ActivityEvent.FAILED);
        }
    }

    /**
     * Cancel an activity.
     */
    public void cancelActivity(String id) {
        Activity activity = activities.get(id);
        if (activity != null) {
            Activity cancelled = new Activity(activity.id(), activity.description(), ActivityState.CANCELLED, activity.startTime());
            activities.put(id, cancelled);
            notifyListeners(cancelled, ActivityEvent.CANCELLED);
        }
    }

    /**
     * Get activity by ID.
     */
    public Activity getActivity(String id) {
        return activities.get(id);
    }

    /**
     * Get all activities.
     */
    public Collection<Activity> getAllActivities() {
        return Collections.unmodifiableCollection(activities.values());
    }

    /**
     * Get running activities.
     */
    public List<Activity> getRunningActivities() {
        return activities.values().stream()
            .filter(a -> a.state() == ActivityState.RUNNING)
            .toList();
    }

    /**
     * Check if any activities are running.
     */
    public boolean hasRunningActivities() {
        return activities.values().stream()
            .anyMatch(a -> a.state() == ActivityState.RUNNING);
    }

    /**
     * Clear completed activities.
     */
    public void clearCompleted() {
        activities.entrySet().removeIf(e ->
            e.getValue().state() == ActivityState.COMPLETED ||
            e.getValue().state() == ActivityState.FAILED ||
            e.getValue().state() == ActivityState.CANCELLED
        );
    }

    /**
     * Add activity listener.
     */
    public void addListener(ActivityListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove activity listener.
     */
    public void removeListener(ActivityListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(Activity activity, ActivityEvent event) {
        for (ActivityListener listener : listeners) {
            try {
                listener.onActivityEvent(activity, event);
            } catch (Exception e) {
                // Ignore listener errors
            }
        }
    }

    /**
     * Activity record.
     */
    public record Activity(
        String id,
        String description,
        ActivityState state,
        long startTime
    ) {
        public long getDuration() {
            return System.currentTimeMillis() - startTime;
        }
    }

    /**
     * Activity state enum.
     */
    public enum ActivityState {
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * Activity event enum.
     */
    public enum ActivityEvent {
        STARTED,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * Activity listener interface.
     */
    public interface ActivityListener {
        void onActivityEvent(Activity activity, ActivityEvent event);
    }

    // Singleton instance for static access
    private static final ActivityManager INSTANCE = new ActivityManager();

    public static ActivityManager getInstance() {
        return INSTANCE;
    }

    // Static method for last interaction time
    private volatile long lastInteractionTime = System.currentTimeMillis();

    public long getLastInteractionTime() {
        return lastInteractionTime;
    }

    public void updateLastInteractionTime() {
        this.lastInteractionTime = System.currentTimeMillis();
    }
}