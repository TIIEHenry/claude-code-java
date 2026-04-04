/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EarlyInput.
 */
class EarlyInputTest {

    @BeforeEach
    void setUp() {
        EarlyInput.clearEarlyInput();
        if (EarlyInput.isCapturingEarlyInput()) {
            EarlyInput.stopCapturingEarlyInput();
        }
    }

    @Test
    @DisplayName("EarlyInput clearEarlyInput")
    void clearEarlyInput() {
        EarlyInput.seedEarlyInput("test");
        EarlyInput.clearEarlyInput();
        assertEquals("", EarlyInput.peekEarlyInput());
    }

    @Test
    @DisplayName("EarlyInput seedEarlyInput")
    void seedEarlyInput() {
        EarlyInput.seedEarlyInput("hello world");
        assertEquals("hello world", EarlyInput.peekEarlyInput());
    }

    @Test
    @DisplayName("EarlyInput peekEarlyInput")
    void peekEarlyInput() {
        EarlyInput.seedEarlyInput("test input");
        String peeked = EarlyInput.peekEarlyInput();
        assertEquals("test input", peeked);
        // Peek should not consume
        assertEquals("test input", EarlyInput.peekEarlyInput());
    }

    @Test
    @DisplayName("EarlyInput hasEarlyInput true")
    void hasEarlyInputTrue() {
        EarlyInput.seedEarlyInput("test");
        assertTrue(EarlyInput.hasEarlyInput());
    }

    @Test
    @DisplayName("EarlyInput hasEarlyInput false")
    void hasEarlyInputFalse() {
        EarlyInput.clearEarlyInput();
        assertFalse(EarlyInput.hasEarlyInput());
    }

    @Test
    @DisplayName("EarlyInput hasEarlyInput whitespace only")
    void hasEarlyInputWhitespace() {
        EarlyInput.seedEarlyInput("   ");
        assertFalse(EarlyInput.hasEarlyInput());
    }

    @Test
    @DisplayName("EarlyInput consumeEarlyInput")
    void consumeEarlyInput() {
        EarlyInput.seedEarlyInput("test input");
        String consumed = EarlyInput.consumeEarlyInput();
        assertEquals("test input", consumed);
        // Should be consumed
        assertFalse(EarlyInput.hasEarlyInput());
    }

    @Test
    @DisplayName("EarlyInput consumeEarlyInput trims")
    void consumeEarlyInputTrims() {
        EarlyInput.seedEarlyInput("  test  ");
        String consumed = EarlyInput.consumeEarlyInput();
        assertEquals("test", consumed);
    }

    @Test
    @DisplayName("EarlyInput isCapturingEarlyInput initially false")
    void isCapturingEarlyInputInitiallyFalse() {
        assertFalse(EarlyInput.isCapturingEarlyInput());
    }

    @Test
    @DisplayName("EarlyInput stopCapturingEarlyInput when not capturing")
    void stopCapturingEarlyInputWhenNotCapturing() {
        // Should not throw
        assertDoesNotThrow(() -> EarlyInput.stopCapturingEarlyInput());
    }
}