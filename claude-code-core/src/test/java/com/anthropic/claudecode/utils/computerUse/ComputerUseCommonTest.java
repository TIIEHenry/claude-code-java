/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.computerUse;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for ComputerUseCommon.
 */
@DisplayName("ComputerUseCommon Tests")
class ComputerUseCommonTest {

    @BeforeEach
    void setUp() {
        ComputerUseCommon.setEnabled(false);
        ComputerUseCommon.setScreenDimensions(1920, 1080);
        ComputerUseCommon.setScaleFactor(1.0);
    }

    @Test
    @DisplayName("ComputerUseCommon isEnabled starts false")
    void isEnabledStartsFalse() {
        assertFalse(ComputerUseCommon.isEnabled());
    }

    @Test
    @DisplayName("ComputerUseCommon setEnabled changes enabled state")
    void setEnabledChangesEnabledState() {
        ComputerUseCommon.setEnabled(true);
        assertTrue(ComputerUseCommon.isEnabled());

        ComputerUseCommon.setEnabled(false);
        assertFalse(ComputerUseCommon.isEnabled());
    }

    @Test
    @DisplayName("ComputerUseCommon getScreenDimensions returns default")
    void getScreenDimensionsReturnsDefault() {
        assertEquals("1920x1080", ComputerUseCommon.getScreenDimensions());
    }

    @Test
    @DisplayName("ComputerUseCommon setScreenDimensions changes dimensions")
    void setScreenDimensionsChangesDimensions() {
        ComputerUseCommon.setScreenDimensions(2560, 1440);
        assertEquals("2560x1440", ComputerUseCommon.getScreenDimensions());
    }

    @Test
    @DisplayName("ComputerUseCommon getScaleFactor returns default")
    void getScaleFactorReturnsDefault() {
        assertEquals(1.0, ComputerUseCommon.getScaleFactor());
    }

    @Test
    @DisplayName("ComputerUseCommon setScaleFactor changes factor")
    void setScaleFactorChangesFactor() {
        ComputerUseCommon.setScaleFactor(2.0);
        assertEquals(2.0, ComputerUseCommon.getScaleFactor());
    }

    @Test
    @DisplayName("ComputerUseCommon parseDimensions parses valid string")
    void parseDimensionsParsesValidString() {
        ComputerUseCommon.Dimensions dims = ComputerUseCommon.parseDimensions("1920x1080");

        assertEquals(1920, dims.width());
        assertEquals(1080, dims.height());
    }

    @Test
    @DisplayName("ComputerUseCommon parseDimensions returns default for null")
    void parseDimensionsReturnsDefaultForNull() {
        ComputerUseCommon.Dimensions dims = ComputerUseCommon.parseDimensions(null);

        assertEquals(1920, dims.width());
        assertEquals(1080, dims.height());
    }

    @Test
    @DisplayName("ComputerUseCommon parseDimensions returns default for invalid")
    void parseDimensionsReturnsDefaultForInvalid() {
        ComputerUseCommon.Dimensions dims = ComputerUseCommon.parseDimensions("invalid");

        assertEquals(1920, dims.width());
        assertEquals(1080, dims.height());
    }

    @Test
    @DisplayName("ComputerUseCommon Dimensions getTotalPixels works correctly")
    void dimensionsGetTotalPixelsWorksCorrectly() {
        ComputerUseCommon.Dimensions dims = new ComputerUseCommon.Dimensions(1920, 1080);

        assertEquals(2073600, dims.getTotalPixels());
    }

    @Test
    @DisplayName("ComputerUseCommon Dimensions getAspectRatio works correctly")
    void dimensionsGetAspectRatioWorksCorrectly() {
        ComputerUseCommon.Dimensions dims = new ComputerUseCommon.Dimensions(1920, 1080);

        assertEquals(16.0 / 9.0, dims.getAspectRatio(), 0.001);
    }

    @Test
    @DisplayName("ComputerUseCommon Dimensions format works correctly")
    void dimensionsFormatWorksCorrectly() {
        ComputerUseCommon.Dimensions dims = new ComputerUseCommon.Dimensions(2560, 1440);

        assertEquals("2560x1440", dims.format());
    }

    @Test
    @DisplayName("ComputerUseCommon Coordinate scaled works correctly")
    void coordinateScaledWorksCorrectly() {
        ComputerUseCommon.Coordinate coord = new ComputerUseCommon.Coordinate(100, 200);

        ComputerUseCommon.Coordinate scaled = coord.scaled(2.0);

        assertEquals(200, scaled.x());
        assertEquals(400, scaled.y());
    }

    @Test
    @DisplayName("ComputerUseCommon Coordinate unscaled works correctly")
    void coordinateUnscaledWorksCorrectly() {
        ComputerUseCommon.Coordinate coord = new ComputerUseCommon.Coordinate(200, 400);

        ComputerUseCommon.Coordinate unscaled = coord.unscaled(2.0);

        assertEquals(100, unscaled.x());
        assertEquals(200, unscaled.y());
    }

    @Test
    @DisplayName("ComputerUseCommon Coordinate isInBounds works correctly")
    void coordinateIsInBoundsWorksCorrectly() {
        ComputerUseCommon.Dimensions dims = new ComputerUseCommon.Dimensions(1920, 1080);

        assertTrue(new ComputerUseCommon.Coordinate(0, 0).isInBounds(dims));
        assertTrue(new ComputerUseCommon.Coordinate(1919, 1079).isInBounds(dims));
        assertFalse(new ComputerUseCommon.Coordinate(1920, 1080).isInBounds(dims));
        assertFalse(new ComputerUseCommon.Coordinate(-1, 0).isInBounds(dims));
    }

