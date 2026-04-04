/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code early input capture utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Early Input Capture for capturing terminal input before REPL is initialized.
 *
 * This module captures terminal input that is typed before the REPL is fully
 * initialized. Users often type their prompt immediately, but those early
 * keystrokes would otherwise be lost during startup.
 */
public final class EarlyInput {
    private EarlyInput() {}

    // Buffer for early input characters
    private static final StringBuilder earlyInputBuffer = new StringBuilder();

    // Flag to track if we're currently capturing
    private static final AtomicBoolean isCapturing = new AtomicBoolean(false);

    // Reader thread
    private static final AtomicReference<Thread> readerThread = new AtomicReference<>(null);

    /**
     * Start capturing stdin data early, before the REPL is initialized.
     */
    public static void startCapturingEarlyInput() {
        if (!System.console().writer().equals(System.out)) {
            return; // Not a TTY
        }

        if (isCapturing.get()) {
            return; // Already capturing
        }

        isCapturing.set(true);
        earlyInputBuffer.setLength(0);

        // Start a reader thread
        Thread t = new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                int ch;
                while (isCapturing.get() && (ch = reader.read()) != -1) {
                    processChar(ch);
                }
            } catch (IOException e) {
                // Ignore - stdin closed
            }
        });
        t.setDaemon(true);
        t.setName("early-input-reader");
        t.start();
        readerThread.set(t);
    }

    /**
     * Process a single character.
     */
    private static void processChar(int ch) {
        // Ctrl+C
        if (ch == 3) {
            stopCapturingEarlyInput();
            System.exit(130);
            return;
        }

        // Ctrl+D
        if (ch == 4) {
            stopCapturingEarlyInput();
            return;
        }

        // Backspace
        if (ch == 127 || ch == 8) {
            if (earlyInputBuffer.length() > 0) {
                // Remove last character (or grapheme cluster)
                earlyInputBuffer.deleteCharAt(earlyInputBuffer.length() - 1);
            }
            return;
        }

        // Escape sequences (arrow keys, function keys, etc.)
        if (ch == 27) {
            // Skip escape sequence
            return;
        }

        // Skip other control characters (except tab and newline)
        if (ch < 32 && ch != 9 && ch != 10 && ch != 13) {
            return;
        }

        // Convert carriage return to newline
        if (ch == 13) {
            earlyInputBuffer.append('\n');
            return;
        }

        // Add printable character
        earlyInputBuffer.append((char) ch);
    }

    /**
     * Stop capturing early input.
     */
    public static void stopCapturingEarlyInput() {
        if (!isCapturing.get()) {
            return;
        }

        isCapturing.set(false);

        Thread t = readerThread.getAndSet(null);
        if (t != null) {
            t.interrupt();
        }
    }

    /**
     * Consume any early input that was captured.
     */
    public static String consumeEarlyInput() {
        stopCapturingEarlyInput();
        String input = earlyInputBuffer.toString().trim();
        earlyInputBuffer.setLength(0);
        return input;
    }

    /**
     * Check if there is any early input available.
     */
    public static boolean hasEarlyInput() {
        return earlyInputBuffer.length() > 0 && earlyInputBuffer.toString().trim().length() > 0;
    }

    /**
     * Seed the early input buffer with text.
     */
    public static void seedEarlyInput(String text) {
        earlyInputBuffer.setLength(0);
        earlyInputBuffer.append(text);
    }

    /**
     * Check if early input capture is active.
     */
    public static boolean isCapturingEarlyInput() {
        return isCapturing.get();
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