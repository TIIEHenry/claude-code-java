/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code circuit breaker
 */
package com.anthropic.claudecode.utils;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Circuit breaker for fault tolerance.
 */
public final class CircuitBreaker {
    private final String name;
    private final int failureThreshold;
    private final Duration resetTimeout;
    private final Duration halfOpenTimeout;

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong stateChangedTime = new AtomicLong(System.currentTimeMillis());

    public enum State {
        CLOSED,      // Normal operation
        OPEN,        // Failing, reject all calls
        HALF_OPEN    // Testing if recovered
    }

    public CircuitBreaker(String name, int failureThreshold, Duration resetTimeout) {
        this(name, failureThreshold, resetTimeout, Duration.ofSeconds(30));
    }

    public CircuitBreaker(String name, int failureThreshold, Duration resetTimeout, Duration halfOpenTimeout) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.resetTimeout = resetTimeout;
        this.halfOpenTimeout = halfOpenTimeout;
    }

    /**
     * Execute through circuit breaker.
     */
    public <T> T execute(Supplier<T> operation) throws CircuitBreakerException {
        return execute(operation, null);
    }

    /**
     * Execute with fallback.
     */
    public <T> T execute(Supplier<T> operation, Supplier<T> fallback) throws CircuitBreakerException {
        State currentState = getCurrentState();

        if (currentState == State.OPEN) {
            if (fallback != null) {
                return fallback.get();
            }
            throw new CircuitBreakerException(name, "Circuit breaker is OPEN");
        }

        if (currentState == State.HALF_OPEN) {
            // Allow limited calls in half-open state
            if (successCount.get() >= failureThreshold) {
                transitionTo(State.CLOSED);
            }
        }

        try {
            T result = operation.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            if (fallback != null) {
                return fallback.get();
            }
            throw new CircuitBreakerException(name, "Operation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute runnable through circuit breaker.
     */
    public void execute(Runnable operation) throws CircuitBreakerException {
        execute(operation, null);
    }

    /**
     * Execute runnable with fallback.
     */
    public void execute(Runnable operation, Runnable fallback) throws CircuitBreakerException {
        execute(() -> {
            operation.run();
            return null;
        }, fallback != null ? () -> {
            fallback.run();
            return null;
        } : null);
    }

    /**
     * Get current state (may transition automatically).
     */
    public State getCurrentState() {
        State currentState = state.get();

        if (currentState == State.OPEN) {
            long elapsed = System.currentTimeMillis() - lastFailureTime.get();
            if (elapsed >= resetTimeout.toMillis()) {
                transitionTo(State.HALF_OPEN);
                return State.HALF_OPEN;
            }
        }

        if (currentState == State.HALF_OPEN) {
            long elapsed = System.currentTimeMillis() - stateChangedTime.get();
            if (elapsed >= halfOpenTimeout.toMillis()) {
                transitionTo(State.OPEN);
                return State.OPEN;
            }
        }

        return currentState;
    }

    /**
     * Get state without auto-transition.
     */
    public State getState() {
        return state.get();
    }

    /**
     * Handle success.
     */
    private void onSuccess() {
        State currentState = state.get();

        if (currentState == State.HALF_OPEN) {
            successCount.incrementAndGet();
            if (successCount.get() >= failureThreshold) {
                transitionTo(State.CLOSED);
            }
        } else {
            failureCount.set(0);
        }
    }

    /**
     * Handle failure.
     */
    private void onFailure() {
        lastFailureTime.set(System.currentTimeMillis());

        State currentState = state.get();

        if (currentState == State.HALF_OPEN) {
            transitionTo(State.OPEN);
        } else {
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold) {
                transitionTo(State.OPEN);
            }
        }
    }

    /**
     * Transition to new state.
     */
    private void transitionTo(State newState) {
        State oldState = state.get();
        if (oldState != newState && state.compareAndSet(oldState, newState)) {
            stateChangedTime.set(System.currentTimeMillis());

            if (newState == State.CLOSED) {
                failureCount.set(0);
                successCount.set(0);
            } else if (newState == State.HALF_OPEN) {
                successCount.set(0);
            } else if (newState == State.OPEN) {
                failureCount.set(failureThreshold);
            }
        }
    }

    /**
     * Manually open circuit breaker.
     */
    public void open() {
        transitionTo(State.OPEN);
        lastFailureTime.set(System.currentTimeMillis());
    }

    /**
     * Manually close circuit breaker.
     */
    public void close() {
        transitionTo(State.CLOSED);
    }

    /**
     * Manually half-open circuit breaker.
     */
    public void halfOpen() {
        transitionTo(State.HALF_OPEN);
    }

    /**
     * Get failure count.
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * Get success count (in half-open state).
     */
    public int getSuccessCount() {
        return successCount.get();
    }

    /**
     * Get name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get failure threshold.
     */
    public int getFailureThreshold() {
        return failureThreshold;
    }

    /**
     * Get reset timeout.
     */
    public Duration getResetTimeout() {
        return resetTimeout;
    }

    /**
     * Get time since last failure.
     */
    public Duration getTimeSinceLastFailure() {
        return Duration.ofMillis(System.currentTimeMillis() - lastFailureTime.get());
    }

    /**
     * Check if circuit breaker is healthy (closed).
     */
    public boolean isHealthy() {
        return getState() == State.CLOSED;
    }

    /**
     * Check if circuit breaker is open.
     */
    public boolean isOpen() {
        return getState() == State.OPEN;
    }

    /**
     * Check if circuit breaker is half-open.
     */
    public boolean isHalfOpen() {
        return getState() == State.HALF_OPEN;
    }

    /**
     * Get statistics.
     */
    public CircuitBreakerStats getStats() {
        return new CircuitBreakerStats(
            name,
            getState(),
            failureCount.get(),
            successCount.get(),
            failureThreshold,
            Duration.ofMillis(System.currentTimeMillis() - stateChangedTime.get()),
            Duration.ofMillis(System.currentTimeMillis() - lastFailureTime.get())
        );
    }

    @Override
    public String toString() {
        return String.format("CircuitBreaker[%s, state=%s, failures=%d/%d]",
            name, getState(), getFailureCount(), failureThreshold);
    }

    /**
     * Circuit breaker exception.
     */
    public static final class CircuitBreakerException extends RuntimeException {
        private final String circuitBreakerName;

        public CircuitBreakerException(String name, String message) {
            super(message);
            this.circuitBreakerName = name;
        }

        public CircuitBreakerException(String name, String message, Throwable cause) {
            super(message, cause);
            this.circuitBreakerName = name;
        }

        public String getCircuitBreakerName() {
            return circuitBreakerName;
        }
    }

    /**
     * Circuit breaker statistics.
     */
    public record CircuitBreakerStats(
        String name,
        State state,
        int failureCount,
        int successCount,
        int failureThreshold,
        Duration timeInState,
        Duration timeSinceLastFailure
    ) {
        public String format() {
            return String.format("%s: state=%s, failures=%d/%d, timeInState=%s",
                name, state, failureCount, failureThreshold,
                DurationUtils.formatCompact(timeInState));
        }
    }

    /**
     * Circuit breaker builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class.
     */
    public static final class Builder {
        private String name = "default";
        private int failureThreshold = 5;
        private Duration resetTimeout = Duration.ofSeconds(60);
        private Duration halfOpenTimeout = Duration.ofSeconds(30);

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder failureThreshold(int threshold) {
            this.failureThreshold = threshold;
            return this;
        }

        public Builder resetTimeout(Duration timeout) {
            this.resetTimeout = timeout;
            return this;
        }

        public Builder resetTimeout(long seconds) {
            this.resetTimeout = Duration.ofSeconds(seconds);
            return this;
        }

        public Builder halfOpenTimeout(Duration timeout) {
            this.halfOpenTimeout = timeout;
            return this;
        }

        public CircuitBreaker build() {
            return new CircuitBreaker(name, failureThreshold, resetTimeout, halfOpenTimeout);
        }
    }

    /**
     * Circuit breaker registry.
     */
    public static final class CircuitBreakerRegistry {
        private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

        public CircuitBreaker getOrCreate(String name, int failureThreshold, Duration resetTimeout) {
            return circuitBreakers.computeIfAbsent(name,
                n -> new CircuitBreaker(n, failureThreshold, resetTimeout));
        }

        public CircuitBreaker getOrCreate(String name, Builder builder) {
            return circuitBreakers.computeIfAbsent(name, n -> builder.name(n).build());
        }

        public Optional<CircuitBreaker> get(String name) {
            return Optional.ofNullable(circuitBreakers.get(name));
        }

        public void remove(String name) {
            circuitBreakers.remove(name);
        }

        public void clear() {
            circuitBreakers.clear();
        }

        public Map<String, CircuitBreakerStats> getAllStats() {
            return circuitBreakers.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().getStats()
                ));
        }

        public List<CircuitBreaker> getAll() {
            return new ArrayList<>(circuitBreakers.values());
        }
    }
}