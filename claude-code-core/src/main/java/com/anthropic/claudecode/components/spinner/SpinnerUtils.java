/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/Spinner/utils
 */
package com.anthropic.claudecode.components.spinner;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spinner utilities - Color and character utilities for spinner.
 */
public final class SpinnerUtils {

    // RGB cache for performance
    private static final Map<String, RGBColor> RGB_CACHE = new ConcurrentHashMap<>();

    /**
     * Get default spinner characters.
     */
    public static List<String> getDefaultCharacters() {
        String term = System.getenv("TERM");
        String os = System.getProperty("os.name").toLowerCase();

        if ("xterm-ghostty".equals(term)) {
            return List.of("·", "✢", "✳", "✶", "✻", "*");
        }

        if (os.contains("mac")) {
            return List.of("·", "✢", "✳", "✶", "✻", "✽");
        }

        return List.of("·", "✢", "*", "✶", "✻", "✽");
    }

    /**
     * Interpolate between two RGB colors.
     */
    public static RGBColor interpolateColor(RGBColor color1, RGBColor color2, double t) {
        return new RGBColor(
            (int) Math.round(color1.r + (color2.r - color1.r) * t),
            (int) Math.round(color1.g + (color2.g - color1.g) * t),
            (int) Math.round(color1.b + (color2.b - color1.b) * t)
        );
    }

    /**
     * Convert RGB to ANSI escape code.
     */
    public static String toAnsiColor(RGBColor color) {
        return String.format("\u001B[38;2;%d;%d;%dm", color.r, color.g, color.b);
    }

    /**
     * Convert RGB to CSS rgb() string.
     */
    public static String toRgbString(RGBColor color) {
        return String.format("rgb(%d,%d,%d)", color.r, color.g, color.b);
    }

    /**
     * Convert HSL hue to RGB.
     */
    public static RGBColor hueToRgb(double hue) {
        double h = ((hue % 360) + 360) % 360;
        double s = 0.7;
        double l = 0.6;
        double c = (1 - Math.abs(2 * l - 1)) * s;
        double x = c * (1 - Math.abs(((h / 60) % 2) - 1));
        double m = l - c / 2;

        double r, g, b;

        if (h < 60) {
            r = c; g = x; b = 0;
        } else if (h < 120) {
            r = x; g = c; b = 0;
        } else if (h < 180) {
            r = 0; g = c; b = x;
        } else if (h < 240) {
            r = 0; g = x; b = c;
        } else if (h < 300) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }

        return new RGBColor(
            (int) Math.round((r + m) * 255),
            (int) Math.round((g + m) * 255),
            (int) Math.round((b + m) * 255)
        );
    }

    /**
     * Parse RGB string.
     */
    public static RGBColor parseRgb(String colorStr) {
        return RGB_CACHE.computeIfAbsent(colorStr, s -> {
            if (s == null) return null;

            // Match rgb(r, g, b) or rgb(r g b)
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "rgb\\(\\s*(\\d+)\\s*,?\\s*(\\d+)\\s*,?\\s*(\\d+)\\s*\\)"
            );
            java.util.regex.Matcher matcher = pattern.matcher(s);

            if (matcher.matches()) {
                return new RGBColor(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
                );
            }

            return null;
        });
    }

    /**
     * Get ANSI reset code.
     */
    public static String resetColor() {
        return "\u001B[0m";
    }

    /**
     * RGB color record.
     */
    public record RGBColor(int r, int g, int b) {}
}