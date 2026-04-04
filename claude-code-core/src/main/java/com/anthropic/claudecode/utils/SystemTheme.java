/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code system theme utilities
 */
package com.anthropic.claudecode.utils;

/**
 * Terminal dark/light mode detection for the 'auto' theme setting.
 *
 * Detection is based on the terminal's actual background color (queried via
 * OSC 11) rather than the OS appearance setting.
 */
public final class SystemTheme {
    private SystemTheme() {}

    /**
     * System theme enum.
     */
    public enum Theme {
        DARK, LIGHT
    }

    // Cached theme
    private static volatile Theme cachedSystemTheme = null;

    /**
     * Get the current terminal theme. Cached after first detection.
     */
    public static Theme getSystemThemeName() {
        if (cachedSystemTheme == null) {
            cachedSystemTheme = detectFromColorFgBg();
            if (cachedSystemTheme == null) {
                cachedSystemTheme = Theme.DARK; // Default
            }
        }
        return cachedSystemTheme;
    }

    /**
     * Update the cached terminal theme.
     */
    public static void setCachedSystemTheme(Theme theme) {
        cachedSystemTheme = theme;
    }

    /**
     * Resolve a theme setting to a concrete theme.
     */
    public static Theme resolveThemeSetting(String setting) {
        if ("auto".equalsIgnoreCase(setting)) {
            return getSystemThemeName();
        }
        if ("light".equalsIgnoreCase(setting) ||
            "light-daltonized".equalsIgnoreCase(setting) ||
            "light-ansi".equalsIgnoreCase(setting)) {
            return Theme.LIGHT;
        }
        return Theme.DARK;
    }

    /**
     * Parse an OSC color response data string into a theme.
     *
     * Accepts XParseColor formats returned by OSC 10/11 queries:
     * - `rgb:R/G/B` where each component is 1–4 hex digits
     * - `#RRGGBB` / `#RRRRGGGGBBBB`
     *
     * Returns null for unrecognized formats.
     */
    public static Theme themeFromOscColor(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        double[] rgb = parseOscRgb(data);
        if (rgb == null) {
            return null;
        }

        // ITU-R BT.709 relative luminance. Midpoint split: > 0.5 is light.
        double luminance = 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
        return luminance > 0.5 ? Theme.LIGHT : Theme.DARK;
    }

    /**
     * Parse OSC RGB data.
     */
    private static double[] parseOscRgb(String data) {
        // rgb:RRRR/GGGG/BBBB — each component is 1–4 hex digits.
        // Some terminals append an alpha component (rgba:…/…/…/…); ignore it.
        java.util.regex.Pattern rgbPattern = java.util.regex.Pattern.compile(
                "^rgba?:([0-9a-f]{1,4})/([0-9a-f]{1,4})/([0-9a-f]{1,4})",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher rgbMatcher = rgbPattern.matcher(data);
        if (rgbMatcher.find()) {
            return new double[]{
                    hexComponent(rgbMatcher.group(1)),
                    hexComponent(rgbMatcher.group(2)),
                    hexComponent(rgbMatcher.group(3))
            };
        }

        // #RRGGBB or #RRRRGGGGBBBB — split into three equal hex runs.
        java.util.regex.Pattern hashPattern = java.util.regex.Pattern.compile(
                "^#([0-9a-f]+)$",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher hashMatcher = hashPattern.matcher(data);
        if (hashMatcher.find() && hashMatcher.group(1).length() % 3 == 0) {
            String hex = hashMatcher.group(1);
            int n = hex.length() / 3;
            return new double[]{
                    hexComponent(hex.substring(0, n)),
                    hexComponent(hex.substring(n, 2 * n)),
                    hexComponent(hex.substring(2 * n))
            };
        }

        return null;
    }

    /**
     * Normalize a 1–4 digit hex component to [0, 1].
     */
    private static double hexComponent(String hex) {
        int max = (int) Math.pow(16, hex.length()) - 1;
        return (double) Integer.parseInt(hex, 16) / max;
    }

    /**
     * Read $COLORFGBG for a synchronous initial guess before the OSC 11
     * round-trip completes. Format is `fg;bg` (or `fg;other;bg`) where values
     * are ANSI color indices.
     */
    private static Theme detectFromColorFgBg() {
        String colorfgbg = System.getenv("COLORFGBG");
        if (colorfgbg == null || colorfgbg.isEmpty()) {
            return null;
        }

        String[] parts = colorfgbg.split(";");
        if (parts.length == 0) {
            return null;
        }

        String bg = parts[parts.length - 1];
        if (bg == null || bg.isEmpty()) {
            return null;
        }

        try {
            int bgNum = Integer.parseInt(bg);
            if (bgNum < 0 || bgNum > 15) {
                return null;
            }
            // 0–6 and 8 are dark ANSI colors; 7 (white) and 9–15 (bright) are light.
            return (bgNum <= 6 || bgNum == 8) ? Theme.DARK : Theme.LIGHT;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Check if current theme is dark.
     */
    public static boolean isDarkTheme() {
        return getSystemThemeName() == Theme.DARK;
    }

    /**
     * Check if current theme is light.
     */
    public static boolean isLightTheme() {
        return getSystemThemeName() == Theme.LIGHT;
    }
}