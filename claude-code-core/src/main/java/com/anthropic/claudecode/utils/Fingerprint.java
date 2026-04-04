/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code fingerprint utilities
 */
package com.anthropic.claudecode.utils;

import java.nio.charset.*;
import java.security.*;
import java.util.*;

/**
 * Fingerprint computation for OAuth attribution.
 */
public final class Fingerprint {
    private Fingerprint() {}

    private static final String VERSION = "1.0.0";

    /**
     * Compute fingerprint for OAuth validation.
     */
    public static String computeFingerprint(String messageText, String version) {
        try {
            String input = messageText + ":" + version;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            // Should never happen with SHA-256
            return UUID.randomUUID().toString().substring(0, 16);
        }
    }

    /**
     * Compute fingerprint with default version.
     */
    public static String computeFingerprint(String messageText) {
        return computeFingerprint(messageText, VERSION);
    }

    /**
     * Compute device fingerprint.
     */
    public static String computeDeviceFingerprint() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(System.getProperty("user.name", "unknown"));
            sb.append(System.getProperty("os.name", "unknown"));
            sb.append(System.getProperty("os.version", "unknown"));
            sb.append(System.getProperty("user.home", "unknown"));

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    /**
     * Compute session fingerprint.
     */
    public static String computeSessionFingerprint(String sessionId) {
        try {
            String input = sessionId + ":" + System.currentTimeMillis();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString().substring(0, 16);
        }
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

    /**
     * Get current version.
     */
    public static String getVersion() {
        return VERSION;
    }
}