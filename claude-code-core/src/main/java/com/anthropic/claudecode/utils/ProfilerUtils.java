/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code profiler utilities
 */
package com.anthropic.claudecode.utils;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Performance profiler utilities.
 */
public final class ProfilerUtils {
    private ProfilerUtils() {}

    /**
     * Simple profiler for measuring execution time.
     */
    public static final class Profiler {
        private final Map<String, List<Duration>> measurements = new ConcurrentHashMap<>();
        private final Map<String, Integer> callCounts = new ConcurrentHashMap<>();

        /**
         * Profile a runnable.
         */
        public Duration profile(String name, Runnable runnable) {
            Stopwatch sw = Stopwatch.startNew();
            try {
                runnable.run();
            } finally {
                sw.stop();
                record(name, sw.elapsed());
            }
            return sw.elapsed();
        }

        /**
         * Profile a supplier.
         */
        public <T> ProfiledResult<T> profile(String name, Supplier<T> supplier) {
            Stopwatch sw = Stopwatch.startNew();
            try {
                T result = supplier.get();
                sw.stop();
                record(name, sw.elapsed());
                return new ProfiledResult<>(result, sw.elapsed(), name);
            } catch (Exception e) {
                sw.stop();
                record(name, sw.elapsed());
                throw e;
            }
        }

        /**
         * Profile with custom stopwatch.
         */
        public void record(String name, Duration duration) {
            measurements.computeIfAbsent(name, k -> new CopyOnWriteArrayList<>()).add(duration);
            callCounts.merge(name, 1, Integer::sum);
        }

        /**
         * Get statistics for a name.
         */
        public ProfileStats getStats(String name) {
            List<Duration> durations = measurements.getOrDefault(name, List.of());
            if (durations.isEmpty()) {
                return new ProfileStats(name, 0, Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO);
            }

            long totalNanos = durations.stream().mapToLong(Duration::toNanos).sum();
            long avgNanos = totalNanos / durations.size();
            long minNanos = durations.stream().mapToLong(Duration::toNanos).min().orElse(0);
            long maxNanos = durations.stream().mapToLong(Duration::toNanos).max().orElse(0);

            // Calculate standard deviation
            double variance = durations.stream()
                .mapToLong(Duration::toNanos)
                .map(n -> (n - avgNanos) * (n - avgNanos))
                .average()
                .orElse(0);
            long stdDevNanos = (long) Math.sqrt(variance);

            return new ProfileStats(
                name,
                durations.size(),
                Duration.ofNanos(totalNanos),
                Duration.ofNanos(avgNanos),
                Duration.ofNanos(minNanos),
                Duration.ofNanos(maxNanos),
                Duration.ofNanos(stdDevNanos)
            );
        }

