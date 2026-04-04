/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/computerUse/common
 */
package com.anthropic.claudecode.utils.computerUse;

import java.util.*;

/**
 * Computer use common - Common computer use utilities.
 */
public final class ComputerUseCommon {
    private static volatile boolean computerUseEnabled = false;
    private static volatile String screenDimensions = "1920x1080";
    private static volatile double scaleFactor = 1.0;

    /**
     * Check if computer use is enabled.
     */
    public static boolean isEnabled() {
        return computerUseEnabled;
    }

    /**
     * Enable/disable computer use.
     */
    public static void setEnabled(boolean enabled) {
        computerUseEnabled = enabled;
    }

    /**
     * Get screen dimensions.
     */
    public static String getScreenDimensions() {
        return screenDimensions;
    }

    /**
     * Set screen dimensions.
     */
    public static void setScreenDimensions(int width, int height) {
        screenDimensions = width + "x" + height;
    }

    /**
     * Get scale factor.
     */
    public static double getScaleFactor() {
        return scaleFactor;
    }

    /**
     * Set scale factor.
     */
    public static void setScaleFactor(double factor) {
        scaleFactor = factor;
    }

    /**
     * Parse dimensions string.
     */
    public static Dimensions parseDimensions(String dims) {
        if (dims == null || !dims.contains("x")) {
            return new Dimensions(1920, 1080);
        }

        String[] parts = dims.split("x");
        int width = Integer.parseInt(parts[0]);
        int height = Integer.parseInt(parts[1]);

        return new Dimensions(width, height);
    }

    /**
     * Dimensions record.
     */
    public record Dimensions(int width, int height) {
        public int getTotalPixels() {
            return width * height;
        }

        public double getAspectRatio() {
            return (double) width / height;
        }

        public String format() {
            return width + "x" + height;
        }
    }

    /**
     * Coordinate record.
     */
    public record Coordinate(int x, int y) {
        public Coordinate scaled(double factor) {
            return new Coordinate((int)(x * factor), (int)(y * factor));
        }

        public Coordinate unscaled(double factor) {
            return new Coordinate((int)(x / factor), (int)(y / factor));
        }

        public boolean isInBounds(Dimensions dims) {
            return x >= 0 && x < dims.width() && y >= 0 && y < dims.height();
        }
    }

    /**
     * Action type enum.
     */
    public enum ActionType {
        KEY,
        TYPE,
        MOUSE_MOVE,
        LEFT_CLICK,
        RIGHT_CLICK,
        DOUBLE_CLICK,
        SCROLL,
        SCREENSHOT,
        CURSOR_POSITION
    }

    /**
     * Computer action record.
     */
    public record ComputerAction(
        ActionType action,
        Coordinate coordinate,
        String text,
        List<String> keys,
        int scrollDirection,
        int scrollAmount
    ) {
        public static ComputerAction keyPress(List<String> keys) {
            return new ComputerAction(ActionType.KEY, null, null, keys, 0, 0);
        }

        public static ComputerAction typeText(String text) {
            return new ComputerAction(ActionType.TYPE, null, text, null, 0, 0);
        }

        public static ComputerAction mouseMove(int x, int y) {
            return new ComputerAction(ActionType.MOUSE_MOVE, new Coordinate(x, y), null, null, 0, 0);
        }

        public static ComputerAction leftClick(int x, int y) {
            return new ComputerAction(ActionType.LEFT_CLICK, new Coordinate(x, y), null, null, 0, 0);
        }

        public static ComputerAction rightClick(int x, int y) {
            return new ComputerAction(ActionType.RIGHT_CLICK, new Coordinate(x, y), null, null, 0, 0);
        }

        public static ComputerAction doubleClick(int x, int y) {
            return new ComputerAction(ActionType.DOUBLE_CLICK, new Coordinate(x, y), null, null, 0, 0);
        }

        public static ComputerAction screenshot() {
            return new ComputerAction(ActionType.SCREENSHOT, null, null, null, 0, 0);
        }

        public static ComputerAction scroll(int direction, int amount) {
            return new ComputerAction(ActionType.SCROLL, null, null, null, direction, amount);
        }
    }
}