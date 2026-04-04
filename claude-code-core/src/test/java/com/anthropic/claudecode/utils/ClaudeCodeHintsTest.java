/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClaudeCodeHints.
 */
class ClaudeCodeHintsTest {

    @BeforeEach
    void setUp() {
        ClaudeCodeHints.reset();
    }

    @Test
    @DisplayName("ClaudeCodeHints HintType enum")
    void hintTypeEnum() {
        ClaudeCodeHints.HintType[] types = ClaudeCodeHints.HintType.values();
        assertEquals(1, types.length);
        assertEquals(ClaudeCodeHints.HintType.PLUGIN, ClaudeCodeHints.HintType.valueOf("PLUGIN"));
    }

    @Test
    @DisplayName("ClaudeCodeHints ClaudeCodeHint record")
    void claudeCodeHintRecord() {
        ClaudeCodeHints.ClaudeCodeHint hint = new ClaudeCodeHints.ClaudeCodeHint(
            1, ClaudeCodeHints.HintType.PLUGIN, "test-value", "npm"
        );

        assertEquals(1, hint.v());
        assertEquals(ClaudeCodeHints.HintType.PLUGIN, hint.type());
        assertEquals("test-value", hint.value());
        assertEquals("npm", hint.sourceCommand());
    }

    @Test
    @DisplayName("ClaudeCodeHints extractClaudeCodeHints no hint")
    void extractNoHint() {
        String output = "Normal output without hints";
        ClaudeCodeHints.ExtractResult result = ClaudeCodeHints.extractClaudeCodeHints(output, "test");

        assertTrue(result.hints().isEmpty());
        assertEquals(output, result.stripped());
    }

    @Test
    @DisplayName("ClaudeCodeHints extractClaudeCodeHints valid hint")
    void extractValidHint() {
        String output = "<claude-code-hint v=\"1\" type=\"plugin\" value=\"my-plugin\" />\nNormal output";
        ClaudeCodeHints.ExtractResult result = ClaudeCodeHints.extractClaudeCodeHints(output, "npm install");

        assertEquals(1, result.hints().size());
        ClaudeCodeHints.ClaudeCodeHint hint = result.hints().get(0);
        assertEquals(1, hint.v());
        assertEquals(ClaudeCodeHints.HintType.PLUGIN, hint.type());
        assertEquals("my-plugin", hint.value());
        assertEquals("npm", hint.sourceCommand());
        assertTrue(result.stripped().contains("Normal output"));
    }

    @Test
    @DisplayName("ClaudeCodeHints extractClaudeCodeHints unsupported version")
    void extractUnsupportedVersion() {
        String output = "<claude-code-hint v=\"2\" type=\"plugin\" value=\"my-plugin\" />";
        ClaudeCodeHints.ExtractResult result = ClaudeCodeHints.extractClaudeCodeHints(output, "test");

        assertTrue(result.hints().isEmpty());
    }

    @Test
    @DisplayName("ClaudeCodeHints extractClaudeCodeHints unsupported type")
    void extractUnsupportedType() {
        String output = "<claude-code-hint v=\"1\" type=\"unknown\" value=\"my-plugin\" />";
        ClaudeCodeHints.ExtractResult result = ClaudeCodeHints.extractClaudeCodeHints(output, "test");

        assertTrue(result.hints().isEmpty());
    }

    @Test
    @DisplayName("ClaudeCodeHints extractClaudeCodeHints missing value")
    void extractMissingValue() {
        String output = "<claude-code-hint v=\"1\" type=\"plugin\" />";
        ClaudeCodeHints.ExtractResult result = ClaudeCodeHints.extractClaudeCodeHints(output, "test");

        assertTrue(result.hints().isEmpty());
    }

    @Test
    @DisplayName("ClaudeCodeHints extractClaudeCodeHints empty value")
    void extractEmptyValue() {
        String output = "<claude-code-hint v=\"1\" type=\"plugin\" value=\"\" />";
        ClaudeCodeHints.ExtractResult result = ClaudeCodeHints.extractClaudeCodeHints(output, "test");

        assertTrue(result.hints().isEmpty());
    }

    @Test
    @DisplayName("ClaudeCodeHints extractClaudeCodeHints with whitespace")
    void extractWithWhitespace() {
        String output = "  <claude-code-hint v=\"1\" type=\"plugin\" value=\"test\" />  ";
        ClaudeCodeHints.ExtractResult result = ClaudeCodeHints.extractClaudeCodeHints(output, "test");

        assertEquals(1, result.hints().size());
    }

