/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code encoding utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.nio.charset.*;
import java.io.*;

/**
 * Encoding and charset utilities.
 */
public final class EncodingUtils {
    private EncodingUtils() {}

    /**
     * Common charset constants.
     */
    public static final Charset UTF_8 = StandardCharsets.UTF_8;
    public static final Charset UTF_16 = StandardCharsets.UTF_16;
    public static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;
    public static final Charset US_ASCII = StandardCharsets.US_ASCII;

    /**
     * Detect charset from byte array.
     */
    public static Charset detectCharset(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return UTF_8;
        }

        // Check for UTF-8 BOM
        if (bytes.length >= 3 && bytes[0] == 0xEF && bytes[1] == 0xBB && bytes[2] == 0xBF) {
            return UTF_8;
        }

        // Check for UTF-16 BOM
        if (bytes.length >= 2) {
            if (bytes[0] == 0xFE && bytes[1] == 0xFF) {
                return StandardCharsets.UTF_16BE;
            }
            if (bytes[0] == 0xFF && bytes[1] == 0xFE) {
                return StandardCharsets.UTF_16LE;
            }
        }

        // Check for UTF-32 BOM
        if (bytes.length >= 4) {
            if (bytes[0] == 0x00 && bytes[1] == 0x00 && bytes[2] == 0xFE && bytes[3] == 0xFF) {
                return Charset.forName("UTF-32BE");
            }
            if (bytes[0] == 0xFF && bytes[1] == 0xFE && bytes[2] == 0x00 && bytes[3] == 0x00) {
                return Charset.forName("UTF-32LE");
            }
        }

        // Try UTF-8 validation
        if (isValidUtf8(bytes)) {
            return UTF_8;
        }

        // Check for high bytes indicating non-ASCII
        boolean hasHighBytes = false;
        for (byte b : bytes) {
            if (b < 0) {
                hasHighBytes = true;
                break;
            }
        }

        if (!hasHighBytes) {
            return US_ASCII;
        }

