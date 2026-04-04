/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code hash utilities
 */
package com.anthropic.claudecode.utils;

import java.nio.charset.*;
import java.security.*;

/**
 * Hash utilities for content hashing and change detection.
 */
public final class HashUtils {
    private HashUtils() {}

    /**
     * DJB2 string hash — fast non-cryptographic hash returning a signed 32-bit int.
     * Deterministic across runtimes. Use for cache directory names and similar.
     */
    public static int djb2Hash(String str) {
        int hash = 0;
        for (int i = 0; i < str.length(); i++) {
            hash = ((hash << 5) - hash + str.charAt(i));
        }
        return hash;
    }

    /**
     * Hash arbitrary content for change detection using SHA-256.
     */
    public static String hashContent(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Hash byte content.
     */
    public static String hashBytes(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Hash two strings without allocating a concatenated temp string.
     */
    public static String hashPair(String a, String b) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(a.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0); // Separator
            digest.update(b.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Hash multiple strings.
     */
    public static String hashStrings(String... strings) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String s : strings) {
                if (s != null) {
                    digest.update(s.getBytes(StandardCharsets.UTF_8));
                }
                digest.update((byte) 0); // Separator
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Compute MD5 hash (for non-security use cases like cache keys).
     */
    public static String md5Hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    /**
     * Convert bytes to hex string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * Quick hash for short strings (for internal use).
     */
    public static int quickHash(String str) {
        return str == null ? 0 : str.hashCode();
    }

    /**
     * Combine two hash codes.
     */
    public static int combineHashes(int a, int b) {
        // From Guava's HashCombine
        return a * 31 + b;
    }

    /**
     * Hash a path (normalize separators first).
     */
    public static String hashPath(String path) {
        if (path == null) return "";
        // Normalize path separators
        String normalized = path.replace('\\', '/');
        return hashContent(normalized);
    }
}