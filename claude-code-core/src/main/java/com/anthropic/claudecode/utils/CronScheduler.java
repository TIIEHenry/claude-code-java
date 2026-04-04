/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code cron scheduler
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Cron scheduler for scheduled tasks.
 */
public final class CronScheduler {
    private CronScheduler() {}

    private static final long CHECK_INTERVAL_MS = 1000;
    private static final long FILE_STABILITY_MS = 300;
    private static final long LOCK_PROBE_INTERVAL_MS = 5000;

    /**
     * Scheduler options.
     */
    public record SchedulerOptions(
            java.util.function.Consumer<String> onFire,
            java.util.function.Supplier<Boolean> isLoading,
            boolean assistantMode,
            java.util.function.Consumer<CronTasks.CronTask> onFireTask,
            java.util.function.Consumer<List<CronTasks.CronTask>> onMissed,
            Path dir,
            String lockIdentity,
            java.util.function.Supplier<CronJitterConfig.Config> getJitterConfig,
            java.util.function.Supplier<Boolean> isKilled,
            java.util.function.Predicate<CronTasks.CronTask> filter
    ) {}

    /**
     * Scheduler instance.
     */
    public static class Scheduler {
        private ScheduledExecutorService executor;
        private ScheduledFuture<?> checkTask;
        private List<CronTasks.CronTask> tasks = new ArrayList<>();
        private Map<String, Long> nextFireAt = new ConcurrentHashMap<>();
        private Set<String> missedAsked = ConcurrentHashMap.newKeySet();
        private Set<String> inFlight = ConcurrentHashMap.newKeySet();
        private volatile boolean stopped = false;
        private volatile boolean isOwner = true;
        private final SchedulerOptions options;

        public Scheduler(SchedulerOptions options) {
            this.options = options;
        }

        /**
         * Start the scheduler.
         */
        public void start() {
            stopped = false;
            executor = Executors.newSingleThreadScheduledExecutor();

            // Load tasks
            loadTasks(true);

            // Start check timer
            checkTask = executor.scheduleAtFixedRate(
                    this::check,
                    CHECK_INTERVAL_MS,
                    CHECK_INTERVAL_MS,
                    TimeUnit.MILLISECONDS
            );

            Debug.log("[ScheduledTasks] Scheduler started");
        }

        /**
         * Stop the scheduler.
         */
        public void stop() {
            stopped = true;
            if (checkTask != null) {
                checkTask.cancel(false);
            }
            if (executor != null) {
                executor.shutdown();
            }
            Debug.log("[ScheduledTasks] Scheduler stopped");
        }

        /**
         * Get next fire time.
         */
        public Long getNextFireTime() {
            long min = Long.MAX_VALUE;
            for (Long time : nextFireAt.values()) {
                if (time < min && time != Long.MAX_VALUE) {
                    min = time;
                }
            }
            return min == Long.MAX_VALUE ? null : min;
        }

        private void loadTasks(boolean initial) {
            Path dir = options.dir();
            tasks = CronTasks.readCronTasks(dir);

            if (!initial) return;

            // Check for missed tasks on initial load
            long now = System.currentTimeMillis();
            List<CronTasks.CronTask> missed = CronTasks.findMissedTasks(tasks, now).stream()
                    .filter(t -> !t.recurring() && !missedAsked.contains(t.id()))
                    .filter(t -> options.filter() == null || options.filter().test(t))
                    .toList();

            if (!missed.isEmpty()) {
                for (CronTasks.CronTask t : missed) {
                    missedAsked.add(t.id());
                    nextFireAt.put(t.id(), Long.MAX_VALUE);
                }

                if (options.onMissed() != null) {
                    options.onMissed().accept(missed);
                } else if (options.onFire() != null) {
                    options.onFire().accept(buildMissedTaskNotification(missed));
                }

                CronTasks.removeCronTasks(
                        missed.stream().map(CronTasks.CronTask::id).toList(),
                        dir
                );
            }
        }

