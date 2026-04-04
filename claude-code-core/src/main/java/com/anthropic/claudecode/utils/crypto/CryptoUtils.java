/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/crypto
 */
package com.anthropic.claudecode.utils.crypto;

import java.util.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * Crypto utils - Encryption utilities.
 */
public final class CryptoUtils {
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * Generate random key.
     */
    public static String generateKey() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[32];
        random.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    /**
     * Generate random IV.
     */
    public static byte[] generateIv() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[GCM_IV_LENGTH];
        random.nextBytes(iv);
        return iv;
    }

    /**
     * Encrypt string with AES-GCM.
     */
    public static EncryptionResult encrypt(String plaintext, String base64Key) {
        try {
            byte[] key = Base64.getDecoder().decode(base64Key);
            byte[] iv = generateIv();

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Combine IV and ciphertext
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return new EncryptionResult(
                Base64.getEncoder().encodeToString(combined),
                true,
                null
            );
        } catch (Exception e) {
            return new EncryptionResult(null, false, e.getMessage());
        }
    }

    /**
     * Decrypt string with AES-GCM.
     */
    public static DecryptionResult decrypt(String ciphertext, String base64Key) {
        try {
            byte[] key = Base64.getDecoder().decode(base64Key);
            byte[] combined = Base64.getDecoder().decode(ciphertext);

            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ct = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ct, 0, ct.length);

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plaintext = cipher.doFinal(ct);

            return new DecryptionResult(
                new String(plaintext, java.nio.charset.StandardCharsets.UTF_8),
                true,
                null
            );
        } catch (Exception e) {
            return new DecryptionResult(null, false, e.getMessage());
        }
    }

    /**
     * Simple obfuscation.
     */
    public static String obfuscate(String input) {
        if (input == null) return null;
        return Base64.getEncoder().encodeToString(
            xorBytes(input.getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );
    }

    /**
     * Simple deobfuscation.
     */
    public static String deobfuscate(String input) {
        if (input == null) return null;
        try {
            return new String(
                xorBytes(Base64.getDecoder().decode(input)),
                java.nio.charset.StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * XOR bytes with fixed pattern.
     */
    private static byte[] xorBytes(byte[] input) {
        byte[] pattern = "ClaudeCodeObfuscationKey".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] result = new byte[input.length];

        for (int i = 0; i < input.length; i++) {
            result[i] = (byte) (input[i] ^ pattern[i % pattern.length]);
        }

        return result;
    }

    /**
     * Generate secure random string.
     */
    public static String secureRandomString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Encryption result record.
     */
    public record EncryptionResult(
        String ciphertext,
        boolean success,
        String error
    ) {
        public static EncryptionResult success(String ciphertext) {
            return new EncryptionResult(ciphertext, true, null);
        }

        public static EncryptionResult failure(String error) {
            return new EncryptionResult(null, false, error);
        }
    }

    /**
     * Decryption result record.
     */
    public record DecryptionResult(
        String plaintext,
        boolean success,
        String error
    ) {
        public static DecryptionResult success(String plaintext) {
            return new DecryptionResult(plaintext, true, null);
        }

        public static DecryptionResult failure(String error) {
            return new DecryptionResult(null, false, error);
        }
    }

    /**
     * Key pair record.
     */
    public record KeyPair(String publicKey, String privateKey) {}

    /**
     * Crypto config record.
     */
    public record CryptoConfig(
        String algorithm,
        int keySize,
        int ivSize,
        String transformation
    ) {
        public static CryptoConfig defaultConfig() {
            return new CryptoConfig("AES", 256, 12, "AES/GCM/NoPadding");
        }
    }
}