/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Subscription.
 */
class SubscriptionTest {

    @Test
    @DisplayName("Subscription calls unsubscribe on close")
    void subscriptionCloses() {
        AtomicBoolean called = new AtomicBoolean(false);
        Subscription sub = new Subscription(() -> called.set(true));

        assertFalse(sub.isClosed());
        sub.close();

        assertTrue(sub.isClosed());
        assertTrue(called.get());
    }

    @Test
    @DisplayName("Subscription only unsubscribes once")
    void subscriptionUnsubscribesOnce() {
        AtomicInteger count = new AtomicInteger(0);
        Subscription sub = new Subscription(() -> count.incrementAndGet());

        sub.close();
        sub.close();
        sub.close();

        assertEquals(1, count.get());
    }

    @Test
    @DisplayName("composite closes all subscriptions")
    void compositeClosesAll() {
        AtomicBoolean called1 = new AtomicBoolean(false);
        AtomicBoolean called2 = new AtomicBoolean(false);

        Subscription sub1 = new Subscription(() -> called1.set(true));
        Subscription sub2 = new Subscription(() -> called2.set(true));

        Subscription composite = Subscription.composite(sub1, sub2);
        composite.close();

        assertTrue(called1.get());
        assertTrue(called2.get());
        assertTrue(composite.isClosed());
    }

    @Test
    @DisplayName("Subscription works with try-with-resources")
    void subscriptionTryWithResources() {
        AtomicBoolean called = new AtomicBoolean(false);

        try (Subscription sub = new Subscription(() -> called.set(true))) {
            assertFalse(sub.isClosed());
            assertFalse(called.get());
        }

        assertTrue(called.get());
    }
}