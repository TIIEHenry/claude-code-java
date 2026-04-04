/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Advisor.
 */
class AdvisorTest {

    private Advisor advisor;

    @BeforeEach
    void setUp() {
        advisor = new Advisor();
    }

    @Test
    @DisplayName("Advisor AdvisoryLevel enum values")
    void advisoryLevelEnum() {
        assertEquals(5, Advisor.AdvisoryLevel.values().length);
        assertEquals(Advisor.AdvisoryLevel.DEBUG, Advisor.AdvisoryLevel.values()[0]);
        assertEquals(Advisor.AdvisoryLevel.INFO, Advisor.AdvisoryLevel.values()[1]);
        assertEquals(Advisor.AdvisoryLevel.WARNING, Advisor.AdvisoryLevel.values()[2]);
        assertEquals(Advisor.AdvisoryLevel.ERROR, Advisor.AdvisoryLevel.values()[3]);
        assertEquals(Advisor.AdvisoryLevel.CRITICAL, Advisor.AdvisoryLevel.values()[4]);
    }

    @Test
    @DisplayName("Advisor AdvisoryMessage record")
    void advisoryMessageRecord() {
        Advisor.AdvisoryMessage msg = new Advisor.AdvisoryMessage(
            Advisor.AdvisoryLevel.WARNING, "Test message", 12345L
        );

        assertEquals(Advisor.AdvisoryLevel.WARNING, msg.level());
        assertEquals("Test message", msg.message());
        assertEquals(12345L, msg.timestamp());
    }

    @Test
    @DisplayName("Advisor AdvisoryMessage format")
    void advisoryMessageFormat() {
        Advisor.AdvisoryMessage msg = new Advisor.AdvisoryMessage(
            Advisor.AdvisoryLevel.ERROR, "Error occurred", 1000L
        );

        assertEquals("[ERROR] Error occurred", msg.format());
    }

    @Test
    @DisplayName("Advisor info adds message")
    void infoAddsMessage() {
        advisor.info("Info message");
        assertEquals(1, advisor.getMessages().size());
        assertEquals(Advisor.AdvisoryLevel.INFO, advisor.getMessages().get(0).level());
    }

    @Test
    @DisplayName("Advisor warning adds message")
    void warningAddsMessage() {
        advisor.warning("Warning message");
        assertEquals(1, advisor.getMessages().size());
        assertEquals(Advisor.AdvisoryLevel.WARNING, advisor.getMessages().get(0).level());
    }

    @Test
    @DisplayName("Advisor error adds message")
    void errorAddsMessage() {
        advisor.error("Error message");
        assertEquals(1, advisor.getMessages().size());
        assertEquals(Advisor.AdvisoryLevel.ERROR, advisor.getMessages().get(0).level());
    }

    @Test
    @DisplayName("Advisor multiple messages")
    void multipleMessages() {
        advisor.info("Info");
        advisor.warning("Warning");
        advisor.error("Error");

        assertEquals(3, advisor.getMessages().size());
    }

    @Test
    @DisplayName("Advisor getMessagesByLevel")
    void getMessagesByLevel() {
        advisor.info("Info1");
        advisor.info("Info2");
        advisor.warning("Warning");

        assertEquals(2, advisor.getMessagesByLevel(Advisor.AdvisoryLevel.INFO).size());
        assertEquals(1, advisor.getMessagesByLevel(Advisor.AdvisoryLevel.WARNING).size());
        assertEquals(0, advisor.getMessagesByLevel(Advisor.AdvisoryLevel.ERROR).size());
    }

    @Test
    @DisplayName("Advisor clear removes messages")
    void clearRemovesMessages() {
        advisor.info("Info");
        advisor.warning("Warning");
        advisor.clear();

        assertEquals(0, advisor.getMessages().size());
    }

    @Test
    @DisplayName("Advisor hasErrors")
    void hasErrors() {
        assertFalse(advisor.hasErrors());
        advisor.info("Info");
        assertFalse(advisor.hasErrors());
        advisor.error("Error");
        assertTrue(advisor.hasErrors());
    }

    @Test
    @DisplayName("Advisor hasWarnings")
    void hasWarnings() {
        assertFalse(advisor.hasWarnings());
        advisor.info("Info");
        assertFalse(advisor.hasWarnings());
        advisor.warning("Warning");
        assertTrue(advisor.hasWarnings());
    }

    @Test
    @DisplayName("Advisor getSummary")
    void getSummary() {
        advisor.info("Info1");
        advisor.info("Info2");
        advisor.warning("Warning");
        advisor.error("Error");

        String summary = advisor.getSummary();
        assertTrue(summary.contains("Errors: 1"));
        assertTrue(summary.contains("Warnings: 1"));
        assertTrue(summary.contains("Info: 2"));
    }

    @Test
    @DisplayName("Advisor getSummary empty")
    void getSummaryEmpty() {
        String summary = advisor.getSummary();
        assertEquals("Errors: 0, Warnings: 0, Info: 0", summary);
    }

    @Test
    @DisplayName("Advisor getMessages returns unmodifiable list")
    void getMessagesUnmodifiable() {
        advisor.info("Info");
        var messages = advisor.getMessages();

        assertThrows(UnsupportedOperationException.class, () -> messages.add(
            new Advisor.AdvisoryMessage(Advisor.AdvisoryLevel.INFO, "test", 0L)
        ));
    }
}