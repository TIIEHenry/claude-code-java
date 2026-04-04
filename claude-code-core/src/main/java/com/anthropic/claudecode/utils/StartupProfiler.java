/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/startupProfiler
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Startup profiler - Measure and report time spent in initialization phases.
 *
 * Two modes:
 * 1. Sampled logging: 100% of ant users, 0.5% of external users - logs phases
 * 2. Detailed profiling: CLAUDE_CODE_PROFILE_STARTUP=1 - full report with memory snapshots
 */
public final class StartupProfiler {
    private static final boolean DETAILED_PROFILING = isDetailedProfilingEnabled();
    private static final double STATSIG_SAMPLE_RATE = 0.005;
    private static final boolean STATSIG_LOGGING_SAMPLED;

    static {
        String userType = System.getenv("USER_TYPE");
        STATSIG_LOGGING_SAMPLED = "ant".equals(userType) || Math.random() < STATSIG_SAMPLE_RATE;
    }

    private static final boolean SHOULD_PROFILE = DETAILED_PROFILING || STATSIG_LOGGING_SAMPLED;

    private static final List<Checkpoint> checkpoints = new CopyOnWriteArrayList<>();
    private static final List<MemorySnapshot> memorySnapshots = new CopyOnWriteArrayList<>();

    /**
     * Checkpoint record.
     */
    public record Checkpoint(
        String name,
        long timestampMs,
        long relativeMs
    ) {}

    /**
     * Memory snapshot record.
     */
    public record MemorySnapshot(
        long heapUsed,
        long heapCommitted,
        long heapMax,
        long nonHeapUsed
    ) {}

    // Phase definitions for logging
    private static final Map<String, String[]> PHASE_DEFINITIONS = Map.of(
        "import_time", new String[]{"cli_entry", "main_tsx_imports_loaded"},
        "init_time", new String[]{"init_function_start", "init_function_end"},
        "settings_time", new String[]{"eagerLoadSettings_start", "eagerLoadSettings_end"},
        "total_time", new String[]{"cli_entry", "main_after_run"}
    );

    // Record initial checkpoint if profiling is enabled
    static {
        if (SHOULD_PROFILE) {
            profileCheckpoint("profiler_initialized");
        }
    }

    /**
     * Check if detailed profiling is enabled.
     */
    public static boolean isDetailedProfilingEnabled() {
        return "true".equals(System.getenv("CLAUDE_CODE_PROFILE_STARTUP")) ||
               "1".equals(System.getenv("CLAUDE_CODE_PROFILE_STARTUP"));
    }

    /**
     * Record a checkpoint with the given name.
     */
    public static void profileCheckpoint(String name) {
        if (!SHOULD_PROFILE) return;

        long timestamp = System.currentTimeMillis();
        long relative = checkpoints.isEmpty() ? 0 : timestamp - checkpoints.get(0).timestampMs();

        checkpoints.add(new Checkpoint(name, timestamp, relative));

        if (DETAILED_PROFILING) {
            memorySnapshots.add(captureMemorySnapshot());
        }
    }

    /**
     * Get startup performance log path.
     */
    public static String getStartupPerfLogPath() {
        String configHome = System.getProperty("user.home") + "/.claude";
        String sessionId = System.getProperty("claude.code.session.id", "default");
        return configHome + "/startup-perf/" + sessionId + ".txt";
    }

    /**
     * Generate and output profiling report.
     */
    public static void profileReport() {
        // Log to Statsig
        logStartupPerf();

        // Output detailed report if enabled
        if (DETAILED_PROFILING) {
            String report = getReport();
            System.err.println(report);
        }
    }

    /**
     * Get formatted report of all checkpoints.
     */
    private static String getReport() {
        if (!DETAILED_PROFILING) {
            return "Startup profiling not enabled";
        }

        if (checkpoints.isEmpty()) {
            return "No profiling checkpoints recorded";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(80)).append("\n");
        sb.append("STARTUP PROFILING REPORT\n");
        sb.append("=".repeat(80)).append("\n\n");

        long prevTime = 0;
        for (int i = 0; i < checkpoints.size(); i++) {
            Checkpoint cp = checkpoints.get(i);
            MemorySnapshot mem = i < memorySnapshots.size() ? memorySnapshots.get(i) : null;

            long delta = cp.timestampMs() - (i > 0 ? checkpoints.get(i - 1).timestampMs() : 0);
            sb.append(formatTimelineLine(cp.timestampMs, delta, cp.name, mem));
            sb.append("\n");
        }

        Checkpoint last = checkpoints.get(checkpoints.size() - 1);
        sb.append("\nTotal startup time: ").append(last.relativeMs()).append("ms\n");
        sb.append("=".repeat(80)).append("\n");

        return sb.toString();
    }

    /**
     * Log startup performance to analytics.
     */
    private static void logStartupPerf() {
        if (!STATSIG_LOGGING_SAMPLED || checkpoints.isEmpty()) return;

        // Build checkpoint lookup
        Map<String, Long> checkpointTimes = new HashMap<>();
        for (Checkpoint cp : checkpoints) {
            checkpointTimes.put(cp.name(), cp.relativeMs());
        }

        // Compute phase durations
        Map<String, Object> metadata = new HashMap<>();
        for (Map.Entry<String, String[]> entry : PHASE_DEFINITIONS.entrySet()) {
            String phaseName = entry.getKey();
            String[] checkpoints = entry.getValue();
            String startCheckpoint = checkpoints[0];
            String endCheckpoint = checkpoints[1];

            Long startTime = checkpointTimes.get(startCheckpoint);
            Long endTime = checkpointTimes.get(endCheckpoint);

            if (startTime != null && endTime != null) {
                metadata.put(phaseName + "_ms", endTime - startTime);
            }
        }

        metadata.put("checkpoint_count", checkpoints.size());

        // Would log to analytics
        // Analytics.logEvent("tengu_startup_perf", metadata);
    }

    /**
     * Capture memory snapshot.
     */
    private static MemorySnapshot captureMemorySnapshot() {
        Runtime runtime = Runtime.getRuntime();
        return new MemorySnapshot(
            runtime.totalMemory() - runtime.freeMemory(),
            runtime.totalMemory(),
            runtime.maxMemory(),
            0 // Non-heap not easily available
        );
    }

    /**
     * Format timeline line.
     */
    private static String formatTimelineLine(
        long timestamp,
        long delta,
        String name,
        MemorySnapshot mem
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%8d ms", timestamp));
        sb.append(String.format(" (+%5d ms)", delta));
        sb.append("  ").append(name);

        if (mem != null) {
            sb.append(String.format("  [heap: %d MB]", mem.heapUsed() / 1024 / 1024));
        }

        return sb.toString();
    }
}