        private void check() {
            if (options.isKilled() != null && options.isKilled().get()) return;
            if (options.isLoading() != null && options.isLoading().get() && !options.assistantMode()) return;

            long now = System.currentTimeMillis();
            Set<String> seen = new HashSet<>();
            List<String> firedFileRecurring = new ArrayList<>();

            CronJitterConfig.Config jitterCfg = options.getJitterConfig() != null
                    ? options.getJitterConfig().get()
                    : CronJitterConfig.getConfig();

            // Process file-backed tasks
            if (isOwner) {
                for (CronTasks.CronTask t : tasks) {
                    if (options.filter() != null && !options.filter().test(t)) continue;
                    processTask(t, false, now, jitterCfg, seen, firedFileRecurring);
                }

                // Batch write lastFiredAt
                if (!firedFileRecurring.isEmpty()) {
                    CronTasks.markCronTasksFired(firedFileRecurring, now, options.dir());
                }
            }

            // Process session tasks
            if (options.dir() == null) {
                for (CronTasks.CronTask t : CronTasks.getSessionCronTasks()) {
                    processTask(t, true, now, jitterCfg, seen, firedFileRecurring);
                }
            }

            // Evict stale entries
            if (seen.isEmpty()) {
                nextFireAt.clear();
            } else {
                nextFireAt.keySet().removeIf(id -> !seen.contains(id));
            }
        }

        private void processTask(
                CronTasks.CronTask t,
                boolean isSession,
                long now,
                CronJitterConfig.Config jitterCfg,
                Set<String> seen,
                List<String> firedFileRecurring
        ) {
            seen.add(t.id());
            if (inFlight.contains(t.id())) return;

            Long next = nextFireAt.get(t.id());
            if (next == null) {
                // First sight - compute next fire time
                long anchor = t.recurring()
                        ? (t.lastFiredAt() != null ? t.lastFiredAt() : t.createdAt())
                        : t.createdAt();

                next = t.recurring()
                        ? CronTasks.jitteredNextCronRunMs(t.cron(), anchor, t.id(), jitterCfg)
                        : CronTasks.oneShotJitteredNextCronRunMs(t.cron(), anchor, t.id(), jitterCfg);

                if (next == null) next = Long.MAX_VALUE;
                nextFireAt.put(t.id(), next);
            }

            if (now < next) return;

            Debug.log("[ScheduledTasks] firing " + t.id() + (t.recurring() ? " (recurring)" : ""));

            // Fire the task
            if (options.onFireTask() != null) {
                options.onFireTask().accept(t);
            } else if (options.onFire() != null) {
                options.onFire().accept(t.prompt());
            }

            // Check if recurring task has aged out
            boolean aged = isRecurringTaskAged(t, now, jitterCfg.recurringMaxAgeMs());

            if (t.recurring() && !aged) {
                // Reschedule
                Long newNext = CronTasks.jitteredNextCronRunMs(t.cron(), now, t.id(), jitterCfg);
                nextFireAt.put(t.id(), newNext != null ? newNext : Long.MAX_VALUE);
                if (!isSession) firedFileRecurring.add(t.id());
            } else if (isSession) {
                // One-shot session task - remove immediately
                CronTasks.removeSessionCronTasks(List.of(t.id()));
                nextFireAt.remove(t.id());
            } else {
                // One-shot file task - mark for removal
                inFlight.add(t.id());
                CronTasks.removeCronTasks(List.of(t.id()), options.dir());
                inFlight.remove(t.id());
                nextFireAt.remove(t.id());
            }
        }

        /**
         * Check if a recurring task has aged out.
         */
        public static boolean isRecurringTaskAged(CronTasks.CronTask t, long nowMs, long maxAgeMs) {
            if (maxAgeMs == 0) return false;
            return t.recurring() && !t.permanent() && nowMs - t.createdAt() >= maxAgeMs;
        }

        /**
         * Build missed task notification.
         */
        public static String buildMissedTaskNotification(List<CronTasks.CronTask> missed) {
            boolean plural = missed.size() > 1;
            StringBuilder sb = new StringBuilder();
            sb.append("The following one-shot scheduled task")
                    .append(plural ? "s were" : " was")
                    .append(" missed while Claude was not running.\n\n");

            for (CronTasks.CronTask t : missed) {
                sb.append("[").append(Cron.cronToHuman(t.cron()))
                        .append(", created ").append(new java.util.Date(t.createdAt()))
                        .append("]\n```\n").append(t.prompt()).append("\n```\n\n");
            }

            return sb.toString();
        }
    }

    /**
     * Create a new scheduler.
     */
    public static Scheduler createScheduler(SchedulerOptions options) {
        return new Scheduler(options);
    }
}