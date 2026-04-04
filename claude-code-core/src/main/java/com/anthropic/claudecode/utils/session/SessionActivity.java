/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code session activity tracking
 */
package com.anthropic.claudecode.utils.session;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Session activity tracking with refcount-based heartbeat timer.
 *
 * The transport registers its keep-alive sender via registerSessionActivityCallback().
 * Callers bracket their work with startSessionActivity() / stopSessionActivity().
 * When the refcount is >0 a periodic timer fires the registered callback every 30 seconds.
 */
public final class SessionActivity {
    private SessionActivity() {}

    private static final long SESSION_ACTIVITY_INTERVAL_MS = 30_000;

    public enum ActivityReason {
        API_CALL,
        TOOL_EXEC
    }

    private static volatile Runnable activityCallback = null;
    private static final AtomicInteger refcount = new AtomicInteger(0);
    private static final ConcurrentHashMap<ActivityReason, AtomicInteger> activeReasons = new ConcurrentHashMap<>();
    private static volatile Long oldestActivityStartedAt = null;
    private static volatile ScheduledFuture<?> heartbeatTimer = null;
    private static volatile ScheduledFuture<?> idleTimer = null;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Start the heartbeat timer.
     */
    private static void startHeartbeatTimer() {
        clearIdleTimer();

        heartbeatTimer = scheduler.scheduleAtFixedRate(() -> {
            if (Boolean.parseBoolean(System.getenv("CLAUDE_CODE_REMOTE_SEND_KEEPALIVES"))) {
                if (activityCallback != null) {
                    activityCallback.run();
                }
            }
        }, SESSION_ACTIVITY_INTERVAL_MS, SESSION_ACTIVITY_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Start the idle timer.
     */
    private static void startIdleTimer() {
        clearIdleTimer();

        if (activityCallback == null) {
            return;
        }

        idleTimer = scheduler.schedule(() -> {
            idleTimer = null;
        }, SESSION_ACTIVITY_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Clear the idle timer.
     */
    private static void clearIdleTimer() {
        ScheduledFuture<?> timer = idleTimer;
        if (timer != null) {
            timer.cancel(false);
            idleTimer = null;
        }
    }

    /**
     * Register a session activity callback.
     */
    public static void registerSessionActivityCallback(Runnable cb) {
        activityCallback = cb;
        // Restart timer if work is already in progress
        if (refcount.get() > 0 && heartbeatTimer == null) {
            startHeartbeatTimer();
        }
    }

    /**
     * Unregister the session activity callback.
     */
    public static void unregisterSessionActivityCallback() {
        activityCallback = null;
        // Stop timer if the callback is removed
        ScheduledFuture<?> timer = heartbeatTimer;
        if (timer != null) {
            timer.cancel(false);
            heartbeatTimer = null;
        }
        clearIdleTimer();
    }

    /**
     * Send a session activity signal directly.
     */
    public static void sendSessionActivitySignal() {
        if (Boolean.parseBoolean(System.getenv("CLAUDE_CODE_REMOTE_SEND_KEEPALIVES"))) {
            if (activityCallback != null) {
                activityCallback.run();
            }
        }
    }

    /**
     * Check if session activity tracking is active.
     */
    public static boolean isSessionActivityTrackingActive() {
        return activityCallback != null;
    }

    /**
     * Increment the activity refcount.
     * When it transitions from 0→1 and a callback is registered, start the heartbeat timer.
     */
    public static void startSessionActivity(ActivityReason reason) {
        refcount.incrementAndGet();
        activeReasons.computeIfAbsent(reason, k -> new AtomicInteger(0)).incrementAndGet();

        if (refcount.get() == 1) {
            oldestActivityStartedAt = System.currentTimeMillis();
            if (activityCallback != null && heartbeatTimer == null) {
                startHeartbeatTimer();
            }
        }
    }

    /**
     * Decrement the activity refcount.
     * When it reaches 0, stop the heartbeat timer and start an idle timer.
     */
    public static void stopSessionActivity(ActivityReason reason) {
        int count = refcount.get();
        if (count > 0) {
            refcount.decrementAndGet();
        }

        AtomicInteger reasonCount = activeReasons.get(reason);
        if (reasonCount != null) {
            int n = reasonCount.decrementAndGet();
            if (n <= 0) {
                activeReasons.remove(reason);
            }
        }

        if (refcount.get() == 0 && heartbeatTimer != null) {
            heartbeatTimer.cancel(false);
            heartbeatTimer = null;
            startIdleTimer();
        }
    }

    /**
     * Get current refcount.
     */
    public static int getRefcount() {
        return refcount.get();
    }

    /**
     * Get active reasons snapshot.
     */
    public static Map<ActivityReason, Integer> getActiveReasons() {
        Map<ActivityReason, Integer> result = new HashMap<>();
        activeReasons.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    /**
     * Get oldest activity start time.
     */
    public static Long getOldestActivityStartedAt() {
        return oldestActivityStartedAt;
    }

    /**
     * Get activity duration in milliseconds.
     */
    public static long getActivityDurationMs() {
        Long started = oldestActivityStartedAt;
        if (started != null && refcount.get() > 0) {
            return System.currentTimeMillis() - started;
        }
        return 0;
    }

    /**
     * Shutdown the scheduler.
     */
    public static void shutdown() {
        scheduler.shutdown();
    }
}