        /**
         * Get all statistics.
         */
        public Map<String, ProfileStats> getAllStats() {
            return measurements.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), this::getStats));
        }

        /**
         * Reset profiler.
         */
        public void reset() {
            measurements.clear();
            callCounts.clear();
        }

        /**
         * Reset specific name.
         */
        public void reset(String name) {
            measurements.remove(name);
            callCounts.remove(name);
        }

        /**
         * Get call count for name.
         */
        public int getCallCount(String name) {
            return callCounts.getOrDefault(name, 0);
        }

        /**
         * Get all call counts.
         */
        public Map<String, Integer> getAllCallCounts() {
            return new HashMap<>(callCounts);
        }

        /**
         * Summary report.
         */
        public String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Profiler Summary:\n");
            sb.append("-".repeat(80)).append("\n");

            getAllStats().values().stream()
                .sorted(Comparator.comparing(ProfileStats::totalTime).reversed())
                .forEach(stats -> {
                    sb.append(String.format("%-30s calls=%-5d total=%-12s avg=%-12s min=%-12s max=%-12s\n",
                        stats.name(),
                        stats.callCount(),
                        DurationUtils.format(stats.totalTime()),
                        DurationUtils.format(stats.averageTime()),
                        DurationUtils.format(stats.minTime()),
                        DurationUtils.format(stats.maxTime())
                    ));
                });

            return sb.toString();
        }

        /**
         * Detailed report.
         */
        public String detailed(String name) {
            ProfileStats stats = getStats(name);
            List<Duration> durations = measurements.getOrDefault(name, List.of());

            StringBuilder sb = new StringBuilder();
            sb.append("Detailed Profile for: ").append(name).append("\n");
            sb.append("-".repeat(40)).append("\n");
            sb.append("Calls: ").append(stats.callCount()).append("\n");
            sb.append("Total: ").append(DurationUtils.format(stats.totalTime())).append("\n");
            sb.append("Average: ").append(DurationUtils.format(stats.averageTime())).append("\n");
            sb.append("Min: ").append(DurationUtils.format(stats.minTime())).append("\n");
            sb.append("Max: ").append(DurationUtils.format(stats.maxTime())).append("\n");
            sb.append("Std Dev: ").append(DurationUtils.format(stats.stdDev())).append("\n");
            sb.append("\nIndividual measurements:\n");

            for (int i = 0; i < durations.size(); i++) {
                sb.append(String.format("  #%d: %s\n", i + 1, DurationUtils.format(durations.get(i))));
            }

            return sb.toString();
        }
    }

    /**
     * Profiled result.
     */
    public record ProfiledResult<T>(T value, Duration duration, String name) {
        public long millis() {
            return duration.toMillis();
        }

        public double seconds() {
            return duration.toNanos() / 1_000_000_000.0;
        }

        public String format() {
            return DurationUtils.format(duration);
        }
    }

    /**
     * Profile statistics.
     */
    public record ProfileStats(
        String name,
        int callCount,
        Duration totalTime,
        Duration averageTime,
        Duration minTime,
        Duration maxTime,
        Duration stdDev
    ) {}

    /**
     * Memory profiler.
     */
    public static final class MemoryProfiler {
        private final List<MemorySnapshot> snapshots = new ArrayList<>();

        /**
         * Take memory snapshot.
         */
        public MemorySnapshot snapshot() {
            Runtime runtime = Runtime.getRuntime();
            long total = runtime.totalMemory();
            long free = runtime.freeMemory();
            long used = total - free;
            long max = runtime.maxMemory();
            return new MemorySnapshot(
                Instant.now(),
                total,
                free,
                used,
                max
            );
        }

        /**
         * Take and record snapshot.
         */
        public MemorySnapshot recordSnapshot() {
            MemorySnapshot snap = snapshot();
            snapshots.add(snap);
            return snap;
        }

        /**
         * Get all snapshots.
         */
        public List<MemorySnapshot> getSnapshots() {
            return new ArrayList<>(snapshots);
        }

        /**
         * Get memory stats.
         */
        public MemoryStats getStats() {
            if (snapshots.isEmpty()) {
                return new MemoryStats(0, 0, 0, 0, 0, 0);
            }

            long avgUsed = (long) snapshots.stream().mapToLong(MemorySnapshot::used).average().orElse(0);
            long maxUsed = snapshots.stream().mapToLong(MemorySnapshot::used).max().orElse(0);
            long minUsed = snapshots.stream().mapToLong(MemorySnapshot::used).min().orElse(0);
            long avgFree = (long) snapshots.stream().mapToLong(MemorySnapshot::free).average().orElse(0);

            return new MemoryStats(
                snapshots.size(),
                avgUsed,
                maxUsed,
                minUsed,
                avgFree,
                snapshots.get(snapshots.size() - 1).maxAvailable()
            );
        }

        /**
         * Clear snapshots.
         */
        public void clear() {
            snapshots.clear();
        }

        /**
         * Trigger garbage collection (for testing).
         */
        public void gc() {
            System.gc();
        }

        /**
         * Compare before and after operation.
         */
        public MemoryDiff measureOperation(Runnable operation) {
            MemorySnapshot before = recordSnapshot();
            operation.run();
            MemorySnapshot after = recordSnapshot();
            return new MemoryDiff(before, after);
        }
    }

    /**
     * Memory snapshot.
     */
    public record MemorySnapshot(
        Instant timestamp,
        long total,
        long free,
        long used,
        long maxAvailable
    ) {
        public double usedPercent() {
            return maxAvailable > 0 ? (used * 100.0 / maxAvailable) : 0;
        }

        public double freePercent() {
            return maxAvailable > 0 ? (free * 100.0 / maxAvailable) : 0;
        }

        public String format() {
            return String.format("Used: %d MB (%.1f%%), Free: %d MB, Max: %d MB",
                used / (1024 * 1024),
                usedPercent(),
                free / (1024 * 1024),
                maxAvailable / (1024 * 1024)
            );
        }
    }

    /**
     * Memory statistics.
     */
    public record MemoryStats(
        int snapshotCount,
        long averageUsed,
        long maxUsed,
        long minUsed,
        long averageFree,
        long maxAvailable
    ) {
        public String format() {
            return String.format("Snapshots: %d, Avg Used: %d MB, Max Used: %d MB, Min Used: %d MB",
                snapshotCount,
                averageUsed / (1024 * 1024),
                maxUsed / (1024 * 1024),
                minUsed / (1024 * 1024)
            );
        }
    }

    /**
     * Memory difference.
     */
    public record MemoryDiff(MemorySnapshot before, MemorySnapshot after) {
        public long deltaUsed() {
            return after.used() - before.used();
        }

        public long deltaFree() {
            return after.free() - before.free();
        }

        public Duration timeElapsed() {
            return Duration.between(before.timestamp(), after.timestamp());
        }

        public boolean increased() {
            return deltaUsed() > 0;
        }

        public boolean decreased() {
            return deltaUsed() < 0;
        }
    }

    /**
     * Create new profiler.
     */
    public static Profiler create() {
        return new Profiler();
    }

    /**
     * Create memory profiler.
     */
    public static MemoryProfiler memoryProfiler() {
        return new MemoryProfiler();
    }

    /**
     * Quick profile.
     */
    public static Duration quickProfile(Runnable runnable) {
        return Stopwatch.measure(runnable);
    }

    /**
     * Quick profile with result.
     */
    public static <T> Stopwatch.MeasuredResult<T> quickProfile(Supplier<T> supplier) {
        return Stopwatch.measure(supplier);
    }

    /**
     * Benchmark - run multiple iterations.
     */
    public static BenchmarkResult benchmark(String name, Supplier<?> operation, int iterations) {
        Profiler profiler = create();
        for (int i = 0; i < iterations; i++) {
            profiler.profile(name, () -> operation.get());
        }
        return new BenchmarkResult(profiler.getStats(name), iterations);
    }

    /**
     * Benchmark with warmup.
     */
    public static BenchmarkResult benchmarkWithWarmup(String name, Supplier<?> operation,
            int warmupIterations, int iterations) {
        // Warmup
        for (int i = 0; i < warmupIterations; i++) {
            operation.get();
        }

        // Actual benchmark
        return benchmark(name, operation, iterations);
    }

    /**
     * Benchmark result.
     */
    public record BenchmarkResult(ProfileStats stats, int iterations) {
        public double opsPerSecond() {
            if (stats.totalTime().isZero()) return Double.MAX_VALUE;
            return iterations / (stats.totalTime().toNanos() / 1_000_000_000.0);
        }

        public String format() {
            return String.format("%s: %d iterations, %.2f ops/sec, avg: %s",
                stats.name(),
                iterations,
                opsPerSecond(),
                DurationUtils.format(stats.averageTime())
            );
        }
    }
}