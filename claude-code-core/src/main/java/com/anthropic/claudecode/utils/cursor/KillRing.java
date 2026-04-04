/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code kill ring and cursor utilities
 */
package com.anthropic.claudecode.utils.cursor;

import java.util.*;

/**
 * Kill ring for storing killed (cut) text that can be yanked (pasted).
 * This is global state that shares one kill ring across all input fields.
 */
public final class KillRing {
    private KillRing() {}

    private static final int KILL_RING_MAX_SIZE = 10;
    private static final List<String> killRing = new ArrayList<>();
    private static int killRingIndex = 0;
    private static boolean lastActionWasKill = false;

    // Yank tracking for yank-pop
    private static int lastYankStart = 0;
    private static int lastYankLength = 0;
    private static boolean lastActionWasYank = false;

    /**
     * Push text to the kill ring.
     */
    public static void pushToKillRing(String text, String direction) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (lastActionWasKill && !killRing.isEmpty()) {
            // Accumulate with the most recent kill
            String current = killRing.get(0);
            if ("prepend".equals(direction)) {
                killRing.set(0, text + current);
            } else {
                killRing.set(0, current + text);
            }
        } else {
            // Add new entry to front of ring
            killRing.add(0, text);
            if (killRing.size() > KILL_RING_MAX_SIZE) {
                killRing.remove(killRing.size() - 1);
            }
        }

        lastActionWasKill = true;
        lastActionWasYank = false;
    }

    /**
     * Push text to the kill ring (append mode).
     */
    public static void pushToKillRing(String text) {
        pushToKillRing(text, "append");
    }

    /**
     * Get the last killed text.
     */
    public static String getLastKill() {
        return killRing.isEmpty() ? "" : killRing.get(0);
    }

    /**
     * Get item from kill ring at index.
     */
    public static String getKillRingItem(int index) {
        if (killRing.isEmpty()) return "";
        int normalizedIndex = ((index % killRing.size()) + killRing.size()) % killRing.size();
        return killRing.get(normalizedIndex);
    }

    /**
     * Get kill ring size.
     */
    public static int getKillRingSize() {
        return killRing.size();
    }

    /**
     * Clear the kill ring.
     */
    public static void clearKillRing() {
        killRing.clear();
        killRingIndex = 0;
        lastActionWasKill = false;
        lastActionWasYank = false;
        lastYankStart = 0;
        lastYankLength = 0;
    }

    /**
     * Reset kill accumulation.
     */
    public static void resetKillAccumulation() {
        lastActionWasKill = false;
    }

    /**
     * Record a yank operation.
     */
    public static void recordYank(int start, int length) {
        lastYankStart = start;
        lastYankLength = length;
        lastActionWasYank = true;
        killRingIndex = 0;
    }

    /**
     * Check if yank-pop is possible.
     */
    public static boolean canYankPop() {
        return lastActionWasYank && killRing.size() > 1;
    }

    /**
     * Perform yank-pop (cycle to next kill ring item).
     */
    public static YankPopResult yankPop() {
        if (!lastActionWasYank || killRing.size() <= 1) {
            return null;
        }

        killRingIndex = (killRingIndex + 1) % killRing.size();
        String text = killRing.get(killRingIndex);

        return new YankPopResult(text, lastYankStart, lastYankLength);
    }

    /**
     * Update yank length.
     */
    public static void updateYankLength(int length) {
        lastYankLength = length;
    }

    /**
     * Reset yank state.
     */
    public static void resetYankState() {
        lastActionWasYank = false;
    }

    /**
     * Result of yank-pop operation.
     */
    public record YankPopResult(String text, int start, int length) {}

    // ========== Vim Character Classification ==========

    private static final String VIM_WORD_CHARS = "\\p{L}\\p{N}\\p{M}_";

    /**
     * Check if character is a Vim word character.
     */
    public static boolean isVimWordChar(String ch) {
        if (ch == null || ch.isEmpty()) return false;
        return ch.matches("[" + VIM_WORD_CHARS + "]");
    }

    /**
     * Check if character is whitespace.
     */
    public static boolean isVimWhitespace(String ch) {
        if (ch == null || ch.isEmpty()) return false;
        return ch.matches("\\s");
    }

    /**
     * Check if character is punctuation.
     */
    public static boolean isVimPunctuation(String ch) {
        if (ch == null || ch.isEmpty()) return false;
        return !isVimWhitespace(ch) && !isVimWordChar(ch);
    }

    // ========== String Width Utilities ==========

    /**
     * Calculate display width of a string.
     * CJK characters are typically double-width.
     */
    public static int stringWidth(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }

        int width = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            width += charDisplayWidth(c);
        }
        return width;
    }

    /**
     * Calculate display width of a character.
     */
    public static int charDisplayWidth(char c) {
        // CJK characters are typically double-width
        if (isCJK(c)) {
            return 2;
        }

        // Control characters and zero-width
        if (c < ' ' || c == '\u200B' || c == '\uFEFF') {
            return 0;
        }

        // Combining characters
        if (isCombining(c)) {
            return 0;
        }

        return 1;
    }

    /**
     * Check if character is CJK.
     */
    private static boolean isCJK(char c) {
        return (c >= '\u4E00' && c <= '\u9FFF') ||  // CJK Unified Ideographs
               (c >= '\u3000' && c <= '\u303F') ||  // CJK Symbols and Punctuation
               (c >= '\uFF00' && c <= '\uFFEF');    // Halfwidth and Fullwidth Forms
    }

    /**
     * Check if character is a combining character.
     */
    private static boolean isCombining(char c) {
        return (c >= '\u0300' && c <= '\u036F') ||  // Combining Diacritical Marks
               (c >= '\u1AB0' && c <= '\u1AFF') ||  // Combining Diacritical Marks Extended
               (c >= '\u1DC0' && c <= '\u1DFF') ||  // Combining Diacritical Marks Supplement
               (c >= '\u20D0' && c <= '\u20FF') ||  // Combining Diacritical Marks for Symbols
               (c >= '\uFE20' && c <= '\uFE2F');    // Combining Half Marks
    }

    /**
     * Wrap text to a given width.
     */
    public static List<String> wrapText(String text, int width) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            int wordWidth = stringWidth(word);

            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else if (stringWidth(currentLine.toString()) + 1 + wordWidth <= width) {
                currentLine.append(" ").append(word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
}