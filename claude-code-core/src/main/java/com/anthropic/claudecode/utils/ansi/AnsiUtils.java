/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/ansi
 */
package com.anthropic.claudecode.utils.ansi;

import java.util.*;
import java.util.regex.*;

/**
 * ANSI utils - ANSI escape code utilities.
 */
public final class AnsiUtils {
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\033\\[[0-9;]*m");
    private static final Pattern ANSI_CURSOR = Pattern.compile("\033\\[[0-9;]*[A-Za-z]");

    /**
     * ANSI color codes.
     */
    public static final class Colors {
        public static final String RESET = "\033[0m";
        public static final String BLACK = "\033[30m";
        public static final String RED = "\033[31m";
        public static final String GREEN = "\033[32m";
        public static final String YELLOW = "\033[33m";
        public static final String BLUE = "\033[34m";
        public static final String MAGENTA = "\033[35m";
        public static final String CYAN = "\033[36m";
        public static final String WHITE = "\033[37m";
        public static final String DEFAULT = "\033[39m";

        public static final String BG_BLACK = "\033[40m";
        public static final String BG_RED = "\033[41m";
        public static final String BG_GREEN = "\033[42m";
        public static final String BG_YELLOW = "\033[43m";
        public static final String BG_BLUE = "\033[44m";
        public static final String BG_MAGENTA = "\033[45m";
        public static final String BG_CYAN = "\033[46m";
        public static final String BG_WHITE = "\033[47m";
        public static final String BG_DEFAULT = "\033[49m";

        // Bright colors
        public static final String BRIGHT_BLACK = "\033[90m";
        public static final String BRIGHT_RED = "\033[91m";
        public static final String BRIGHT_GREEN = "\033[92m";
        public static final String BRIGHT_YELLOW = "\033[93m";
        public static final String BRIGHT_BLUE = "\033[94m";
        public static final String BRIGHT_MAGENTA = "\033[95m";
        public static final String BRIGHT_CYAN = "\033[96m";
        public static final String BRIGHT_WHITE = "\033[97m";
    }

    /**
     * ANSI styles.
     */
    public static final class Styles {
        public static final String BOLD = "\033[1m";
        public static final String DIM = "\033[2m";
        public static final String ITALIC = "\033[3m";
        public static final String UNDERLINE = "\033[4m";
        public static final String BLINK = "\033[5m";
        public static final String REVERSE = "\033[7m";
        public static final String HIDDEN = "\033[8m";
        public static final String STRIKETHROUGH = "\033[9m";

        public static final String NO_BOLD = "\033[22m";
        public static final String NO_DIM = "\033[22m";
        public static final String NO_ITALIC = "\033[23m";
        public static final String NO_UNDERLINE = "\033[24m";
        public static final String NO_BLINK = "\033[25m";
        public static final String NO_REVERSE = "\033[27m";
        public static final String NO_HIDDEN = "\033[28m";
        public static final String NO_STRIKETHROUGH = "\033[29m";
    }

    /**
     * Strip ANSI codes.
     */
    public static String stripAnsi(String text) {
        if (text == null) return null;
        return ANSI_ESCAPE.matcher(text).replaceAll("");
    }

    /**
     * Strip all ANSI codes.
     */
    public static String stripAllAnsi(String text) {
        if (text == null) return null;
        return text.replaceAll("\033\\[[0-9;]*[A-Za-z]", "");
    }

    /**
     * Get visible length.
     */
    public static int getVisibleLength(String text) {
        if (text == null) return 0;
        return stripAnsi(text).length();
    }

    /**
     * Color text.
     */
    public static String color(String text, String color) {
        return color + text + Colors.RESET;
    }

    /**
     * Color text with background.
     */
    public static String color(String text, String foreground, String background) {
        return foreground + background + text + Colors.RESET;
    }

    /**
     * Apply style.
     */
    public static String style(String text, String style) {
        return style + text + Colors.RESET;
    }

    /**
     * RGB color.
     */
    public static String rgb(int r, int g, int b) {
        return String.format("\033[38;2;%d;%d;%dm", r, g, b);
    }

    /**
     * RGB background.
     */
    public static String bgRgb(int r, int g, int b) {
        return String.format("\033[48;2;%d;%d;%dm", r, g, b);
    }

    /**
     * 256-color.
     */
    public static String color256(int code) {
        return String.format("\033[38;5;%dm", code);
    }

    /**
     * 256-color background.
     */
    public static String bg256(int code) {
        return String.format("\033[48;5;%dm", code);
    }

    /**
     * Move cursor.
     */
    public static String cursorUp(int n) {
        return String.format("\033[%dA", n);
    }

    public static String cursorDown(int n) {
        return String.format("\033[%dB", n);
    }

    public static String cursorForward(int n) {
        return String.format("\033[%dC", n);
    }

    public static String cursorBack(int n) {
        return String.format("\033[%dD", n);
    }

    public static String cursorTo(int row, int col) {
        return String.format("\033[%d;%dH", row, col);
    }

    public static String cursorSave() {
        return "\033[s";
    }

    public static String cursorRestore() {
        return "\033[u";
    }

    public static String cursorHide() {
        return "\033[?25l";
    }

    public static String cursorShow() {
        return "\033[?25h";
    }

    /**
     * Screen control.
     */
    public static String clearScreen() {
        return "\033[2J";
    }

    public static String clearLine() {
        return "\033[2K";
    }

    public static String clearToEndOfLine() {
        return "\033[K";
    }

    public static String clearToStartOfLine() {
        return "\033[1K";
    }

    /**
     * Link.
     */
    public static String link(String text, String url) {
        return String.format("\033]8;;%s\033\\%s\033]8;;\033\\", url, text);
    }

    /**
     * Check if supports ANSI.
     */
    public static boolean supportsAnsi() {
        String term = System.getenv("TERM");
        if (term != null && !term.isEmpty()) {
            return true;
        }

        String colorterm = System.getenv("COLORTERM");
        return colorterm != null && !colorterm.isEmpty();
    }

    /**
     * Convert to plain text if no ANSI support.
     */
    public static String toPlainIfNoSupport(String text) {
        if (!supportsAnsi()) {
            return stripAnsi(text);
        }
        return text;
    }

    /**
     * Truncate with ANSI awareness.
     */
    public static String truncate(String text, int maxLength) {
        if (text == null) return null;

        int visibleLength = getVisibleLength(text);
        if (visibleLength <= maxLength) {
            return text;
        }

        // Need to truncate while preserving ANSI codes
        StringBuilder result = new StringBuilder();
        int visibleCount = 0;
        boolean inEscape = false;

        for (int i = 0; i < text.length() && visibleCount < maxLength; i++) {
            char c = text.charAt(i);

            if (c == '\033') {
                inEscape = true;
                result.append(c);
            } else if (inEscape) {
                result.append(c);
                if (c == 'm') {
                    inEscape = false;
                }
            } else {
                result.append(c);
                visibleCount++;
            }
        }

        result.append(Colors.RESET);
        return result.toString();
    }
}