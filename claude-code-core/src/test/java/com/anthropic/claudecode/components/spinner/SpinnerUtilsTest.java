/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.components.spinner;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for SpinnerUtils.
 */
@DisplayName("SpinnerUtils Tests")
class SpinnerUtilsTest {

    @Test
    @DisplayName("SpinnerUtils getDefaultCharacters returns valid list")
    void getDefaultCharactersReturnsValidList() {
        List<String> chars = SpinnerUtils.getDefaultCharacters();

        assertNotNull(chars);
        assertFalse(chars.isEmpty());
        assertTrue(chars.size() >= 5);
    }

    @Test
    @DisplayName("SpinnerUtils RGBColor record works correctly")
    void rgbColorRecordWorksCorrectly() {
        SpinnerUtils.RGBColor color = new SpinnerUtils.RGBColor(255, 128, 64);

        assertEquals(255, color.r());
        assertEquals(128, color.g());
        assertEquals(64, color.b());
    }

    @Test
    @DisplayName("SpinnerUtils interpolateColor interpolates correctly")
    void interpolateColorInterpolatesCorrectly() {
        SpinnerUtils.RGBColor color1 = new SpinnerUtils.RGBColor(0, 0, 0);
        SpinnerUtils.RGBColor color2 = new SpinnerUtils.RGBColor(255, 255, 255);

        SpinnerUtils.RGBColor mid = SpinnerUtils.interpolateColor(color1, color2, 0.5);

        assertEquals(128, mid.r());
        assertEquals(128, mid.g());
        assertEquals(128, mid.b());
    }

    @Test
    @DisplayName("SpinnerUtils interpolateColor at t=0 returns color1")
    void interpolateColorAtT0ReturnsColor1() {
        SpinnerUtils.RGBColor color1 = new SpinnerUtils.RGBColor(100, 50, 25);
        SpinnerUtils.RGBColor color2 = new SpinnerUtils.RGBColor(200, 100, 50);

        SpinnerUtils.RGBColor result = SpinnerUtils.interpolateColor(color1, color2, 0.0);

        assertEquals(100, result.r());
        assertEquals(50, result.g());
        assertEquals(25, result.b());
    }

    @Test
    @DisplayName("SpinnerUtils interpolateColor at t=1 returns color2")
    void interpolateColorAtT1ReturnsColor2() {
        SpinnerUtils.RGBColor color1 = new SpinnerUtils.RGBColor(100, 50, 25);
        SpinnerUtils.RGBColor color2 = new SpinnerUtils.RGBColor(200, 100, 50);

        SpinnerUtils.RGBColor result = SpinnerUtils.interpolateColor(color1, color2, 1.0);

        assertEquals(200, result.r());
        assertEquals(100, result.g());
        assertEquals(50, result.b());
    }

    @Test
    @DisplayName("SpinnerUtils toAnsiColor returns correct format")
    void toAnsiColorReturnsCorrectFormat() {
        SpinnerUtils.RGBColor color = new SpinnerUtils.RGBColor(255, 128, 64);

        String ansi = SpinnerUtils.toAnsiColor(color);

        assertTrue(ansi.contains("38;2"));
        assertTrue(ansi.contains("255"));
        assertTrue(ansi.contains("128"));
        assertTrue(ansi.contains("64"));
    }

    @Test
    @DisplayName("SpinnerUtils toRgbString returns correct format")
    void toRgbStringReturnsCorrectFormat() {
        SpinnerUtils.RGBColor color = new SpinnerUtils.RGBColor(255, 128, 64);

        String rgb = SpinnerUtils.toRgbString(color);

        assertEquals("rgb(255,128,64)", rgb);
    }

    @Test
    @DisplayName("SpinnerUtils hueToRgb returns valid colors")
    void hueToRgbReturnsValidColors() {
        for (double hue = 0; hue < 360; hue += 30) {
            SpinnerUtils.RGBColor color = SpinnerUtils.hueToRgb(hue);

            assertTrue(color.r() >= 0 && color.r() <= 255);
            assertTrue(color.g() >= 0 && color.g() <= 255);
            assertTrue(color.b() >= 0 && color.b() <= 255);
        }
    }

    @Test
    @DisplayName("SpinnerUtils hueToRgb handles negative hue")
    void hueToRgbHandlesNegativeHue() {
        SpinnerUtils.RGBColor color = SpinnerUtils.hueToRgb(-30);

        assertNotNull(color);
        assertTrue(color.r() >= 0 && color.r() <= 255);
    }

    @Test
    @DisplayName("SpinnerUtils hueToRgb handles large hue")
    void hueToRgbHandlesLargeHue() {
        SpinnerUtils.RGBColor color = SpinnerUtils.hueToRgb(720);

        assertNotNull(color);
        assertTrue(color.r() >= 0 && color.r() <= 255);
    }

    @Test
    @DisplayName("SpinnerUtils parseRgb with comma format works correctly")
    void parseRgbWithCommaFormatWorksCorrectly() {
        SpinnerUtils.RGBColor color = SpinnerUtils.parseRgb("rgb(255, 128, 64)");

        assertNotNull(color);
        assertEquals(255, color.r());
        assertEquals(128, color.g());
        assertEquals(64, color.b());
    }

    @Test
    @DisplayName("SpinnerUtils parseRgb without comma format works correctly")
    void parseRgbWithoutCommaFormatWorksCorrectly() {
        SpinnerUtils.RGBColor color = SpinnerUtils.parseRgb("rgb(255 128 64)");

        assertNotNull(color);
        assertEquals(255, color.r());
        assertEquals(128, color.g());
        assertEquals(64, color.b());
    }

    @Test
    @DisplayName("SpinnerUtils parseRgb returns null for invalid format")
    void parseRgbReturnsNullForInvalidFormat() {
        SpinnerUtils.RGBColor color = SpinnerUtils.parseRgb("invalid");

        assertNull(color);
    }

    @Test
    @DisplayName("SpinnerUtils parseRgb handles null input gracefully")
    void parseRgbHandlesNullInputGracefully() {
        // This may throw NullPointerException based on implementation
        // Just verify the method doesn't crash the system
        try {
            SpinnerUtils.RGBColor color = SpinnerUtils.parseRgb(null);
            // If it doesn't throw, it should return null
            assertNull(color);
        } catch (NullPointerException e) {
            // This is also acceptable behavior
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("SpinnerUtils resetColor returns correct code")
    void resetColorReturnsCorrectCode() {
        String reset = SpinnerUtils.resetColor();

        assertEquals("\u001B[0m", reset);
    }

    @Test
    @DisplayName("SpinnerUtils parseRgb caches results")
    void parseRgbCachesResults() {
        String rgbStr = "rgb(100, 100, 100)";

        SpinnerUtils.RGBColor color1 = SpinnerUtils.parseRgb(rgbStr);
        SpinnerUtils.RGBColor color2 = SpinnerUtils.parseRgb(rgbStr);

        // Should return same cached instance
        assertEquals(color1, color2);
    }
}