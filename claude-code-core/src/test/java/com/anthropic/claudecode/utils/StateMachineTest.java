/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StateMachine.
 */
class StateMachineTest {

    enum TestState { IDLE, RUNNING, STOPPED, ERROR }

    @Test
    @DisplayName("StateMachine creates with initial state")
    void createsWithInitialState() {
        StateMachine<TestState> machine = new StateMachine<>("test", TestState.IDLE);

        assertEquals(TestState.IDLE, machine.getCurrentState());
        assertEquals("test", machine.getName());
        assertFalse(machine.isRunning());
    }

    @Test
    @DisplayName("StateMachine start sets running")
    void startWorks() {
        StateMachine<TestState> machine = new StateMachine<>("test", TestState.IDLE);

        machine.start();

        assertTrue(machine.isRunning());
    }

    @Test
    @DisplayName("StateMachine stop stops running")
    void stopWorks() {
        StateMachine<TestState> machine = new StateMachine<>("test", TestState.IDLE);

        machine.start();
        machine.stop();

        assertFalse(machine.isRunning());
    }

    @Test
    @DisplayName("StateMachine transitionTo changes state")
    void transitionToWorks() {
        StateMachine<TestState> machine = new StateMachine<>("test", TestState.IDLE);
        machine.addTransition(TestState.IDLE, TestState.RUNNING);

        machine.start();
        boolean result = machine.transitionTo(TestState.RUNNING);

        assertTrue(result);
        assertEquals(TestState.RUNNING, machine.getCurrentState());
    }

    @Test
    @DisplayName("StateMachine transitionTo fails when not running")
    void transitionToNotRunning() {
        StateMachine<TestState> machine = new StateMachine<>("test", TestState.IDLE);
        machine.addTransition(TestState.IDLE, TestState.RUNNING);

        assertThrows(IllegalStateException.class, () -> machine.transitionTo(TestState.RUNNING));
    }

    @Test
    @DisplayName("StateMachine transitionTo fails when no transition")
    void transitionToNoTransition() {
        StateMachine<TestState> machine = new StateMachine<>("test", TestState.IDLE);

        machine.start();
        boolean result = machine.transitionTo(TestState.RUNNING);

        assertFalse(result);
    }

    @Test
    @DisplayName("StateMachine addTransition allows transition")
    void addTransitionWorks() {
        StateMachine<TestState> machine = new StateMachine<>("test", TestState.IDLE);

        machine.addTransition(TestState.IDLE, TestState.RUNNING);
        machine.addTransition(TestState.RUNNING, TestState.STOPPED);

        machine.start();
        assertTrue(machine.canTransitionTo(TestState.RUNNING));

        machine.transitionTo(TestState.RUNNING);
        assertTrue(machine.canTransitionTo(TestState.STOPPED));
    }

    @Test
    @DisplayName("StateMachine onEnter executes on enter")
    void onEnterWorks() {
        AtomicInteger counter = new AtomicInteger(0);
        StateMachine<TestState> machine = new StateMachine<>("test", TestState.IDLE);

        machine.onEnter(TestState.RUNNING, s -> counter.incrementAndGet());
        machine.addTransition(TestState.IDLE, TestState.RUNNING);

        machine.start();
        machine.transitionTo(TestState.RUNNING);

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("StateMachine onExit executes on exit")
    void onExitWorks() {
        AtomicInteger counter = new AtomicInteger(0);
        StateMachine<TestState> machine = new StateMachine<>("test", TestState.IDLE);

        machine.onExit(TestState.IDLE, s -> counter.incrementAndGet());
        machine.addTransition(TestState.IDLE, TestState.RUNNING);

        machine.start();
        machine.transitionTo(TestState.RUNNING);

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("StateMachine addTransition with condition")
    void transitionWithCondition() {
        StateMachine<TestState> machine = new StateMachine<>("test", TestState.IDLE);

        machine.addTransition(TestState.IDLE, TestState.RUNNING, s -> true, null);
        machine.addTransition(TestState.IDLE, TestState.ERROR, s -> false, null);

        machine.start();
        assertTrue(machine.canTransitionTo(TestState.RUNNING));
        assertFalse(machine.canTransitionTo(TestState.ERROR));
    }

    @Test
    @DisplayName("StateMachine addTransition with action")
    void transitionWithAction() {
        AtomicInteger counter = new AtomicInteger(0);
        StateMachine<TestState> machine = new StateMachine<>("test", TestState.IDLE);

        machine.addTransition(TestState.IDLE, TestState.RUNNING, null, s -> counter.incrementAndGet());

        machine.start();
        machine.transitionTo(TestState.RUNNING);

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("StateMachine addListener receives events")
    void listenerWorks() {
        AtomicInteger counter = new AtomicInteger(0);
        StateMachine<TestState> machine = new StateMachine<>("test", TestState.IDLE);

        machine.addListener(event -> counter.incrementAndGet());
        machine.addTransition(TestState.IDLE, TestState.RUNNING);

        machine.start();
        machine.transitionTo(TestState.RUNNING);

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("StateMachine getPossibleTransitions returns list")
    void getPossibleTransitions() {
        StateMachine<TestState> machine = new StateMachine<>("test", TestState.IDLE);

        machine.addTransition(TestState.IDLE, TestState.RUNNING);
        machine.addTransition(TestState.IDLE, TestState.STOPPED);

        machine.start();
        List<TestState> transitions = machine.getPossibleTransitions();

        assertEquals(2, transitions.size());
        assertTrue(transitions.contains(TestState.RUNNING));
        assertTrue(transitions.contains(TestState.STOPPED));
    }

    @Test
    @DisplayName("StateMachine reset resets state")
    void resetWorks() {
        StateMachine<TestState> machine = new StateMachine<>("test", TestState.IDLE);

        machine.addTransition(TestState.IDLE, TestState.RUNNING);
        machine.start();
        machine.transitionTo(TestState.RUNNING);

        machine.reset(TestState.IDLE);

        assertFalse(machine.isRunning());
        assertEquals(TestState.IDLE, machine.getCurrentState());
    }

    @Test
    @DisplayName("StateMachine builder creates machine")
    void builderWorks() {
        StateMachine<TestState> machine = StateMachine.builder("test", TestState.IDLE)
            .transition(TestState.IDLE, TestState.RUNNING)
            .transition(TestState.RUNNING, TestState.STOPPED)
            .onEnter(TestState.RUNNING, s -> {})
            .onExit(TestState.IDLE, s -> {})
            .build();

        assertEquals(TestState.IDLE, machine.getCurrentState());
    }

    @Test
    @DisplayName("StateMachine start executes enter actions")
    void startEnterAction() {
        AtomicInteger counter = new AtomicInteger(0);
        StateMachine<TestState> machine = new StateMachine<>("test", TestState.IDLE);

        machine.onEnter(TestState.IDLE, s -> counter.incrementAndGet());

        machine.start();

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("StateMachine TransitionEvent has info")
    void transitionEvent() {
        StateMachine.TransitionEvent<TestState> event =
            new StateMachine.TransitionEvent<>("test", TestState.IDLE, TestState.RUNNING, null);

        assertEquals("test", event.machineName());
        assertEquals(TestState.IDLE, event.from());
        assertEquals(TestState.RUNNING, event.to());
    }
}