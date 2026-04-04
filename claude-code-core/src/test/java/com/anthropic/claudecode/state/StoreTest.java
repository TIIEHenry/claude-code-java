/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.state;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Store.
 */
class StoreTest {

    @Test
    @DisplayName("Store create returns store")
    void create() {
        Store<String> store = Store.create("initial");

        assertNotNull(store);
        assertEquals("initial", store.getState());
    }

    @Test
    @DisplayName("Store getState returns current state")
    void getState() {
        Store<Integer> store = new Store<>(42);

        assertEquals(42, store.getState());
    }

    @Test
    @DisplayName("Store setState updates state")
    void setState() {
        Store<String> store = new Store<>("initial");

        store.setState("updated");

        assertEquals("updated", store.getState());
    }

    @Test
    @DisplayName("Store updateState updates using function")
    void updateState() {
        Store<Integer> store = new Store<>(10);

        store.updateState(n -> n * 2);

        assertEquals(20, store.getState());
    }

    @Test
    @DisplayName("Store subscribe receives updates")
    void subscribe() {
        Store<String> store = new Store<>("initial");
        AtomicReference<String> received = new AtomicReference<>("");

        store.subscribe(state -> received.set(state));

        store.setState("new");

        assertEquals("new", received.get());
    }

    @Test
    @DisplayName("Store subscribe unsubscribe stops receiving")
    void subscribeUnsubscribe() {
        Store<String> store = new Store<>("initial");
        AtomicReference<String> received = new AtomicReference<>("");

        Store.Subscription sub = store.subscribe(state -> received.set(state));
        sub.unsubscribe();

        store.setState("new");

        assertEquals("", received.get());
    }

    @Test
    @DisplayName("Store onChange receives old and new state")
    void onChange() {
        Store<String> store = new Store<>("initial");
        AtomicReference<String> oldState = new AtomicReference<>("");
        AtomicReference<String> newState = new AtomicReference<>("");

        store.onChange((old, newS) -> {
            oldState.set(old);
            newState.set(newS);
        });

        store.setState("updated");

        assertEquals("initial", oldState.get());
        assertEquals("updated", newState.get());
    }

    @Test
    @DisplayName("Store Subscription unsubscribe idempotent")
    void subscriptionIdempotent() {
        Store<String> store = new Store<>("initial");
        Store.Subscription sub = store.subscribe(s -> {});

        sub.unsubscribe();
        sub.unsubscribe(); // Should not throw
    }
}