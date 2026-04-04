/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/cronTasks.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.time.*;
import java.time.temporal.ChronoUnit;

/**
 * Cron task utilities for scheduling prompts.
 */
public final class CronUtils {
    private CronUtils() {}

    private static final Map<String, CronTask> sessionTasks = new ConcurrentHashMap<>();
    private static volatile boolean scheduledTasksEnabled = false;

    /**
     * Cron task record.
     */
    public record CronTask(
        String id,
        String cron,
        String prompt,
        boolean recurring,
        boolean durable,
        long createdAt
    ) {}

    /**
     * Check if cron expression is valid.
     */
    public static boolean isValidCron(String cron) {
        if (cron == null || cron.isEmpty()) return false;
        String[] parts = cron.trim().split("\\s+");
        if (parts.length != 5) return false;

        // Validate each field
        return isValidMinute(parts[0]) &&
               isValidHour(parts[1]) &&
               isValidDayOfMonth(parts[2]) &&
               isValidMonth(parts[3]) &&
               isValidDayOfWeek(parts[4]);
    }

    private static boolean isValidMinute(String field) {
        return isValidField(field, 0, 59);
    }

    private static boolean isValidHour(String field) {
        return isValidField(field, 0, 23);
    }

    private static boolean isValidDayOfMonth(String field) {
        return isValidField(field, 1, 31);
    }

    private static boolean isValidMonth(String field) {
        return isValidField(field, 1, 12);
    }

    private static boolean isValidDayOfWeek(String field) {
        return isValidField(field, 0, 6);
    }

