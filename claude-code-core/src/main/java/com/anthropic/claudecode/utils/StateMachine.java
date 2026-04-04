/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code state machine
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;

/**
 * State machine implementation.
 */
public final class StateMachine<S> {
    private final String name;
    private final Map<S, Map<S, Transition<S>>> transitions;
    private final Map<S, List<Consumer<S>>> enterActions;
    private final Map<S, List<Consumer<S>>> exitActions;
    private final List<Consumer<TransitionEvent<S>>> globalListeners;
    private final AtomicReference<S> currentState;
    private final AtomicBoolean running;

    public StateMachine(String name, S initialState) {
        this.name = name;
        this.transitions = new HashMap<>();
        this.enterActions = new HashMap<>();
        this.exitActions = new HashMap<>();
        this.globalListeners = new ArrayList<>();
        this.currentState = new AtomicReference<>(initialState);
        this.running = new AtomicBoolean(false);
    }

    /**
     * Add transition.
     */
    public StateMachine<S> addTransition(S from, S to) {
        return addTransition(from, to, null, null);
    }

    /**
     * Add transition with condition and action.
     */
    public StateMachine<S> addTransition(S from, S to,
            Predicate<S> condition, Consumer<S> action) {
        transitions.computeIfAbsent(from, k -> new HashMap<>())
            .put(to, new Transition<>(from, to, condition, action));
        return this;
    }

    /**
     * Add enter action for state.
     */
    public StateMachine<S> onEnter(S state, Consumer<S> action) {
        enterActions.computeIfAbsent(state, k -> new ArrayList<>()).add(action);
        return this;
    }

    /**
     * Add exit action for state.
     */
    public StateMachine<S> onExit(S state, Consumer<S> action) {
        exitActions.computeIfAbsent(state, k -> new ArrayList<>()).add(action);
        return this;
    }

    /**
     * Add global listener.
     */
    public StateMachine<S> addListener(Consumer<TransitionEvent<S>> listener) {
        globalListeners.add(listener);
        return this;
    }

    /**
     * Start state machine.
     */
    public StateMachine<S> start() {
        if (running.compareAndSet(false, true)) {
            S initial = currentState.get();
            executeEnterActions(initial);
        }
        return this;
    }

    /**
     * Stop state machine.
     */
    public StateMachine<S> stop() {
        running.set(false);
        return this;
    }

    /**
     * Try transition to new state.
     */
    public boolean transitionTo(S newState) {
        if (!running.get()) {
            throw new IllegalStateException("State machine is not running");
        }

        S current = currentState.get();
        Map<S, Transition<S>> availableTransitions = transitions.get(current);

        if (availableTransitions == null) {
            return false;
        }

        Transition<S> transition = availableTransitions.get(newState);
        if (transition == null) {
            return false;
        }

        // Check condition
        if (transition.condition != null && !transition.condition.test(current)) {
            return false;
        }

        // Execute transition
        executeTransition(current, newState, transition);
        return true;
    }

    /**
     * Execute transition.
     */
    private void executeTransition(S from, S to, Transition<S> transition) {
        // Exit current state
        executeExitActions(from);

        // Transition action
        if (transition.action != null) {
            transition.action.accept(from);
        }

        // Update state
        currentState.set(to);

        // Enter new state
        executeEnterActions(to);

        // Notify listeners
        TransitionEvent<S> event = new TransitionEvent<>(name, from, to, transition);
        globalListeners.forEach(listener -> listener.accept(event));
    }

    /**
     * Execute enter actions.
     */
    private void executeEnterActions(S state) {
        List<Consumer<S>> actions = enterActions.get(state);
        if (actions != null) {
            actions.forEach(action -> action.accept(state));
        }
    }

    /**
     * Execute exit actions.
     */
    private void executeExitActions(S state) {
        List<Consumer<S>> actions = exitActions.get(state);
        if (actions != null) {
            actions.forEach(action -> action.accept(state));
        }
    }

    /**
     * Get current state.
     */
    public S getCurrentState() {
        return currentState.get();
    }

    /**
     * Get possible next states.
     */
    public List<S> getPossibleTransitions() {
        S current = currentState.get();
        Map<S, Transition<S>> available = transitions.get(current);
        if (available == null) {
            return List.of();
        }
        return new ArrayList<>(available.keySet());
    }

    /**
     * Check if can transition to.
     */
    public boolean canTransitionTo(S state) {
        S current = currentState.get();
        Map<S, Transition<S>> available = transitions.get(current);
        if (available == null) {
            return false;
        }
        Transition<S> transition = available.get(state);
        if (transition == null) {
            return false;
        }
        return transition.condition == null || transition.condition.test(current);
    }

    /**
     * Is running.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Get name.
     */
    public String getName() {
        return name;
    }

    /**
     * Reset to initial state.
     */
    public StateMachine<S> reset(S initialState) {
        stop();
        currentState.set(initialState);
        return this;
    }

    /**
     * Transition record.
     */
    private record Transition<S>(S from, S to, Predicate<S> condition, Consumer<S> action) {}

    /**
     * Transition event.
     */
    public record TransitionEvent<S>(String machineName, S from, S to, Transition<S> transition) {}

    /**
     * State machine builder.
     */
    public static <S> Builder<S> builder(String name, S initialState) {
        return new Builder<>(name, initialState);
    }

    /**
     * Builder class.
     */
    public static final class Builder<S> {
        private final StateMachine<S> machine;

        public Builder(String name, S initialState) {
            this.machine = new StateMachine<>(name, initialState);
        }

        public Builder<S> transition(S from, S to) {
            machine.addTransition(from, to);
            return this;
        }

        public Builder<S> transition(S from, S to, Predicate<S> condition) {
            machine.addTransition(from, to, condition, null);
            return this;
        }

        public Builder<S> transition(S from, S to, Consumer<S> action) {
            machine.addTransition(from, to, null, action);
            return this;
        }

        public Builder<S> transition(S from, S to, Predicate<S> condition, Consumer<S> action) {
            machine.addTransition(from, to, condition, action);
            return this;
        }

        public Builder<S> onEnter(S state, Consumer<S> action) {
            machine.onEnter(state, action);
            return this;
        }

        public Builder<S> onExit(S state, Consumer<S> action) {
            machine.onExit(state, action);
            return this;
        }

        public Builder<S> listener(Consumer<TransitionEvent<S>> listener) {
            machine.addListener(listener);
            return this;
        }

        public StateMachine<S> build() {
            return machine;
        }
    }

    /**
     * Atomic reference helper.
     */
    private static final class AtomicReference<T> extends java.util.concurrent.atomic.AtomicReference<T> {
        public AtomicReference(T initialValue) {
            super(initialValue);
        }
    }

    /**
     * Atomic boolean helper.
     */
    private static final class AtomicBoolean extends java.util.concurrent.atomic.AtomicBoolean {
        public AtomicBoolean(boolean initialValue) {
            super(initialValue);
        }
    }
}