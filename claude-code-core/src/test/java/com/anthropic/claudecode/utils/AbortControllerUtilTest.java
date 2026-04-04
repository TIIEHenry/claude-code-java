/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AbortControllerUtil.
 */
class AbortControllerUtilTest {

    @Test
    @DisplayName("AbortControllerUtil createAbortController creates controller")
    void createAbortController() {
        AbortControllerUtil.AbortController controller = AbortControllerUtil.createAbortController();

        assertNotNull(controller);
        assertFalse(controller.isAborted());
    }

    @Test
    @DisplayName("AbortControllerUtil createAbortController with max listeners")
    void createAbortControllerWithMaxListeners() {
        AbortControllerUtil.AbortController controller = AbortControllerUtil.createAbortController(100);

        assertEquals(100, controller.getMaxListeners());
    }

    @Test
    @DisplayName("AbortControllerUtil abort sets aborted flag")
    void abortSetsFlag() {
        AbortControllerUtil.AbortController controller = AbortControllerUtil.createAbortController();

        controller.abort();

        assertTrue(controller.isAborted());
    }

    @Test
    @DisplayName("AbortControllerUtil abort with reason stores reason")
    void abortWithReason() {
        AbortControllerUtil.AbortController controller = AbortControllerUtil.createAbortController();

        controller.abort("test reason");

        assertEquals("test reason", controller.getReason());
    }

    @Test
    @DisplayName("AbortControllerUtil abort only once")
    void abortOnlyOnce() {
        AbortControllerUtil.AbortController controller = AbortControllerUtil.createAbortController();
        int[] count = {0};
        controller.addListener(reason -> count[0]++);

        controller.abort("first");
        controller.abort("second");

        assertEquals(1, count[0]);
        assertEquals("first", controller.getReason());
    }

    @Test
    @DisplayName("AbortControllerUtil addListener is called on abort")
    void listenerCalled() {
        AbortControllerUtil.AbortController controller = AbortControllerUtil.createAbortController();
        String[] receivedReason = {null};
        controller.addListener(reason -> receivedReason[0] = reason);

        controller.abort("test");

        assertEquals("test", receivedReason[0]);
    }

    @Test
    @DisplayName("AbortControllerUtil removeListener prevents call")
    void removeListenerWorks() {
        AbortControllerUtil.AbortController controller = AbortControllerUtil.createAbortController();
        boolean[] called = {false};
        AbortControllerUtil.Consumer<String> listener = reason -> called[0] = true;

        controller.addListener(listener);
        controller.removeListener(listener);
        controller.abort();

        assertFalse(called[0]);
    }

    @Test
    @DisplayName("AbortControllerUtil setMaxListeners works")
    void setMaxListeners() {
        AbortControllerUtil.AbortController controller = AbortControllerUtil.createAbortController();

        controller.setMaxListeners(200);

        assertEquals(200, controller.getMaxListeners());
    }

    @Test
    @DisplayName("AbortControllerUtil createChildAbortController creates child")
    void createChildAbortController() {
        AbortControllerUtil.AbortController parent = AbortControllerUtil.createAbortController();
        AbortControllerUtil.AbortController child = AbortControllerUtil.createChildAbortController(parent);

        assertNotNull(child);
        assertFalse(child.isAborted());
    }

    @Test
    @DisplayName("AbortControllerUtil child aborts when parent aborts")
    void childAbortsOnParentAbort() {
        AbortControllerUtil.AbortController parent = AbortControllerUtil.createAbortController();
        AbortControllerUtil.AbortController child = AbortControllerUtil.createChildAbortController(parent);

        parent.abort("parent reason");

        assertTrue(child.isAborted());
        assertEquals("parent reason", child.getReason());
    }

    @Test
    @DisplayName("AbortControllerUtil child already aborted if parent is aborted")
    void childAlreadyAborted() {
        AbortControllerUtil.AbortController parent = AbortControllerUtil.createAbortController();
        parent.abort("already aborted");

        AbortControllerUtil.AbortController child = AbortControllerUtil.createChildAbortController(parent);

        assertTrue(child.isAborted());
        assertEquals("already aborted", child.getReason());
    }

    @Test
    @DisplayName("AbortControllerUtil aborting child does not affect parent")
    void childAbortDoesNotAffectParent() {
        AbortControllerUtil.AbortController parent = AbortControllerUtil.createAbortController();
        AbortControllerUtil.AbortController child = AbortControllerUtil.createChildAbortController(parent);

        child.abort("child reason");

        assertFalse(parent.isAborted());
    }

    @Test
    @DisplayName("AbortControllerUtil listener exceptions are caught")
    void listenerExceptionsCaught() {
        AbortControllerUtil.AbortController controller = AbortControllerUtil.createAbortController();
        controller.addListener(reason -> {
            throw new RuntimeException("Test exception");
        });
        boolean[] secondCalled = {false};
        controller.addListener(reason -> secondCalled[0] = true);

        // Should not throw, and second listener should still be called
        assertDoesNotThrow(() -> controller.abort("test"));
        assertTrue(secondCalled[0]);
    }

    @Test
    @DisplayName("AbortControllerUtil Consumer interface works")
    void consumerInterface() {
        AbortControllerUtil.Consumer<String> consumer = s -> {};
        assertNotNull(consumer);
    }
}