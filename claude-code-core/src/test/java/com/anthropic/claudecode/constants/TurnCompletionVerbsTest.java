/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TurnCompletionVerbs constants.
 */
class TurnCompletionVerbsTest {

    @Test
    @DisplayName("TurnCompletionVerbs TURN_COMPLETION_VERBS not null")
    void turnCompletionVerbsNotNull() {
        assertNotNull(TurnCompletionVerbs.TURN_COMPLETION_VERBS);
    }

    @Test
    @DisplayName("TurnCompletionVerbs TURN_COMPLETION_VERBS not empty")
    void turnCompletionVerbsNotEmpty() {
        assertFalse(TurnCompletionVerbs.TURN_COMPLETION_VERBS.isEmpty());
    }

    @Test
    @DisplayName("TurnCompletionVerbs TURN_COMPLETION_VERBS contains expected verbs")
    void turnCompletionVerbsContainsExpected() {
        assertTrue(TurnCompletionVerbs.TURN_COMPLETION_VERBS.contains("Worked"));
        assertTrue(TurnCompletionVerbs.TURN_COMPLETION_VERBS.contains("Cooked"));
        assertTrue(TurnCompletionVerbs.TURN_COMPLETION_VERBS.contains("Baked"));
        assertTrue(TurnCompletionVerbs.TURN_COMPLETION_VERBS.contains("Brewed"));
    }

    @Test
    @DisplayName("TurnCompletionVerbs TURN_COMPLETION_VERBS count")
    void turnCompletionVerbsCount() {
        assertEquals(8, TurnCompletionVerbs.TURN_COMPLETION_VERBS.size());
    }

    @Test
    @DisplayName("TurnCompletionVerbs all verbs are non-empty")
    void allVerbsNonEmpty() {
        for (String verb : TurnCompletionVerbs.TURN_COMPLETION_VERBS) {
            assertFalse(verb.isEmpty());
        }
    }

    @Test
    @DisplayName("TurnCompletionVerbs list is immutable")
    void turnCompletionVerbsImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> {
            TurnCompletionVerbs.TURN_COMPLETION_VERBS.add("NewVerb");
        });
    }
}