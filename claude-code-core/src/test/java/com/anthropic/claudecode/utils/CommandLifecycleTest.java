/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CommandLifecycle.
 */
class CommandLifecycleTest {

    @BeforeEach
    void setUp() {
        CommandLifecycle.clearListener();
    }

    @Test
    @DisplayName("CommandLifecycle State enum")
    void stateEnum() {
        CommandLifecycle.State[] states = CommandLifecycle.State.values();
        assertEquals(2, states.length);
        assertEquals(CommandLifecycle.State.STARTED, CommandLifecycle.State.valueOf("STARTED"));
        assertEquals(CommandLifecycle.State.COMPLETED, CommandLifecycle.State.valueOf("COMPLETED"));
    }

    @Test
    @DisplayName("CommandLifecycle setCommandLifecycleListener")
    void setListener() {
        AtomicReference<String> receivedUuid = new AtomicReference<>();
        AtomicReference<CommandLifecycle.State> receivedState = new AtomicReference<>();

        CommandLifecycle.setCommandLifecycleListener((uuid, state) -> {
            receivedUuid.set(uuid);
            receivedState.set(state);
        });

        CommandLifecycle.notifyCommandLifecycle("test-uuid", CommandLifecycle.State.STARTED);

        assertEquals("test-uuid", receivedUuid.get());
        assertEquals(CommandLifecycle.State.STARTED, receivedState.get());
    }

    @Test
    @DisplayName("CommandLifecycle notifyStarted")
    void notifyStarted() {
        AtomicReference<String> receivedUuid = new AtomicReference<>();
        AtomicReference<CommandLifecycle.State> receivedState = new AtomicReference<>();

        CommandLifecycle.setCommandLifecycleListener((uuid, state) -> {
            receivedUuid.set(uuid);
            receivedState.set(state);
        });

        CommandLifecycle.notifyStarted("started-uuid");

        assertEquals("started-uuid", receivedUuid.get());
        assertEquals(CommandLifecycle.State.STARTED, receivedState.get());
    }

    @Test
    @DisplayName("CommandLifecycle notifyCompleted")
    void notifyCompleted() {
        AtomicReference<String> receivedUuid = new AtomicReference<>();
        AtomicReference<CommandLifecycle.State> receivedState = new AtomicReference<>();

        CommandLifecycle.setCommandLifecycleListener((uuid, state) -> {
            receivedUuid.set(uuid);
            receivedState.set(state);
        });

        CommandLifecycle.notifyCompleted("completed-uuid");

        assertEquals("completed-uuid", receivedUuid.get());
        assertEquals(CommandLifecycle.State.COMPLETED, receivedState.get());
    }

    @Test
    @DisplayName("CommandLifecycle no listener does not throw")
    void noListener() {
        assertDoesNotThrow(() -> CommandLifecycle.notifyStarted("test-uuid"));
        assertDoesNotThrow(() -> CommandLifecycle.notifyCompleted("test-uuid"));
        assertDoesNotThrow(() -> CommandLifecycle.notifyCommandLifecycle("test-uuid", CommandLifecycle.State.STARTED));
    }

    @Test
    @DisplayName("CommandLifecycle clearListener")
    void clearListener() {
        int[] callCount = new int[]{0};

        CommandLifecycle.setCommandLifecycleListener((uuid, state) -> {
            callCount[0]++;
        });

        CommandLifecycle.notifyStarted("test-uuid");
        assertEquals(1, callCount[0]);

        CommandLifecycle.clearListener();

        CommandLifecycle.notifyStarted("test-uuid-2");
        assertEquals(1, callCount[0]); // Not incremented after clear
    }

    @Test
    @DisplayName("CommandLifecycle replace listener")
    void replaceListener() {
        AtomicReference<String> receivedUuid = new AtomicReference<>();

        CommandLifecycle.setCommandLifecycleListener((uuid, state) -> {
            receivedUuid.set("first-" + uuid);
        });

        CommandLifecycle.notifyStarted("uuid-1");
        assertEquals("first-uuid-1", receivedUuid.get());

        CommandLifecycle.setCommandLifecycleListener((uuid, state) -> {
            receivedUuid.set("second-" + uuid);
        });

        CommandLifecycle.notifyStarted("uuid-2");
        assertEquals("second-uuid-2", receivedUuid.get());
    }
}