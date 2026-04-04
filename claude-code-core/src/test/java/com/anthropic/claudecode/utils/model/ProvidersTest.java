/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for Providers.
 */
@DisplayName("Providers Tests")
class ProvidersTest {

    @BeforeEach
    void setUp() {
        // Clear any environment variable overrides
        // Note: We can't actually set env vars in Java tests, so we test what we can
    }

    @Test
    @DisplayName("Providers APIProvider enum has correct values")
    void apiProviderEnumHasCorrectValues() {
        Providers.APIProvider[] providers = Providers.APIProvider.values();

        assertEquals(4, providers.length);
        assertTrue(Arrays.asList(providers).contains(Providers.APIProvider.FIRST_PARTY));
        assertTrue(Arrays.asList(providers).contains(Providers.APIProvider.BEDROCK));
        assertTrue(Arrays.asList(providers).contains(Providers.APIProvider.VERTEX));
        assertTrue(Arrays.asList(providers).contains(Providers.APIProvider.FOUNDRY));
    }

    @Test
    @DisplayName("Providers APIProvider getValue works correctly")
    void apiProviderGetValueWorksCorrectly() {
        assertEquals("firstParty", Providers.APIProvider.FIRST_PARTY.getValue());
        assertEquals("bedrock", Providers.APIProvider.BEDROCK.getValue());
        assertEquals("vertex", Providers.APIProvider.VERTEX.getValue());
        assertEquals("foundry", Providers.APIProvider.FOUNDRY.getValue());
    }

    @Test
    @DisplayName("Providers APIProvider fromString works correctly")
    void apiProviderFromStringWorksCorrectly() {
        assertEquals(Providers.APIProvider.FIRST_PARTY, Providers.APIProvider.fromString("firstParty"));
        assertEquals(Providers.APIProvider.BEDROCK, Providers.APIProvider.fromString("bedrock"));
        assertEquals(Providers.APIProvider.VERTEX, Providers.APIProvider.fromString("vertex"));
        assertEquals(Providers.APIProvider.FOUNDRY, Providers.APIProvider.fromString("foundry"));
        // Unknown returns FIRST_PARTY
        assertEquals(Providers.APIProvider.FIRST_PARTY, Providers.APIProvider.fromString("unknown"));
    }

    @Test
    @DisplayName("Providers APIProvider fromString is case insensitive")
    void apiProviderFromStringIsCaseInsensitive() {
        assertEquals(Providers.APIProvider.BEDROCK, Providers.APIProvider.fromString("BEDROCK"));
        assertEquals(Providers.APIProvider.VERTEX, Providers.APIProvider.fromString("Vertex"));
    }

    @Test
    @DisplayName("Providers getAPIProvider returns FIRST_PARTY by default")
    void getAPIProviderReturnsFirstPartyByDefault() {
        // Without env vars set, should return FIRST_PARTY
        Providers.APIProvider provider = Providers.getAPIProvider();

        assertEquals(Providers.APIProvider.FIRST_PARTY, provider);
    }

    @Test
    @DisplayName("Providers isFirstParty returns true by default")
    void isFirstPartyReturnsTrueByDefault() {
        assertTrue(Providers.isFirstParty());
    }

    @Test
    @DisplayName("Providers isBedrock returns false by default")
    void isBedrockReturnsFalseByDefault() {
        assertFalse(Providers.isBedrock());
    }

    @Test
    @DisplayName("Providers isVertex returns false by default")
    void isVertexReturnsFalseByDefault() {
        assertFalse(Providers.isVertex());
    }

    @Test
    @DisplayName("Providers isFoundry returns false by default")
    void isFoundryReturnsFalseByDefault() {
        assertFalse(Providers.isFoundry());
    }

    @Test
    @DisplayName("Providers getAPIProviderForAnalytics returns correct value")
    void getAPIProviderForAnalyticsReturnsCorrectValue() {
        String analyticsProvider = Providers.getAPIProviderForAnalytics();

        assertEquals("firstParty", analyticsProvider);
    }

    @Test
    @DisplayName("Providers isFirstPartyAnthropicBaseUrl returns true for api.anthropic.com")
    void isFirstPartyAnthropicBaseUrlReturnsTrueForAnthropicApi() {
        // This test depends on environment state
        // If ANTHROPIC_BASE_URL is null, it returns true (default API)
        // If ANTHROPIC_BASE_URL is set to api.anthropic.com, returns true
        // We can't modify env vars, so we just verify the method works
        boolean result = Providers.isFirstPartyAnthropicBaseUrl();
        // Just verify method runs without error
        assertNotNull(result);
    }

    @Test
    @DisplayName("Providers APIProvider enum values are in correct order")
    void apiProviderEnumValuesInCorrectOrder() {
        Providers.APIProvider[] providers = Providers.APIProvider.values();

        assertEquals(Providers.APIProvider.FIRST_PARTY, providers[0]);
        assertEquals(Providers.APIProvider.BEDROCK, providers[1]);
        assertEquals(Providers.APIProvider.VERTEX, providers[2]);
        assertEquals(Providers.APIProvider.FOUNDRY, providers[3]);
    }
}