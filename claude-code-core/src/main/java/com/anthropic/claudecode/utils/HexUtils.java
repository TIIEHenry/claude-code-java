/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code hex utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Hexadecimal utilities.
 */
public final class HexUtils {
    private HexUtils() {}

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static final char[] HEX_CHARS_UPPER = "0123456789ABCDEF".toCharArray();
    private static final int[] HEX_VALUES = new int[128];

    static {
        for (int i = 0; i < HEX_VALUES.length; i++) {
            HEX_VALUES[i] = -1;
        }
        for (int i = 0; i < 10; i++) {
            HEX_VALUES['0' + i] = i;
        }
        for (int i = 0; i < 6; i++) {
            HEX_VALUES['a' + i] = 10 + i;
            HEX_VALUES['A' + i] = 10 + i;
        }
    }

    /**
     * Convert bytes to hex string (lowercase).
     */
    public static String encode(byte[] bytes) {
        return encode(bytes, false);
    }

    /**
     * Convert bytes to hex string.
     */
    public static String encode(byte[] bytes, boolean uppercase) {
        if (bytes == null) return "";
        char[] chars = new char[bytes.length * 2];
        char[] hexChars = uppercase ? HEX_CHARS_UPPER : HEX_CHARS;

        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            chars[i * 2] = hexChars[v >>> 4];
            chars[i * 2 + 1] = hexChars[v & 0x0F];
        }