        // Default to UTF-8
        return UTF_8;
    }

    /**
     * Check if byte array is valid UTF-8.
     */
    public static boolean isValidUtf8(byte[] bytes) {
        try {
            String decoded = new String(bytes, UTF_8);
            byte[] reencoded = decoded.getBytes(UTF_8);
            return Arrays.equals(bytes, reencoded);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convert string to UTF-8 bytes.
     */
    public static byte[] toUtf8Bytes(String str) {
        if (str == null) return null;
        return str.getBytes(UTF_8);
    }

    /**
     * Convert UTF-8 bytes to string.
     */
    public static String fromUtf8Bytes(byte[] bytes) {
        if (bytes == null) return null;
        return new String(bytes, UTF_8);
    }

    /**
     * Convert bytes to string with detected charset.
     */
    public static String bytesToString(byte[] bytes) {
        Charset charset = detectCharset(bytes);
        return new String(bytes, charset);
    }

    /**
     * Convert bytes to string with specified charset.
     */
    public static String bytesToString(byte[] bytes, Charset charset) {
        if (bytes == null) return null;
        return new String(bytes, charset);
    }

    /**
     * Convert bytes to string with charset name.
     */
    public static String bytesToString(byte[] bytes, String charsetName) {
        if (bytes == null) return null;
        try {
            return new String(bytes, charsetName);
        } catch (UnsupportedEncodingException e) {
            return new String(bytes, UTF_8);
        }
    }

    /**
     * Convert string to bytes with charset.
     */
    public static byte[] stringToBytes(String str, Charset charset) {
        if (str == null) return null;
        return str.getBytes(charset);
    }

    /**
     * Convert string from one charset to another.
     */
    public static String convertCharset(String str, Charset from, Charset to) {
        if (str == null) return null;
        byte[] bytes = str.getBytes(from);
        return new String(bytes, to);
    }

    /**
     * Check if a charset is available.
     */
    public static boolean isCharsetAvailable(String charsetName) {
        try {
            Charset.forName(charsetName);
            return true;
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            return false;
        }
    }

    /**
     * Get all available charsets.
     */
    public static Set<String> getAvailableCharsets() {
        return Charset.availableCharsets().keySet();
    }

    /**
     * Get default charset for system.
     */
    public static Charset getDefaultCharset() {
        return Charset.defaultCharset();
    }

    /**
     * Get system charset name.
     */
    public static String getDefaultCharsetName() {
        return Charset.defaultCharset().name();
    }

    /**
     * Check if system charset is UTF-8.
     */
    public static boolean isSystemUtf8() {
        return getDefaultCharset().equals(UTF_8);
    }

    /**
     * Detect line ending type from string.
     */
    public static LineEnding detectLineEnding(String text) {
        if (text == null || text.isEmpty()) {
            return LineEnding.NONE;
        }

        int crlfCount = 0;
        int lfCount = 0;
        int crCount = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\r') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    crlfCount++;
                    i++; // Skip LF
                } else {
                    crCount++;
                }
            } else if (c == '\n') {
                lfCount++;
            }
        }

        if (crlfCount > 0 && crlfCount >= lfCount && crlfCount >= crCount) {
            return LineEnding.CRLF;
        }
        if (crCount > 0 && crCount >= lfCount) {
            return LineEnding.CR;
        }
        if (lfCount > 0) {
            return LineEnding.LF;
        }
        return LineEnding.NONE;
    }

    /**
     * Line ending types.
     */
    public enum LineEnding {
        LF("\n"),
        CRLF("\r\n"),
        CR("\r"),
        NONE("");

        private final String chars;

        LineEnding(String chars) {
            this.chars = chars;
        }

        public String getChars() {
            return chars;
        }
    }

    /**
     * Normalize line endings to LF.
     */
    public static String normalizeLineEndingsToLf(String text) {
        if (text == null) return null;
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    /**
     * Normalize line endings to CRLF.
     */
    public static String normalizeLineEndingsToCrLf(String text) {
        if (text == null) return null;
        // First normalize to LF, then convert to CRLF
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        return normalized.replace("\n", "\r\n");
    }

    /**
     * Normalize line endings to specified type.
     */
    public static String normalizeLineEndings(String text, LineEnding ending) {
        switch (ending) {
            case LF:
                return normalizeLineEndingsToLf(text);
            case CRLF:
                return normalizeLineEndingsToCrLf(text);
            case CR:
                if (text == null) return null;
                return text.replace("\r\n", "\n").replace('\n', '\r');
            default:
                return text;
        }
    }

    /**
     * Normalize line endings for current platform.
     */
    public static String normalizeLineEndingsForPlatform(String text) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return normalizeLineEndingsToCrLf(text);
        }
        return normalizeLineEndingsToLf(text);
    }

    /**
     * Get platform line ending.
     */
    public static LineEnding getPlatformLineEnding() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return LineEnding.CRLF;
        }
        return LineEnding.LF;
    }

    /**
     * Count characters in a string (handling multi-byte chars correctly).
     */
    public static int countCharacters(String text) {
        if (text == null) return 0;
        return text.codePointCount(0, text.length());
    }

    /**
     * Count bytes in a string when encoded with UTF-8.
     */
    public static int countUtf8Bytes(String text) {
        if (text == null) return 0;
        return text.getBytes(UTF_8).length;
    }

    /**
     * Check if string contains non-ASCII characters.
     */
    public static boolean containsNonAscii(String text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) > 127) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if string contains multi-byte UTF-8 characters.
     */
    public static boolean containsMultiByteUtf8(String text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c > 127) {
                return true;
            }
        }
        return false;
    }

    /**
     * Strip non-ASCII characters.
     */
    public static String stripNonAscii(String text) {
        if (text == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c <= 127) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Replace non-ASCII characters with specified replacement.
     */
    public static String replaceNonAscii(String text, String replacement) {
        if (text == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c <= 127) {
                sb.append(c);
            } else {
                sb.append(replacement);
            }
        }
        return sb.toString();
    }

    /**
     * Base64 encode.
     */
    public static String base64Encode(byte[] bytes) {
        if (bytes == null) return null;
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Base64 encode string.
     */
    public static String base64Encode(String str) {
        if (str == null) return null;
        return base64Encode(toUtf8Bytes(str));
    }

    /**
     * Base64 decode.
     */
    public static byte[] base64Decode(String base64) {
        if (base64 == null) return null;
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Base64 decode to string.
     */
    public static String base64DecodeToString(String base64) {
        if (base64 == null) return null;
        return fromUtf8Bytes(base64Decode(base64));
    }

    /**
     * URL-safe Base64 encode.
     */
    public static String base64UrlEncode(byte[] bytes) {
        if (bytes == null) return null;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * URL-safe Base64 decode.
     */
    public static byte[] base64UrlDecode(String base64) {
        if (base64 == null) return null;
        return Base64.getUrlDecoder().decode(base64);
    }

    /**
     * Hex encode.
     */
    public static String hexEncode(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Hex decode.
     */
    public static byte[] hexDecode(String hex) {
        if (hex == null) return null;
        int len = hex.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return bytes;
    }

    /**
     * Detect file encoding by reading first bytes.
     */
    public static Charset detectFileEncoding(java.nio.file.Path file) throws IOException {
        byte[] header = new byte[4];
        try (InputStream is = java.nio.file.Files.newInputStream(file)) {
            int read = is.read(header);
            if (read < header.length) {
                header = Arrays.copyOf(header, read);
            }
        }
        return detectCharset(header);
    }

    /**
     * Read file content as UTF-8 string.
     */
    public static String readFileAsUtf8String(java.nio.file.Path file) throws IOException {
        return java.nio.file.Files.readString(file, UTF_8);
    }

    /**
     * Write string to file as UTF-8.
     */
    public static void writeUtf8StringToFile(java.nio.file.Path file, String content) throws IOException {
        java.nio.file.Files.writeString(file, content, UTF_8);
    }
}