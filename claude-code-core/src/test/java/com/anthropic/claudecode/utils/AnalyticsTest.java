/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Analytics.
 */
class AnalyticsTest {

    @BeforeEach
    void setUp() {
        Analytics.clear();
        Analytics.setEnabled(true);
    }

    @Test
    @DisplayName("Analytics logEvent adds event")
    void logEvent() {
        Analytics.logEvent("test_event");
        assertEquals(1, Analytics.getQueuedEventCount());
    }

    @Test
    @DisplayName("Analytics logEvent with metadata")
    void logEventWithMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", 42);
        
        Analytics.logEvent("test_event", metadata);
        assertEquals(1, Analytics.getQueuedEventCount());
    }

    @Test
    @DisplayName("Analytics logEvent when disabled does nothing")
    void logEventDisabled() {
        Analytics.setEnabled(false);
        Analytics.logEvent("test_event");
        assertEquals(0, Analytics.getQueuedEventCount());
    }

    @Test
    @DisplayName("Analytics setEnabled false")
    void setEnabledFalse() {
        Analytics.setEnabled(false);
        assertFalse(Analytics.isEnabled());
    }

    @Test
    @DisplayName("Analytics setEnabled true")
    void setEnabledTrue() {
        Analytics.setEnabled(true);
        assertTrue(Analytics.isEnabled());
    }

    @Test
    @DisplayName("Analytics isEnabled default true")
    void isEnabledDefault() {
        assertTrue(Analytics.isEnabled());
    }

    @Test
    @DisplayName("Analytics flush clears queue")
    void flush() {
        Analytics.logEvent("test1");
        Analytics.logEvent("test2");
        Analytics.flush();
        assertEquals(0, Analytics.getQueuedEventCount());
    }

    @Test
    @DisplayName("Analytics flush empty queue does nothing")
    void flushEmpty() {
        Analytics.flush();
        assertEquals(0, Analytics.getQueuedEventCount());
    }

    @Test
    @DisplayName("Analytics getQueuedEventCount")
    void getQueuedEventCount() {
        assertEquals(0, Analytics.getQueuedEventCount());
        Analytics.logEvent("test1");
        assertEquals(1, Analytics.getQueuedEventCount());
        Analytics.logEvent("test2");
        assertEquals(2, Analytics.getQueuedEventCount());
    }

    @Test
    @DisplayName("Analytics clear removes all events")
    void clear() {
        Analytics.logEvent("test1");
        Analytics.logEvent("test2");
        Analytics.clear();
        assertEquals(0, Analytics.getQueuedEventCount());
    }

    @Test
    @DisplayName("Analytics auto-flush on large queue")
    void autoFlushOnLargeQueue() {
        // Add more than 100 events to trigger auto-flush
        for (int i = 0; i < 101; i++) {
            Analytics.logEvent("test_" + i);
        }
        // Queue should be cleared after auto-flush
        assertEquals(0, Analytics.getQueuedEventCount());
    }

    @Test
    @DisplayName("Analytics logEvent with null metadata")
    void logEventNullMetadata() {
        Analytics.logEvent("test_event", null);
        assertEquals(1, Analytics.getQueuedEventCount());
    }
}