    @Test
    @DisplayName("ClaudeCodeHints extractClaudeCodeHints multiple hints")
    void extractMultipleHints() {
        String output = "<claude-code-hint v=\"1\" type=\"plugin\" value=\"plugin1\" />\n" +
                        "<claude-code-hint v=\"1\" type=\"plugin\" value=\"plugin2\" />\n" +
                        "Normal output";
        ClaudeCodeHints.ExtractResult result = ClaudeCodeHints.extractClaudeCodeHints(output, "test");

        assertEquals(2, result.hints().size());
    }

    @Test
    @DisplayName("ClaudeCodeHints extractClaudeCodeHints null command")
    void extractNullCommand() {
        String output = "<claude-code-hint v=\"1\" type=\"plugin\" value=\"test\" />";
        ClaudeCodeHints.ExtractResult result = ClaudeCodeHints.extractClaudeCodeHints(output, null);

        assertEquals(1, result.hints().size());
        assertEquals("", result.hints().get(0).sourceCommand());
    }

    @Test
    @DisplayName("ClaudeCodeHints setPendingHint")
    void setPendingHint() {
        ClaudeCodeHints.ClaudeCodeHint hint = new ClaudeCodeHints.ClaudeCodeHint(
            1, ClaudeCodeHints.HintType.PLUGIN, "test", "npm"
        );

        ClaudeCodeHints.setPendingHint(hint);
        assertEquals(hint, ClaudeCodeHints.getPendingHintSnapshot());
    }

    @Test
    @DisplayName("ClaudeCodeHints clearPendingHint")
    void clearPendingHint() {
        ClaudeCodeHints.ClaudeCodeHint hint = new ClaudeCodeHints.ClaudeCodeHint(
            1, ClaudeCodeHints.HintType.PLUGIN, "test", "npm"
        );

        ClaudeCodeHints.setPendingHint(hint);
        assertNotNull(ClaudeCodeHints.getPendingHintSnapshot());

        ClaudeCodeHints.clearPendingHint();
        assertNull(ClaudeCodeHints.getPendingHintSnapshot());
    }

    @Test
    @DisplayName("ClaudeCodeHints markShownThisSession")
    void markShownThisSession() {
        assertFalse(ClaudeCodeHints.hasShownHintThisSession());

        ClaudeCodeHints.markShownThisSession();
        assertTrue(ClaudeCodeHints.hasShownHintThisSession());
    }

    @Test
    @DisplayName("ClaudeCodeHints setPendingHint after shown")
    void setPendingHintAfterShown() {
        ClaudeCodeHints.markShownThisSession();

        ClaudeCodeHints.ClaudeCodeHint hint = new ClaudeCodeHints.ClaudeCodeHint(
            1, ClaudeCodeHints.HintType.PLUGIN, "test", "npm"
        );

        ClaudeCodeHints.setPendingHint(hint);
        // Should not set since shownThisSession is true
        assertNull(ClaudeCodeHints.getPendingHintSnapshot());
    }

    @Test
    @DisplayName("ClaudeCodeHints subscribeToPendingHint")
    void subscribeToPendingHint() {
        int[] callCount = new int[]{0};
        ClaudeCodeHints.subscribeToPendingHint(() -> callCount[0]++);

        ClaudeCodeHints.ClaudeCodeHint hint = new ClaudeCodeHints.ClaudeCodeHint(
            1, ClaudeCodeHints.HintType.PLUGIN, "test", "npm"
        );

        ClaudeCodeHints.setPendingHint(hint);
        assertTrue(callCount[0] >= 1);

        ClaudeCodeHints.clearPendingHint();
        assertTrue(callCount[0] >= 2);
    }

    @Test
    @DisplayName("ClaudeCodeHints reset")
    void reset() {
        ClaudeCodeHints.ClaudeCodeHint hint = new ClaudeCodeHints.ClaudeCodeHint(
            1, ClaudeCodeHints.HintType.PLUGIN, "test", "npm"
        );

        ClaudeCodeHints.setPendingHint(hint);
        ClaudeCodeHints.markShownThisSession();

        ClaudeCodeHints.reset();

        assertNull(ClaudeCodeHints.getPendingHintSnapshot());
        assertFalse(ClaudeCodeHints.hasShownHintThisSession());
    }

    @Test
    @DisplayName("ClaudeCodeHints ExtractResult record")
    void extractResultRecord() {
        ClaudeCodeHints.ClaudeCodeHint hint = new ClaudeCodeHints.ClaudeCodeHint(
            1, ClaudeCodeHints.HintType.PLUGIN, "test", "npm"
        );

        ClaudeCodeHints.ExtractResult result = new ClaudeCodeHints.ExtractResult(
            List.of(hint), "stripped output"
        );

        assertEquals(1, result.hints().size());
        assertEquals("stripped output", result.stripped());
    }
}