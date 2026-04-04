/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.vcr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VcrService.
 */
class VcrServiceTest {

    private VcrService vcrService;

    @BeforeEach
    void setUp() {
        vcrService = new VcrService();
        vcrService.disable();
    }

    @Test
    @DisplayName("VcrService shouldUseVCR returns false initially")
    void shouldUseVCRInitiallyFalse() {
        assertFalse(vcrService.shouldUseVCR());
    }

    @Test
    @DisplayName("VcrService enable and shouldUseVCR")
    void enableAndShouldUseVCR() {
        vcrService.enable(false);
        assertTrue(vcrService.shouldUseVCR());
    }

    @Test
    @DisplayName("VcrService disable")
    void disable() {
        vcrService.enable(false);
        vcrService.disable();
        assertFalse(vcrService.shouldUseVCR());
    }

    @Test
    @DisplayName("VcrService setFixturesRoot")
    void setFixturesRoot() {
        vcrService.setFixturesRoot(Paths.get("/tmp/fixtures"));
        // No exception
    }

    @Test
    @DisplayName("VcrService dehydrateValue handles null")
    void dehydrateValueNull() {
        assertNull(vcrService.dehydrateValue(null));
    }

    @Test
    @DisplayName("VcrService dehydrateValue replaces patterns")
    void dehydrateValuePatterns() {
        String input = "num_files=\"123\" duration_ms=\"456\" cost_usd=\"789\"";
        String result = vcrService.dehydrateValue(input);

        assertTrue(result.contains("[NUM]"));
        assertTrue(result.contains("[DURATION]"));
        assertTrue(result.contains("[COST]"));
    }

    @Test
    @DisplayName("VcrService hydrateValue handles null")
    void hydrateValueNull() {
        assertNull(vcrService.hydrateValue(null));
    }

    @Test
    @DisplayName("VcrService hydrateValue replaces placeholders")
    void hydrateValuePlaceholders() {
        String input = "[NUM] files in [DURATION] ms";
        String result = vcrService.hydrateValue(input);

        assertTrue(result.contains("1 files"));
        assertTrue(result.contains("100 ms"));
    }

    @Test
    @DisplayName("VcrService withFixture returns result when disabled")
    void withFixtureDisabled() throws Exception {
        vcrService.disable();

        String result = vcrService.withFixture(
            "input", "test-fixture",
            () -> java.util.concurrent.CompletableFuture.completedFuture("test-result")
        ).get();

        assertEquals("test-result", result);
    }

    @Test
    @DisplayName("VcrService withVCR returns result when disabled")
    void withVCRDisabled() throws Exception {
        vcrService.disable();

        java.util.List<String> result = vcrService.withVCR(
            java.util.List.of("msg1", "msg2"),
            () -> java.util.concurrent.CompletableFuture.completedFuture(java.util.List.of("result"))
        ).get();

        assertEquals(1, result.size());
        assertEquals("result", result.get(0));
    }
}