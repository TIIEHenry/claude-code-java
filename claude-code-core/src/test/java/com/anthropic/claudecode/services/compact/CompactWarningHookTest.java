/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.compact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CompactWarningHook.
 */
class CompactWarningHookTest {

    @Test
    @DisplayName("CompactWarningHook WarningType enum values")
    void warningTypeEnum() {
        CompactWarningHook.WarningType[] types = CompactWarningHook.WarningType.values();
        assertEquals(5, types.length);
        assertEquals(CompactWarningHook.WarningType.APPROACHING_LIMIT, CompactWarningHook.WarningType.valueOf("APPROACHING_LIMIT"));
        assertEquals(CompactWarningHook.WarningType.LIMIT_EXCEEDED, CompactWarningHook.WarningType.valueOf("LIMIT_EXCEEDED"));
        assertEquals(CompactWarningHook.WarningType.COMPACTION_RECOMMENDED, CompactWarningHook.WarningType.valueOf("COMPACTION_RECOMMENDED"));
        assertEquals(CompactWarningHook.WarningType.COMPACTION_REQUIRED, CompactWarningHook.WarningType.valueOf("COMPACTION_REQUIRED"));
        assertEquals(CompactWarningHook.WarningType.TOKEN_BUDGET_LOW, CompactWarningHook.WarningType.valueOf("TOKEN_BUDGET_LOW"));
    }

    @Test
    @DisplayName("CompactWarningHook CompactWarning approachingLimit factory")
    void compactWarningApproachingLimit() {
        CompactWarningHook.CompactWarning warning = CompactWarningHook.CompactWarning.approachingLimit(1000, 800, 2000);

        assertEquals(CompactWarningHook.WarningType.APPROACHING_LIMIT, warning.type());
        assertTrue(warning.message().contains("50%"));
        assertTrue(warning.message().contains("1000"));
        assertEquals(1000, warning.currentTokens());
        assertEquals(800, warning.threshold());
        assertEquals(2000, warning.limit());
        assertEquals(50.0, warning.percentage(), 0.1);
        assertNotNull(warning.timestamp());
        assertFalse(warning.suggestions().isEmpty());
    }

    @Test
    @DisplayName("CompactWarningHook CompactWarning limitExceeded factory")
    void compactWarningLimitExceeded() {
        CompactWarningHook.CompactWarning warning = CompactWarningHook.CompactWarning.limitExceeded(2500, 2000);

        assertEquals(CompactWarningHook.WarningType.LIMIT_EXCEEDED, warning.type());
        assertTrue(warning.message().contains("exceeded"));
        assertEquals(2500, warning.currentTokens());
        assertEquals(2000, warning.limit());
        assertEquals(100, warning.percentage(), 0.1);
        assertTrue(warning.requiresAction());
    }

    @Test
    @DisplayName("CompactWarningHook CompactWarning requiresAction")
    void compactWarningRequiresAction() {
        CompactWarningHook.CompactWarning approaching = CompactWarningHook.CompactWarning.approachingLimit(1000, 800, 2000);
        CompactWarningHook.CompactWarning exceeded = CompactWarningHook.CompactWarning.limitExceeded(2500, 2000);

        assertFalse(approaching.requiresAction());
        assertTrue(exceeded.requiresAction());
    }

    @Test
    @DisplayName("CompactWarningHook CompactWarning record")
    void compactWarningRecord() {
        Instant now = Instant.now();
        CompactWarningHook.CompactWarning warning = new CompactWarningHook.CompactWarning(
            CompactWarningHook.WarningType.COMPACTION_RECOMMENDED,
            "Test message", 500, 400, 1000, 50.0,
            List.of("Suggestion 1", "Suggestion 2"), now
        );

        assertEquals(CompactWarningHook.WarningType.COMPACTION_RECOMMENDED, warning.type());
        assertEquals("Test message", warning.message());
        assertEquals(500, warning.currentTokens());
        assertEquals(400, warning.threshold());
        assertEquals(1000, warning.limit());
        assertEquals(50.0, warning.percentage(), 0.01);
        assertEquals(2, warning.suggestions().size());
        assertEquals(now, warning.timestamp());
    }

    @Test
    @DisplayName("CompactWarningHook checkWarning returns empty when below threshold")
    void checkWarningBelowThreshold() {
        CompactWarningHook hook = new CompactWarningHook();

        Optional<CompactWarningHook.CompactWarning> warning = hook.checkWarning(500, 800, 2000);

        assertFalse(warning.isPresent());
    }

    @Test
    @DisplayName("CompactWarningHook checkWarning returns approaching limit")
    void checkWarningApproachingLimit() {
        CompactWarningHook hook = new CompactWarningHook();

        Optional<CompactWarningHook.CompactWarning> warning = hook.checkWarning(1000, 800, 2000);

        assertTrue(warning.isPresent());
        assertEquals(CompactWarningHook.WarningType.APPROACHING_LIMIT, warning.get().type());
    }

    @Test
    @DisplayName("CompactWarningHook checkWarning returns limit exceeded")
    void checkWarningLimitExceeded() {
        CompactWarningHook hook = new CompactWarningHook();

        Optional<CompactWarningHook.CompactWarning> warning = hook.checkWarning(2500, 800, 2000);

        assertTrue(warning.isPresent());
        assertEquals(CompactWarningHook.WarningType.LIMIT_EXCEEDED, warning.get().type());
    }

    @Test
    @DisplayName("CompactWarningHook checkWarning updates state")
    void checkWarningUpdatesState() {
        CompactWarningHook hook = new CompactWarningHook();

        hook.checkWarning(1000, 800, 2000);

        assertTrue(hook.getState().isWarningActive());
        assertEquals(1, hook.getState().getWarningCount());
    }

    @Test
    @DisplayName("CompactWarningHook listener is notified")
    void listenerNotified() {
        CompactWarningHook hook = new CompactWarningHook();
        AtomicReference<CompactWarningHook.CompactWarning> received = new AtomicReference<>();

        hook.addListener(received::set);
        hook.checkWarning(1000, 800, 2000);

        assertNotNull(received.get());
        assertEquals(CompactWarningHook.WarningType.APPROACHING_LIMIT, received.get().type());
    }

    @Test
    @DisplayName("CompactWarningHook removeListener stops notifications")
    void removeListener() {
        CompactWarningHook hook = new CompactWarningHook();
        AtomicReference<CompactWarningHook.CompactWarning> received = new AtomicReference<>();

        CompactWarningHook.WarningListener listener = received::set;
        hook.addListener(listener);
        hook.removeListener(listener);
        hook.checkWarning(1000, 800, 2000);

        assertNull(received.get());
    }

    @Test
    @DisplayName("CompactWarningHook getState returns state")
    void getState() {
        CompactWarningHook hook = new CompactWarningHook();

        assertNotNull(hook.getState());
    }
}