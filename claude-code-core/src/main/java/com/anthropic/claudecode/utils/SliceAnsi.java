/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code slice ANSI utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;

/**
 * Slice strings containing ANSI escape codes while preserving the escape sequences.
 */
public final class SliceAnsi {
    private SliceAnsi() {}

    // ANSI escape sequence pattern
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[0-9;]*[a-zA-Z]|\u001B\\][^\u0007]*\u0007|\u001B[PX^_].*?\u001B\\\\");

    /**
     * Slice a string containing ANSI escape codes.
     *
     * @param str   The string to slice
     * @param start The start position (in display cells)
     * @return The sliced string with ANSI codes preserved
     */
    public static String slice(String str, int start) {
        return slice(str, start, Integer.MAX_VALUE);
    }

    /**
     * Slice a string containing ANSI escape codes.
     *
     * @param str   The string to slice
     * @param start The start position (in display cells)
     * @param end   The end position (in display cells)
     * @return The sliced string with ANSI codes preserved
     */
    public static String slice(String str, int start, int end) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        // Tokenize into ANSI sequences and text
        List<Token> tokens = tokenize(str);

        StringBuilder result = new StringBuilder();
        List<String> activeCodes = new ArrayList<>();
        int position = 0;
        boolean include = false;

        for (Token token : tokens) {
            int width = token.type == TokenType.ANSI ? 0 : stringWidth(token.value);

            // Check if we've passed the end
            if (end != Integer.MAX_VALUE && position >= end) {
                if (token.type == TokenType.ANSI || width > 0 || !include) {
                    break;
                }
            }

            if (token.type == TokenType.ANSI) {
                activeCodes.add(token.value);
                if (include) {
                    result.append(token.value);
                }
            } else {
                if (!include && position >= start) {
                    if (start > 0 && width == 0) {
                        // Skip leading zero-width marks
                        continue;
                    }
                    include = true;
                    // Add active ANSI codes
                    for (String code : activeCodes) {
                        result.append(code);
                    }
                }

                if (include) {
                    result.append(token.value);
                }

                position += width;
            }
        }

        // Close any remaining ANSI codes
        result.append(undoAnsiCodes(activeCodes));

        return result.toString();
    }

    /**
     * Tokenize string into ANSI sequences and text.
     */
    private static List<Token> tokenize(String str) {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = ANSI_PATTERN.matcher(str);
        int lastEnd = 0;

        while (matcher.find()) {
            // Add text before this match
            if (matcher.start() > lastEnd) {
                tokens.add(new Token(TokenType.TEXT, str.substring(lastEnd, matcher.start())));
            }
            // Add the ANSI sequence
            tokens.add(new Token(TokenType.ANSI, matcher.group()));
            lastEnd = matcher.end();
        }

        // Add remaining text
        if (lastEnd < str.length()) {
            tokens.add(new Token(TokenType.TEXT, str.substring(lastEnd)));
        }

        return tokens;
    }

    /**
     * Calculate display width of a string.
     */
    private static int stringWidth(String str) {
        if (str == null) return 0;

        int width = 0;
        for (int i = 0; i < str.length(); ) {
            int cp = str.codePointAt(i);
            i += Character.charCount(cp);

            // Control characters and combining marks have width 0
            if (Character.getType(cp) == Character.FORMAT ||
                Character.getType(cp) == Character.CONTROL ||
                Character.getType(cp) == Character.NON_SPACING_MARK ||
                Character.getType(cp) == Character.ENCLOSING_MARK) {
                continue;
            }

            // Full-width characters have width 2
            if (isWideCharacter(cp)) {
                width += 2;
            } else {
                width += 1;
            }
        }
        return width;
    }

    /**
     * Check if a code point is a wide character (East Asian Width).
     * Java doesn't have Character.isWide(), so we implement our own check.
     */
    private static boolean isWideCharacter(int cp) {
        // CJK Unified Ideographs
        if (cp >= 0x4E00 && cp <= 0x9FFF) return true;
        // CJK Unified Ideographs Extension A
        if (cp >= 0x3400 && cp <= 0x4DBF) return true;
        // CJK Unified Ideographs Extension B-F
        if (cp >= 0x20000 && cp <= 0x2CEAF) return true;
        // CJK Compatibility Ideographs
        if (cp >= 0xF900 && cp <= 0xFAFF) return true;
        // Japanese Hiragana
        if (cp >= 0x3040 && cp <= 0x309F) return true;
        // Japanese Katakana
        if (cp >= 0x30A0 && cp <= 0x30FF) return true;
        // Korean Hangul
        if (cp >= 0xAC00 && cp <= 0xD7AF) return true;
        // Fullwidth ASCII variants
        if (cp >= 0xFF01 && cp <= 0xFF5E) return true;
        return false;
    }

    /**
     * Generate undo sequences for ANSI codes.
     */
    private static String undoAnsiCodes(List<String> codes) {
        StringBuilder sb = new StringBuilder();

        // Reset all codes
        if (!codes.isEmpty()) {
            sb.append("\u001B[0m");
        }

        return sb.toString();
    }

    private enum TokenType {
        ANSI, TEXT
    }

    private record Token(TokenType type, String value) {}
}