    private static boolean isValidField(String field, int min, int max) {
        if (field.equals("*")) return true;
        if (field.startsWith("*/")) {
            try {
                int step = Integer.parseInt(field.substring(2));
                return step > 0 && step <= max;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if (field.contains(",")) {
            for (String part : field.split(",")) {
                if (!isValidSingleValue(part.trim(), min, max)) return false;
            }
            return true;
        }
        if (field.contains("-")) {
            String[] range = field.split("-");
            if (range.length != 2) return false;
            try {
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);
                return start >= min && end <= max && start <= end;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return isValidSingleValue(field, min, max);
    }

    private static boolean isValidSingleValue(String field, int min, int max) {
        try {
            int value = Integer.parseInt(field);
            return value >= min && value <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Get next run time in milliseconds.
     */
    public static Long nextRun(String cron, long fromTime) {
        if (!isValidCron(cron)) return null;

        String[] parts = cron.trim().split("\\s+");
        String minute = parts[0];
        String hour = parts[1];
        String dayOfMonth = parts[2];
        String month = parts[3];
        String dayOfWeek = parts[4];

        // Start from the next minute
        LocalDateTime time = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(fromTime),
            ZoneId.systemDefault()
        ).truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);

        // Search for next matching time (up to 366 days)
        for (int i = 0; i < 527040; i++) { // 366 days * 24 * 60
            if (matchesCron(time, minute, hour, dayOfMonth, month, dayOfWeek)) {
                return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            time = time.plusMinutes(1);
        }

        return null;
    }

    private static boolean matchesCron(LocalDateTime time, String minute, String hour,
                                       String dayOfMonth, String month, String dayOfWeek) {
        return matchesField(minute, time.getMinute(), 0, 59) &&
               matchesField(hour, time.getHour(), 0, 23) &&
               matchesField(dayOfMonth, time.getDayOfMonth(), 1, 31) &&
               matchesField(month, time.getMonthValue(), 1, 12) &&
               matchesField(dayOfWeek, time.getDayOfWeek().getValue() % 7, 0, 6);
    }

    private static boolean matchesField(String field, int value, int min, int max) {
        if (field.equals("*")) return true;
        if (field.startsWith("*/")) {
            int step = Integer.parseInt(field.substring(2));
            return (value - min) % step == 0;
        }
        if (field.contains(",")) {
            for (String part : field.split(",")) {
                if (Integer.parseInt(part.trim()) == value) return true;
            }
            return false;
        }
        if (field.contains("-")) {
            String[] range = field.split("-");
            int start = Integer.parseInt(range[0]);
            int end = Integer.parseInt(range[1]);
            return value >= start && value <= end;
        }
        return Integer.parseInt(field) == value;
    }

    /**
     * Convert cron expression to human-readable format.
     */
    public static String cronToHuman(String cron) {
        if (!isValidCron(cron)) return "invalid";

        String[] parts = cron.trim().split("\\s+");
        String minute = parts[0];
        String hour = parts[1];
        String dayOfMonth = parts[2];
        String month = parts[3];
        String dayOfWeek = parts[4];

        // Every minute
        if (minute.equals("*") && hour.equals("*") && dayOfMonth.equals("*") &&
            month.equals("*") && dayOfWeek.equals("*")) {
            return "every minute";
        }

        // Every N minutes
        if (minute.startsWith("*/") && hour.equals("*") && dayOfMonth.equals("*") &&
            month.equals("*") && dayOfWeek.equals("*")) {
            int n = Integer.parseInt(minute.substring(2));
            return "every " + n + " minute" + (n > 1 ? "s" : "");
        }

        // Every hour at :00
        if (minute.equals("0") && hour.equals("*") && dayOfMonth.equals("*") &&
            month.equals("*") && dayOfWeek.equals("*")) {
            return "hourly";
        }

        // Every N hours
        if (minute.equals("0") && hour.startsWith("*/") && dayOfMonth.equals("*") &&
            month.equals("*") && dayOfWeek.equals("*")) {
            int n = Integer.parseInt(hour.substring(2));
            return "every " + n + " hour" + (n > 1 ? "s" : "");
        }

        // Daily at specific time
        if (!minute.equals("*") && !hour.equals("*") && dayOfMonth.equals("*") &&
            month.equals("*") && dayOfWeek.equals("*")) {
            return "daily at " + formatTime(hour, minute);
        }

        // Weekly
        if (!minute.equals("*") && !hour.equals("*") && dayOfMonth.equals("*") &&
            month.equals("*") && !dayOfWeek.equals("*")) {
            String day = getDayName(dayOfWeek);
            return "every " + day + " at " + formatTime(hour, minute);
        }

        // Default: show the cron expression
        return cron;
    }

    private static String formatTime(String hour, String minute) {
        int h = Integer.parseInt(hour);
        int m = Integer.parseInt(minute);
        String period = h >= 12 ? "pm" : "am";
        h = h % 12;
        if (h == 0) h = 12;
        return h + ":" + String.format("%02d", m) + period;
    }

    private static String getDayName(String dayOfWeek) {
        int d = Integer.parseInt(dayOfWeek);
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return days[d];
    }

    /**
     * Add a cron task.
     */
    public static String addCronTask(String cron, String prompt, boolean recurring, boolean durable) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        CronTask task = new CronTask(id, cron, prompt, recurring, durable, System.currentTimeMillis());
        sessionTasks.put(id, task);

        if (durable) {
            saveDurableTask(task);
        }

        return id;
    }

    /**
     * Remove a cron task.
     */
    public static boolean removeCronTask(String id) {
        CronTask removed = sessionTasks.remove(id);
        if (removed != null && removed.durable()) {
            removeDurableTask(id);
        }
        return removed != null;
    }

    /**
     * Get all cron tasks.
     */
    public static List<CronTask> listAllCronTasks() {
        return new ArrayList<>(sessionTasks.values());
    }

    /**
     * Get cron task count.
     */
    public static int getJobCount() {
        return sessionTasks.size();
    }

    /**
     * Check if scheduled tasks are enabled.
     */
    public static boolean isScheduledTasksEnabled() {
        return scheduledTasksEnabled;
    }

    /**
     * Set scheduled tasks enabled.
     */
    public static void setScheduledTasksEnabled(boolean enabled) {
        scheduledTasksEnabled = enabled;
    }

    /**
     * Get cron file path.
     */
    public static Path getCronFilePath() {
        String home = System.getProperty("user.home");
        String cwd = System.getProperty("user.dir");
        String slug = cwd.replaceAll("[^a-zA-Z0-9]", "-");
        return Paths.get(home, ".claude", "projects", slug, "scheduled_tasks.json");
    }

    private static void saveDurableTask(CronTask task) {
        try {
            Path path = getCronFilePath();
            Files.createDirectories(path.getParent());

            // Read existing tasks
            String existing = "[]";
            if (Files.exists(path)) {
                existing = Files.readString(path);
                if (existing.trim().isEmpty()) existing = "[]";
            }

            // Parse existing JSON array
            List<CronTask> tasks = parseTasksJson(existing);
            tasks.add(task);

            // Write back
            String json = tasksToJson(tasks);
            Files.writeString(path, json);
        } catch (Exception e) {
            // Ignore errors
        }
    }

    private static void removeDurableTask(String id) {
        try {
            Path path = getCronFilePath();
            if (!Files.exists(path)) return;

            // Read existing tasks
            String existing = Files.readString(path);
            List<CronTask> tasks = parseTasksJson(existing);

            // Remove task with matching ID
            tasks.removeIf(t -> t.id().equals(id));

            // Write back
            String json = tasksToJson(tasks);
            Files.writeString(path, json);
        } catch (Exception e) {
            // Ignore errors
        }
    }

    /**
     * Parse tasks JSON array.
     */
    private static List<CronTask> parseTasksJson(String json) {
        List<CronTask> tasks = new ArrayList<>();
        try {
            // Find array contents
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start < 0 || end < 0) return tasks;

            String content = json.substring(start + 1, end).trim();
            if (content.isEmpty()) return tasks;

            // Parse each object
            int i = 0;
            while (i < content.length()) {
                int objStart = content.indexOf('{', i);
                if (objStart < 0) break;

                int depth = 1;
                int objEnd = objStart + 1;
                while (objEnd < content.length() && depth > 0) {
                    char c = content.charAt(objEnd);
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                    objEnd++;
                }

                String obj = content.substring(objStart, objEnd);
                CronTask task = parseTaskObject(obj);
                if (task != null) tasks.add(task);

                i = objEnd;
            }
        } catch (Exception e) {
            // Ignore parse errors
        }
        return tasks;
    }

    /**
     * Parse single task object.
     */
    private static CronTask parseTaskObject(String json) {
        try {
            String id = extractJsonString(json, "id");
            String cron = extractJsonString(json, "cron");
            String prompt = extractJsonString(json, "prompt");
            boolean recurring = json.contains("\"recurring\":true");
            boolean durable = json.contains("\"durable\":true");
            long createdAt = extractJsonLong(json, "createdAt");

            if (id != null && cron != null) {
                return new CronTask(id, cron, prompt != null ? prompt : "", recurring, durable, createdAt);
            }
        } catch (Exception e) {
            // Ignore parse errors
        }
        return null;
    }

    /**
     * Convert tasks list to JSON.
     */
    private static String tasksToJson(List<CronTask> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < tasks.size(); i++) {
            if (i > 0) sb.append(",\n");
            CronTask t = tasks.get(i);
            sb.append("  {");
            sb.append("\"id\":\"").append(t.id()).append("\",");
            sb.append("\"cron\":\"").append(t.cron()).append("\",");
            sb.append("\"prompt\":\"").append(escapeJson(t.prompt())).append("\",");
            sb.append("\"recurring\":").append(t.recurring()).append(",");
            sb.append("\"durable\":").append(t.durable()).append(",");
            sb.append("\"createdAt\":").append(t.createdAt());
            sb.append("}");
        }
        sb.append("\n]");
        return sb.toString();
    }

    private static String extractJsonString(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int valStart = json.indexOf("\"", idx + key.length() + 2) + 1;
        if (valStart < 1) return null;
        int valEnd = json.indexOf("\"", valStart);
        return json.substring(valStart, valEnd);
    }

    private static long extractJsonLong(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return 0;
        int valStart = json.indexOf(":", idx + key.length() + 2) + 1;
        while (valStart < json.length() && Character.isWhitespace(json.charAt(valStart))) valStart++;
        int valEnd = valStart;
        while (valEnd < json.length() && Character.isDigit(json.charAt(valEnd))) valEnd++;
        try {
            return Long.parseLong(json.substring(valStart, valEnd));
        } catch (Exception e) {
            return 0;
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}