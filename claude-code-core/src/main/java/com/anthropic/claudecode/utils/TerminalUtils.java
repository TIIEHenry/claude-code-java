/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code terminal utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.io.*;

/**
 * Terminal detection and capability utilities.
 */
public final class TerminalUtils {
    private TerminalUtils() {}

    /**
     * Terminal type.
     */
    public enum TerminalType {
        TERMINAL_APP("Terminal.app", true, true, true),
        ITERM2("iTerm2", true, true, true),
        VTE("vte", true, true, false),
        XTERM("xterm", true, true, false),
        GNOME_TERMINAL("gnome-terminal", true, true, false),
        KONSOLE("konsole", true, true, false),
        WINDOWS_TERMINAL("Windows Terminal", true, true, true),
        CMD("cmd.exe", false, false, false),
        POWERSHELL("powershell", false, true, false),
        VS_CODE("vscode", true, false, false),
        JETBRAINS("jetbrains", true, false, false),
        UNKNOWN("unknown", false, false, false);

        private final String name;
        private final boolean supportsAnsi;
        private final boolean supportsTrueColor;
        private final boolean supportsImages;

        TerminalType(String name, boolean supportsAnsi, boolean supportsTrueColor, boolean supportsImages) {
            this.name = name;
            this.supportsAnsi = supportsAnsi;
            this.supportsTrueColor = supportsTrueColor;
            this.supportsImages = supportsImages;
        }

        public String getName() { return name; }
        public boolean supportsAnsi() { return supportsAnsi; }
        public boolean supportsTrueColor() { return supportsTrueColor; }
        public boolean supportsImages() { return supportsImages; }
    }

    /**
     * Detect terminal type.
     */
    public static TerminalType detectTerminal() {
        // Check environment variables
        String termProgram = System.getenv("TERM_PROGRAM");
        String term = System.getenv("TERM");
        String iTerm = System.getenv("ITERM_SESSION_ID");
        String vscode = System.getenv("VSCODE_INJECTION");
        String jetbrains = System.getenv("JETBRAINS_IDE");
        String wtSession = System.getenv("WT_SESSION");

        if (termProgram != null) {
            if (termProgram.contains("Terminal.app")) return TerminalType.TERMINAL_APP;
            if (termProgram.contains("iTerm.app")) return TerminalType.ITERM2;
            if (termProgram.contains("vscode")) return TerminalType.VS_CODE;
        }

        if (iTerm != null) return TerminalType.ITERM2;
        if (vscode != null) return TerminalType.VS_CODE;
        if (jetbrains != null) return TerminalType.JETBRAINS;
        if (wtSession != null) return TerminalType.WINDOWS_TERMINAL;

        if (term != null) {
            if (term.contains("xterm-256color")) return TerminalType.XTERM;
            if (term.contains("xterm")) return TerminalType.XTERM;
            if (term.contains("vte")) return TerminalType.VTE;
            if (term.contains("gnome")) return TerminalType.GNOME_TERMINAL;
            if (term.contains("konsole")) return TerminalType.KONSOLE;
        }

        // Check platform
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            String psModule = System.getenv("PSModulePath");
            if (psModule != null) return TerminalType.POWERSHELL;
            return TerminalType.CMD;
        }

