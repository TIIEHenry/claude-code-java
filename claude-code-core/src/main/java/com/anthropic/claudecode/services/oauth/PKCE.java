/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/oauth/pkce
 */
package com.anthropic.claudecode.services.oauth;

import java.util.*;
import java.security.*;
import java.nio.charset.*;

/**
 * PKCE - PKCE (Proof Key for Code Exchange) utilities.
 */
public final class PKCE {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int VERIFIER_LENGTH = 64;
    private static final int MIN_VERIFIER_LENGTH = 43;
    private static final int MAX_VERIFIER_LENGTH = 128;

    /**
     * Generate code verifier.
     */
    public static String generateCodeVerifier() {
        byte[] bytes = new byte[VERIFIER_LENGTH];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generate code challenge.
     */
    public static String generateCodeChallenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate S256 challenge.
     */
    public static String generateS256Challenge(String verifier) {
        return generateCodeChallenge(verifier);
    }

    /**
     * Generate plain challenge.
     */
    public static String generatePlainChallenge(String verifier) {
        return verifier;
    }

    /**
     * Validate verifier.
     */
    public static boolean isValidVerifier(String verifier) {
        if (verifier == null) return false;
        int length = verifier.length();
        if (length < MIN_VERIFIER_LENGTH || length > MAX_VERIFIER_LENGTH) {
            return false;
        }
        // Must only contain unreserved characters
        return verifier.matches("[A-Za-z0-9\\-._~]+");
    }

    /**
     * Validate challenge.
     */
    public static boolean isValidChallenge(String challenge, ChallengeMethod method) {
        if (challenge == null) return false;

        return switch (method) {
            case S256 -> {
                // S256 challenge should be 43 characters (base64url encoded SHA-256)
                yield challenge.length() == 43 && challenge.matches("[A-Za-z0-9\\-._~]+");
            }
            case PLAIN -> isValidVerifier(challenge);
        };
    }

    /**
     * Challenge method enum.
     */
    public enum ChallengeMethod {
        S256("S256"),
        PLAIN("plain");

        private final String value;

        ChallengeMethod(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * PKCE pair record.
     */
    public record PKCEPair(String verifier, String challenge, ChallengeMethod method) {
        public static PKCEPair generate() {
            String verifier = generateCodeVerifier();
            String challenge = generateS256Challenge(verifier);
            return new PKCEPair(verifier, challenge, ChallengeMethod.S256);
        }

        public static PKCEPair generatePlain() {
            String verifier = generateCodeVerifier();
            return new PKCEPair(verifier, verifier, ChallengeMethod.PLAIN);
        }

        public boolean isValid() {
            return isValidVerifier(verifier) && isValidChallenge(challenge, method);
        }
    }
}