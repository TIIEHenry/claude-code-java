/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.hooks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HookEvents.
 */
class HookEventsTest {

    @BeforeEach
    void setUp() {
        HookEvents.clearHookEventState();
    }

    @Test
    @DisplayName("HookEvents HookStartedEvent record")
    void hookStartedEvent() {
        HookEvents.HookStartedEvent event = new HookEvents.HookStartedEvent("hook-1", "TestHook", "PreToolUse");
        assertEquals("started", event.type());
        assertEquals("hook-1", event.hookId());
        assertEquals("TestHook", event.hookName());
        assertEquals("PreToolUse", event.hookEvent());
    }

    @Test
    @DisplayName("HookEvents HookProgressEvent record")
    void hookProgressEvent() {
        HookEvents.HookProgressEvent event = new HookEvents.HookProgressEvent(
            "hook-1", "TestHook", "PreToolUse", "stdout", "stderr", "combined"
        );
        assertEquals("progress", event.type());
        assertEquals("hook-1", event.hookId());
        assertEquals("stdout", event.stdout());
        assertEquals("stderr", event.stderr());
        assertEquals("combined", event.output());
    }

    @Test
    @DisplayName("HookEvents HookResponseEvent record")
    void hookResponseEvent() {
        HookEvents.HookResponseEvent event = new HookEvents.HookResponseEvent(
            "hook-1", "TestHook", "PreToolUse", "output", "stdout", "stderr", 0, "success"
        );
        assertEquals("response", event.type());
        assertEquals("hook-1", event.hookId());
        assertEquals(0, event.exitCode());
        assertEquals("success", event.outcome());
    }

    @Test
    @DisplayName("HookEvents registerHookEventHandler receives events")
    void registerHookEventHandler() {
        AtomicReference<HookEvents.HookExecutionEvent> received = new AtomicReference<>();
        HookEvents.registerHookEventHandler(received::set);

        HookEvents.emitHookStarted("hook-1", "TestHook", "SessionStart");

        assertNotNull(received.get());
        assertEquals("hook-1", ((HookEvents.HookStartedEvent) received.get()).hookId());
    }

    @Test
    @DisplayName("HookEvents emitHookStarted without handler queues event")
    void emitHookStartedWithoutHandler() {
        // Clear any existing handler
        HookEvents.clearHookEventState();

        HookEvents.emitHookStarted("hook-1", "TestHook", "SessionStart");

        // Now register handler - should receive queued event
        AtomicReference<HookEvents.HookExecutionEvent> received = new AtomicReference<>();
        HookEvents.registerHookEventHandler(received::set);

        assertNotNull(received.get());
    }

    @Test
    @DisplayName("HookEvents emitHookProgress emits progress event")
    void emitHookProgress() {
        AtomicReference<HookEvents.HookExecutionEvent> received = new AtomicReference<>();
        HookEvents.registerHookEventHandler(received::set);

        HookEvents.emitHookProgress("hook-1", "TestHook", "SessionStart", "out", "err", "combined");

        assertNotNull(received.get());
        assertTrue(received.get() instanceof HookEvents.HookProgressEvent);
    }

    @Test
    @DisplayName("HookEvents emitHookResponse emits response event")
    void emitHookResponse() {
        AtomicReference<HookEvents.HookExecutionEvent> received = new AtomicReference<>();
        HookEvents.registerHookEventHandler(received::set);

        HookEvents.emitHookResponse("hook-1", "TestHook", "SessionStart", "out", "stdout", "stderr", 0, "done");

        assertNotNull(received.get());
        assertTrue(received.get() instanceof HookEvents.HookResponseEvent);
    }

    @Test
    @DisplayName("HookEvents setAllHookEventsEnabled controls emission")
    void setAllHookEventsEnabled() {
        AtomicReference<HookEvents.HookExecutionEvent> received = new AtomicReference<>();
        HookEvents.registerHookEventHandler(received::set);

        HookEvents.setAllHookEventsEnabled(true);
        HookEvents.emitHookStarted("hook-1", "TestHook", "CustomEvent");

        assertNotNull(received.get());

        received.set(null);
        HookEvents.setAllHookEventsEnabled(false);
        HookEvents.emitHookStarted("hook-2", "TestHook", "CustomEvent");

        // Should not emit for non-always-emitted events
        assertNull(received.get());
    }

    @Test
    @DisplayName("HookEvents clearHookEventState clears state")
    void clearHookEventState() {
        AtomicReference<HookEvents.HookExecutionEvent> received = new AtomicReference<>();
        HookEvents.registerHookEventHandler(received::set);

        HookEvents.clearHookEventState();
        HookEvents.emitHookStarted("hook-1", "TestHook", "SessionStart");

        // After clear, no handler is registered, but event should still queue
        received.set(null);
        HookEvents.registerHookEventHandler(received::set);
        assertNotNull(received.get());
    }

    @Test
    @DisplayName("HookEvents startHookProgressInterval returns runnable")
    void startHookProgressInterval() {
        Runnable stopper = HookEvents.startHookProgressInterval(
            "hook-1", "TestHook", "SessionStart", () -> new AsyncHookRegistry.OutputResult("", "", "")
        );
        assertNotNull(stopper);
        stopper.run(); // Should not throw
    }

    @Test
    @DisplayName("HookEvents HookExecutionEvent sealed interface")
    void hookExecutionEventSealed() {
        // Verify all event types implement the interface
        HookEvents.HookExecutionEvent started = new HookEvents.HookStartedEvent("id", "name", "event");
        HookEvents.HookExecutionEvent progress = new HookEvents.HookProgressEvent("id", "name", "event", "", "", "");
        HookEvents.HookExecutionEvent response = new HookEvents.HookResponseEvent("id", "name", "event", "", "", "", 0, "");

        assertNotNull(started);
        assertNotNull(progress);
        assertNotNull(response);
    }
}