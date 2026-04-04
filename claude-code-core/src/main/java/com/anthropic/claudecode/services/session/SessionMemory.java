/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/SessionMemory/sessionMemory.ts
 */
package com.anthropic.claudecode.services.session;

import java.util.*;
import java.util.concurrent.*;

/**
 * Session memory service for maintaining conversation notes.
 */
public final class SessionMemory {
    private SessionMemory() {}

    // Configuration
    private static volatile SessionMemoryConfig config = new SessionMemoryConfig();
    private static volatile boolean initialized = false;
    private static volatile boolean extractionInProgress = false;
    private static volatile String lastMemoryMessageUuid = null;
    private static volatile int lastExtractionTokenCount = 0;

    /**
     * Session memory configuration.
     */
    public record SessionMemoryConfig(
        int minimumMessageTokensToInit,
        int minimumTokensBetweenUpdate,
        int toolCallsBetweenUpdates
    ) {
        public SessionMemoryConfig() {
            this(10000, 5000, 10); // defaults
        }
    }

    /**
     * Check if session memory is enabled.
     */
    public static boolean isEnabled() {
        // Would check feature flag
        return true;
    }

    /**
     * Check if initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Mark as initialized.
     */
    public static void markInitialized() {
        initialized = true;
    }

    /**
     * Get current config.
     */
    public static SessionMemoryConfig getConfig() {
        return config;
    }

    /**
     * Set config.
     */
    public static void setConfig(SessionMemoryConfig newConfig) {
        config = newConfig;
    }

    /**
     * Check if extraction is in progress.
     */
    public static boolean isExtractionInProgress() {
        return extractionInProgress;
    }

    /**
     * Mark extraction started.
     */
    public static void markExtractionStarted() {
        extractionInProgress = true;
    }

    /**
     * Mark extraction completed.
     */
    public static void markExtractionCompleted() {
        extractionInProgress = false;
    }

    /**
     * Check if should extract memory based on messages.
     */
    public static boolean shouldExtractMemory(List<?> messages, int currentTokenCount) {
        // Check initialization threshold
        if (!initialized) {
            if (currentTokenCount < config.minimumMessageTokensToInit()) {
                return false;
            }
            initialized = true;
        }

        // Check if we've met token threshold since last extraction
        int tokensSinceLastExtraction = currentTokenCount - lastExtractionTokenCount;
        boolean hasMetTokenThreshold =
            tokensSinceLastExtraction >= config.minimumTokensBetweenUpdate();

        // In real implementation, would also check:
        // - Tool call count threshold
        // - Last assistant turn has no tool calls

        return hasMetTokenThreshold;
    }

    /**
     * Record extraction token count.
     */
    public static void recordExtractionTokenCount(int tokenCount) {
        lastExtractionTokenCount = tokenCount;
    }

    /**
     * Get session memory file path.
     */
    public static String getSessionMemoryPath() {
        String home = System.getProperty("user.home");
        return home + "/.claude/session_memory.md";
    }

    /**
     * Get session memory directory.
     */
    public static String getSessionMemoryDir() {
        String home = System.getProperty("user.home");
        return home + "/.claude";
    }

    /**
     * Reset state (for testing).
     */
    public static void reset() {
        initialized = false;
        extractionInProgress = false;
        lastMemoryMessageUuid = null;
        lastExtractionTokenCount = 0;
        config = new SessionMemoryConfig();
    }
}