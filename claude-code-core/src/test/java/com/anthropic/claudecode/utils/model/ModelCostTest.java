/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ModelCost.
 */
@DisplayName("ModelCost Tests")
class ModelCostTest {

    @Test
    @DisplayName("Costs record formatPricing works correctly")
    void costsFormatPricingWorksCorrectly() {
        ModelCost.Costs costs = new ModelCost.Costs(3, 15, 3.75, 0.3, 0.01);

        String pricing = costs.formatPricing();
        assertTrue(pricing.contains("$3"));
        assertTrue(pricing.contains("$15"));
        assertTrue(pricing.contains("per Mtok"));
    }

    @Test
    @DisplayName("Costs record formatPrice handles decimals")
    void costsFormatPriceHandlesDecimals() {
        ModelCost.Costs costs = new ModelCost.Costs(3.75, 15.50, 3.75, 0.3, 0.01);

        String pricing = costs.formatPricing();
        assertTrue(pricing.contains("3.75") || pricing.contains("3.75"));
    }

    @Test
    @DisplayName("getModelCosts returns correct costs for known models")
    void getModelCostsReturnsCorrectCostsForKnownModels() {
        ModelCost.Costs haikuCosts = ModelCost.getModelCosts("claude-haiku-4-5");
        assertEquals(1, haikuCosts.inputTokens());
        assertEquals(5, haikuCosts.outputTokens());

        ModelCost.Costs sonnetCosts = ModelCost.getModelCosts("claude-sonnet-4-6");
        assertEquals(3, sonnetCosts.inputTokens());
        assertEquals(15, sonnetCosts.outputTokens());

        ModelCost.Costs opusCosts = ModelCost.getModelCosts("claude-opus-4-6");
        assertEquals(5, opusCosts.inputTokens());
        assertEquals(25, opusCosts.outputTokens());
    }

    @Test
    @DisplayName("getModelCosts returns default for unknown models")
    void getModelCostsReturnsDefaultForUnknownModels() {
        ModelCost.Costs costs = ModelCost.getModelCosts("unknown-model");
        assertEquals(ModelCost.DEFAULT_COSTS, costs);
    }

    @Test
    @DisplayName("getModelCosts handles null model")
    void getModelCostsHandlesNullModel() {
        ModelCost.Costs costs = ModelCost.getModelCosts(null);
        assertEquals(ModelCost.DEFAULT_COSTS, costs);
    }

    @Test
    @DisplayName("getModelCosts normalizes model names")
    void getModelCostsNormalizesModelNames() {
        ModelCost.Costs costs1 = ModelCost.getModelCosts("anthropic/claude-sonnet-4-6");
        ModelCost.Costs costs2 = ModelCost.getModelCosts("claude-sonnet-4-6");

        assertEquals(costs1.inputTokens(), costs2.inputTokens());
    }

    @Test
    @DisplayName("calculateCost calculates correctly for basic usage")
    void calculateCostCalculatesCorrectlyForBasicUsage() {
        ModelCost.TokenUsage usage = ModelCost.TokenUsage.of(1_000_000, 1_000_000);

        // Sonnet: $3/M input, $15/M output
        double cost = ModelCost.calculateCost("claude-sonnet-4-6", usage);
        assertEquals(18.0, cost, 0.001);
    }

    @Test
    @DisplayName("calculateCost calculates correctly with cache")
    void calculateCostCalculatesCorrectlyWithCache() {
        ModelCost.TokenUsage usage = ModelCost.TokenUsage.withCache(
            1_000_000, 1_000_000, 500_000, 100_000
        );

        double cost = ModelCost.calculateCost("claude-sonnet-4-6", usage);
        assertTrue(cost > 18.0); // Should include cache costs
    }

    @Test
    @DisplayName("formatCost formats small values correctly")
    void formatCostFormatsSmallValuesCorrectly() {
        assertEquals("$0.0050", ModelCost.formatCost(0.005));
        assertEquals("$0.0001", ModelCost.formatCost(0.0001));
    }

    @Test
    @DisplayName("formatCost formats larger values correctly")
    void formatCostFormatsLargerValuesCorrectly() {
        assertEquals("$1.50", ModelCost.formatCost(1.5));
        assertEquals("$10.00", ModelCost.formatCost(10.0));
    }

    @Test
    @DisplayName("isOpusModel detects opus models")
    void isOpusModelDetectsOpusModels() {
        assertTrue(ModelCost.isOpusModel("claude-opus-4-6"));
        assertTrue(ModelCost.isOpusModel("claude-4-opus"));
        assertTrue(ModelCost.isOpusModel("anthropic/claude-opus-4-5"));
        assertFalse(ModelCost.isOpusModel("claude-sonnet-4-6"));
        assertFalse(ModelCost.isOpusModel("claude-haiku-4-5"));
    }

    @Test
    @DisplayName("isSonnetModel detects sonnet models")
    void isSonnetModelDetectsSonnetModels() {
        assertTrue(ModelCost.isSonnetModel("claude-sonnet-4-6"));
        assertTrue(ModelCost.isSonnetModel("claude-4-sonnet"));
        assertFalse(ModelCost.isSonnetModel("claude-opus-4-6"));
        assertFalse(ModelCost.isSonnetModel("claude-haiku-4-5"));
    }

    @Test
    @DisplayName("isHaikuModel detects haiku models")
    void isHaikuModelDetectsHaikuModels() {
        assertTrue(ModelCost.isHaikuModel("claude-haiku-4-5"));
        assertTrue(ModelCost.isHaikuModel("claude-4-5-haiku"));
        assertFalse(ModelCost.isHaikuModel("claude-sonnet-4-6"));
        assertFalse(ModelCost.isHaikuModel("claude-opus-4-6"));
    }

    @Test
    @DisplayName("getPricingString returns formatted pricing")
    void getPricingStringReturnsFormattedPricing() {
        String pricing = ModelCost.getPricingString("claude-sonnet-4-6");
        assertNotNull(pricing);
        assertTrue(pricing.contains("$"));
    }

    @Test
    @DisplayName("TokenUsage factory methods work correctly")
    void tokenUsageFactoryMethodsWorkCorrectly() {
        ModelCost.TokenUsage usage1 = ModelCost.TokenUsage.of(100, 200);
        assertEquals(100, usage1.inputTokens());
        assertEquals(200, usage1.outputTokens());
        assertEquals(0, usage1.cacheReadInputTokens());

        ModelCost.TokenUsage usage2 = ModelCost.TokenUsage.withCache(100, 200, 50, 25);
        assertEquals(100, usage2.inputTokens());
        assertEquals(200, usage2.outputTokens());
        assertEquals(50, usage2.cacheReadInputTokens());
        assertEquals(25, usage2.cacheCreationInputTokens());
    }

    @Test
    @DisplayName("Fast mode opus returns higher costs")
    void fastModeOpusReturnsHigherCosts() {
        ModelCost.TokenUsage normalUsage = new ModelCost.TokenUsage(100, 100, 0, 0, 0, null);
        ModelCost.TokenUsage fastUsage = new ModelCost.TokenUsage(100, 100, 0, 0, 0, "fast");

        ModelCost.Costs normalCosts = ModelCost.getModelCosts("claude-opus-4-6", normalUsage);
        ModelCost.Costs fastCosts = ModelCost.getModelCosts("claude-opus-4-6", fastUsage);

        assertTrue(fastCosts.inputTokens() > normalCosts.inputTokens());
    }
}
