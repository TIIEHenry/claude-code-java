/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/oauth/crypto
 */
package com.anthropic.claudecode.services.oauth;

import java.security.*;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * OAuth crypto utilities - PKCE code verifier/challenge generation.
 */
public final class OAuthCrypto {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate a random code verifier for PKCE.
     * Must be between 43 and 128 characters, containing only unreserved characters.
     */
    public static String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generate code challenge from code verifier using S256 method.
     * SHA256 hash, then base64url encoding without padding.
     */
    public static String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Generate random state parameter for CSRF protection.
     */
    public static String generateState() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Validate code verifier format (43-128 characters, unreserved chars only).
     */
    public static boolean isValidCodeVerifier(String codeVerifier) {
        if (codeVerifier == null) {
            return false;
        }

        int length = codeVerifier.length();
        if (length < 43 || length > 128) {
            return false;
        }

        // Check for unreserved characters only: A-Z, a-z, 0-9, -, ., _, ~
        for (char c : codeVerifier.toCharArray()) {
            if (!isUnreservedChar(c)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isUnreservedChar(char c) {
        return (c >= 'A' && c <= 'Z') ||
               (c >= 'a' && c <= 'z') ||
               (c >= '0' && c <= '9') ||
               c == '-' || c == '.' || c == '_' || c == '~';
    }

    /**
     * Base64url decode (for decoding JWT tokens etc).
     */
    public static byte[] base64UrlDecode(String input) {
        return Base64.getUrlDecoder().decode(input);
    }

    /**
     * Base64url encode without padding.
     */
    public static String base64UrlEncode(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }
}