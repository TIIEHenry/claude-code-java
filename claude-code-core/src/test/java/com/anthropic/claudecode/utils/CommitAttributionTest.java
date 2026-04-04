/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CommitAttribution.
 */
class CommitAttributionTest {

    @Test
    @DisplayName("CommitAttribution FileAttributionState record")
    void fileAttributionStateRecord() {
        CommitAttribution.FileAttributionState state = new CommitAttribution.FileAttributionState(
            "hash123", 100, 1234567890L
        );

        assertEquals("hash123", state.contentHash());
        assertEquals(100, state.claudeContribution());
        assertEquals(1234567890L, state.mtime());
    }

    @Test
    @DisplayName("CommitAttribution BaselineState record")
    void baselineStateRecord() {
        CommitAttribution.BaselineState state = new CommitAttribution.BaselineState(
            "hash456", 9876543210L
        );

        assertEquals("hash456", state.contentHash());
        assertEquals(9876543210L, state.mtime());
    }

    @Test
    @DisplayName("CommitAttribution AttributionSummary record")
    void attributionSummaryRecord() {
        CommitAttribution.AttributionSummary summary = new CommitAttribution.AttributionSummary(
            75, 3000, 1000, java.util.List.of("cli")
        );

        assertEquals(75, summary.claudePercent());
        assertEquals(3000, summary.claudeChars());
        assertEquals(1000, summary.humanChars());
        assertEquals(1, summary.surfaces().size());
    }

    @Test
    @DisplayName("CommitAttribution FileAttribution record")
    void fileAttributionRecord() {
        CommitAttribution.FileAttribution attr = new CommitAttribution.FileAttribution(
            500, 200, 71, "cli"
        );

        assertEquals(500, attr.claudeChars());
        assertEquals(200, attr.humanChars());
        assertEquals(71, attr.percent());
        assertEquals("cli", attr.surface());
    }

    @Test
    @DisplayName("CommitAttribution SurfaceBreakdown record")
    void surfaceBreakdownRecord() {
        CommitAttribution.SurfaceBreakdown breakdown = new CommitAttribution.SurfaceBreakdown(
            1000, 80
        );

        assertEquals(1000, breakdown.claudeChars());
        assertEquals(80, breakdown.percent());
    }

    @Test
    @DisplayName("CommitAttribution AttributionState createEmpty")
    void attributionStateCreateEmpty() {
        CommitAttribution.AttributionState state = CommitAttribution.AttributionState.createEmpty();

        assertTrue(state.fileStates().isEmpty());
        assertTrue(state.sessionBaselines().isEmpty());
        assertNotNull(state.surface());
        assertNull(state.startingHeadSha());
        assertEquals(0, state.promptCount());
    }

    @Test
    @DisplayName("CommitAttribution getClientSurface")
    void getClientSurface() {
        String surface = CommitAttribution.getClientSurface();
        // Returns "cli" if no env var set
        assertNotNull(surface);
    }

    @Test
    @DisplayName("CommitAttribution buildSurfaceKey")
    void buildSurfaceKey() {
        String key = CommitAttribution.buildSurfaceKey("cli", "claude-sonnet-4-6");
        assertEquals("cli/claude-sonnet-4-6", key);
    }

    @Test
    @DisplayName("CommitAttribution computeContentHash")
    void computeContentHash() {
        String hash = CommitAttribution.computeContentHash("test content");
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 produces 64 hex chars
    }

