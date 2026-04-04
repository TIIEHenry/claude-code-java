/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code cron tasks
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/**
 * Scheduled tasks stored in .claude/scheduled_tasks.json.
 */
public final class CronTasks {
    private CronTasks() {}

    private static final String CRON_FILE_REL = ".claude/scheduled_tasks.json";
    private static final int MAX_JOBS = 50;

    /**
     * Cron task definition.
     */
    public record CronTask(
            String id,
            String cron,
            String prompt,
            long createdAt,
            Long lastFiredAt,
            boolean recurring,
            boolean permanent,
            Boolean durable,
            String agentId
    ) {}

    /**
     * Cron file format.
     */
    private record CronFile(List<CronTask> tasks) {}

    /**
     * Get the cron file path.
     */
    public static Path getCronFilePath() {
        return getCronFilePath(null);
    }

    /**
     * Get the cron file path with explicit directory.
     */
    public static Path getCronFilePath(Path dir) {
        Path root = dir != null ? dir : Paths.get(System.getProperty("user.dir"));
        return root.resolve(CRON_FILE_REL);
    }

    /**
     * Read and parse scheduled_tasks.json.
     */
    public static List<CronTask> readCronTasks() {
        return readCronTasks(null);
    }

    /**
     * Read and parse scheduled_tasks.json with explicit directory.
     */
    public static List<CronTask> readCronTasks(Path dir) {
        try {
            Path path = getCronFilePath(dir);
            if (!Files.exists(path)) {
                return new ArrayList<>();
            }

            String raw = Files.readString(path);
            Map<String, Object> parsed = SlowOperations.jsonParseMap(raw);
            List<Map<String, Object>> tasksList = (List<Map<String, Object>>) parsed.get("tasks");

            if (tasksList == null) {
                return new ArrayList<>();
            }

            List<CronTask> result = new ArrayList<>();
            for (Map<String, Object> t : tasksList) {
                try {
                    String id = (String) t.get("id");
                    String cron = (String) t.get("cron");
                    String prompt = (String) t.get("prompt");
                    Number createdAt = (Number) t.get("createdAt");

                    if (id == null || cron == null || prompt == null || createdAt == null) {
                        continue;
                    }

                    // Validate cron
                    if (Cron.parseCronExpression(cron) == null) {
                        continue;
                    }

                    Long lastFiredAt = t.get("lastFiredAt") instanceof Number
                            ? ((Number) t.get("lastFiredAt")).longValue()
                            : null;

                    boolean recurring = Boolean.TRUE.equals(t.get("recurring"));
                    boolean permanent = Boolean.TRUE.equals(t.get("permanent"));

                    result.add(new CronTask(
                            id, cron, prompt,
                            createdAt.longValue(),
                            lastFiredAt,
                            recurring,
                            permanent,
                            null,
                            (String) t.get("agentId")
                    ));
                } catch (Exception e) {
                    // Skip malformed task
                }
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Write tasks to scheduled_tasks.json.
     */
    public static void writeCronTasks(List<CronTask> tasks) {
        writeCronTasks(tasks, null);
    }

    /**
     * Write tasks to scheduled_tasks.json with explicit directory.
     */
    public static void writeCronTasks(List<CronTask> tasks, Path dir) {
        try {
            Path path = getCronFilePath(dir);
            Files.createDirectories(path.getParent());

            // Strip durable flag for serialization
            List<Map<String, Object>> tasksList = new ArrayList<>();
            for (CronTask task : tasks) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", task.id());
                map.put("cron", task.cron());
                map.put("prompt", task.prompt());
                map.put("createdAt", task.createdAt());
                if (task.lastFiredAt() != null) {
                    map.put("lastFiredAt", task.lastFiredAt());
                }
                if (task.recurring()) {
                    map.put("recurring", true);
                }
                if (task.permanent()) {
                    map.put("permanent", true);
                }
                tasksList.add(map);
            }

            Map<String, Object> file = new LinkedHashMap<>();
            file.put("tasks", tasksList);

            Files.writeString(path, SlowOperations.jsonStringify(file));
        } catch (Exception e) {
            Debug.log("Failed to write cron tasks: " + e.getMessage());
        }
    }

    /**
     * Add a new cron task.
     */
    public static String addCronTask(String cron, String prompt, boolean recurring, boolean durable) {
        return addCronTask(cron, prompt, recurring, durable, null);
    }

    /**
     * Add a new cron task with agent ID.
     */
    public static String addCronTask(String cron, String prompt, boolean recurring, boolean durable, String agentId) {
        String id = UUID.randomUUID().toString().substring(0, 8);

        CronTask task = new CronTask(
                id, cron, prompt,
                System.currentTimeMillis(),
                null,
                recurring,
                false,
                durable ? null : false,
                agentId
        );

        if (!durable) {
            // Session-only task
            addSessionCronTask(task);
            return id;
        }

        List<CronTask> tasks = readCronTasks();
        if (tasks.size() >= MAX_JOBS) {
            throw new IllegalStateException("Maximum number of scheduled tasks reached");
        }
        tasks.add(task);
        writeCronTasks(tasks);
        return id;
    }

    /**
     * Remove tasks by ID.
     */
    public static void removeCronTasks(List<String> ids) {
        removeCronTasks(ids, null);
    }

    /**
     * Remove tasks by ID with explicit directory.
     */
    public static void removeCronTasks(List<String> ids, Path dir) {
        if (ids.isEmpty()) return;

        Set<String> idSet = new HashSet<>(ids);
        List<CronTask> tasks = readCronTasks(dir);
        List<CronTask> remaining = tasks.stream()
                .filter(t -> !idSet.contains(t.id()))
                .toList();

        if (remaining.size() < tasks.size()) {
            writeCronTasks(remaining, dir);
        }
    }

    /**
     * Mark tasks as fired.
     */
    public static void markCronTasksFired(List<String> ids, long firedAt) {
        markCronTasksFired(ids, firedAt, null);
    }

    /**
     * Mark tasks as fired with explicit directory.
     */
    public static void markCronTasksFired(List<String> ids, long firedAt, Path dir) {
        if (ids.isEmpty()) return;

        Set<String> idSet = new HashSet<>(ids);
        List<CronTask> tasks = readCronTasks(dir);
        List<CronTask> updated = new ArrayList<>();

        for (CronTask task : tasks) {
            if (idSet.contains(task.id())) {
                updated.add(new CronTask(
                        task.id(), task.cron(), task.prompt(),
                        task.createdAt(), firedAt,
                        task.recurring(), task.permanent(),
                        task.durable(), task.agentId()
                ));
            } else {
                updated.add(task);
            }
        }

        writeCronTasks(updated, dir);
    }

    // Session-only tasks storage
    private static final List<CronTask> sessionTasks = new ArrayList<>();

    /**
     * Add a session-only cron task.
     */
    public static void addSessionCronTask(CronTask task) {
        sessionTasks.add(task);
    }

    /**
     * Get session cron tasks.
     */
    public static List<CronTask> getSessionCronTasks() {
        return new ArrayList<>(sessionTasks);
    }

    /**
     * Remove session cron tasks by ID.
     */
    public static int removeSessionCronTasks(List<String> ids) {
        Set<String> idSet = new HashSet<>(ids);
        int initialSize = sessionTasks.size();
        sessionTasks.removeIf(t -> idSet.contains(t.id()));
        return initialSize - sessionTasks.size();
    }

    /**
     * List all cron tasks (file + session).
     */
    public static List<CronTask> listAllCronTasks() {
        List<CronTask> all = new ArrayList<>(readCronTasks());
        for (CronTask sessionTask : sessionTasks) {
            all.add(new CronTask(
                    sessionTask.id(), sessionTask.cron(), sessionTask.prompt(),
                    sessionTask.createdAt(), sessionTask.lastFiredAt(),
                    sessionTask.recurring(), sessionTask.permanent(),
                    false, // durable: false for session tasks
                    sessionTask.agentId()
            ));
        }
        return all;
    }

    /**
     * Next fire time for a cron string.
     */
    public static Long nextCronRunMs(String cron, long fromMs) {
        Cron.CronFields fields = Cron.parseCronExpression(cron);
        if (fields == null) return null;
        LocalDateTime next = Cron.computeNextCronRun(fields, Instant.ofEpochMilli(fromMs).atZone(ZoneId.systemDefault()).toLocalDateTime());
        return next != null ? next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }

    /**
     * Calculate jitter fraction from task ID.
     */
    public static double jitterFrac(String taskId) {
        try {
            String hex = taskId.length() >= 8 ? taskId.substring(0, 8) : taskId;
            long value = Long.parseLong(hex, 16);
            return (double) value / 0x1_0000_0000L;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Jittered next fire time for recurring tasks.
     */
    public static Long jitteredNextCronRunMs(String cron, long fromMs, String taskId) {
        return jitteredNextCronRunMs(cron, fromMs, taskId, CronJitterConfig.getConfig());
    }

    /**
     * Jittered next fire time for recurring tasks with config.
     */
    public static Long jitteredNextCronRunMs(String cron, long fromMs, String taskId, CronJitterConfig.Config cfg) {
        Long t1 = nextCronRunMs(cron, fromMs);
        if (t1 == null) return null;

        Long t2 = nextCronRunMs(cron, t1);
        if (t2 == null) return t1;

        long interval = t2 - t1;
        double jitter = Math.min(jitterFrac(taskId) * cfg.recurringFrac() * interval, cfg.recurringCapMs());
        return t1 + (long) jitter;
    }

    /**
     * Jittered next fire time for one-shot tasks.
     */
    public static Long oneShotJitteredNextCronRunMs(String cron, long fromMs, String taskId) {
        return oneShotJitteredNextCronRunMs(cron, fromMs, taskId, CronJitterConfig.getConfig());
    }

    /**
     * Jittered next fire time for one-shot tasks with config.
     */
    public static Long oneShotJitteredNextCronRunMs(String cron, long fromMs, String taskId, CronJitterConfig.Config cfg) {
        Long t1 = nextCronRunMs(cron, fromMs);
        if (t1 == null) return null;

        // Check if minute is a round number
        ZonedDateTime fireTime = Instant.ofEpochMilli(t1).atZone(ZoneId.systemDefault());
        if (fireTime.getMinute() % cfg.oneShotMinuteMod() != 0) {
            return t1;
        }

        double lead = cfg.oneShotFloorMs() + jitterFrac(taskId) * (cfg.oneShotMaxMs() - cfg.oneShotFloorMs());
        return Math.max(t1 - (long) lead, fromMs);
    }

    /**
     * Find missed tasks.
     */
    public static List<CronTask> findMissedTasks(List<CronTask> tasks, long nowMs) {
        return tasks.stream()
                .filter(t -> {
                    Long next = nextCronRunMs(t.cron(), t.createdAt());
                    return next != null && next < nowMs;
                })
                .toList();
    }
}