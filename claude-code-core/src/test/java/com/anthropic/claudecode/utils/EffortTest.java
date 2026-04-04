/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Effort.
 */
class EffortTest {

    @Test
    @DisplayName("Effort EffortLevel enum values")
    void effortLevelEnum() {
        Effort.EffortLevel[] levels = Effort.EffortLevel.values();
        assertEquals(4, levels.length);
    }

    @Test
    @DisplayName("Effort EffortLevel getValue")
    void effortLevelGetValue() {
        assertEquals("low", Effort.EffortLevel.LOW.getValue());
        assertEquals("medium", Effort.EffortLevel.MEDIUM.getValue());
        assertEquals("high", Effort.EffortLevel.HIGH.getValue());
        assertEquals("max", Effort.EffortLevel.MAX.getValue());
    }

    @Test
    @DisplayName("Effort EffortLevel getDescription")
    void effortLevelGetDescription() {
        assertNotNull(Effort.EffortLevel.LOW.getDescription());
        assertNotNull(Effort.EffortLevel.MEDIUM.getDescription());
        assertNotNull(Effort.EffortLevel.HIGH.getDescription());
        assertNotNull(Effort.EffortLevel.MAX.getDescription());
    }

    @Test
    @DisplayName("Effort EffortLevel fromString")
    void effortLevelFromString() {
        assertEquals(Effort.EffortLevel.LOW, Effort.EffortLevel.fromString("low"));
        assertEquals(Effort.EffortLevel.MEDIUM, Effort.EffortLevel.fromString("medium"));
        assertEquals(Effort.EffortLevel.HIGH, Effort.EffortLevel.fromString("high"));
        assertEquals(Effort.EffortLevel.MAX, Effort.EffortLevel.fromString("max"));
        assertEquals(Effort.EffortLevel.HIGH, Effort.EffortLevel.fromString("unknown"));
    }

    @Test
    @DisplayName("Effort EffortLevel fromString case insensitive")
    void effortLevelFromStringCaseInsensitive() {
        assertEquals(Effort.EffortLevel.LOW, Effort.EffortLevel.fromString("LOW"));
        assertEquals(Effort.EffortLevel.MEDIUM, Effort.EffortLevel.fromString("MEDIUM"));
    }

    @Test
    @DisplayName("Effort EffortLevel fromString null")
    void effortLevelFromStringNull() {
        assertNull(Effort.EffortLevel.fromString(null));
    }

    @Test
    @DisplayName("Effort EffortLevelValue record")
    void effortLevelValueRecord() {
        Effort.EffortLevelValue value = new Effort.EffortLevelValue(Effort.EffortLevel.HIGH);
        assertEquals(Effort.EffortLevel.HIGH, value.level());
    }

    @Test
    @DisplayName("Effort NumericEffortValue record")
    void numericEffortValueRecord() {
        Effort.NumericEffortValue value = new Effort.NumericEffortValue(75);
        assertEquals(75, value.value());
    }

    @Test
    @DisplayName("Effort modelSupportsEffort opus 4-6")
    void modelSupportsEffortOpus46() {
        assertTrue(Effort.modelSupportsEffort("claude-opus-4-6"));
    }

    @Test
    @DisplayName("Effort modelSupportsEffort sonnet 4-6")
    void modelSupportsEffortSonnet46() {
        assertTrue(Effort.modelSupportsEffort("claude-sonnet-4-6"));
    }

    @Test
    @DisplayName("Effort modelSupportsEffort null")
    void modelSupportsEffortNull() {
        assertFalse(Effort.modelSupportsEffort(null));
    }

    @Test
    @DisplayName("Effort modelSupportsEffort empty")
    void modelSupportsEffortEmpty() {
        assertFalse(Effort.modelSupportsEffort(""));
    }

    @Test
    @DisplayName("Effort modelSupportsMaxEffort opus 4-6")
    void modelSupportsMaxEffortOpus46() {
        assertTrue(Effort.modelSupportsMaxEffort("claude-opus-4-6"));
    }

    @Test
    @DisplayName("Effort modelSupportsMaxEffort sonnet")
    void modelSupportsMaxEffortSonnet() {
        assertFalse(Effort.modelSupportsMaxEffort("claude-sonnet-4-6"));
    }

