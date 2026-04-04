/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AbortControllerUtils.
 */
class AbortControllerUtilsTest {

    @Test
    @DisplayName("AbortControllerUtils DEFAULT_MAX_LISTENERS constant")
    void defaultMaxListeners() {
        assertEquals(50, AbortControllerUtils.DEFAULT_MAX_LISTENERS);
    }

    @Test
    @DisplayName("AbortControllerUtils createAbortController returns controller")
    void createAbortController() {
        AbortControllerUtils.AbortController controller = AbortControllerUtils.createAbortController();
        assertNotNull(controller);
        assertNotNull(controller.getSignal());
    }

    @Test
    @DisplayName("AbortControllerUtils createAbortController with max listeners")
    void createAbortControllerWithMax() {
        AbortControllerUtils.AbortController controller = AbortControllerUtils.createAbortController(10);
        assertNotNull(controller);
    }

    @Test
    @DisplayName("AbortControllerUtils AbortSignal initially not aborted")
    void signalInitiallyNotAborted() {
        AbortControllerUtils.AbortController controller = AbortControllerUtils.createAbortController();
        assertFalse(controller.getSignal().isAborted());
    }

    @Test
    @DisplayName("AbortControllerUtils abort marks signal aborted")
    void abortMarksAborted() {
        AbortControllerUtils.AbortController controller = AbortControllerUtils.createAbortController();
        controller.abort();
        assertTrue(controller.getSignal().isAborted());
    }

    @Test
    @DisplayName("AbortControllerUtils abort with reason")
    void abortWithReason() {
        AbortControllerUtils.AbortController controller = AbortControllerUtils.createAbortController();
        controller.abort("test reason");
        assertTrue(controller.getSignal().isAborted());
        assertEquals("test reason", controller.getSignal().getReason());
    }

