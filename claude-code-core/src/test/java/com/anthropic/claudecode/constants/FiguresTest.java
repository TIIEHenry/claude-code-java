/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Figures.
 */
class FiguresTest {

    @Test
    @DisplayName("Figures BLACK_CIRCLE is non-null")
    void blackCircle() {
        assertNotNull(Figures.BLACK_CIRCLE);
        assertFalse(Figures.BLACK_CIRCLE.isEmpty());
    }

    @Test
    @DisplayName("Figures BULLET_OPERATOR is defined")
    void bulletOperator() {
        assertEquals("∙", Figures.BULLET_OPERATOR);
    }

    @Test
    @DisplayName("Figures TEARDROP_ASTERISK is defined")
    void teardropAsterisk() {
        assertEquals("✻", Figures.TEARDROP_ASTERISK);
    }

    @Test
    @DisplayName("Figures UP_ARROW is defined")
    void upArrow() {
        assertEquals("\u2191", Figures.UP_ARROW);
    }

    @Test
    @DisplayName("Figures DOWN_ARROW is defined")
    void downArrow() {
        assertEquals("\u2193", Figures.DOWN_ARROW);
    }

    @Test
    @DisplayName("Figures LIGHTNING_BOLT is defined")
    void lightningBolt() {
        assertEquals("↯", Figures.LIGHTNING_BOLT);
    }

    @Test
    @DisplayName("Figures REFRESH_ARROW is defined")
    void refreshArrow() {
        assertEquals("\u21bb", Figures.REFRESH_ARROW);
    }

    @Test
    @DisplayName("Figures EFFORT indicators are defined")
    void effortIndicators() {
        assertEquals("\u25cb", Figures.EFFORT_LOW);    // ○
        assertEquals("\u25d0", Figures.EFFORT_MEDIUM); // ◐
        assertEquals("\u25cf", Figures.EFFORT_HIGH);   // ●
        assertEquals("\u25c9", Figures.EFFORT_MAX);    // ◉
    }

    @Test
    @DisplayName("Figures PLAY_ICON is defined")
    void playIcon() {
        assertEquals("\u25b6", Figures.PLAY_ICON);
    }

    @Test
    @DisplayName("Figures PAUSE_ICON is defined")
    void pauseIcon() {
        assertEquals("\u23f8", Figures.PAUSE_ICON);
    }

    @Test
    @DisplayName("Figures FORK_GLYPH is defined")
    void forkGlyph() {
        assertEquals("\u2442", Figures.FORK_GLYPH);
    }

    @Test
    @DisplayName("Figures DIAMOND indicators are defined")
    void diamondIndicators() {
        assertEquals("\u25c7", Figures.DIAMOND_OPEN);   // ◇
        assertEquals("\u25c6", Figures.DIAMOND_FILLED); // ◆
        assertEquals("\u203b", Figures.REFERENCE_MARK); // ※
    }

    @Test
    @DisplayName("Figures FLAG_ICON is defined")
    void flagIcon() {
        assertEquals("\u2691", Figures.FLAG_ICON);
    }

    @Test
    @DisplayName("Figures BLOCKQUOTE_BAR is defined")
    void blockquoteBar() {
        assertEquals("\u258e", Figures.BLOCKQUOTE_BAR);
    }

    @Test
    @DisplayName("Figures HEAVY_HORIZONTAL is defined")
    void heavyHorizontal() {
        assertEquals("\u2501", Figures.HEAVY_HORIZONTAL);
    }

    @Test
    @DisplayName("Figures BRIDGE_SPINNER_FRAMES has 4 frames")
    void bridgeSpinnerFrames() {
        assertEquals(4, Figures.BRIDGE_SPINNER_FRAMES.length);
        assertEquals("·|·", Figures.BRIDGE_SPINNER_FRAMES[0]);
        assertEquals("·/·", Figures.BRIDGE_SPINNER_FRAMES[1]);
        assertEquals("·—·", Figures.BRIDGE_SPINNER_FRAMES[2]);
        assertEquals("·\\·", Figures.BRIDGE_SPINNER_FRAMES[3]);
    }

    @Test
    @DisplayName("Figures BRIDGE_READY_INDICATOR is defined")
    void bridgeReadyIndicator() {
        assertEquals("·✔︎·", Figures.BRIDGE_READY_INDICATOR);
    }

    @Test
    @DisplayName("Figures BRIDGE_FAILED_INDICATOR is defined")
    void bridgeFailedIndicator() {
        assertEquals("×", Figures.BRIDGE_FAILED_INDICATOR);
    }

    @Test
    @DisplayName("Figures all figures are single characters or short strings")
    void allFiguresAreShort() {
        assertTrue(Figures.UP_ARROW.length() <= 3);
        assertTrue(Figures.DOWN_ARROW.length() <= 3);
        assertTrue(Figures.PLAY_ICON.length() <= 3);
        assertTrue(Figures.PAUSE_ICON.length() <= 3);
    }
}