/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.compact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CompactWarningState.
 */
class CompactWarningStateTest {

    private CompactWarningState state;

    @BeforeEach
    void setUp() {
        state = new CompactWarningState();
    }

    @Test
    @DisplayName("CompactWarningState initial state is not active")
    void initialStateNotActive() {
        assertFalse(state.isWarningActive());
        assertNull(state.getLastWarning());
        assertEquals(0, state.getWarningCount());
        assertNull(state.getLastWarningTime());
        assertFalse(state.isDismissed());
    }

    @Test
    @DisplayName("CompactWarningState recordWarning activates warning")
    void recordWarningActivates() {
        CompactWarningHook.CompactWarning warning = createTestWarning();

        state.recordWarning(warning);

        assertTrue(state.isWarningActive());
        assertEquals(warning, state.getLastWarning());
        assertEquals(1, state.getWarningCount());
        assertNotNull(state.getLastWarningTime());
    }

    @Test
    @DisplayName("CompactWarningState recordWarning increments count")
    void recordWarningIncrementsCount() {
        CompactWarningHook.CompactWarning warning = createTestWarning();

        state.recordWarning(warning);
        state.recordWarning(warning);
        state.recordWarning(warning);

        assertEquals(3, state.getWarningCount());
    }

    @Test
    @DisplayName("CompactWarningState dismiss deactivates warning")
    void dismissDeactivates() {
        state.recordWarning(createTestWarning());
        assertTrue(state.isWarningActive());

        state.dismiss();

        assertFalse(state.isWarningActive());
        assertTrue(state.isDismissed());
    }

    @Test
    @DisplayName("CompactWarningState clear resets warning")
    void clearResetsWarning() {
        state.recordWarning(createTestWarning());

        state.clear();

        assertFalse(state.isWarningActive());
        assertFalse(state.isDismissed());
        assertNull(state.getLastWarning());
    }

    @Test
    @DisplayName("CompactWarningState isWarningActive false after dismiss")
    void isWarningActiveAfterDismiss() {
        state.recordWarning(createTestWarning());
        state.dismiss();

        // Warning is recorded but dismissed
        assertEquals(1, state.getWarningCount());
        assertNotNull(state.getLastWarning());
        assertFalse(state.isWarningActive());
    }

    @Test
    @DisplayName("CompactWarningState getLastWarning returns most recent")
    void getLastWarningReturnsMostRecent() {
        CompactWarningHook.CompactWarning warning1 = CompactWarningHook.CompactWarning.approachingLimit(1000, 800, 2000);
        CompactWarningHook.CompactWarning warning2 = CompactWarningHook.CompactWarning.limitExceeded(2500, 2000);

        state.recordWarning(warning1);
        state.recordWarning(warning2);

        assertEquals(warning2, state.getLastWarning());
    }

    @Test
    @DisplayName("CompactWarningState getTimeSinceLastWarning")
    void getTimeSinceLastWarning() throws InterruptedException {
        state.recordWarning(createTestWarning());

        Thread.sleep(50);

        Duration duration = state.getTimeSinceLastWarning();
        assertTrue(duration.toMillis() >= 50);
    }

    @Test
    @DisplayName("CompactWarningState getTimeSinceLastWarning zero when no warning")
    void getTimeSinceLastWarningNoWarning() {
        Duration duration = state.getTimeSinceLastWarning();
        assertEquals(Duration.ZERO, duration);
    }

    @Test
    @DisplayName("CompactWarningState getSnapshot")
    void getSnapshot() {
        state.recordWarning(createTestWarning());

        CompactWarningState.StateSnapshot snapshot = state.getSnapshot();

        assertTrue(snapshot.warningActive());
        assertNotNull(snapshot.lastWarning());
        assertEquals(1, snapshot.warningCount());
        assertNotNull(snapshot.lastWarningTime());
        assertFalse(snapshot.dismissed());
    }

    @Test
    @DisplayName("CompactWarningState StateSnapshot format active warning")
    void stateSnapshotFormatActive() {
        CompactWarningHook.CompactWarning warning = createTestWarning();
        state.recordWarning(warning);

        CompactWarningState.StateSnapshot snapshot = state.getSnapshot();
        String formatted = snapshot.format();

        assertTrue(formatted.contains("Warning"));
        assertTrue(formatted.contains("1 warnings"));
    }

    @Test
    @DisplayName("CompactWarningState StateSnapshot format no warning")
    void stateSnapshotFormatNoWarning() {
        CompactWarningState.StateSnapshot snapshot = state.getSnapshot();
        String formatted = snapshot.format();

        assertEquals("No active warnings", formatted);
    }

    @Test
    @DisplayName("CompactWarningState reset clears all state")
    void resetClearsAll() {
        state.recordWarning(createTestWarning());
        state.dismiss();

        state.reset();

        assertFalse(state.isWarningActive());
        assertNull(state.getLastWarning());
        assertEquals(0, state.getWarningCount());
        assertNull(state.getLastWarningTime());
        assertFalse(state.isDismissed());
    }

    private CompactWarningHook.CompactWarning createTestWarning() {
        return CompactWarningHook.CompactWarning.approachingLimit(1000, 800, 2000);
    }
}