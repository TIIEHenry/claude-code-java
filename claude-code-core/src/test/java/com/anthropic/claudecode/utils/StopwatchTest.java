/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Stopwatch.
 */
class StopwatchTest {

    @Test
    @DisplayName("Stopwatch creates stopped")
    void createsStopped() {
        Stopwatch sw = new Stopwatch();

        assertFalse(sw.isRunning());
        assertTrue(sw.isReset());
        assertEquals(Duration.ZERO, sw.elapsed());
    }

    @Test
    @DisplayName("Stopwatch startNew creates and starts")
    void startNewWorks() {
        Stopwatch sw = Stopwatch.startNew();

        assertTrue(sw.isRunning());
        assertFalse(sw.isReset());
    }

    @Test
    @DisplayName("Stopwatch start starts timer")
    void startWorks() {
        Stopwatch sw = new Stopwatch();

        sw.start();

        assertTrue(sw.isRunning());
    }

    @Test
    @DisplayName("Stopwatch start fails when already running")
    void startFailsWhenRunning() {
        Stopwatch sw = Stopwatch.startNew();

        assertThrows(IllegalStateException.class, sw::start);
    }

    @Test
    @DisplayName("Stopwatch stop stops timer")
    void stopWorks() {
        Stopwatch sw = Stopwatch.startNew();

        sw.stop();

        assertFalse(sw.isRunning());
        assertTrue(sw.isStopped());
    }

    @Test
    @DisplayName("Stopwatch stop fails when not running")
    void stopFailsWhenNotRunning() {
        Stopwatch sw = new Stopwatch();

        assertThrows(IllegalStateException.class, sw::stop);
    }

    @Test
    @DisplayName("Stopwatch reset clears timer")
    void resetWorks() {
        Stopwatch sw = Stopwatch.startNew();
        sw.stop();

        sw.reset();

        assertTrue(sw.isReset());
        assertEquals(Duration.ZERO, sw.elapsed());
    }

    @Test
    @DisplayName("Stopwatch restart resets and starts")
    void restartWorks() {
        Stopwatch sw = Stopwatch.startNew();
        sw.stop();

        sw.restart();

        assertTrue(sw.isRunning());
    }

    @Test
    @DisplayName("Stopwatch elapsed returns duration")
    void elapsedWorks() {
        Stopwatch sw = Stopwatch.startNew();

        Duration elapsed = sw.elapsed();

        assertNotNull(elapsed);
        assertTrue(elapsed.compareTo(Duration.ZERO) >= 0);
    }

    @Test
    @DisplayName("Stopwatch elapsedMillis returns milliseconds")
    void elapsedMillis() {
        Stopwatch sw = Stopwatch.startNew();

        long millis = sw.elapsedMillis();

        assertTrue(millis >= 0);
    }

    @Test
    @DisplayName("Stopwatch elapsedNanos returns nanoseconds")
    void elapsedNanos() {
        Stopwatch sw = Stopwatch.startNew();

        long nanos = sw.elapsedNanos();

        assertTrue(nanos >= 0);
    }

    @Test
    @DisplayName("Stopwatch elapsedSeconds returns seconds")
    void elapsedSeconds() {
        Stopwatch sw = Stopwatch.startNew();

        double seconds = sw.elapsedSeconds();

        assertTrue(seconds >= 0);
    }

    @Test
    @DisplayName("Stopwatch lap records lap time")
    void lapWorks() {
        Stopwatch sw = Stopwatch.startNew();

        Duration lap = sw.lap();

        assertNotNull(lap);
        assertEquals(1, sw.getLapCount());
    }

    @Test
    @DisplayName("Stopwatch lap fails when not running")
    void lapFailsWhenNotRunning() {
        Stopwatch sw = new Stopwatch();

        assertThrows(IllegalStateException.class, sw::lap);
    }

    @Test
    @DisplayName("Stopwatch getLaps returns lap times")
    void getLaps() {
        Stopwatch sw = Stopwatch.startNew();
        sw.lap();
        sw.lap();

        List<Duration> laps = sw.getLaps();

        assertEquals(2, laps.size());
    }

