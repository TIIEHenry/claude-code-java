/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FirstTokenDate.
 */
class FirstTokenDateTest {

    @BeforeEach
    void setUp() {
        FirstTokenDate.clear();
    }

    @Test
    @DisplayName("FirstTokenDate get returns null initially")
    void getInitial() {
        assertNull(FirstTokenDate.get());
    }

    @Test
    @DisplayName("FirstTokenDate set and get")
    void setAndGet() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        FirstTokenDate.set(date);

        assertEquals(date, FirstTokenDate.get());
    }

    @Test
    @DisplayName("FirstTokenDate setFromTimestamp")
    void setFromTimestamp() {
        // 2024-01-15 00:00:00 UTC
        long timestamp = 1705276800000L;
        FirstTokenDate.setFromTimestamp(timestamp);

        LocalDate date = FirstTokenDate.get();
        assertNotNull(date);
        // Date will depend on timezone, but should be around 2024-01-15
        assertEquals(2024, date.getYear());
    }

    @Test
    @DisplayName("FirstTokenDate clear")
    void clear() {
        FirstTokenDate.set(LocalDate.now());
        assertTrue(FirstTokenDate.isSet());

        FirstTokenDate.clear();

        assertNull(FirstTokenDate.get());
        assertFalse(FirstTokenDate.isSet());
    }

    @Test
    @DisplayName("FirstTokenDate isSet returns false initially")
    void isSetInitial() {
        assertFalse(FirstTokenDate.isSet());
    }

    @Test
    @DisplayName("FirstTokenDate isSet returns true after set")
    void isSetAfterSet() {
        FirstTokenDate.set(LocalDate.now());
        assertTrue(FirstTokenDate.isSet());
    }

    @Test
    @DisplayName("FirstTokenDate toISOString returns null when not set")
    void toISOStringNull() {
        assertNull(FirstTokenDate.toISOString());
    }

    @Test
    @DisplayName("FirstTokenDate toISOString returns ISO format")
    void toISOString() {
        LocalDate date = LocalDate.of(2024, 6, 15);
        FirstTokenDate.set(date);

        assertEquals("2024-06-15", FirstTokenDate.toISOString());
    }

    @Test
    @DisplayName("FirstTokenDate fromISOString parses correctly")
    void fromISOString() {
        FirstTokenDate.fromISOString("2024-03-20");

        LocalDate date = FirstTokenDate.get();
        assertNotNull(date);
        assertEquals(2024, date.getYear());
        assertEquals(3, date.getMonthValue());
        assertEquals(20, date.getDayOfMonth());
    }

    @Test
    @DisplayName("FirstTokenDate fromISOString handles null")
    void fromISOStringNull() {
        FirstTokenDate.set(LocalDate.now());
        FirstTokenDate.fromISOString(null);

        // Should remain unchanged or be null
        // Implementation clears it if null passed
    }

    @Test
    @DisplayName("FirstTokenDate fromISOString handles empty")
    void fromISOStringEmpty() {
        FirstTokenDate.set(LocalDate.now());
        FirstTokenDate.fromISOString("");

        // Should remain unchanged or be null
    }

    @Test
    @DisplayName("FirstTokenDate fromISOString handles invalid format")
    void fromISOStringInvalid() {
        FirstTokenDate.set(LocalDate.now());
        FirstTokenDate.fromISOString("not-a-date");

        // Should not crash, date should remain unchanged or null
        // Implementation ignores parse errors
    }
}