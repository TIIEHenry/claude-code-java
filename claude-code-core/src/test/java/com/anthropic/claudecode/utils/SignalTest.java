/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Signal.
 */
class SignalTest {

    @Test
    @DisplayName("Signal create returns new signal")
    void create() {
        Signal signal = Signal.create();

        assertNotNull(signal);
    }

    @Test
    @DisplayName("Signal subscribe adds listener")
    void subscribe() {
        Signal signal = new Signal();
        AtomicInteger counter = new AtomicInteger(0);

        signal.subscribe(() -> counter.incrementAndGet());
        signal.notifyListeners();

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("Signal notifyListeners increments revision")
    void notifyListenersRevision() {
        Signal signal = new Signal();

        assertEquals(0, signal.getRevision());
        signal.notifyListeners();
        assertEquals(1, signal.getRevision());
        signal.notifyListeners();
        assertEquals(2, signal.getRevision());
    }

    @Test
    @DisplayName("Signal emit is alias for notifyListeners")
    void emit() {
        Signal signal = new Signal();
        AtomicInteger counter = new AtomicInteger(0);

        signal.subscribe(() -> counter.incrementAndGet());
        signal.emit();

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("Signal subscription unsubscribe removes listener")
    void subscriptionUnsubscribe() {
        Signal signal = new Signal();
        AtomicInteger counter = new AtomicInteger(0);

        Subscription sub = signal.subscribe(() -> counter.incrementAndGet());
        signal.notifyListeners();
        assertEquals(1, counter.get());

        sub.close();
        signal.notifyListeners();
        assertEquals(1, counter.get()); // Should not increment
    }

    @Test
    @DisplayName("Signal multiple listeners")
    void multipleListeners() {
        Signal signal = new Signal();
        AtomicInteger counter = new AtomicInteger(0);

        signal.subscribe(() -> counter.incrementAndGet());
        signal.subscribe(() -> counter.incrementAndGet());
        signal.subscribe(() -> counter.incrementAndGet());

        signal.notifyListeners();

        assertEquals(3, counter.get());
    }

    @Test
    @DisplayName("Signal getRevision returns current revision")
    void getRevision() {
        Signal signal = new Signal();

        assertEquals(0, signal.getRevision());

        for (int i = 0; i < 5; i++) {
            signal.notifyListeners();
        }

        assertEquals(5, signal.getRevision());
    }
}