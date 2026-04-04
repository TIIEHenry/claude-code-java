/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code ANSI slicing utilities
 */
package com.anthropic.claudecode.utils.ansi;

import java.util.*;
import java.util.regex.*;

/**
 * Slice strings containing ANSI escape codes properly.
 *
 * Unlike standard string slicing, this properly handles ANSI escape sequences
 * including OSC 8 hyperlinks, maintaining proper escape code state.
 */
public final class SliceAnsi {
    private SliceAnsi() {}

    // ANSI escape sequence patterns
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\u001B\\[[;\\d]*[A-Za-z]");
    private static final Pattern OSC_HYPERLINK = Pattern.compile("\u001B\\]8;;[^\u001B]*\u001B\\\\");
    private static final Pattern OSC_HYPERLINK_END = Pattern.compile("\u001B\\]8;;\u001B\\\\");

    /**
     * Token from ANSI tokenization.
     */
    public sealed interface AnsiToken permits Text, Escape, Hyperlink {
        int width();
    }

    public record Text(String value, int displayWidth) implements AnsiToken {
        @Override
        public int width() {
            return displayWidth;
        }
    }

    public record Escape(String code) implements AnsiToken {
        @Override
        public int width() {
            return 0;
        }
    }

    public record Hyperlink(String url, String text, boolean isEnd) implements AnsiToken {
        @Override
        public int width() {
            return isEnd ? 0 : (text != null ? stringWidth(text) : 0);
        }
    }

    /**
     * Slice a string containing ANSI escape codes by display width.
     *
     * @param str The string to slice
     * @param start Start position in display cells
     * @param end End position in display cells (optional, use -1 for no end)
     * @return The sliced string with proper ANSI codes
     */
    public static String slice(String str, int start, int end) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        if (start < 0) start = 0;
        if (end != -1 && end > str.length()) {
            end = str.length();
        }
        if (end != -1 && start >= end) {
            return "";
        }

        List<AnsiToken> tokens = tokenize(str);
        List<String> activeCodes = new ArrayList<>();
        int position = 0;
        StringBuilder result = new StringBuilder();
        boolean include = false;

        for (AnsiToken token : tokens) {
            int width = token.width();

            // Break at end position
            if (end != -1 && position >= end) {
                if (width > 0 || !include) break;
            }

            if (token instanceof Escape e) {
                activeCodes.add(e.code());
                if (include) {
                    result.append(e.code());
                }
            } else if (token instanceof Text t) {
                if (!include && position >= start) {
                    // Skip leading zero-width marks at start boundary
                    if (start > 0 && width == 0) continue;
                    include = true;
                    // Emit active codes
                    for (String code : activeCodes) {
                        result.append(code);
                    }
                }

                if (include) {
                    result.append(t.value());
                }

                position += width;
            } else if (token instanceof Hyperlink h) {
                if (include && !h.isEnd()) {
                    result.append("\u001B]8;;").append(h.url()).append("\u001B\\");
                    if (h.text() != null) result.append(h.text());
                }
            }
        }

        // Close any active ANSI codes
        result.append(undoAnsiCodes(activeCodes));

        return result.toString();
    }

    /**
     * Slice a string from start position to end.
     */
    public static String slice(String str, int start) {
        return slice(str, start, -1);
    }

    /**
     * Tokenize a string into ANSI tokens.
     */
    private static List<AnsiToken> tokenize(String str) {
        List<AnsiToken> tokens = new ArrayList<>();

        int i = 0;
        while (i < str.length()) {
            // Check for OSC 8 hyperlink
            if (str.startsWith("\u001B]8;;", i)) {
                int endPos = str.indexOf("\u001B\\", i + 6);
                if (endPos != -1) {
                    String urlPart = str.substring(i + 6, endPos);
                    if (urlPart.isEmpty()) {
                        // End of hyperlink
                        tokens.add(new Hyperlink("", null, true));
                    } else {
                        // Start of hyperlink - find the text until closing
                        int textEnd = str.indexOf("\u001B]8;;\u001B\\", endPos + 2);
                        String text = "";
                        if (textEnd != -1) {
                            text = str.substring(endPos + 2, textEnd);
                        }
                        tokens.add(new Hyperlink(urlPart, text, false));
                    }
                    i = endPos + 2;
                    continue;
                }
            }

            // Check for ANSI escape
            Matcher m = ANSI_ESCAPE.matcher(str.substring(i));
            if (m.lookingAt()) {
                tokens.add(new Escape(m.group()));
                i += m.group().length();
                continue;
            }

            // Regular text - calculate display width
            int charWidth = charDisplayWidth(str.charAt(i));
            StringBuilder text = new StringBuilder();

            while (i < str.length()) {
                // Check for escape sequences
                if (str.charAt(i) == '\u001B') {
                    break;
                }

                int cw = charDisplayWidth(str.charAt(i));
                // Combine zero-width chars with previous text
                if (cw == 0 && text.length() > 0 || cw > 0) {
                    text.append(str.charAt(i));
                }
                i++;
            }

            if (text.length() > 0) {
                tokens.add(new Text(text.toString(), stringWidth(text.toString())));
            }
        }

        return tokens;
    }

    /**
     * Undo ANSI codes by emitting reset sequences.
     */
    private static String undoAnsiCodes(List<String> codes) {
        if (codes.isEmpty()) {
            return "";
        }

        // Simple approach: emit SGR reset
        boolean hasSgr = codes.stream().anyMatch(c -> c.contains("[") && !c.contains("]8"));
        if (hasSgr) {
            return "\u001B[0m";
        }

        return "";
    }

    /**
     * Calculate display width of a character.
     */
    private static int charDisplayWidth(char c) {
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
     * Calculate string display width.
     */
    public static int stringWidth(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }

        int width = 0;
        for (int i = 0; i < str.length(); i++) {
            width += charDisplayWidth(str.charAt(i));
        }
        return width;
    }

    /**
     * Check if character is CJK (Chinese, Japanese, Korean).
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
}