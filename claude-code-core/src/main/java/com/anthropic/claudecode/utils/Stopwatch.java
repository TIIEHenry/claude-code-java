/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code stopwatch utility
 */
package com.anthropic.claudecode.utils;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Stopwatch for precise timing measurements.
 */
public final class Stopwatch {
    private final Clock clock;
    private Instant startTime;
    private Instant endTime;
    private Duration totalElapsed;
    private boolean running;
    private final List<Duration> laps;

    public Stopwatch() {
        this(Clock.systemUTC());
    }

    public Stopwatch(Clock clock) {
        this.clock = clock;
        this.totalElapsed = Duration.ZERO;
        this.laps = new ArrayList<>();
        this.running = false;
    }

    /**
     * Create and start a new stopwatch.
     */
    public static Stopwatch startNew() {
        Stopwatch sw = new Stopwatch();
        sw.start();
        return sw;
    }

    /**
     * Start the stopwatch.
     */
    public Stopwatch start() {
        if (running) {
            throw new IllegalStateException("Stopwatch is already running");
        }
        startTime = clock.instant();
        running = true;
        return this;
    }

    /**
     * Stop the stopwatch.
     */
    public Stopwatch stop() {
        if (!running) {
            throw new IllegalStateException("Stopwatch is not running");
        }
        endTime = clock.instant();
        Duration elapsed = Duration.between(startTime, endTime);
        totalElapsed = totalElapsed.plus(elapsed);
        running = false;
        return this;
    }

    /**
     * Reset the stopwatch.
     */
    public Stopwatch reset() {
        startTime = null;
        endTime = null;
        totalElapsed = Duration.ZERO;
        running = false;
        laps.clear();
        return this;
    }

    /**
     * Restart (reset and start).
     */
    public Stopwatch restart() {
        reset();
        start();
        return this;
    }

    /**
     * Record a lap.
     */
    public Duration lap() {
        if (!running) {
            throw new IllegalStateException("Stopwatch is not running");
        }
        Instant now = clock.instant();
        Duration lapTime = Duration.between(startTime, now);
        laps.add(lapTime);
        startTime = now; // Reset start for next lap
        return lapTime;
    }

    /**
     * Get elapsed time (current if running, final if stopped).
     */
    public Duration elapsed() {
        if (running) {
            return totalElapsed.plus(Duration.between(startTime, clock.instant()));
        }
        return totalElapsed;
    }

    /**
     * Get elapsed milliseconds.
     */
    public long elapsedMillis() {
        return elapsed().toMillis();
    }

    /**
     * Get elapsed nanoseconds.
     */
    public long elapsedNanos() {
        return elapsed().toNanos();
    }

    /**
     * Get elapsed seconds.
     */
    public double elapsedSeconds() {
        return elapsed().toNanos() / 1_000_000_000.0;
    }

    /**
     * Check if running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Check if stopped.
     */
    public boolean isStopped() {
        return !running && startTime != null;
    }

    /**
     * Check if reset (never started or reset).
     */
    public boolean isReset() {
        return !running && startTime == null;
    }

    /**
     * Get all lap times.
     */
    public List<Duration> getLaps() {
        return new ArrayList<>(laps);
    }

    /**
     * Get lap count.
     */
    public int getLapCount() {
        return laps.size();
    }

    /**
     * Get average lap time.
     */
    public Duration averageLap() {
        if (laps.isEmpty()) return Duration.ZERO;
        long totalNanos = laps.stream().mapToLong(Duration::toNanos).sum();
        return Duration.ofNanos(totalNanos / laps.size());
    }

    /**
     * Get minimum lap time.
     */
    public Duration minLap() {
        return laps.stream().min(Duration::compareTo).orElse(Duration.ZERO);
    }

    /**
     * Get maximum lap time.
     */
    public Duration maxLap() {
        return laps.stream().max(Duration::compareTo).orElse(Duration.ZERO);
    }

    /**
     * Format elapsed time.
     */
    public String format() {
        return DurationUtils.format(elapsed());
    }

    /**
     * Format elapsed time compact.
     */
    public String formatCompact() {
        return DurationUtils.formatCompact(elapsed());
    }

    /**
     * Get start time.
     */
    public Optional<Instant> getStartTime() {
        return Optional.ofNullable(startTime);
    }

    /**
     * Get end time.
     */
    public Optional<Instant> getEndTime() {
        return Optional.ofNullable(endTime);
    }

    /**
     * Execute and measure.
     */
    public static Duration measure(Runnable runnable) {
        Stopwatch sw = startNew();
        runnable.run();
        sw.stop();
        return sw.elapsed();
    }

    /**
     * Execute and measure with result.
     */
    public static <T> MeasuredResult<T> measure(Supplier<T> supplier) {
        Stopwatch sw = startNew();
        T result = supplier.get();
        sw.stop();
        return new MeasuredResult<>(result, sw.elapsed());
    }

    /**
     * Result with timing.
     */
    public record MeasuredResult<T>(T value, Duration duration) {
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
     * Stopwatch utilities.
     */
    public static final class StopwatchUtils {
        private StopwatchUtils() {}

        /**
         * Create a thread-safe stopwatch.
         */
        public static ThreadSafeStopwatch threadSafe() {
            return new ThreadSafeStopwatch();
        }

        /**
         * Create a stopwatch with custom clock.
         */
        public static Stopwatch withClock(Clock clock) {
            return new Stopwatch(clock);
        }

        /**
         * Create a stopwatch with nanosecond precision.
         */
        public static Stopwatch nanosecondPrecision() {
            return new Stopwatch(NanoClock.INSTANCE);
        }

        /**
         * Compare stopwatches by elapsed time.
         */
        public static Comparator<Stopwatch> byElapsed() {
            return Comparator.comparing(sw -> sw.elapsed().toNanos());
        }
    }

    /**
     * Thread-safe stopwatch.
     */
    public static final class ThreadSafeStopwatch {
        private final Stopwatch stopwatch = new Stopwatch();

        public synchronized ThreadSafeStopwatch start() {
            stopwatch.start();
            return this;
        }

        public synchronized ThreadSafeStopwatch stop() {
            stopwatch.stop();
            return this;
        }

        public synchronized Duration elapsed() {
            return stopwatch.elapsed();
        }

        public synchronized ThreadSafeStopwatch reset() {
            stopwatch.reset();
            return this;
        }

        public synchronized boolean isRunning() {
            return stopwatch.isRunning();
        }
    }

    /**
     * Nano clock for high precision.
     */
    private static final class NanoClock extends Clock {
        static final NanoClock INSTANCE = new NanoClock();

        @Override
        public Instant instant() {
            return Instant.EPOCH.plusNanos(System.nanoTime());
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}