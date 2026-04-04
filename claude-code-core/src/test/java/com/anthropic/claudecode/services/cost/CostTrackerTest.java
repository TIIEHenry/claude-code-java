/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.cost;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CostTracker.
 */
class CostTrackerTest {

    @BeforeEach
    void setUp() {
        CostTracker.resetStateForTests();
    }

    @Test
    @DisplayName("CostTracker addToTotalSessionCost adds cost")
    void addToTotalSessionCost() {
        CostTracker.InputOutputTokens tokens = new CostTracker.InputOutputTokens(
            100, 50, 0, 0, 0, 200000, 4096
        );

        double cost = CostTracker.addToTotalSessionCost(0.05, tokens, "claude-sonnet");

        assertEquals(0.05, cost);
        assertEquals(0.05, CostTracker.getTotalCostUSD());
        assertEquals(100, CostTracker.getTotalInputTokens());
        assertEquals(50, CostTracker.getTotalOutputTokens());
    }

    @Test
    @DisplayName("CostTracker multiple additions accumulate")
    void multipleAdditions() {
        CostTracker.InputOutputTokens tokens1 = new CostTracker.InputOutputTokens(
            100, 50, 10, 5, 0, 200000, 4096
        );
        CostTracker.InputOutputTokens tokens2 = new CostTracker.InputOutputTokens(
            200, 100, 20, 10, 2, 200000, 4096
        );

        CostTracker.addToTotalSessionCost(0.05, tokens1, "claude-sonnet");
        CostTracker.addToTotalSessionCost(0.10, tokens2, "claude-opus");

        // Use tolerance for floating point comparison
        assertEquals(0.15, CostTracker.getTotalCostUSD(), 0.001);
        assertEquals(300, CostTracker.getTotalInputTokens());
        assertEquals(150, CostTracker.getTotalOutputTokens());
        assertEquals(30, CostTracker.getTotalCacheReadInputTokens());
        assertEquals(15, CostTracker.getTotalCacheCreationInputTokens());
        assertEquals(2, CostTracker.getTotalWebSearchRequests());
    }

    @Test
    @DisplayName("CostTracker model usage tracking")
    void modelUsageTracking() {
        CostTracker.InputOutputTokens tokens = new CostTracker.InputOutputTokens(
            100, 50, 0, 0, 0, 200000, 4096
        );

        CostTracker.addToTotalSessionCost(0.05, tokens, "claude-sonnet");

        Map<String, CostTracker.ModelUsage> usage = CostTracker.getModelUsage();
        assertTrue(usage.containsKey("claude-sonnet"));

        CostTracker.ModelUsage sonnetUsage = CostTracker.getUsageForModel("claude-sonnet");
        assertNotNull(sonnetUsage);
        assertEquals(100, sonnetUsage.inputTokens());
        assertEquals(50, sonnetUsage.outputTokens());
        assertEquals(0.05, sonnetUsage.costUSD());
    }

    @Test
    @DisplayName("CostTracker addToTotalAPIDuration")
    void addToTotalAPIDuration() {
        CostTracker.addToTotalAPIDuration(1000);
        CostTracker.addToTotalAPIDuration(500);

        assertEquals(1500, CostTracker.getTotalAPIDuration());
    }

    @Test
    @DisplayName("CostTracker addToTotalAPIDurationWithoutRetries")
    void addToTotalAPIDurationWithoutRetries() {
        CostTracker.addToTotalAPIDurationWithoutRetries(1000);
        CostTracker.addToTotalAPIDurationWithoutRetries(500);

        assertEquals(1500, CostTracker.getTotalAPIDurationWithoutRetries());
    }

    @Test
    @DisplayName("CostTracker addToTotalToolDuration")
    void addToTotalToolDuration() {
        CostTracker.addToTotalToolDuration(2000);
        CostTracker.addToTotalToolDuration(1000);

        assertEquals(3000, CostTracker.getTotalToolDuration());
    }

    @Test
    @DisplayName("CostTracker addToTotalLinesChanged")
    void addToTotalLinesChanged() {
        CostTracker.addToTotalLinesChanged(50, 20);
        CostTracker.addToTotalLinesChanged(30, 10);

        assertEquals(80, CostTracker.getTotalLinesAdded());
        assertEquals(30, CostTracker.getTotalLinesRemoved());
    }

    @Test
    @DisplayName("CostTracker resetCostState resets all values")
    void resetCostState() {
        CostTracker.InputOutputTokens tokens = new CostTracker.InputOutputTokens(
            100, 50, 0, 0, 0, 200000, 4096
        );
        CostTracker.addToTotalSessionCost(0.05, tokens, "claude-sonnet");
        CostTracker.addToTotalAPIDuration(1000);
        CostTracker.addToTotalLinesChanged(50, 20);

        CostTracker.resetCostState();

        assertEquals(0.0, CostTracker.getTotalCostUSD());
        assertEquals(0, CostTracker.getTotalAPIDuration());
        assertEquals(0, CostTracker.getTotalLinesAdded());
        assertEquals(0, CostTracker.getTotalLinesRemoved());
        assertEquals(0, CostTracker.getTotalInputTokens());
        assertEquals(0, CostTracker.getTotalOutputTokens());
        assertTrue(CostTracker.getModelUsage().isEmpty());
    }

    @Test
    @DisplayName("CostTracker formatCost formats correctly")
    void formatCost() {
        assertEquals("$0.0000", CostTracker.formatCost(0.0));
        assertEquals("$0.0500", CostTracker.formatCost(0.05));
        // 0.5 is NOT > 0.5, so uses 4 decimal places
        assertEquals("$0.5000", CostTracker.formatCost(0.5));
        // > 0.5 rounds to 2 decimals
        assertEquals("$1.00", CostTracker.formatCost(1.0));
        assertEquals("$0.51", CostTracker.formatCost(0.51));
    }