        return new String(chars);
    }

    /**
     * Convert hex string to bytes.
     */
    public static byte[] decode(String hex) {
        if (hex == null || hex.isEmpty()) {
            return new byte[0];
        }

        String cleanHex = hex;
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            cleanHex = hex.substring(2);
        }

        if (cleanHex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }

        byte[] bytes = new byte[cleanHex.length() / 2];

        for (int i = 0; i < bytes.length; i++) {
            int high = decodeChar(cleanHex.charAt(i * 2));
            int low = decodeChar(cleanHex.charAt(i * 2 + 1));
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("Invalid hex character");
            }
            bytes[i] = (byte) ((high << 4) | low);
        }

        return bytes;
    }

    /**
     * Decode single hex character.
     */
    private static int decodeChar(char c) {
        if (c < 0 || c >= HEX_VALUES.length) {
            return -1;
        }
        return HEX_VALUES[c];
    }

    /**
     * Convert int to hex string.
     */
    public static String toHex(int value) {
        return toHex(value, 8);
    }

    /**
     * Convert int to hex string with padding.
     */
    public static String toHex(int value, int digits) {
        StringBuilder sb = new StringBuilder(digits);
        String hex = Integer.toHexString(value);
        while (sb.length() < digits - hex.length()) {
            sb.append('0');
        }
        sb.append(hex);
        return sb.toString();
    }

    /**
     * Convert long to hex string.
     */
    public static String toHex(long value) {
        return toHex(value, 16);
    }

    /**
     * Convert long to hex string with padding.
     */
    public static String toHex(long value, int digits) {
        StringBuilder sb = new StringBuilder(digits);
        String hex = Long.toHexString(value);
        while (sb.length() < digits - hex.length()) {
            sb.append('0');
        }
        sb.append(hex);
        return sb.toString();
    }

    /**
     * Parse int from hex.
     */
    public static int parseInt(String hex) {
        String cleanHex = hex.startsWith("0x") ? hex.substring(2) : hex;
        return (int) parseLong(cleanHex);
    }

    /**
     * Parse long from hex.
     */
    public static long parseLong(String hex) {
        String cleanHex = hex;
        if (hex.startsWith("0x") || hex.startsWith("0X") || hex.startsWith("-0x") || hex.startsWith("-0X")) {
            cleanHex = hex.replace("0x", "").replace("0X", "");
        }

        long value = 0;
        boolean negative = cleanHex.startsWith("-");
        if (negative) {
            cleanHex = cleanHex.substring(1);
        }

        for (int i = 0; i < cleanHex.length(); i++) {
            int digit = decodeChar(cleanHex.charAt(i));
            if (digit == -1) {
                throw new IllegalArgumentException("Invalid hex character: " + cleanHex.charAt(i));
            }
            value = (value << 4) | digit;
        }

        return negative ? -value : value;
    }

    /**
     * Check if string is valid hex.
     */
    public static boolean isValidHex(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        String check = str;
        if (str.startsWith("0x") || str.startsWith("0X")) {
            check = str.substring(2);
        }

        for (int i = 0; i < check.length(); i++) {
            char c = check.charAt(i);
            if (c >= HEX_VALUES.length || HEX_VALUES[c] == -1) {
                return false;
            }
        }

        return true;
    }

    /**
     * Format bytes as hex with separator.
     */
    public static String formatWithSeparator(byte[] bytes, String separator) {
        if (bytes == null || bytes.length == 0) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(separator);
            sb.append(toHex(bytes[i], 2));
        }
        return sb.toString();
    }

    /**
     * Format as MAC address (colon-separated).
     */
    public static String formatMacAddress(byte[] bytes) {
        return formatWithSeparator(bytes, ":");
    }

    /**
     * Format as UUID.
     */
    public static String formatUuid(byte[] bytes) {
        if (bytes.length != 16) {
            throw new IllegalArgumentException("UUID requires 16 bytes");
        }
        StringBuilder sb = new StringBuilder(36);
        sb.append(encode(bytes, 0, 4)).append('-');
        sb.append(encode(bytes, 4, 2)).append('-');
        sb.append(encode(bytes, 6, 2)).append('-');
        sb.append(encode(bytes, 8, 2)).append('-');
        sb.append(encode(bytes, 10, 6));
        return sb.toString();
    }

    /**
     * Encode bytes range.
     */
    private static String encode(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder(length * 2);
        for (int i = offset; i < offset + length; i++) {
            sb.append(HEX_CHARS[(bytes[i] >>> 4) & 0x0F]);
            sb.append(HEX_CHARS[bytes[i] & 0x0F]);
        }
        return sb.toString();
    }

    /**
     * Parse MAC address to bytes.
     */
    public static byte[] parseMacAddress(String mac) {
        String[] parts = mac.split(":|-");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid MAC address format");
        }

        byte[] bytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            bytes[i] = (byte) parseInt(parts[i]);
        }
        return bytes;
    }

    /**
     * Parse UUID to bytes.
     */
    public static byte[] parseUuid(String uuid) {
        String clean = uuid.replace("-", "");
        return decode(clean);
    }

    /**
     * Reverse byte order.
     */
    public static byte[] reverse(byte[] bytes) {
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            reversed[i] = bytes[bytes.length - 1 - i];
        }
        return reversed;
    }

    /**
     * Convert byte to hex char array.
     */
    public static char[] toHexChars(byte b) {
        return new char[] {
            HEX_CHARS[(b >>> 4) & 0x0F],
            HEX_CHARS[b & 0x0F]
        };
    }

    /**
     * Dump bytes as hex dump (like xxd).
     */
    public static String dump(byte[] bytes) {
        return dump(bytes, 0, bytes.length);
    }

    /**
     * Dump bytes range as hex dump.
     */
    public static String dump(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i += 16) {
            // Address
            sb.append(toHex(offset + i, 8)).append(":  ");

            // Hex part
            int lineLength = Math.min(16, length - i);
            for (int j = 0; j < lineLength; j++) {
                sb.append(toHex(bytes[offset + i + j], 2));
                if (j == 7) sb.append(" ");
                sb.append(" ");
            }

            // Padding
            for (int j = lineLength; j < 16; j++) {
                sb.append("  ");
                if (j == 7) sb.append(" ");
                sb.append(" ");
            }

            // ASCII part
            sb.append(" |");
            for (int j = 0; j < lineLength; j++) {
                char c = (char) bytes[offset + i + j];
                sb.append(c >= 32 && c < 127 ? c : '.');
            }
            sb.append("|\n");
        }

        return sb.toString();
    }

    /**
     * Count hex digits needed for value.
     */
    public static int digitsNeeded(long value) {
        if (value == 0) return 1;
        int digits = 0;
        while (value != 0) {
            value >>>= 4;
            digits++;
        }
        return digits;
    }

    /**
     * Compare two hex strings.
     */
    public static int compare(String hex1, String hex2) {
        byte[] bytes1 = decode(hex1);
        byte[] bytes2 = decode(hex2);

        for (int i = 0; i < Math.min(bytes1.length, bytes2.length); i++) {
            int cmp = Byte.compareUnsigned(bytes1[i], bytes2[i]);
            if (cmp != 0) return cmp;
        }

        return Integer.compare(bytes1.length, bytes2.length);
    }
}