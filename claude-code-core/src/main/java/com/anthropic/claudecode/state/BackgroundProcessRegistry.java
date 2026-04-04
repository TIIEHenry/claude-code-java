/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code background process registry
 */
package com.anthropic.claudecode.state;

import java.util.*;
import java.util.concurrent.*;

/**
 * Registry for tracking background processes.
 */
public final class BackgroundProcessRegistry {
    private BackgroundProcessRegistry() {}

    private static final ConcurrentHashMap<String, ProcessInfo> processes = new ConcurrentHashMap<>();

    /**
     * Process info record.
     */
    public record ProcessInfo(
        Process process,
        String command,
        long startTime,
        String storedOutput,
        String workingDirectory
    ) {
        public ProcessInfo(Process process, String command, String workingDirectory) {
            this(process, command, System.currentTimeMillis(), null, workingDirectory);
        }
    }

    /**
     * Register a background process.
     */
    public static String register(Process process, String command, String workingDirectory) {
        String id = generateId();
        processes.put(id, new ProcessInfo(process, command, workingDirectory));
        return id;
    }

    /**
     * Register a process with a specific ID.
     */
    public static void register(String id, Process process, String command, String workingDirectory) {
        processes.put(id, new ProcessInfo(process, command, workingDirectory));
    }

    /**
     * Get a process by ID.
     */
    public static ProcessInfo get(String id) {
        return processes.get(id);
    }

    /**
     * Remove a process by ID.
     */
    public static ProcessInfo remove(String id) {
        return processes.remove(id);
    }

    /**
     * Check if a process exists.
     */
    public static boolean contains(String id) {
        return processes.containsKey(id);
    }

    /**
     * Get all process IDs.
     */
    public static Set<String> getAllIds() {
        return new HashSet<>(processes.keySet());
    }

    /**
     * Get all running processes.
     */
    public static Map<String, ProcessInfo> getAllRunning() {
        Map<String, ProcessInfo> running = new LinkedHashMap<>();
        for (Map.Entry<String, ProcessInfo> entry : processes.entrySet()) {
            Process process = entry.getValue().process();
            if (process != null && process.isAlive()) {
                running.put(entry.getKey(), entry.getValue());
            }
        }
        return running;
    }

    /**
     * Kill a process by ID.
     */
    public static boolean kill(String id) {
        ProcessInfo info = processes.get(id);
        if (info != null && info.process() != null && info.process().isAlive()) {
            info.process().destroyForcibly();
            return true;
        }
        return false;
    }

    /**
     * Kill all processes.
     */
    public static void killAll() {
        for (ProcessInfo info : processes.values()) {
            if (info.process() != null && info.process().isAlive()) {
                info.process().destroyForcibly();
            }
        }
        processes.clear();
    }

    /**
     * Get count of registered processes.
     */
    public static int size() {
        return processes.size();
    }

    /**
     * Clear all completed processes.
     */
    public static void clearCompleted() {
        processes.entrySet().removeIf(entry -> {
            Process process = entry.getValue().process();
            return process == null || !process.isAlive();
        });
    }

    private static String generateId() {
        return "bash_" + UUID.randomUUID().toString().substring(0, 8);
    }
}