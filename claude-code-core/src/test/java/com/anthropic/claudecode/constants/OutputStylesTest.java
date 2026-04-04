/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OutputStyles.
 */
class OutputStylesTest {

    @Test
    @DisplayName("OutputStyles DEFAULT_OUTPUT_STYLE_NAME")
    void defaultOutputStyleName() {
        assertEquals("default", OutputStyles.DEFAULT_OUTPUT_STYLE_NAME);
    }

    @Test
    @DisplayName("OutputStyles OUTPUT_STYLE_CONFIG not empty")
    void outputStyleConfigNotEmpty() {
        assertFalse(OutputStyles.OUTPUT_STYLE_CONFIG.isEmpty());
    }

    @Test
    @DisplayName("OutputStyles OUTPUT_STYLE_CONFIG contains default")
    void outputStyleConfigContainsDefault() {
        assertTrue(OutputStyles.OUTPUT_STYLE_CONFIG.containsKey("default"));
        assertNull(OutputStyles.OUTPUT_STYLE_CONFIG.get("default"));
    }

    @Test
    @DisplayName("OutputStyles OUTPUT_STYLE_CONFIG contains Explanatory")
    void outputStyleConfigContainsExplanatory() {
        assertTrue(OutputStyles.OUTPUT_STYLE_CONFIG.containsKey("Explanatory"));

        OutputStyles.OutputStyleConfig config = OutputStyles.OUTPUT_STYLE_CONFIG.get("Explanatory");
        assertNotNull(config);
        assertEquals("Explanatory", config.name());
        assertTrue(config.keepCodingInstructions());
        assertFalse(config.forceForPlugin());
    }

    @Test
    @DisplayName("OutputStyles OUTPUT_STYLE_CONFIG contains Learning")
    void outputStyleConfigContainsLearning() {
        assertTrue(OutputStyles.OUTPUT_STYLE_CONFIG.containsKey("Learning"));

        OutputStyles.OutputStyleConfig config = OutputStyles.OUTPUT_STYLE_CONFIG.get("Learning");
        assertNotNull(config);
        assertEquals("Learning", config.name());
        assertTrue(config.keepCodingInstructions());
    }

    @Test
    @DisplayName("OutputStyles OutputStyleConfig record")
    void outputStyleConfigRecord() {
        OutputStyles.OutputStyleConfig config = new OutputStyles.OutputStyleConfig(
            "test",
            "Test style",
            "prompt",
            "built-in",
            true,
            false
        );

        assertEquals("test", config.name());
        assertEquals("Test style", config.description());
        assertEquals("prompt", config.prompt());
        assertEquals("built-in", config.source());
        assertTrue(config.keepCodingInstructions());
        assertFalse(config.forceForPlugin());
    }

    @Test
    @DisplayName("OutputStyles Explanatory prompt contains insight")
    void explanatoryPromptContainsInsight() {
        OutputStyles.OutputStyleConfig config = OutputStyles.OUTPUT_STYLE_CONFIG.get("Explanatory");

        assertTrue(config.prompt().contains("Insight"));
        assertTrue(config.prompt().contains("educational"));
    }

    @Test
    @DisplayName("OutputStyles Learning prompt contains hands-on")
    void learningPromptContainsHandsOn() {
        OutputStyles.OutputStyleConfig config = OutputStyles.OUTPUT_STYLE_CONFIG.get("Learning");

        assertTrue(config.prompt().contains("hands-on"));
        assertTrue(config.prompt().contains("Learning Style Active"));
    }
}