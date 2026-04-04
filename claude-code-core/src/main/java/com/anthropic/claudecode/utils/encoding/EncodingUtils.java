/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/encoding
 */
package com.anthropic.claudecode.utils.encoding;

import java.util.*;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.*;
import java.util.Base64;
import java.io.UnsupportedEncodingException;

/**
 * Encoding utils - Encoding and decoding utilities.
 */
public final class EncodingUtils {

    /**
     * Base64 encode.
     */
    public static String base64Encode(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64 encode bytes.
     */
    public static String base64Encode(byte[] input) {
        return Base64.getEncoder().encodeToString(input);
    }

    /**
     * Base64 decode.
     */
    public static String base64Decode(String input) {
        return new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
    }

    /**
     * Base64 decode to bytes.
     */
    public static byte[] base64DecodeBytes(String input) {
        return Base64.getDecoder().decode(input);
    }

    /**
     * URL-safe Base64 encode.
     */
    public static String base64UrlEncode(String input) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * URL-safe Base64 decode.
     */
    public static String base64UrlDecode(String input) {
        return new String(
            Base64.getUrlDecoder().decode(input),
            StandardCharsets.UTF_8
        );
    }

    /**
     * Hex encode.
     */
    public static String hexEncode(byte[] input) {
        StringBuilder sb = new StringBuilder();
        for (byte b : input) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Hex decode.
     */
    public static byte[] hexDecode(String input) {
        int len = input.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(input.charAt(i), 16) << 4)
                                 + Character.digit(input.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * URL encode.
     */
    public static String urlEncode(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return input;
        }
    }

    /**
     * URL decode.
     */
    public static String urlDecode(String input) {
        try {
            return URLDecoder.decode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return input;
        }
    }

    /**
     * HTML encode.
     */
    public static String htmlEncode(String input) {
        if (input == null) return "";
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    /**
     * HTML decode.
     */
    public static String htmlDecode(String input) {
        if (input == null) return "";
        return input
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ");
    }

    /**
     * JSON string escape.
     */
    public static String jsonEscape(String input) {
        if (input == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * JSON string unescape.
     */
    public static String jsonUnescape(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;

        for (char c : input.toCharArray()) {
            if (escaped) {
                switch (c) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        // Handle unicode escape
                    }
                    default -> sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Detect encoding from BOM.
     */
    public static Charset detectEncoding(byte[] bytes) {
        if (bytes.length >= 3) {
            // UTF-8 BOM
            if (bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                return StandardCharsets.UTF_8;
            }
        }
        if (bytes.length >= 2) {
            // UTF-16 BE BOM
            if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                return StandardCharsets.UTF_16BE;
            }
            // UTF-16 LE BOM
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                return StandardCharsets.UTF_16LE;
            }
        }

        return StandardCharsets.UTF_8; // Default
    }

    /**
     * Convert encoding.
     */
    public static String convertEncoding(String input, Charset from, Charset to) {
        byte[] bytes = input.getBytes(from);
        return new String(bytes, to);
    }

    /**
     * Encoding result record.
     */
    public record EncodingResult(
        String encoded,
        String encoding,
        boolean success,
        String error
    ) {
        public static EncodingResult success(String encoded, String encoding) {
            return new EncodingResult(encoded, encoding, true, null);
        }

        public static EncodingResult failure(String error) {
            return new EncodingResult(null, null, false, error);
        }
    }
}