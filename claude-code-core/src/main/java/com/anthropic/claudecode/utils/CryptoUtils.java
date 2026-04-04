/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/crypto.ts
 */
package com.anthropic.claudecode.utils;

import java.security.*;
import java.util.*;

/**
 * Cryptographic utilities.
 */
public final class CryptoUtils {
    private CryptoUtils() {}

    /**
     * Generate a random UUID.
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate a random UUID without dashes.
     */
    public static String randomUUIDNoDashes() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Generate a short random ID (8 characters).
     */
    public static String randomShortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Generate a random alphanumeric string.
     */
    public static String randomAlphanumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Generate a random hex string.
     */
    public static String randomHex(int bytes) {
        SecureRandom random = new SecureRandom();
        byte[] data = new byte[bytes];
        random.nextBytes(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Hash a string using SHA-256.
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Hash a string using MD5 (for non-security purposes like cache keys).
     */
    public static String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    /**
     * Generate a hash ID from input.
     */
    public static String hashId(String input) {
        return sha256(input).substring(0, 16);
    }
}