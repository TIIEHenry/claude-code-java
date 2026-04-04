/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code theme utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Theme configuration for terminal output colors.
 */
public final class Theme {
    private Theme() {}

    /**
     * Theme color names.
     */
    public enum ThemeName {
        DARK("dark"),
        LIGHT("light"),
        LIGHT_DALTONIZED("light-daltonized"),
        DARK_DALTONIZED("dark-daltonized"),
        LIGHT_ANSI("light-ansi"),
        DARK_ANSI("dark-ansi");

        private final String name;

        ThemeName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static ThemeName fromString(String name) {
            for (ThemeName theme : values()) {
                if (theme.name.equals(name)) {
                    return theme;
                }
            }
            return DARK; // Default
        }
    }

    /**
     * Theme colors record.
     */
    public record ThemeColors(
            String autoAccept,
            String bashBorder,
            String claude,
            String claudeShimmer,
            String permission,
            String permissionShimmer,
            String planMode,
            String ide,
            String promptBorder,
            String promptBorderShimmer,
            String text,
            String inverseText,
            String inactive,
            String inactiveShimmer,
            String subtle,
            String suggestion,
            String remember,
            String background,
            String success,
            String error,
            String warning,
            String warningShimmer,
            String diffAdded,
            String diffRemoved,
            String diffAddedDimmed,
            String diffRemovedDimmed
    ) {
        /**
         * Dark theme (default).
         */
        public static ThemeColors dark() {
            return new ThemeColors(
                    "#00ff00",     // autoAccept - green
                    "#444444",     // bashBorder
                    "#ff7700",     // claude - orange
                    "#ff9944",     // claudeShimmer
                    "#ff4444",     // permission - red
                    "#ff6666",     // permissionShimmer
                    "#aa44ff",     // planMode - purple
                    "#4488ff",     // ide - blue
                    "#00ff88",     // promptBorder - cyan
                    "#44ffaa",     // promptBorderShimmer
                    "#ffffff",     // text
                    "#000000",     // inverseText
                    "#666666",     // inactive
                    "#888888",     // inactiveShimmer
                    "#888888",     // subtle
                    "#4488ff",     // suggestion
                    "#ff8800",     // remember
                    "#1a1a1a",     // background
                    "#00ff00",     // success
                    "#ff4444",     // error
                    "#ffaa00",     // warning
                    "#ffcc44",     // warningShimmer
                    "#00ff00",     // diffAdded
                    "#ff0000",     // diffRemoved
                    "#008800",     // diffAddedDimmed
                    "#880000"      // diffRemovedDimmed
            );
        }

        /**
         * Light theme.
         */
        public static ThemeColors light() {
            return new ThemeColors(
                    "#008800",     // autoAccept
                    "#cccccc",     // bashBorder
                    "#cc5500",     // claude
                    "#ee7722",     // claudeShimmer
                    "#cc0000",     // permission
                    "#ee2222",     // permissionShimmer
                    "#7700cc",     // planMode
                    "#0055cc",     // ide
                    "#008855",     // promptBorder
                    "#22aa77",     // promptBorderShimmer
                    "#000000",     // text
                    "#ffffff",     // inverseText
                    "#888888",     // inactive
                    "#aaaaaa",     // inactiveShimmer
                    "#666666",     // subtle
                    "#0055cc",     // suggestion
                    "#cc6600",     // remember
                    "#ffffff",     // background
                    "#008800",     // success
                    "#cc0000",     // error
                    "#cc8800",     // warning
                    "#eeaa22",     // warningShimmer
                    "#008800",     // diffAdded
                    "#cc0000",     // diffRemoved
                    "#006600",     // diffAddedDimmed
                    "#880000"      // diffRemovedDimmed
            );
        }
    }

    private static volatile ThemeName currentTheme = ThemeName.DARK;
    private static volatile ThemeColors currentColors = ThemeColors.dark();

    /**
     * Get the current theme name.
     */
    public static ThemeName getCurrentThemeName() {
        return currentTheme;
    }

    /**
     * Set the current theme.
     */
    public static void setTheme(ThemeName theme) {
        currentTheme = theme;
        currentColors = switch (theme) {
            case LIGHT, LIGHT_DALTONIZED, LIGHT_ANSI -> ThemeColors.light();
            default -> ThemeColors.dark();
        };
    }

    /**
     * Get the current theme colors.
     */
    public static ThemeColors getColors() {
        return currentColors;
    }

    /**
     * Check if current theme is dark.
     */
    public static boolean isDarkTheme() {
        return currentTheme == ThemeName.DARK ||
               currentTheme == ThemeName.DARK_DALTONIZED ||
               currentTheme == ThemeName.DARK_ANSI;
    }

    /**
     * Apply ANSI color code to text.
     */
    public static String colorize(String text, String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            return text;
        }

        // Convert hex to RGB
        int r = Integer.parseInt(hexColor.substring(1, 3), 16);
        int g = Integer.parseInt(hexColor.substring(3, 5), 16);
        int b = Integer.parseInt(hexColor.substring(5, 7), 16);

        return String.format("\u001B[38;2;%d;%d;%dm%s\u001B[0m", r, g, b, text);
    }

    /**
     * Apply ANSI background color to text.
     */
    public static String backgroundize(String text, String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            return text;
        }

        int r = Integer.parseInt(hexColor.substring(1, 3), 16);
        int g = Integer.parseInt(hexColor.substring(3, 5), 16);
        int b = Integer.parseInt(hexColor.substring(5, 7), 16);

        return String.format("\u001B[48;2;%d;%d;%dm%s\u001B[0m", r, g, b, text);
    }

    /**
     * Reset all ANSI formatting.
     */
    public static String reset() {
        return "\u001B[0m";
    }

    /**
     * Bold text.
     */
    public static String bold(String text) {
        return "\u001B[1m" + text + "\u001B[0m";
    }

    /**
     * Dim text.
     */
    public static String dim(String text) {
        return "\u001B[2m" + text + "\u001B[0m";
    }

    /**
     * Italic text.
     */
    public static String italic(String text) {
        return "\u001B[3m" + text + "\u001B[0m";
    }

    /**
     * Underline text.
     */
    public static String underline(String text) {
        return "\u001B[4m" + text + "\u001B[0m";
    }
}