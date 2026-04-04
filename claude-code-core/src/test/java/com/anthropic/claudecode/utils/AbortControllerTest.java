/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AbortController.
 */
class AbortControllerTest {

    @Test
    @DisplayName("AbortController starts not aborted")
    void startsNotAborted() {
        AbortController controller = new AbortController();
        assertFalse(controller.isAborted());
        assertNull(controller.getReason());
    }

    @Test
    @DisplayName("AbortController aborts without reason")
    void abortWithoutReason() {
        AbortController controller = new AbortController();
        controller.abort();

        assertTrue(controller.isAborted());
        assertNull(controller.getReason());
    }

    @Test
    @DisplayName("AbortController aborts with reason")
    void abortWithReason() {
        AbortController controller = new AbortController();
        controller.abort("User cancelled");

        assertTrue(controller.isAborted());
        assertEquals("User cancelled", controller.getReason());
    }

    @Test
    @DisplayName("AbortController only aborts once")
    void abortsOnlyOnce() {
        AbortController controller = new AbortController();
        controller.abort("First reason");
        controller.abort("Second reason");

        assertEquals("First reason", controller.getReason());
    }

    @Test
    @DisplayName("AbortSignal reflects controller state")
    void signalReflectsState() {
        AbortController controller = new AbortController();
        AbortController.AbortSignal signal = controller.getSignal();

        assertFalse(signal.isAborted());

        controller.abort("Test");
        assertTrue(signal.isAborted());
        assertEquals("Test", signal.getReason());
    }

    @Test
    @DisplayName("Abort listeners are called on abort")
    void listenersCalled() {
        AbortController controller = new AbortController();
        AtomicBoolean called = new AtomicBoolean(false);

        controller.addListener(reason -> called.set(true));
        controller.abort("Test");

        assertTrue(called.get());
    }

    @Test
    @DisplayName("Abort listener receives reason")
    void listenerReceivesReason() {
        AbortController controller = new AbortController();
        String[] receivedReason = new String[1];

        controller.addListener(reason -> receivedReason[0] = reason);
        controller.abort("Test reason");

        assertEquals("Test reason", receivedReason[0]);
    }

    @Test
    @DisplayName("Child aborts when parent aborts")
    void childAbortsWhenParentAborts() {
        AbortController parent = new AbortController();
        AbortController child = parent.createChild();

        assertFalse(child.isAborted());

        parent.abort("Parent reason");
        assertTrue(child.isAborted());
        assertEquals("Parent reason", child.getReason());
    }

    @Test
    @DisplayName("Already aborted parent creates aborted child")
    void abortedParentCreatesAbortedChild() {
        AbortController parent = new AbortController();
        parent.abort("Already aborted");

        AbortController child = parent.createChild();
        assertTrue(child.isAborted());
        assertEquals("Already aborted", child.getReason());
    }

    @Test
    @DisplayName("Remove listener works")
    void removeListenerWorks() {
        AbortController controller = new AbortController();
        AtomicBoolean called = new AtomicBoolean(false);

        AbortController.AbortListener listener = reason -> called.set(true);
        controller.addListener(listener);
        controller.removeListener(listener);
        controller.abort("Test");

        assertFalse(called.get());
    }

    @Test
    @DisplayName("Combine abort signals - first aborted")
    void combineFirstAborted() {
        AbortController ctrl1 = new AbortController();
        ctrl1.abort("First");
        AbortController ctrl2 = new AbortController();

        AbortController combined = AbortController.combine(
            ctrl1.getSignal(), ctrl2.getSignal()
        );

        assertTrue(combined.isAborted());
        assertEquals("First", combined.getReason());
    }

    @Test
    @DisplayName("Combine abort signals - abort propagates")
    void combineAbortPropagates() {
        AbortController ctrl1 = new AbortController();
        AbortController ctrl2 = new AbortController();

        AbortController combined = AbortController.combine(
            ctrl1.getSignal(), ctrl2.getSignal()
        );

        assertFalse(combined.isAborted());

        ctrl1.abort("Signal 1 aborted");
        assertTrue(combined.isAborted());
        assertEquals("Signal 1 aborted", combined.getReason());
    }
}