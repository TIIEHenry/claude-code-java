/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/timer
 */
package com.anthropic.claudecode.utils.timer;

import java.util.*;
import java.time.*;

/**
 * Timer utils - Timing and benchmarking utilities.
 */
public final class TimerUtils {

    /**
     * Timer record.
     */
    public record Timer(
        String name,
        Instant startTime,
        Instant endTime,
        Duration duration,
        TimerStatus status
    ) {
        public static Timer start(String name) {
            return new Timer(name, Instant.now(), null, Duration.ZERO, TimerStatus.RUNNING);
        }

        public Timer stop() {
            Instant end = Instant.now();
            return new Timer(name, startTime, end, Duration.between(startTime, end), TimerStatus.STOPPED);
        }

        public long getElapsedMs() {
            if (status == TimerStatus.RUNNING) {
                return Duration.between(startTime, Instant.now()).toMillis();
            }
            return duration.toMillis();
        }

        public long getElapsedNanos() {
            if (status == TimerStatus.RUNNING) {
                return Duration.between(startTime, Instant.now()).toNanos();
            }
            return duration.toNanos();
        }

        public String format() {
            long ms = getElapsedMs();
            if (ms < 1000) {
                return ms + "ms";
            }
            long seconds = ms / 1000;
            if (seconds < 60) {
                return seconds + "." + (ms % 1000) + "s";
            }
            long minutes = seconds / 60;
            return minutes + "m " + (seconds % 60) + "s";
        }
    }

    /**
     * Timer status enum.
     */
    public enum TimerStatus {
        RUNNING,
        STOPPED,
        PAUSED
    }

    /**
     * Stopwatch implementation.
     */
    public static final class Stopwatch {
        private Instant startTime;
        private Duration accumulated = Duration.ZERO;
        private boolean running = false;

        public static Stopwatch createStarted() {
            Stopwatch sw = new Stopwatch();
            sw.start();
            return sw;
        }

        public void start() {
            if (!running) {
                startTime = Instant.now();
                running = true;
            }
        }

        public void stop() {
            if (running) {
                accumulated = accumulated.plus(Duration.between(startTime, Instant.now()));
                running = false;
            }
        }

        public void reset() {
            accumulated = Duration.ZERO;
            running = false;
        }

        public void restart() {
            reset();
            start();
        }

        public Duration getElapsed() {
            if (running) {
                return accumulated.plus(Duration.between(startTime, Instant.now()));
            }
            return accumulated;
        }

        public long getElapsedMs() {
            return getElapsed().toMillis();
        }

        public boolean isRunning() {
            return running;
        }

        public String format() {
            long ms = getElapsedMs();
            if (ms < 1000) return ms + "ms";
            if (ms < 60000) return String.format("%.2fs", ms / 1000.0);
            return String.format("%dm %ds", ms / 60000, (ms / 1000) % 60);
        }
    }

    /**
     * Benchmark result record.
     */
    public record BenchmarkResult(
        String name,
        int iterations,
        long totalTimeMs,
        double avgTimeMs,
        long minTimeMs,
        long maxTimeMs,
        double opsPerSecond
    ) {
        public String format() {
            return String.format(
                "%s: %d iterations, %.2fms avg, %dms min, %dms max, %.0f ops/s",
                name, iterations, avgTimeMs, minTimeMs, maxTimeMs, opsPerSecond
            );
        }
    }

    /**
     * Run benchmark.
     */
    public static BenchmarkResult benchmark(String name, Runnable action, int iterations, int warmupIterations) {
        // Warmup
        for (int i = 0; i < warmupIterations; i++) {
            action.run();
        }

        // Actual benchmark
        long[] times = new long[iterations];
        long total = 0;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            action.run();
            long end = System.nanoTime();
            times[i] = (end - start) / 1_000_000;
            total += times[i];
        }

        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        for (long time : times) {
            min = Math.min(min, time);
            max = Math.max(max, time);
        }

        double avg = (double) total / iterations;
        double opsPerSec = iterations > 0 && total > 0 ? 1000.0 / avg : 0;

        return new BenchmarkResult(name, iterations, total, avg, min, max, opsPerSec);
    }

    /**
     * Timer group for multiple timers.
     */
    public static final class TimerGroup {
        private final Map<String, Timer> timers = new LinkedHashMap<>();

        public Timer start(String name) {
            Timer timer = Timer.start(name);
            timers.put(name, timer);
            return timer;
        }

        public Timer stop(String name) {
            Timer timer = timers.get(name);
            if (timer != null && timer.status() == TimerStatus.RUNNING) {
                Timer stopped = timer.stop();
                timers.put(name, stopped);
                return stopped;
            }
            return timer;
        }

        public Timer get(String name) {
            return timers.get(name);
        }

        public Map<String, Timer> getAll() {
            return Collections.unmodifiableMap(timers);
        }

        public String format() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Timer> entry : timers.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue().format()).append("\n");
            }
            return sb.toString();
        }

        public void clear() {
            timers.clear();
        }
    }

    /**
     * Create timer group.
     */
    public static TimerGroup createGroup() {
        return new TimerGroup();
    }
}