/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code hyperlink utilities
 */
package com.anthropic.claudecode.utils;

/**
 * OSC 8 hyperlink escape sequences for terminal hyperlinks.
 * Format: ESC]8;;URL BEL TEXT ESC]8;; BEL
 */
public final class Hyperlink {
    private Hyperlink() {}

    // OSC 8 escape sequences
    public static final String OSC8_START = "\u001b]8;;";
    public static final String OSC8_END = "\u0007"; // BEL character

    // ANSI color codes
    private static final String ANSI_BLUE = "\u001b[34m";
    private static final String ANSI_RESET = "\u001b[0m";

    /**
     * Check if the terminal supports hyperlinks.
     */
    public static boolean supportsHyperlinks() {
        // Check for terminals known to support OSC 8
        String term = System.getenv("TERM");
        String termProgram = System.getenv("TERM_PROGRAM");

        // iTerm2, WezTerm, Kitty, and modern VTE-based terminals support OSC 8
        if (termProgram != null) {
            return termProgram.equals("iTerm.app") ||
                   termProgram.equals("WezTerm") ||
                   termProgram.equals("kitty");
        }

        if (term != null) {
            return term.contains("xterm") ||
                   term.contains("vte") ||
                   term.contains("kitty");
        }

        return false;
    }

    /**
     * Apply ANSI blue color to text.
     */
    public static String blue(String text) {
        return ANSI_BLUE + text + ANSI_RESET;
    }

    /**
     * Create a clickable hyperlink using OSC 8 escape sequences.
     * Falls back to plain text if the terminal doesn't support hyperlinks.
     *
     * @param url The URL to link to
     * @return The hyperlink or plain URL
     */
    public static String createHyperlink(String url) {
        return createHyperlink(url, null);
    }

    /**
     * Create a clickable hyperlink with custom display text.
     *
     * @param url The URL to link to
     * @param content Optional content to display as the link text
     * @return The hyperlink or plain URL
     */
    public static String createHyperlink(String url, String content) {
        return createHyperlink(url, content, null);
    }

    /**
     * Create a clickable hyperlink with options.
     *
     * @param url The URL to link to
     * @param content Optional content to display as the link text
     * @param supportsHyperlinksOverride Override for testing
     * @return The hyperlink or plain URL
     */
    public static String createHyperlink(String url, String content, Boolean supportsHyperlinksOverride) {
        boolean hasSupport = supportsHyperlinksOverride != null
                ? supportsHyperlinksOverride
                : supportsHyperlinks();

        if (!hasSupport) {
            return url;
        }

        String displayText = content != null ? content : url;
        // Apply ANSI blue color
        String coloredText = blue(displayText);
        return OSC8_START + url + OSC8_END + coloredText + OSC8_START + OSC8_END;
    }

    /**
     * Create hyperlink with explicit support check.
     */
    public static HyperlinkResult createHyperlinkWithSupport(String url, String content) {
        boolean hasSupport = supportsHyperlinks();
        String link = createHyperlink(url, content, hasSupport);
        return new HyperlinkResult(link, hasSupport);
    }

    /**
     * Hyperlink result with support info.
     */
    public record HyperlinkResult(String link, boolean supported) {}
}