    @Test
    @DisplayName("ComputerUseCommon ActionType enum has correct values")
    void actionTypeEnumHasCorrectValues() {
        ComputerUseCommon.ActionType[] types = ComputerUseCommon.ActionType.values();

        assertEquals(9, types.length);
        assertTrue(Arrays.asList(types).contains(ComputerUseCommon.ActionType.KEY));
        assertTrue(Arrays.asList(types).contains(ComputerUseCommon.ActionType.TYPE));
        assertTrue(Arrays.asList(types).contains(ComputerUseCommon.ActionType.MOUSE_MOVE));
        assertTrue(Arrays.asList(types).contains(ComputerUseCommon.ActionType.LEFT_CLICK));
        assertTrue(Arrays.asList(types).contains(ComputerUseCommon.ActionType.RIGHT_CLICK));
        assertTrue(Arrays.asList(types).contains(ComputerUseCommon.ActionType.DOUBLE_CLICK));
        assertTrue(Arrays.asList(types).contains(ComputerUseCommon.ActionType.SCROLL));
        assertTrue(Arrays.asList(types).contains(ComputerUseCommon.ActionType.SCREENSHOT));
        assertTrue(Arrays.asList(types).contains(ComputerUseCommon.ActionType.CURSOR_POSITION));
    }

    @Test
    @DisplayName("ComputerUseCommon ComputerAction keyPress factory works")
    void computerActionKeyPressFactoryWorks() {
        ComputerUseCommon.ComputerAction action = ComputerUseCommon.ComputerAction.keyPress(List.of("Cmd", "C"));

        assertEquals(ComputerUseCommon.ActionType.KEY, action.action());
        assertEquals(2, action.keys().size());
        assertNull(action.coordinate());
        assertNull(action.text());
    }

    @Test
    @DisplayName("ComputerUseCommon ComputerAction typeText factory works")
    void computerActionTypeTextFactoryWorks() {
        ComputerUseCommon.ComputerAction action = ComputerUseCommon.ComputerAction.typeText("Hello");

        assertEquals(ComputerUseCommon.ActionType.TYPE, action.action());
        assertEquals("Hello", action.text());
        assertNull(action.coordinate());
        assertNull(action.keys());
    }

    @Test
    @DisplayName("ComputerUseCommon ComputerAction mouseMove factory works")
    void computerActionMouseMoveFactoryWorks() {
        ComputerUseCommon.ComputerAction action = ComputerUseCommon.ComputerAction.mouseMove(100, 200);

        assertEquals(ComputerUseCommon.ActionType.MOUSE_MOVE, action.action());
        assertEquals(100, action.coordinate().x());
        assertEquals(200, action.coordinate().y());
        assertNull(action.text());
        assertNull(action.keys());
    }

    @Test
    @DisplayName("ComputerUseCommon ComputerAction leftClick factory works")
    void computerActionLeftClickFactoryWorks() {
        ComputerUseCommon.ComputerAction action = ComputerUseCommon.ComputerAction.leftClick(50, 100);

        assertEquals(ComputerUseCommon.ActionType.LEFT_CLICK, action.action());
        assertEquals(50, action.coordinate().x());
        assertEquals(100, action.coordinate().y());
    }

    @Test
    @DisplayName("ComputerUseCommon ComputerAction rightClick factory works")
    void computerActionRightClickFactoryWorks() {
        ComputerUseCommon.ComputerAction action = ComputerUseCommon.ComputerAction.rightClick(50, 100);

        assertEquals(ComputerUseCommon.ActionType.RIGHT_CLICK, action.action());
        assertEquals(50, action.coordinate().x());
        assertEquals(100, action.coordinate().y());
    }

    @Test
    @DisplayName("ComputerUseCommon ComputerAction doubleClick factory works")
    void computerActionDoubleClickFactoryWorks() {
        ComputerUseCommon.ComputerAction action = ComputerUseCommon.ComputerAction.doubleClick(50, 100);

        assertEquals(ComputerUseCommon.ActionType.DOUBLE_CLICK, action.action());
        assertEquals(50, action.coordinate().x());
        assertEquals(100, action.coordinate().y());
    }

    @Test
    @DisplayName("ComputerUseCommon ComputerAction screenshot factory works")
    void computerActionScreenshotFactoryWorks() {
        ComputerUseCommon.ComputerAction action = ComputerUseCommon.ComputerAction.screenshot();

        assertEquals(ComputerUseCommon.ActionType.SCREENSHOT, action.action());
        assertNull(action.coordinate());
        assertNull(action.text());
        assertNull(action.keys());
    }

    @Test
    @DisplayName("ComputerUseCommon ComputerAction scroll factory works")
    void computerActionScrollFactoryWorks() {
        ComputerUseCommon.ComputerAction action = ComputerUseCommon.ComputerAction.scroll(1, 5);

        assertEquals(ComputerUseCommon.ActionType.SCROLL, action.action());
        assertEquals(1, action.scrollDirection());
        assertEquals(5, action.scrollAmount());
    }

    @Test
    @DisplayName("ComputerUseCommon ComputerAction record works with all parameters")
    void computerActionRecordWorksWithAllParameters() {
        ComputerUseCommon.ComputerAction action = new ComputerUseCommon.ComputerAction(
            ComputerUseCommon.ActionType.KEY,
            new ComputerUseCommon.Coordinate(10, 20),
            "text",
            List.of("A", "B"),
            1,
            10
        );

        assertEquals(ComputerUseCommon.ActionType.KEY, action.action());
        assertEquals(10, action.coordinate().x());
        assertEquals("text", action.text());
        assertEquals(2, action.keys().size());
        assertEquals(1, action.scrollDirection());
        assertEquals(10, action.scrollAmount());
    }
}