    @Test
    @DisplayName("CostTracker formatDuration formats correctly")
    void formatDuration() {
        assertEquals("0s", CostTracker.formatDuration(0));
        assertEquals("30s", CostTracker.formatDuration(30000));
        assertEquals("1m 0s", CostTracker.formatDuration(60000));
        assertEquals("1m 30s", CostTracker.formatDuration(90000));
        assertEquals("1h 0m", CostTracker.formatDuration(3600000));
        assertEquals("1h 30m", CostTracker.formatDuration(5400000));
    }

    @Test
    @DisplayName("CostTracker formatNumber formats with thousands separator")
    void formatNumber() {
        assertEquals("0", CostTracker.formatNumber(0));
        assertEquals("100", CostTracker.formatNumber(100));
        assertEquals("1,000", CostTracker.formatNumber(1000));
        assertEquals("1,000,000", CostTracker.formatNumber(1000000));
    }

    @Test
    @DisplayName("CostTracker hasUnknownModelCost")
    void hasUnknownModelCost() {
        assertFalse(CostTracker.hasUnknownModelCost());

        CostTracker.setHasUnknownModelCost(true);
        assertTrue(CostTracker.hasUnknownModelCost());

        CostTracker.setHasUnknownModelCost(false);
        assertFalse(CostTracker.hasUnknownModelCost());
    }

    @Test
    @DisplayName("CostTracker sessionId is generated")
    void sessionId() {
        String sessionId = CostTracker.getSessionId();
        assertNotNull(sessionId);
        assertFalse(sessionId.isEmpty());
    }

    @Test
    @DisplayName("CostTracker StoredCostState record")
    void storedCostStateRecord() {
        CostTracker.InputOutputTokens tokens = new CostTracker.InputOutputTokens(
            100, 50, 0, 0, 0, 200000, 4096
        );
        CostTracker.addToTotalSessionCost(0.05, tokens, "claude-sonnet");
        CostTracker.addToTotalAPIDuration(1000);

        CostTracker.StoredCostState state = CostTracker.getStoredCostState();

        assertEquals(0.05, state.totalCostUSD());
        assertEquals(1000, state.totalAPIDuration());
        assertTrue(state.modelUsage().containsKey("claude-sonnet"));
    }

    @Test
    @DisplayName("CostTracker setCostStateForRestore restores state")
    void setCostStateForRestore() {
        Map<String, CostTracker.ModelUsage> usage = Map.of(
            "claude-sonnet", new CostTracker.ModelUsage(100, 50, 0, 0, 0, 0.05, 200000, 4096)
        );

        CostTracker.StoredCostState state = new CostTracker.StoredCostState(
            0.10, 2000, 1500, 500, 100, 50, 1000L, usage
        );

        CostTracker.setCostStateForRestore(state);

        assertEquals(0.10, CostTracker.getTotalCostUSD());
        assertEquals(2000, CostTracker.getTotalAPIDuration());
        assertEquals(1500, CostTracker.getTotalAPIDurationWithoutRetries());
        assertEquals(500, CostTracker.getTotalToolDuration());
        assertEquals(100, CostTracker.getTotalLinesAdded());
        assertEquals(50, CostTracker.getTotalLinesRemoved());
        assertTrue(CostTracker.getModelUsage().containsKey("claude-sonnet"));
    }

    @Test
    @DisplayName("CostTracker ModelUsage record default constructor")
    void modelUsageDefault() {
        CostTracker.ModelUsage usage = new CostTracker.ModelUsage();

        assertEquals(0, usage.inputTokens());
        assertEquals(0, usage.outputTokens());
        assertEquals(0, usage.cacheReadInputTokens());
        assertEquals(0, usage.cacheCreationInputTokens());
        assertEquals(0, usage.webSearchRequests());
        assertEquals(0.0, usage.costUSD());
        assertEquals(0, usage.contextWindow());
        assertEquals(0, usage.maxOutputTokens());
    }

    @Test
    @DisplayName("CostTracker ModelUsage add accumulates")
    void modelUsageAdd() {
        CostTracker.ModelUsage initial = new CostTracker.ModelUsage();
        CostTracker.InputOutputTokens tokens = new CostTracker.InputOutputTokens(
            100, 50, 10, 5, 2, 200000, 4096
        );

        CostTracker.ModelUsage updated = initial.add(tokens, 0.05);

        assertEquals(100, updated.inputTokens());
        assertEquals(50, updated.outputTokens());
        assertEquals(10, updated.cacheReadInputTokens());
        assertEquals(5, updated.cacheCreationInputTokens());
        assertEquals(2, updated.webSearchRequests());
        assertEquals(0.05, updated.costUSD());
        assertEquals(200000, updated.contextWindow());
        assertEquals(4096, updated.maxOutputTokens());
    }

    @Test
    @DisplayName("CostTracker formatTotalCost returns formatted string")
    void formatTotalCost() {
        CostTracker.InputOutputTokens tokens = new CostTracker.InputOutputTokens(
            100, 50, 0, 0, 0, 200000, 4096
        );
        CostTracker.addToTotalSessionCost(0.05, tokens, "claude-sonnet");
        CostTracker.addToTotalLinesChanged(10, 5);

        String formatted = CostTracker.formatTotalCost();

        assertTrue(formatted.contains("Total cost:"));
        assertTrue(formatted.contains("Total duration"));
        assertTrue(formatted.contains("Total code changes"));
        assertTrue(formatted.contains("Usage by model"));
    }
}