    @Test
    @DisplayName("AbortControllerUtils addListener called on abort")
    void listenerCalled() {
        AbortControllerUtils.AbortController controller = AbortControllerUtils.createAbortController();
        AtomicInteger counter = new AtomicInteger(0);

        controller.getSignal().addListener(reason -> counter.incrementAndGet());
        controller.abort("reason");

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("AbortControllerUtils removeListener not called")
    void removeListener() {
        AbortControllerUtils.AbortController controller = AbortControllerUtils.createAbortController();
        AtomicInteger counter = new AtomicInteger(0);

        AbortControllerUtils.AbortListener listener = reason -> counter.incrementAndGet();
        controller.getSignal().addListener(listener);
        controller.getSignal().removeListener(listener);
        controller.abort("reason");

        assertEquals(0, counter.get());
    }

    @Test
    @DisplayName("AbortControllerUtils multiple listeners")
    void multipleListeners() {
        AbortControllerUtils.AbortController controller = AbortControllerUtils.createAbortController();
        AtomicInteger counter = new AtomicInteger(0);

        controller.getSignal().addListener(reason -> counter.incrementAndGet());
        controller.getSignal().addListener(reason -> counter.incrementAndGet());
        controller.getSignal().addListener(reason -> counter.incrementAndGet());
        controller.abort("reason");

        assertEquals(3, counter.get());
    }

    @Test
    @DisplayName("AbortControllerUtils listener limit")
    void listenerLimit() {
        AbortControllerUtils.AbortController controller = AbortControllerUtils.createAbortController(2);
        AtomicInteger counter = new AtomicInteger(0);

        // Add 3 listeners but only 2 should be added due to limit
        controller.getSignal().addListener(reason -> counter.incrementAndGet());
        controller.getSignal().addListener(reason -> counter.incrementAndGet());
        controller.getSignal().addListener(reason -> counter.incrementAndGet()); // Should not be added

        controller.abort("reason");
        assertEquals(2, counter.get());
    }

    @Test
    @DisplayName("AbortControllerUtils abort twice only triggers once")
    void abortTwice() {
        AbortControllerUtils.AbortController controller = AbortControllerUtils.createAbortController();
        AtomicInteger counter = new AtomicInteger(0);

        controller.getSignal().addListener(reason -> counter.incrementAndGet());
        controller.abort("reason1");
        controller.abort("reason2");

        assertEquals(1, counter.get());
        assertEquals("reason1", controller.getSignal().getReason()); // First reason retained
    }

    @Test
    @DisplayName("AbortControllerUtils createChildAbortController parent already aborted")
    void childParentAlreadyAborted() {
        AbortControllerUtils.AbortController parent = AbortControllerUtils.createAbortController();
        parent.abort("parent reason");

        AbortControllerUtils.AbortController child = AbortControllerUtils.createChildAbortController(parent);
        assertTrue(child.getSignal().isAborted());
        assertEquals("parent reason", child.getSignal().getReason());
    }

    @Test
    @DisplayName("AbortControllerUtils createChildAbortController propagates abort")
    void childPropagatesAbort() {
        AbortControllerUtils.AbortController parent = AbortControllerUtils.createAbortController();
        AbortControllerUtils.AbortController child = AbortControllerUtils.createChildAbortController(parent);

        assertFalse(child.getSignal().isAborted());
        parent.abort("parent reason");
        assertTrue(child.getSignal().isAborted());
        assertEquals("parent reason", child.getSignal().getReason());
    }

    @Test
    @DisplayName("AbortControllerUtils child abort does not affect parent")
    void childAbortNoParentEffect() {
        AbortControllerUtils.AbortController parent = AbortControllerUtils.createAbortController();
        AbortControllerUtils.AbortController child = AbortControllerUtils.createChildAbortController(parent);

        child.abort("child reason");
        assertFalse(parent.getSignal().isAborted());
        assertTrue(child.getSignal().isAborted());
    }

    @Test
    @DisplayName("AbortControllerUtils combineAbortSignals any aborted")
    void combineSignalsAnyAborted() {
        AbortControllerUtils.AbortController ctrl1 = AbortControllerUtils.createAbortController();
        AbortControllerUtils.AbortController ctrl2 = AbortControllerUtils.createAbortController();
        ctrl1.abort("reason1");

        AbortControllerUtils.AbortSignal combined = AbortControllerUtils.combineAbortSignals(
            ctrl1.getSignal(), ctrl2.getSignal()
        );

        assertTrue(combined.isAborted());
        assertEquals("reason1", combined.getReason());
    }

    @Test
    @DisplayName("AbortControllerUtils combineAbortSignals propagates")
    void combineSignalsPropagates() {
        AbortControllerUtils.AbortController ctrl1 = AbortControllerUtils.createAbortController();
        AbortControllerUtils.AbortController ctrl2 = AbortControllerUtils.createAbortController();

        AbortControllerUtils.AbortSignal combined = AbortControllerUtils.combineAbortSignals(
            ctrl1.getSignal(), ctrl2.getSignal()
        );

        assertFalse(combined.isAborted());
        ctrl2.abort("reason2");
        assertTrue(combined.isAborted());
        assertEquals("reason2", combined.getReason());
    }

    @Test
    @DisplayName("AbortControllerUtils checkAborted throws when aborted")
    void checkAbortedThrows() {
        AbortControllerUtils.AbortController controller = AbortControllerUtils.createAbortController();
        controller.abort("abort reason");

        assertThrows(AbortControllerUtils.AbortException.class,
            () -> AbortControllerUtils.checkAborted(controller.getSignal()));
    }

    @Test
    @DisplayName("AbortControllerUtils checkAborted no throw when not aborted")
    void checkAbortedNoThrow() {
        AbortControllerUtils.AbortController controller = AbortControllerUtils.createAbortController();
        AbortControllerUtils.checkAborted(controller.getSignal()); // Should not throw
    }

    @Test
    @DisplayName("AbortControllerUtils checkAborted null signal no throw")
    void checkAbortedNullSignal() {
        AbortControllerUtils.checkAborted(null); // Should not throw
    }

    @Test
    @DisplayName("AbortControllerUtils AbortException contains reason")
    void abortExceptionReason() {
        AbortControllerUtils.AbortException ex = new AbortControllerUtils.AbortException("test reason");
        assertEquals("test reason", ex.getReason());
        assertEquals("Operation was aborted", ex.getMessage());
    }

    @Test
    @DisplayName("AbortControllerUtils AbortException with throwable reason")
    void abortExceptionThrowable() {
        RuntimeException cause = new RuntimeException("cause");
        AbortControllerUtils.AbortException ex = new AbortControllerUtils.AbortException(cause);
        assertEquals(cause, ex.getReason());
        assertEquals(cause, ex.getCause());
    }

    @Test
    @DisplayName("AbortControllerUtils AbortSignalImpl is sealed")
    void abortSignalImplSealed() {
        // AbortSignalImpl is the only permitted implementation of AbortSignal
        AbortControllerUtils.AbortSignal signal = AbortControllerUtils.createAbortController().getSignal();
        assertTrue(signal instanceof AbortControllerUtils.AbortSignalImpl);
    }

    @Test
    @DisplayName("AbortControllerUtils AbortListener functional interface")
    void abortListenerFunctional() {
        AtomicInteger counter = new AtomicInteger(0);
        AbortControllerUtils.AbortListener listener = reason -> counter.incrementAndGet();
        listener.onAbort("test");
        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("AbortControllerUtils listener exception ignored")
    void listenerExceptionIgnored() {
        AbortControllerUtils.AbortController controller = AbortControllerUtils.createAbortController();
        controller.getSignal().addListener(reason -> {
            throw new RuntimeException("listener error");
        });
        // Should not throw when aborting
        controller.abort("reason");
        assertTrue(controller.getSignal().isAborted());
    }
}