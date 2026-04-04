/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.diagnostics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DiagnosticTrackingService.
 */
class DiagnosticTrackingServiceTest {

    private DiagnosticTrackingService service;

    @BeforeEach
    void setUp() {
        service = new DiagnosticTrackingService();
    }

    @Test
    @DisplayName("DiagnosticTrackingService DiagnosticSeverity enum values")
    void diagnosticSeverityEnum() {
        DiagnosticTrackingService.DiagnosticSeverity[] severities = DiagnosticTrackingService.DiagnosticSeverity.values();
        assertEquals(5, severities.length);
        assertEquals(0, DiagnosticTrackingService.DiagnosticSeverity.DEBUG.getLevel());
        assertEquals(1, DiagnosticTrackingService.DiagnosticSeverity.INFO.getLevel());
        assertEquals(2, DiagnosticTrackingService.DiagnosticSeverity.WARNING.getLevel());
        assertEquals(3, DiagnosticTrackingService.DiagnosticSeverity.ERROR.getLevel());
        assertEquals(4, DiagnosticTrackingService.DiagnosticSeverity.CRITICAL.getLevel());
    }

    @Test
    @DisplayName("DiagnosticTrackingService DiagnosticEvent of factory")
    void diagnosticEventOf() {
        DiagnosticTrackingService.DiagnosticEvent event = DiagnosticTrackingService.DiagnosticEvent.of("test-type", "Test message");

        assertNotNull(event.id());
        assertEquals("test-type", event.type());
        assertEquals("Test message", event.message());
        assertEquals("general", event.category());
        assertEquals(DiagnosticTrackingService.DiagnosticSeverity.INFO, event.severity());
        assertEquals("system", event.source());
        assertNotNull(event.timestamp());
    }

    @Test
    @DisplayName("DiagnosticTrackingService DiagnosticEvent withData")
    void diagnosticEventWithData() {
        DiagnosticTrackingService.DiagnosticEvent event = DiagnosticTrackingService.DiagnosticEvent.of("test", "msg");
        event = event.withData("key1", "value1");
        event = event.withData("key2", 123);

        assertEquals("value1", event.data().get("key1"));
        assertEquals(123, event.data().get("key2"));
    }

    @Test
    @DisplayName("DiagnosticTrackingService DiagnosticEvent withSeverity")
    void diagnosticEventWithSeverity() {
        DiagnosticTrackingService.DiagnosticEvent event = DiagnosticTrackingService.DiagnosticEvent.of("test", "msg");
        event = event.withSeverity(DiagnosticTrackingService.DiagnosticSeverity.ERROR);

        assertEquals(DiagnosticTrackingService.DiagnosticSeverity.ERROR, event.severity());
    }

    @Test
    @DisplayName("DiagnosticTrackingService trackEvent adds event")
    void trackEvent() {
        service.trackEvent("test-type", "Test message");

        assertEquals(1, service.getAllEvents().size());
    }

    @Test
    @DisplayName("DiagnosticTrackingService trackEvent with data")
    void trackEventWithData() {
        service.trackEvent("test-type", "Test message", Map.of("key", "value"));

        var events = service.getAllEvents();
        assertEquals(1, events.size());
        assertEquals("value", events.get(0).data().get("key"));
    }

    @Test
    @DisplayName("DiagnosticTrackingService trackError adds error event")
    void trackError() {
        RuntimeException ex = new RuntimeException("Test error");
        service.trackError("error-type", "Error occurred", ex);

        var events = service.getAllEvents();
        assertEquals(1, events.size());
        assertEquals(DiagnosticTrackingService.DiagnosticSeverity.ERROR, events.get(0).severity());
        assertEquals(RuntimeException.class.getName(), events.get(0).data().get("exception"));
    }

    @Test
    @DisplayName("DiagnosticTrackingService getEvent returns event by ID")
    void getEvent() {
        service.trackEvent("test", "message");
        var allEvents = service.getAllEvents();
        String id = allEvents.get(0).id();

        var event = service.getEvent(id);
        assertNotNull(event);
        assertEquals("test", event.type());
    }

    @Test
    @DisplayName("DiagnosticTrackingService getEvent returns null for missing")
    void getEventMissing() {
        assertNull(service.getEvent("non-existent"));
    }

    @Test
    @DisplayName("DiagnosticTrackingService getEventsByType filters correctly")
    void getEventsByType() {
        service.trackEvent("type-a", "message 1");
        service.trackEvent("type-b", "message 2");
        service.trackEvent("type-a", "message 3");

        var typeAEvents = service.getEventsByType("type-a");
        assertEquals(2, typeAEvents.size());

        var typeBEvents = service.getEventsByType("type-b");
        assertEquals(1, typeBEvents.size());
    }

    @Test
    @DisplayName("DiagnosticTrackingService getEventsBySeverity filters correctly")
    void getEventsBySeverity() {
        service.trackError("error", "error message", new RuntimeException("test"));
        service.trackEvent("info", "info message");

        var errorEvents = service.getEventsBySeverity(DiagnosticTrackingService.DiagnosticSeverity.ERROR);
        assertEquals(1, errorEvents.size());

        var infoEvents = service.getEventsBySeverity(DiagnosticTrackingService.DiagnosticSeverity.INFO);
        assertEquals(1, infoEvents.size());
    }

    @Test
    @DisplayName("DiagnosticTrackingService clearEvents removes all")
    void clearEvents() {
        service.trackEvent("test1", "msg1");
        service.trackEvent("test2", "msg2");

        assertEquals(2, service.getAllEvents().size());

        service.clearEvents();

        assertTrue(service.getAllEvents().isEmpty());
    }

    @Test
    @DisplayName("DiagnosticTrackingService listener receives events")
    void listener() {
        StringBuilder sb = new StringBuilder();
        DiagnosticTrackingService.DiagnosticListener listener = event -> sb.append(event.message());

        service.addListener(listener);
        service.trackEvent("test", "Hello");

        assertEquals("Hello", sb.toString());
    }

    @Test
    @DisplayName("DiagnosticTrackingService removeListener stops notifications")
    void removeListener() {
        int[] count = {0};
        DiagnosticTrackingService.DiagnosticListener listener = event -> count[0]++;

        service.addListener(listener);
        service.trackEvent("test1", "msg1");
        assertEquals(1, count[0]);

        service.removeListener(listener);
        service.trackEvent("test2", "msg2");
        assertEquals(1, count[0]); // Still 1
    }

    @Test
    @DisplayName("DiagnosticTrackingService DiagnosticSummary record")
    void diagnosticSummaryRecord() {
        service.trackEvent("type1", "msg1");
        service.trackError("error", "err", new RuntimeException("test"));

        DiagnosticTrackingService.DiagnosticSummary summary = service.getSummary();

        assertEquals(2, summary.totalEvents());
        assertEquals(1, summary.countBySeverity().get(DiagnosticTrackingService.DiagnosticSeverity.INFO));
        assertEquals(1, summary.countBySeverity().get(DiagnosticTrackingService.DiagnosticSeverity.ERROR));
        assertNotNull(summary.firstEvent());
        assertNotNull(summary.lastEvent());
    }

    @Test
    @DisplayName("DiagnosticTrackingService DiagnosticSummary format")
    void diagnosticSummaryFormat() {
        service.trackEvent("test", "msg");
        service.trackError("err", "error", new RuntimeException("test"));

        DiagnosticTrackingService.DiagnosticSummary summary = service.getSummary();
        String formatted = summary.format();

        assertTrue(formatted.contains("Total: 2"));
        assertTrue(formatted.contains("INFO:"));
        assertTrue(formatted.contains("ERROR:"));
    }
}