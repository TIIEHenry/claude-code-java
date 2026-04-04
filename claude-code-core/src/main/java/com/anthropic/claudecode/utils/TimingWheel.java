/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code timing wheel
 */
package com.anthropic.claudecode.utils;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Timing wheel for efficient timer management.
 */
public final class TimingWheel {
    private final int tickCount;
    private final Duration tickDuration;
    private final List<Set<TimerTask>> wheel;
    private final AtomicInteger currentTick = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    public TimingWheel(int tickCount, Duration tickDuration) {
        this.tickCount = tickCount;
        this.tickDuration = tickDuration;
        this.wheel = new ArrayList<>(tickCount);
        for (int i = 0; i < tickCount; i++) {
            wheel.add(ConcurrentHashMap.newKeySet());
        }
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Start the timing wheel.
     */
    public void start() {
        if (running) return;
        running = true;
        scheduler.scheduleAtFixedRate(
            this::tick,
            tickDuration.toMillis(),
            tickDuration.toMillis(),
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stop the timing wheel.
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
    }

    /**
     * Add timer.
     */
    public TimerTask addTimer(Duration delay, Runnable callback) {
        return addTimer(delay, callback, null);
    }

    /**
     * Add timer with cancellation handler.
     */
    public TimerTask addTimer(Duration delay, Runnable callback, Runnable onCancel) {
        long delayMs = delay.toMillis();
        long tickMs = tickDuration.toMillis();
        int ticks = (int) Math.ceil((double) delayMs / tickMs);
        int targetTick = (currentTick.get() + ticks) % tickCount;

        TimerTask task = new TimerTask(callback, onCancel, targetTick);
        wheel.get(targetTick).add(task);
        return task;
    }

    /**
     * Cancel timer.
     */
    public boolean cancelTimer(TimerTask task) {
        if (task == null || task.tickIndex < 0 || task.tickIndex >= tickCount) {
            return false;
        }
        boolean removed = wheel.get(task.tickIndex).remove(task);
        if (removed && task.onCancel != null) {
            task.onCancel.run();
        }
        return removed;
    }

    /**
     * Process current tick.
     */
    private void tick() {
        int tick = currentTick.get();
        Set<TimerTask> tasks = wheel.get(tick);

        for (TimerTask task : tasks) {
            if (!task.cancelled) {
                try {
                    task.callback.run();
                } catch (Exception ignored) {}
            }
        }
        tasks.clear();

        currentTick.set((tick + 1) % tickCount);
    }

    /**
     * Get pending task count.
     */
    public int getPendingCount() {
        return wheel.stream().mapToInt(Set::size).sum();
    }

    /**
     * Check if running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Timer task.
     */
    public final class TimerTask {
        private final Runnable callback;
        private final Runnable onCancel;
        private final int tickIndex;
        private volatile boolean cancelled = false;

        private TimerTask(Runnable callback, Runnable onCancel, int tickIndex) {
            this.callback = callback;
            this.onCancel = onCancel;
            this.tickIndex = tickIndex;
        }

        public void cancel() {
            cancelled = true;
            cancelTimer(this);
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    // Helper
    private static final class AtomicInteger extends java.util.concurrent.atomic.AtomicInteger {
        public AtomicInteger(int value) { super(value); }
    }

    /**
     * Timing wheel utilities.
     */
    public static final class TimingWheelUtils {
        private TimingWheelUtils() {}

        /**
         * Create with 60 slots of 1 second.
         */
        public static TimingWheel seconds() {
            return new TimingWheel(60, Duration.ofSeconds(1));
        }

        /**
         * Create with 60 slots of 1 minute.
         */
        public static TimingWheel minutes() {
            return new TimingWheel(60, Duration.ofMinutes(1));
        }

        /**
         * Create with 100ms precision.
         */
        public static TimingWheel highPrecision() {
            return new TimingWheel(100, Duration.ofMillis(100));
        }
    }
}