        return TerminalType.UNKNOWN;
    }

    /**
     * Check if running in a terminal.
     */
    public static boolean isTerminal() {
        return System.console() != null;
    }

    /**
     * Check if terminal supports ANSI colors.
     */
    public static boolean supportsAnsi() {
        TerminalType terminal = detectTerminal();
        if (terminal.supportsAnsi()) return true;

        // Check NO_COLOR environment variable
        String noColor = System.getenv("NO_COLOR");
        if (noColor != null) return false;

        // Check TERM for basic color support
        String term = System.getenv("TERM");
        if (term != null && !term.isEmpty() && !term.equals("dumb")) {
            return true;
        }

        return false;
    }

    /**
     * Check if terminal supports true color (24-bit).
     */
    public static boolean supportsTrueColor() {
        TerminalType terminal = detectTerminal();
        if (terminal.supportsTrueColor()) return true;

        // Check COLORTERM
        String colorterm = System.getenv("COLORTERM");
        if (colorterm != null && colorterm.contains("truecolor")) {
            return true;
        }

        return false;
    }

    /**
     * Check if terminal supports images (iTerm2 protocol).
     */
    public static boolean supportsImages() {
        TerminalType terminal = detectTerminal();
        return terminal.supportsImages();
    }

    /**
     * Get terminal width in columns.
     */
    public static int getTerminalWidth() {
        String columns = System.getenv("COLUMNS");
        if (columns != null) {
            try {
                return Integer.parseInt(columns);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // Default width
        return 80;
    }

    /**
     * Get terminal height in lines.
     */
    public static int getTerminalHeight() {
        String lines = System.getenv("LINES");
        if (lines != null) {
            try {
                return Integer.parseInt(lines);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // Default height
        return 24;
    }

    /**
     * Get terminal dimensions.
     */
    public static TerminalSize getTerminalSize() {
        return new TerminalSize(getTerminalWidth(), getTerminalHeight());
    }

    /**
     * Check if terminal is in alternate screen mode.
     */
    public static boolean isAlternateScreen() {
        // This is hard to detect, typically requires terminal querying
        return false;
    }

    /**
     * Check if stdout is a TTY.
     */
    public static boolean isStdoutTty() {
        return System.getenv("TERM") != null;
    }

    /**
     * Check if stdin is a TTY.
     */
    public static boolean isStdinTty() {
        return System.console() != null;
    }

    /**
     * Get color depth supported by terminal.
     */
    public static int getColorDepth() {
        if (supportsTrueColor()) return 24;
        if (supportsAnsi()) {
            String term = System.getenv("TERM");
            if (term != null && term.contains("256color")) {
                return 8;
            }
            return 4;
        }
        return 1; // Monochrome
    }

    /**
     * Check if running in VS Code terminal.
     */
    public static boolean isVsCodeTerminal() {
        String termProgram = System.getenv("TERM_PROGRAM");
        return termProgram != null && termProgram.contains("vscode");
    }

    /**
     * Check if running in JetBrains IDE terminal.
     */
    public static boolean isJetBrainsTerminal() {
        String jetbrains = System.getenv("JETBRAINS_IDE");
        return jetbrains != null;
    }

    /**
     * Check if running in interactive mode.
     */
    public static boolean isInteractive() {
        return isTerminal() && isStdinTty();
    }

    /**
     * Get terminal session ID (iTerm2).
     */
    public static Optional<String> getItermSessionId() {
        return Optional.ofNullable(System.getenv("ITERM_SESSION_ID"));
    }

    /**
     * Get Apple Terminal tab ID.
     */
    public static Optional<String> getTerminalTabId() {
        return Optional.ofNullable(System.getenv("TERM_SESSION_ID"));
    }

    /**
     * Generate iTerm2 image protocol escape sequence.
     */
    public static String iterm2ImageProtocol(byte[] imageData, int width, int height) {
        String base64 = Base64.getEncoder().encodeToString(imageData);
        return "\u001B]1337;File=inline=1;width=" + width + ";height=" + height + ":" + base64 + "\u0007";
    }

    /**
     * Generate cursor position query.
     */
    public static String cursorPositionQuery() {
        return "\u001B[6n";
    }

    /**
     * Generate clear screen sequence.
     */
    public static String clearScreen() {
        return "\u001B[2J\u001B[H";
    }

    /**
     * Generate alternate screen enter sequence.
     */
    public static String enterAlternateScreen() {
        return "\u001B[?1049h";
    }

    /**
     * Generate alternate screen exit sequence.
     */
    public static String exitAlternateScreen() {
        return "\u001B[?1049l";
    }

    /**
     * Generate hide cursor sequence.
     */
    public static String hideCursor() {
        return "\u001B[?25l";
    }

    /**
     * Generate show cursor sequence.
     */
    public static String showCursor() {
        return "\u001B[?25h";
    }

    /**
     * Generate set cursor position sequence.
     */
    public static String setCursorPosition(int row, int col) {
        return "\u001B[" + row + ";" + col + "H";
    }

    /**
     * Terminal size record.
     */
    public record TerminalSize(int width, int height) {
        public boolean isValid() {
            return width > 0 && height > 0;
        }
    }
}