    @Test
    @DisplayName("Stopwatch averageLap returns average")
    void averageLap() {
        Stopwatch sw = Stopwatch.startNew();
        sw.lap();
        sw.lap();

        Duration avg = sw.averageLap();

        assertNotNull(avg);
    }

    @Test
    @DisplayName("Stopwatch averageLap returns zero when no laps")
    void averageLapEmpty() {
        Stopwatch sw = new Stopwatch();

        assertEquals(Duration.ZERO, sw.averageLap());
    }

    @Test
    @DisplayName("Stopwatch minLap returns minimum")
    void minLap() {
        Stopwatch sw = Stopwatch.startNew();
        sw.lap();
        sw.lap();

        assertNotNull(sw.minLap());
    }

    @Test
    @DisplayName("Stopwatch maxLap returns maximum")
    void maxLap() {
        Stopwatch sw = Stopwatch.startNew();
        sw.lap();
        sw.lap();

        assertNotNull(sw.maxLap());
    }

    @Test
    @DisplayName("Stopwatch format returns formatted string")
    void formatWorks() {
        Stopwatch sw = Stopwatch.startNew();

        String formatted = sw.format();

        assertNotNull(formatted);
    }

    @Test
    @DisplayName("Stopwatch formatCompact returns compact string")
    void formatCompactWorks() {
        Stopwatch sw = Stopwatch.startNew();

        String formatted = sw.formatCompact();

        assertNotNull(formatted);
    }

    @Test
    @DisplayName("Stopwatch getStartTime returns start time")
    void getStartTime() {
        Stopwatch sw = Stopwatch.startNew();

        assertTrue(sw.getStartTime().isPresent());
    }

    @Test
    @DisplayName("Stopwatch getEndTime returns end time after stop")
    void getEndTime() {
        Stopwatch sw = Stopwatch.startNew();
        sw.stop();

        assertTrue(sw.getEndTime().isPresent());
    }

    @Test
    @DisplayName("Stopwatch measure measures runnable")
    void measureRunnable() {
        Duration duration = Stopwatch.measure(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // ignore
            }
        });

        assertTrue(duration.toMillis() >= 10);
    }

    @Test
    @DisplayName("Stopwatch measure measures supplier")
    void measureSupplier() {
        Stopwatch.MeasuredResult<String> result = Stopwatch.measure(() -> "test");

        assertEquals("test", result.value());
        assertNotNull(result.duration());
    }

    @Test
    @DisplayName("Stopwatch MeasuredResult methods work")
    void measuredResultMethods() {
        Stopwatch.MeasuredResult<String> result = new Stopwatch.MeasuredResult<>("value", Duration.ofMillis(100));

        assertEquals("value", result.value());
        assertEquals(100, result.millis());
        assertTrue(result.seconds() > 0);
        assertNotNull(result.format());
    }

    @Test
    @DisplayName("Stopwatch StopwatchUtils threadSafe creates thread-safe")
    void stopwatchUtilsThreadSafe() {
        Stopwatch.ThreadSafeStopwatch sw = Stopwatch.StopwatchUtils.threadSafe();

        assertNotNull(sw);
        sw.start();
        assertTrue(sw.isRunning());
    }

    @Test
    @DisplayName("Stopwatch StopwatchUtils withClock creates with clock")
    void stopwatchUtilsWithClock() {
        Clock fixedClock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        Stopwatch sw = Stopwatch.StopwatchUtils.withClock(fixedClock);

        assertNotNull(sw);
    }

    @Test
    @DisplayName("Stopwatch accumulates time across start/stop cycles")
    void accumulatesTime() {
        Stopwatch sw = new Stopwatch();
        sw.start();
        sw.stop();
        Duration first = sw.elapsed();

        sw.start();
        sw.stop();
        Duration second = sw.elapsed();

        assertTrue(second.compareTo(first) >= 0);
    }
}