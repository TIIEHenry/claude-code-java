/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SpinnerVerbs constants.
 */
class SpinnerVerbsTest {

    @Test
    @DisplayName("SpinnerVerbs SPINNER_VERBS not null")
    void spinnerVerbsNotNull() {
        assertNotNull(SpinnerVerbs.SPINNER_VERBS);
    }

    @Test
    @DisplayName("SpinnerVerbs SPINNER_VERBS not empty")
    void spinnerVerbsNotEmpty() {
        assertFalse(SpinnerVerbs.SPINNER_VERBS.isEmpty());
    }

    @Test
    @DisplayName("SpinnerVerbs SPINNER_VERBS contains expected count")
    void spinnerVerbsCount() {
        // Should have at least 100 verbs
        assertTrue(SpinnerVerbs.SPINNER_VERBS.size() >= 100);
    }

    @Test
    @DisplayName("SpinnerVerbs SPINNER_VERBS contains common verbs")
    void spinnerVerbsContainsCommon() {
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Thinking"));
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Processing"));
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Computing"));
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Working"));
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Creating"));
    }

    @Test
    @DisplayName("SpinnerVerbs SPINNER_VERBS contains creative verbs")
    void spinnerVerbsContainsCreative() {
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Clauding"));
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Booping"));
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Quantumizing"));
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Reticulating"));
    }

    @Test
    @DisplayName("SpinnerVerbs SPINNER_VERBS contains cooking verbs")
    void spinnerVerbsContainsCooking() {
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Baking"));
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Brewing"));
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Cooking"));
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Simmering"));
    }

    @Test
    @DisplayName("SpinnerVerbs SPINNER_VERBS contains movement verbs")
    void spinnerVerbsContainsMovement() {
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Wandering"));
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Gallivanting"));
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Meandering"));
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Skedaddling"));
    }

    @Test
    @DisplayName("SpinnerVerbs SPINNER_VERBS contains funny verbs")
    void spinnerVerbsContainsFunny() {
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Discombobulating"));
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Recombobulating"));
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Flibbertigibbeting"));
        assertTrue(SpinnerVerbs.SPINNER_VERBS.contains("Shenaniganing"));
    }

    @Test
    @DisplayName("SpinnerVerbs SPINNER_VERBS all elements are strings")
    void spinnerVerbsAllStrings() {
        for (Object item : SpinnerVerbs.SPINNER_VERBS) {
            assertTrue(item instanceof String);
        }
    }

    @Test
    @DisplayName("SpinnerVerbs SPINNER_VERBS no null elements")
    void spinnerVerbsNoNullElements() {
        for (String verb : SpinnerVerbs.SPINNER_VERBS) {
            assertNotNull(verb);
        }
    }

    @Test
    @DisplayName("SpinnerVerbs SPINNER_VERBS no empty strings")
    void spinnerVerbsNoEmptyStrings() {
        for (String verb : SpinnerVerbs.SPINNER_VERBS) {
            assertFalse(verb.isEmpty());
        }
    }

    @Test
    @DisplayName("SpinnerVerbs SPINNER_VERBS list is immutable")
    void spinnerVerbsImmutable() {
        // List.of() creates an immutable list
        assertThrows(UnsupportedOperationException.class, () -> {
            SpinnerVerbs.SPINNER_VERBS.add("NewVerb");
        });
    }
}