/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.sessionmemory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SessionMemoryUtils.
 */
class SessionMemoryUtilsTest {

    @BeforeEach
    void setUp() {
        SessionMemoryUtils.resetSessionMemoryState();
    }

    @Test
    @DisplayName("SessionMemoryUtils SessionMemoryConfig defaults")
    void sessionMemoryConfigDefaults() {
        SessionMemoryUtils.SessionMemoryConfig config = SessionMemoryUtils.SessionMemoryConfig.defaults();

        assertEquals(10000, config.minimumMessageTokensToInit());
        assertEquals(5000, config.minimumTokensBetweenUpdate());
        assertEquals(3, config.toolCallsBetweenUpdates());
    }

    @Test
    @DisplayName("SessionMemoryUtils SessionMemoryConfig isValid")
    void sessionMemoryConfigIsValid() {
        SessionMemoryUtils.SessionMemoryConfig valid = new SessionMemoryUtils.SessionMemoryConfig(100, 50, 2);
        assertTrue(valid.isValid());

        SessionMemoryUtils.SessionMemoryConfig invalid1 = new SessionMemoryUtils.SessionMemoryConfig(0, 50, 2);
        assertFalse(invalid1.isValid());

        SessionMemoryUtils.SessionMemoryConfig invalid2 = new SessionMemoryUtils.SessionMemoryConfig(100, 0, 2);
        assertFalse(invalid2.isValid());

        SessionMemoryUtils.SessionMemoryConfig invalid3 = new SessionMemoryUtils.SessionMemoryConfig(100, 50, 0);
        assertFalse(invalid3.isValid());
    }

    @Test
    @DisplayName("SessionMemoryUtils getLastSummarizedMessageId returns null initially")
    void getLastSummarizedMessageIdInitial() {
        assertNull(SessionMemoryUtils.getLastSummarizedMessageId());
    }

    @Test
    @DisplayName("SessionMemoryUtils setLastSummarizedMessageId")
    void setLastSummarizedMessageId() {
        SessionMemoryUtils.setLastSummarizedMessageId("msg-123");

        assertEquals("msg-123", SessionMemoryUtils.getLastSummarizedMessageId());
    }

    @Test
    @DisplayName("SessionMemoryUtils markExtractionStarted and in progress")
    void markExtractionStarted() {
        SessionMemoryUtils.markExtractionStarted();

        assertTrue(SessionMemoryUtils.isExtractionInProgress());
        assertTrue(SessionMemoryUtils.getExtractionAge() >= 0);
    }

    @Test
    @DisplayName("SessionMemoryUtils markExtractionCompleted")
    void markExtractionCompleted() {
        SessionMemoryUtils.markExtractionStarted();
        SessionMemoryUtils.markExtractionCompleted();

        assertFalse(SessionMemoryUtils.isExtractionInProgress());
        assertEquals(0, SessionMemoryUtils.getExtractionAge());
    }

    @Test
    @DisplayName("SessionMemoryUtils getSessionMemoryConfig returns defaults initially")
    void getSessionMemoryConfigInitial() {
        SessionMemoryUtils.SessionMemoryConfig config = SessionMemoryUtils.getSessionMemoryConfig();

        assertEquals(SessionMemoryUtils.SessionMemoryConfig.defaults(), config);
    }

    @Test
    @DisplayName("SessionMemoryUtils setSessionMemoryConfig")
    void setSessionMemoryConfig() {
        SessionMemoryUtils.SessionMemoryConfig newConfig = new SessionMemoryUtils.SessionMemoryConfig(5000, 2000, 5);
        SessionMemoryUtils.setSessionMemoryConfig(newConfig);

        SessionMemoryUtils.SessionMemoryConfig config = SessionMemoryUtils.getSessionMemoryConfig();
        assertEquals(5000, config.minimumMessageTokensToInit());
        assertEquals(2000, config.minimumTokensBetweenUpdate());
        assertEquals(5, config.toolCallsBetweenUpdates());
    }

    @Test
    @DisplayName("SessionMemoryUtils isSessionMemoryInitialized returns false initially")
    void isSessionMemoryInitializedInitial() {
        assertFalse(SessionMemoryUtils.isSessionMemoryInitialized());
    }

    @Test
    @DisplayName("SessionMemoryUtils markSessionMemoryInitialized")
    void markSessionMemoryInitialized() {
        SessionMemoryUtils.markSessionMemoryInitialized();

        assertTrue(SessionMemoryUtils.isSessionMemoryInitialized());
    }

    @Test
    @DisplayName("SessionMemoryUtils getTokensAtLastExtraction returns zero initially")
    void getTokensAtLastExtractionInitial() {
        assertEquals(0, SessionMemoryUtils.getTokensAtLastExtraction());
    }

    @Test
    @DisplayName("SessionMemoryUtils recordExtractionTokenCount")
    void recordExtractionTokenCount() {
        SessionMemoryUtils.recordExtractionTokenCount(5000);

        assertEquals(5000, SessionMemoryUtils.getTokensAtLastExtraction());
    }

    @Test
    @DisplayName("SessionMemoryUtils hasMetInitializationThreshold")
    void hasMetInitializationThreshold() {
        assertFalse(SessionMemoryUtils.hasMetInitializationThreshold(5000));
        assertTrue(SessionMemoryUtils.hasMetInitializationThreshold(10000));
        assertTrue(SessionMemoryUtils.hasMetInitializationThreshold(15000));
    }

    @Test
    @DisplayName("SessionMemoryUtils hasMetUpdateThreshold")
    void hasMetUpdateThreshold() {
        SessionMemoryUtils.recordExtractionTokenCount(10000);

        assertFalse(SessionMemoryUtils.hasMetUpdateThreshold(12000));
        assertTrue(SessionMemoryUtils.hasMetUpdateThreshold(15000));
    }

    @Test
    @DisplayName("SessionMemoryUtils getToolCallsBetweenUpdates")
    void getToolCallsBetweenUpdates() {
        assertEquals(3, SessionMemoryUtils.getToolCallsBetweenUpdates());
    }

    @Test
    @DisplayName("SessionMemoryUtils resetSessionMemoryState clears all")
    void resetSessionMemoryState() {
        SessionMemoryUtils.setLastSummarizedMessageId("msg-123");
        SessionMemoryUtils.markSessionMemoryInitialized();
        SessionMemoryUtils.recordExtractionTokenCount(5000);
        SessionMemoryUtils.markExtractionStarted();

        SessionMemoryUtils.resetSessionMemoryState();

        assertNull(SessionMemoryUtils.getLastSummarizedMessageId());
        assertFalse(SessionMemoryUtils.isSessionMemoryInitialized());
        assertEquals(0, SessionMemoryUtils.getTokensAtLastExtraction());
        assertFalse(SessionMemoryUtils.isExtractionInProgress());
        assertEquals(SessionMemoryUtils.SessionMemoryConfig.defaults(), SessionMemoryUtils.getSessionMemoryConfig());
    }

    @Test
    @DisplayName("SessionMemoryUtils getSessionMemoryContent returns null")
    void getSessionMemoryContent() {
        assertNull(SessionMemoryUtils.getSessionMemoryContent());
    }

    @Test
    @DisplayName("SessionMemoryUtils waitForSessionMemoryExtraction does not block when not in progress")
    void waitForSessionMemoryExtractionNoBlock() throws InterruptedException {
        SessionMemoryUtils.markExtractionCompleted();

        SessionMemoryUtils.waitForSessionMemoryExtraction();

        // Should complete quickly without blocking
    }
}