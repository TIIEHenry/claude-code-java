/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code fingerprint utilities
 */
package com.anthropic.claudecode.utils;

import java.nio.charset.*;
import java.security.*;
import java.util.*;

/**
 * Fingerprint computation for Claude Code attribution.
 */
public final class FingerprintUtils {
    private FingerprintUtils() {}

    /**
     * Hardcoded salt from backend validation.
     */
    public static final String FINGERPRINT_SALT = "59cf53e54c78";

    /**
     * Extract text from first user message.
     */
    public static String extractFirstMessageText(List<Map<String, Object>> messages) {
        for (Map<String, Object> msg : messages) {
            if ("user".equals(msg.get("type"))) {
                Object content = msg.get("content");
                if (content instanceof String) {
                    return (String) content;
                }
                if (content instanceof List) {
                    for (Object block : (List<?>) content) {
                        if (block instanceof Map) {
                            Map<?, ?> blockMap = (Map<?, ?>) block;
                            if ("text".equals(blockMap.get("type"))) {
                                return (String) blockMap.get("text");
                            }
                        }
                    }
                }
            }
        }
        return "";
    }

    /**
     * Compute 3-character fingerprint for attribution.
     * Algorithm: SHA256(SALT + msg[4] + msg[7] + msg[20] + version)[:3]
     */
    public static String computeFingerprint(String messageText, String version) {
        // Extract chars at indices [4, 7, 20], use "0" if not found
        char c4 = messageText.length() > 4 ? messageText.charAt(4) : '0';
        char c7 = messageText.length() > 7 ? messageText.charAt(7) : '0';
        char c20 = messageText.length() > 20 ? messageText.charAt(20) : '0';

        String chars = "" + c4 + c7 + c20;
        String fingerprintInput = FINGERPRINT_SALT + chars + version;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fingerprintInput.getBytes(StandardCharsets.UTF_8));
            String hex = bytesToHex(hash);
            return hex.substring(0, 3);
        } catch (NoSuchAlgorithmException e) {
            return "000";
        }
    }

    /**
     * Compute fingerprint from messages.
     */
    public static String computeFingerprintFromMessages(List<Map<String, Object>> messages, String version) {
        String firstMessageText = extractFirstMessageText(messages);
        return computeFingerprint(firstMessageText, version);
    }

    /**
     * Convert bytes to hex string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}