/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code early input capture
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.util.concurrent.*;

/**
 * Early input capture for terminal input typed before REPL initialization.
 */
public final class EarlyInputCapture {
    private EarlyInputCapture() {}

    private static final StringBuilder earlyInputBuffer = new StringBuilder();
    private static volatile boolean isCapturing = false;
    private static Thread captureThread = null;

    /**
     * Start capturing stdin data early, before the REPL is initialized.
     * Should be called as early as possible in the startup sequence.
     */
    public static void startCapturing() {
        if (isCapturing) {
            return;
        }

        // Only capture in interactive mode
        if (System.console() == null) {
            return;
        }

        isCapturing = true;
        earlyInputBuffer.setLength(0);

        // Use traditional thread instead of virtual thread (Java 17 compatible)
        captureThread = new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                while (isCapturing) {
                    if (reader.ready()) {
                        int ch = reader.read();
                        if (ch >= 0) {
                            processChar((char) ch);
                        }
                    } else {
                        Thread.sleep(10);
                    }
                }
            } catch (IOException | InterruptedException e) {
                // Stop capturing on error
                isCapturing = false;
            }
        });
    }

    /**
     * Process a character from early input.
     */
    private static void processChar(char ch) {
        int code = (int) ch;

        // Ctrl+C (code 3) - stop capturing and exit
        if (code == 3) {
            stopCapturing();
            System.exit(130);
            return;
        }

        // Ctrl+D (code 4) - EOF, stop capturing
        if (code == 4) {
            stopCapturing();
            return;
        }

        // Backspace (code 127 or 8) - remove last character
        if (code == 127 || code == 8) {
            if (earlyInputBuffer.length() > 0) {
                earlyInputBuffer.deleteCharAt(earlyInputBuffer.length() - 1);
            }
            return;
        }

        // Skip escape sequences (arrow keys, function keys, etc.)
        if (code == 27) {
            return;
        }

        // Skip other control characters (except tab and newline)
        if (code < 32 && code != 9 && code != 10 && code != 13) {
            return;
        }

        // Convert carriage return to newline
        if (code == 13) {
            earlyInputBuffer.append('\n');
            return;
        }

        // Add printable characters
        earlyInputBuffer.append(ch);
    }

    /**
     * Stop capturing early input.
     */
    public static void stopCapturing() {
        if (!isCapturing) {
            return;
        }

        isCapturing = false;

        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
    }

    /**
     * Consume any early input that was captured.
     * Returns the captured input and clears the buffer.
     */
    public static String consumeEarlyInput() {
        stopCapturing();
        String input = earlyInputBuffer.toString().trim();
        earlyInputBuffer.setLength(0);
        return input;
    }

    /**
     * Check if there is any early input available.
     */
    public static boolean hasEarlyInput() {
        return earlyInputBuffer.length() > 0 && !earlyInputBuffer.toString().trim().isEmpty();
    }

    /**
     * Seed the early input buffer with text.
     */
    public static void seedEarlyInput(String text) {
        earlyInputBuffer.setLength(0);
        earlyInputBuffer.append(text);
    }

    /**
     * Check if early input capture is currently active.
     */
    public static boolean isCapturing() {
        return isCapturing;
    }

    /**
     * Get the current buffer content without consuming it.
     */
    public static String peekEarlyInput() {
        return earlyInputBuffer.toString();
    }

    /**
     * Clear the early input buffer.
     */
    public static void clearEarlyInput() {
        earlyInputBuffer.setLength(0);
    }
}