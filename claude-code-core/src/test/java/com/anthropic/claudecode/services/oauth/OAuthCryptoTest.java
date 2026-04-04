/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.oauth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OAuthCrypto.
 */
class OAuthCryptoTest {

    @Test
    @DisplayName("OAuthCrypto generateCodeVerifier generates valid verifier")
    void generateCodeVerifier() {
        String verifier = OAuthCrypto.generateCodeVerifier();

        assertNotNull(verifier);
        assertTrue(OAuthCrypto.isValidCodeVerifier(verifier));
        // 32 bytes encoded as base64url without padding = 43 characters
        assertEquals(43, verifier.length());
    }

    @Test
    @DisplayName("OAuthCrypto generateCodeVerifier generates unique verifiers")
    void generateCodeVerifierUnique() {
        String v1 = OAuthCrypto.generateCodeVerifier();
        String v2 = OAuthCrypto.generateCodeVerifier();

        assertNotEquals(v1, v2);
    }

    @Test
    @DisplayName("OAuthCrypto generateCodeChallenge generates valid challenge")
    void generateCodeChallenge() {
        String verifier = OAuthCrypto.generateCodeVerifier();
        String challenge = OAuthCrypto.generateCodeChallenge(verifier);

        assertNotNull(challenge);
        // SHA-256 hash = 32 bytes, base64url without padding = 43 characters
        assertEquals(43, challenge.length());
    }

    @Test
    @DisplayName("OAuthCrypto generateCodeChallenge deterministic")
    void generateCodeChallengeDeterministic() {
        String verifier = OAuthCrypto.generateCodeVerifier();

        String c1 = OAuthCrypto.generateCodeChallenge(verifier);
        String c2 = OAuthCrypto.generateCodeChallenge(verifier);

        assertEquals(c1, c2);
    }

    @Test
    @DisplayName("OAuthCrypto generateState generates valid state")
    void generateState() {
        String state = OAuthCrypto.generateState();

        assertNotNull(state);
        // 16 bytes encoded as base64url without padding = 22 characters
        assertEquals(22, state.length());
    }

    @Test
    @DisplayName("OAuthCrypto generateState generates unique states")
    void generateStateUnique() {
        String s1 = OAuthCrypto.generateState();
        String s2 = OAuthCrypto.generateState();

        assertNotEquals(s1, s2);
    }

    @Test
    @DisplayName("OAuthCrypto isValidCodeVerifier validates correct verifiers")
    void isValidCodeVerifierValid() {
        String verifier = OAuthCrypto.generateCodeVerifier();
        assertTrue(OAuthCrypto.isValidCodeVerifier(verifier));

        // 43 characters is minimum
        String minVerifier = "a".repeat(43);
        assertTrue(OAuthCrypto.isValidCodeVerifier(minVerifier));

        // 128 characters is maximum
        String maxVerifier = "a".repeat(128);
        assertTrue(OAuthCrypto.isValidCodeVerifier(maxVerifier));
    }

    @Test
    @DisplayName("OAuthCrypto isValidCodeVerifier rejects null")
    void isValidCodeVerifierNull() {
        assertFalse(OAuthCrypto.isValidCodeVerifier(null));
    }

    @Test
    @DisplayName("OAuthCrypto isValidCodeVerifier rejects too short")
    void isValidCodeVerifierTooShort() {
        // 42 characters is too short
        assertFalse(OAuthCrypto.isValidCodeVerifier("a".repeat(42)));
        assertFalse(OAuthCrypto.isValidCodeVerifier("short"));
    }

    @Test
    @DisplayName("OAuthCrypto isValidCodeVerifier rejects too long")
    void isValidCodeVerifierTooLong() {
        // 129 characters is too long
        assertFalse(OAuthCrypto.isValidCodeVerifier("a".repeat(129)));
    }

    @Test
    @DisplayName("OAuthCrypto isValidCodeVerifier rejects invalid characters")
    void isValidCodeVerifierInvalidChars() {
        assertTrue(OAuthCrypto.isValidCodeVerifier("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~"));
        assertFalse(OAuthCrypto.isValidCodeVerifier("invalid!chars"));
        assertFalse(OAuthCrypto.isValidCodeVerifier("invalid chars"));
        assertFalse(OAuthCrypto.isValidCodeVerifier("invalid@chars"));
    }

    @Test
    @DisplayName("OAuthCrypto base64UrlEncode encodes correctly")
    void base64UrlEncode() {
        byte[] input = "test".getBytes();
        String encoded = OAuthCrypto.base64UrlEncode(input);

        assertNotNull(encoded);
        assertEquals("dGVzdA", encoded);
    }

    @Test
    @DisplayName("OAuthCrypto base64UrlDecode decodes correctly")
    void base64UrlDecode() {
        String input = "dGVzdA";
        byte[] decoded = OAuthCrypto.base64UrlDecode(input);

        assertEquals("test", new String(decoded));
    }

    @Test
    @DisplayName("OAuthCrypto base64UrlEncodeDecode round trip")
    void base64UrlEncodeDecodeRoundTrip() {
        byte[] original = "Hello World 123!".getBytes();
        String encoded = OAuthCrypto.base64UrlEncode(original);
        byte[] decoded = OAuthCrypto.base64UrlDecode(encoded);

        assertEquals(new String(original), new String(decoded));
    }
}