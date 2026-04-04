/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code truncate utilities
 */
package com.anthropic.claudecode.utils;

/**
 * Width-aware truncation and wrapping utilities.
 */
public final class Truncate {
    private Truncate() {}

    private static final String ELLIPSIS = "…";

    /**
     * Truncate a file path in the middle to preserve both directory and filename.
     *
     * @param path     The file path to truncate
     * @param maxLength Maximum display width
     * @return The truncated path
     */
    public static String truncatePathMiddle(String path, int maxLength) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        int width = stringWidth(path);
        if (width <= maxLength) {
            return path;
        }

        if (maxLength <= 0) {
            return ELLIPSIS;
        }

        if (maxLength < 5) {
            return truncateToWidth(path, maxLength);
        }

        // Find filename (last path segment)
        int lastSlash = path.lastIndexOf('/');
        String filename = lastSlash >= 0 ? path.substring(lastSlash) : path;
        String directory = lastSlash >= 0 ? path.substring(0, lastSlash) : "";
        int filenameWidth = stringWidth(filename);

        if (filenameWidth >= maxLength - 1) {
            return truncateStartToWidth(path, maxLength);
        }

        int availableForDir = maxLength - 1 - filenameWidth;

        if (availableForDir <= 0) {
            return truncateStartToWidth(filename, maxLength);
        }

        String truncatedDir = truncateToWidthNoEllipsis(directory, availableForDir);
        return truncatedDir + ELLIPSIS + filename;
    }

    /**
     * Truncate a string to fit within maximum display width.
     */
    public static String truncateToWidth(String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        int width = stringWidth(text);
        if (width <= maxWidth) {
            return text;
        }

        if (maxWidth <= 1) {
            return ELLIPSIS;
        }

        StringBuilder result = new StringBuilder();
        int currentWidth = 0;

        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            int charCount = Character.charCount(cp);
            String segment = new String(Character.toChars(cp));
            int segWidth = charWidth(cp);

            if (currentWidth + segWidth > maxWidth - 1) {
                break;
            }

            result.append(segment);
            currentWidth += segWidth;
            i += charCount;
        }

        return result.toString() + ELLIPSIS;
    }

    /**
     * Truncate from the start of a string, keeping the tail end.
     */
    public static String truncateStartToWidth(String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        int width = stringWidth(text);
        if (width <= maxWidth) {
            return text;
        }

        if (maxWidth <= 1) {
            return ELLIPSIS;
        }

        // Collect segments from end
        StringBuilder result = new StringBuilder();
        int currentWidth = 0;

        for (int i = text.length() - 1; i >= 0; ) {
            int cp;
            if (Character.isLowSurrogate(text.charAt(i)) && i > 0 && Character.isHighSurrogate(text.charAt(i - 1))) {
                cp = Character.toCodePoint(text.charAt(i - 1), text.charAt(i));
                i -= 2;
            } else {
                cp = text.codePointAt(i);
                i--;
            }

            String segment = new String(Character.toChars(cp));
            int segWidth = charWidth(cp);

            if (currentWidth + segWidth > maxWidth - 1) {
                break;
            }

            result.insert(0, segment);
            currentWidth += segWidth;
        }

        return ELLIPSIS + result.toString();
    }

    /**
     * Truncate without appending ellipsis.
     */
    public static String truncateToWidthNoEllipsis(String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        int width = stringWidth(text);
        if (width <= maxWidth) {
            return text;
        }

        if (maxWidth <= 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        int currentWidth = 0;

        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            int charCount = Character.charCount(cp);
            String segment = new String(Character.toChars(cp));
            int segWidth = charWidth(cp);

            if (currentWidth + segWidth > maxWidth) {
                break;
            }

            result.append(segment);
            currentWidth += segWidth;
            i += charCount;
        }

        return result.toString();
    }

    /**
     * Truncate a string with optional single-line mode.
     */
    public static String truncate(String str, int maxWidth, boolean singleLine) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        String result = str;

        if (singleLine) {
            int firstNewline = str.indexOf('\n');
            if (firstNewline != -1) {
                result = str.substring(0, firstNewline);
                if (stringWidth(result) + 1 > maxWidth) {
                    return truncateToWidth(result, maxWidth);
                }
                return result + ELLIPSIS;
            }
        }

        if (stringWidth(result) <= maxWidth) {
            return result;
        }

        return truncateToWidth(result, maxWidth);
    }

    /**
     * Calculate display width of a string.
     */
    public static int stringWidth(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }

        int width = 0;
        for (int i = 0; i < str.length(); ) {
            int cp = str.codePointAt(i);
            width += charWidth(cp);
            i += Character.charCount(cp);
        }
        return width;
    }

    /**
     * Get display width of a single code point.
     */
    private static int charWidth(int cp) {
        // Control characters
        if (Character.getType(cp) == Character.CONTROL) {
            return 0;
        }

        // Combining marks
        int type = Character.getType(cp);
        if (type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK) {
            return 0;
        }

        // Full-width characters (East Asian Width)
        if (isWideCharacter(cp)) {
            return 2;
        }

        return 1;
    }

    /**
     * Check if a code point is a wide character (East Asian Width).
     * Java doesn't have Character.isWide(), so we implement our own check.
     */
    private static boolean isWideCharacter(int cp) {
        // Check for known wide character ranges
        // CJK Unified Ideographs
        if (cp >= 0x4E00 && cp <= 0x9FFF) return true;
        // CJK Unified Ideographs Extension A
        if (cp >= 0x3400 && cp <= 0x4DBF) return true;
        // CJK Unified Ideographs Extension B-F
        if (cp >= 0x20000 && cp <= 0x2CEAF) return true;
        // CJK Compatibility Ideographs
        if (cp >= 0xF900 && cp <= 0xFAFF) return true;
        // CJK Compatibility Ideographs Supplement
        if (cp >= 0x2F800 && cp <= 0x2FA1F) return true;
        // Japanese Hiragana
        if (cp >= 0x3040 && cp <= 0x309F) return true;
        // Japanese Katakana
        if (cp >= 0x30A0 && cp <= 0x30FF) return true;
        // Korean Hangul
        if (cp >= 0xAC00 && cp <= 0xD7AF) return true;
        // Fullwidth ASCII variants
        if (cp >= 0xFF01 && cp <= 0xFF5E) return true;
        // Fullwidth punctuation
        if (cp >= 0xFFE0 && cp <= 0xFFE6) return true;
        // Other wide symbols
        if (cp >= 0x3000 && cp <= 0x303F) return true; // CJK Symbols and Punctuation

        return false;
    }

    /**
     * Wrap text to specified width.
     */
    public static String[] wrapText(String text, int width) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }

        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        int currentWidth = 0;

        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            int charCount = Character.charCount(cp);
            String segment = new String(Character.toChars(cp));
            int segWidth = charWidth(cp);

            if (currentWidth + segWidth <= width) {
                currentLine.append(segment);
                currentWidth += segWidth;
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(segment);
                currentWidth = segWidth;
            }

            i += charCount;
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines.toArray(new String[0]);
    }
}