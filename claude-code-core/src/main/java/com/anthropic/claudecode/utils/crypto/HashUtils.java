/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hash
 */
package com.anthropic.claudecode.utils.crypto;

import java.util.*;
import java.security.*;
import java.security.spec.*;
import java.nio.charset.*;

/**
 * Hash utils - Hashing utilities.
 */
public final class HashUtils {

    /**
     * MD5 hash.
     */
    public static String md5(String input) {
        return hash(input, "MD5");
    }

    /**
     * SHA-1 hash.
     */
    public static String sha1(String input) {
        return hash(input, "SHA-1");
    }

    /**
     * SHA-256 hash.
     */
    public static String sha256(String input) {
        return hash(input, "SHA-256");
    }

    /**
     * SHA-512 hash.
     */
    public static String sha512(String input) {
        return hash(input, "SHA-512");
    }

    /**
     * Generic hash function.
     */
    private static String hash(String input, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    /**
     * Hash file.
     */
    public static String hashFile(java.nio.file.Path path, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] fileBytes = java.nio.file.Files.readAllBytes(path);
            byte[] hash = digest.digest(fileBytes);
            return bytesToHex(hash);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * SHA-256 file hash.
     */
    public static String sha256File(java.nio.file.Path path) {
        return hashFile(path, "SHA-256");
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
     * Generate random hash.
     */
    public static String randomHash() {
        return sha256(UUID.randomUUID().toString());
    }

    /**
     * Generate short hash.
     */
    public static String shortHash(String input) {
        return sha256(input).substring(0, 8);
    }

    /**
     * Hash with salt.
     */
    public static String hashWithSalt(String input, String salt) {
        return sha256(salt + input + salt);
    }

    /**
     * Verify hash with salt.
     */
    public static boolean verifyHash(String input, String salt, String hash) {
        return hashWithSalt(input, salt).equals(hash);
    }

    /**
     * Generate checksum.
     */
    public static Checksum generateChecksum(byte[] data) {
        String sha256 = bytesToHex(hashBytes(data, "SHA-256"));
        String md5 = bytesToHex(hashBytes(data, "MD5"));
        long size = data.length;

        return new Checksum(sha256, md5, size);
    }

    /**
     * Hash bytes.
     */
    private static byte[] hashBytes(byte[] data, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            return new byte[0];
        }
    }

    /**
     * Checksum record.
     */
    public record Checksum(
        String sha256,
        String md5,
        long size
    ) {
        public String format() {
            return String.format("SHA256: %s\nMD5: %s\nSize: %d bytes", sha256, md5, size);
        }
    }

    /**
     * Hash algorithm enum.
     */
    public enum HashAlgorithm {
        MD5("MD5", 32),
        SHA1("SHA-1", 40),
        SHA256("SHA-256", 64),
        SHA512("SHA-512", 128);

        private final String name;
        private final int hashLength;

        HashAlgorithm(String name, int hashLength) {
            this.name = name;
            this.hashLength = hashLength;
        }

        public String getName() { return name; }
        public int getHashLength() { return hashLength; }
    }

    /**
     * Compute hash with algorithm.
     */
    public static String computeHash(String input, HashAlgorithm algorithm) {
        return hash(input, algorithm.getName());
    }

    /**
     * HMAC-SHA256.
     */
    public static String hmacSha256(String input, String key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            return "";
        }
    }

    // SecretKeySpec import
    private static final class SecretKeySpec implements java.security.Key, KeySpec {
        private final byte[] key;
        private final String algorithm;

        SecretKeySpec(byte[] key, String algorithm) {
            this.key = key.clone();
            this.algorithm = algorithm;
        }

        @Override
        public String getAlgorithm() { return algorithm; }
        @Override
        public byte[] getEncoded() { return key.clone(); }
        @Override
        public String getFormat() { return "RAW"; }
    }
}