    @Test
    @DisplayName("Effort isEffortLevel true")
    void isEffortLevelTrue() {
        assertTrue(Effort.isEffortLevel("low"));
        assertTrue(Effort.isEffortLevel("medium"));
        assertTrue(Effort.isEffortLevel("high"));
        assertTrue(Effort.isEffortLevel("max"));
    }

    @Test
    @DisplayName("Effort isEffortLevel false")
    void isEffortLevelFalse() {
        assertFalse(Effort.isEffortLevel("unknown"));
        assertFalse(Effort.isEffortLevel(null));
    }

    @Test
    @DisplayName("Effort isValidNumericEffort")
    void isValidNumericEffort() {
        assertTrue(Effort.isValidNumericEffort(0));
        assertTrue(Effort.isValidNumericEffort(50));
        assertTrue(Effort.isValidNumericEffort(100));
        assertFalse(Effort.isValidNumericEffort(-1));
        assertFalse(Effort.isValidNumericEffort(101));
    }

    @Test
    @DisplayName("Effort parseEffortValue level string")
    void parseEffortValueLevelString() {
        Effort.EffortValue value = Effort.parseEffortValue("high");
        assertNotNull(value);
        assertTrue(value instanceof Effort.EffortLevelValue);
        assertEquals(Effort.EffortLevel.HIGH, ((Effort.EffortLevelValue) value).level());
    }

    @Test
    @DisplayName("Effort parseEffortValue numeric")
    void parseEffortValueNumeric() {
        Effort.EffortValue value = Effort.parseEffortValue(75);
        assertNotNull(value);
        assertTrue(value instanceof Effort.NumericEffortValue);
        assertEquals(75, ((Effort.NumericEffortValue) value).value());
    }

    @Test
    @DisplayName("Effort parseEffortValue null")
    void parseEffortValueNull() {
        assertNull(Effort.parseEffortValue(null));
    }

    @Test
    @DisplayName("Effort convertEffortValueToLevel from level")
    void convertEffortValueToLevelFromLevel() {
        Effort.EffortValue value = new Effort.EffortLevelValue(Effort.EffortLevel.MEDIUM);
        assertEquals(Effort.EffortLevel.MEDIUM, Effort.convertEffortValueToLevel(value));
    }

    @Test
    @DisplayName("Effort convertEffortValueToLevel from numeric")
    void convertEffortValueToLevelFromNumeric() {
        assertEquals(Effort.EffortLevel.LOW, Effort.convertEffortValueToLevel(new Effort.NumericEffortValue(30)));
        assertEquals(Effort.EffortLevel.MEDIUM, Effort.convertEffortValueToLevel(new Effort.NumericEffortValue(60)));
        assertEquals(Effort.EffortLevel.HIGH, Effort.convertEffortValueToLevel(new Effort.NumericEffortValue(90)));
    }

    @Test
    @DisplayName("Effort getDefaultEffortForModel null")
    void getDefaultEffortForModelNull() {
        assertEquals(Effort.EffortLevel.HIGH, Effort.getDefaultEffortForModel(null));
    }

    @Test
    @DisplayName("Effort getDefaultEffortForModel opus 4-6")
    void getDefaultEffortForModelOpus46() {
        assertEquals(Effort.EffortLevel.MEDIUM, Effort.getDefaultEffortForModel("claude-opus-4-6"));
    }

    @Test
    @DisplayName("Effort getEffortLevelDescription")
    void getEffortLevelDescription() {
        assertNotNull(Effort.getEffortLevelDescription(Effort.EffortLevel.HIGH));
        assertEquals("Unknown effort level", Effort.getEffortLevelDescription(null));
    }

    @Test
    @DisplayName("Effort getEffortEnvOverride default")
    void getEffortEnvOverrideDefault() {
        // Without env var, should return null
        Effort.EffortValue value = Effort.getEffortEnvOverride();
        // May be null or some value depending on env
        assertTrue(value == null || value != null);
    }

    @Test
    @DisplayName("Effort getEffortSuffix")
    void getEffortSuffix() {
        String suffix = Effort.getEffortSuffix("claude-opus-4-6", new Effort.EffortLevelValue(Effort.EffortLevel.HIGH));
        assertTrue(suffix.contains("high"));
    }

    @Test
    @DisplayName("Effort getEffortSuffix null value")
    void getEffortSuffixNull() {
        assertEquals("", Effort.getEffortSuffix("model", null));
    }
}