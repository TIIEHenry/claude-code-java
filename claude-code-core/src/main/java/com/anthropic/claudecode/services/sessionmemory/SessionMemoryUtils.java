/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/SessionMemory/sessionMemoryUtils
 */
package com.anthropic.claudecode.services.sessionmemory;

import java.util.concurrent.atomic.*;
import java.time.*;

/**
 * Session memory utils - Utility functions for session memory.
 *
 * These are separate from the main session memory to avoid circular dependencies.
 */
public final class SessionMemoryUtils {
    private static final int EXTRACTION_WAIT_TIMEOUT_MS = 15000;
    private static final int EXTRACTION_STALE_THRESHOLD_MS = 60000; // 1 minute

    // Default configuration
    private static final SessionMemoryConfig DEFAULT_CONFIG = new SessionMemoryConfig(
        10000,  // minimumMessageTokensToInit
        5000,   // minimumTokensBetweenUpdate
        3       // toolCallsBetweenUpdates
    );

    // Current config
    private static volatile SessionMemoryConfig config = DEFAULT_CONFIG;

    // State tracking
    private static final AtomicReference<String> lastSummarizedMessageId = new AtomicReference<>(null);
    private static final AtomicLong extractionStartedAt = new AtomicLong(0);
    private static final AtomicInteger tokensAtLastExtraction = new AtomicInteger(0);
    private static final AtomicBoolean sessionMemoryInitialized = new AtomicBoolean(false);

    /**
     * Session memory config record.
     */
    public record SessionMemoryConfig(
        int minimumMessageTokensToInit,
        int minimumTokensBetweenUpdate,
        int toolCallsBetweenUpdates
    ) {
        public static SessionMemoryConfig defaults() {
            return DEFAULT_CONFIG;
        }

        public boolean isValid() {
            return minimumMessageTokensToInit > 0 &&
                   minimumTokensBetweenUpdate > 0 &&
                   toolCallsBetweenUpdates > 0;
        }
    }

    /**
     * Get last summarized message ID.
     */
    public static String getLastSummarizedMessageId() {
        return lastSummarizedMessageId.get();
    }

    /**
     * Set last summarized message ID.
     */
    public static void setLastSummarizedMessageId(String messageId) {
        lastSummarizedMessageId.set(messageId);
    }

    /**
     * Mark extraction as started.
     */
    public static void markExtractionStarted() {
        extractionStartedAt.set(System.currentTimeMillis());
    }

    /**
     * Mark extraction as completed.
     */
    public static void markExtractionCompleted() {
        extractionStartedAt.set(0);
    }

    /**
     * Wait for session memory extraction to complete.
     */
    public static void waitForSessionMemoryExtraction() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (extractionStartedAt.get() > 0) {
            long extractionAge = System.currentTimeMillis() - extractionStartedAt.get();
            if (extractionAge > EXTRACTION_STALE_THRESHOLD_MS) {
                return; // Extraction is stale
            }

            if (System.currentTimeMillis() - startTime > EXTRACTION_WAIT_TIMEOUT_MS) {
                return; // Timeout
            }

            Thread.sleep(1000);
        }
    }

    /**
     * Get session memory content.
     */
    public static String getSessionMemoryContent() {
        // Would read from session memory file
        return null;
    }

    /**
     * Set session memory config.
     */
    public static void setSessionMemoryConfig(SessionMemoryConfig newConfig) {
        config = newConfig;
    }

    /**
     * Get session memory config.
     */
    public static SessionMemoryConfig getSessionMemoryConfig() {
        return config;
    }

    /**
     * Record extraction token count.
     */
    public static void recordExtractionTokenCount(int currentTokenCount) {
        tokensAtLastExtraction.set(currentTokenCount);
    }

    /**
     * Check if session memory initialized.
     */
    public static boolean isSessionMemoryInitialized() {
        return sessionMemoryInitialized.get();
    }

    /**
     * Mark session memory initialized.
     */
    public static void markSessionMemoryInitialized() {
        sessionMemoryInitialized.set(true);
    }

    /**
     * Check if met initialization threshold.
     */
    public static boolean hasMetInitializationThreshold(int currentTokenCount) {
        return currentTokenCount >= config.minimumMessageTokensToInit();
    }

    /**
     * Check if met update threshold.
     */
    public static boolean hasMetUpdateThreshold(int currentTokenCount) {
        int tokensSinceLastExtraction = currentTokenCount - tokensAtLastExtraction.get();
        return tokensSinceLastExtraction >= config.minimumTokensBetweenUpdate();
    }

    /**
     * Get tool calls between updates.
     */
    public static int getToolCallsBetweenUpdates() {
        return config.toolCallsBetweenUpdates();
    }

    /**
     * Reset session memory state.
     */
    public static void resetSessionMemoryState() {
        config = DEFAULT_CONFIG;
        tokensAtLastExtraction.set(0);
        sessionMemoryInitialized.set(false);
        lastSummarizedMessageId.set(null);
        extractionStartedAt.set(0);
    }

    /**
     * Check if extraction is in progress.
     */
    public static boolean isExtractionInProgress() {
        return extractionStartedAt.get() > 0;
    }

    /**
     * Get extraction age in milliseconds.
     */
    public static long getExtractionAge() {
        long started = extractionStartedAt.get();
        return started > 0 ? System.currentTimeMillis() - started : 0;
    }

    /**
     * Get tokens at last extraction.
     */
    public static int getTokensAtLastExtraction() {
        return tokensAtLastExtraction.get();
    }
}