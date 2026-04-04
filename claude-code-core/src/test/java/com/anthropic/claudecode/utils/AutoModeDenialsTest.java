/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AutoModeDenials.
 */
class AutoModeDenialsTest {

    @BeforeEach
    void setUp() {
        AutoModeDenials.clearDenials();
    }

    @Test
    @DisplayName("AutoModeDenials AutoModeDenial record")
    void autoModeDenial() {
        AutoModeDenials.AutoModeDenial denial = new AutoModeDenials.AutoModeDenial(
            "Bash", "Run command", "Dangerous", System.currentTimeMillis()
        );
        assertEquals("Bash", denial.toolName());
        assertEquals("Run command", denial.display());
        assertEquals("Dangerous", denial.reason());
    }

    @Test
    @DisplayName("AutoModeDenials getAutoModeDenials initially empty")
    void getAutoModeDenialsInitiallyEmpty() {
        assertTrue(AutoModeDenials.getAutoModeDenials().isEmpty());
    }

    @Test
    @DisplayName("AutoModeDenials getDenialCount initially zero")
    void getDenialCountInitiallyZero() {
        assertEquals(0, AutoModeDenials.getDenialCount());
    }

    @Test
    @DisplayName("AutoModeDenials clearDenials clears list")
    void clearDenials() {
        AutoModeDenials.clearDenials();
        assertEquals(0, AutoModeDenials.getDenialCount());
    }

    @Test
    @DisplayName("AutoModeDenials getAutoModeDenials returns unmodifiable list")
    void getAutoModeDenialsUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () ->
            AutoModeDenials.getAutoModeDenials().add(
                new AutoModeDenials.AutoModeDenial("test", "test", "test", 0)
            )
        );
    }
}