    @Test
    @DisplayName("CommitAttribution computeContentHash empty")
    void computeContentHashEmpty() {
        String hash = CommitAttribution.computeContentHash("");
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    @DisplayName("CommitAttribution computeContentHash consistent")
    void computeContentHashConsistent() {
        String hash1 = CommitAttribution.computeContentHash("test");
        String hash2 = CommitAttribution.computeContentHash("test");
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("CommitAttribution normalizeFilePath relative")
    void normalizeFilePathRelative() {
        String normalized = CommitAttribution.normalizeFilePath("test.txt", "/home/user");
        assertEquals("test.txt", normalized);
    }

    @Test
    @DisplayName("CommitAttribution trackFileModification")
    void trackFileModification() {
        CommitAttribution.AttributionState state = CommitAttribution.AttributionState.createEmpty();

        CommitAttribution.AttributionState newState = CommitAttribution.trackFileModification(
            state, "test.txt", "old content", "new content with more text", System.currentTimeMillis()
        );

        assertFalse(newState.fileStates().isEmpty());
        assertTrue(newState.fileStates().containsKey("test.txt"));
    }

    @Test
    @DisplayName("CommitAttribution trackFileCreation")
    void trackFileCreation() {
        CommitAttribution.AttributionState state = CommitAttribution.AttributionState.createEmpty();

        CommitAttribution.AttributionState newState = CommitAttribution.trackFileCreation(
            state, "new.txt", "brand new file content", System.currentTimeMillis()
        );

        assertFalse(newState.fileStates().isEmpty());
        assertTrue(newState.fileStates().containsKey("new.txt"));
    }

    @Test
    @DisplayName("CommitAttribution trackFileDeletion")
    void trackFileDeletion() {
        CommitAttribution.AttributionState state = CommitAttribution.AttributionState.createEmpty();

        CommitAttribution.AttributionState newState = CommitAttribution.trackFileDeletion(
            state, "deleted.txt", "content being deleted"
        );

        assertFalse(newState.fileStates().isEmpty());
        assertTrue(newState.fileStates().containsKey("deleted.txt"));
    }

    @Test
    @DisplayName("CommitAttribution sanitizeModelName opus 4-6")
    void sanitizeModelNameOpus46() {
        assertEquals("claude-opus-4-6", CommitAttribution.sanitizeModelName("opus-4-6-internal"));
    }

    @Test
    @DisplayName("CommitAttribution sanitizeModelName sonnet 4-6")
    void sanitizeModelNameSonnet46() {
        assertEquals("claude-sonnet-4-6", CommitAttribution.sanitizeModelName("sonnet-4-6-test"));
    }

    @Test
    @DisplayName("CommitAttribution sanitizeModelName haiku 4-5")
    void sanitizeModelNameHaiku45() {
        assertEquals("claude-haiku-4-5", CommitAttribution.sanitizeModelName("haiku-4-5-xyz"));
    }

    @Test
    @DisplayName("CommitAttribution sanitizeModelName unknown")
    void sanitizeModelNameUnknown() {
        assertEquals("claude", CommitAttribution.sanitizeModelName("unknown-model"));
    }

    @Test
    @DisplayName("CommitAttribution sanitizeSurfaceKey")
    void sanitizeSurfaceKey() {
        String sanitized = CommitAttribution.sanitizeSurfaceKey("cli/opus-4-6-internal");
        assertEquals("cli/claude-opus-4-6", sanitized);
    }

    @Test
    @DisplayName("CommitAttribution sanitizeSurfaceKey no slash")
    void sanitizeSurfaceKeyNoSlash() {
        String sanitized = CommitAttribution.sanitizeSurfaceKey("surface");
        assertEquals("surface", sanitized);
    }

    @Test
    @DisplayName("CommitAttribution isInternalModelRepoCached")
    void isInternalModelRepoCached() {
        // Just verify it doesn't throw
        boolean result = CommitAttribution.isInternalModelRepoCached();
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("CommitAttribution getRepoClassCached")
    void getRepoClassCached() {
        String cached = CommitAttribution.getRepoClassCached();
        // May be null or "external" after running isInternalModelRepo
        assertTrue(cached == null || "external".equals(cached) || "internal".equals(cached));
    }

    @Test
    @DisplayName("CommitAttribution isInternalModelRepo")
    void isInternalModelRepo() throws Exception {
        CompletableFuture<Boolean> future = CommitAttribution.isInternalModelRepo();
        Boolean result = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertFalse(result); // Should be false for non-internal repo
    }

    @Test
    @DisplayName("CommitAttribution incrementPromptCount")
    void incrementPromptCount() {
        CommitAttribution.AttributionState state = CommitAttribution.AttributionState.createEmpty();
        assertEquals(0, state.promptCount());

        CommitAttribution.AttributionState newState = CommitAttribution.incrementPromptCount(state);
        assertEquals(1, newState.promptCount());
        assertEquals(0, state.promptCount()); // Original unchanged
    }

    @Test
    @DisplayName("CommitAttribution AttributionData record")
    void attributionDataRecord() {
        CommitAttribution.AttributionData data = new CommitAttribution.AttributionData(
            1,
            new CommitAttribution.AttributionSummary(50, 100, 100, java.util.List.of("cli")),
            Map.of("file.txt", new CommitAttribution.FileAttribution(50, 50, 50, "cli")),
            Map.of("cli", new CommitAttribution.SurfaceBreakdown(100, 50)),
            java.util.List.of(),
            java.util.List.of("session-1")
        );

        assertEquals(1, data.version());
        assertNotNull(data.summary());
        assertEquals(1, data.files().size());
        assertEquals(1, data.surfaceBreakdown().size());
        assertEquals(1, data.sessions().size());
    }
}