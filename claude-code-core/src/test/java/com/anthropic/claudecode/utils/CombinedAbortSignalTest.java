/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CombinedAbortSignal.
 */
class CombinedAbortSignalTest {

    @Test
    @DisplayName("CombinedAbortSignal createCombinedAbortSignal with timeout")
    void createWithTimeout() {
        CombinedAbortSignal.Result result = CombinedAbortSignal.createCombinedAbortSignal(1000);

        assertNotNull(result);
        assertNotNull(result.signal());
        assertNotNull(result.cleanup());
        assertFalse(result.signal().isAborted());
    }

    @Test
    @DisplayName("CombinedAbortSignal createCombinedAbortSignal with parent signal")
    void createWithParentSignal() {
        AbortControllerUtil.AbortController parent = AbortControllerUtil.createAbortController();
        CombinedAbortSignal.Result result = CombinedAbortSignal.createCombinedAbortSignal(parent);

        assertNotNull(result.signal());
        assertFalse(result.signal().isAborted());
    }

    @Test
    @DisplayName("CombinedAbortSignal aborts when parent aborts")
    void abortsOnParentAbort() {
        AbortControllerUtil.AbortController parent = AbortControllerUtil.createAbortController();
        CombinedAbortSignal.Result result = CombinedAbortSignal.createCombinedAbortSignal(parent);

        parent.abort("parent aborted");

        assertTrue(result.signal().isAborted());
        assertEquals("parent aborted", result.signal().getReason());
    }

    @Test
    @DisplayName("CombinedAbortSignal returns already aborted if parent is aborted")
    void alreadyAbortedParent() {
        AbortControllerUtil.AbortController parent = AbortControllerUtil.createAbortController();
        parent.abort("already aborted");

        CombinedAbortSignal.Result result = CombinedAbortSignal.createCombinedAbortSignal(parent);

        assertTrue(result.signal().isAborted());
        assertEquals("already aborted", result.signal().getReason());
    }

    @Test
    @DisplayName("CombinedAbortSignal cleanup prevents further aborts")
    void cleanupPreventsAborts() {
        AbortControllerUtil.AbortController parent = AbortControllerUtil.createAbortController();
        CombinedAbortSignal.Result result = CombinedAbortSignal.createCombinedAbortSignal(parent);

        result.cleanup().run();
        parent.abort("should not propagate");

        assertFalse(result.signal().isAborted());
    }

    @Test
    @DisplayName("CombinedAbortSignal with signalB option")
    void withSignalB() {
        AbortControllerUtil.AbortController signalA = AbortControllerUtil.createAbortController();
        AbortControllerUtil.AbortController signalB = AbortControllerUtil.createAbortController();
        CombinedAbortSignal.Options opts = new CombinedAbortSignal.Options(signalB, null);

        CombinedAbortSignal.Result result = CombinedAbortSignal.createCombinedAbortSignal(signalA, opts);

        signalB.abort("signalB aborted");

        assertTrue(result.signal().isAborted());
    }

    @Test
    @DisplayName("CombinedAbortSignal with signal and timeout")
    void withSignalAndTimeout() {
        AbortControllerUtil.AbortController signal = AbortControllerUtil.createAbortController();

        CombinedAbortSignal.Result result = CombinedAbortSignal.createCombinedAbortSignal(signal, 1000);

        assertNotNull(result.signal());
        assertFalse(result.signal().isAborted());
    }

    @Test
    @DisplayName("CombinedAbortSignal Options record works")
    void optionsRecord() {
        AbortControllerUtil.AbortController signalB = AbortControllerUtil.createAbortController();
        CombinedAbortSignal.Options opts = new CombinedAbortSignal.Options(signalB, 5000);

        assertEquals(signalB, opts.signalB());
        assertEquals(5000, opts.timeoutMs());
    }

    @Test
    @DisplayName("CombinedAbortSignal Result record works")
    void resultRecord() {
        AbortControllerUtil.AbortController controller = AbortControllerUtil.createAbortController();
        Runnable cleanup = () -> {};

        CombinedAbortSignal.Result result = new CombinedAbortSignal.Result(controller, cleanup);

        assertEquals(controller, result.signal());
        assertEquals(cleanup, result.cleanup());
    }

    @Test
    @DisplayName("CombinedAbortSignal handles null signal")
    void nullSignal() {
        CombinedAbortSignal.Result result = CombinedAbortSignal.createCombinedAbortSignal(null);

        assertNotNull(result.signal());
        assertFalse(result.signal().isAborted());
    }

    @Test
    @DisplayName("CombinedAbortSignal handles null options")
    void nullOptions() {
        AbortControllerUtil.AbortController signal = AbortControllerUtil.createAbortController();

        CombinedAbortSignal.Result result = CombinedAbortSignal.createCombinedAbortSignal(signal, null);

        assertNotNull(result.signal());
    }
}