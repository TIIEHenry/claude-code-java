/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.oauth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PKCE.
 */
class PKCETest {

    @Test
    @DisplayName("PKCE generateCodeVerifier generates valid verifier")
    void generateCodeVerifier() {
        String verifier = PKCE.generateCodeVerifier();

        assertNotNull(verifier);
        assertTrue(PKCE.isValidVerifier(verifier));
        // 64 bytes encoded as base64url without padding = 86 characters
        assertEquals(86, verifier.length());
    }

    @Test
    @DisplayName("PKCE generateCodeVerifier generates unique verifiers")
    void generateCodeVerifierUnique() {
        String v1 = PKCE.generateCodeVerifier();
        String v2 = PKCE.generateCodeVerifier();

        assertNotEquals(v1, v2);
    }

    @Test
    @DisplayName("PKCE generateCodeChallenge generates valid challenge")
    void generateCodeChallenge() {
        String verifier = PKCE.generateCodeVerifier();
        String challenge = PKCE.generateCodeChallenge(verifier);

        assertNotNull(challenge);
        assertEquals(43, challenge.length());
        assertTrue(PKCE.isValidChallenge(challenge, PKCE.ChallengeMethod.S256));
    }

    @Test
    @DisplayName("PKCE generateS256Challenge same as generateCodeChallenge")
    void generateS256Challenge() {
        String verifier = PKCE.generateCodeVerifier();

        assertEquals(PKCE.generateCodeChallenge(verifier), PKCE.generateS256Challenge(verifier));
    }

    @Test
    @DisplayName("PKCE generatePlainChallenge returns verifier")
    void generatePlainChallenge() {
        String verifier = PKCE.generateCodeVerifier();
        String challenge = PKCE.generatePlainChallenge(verifier);

        assertEquals(verifier, challenge);
    }

    @Test
    @DisplayName("PKCE isValidVerifier validates correct verifiers")
    void isValidVerifierValid() {
        String verifier = PKCE.generateCodeVerifier();
        assertTrue(PKCE.isValidVerifier(verifier));

        // 43 characters is minimum
        String minVerifier = "a".repeat(43);
        assertTrue(PKCE.isValidVerifier(minVerifier));

        // 128 characters is maximum
        String maxVerifier = "a".repeat(128);
        assertTrue(PKCE.isValidVerifier(maxVerifier));
    }

    @Test
    @DisplayName("PKCE isValidVerifier rejects invalid verifiers")
    void isValidVerifierInvalid() {
        assertFalse(PKCE.isValidVerifier(null));
        // Too short (42 characters)
        assertFalse(PKCE.isValidVerifier("a".repeat(42)));
        // Too long (129 characters)
        assertFalse(PKCE.isValidVerifier("a".repeat(129)));
        // Invalid characters
        assertFalse(PKCE.isValidVerifier("invalid!chars"));
    }

    @Test
    @DisplayName("PKCE isValidChallenge validates S256 challenges")
    void isValidChallengeS256() {
        String verifier = PKCE.generateCodeVerifier();
        String challenge = PKCE.generateS256Challenge(verifier);

        assertTrue(PKCE.isValidChallenge(challenge, PKCE.ChallengeMethod.S256));
    }

    @Test
    @DisplayName("PKCE isValidChallenge validates plain challenges")
    void isValidChallengePlain() {
        String verifier = PKCE.generateCodeVerifier();

        assertTrue(PKCE.isValidChallenge(verifier, PKCE.ChallengeMethod.PLAIN));
    }

    @Test
    @DisplayName("PKCE isValidChallenge rejects null")
    void isValidChallengeNull() {
        assertFalse(PKCE.isValidChallenge(null, PKCE.ChallengeMethod.S256));
        assertFalse(PKCE.isValidChallenge(null, PKCE.ChallengeMethod.PLAIN));
    }

    @Test
    @DisplayName("PKCE ChallengeMethod enum values")
    void challengeMethodEnum() {
        PKCE.ChallengeMethod[] methods = PKCE.ChallengeMethod.values();
        assertEquals(2, methods.length);
        assertEquals(PKCE.ChallengeMethod.S256, PKCE.ChallengeMethod.valueOf("S256"));
        assertEquals(PKCE.ChallengeMethod.PLAIN, PKCE.ChallengeMethod.valueOf("PLAIN"));
        assertEquals("S256", PKCE.ChallengeMethod.S256.getValue());
        assertEquals("plain", PKCE.ChallengeMethod.PLAIN.getValue());
    }

    @Test
    @DisplayName("PKCE PKCEPair generate creates valid pair")
    void pkcePairGenerate() {
        PKCE.PKCEPair pair = PKCE.PKCEPair.generate();

        assertNotNull(pair);
        assertNotNull(pair.verifier());
        assertNotNull(pair.challenge());
        assertEquals(PKCE.ChallengeMethod.S256, pair.method());
        assertTrue(pair.isValid());
    }

    @Test
    @DisplayName("PKCE PKCEPair generatePlain creates valid plain pair")
    void pkcePairGeneratePlain() {
        PKCE.PKCEPair pair = PKCE.PKCEPair.generatePlain();

        assertNotNull(pair);
        assertNotNull(pair.verifier());
        assertEquals(pair.verifier(), pair.challenge());
        assertEquals(PKCE.ChallengeMethod.PLAIN, pair.method());
        assertTrue(pair.isValid());
    }

    @Test
    @DisplayName("PKCE PKCEPair record")
    void pkcePairRecord() {
        String verifier = "test-verifier-1234567890abcdefghijklmnopqrstuvwxyz";
        String challenge = "test-challenge-value-1234567890abcdef";
        PKCE.PKCEPair pair = new PKCE.PKCEPair(verifier, challenge, PKCE.ChallengeMethod.S256);

        assertEquals(verifier, pair.verifier());
        assertEquals(challenge, pair.challenge());
        assertEquals(PKCE.ChallengeMethod.S256, pair.method());
    }

    @Test
    @DisplayName("PKCE PKCEPair isValid checks validity")
    void pkcePairIsValid() {
        PKCE.PKCEPair validPair = PKCE.PKCEPair.generate();
        assertTrue(validPair.isValid());

        // Invalid pair with wrong verifier length
        PKCE.PKCEPair invalidPair = new PKCE.PKCEPair("short", "short", PKCE.ChallengeMethod.PLAIN);
        assertFalse(invalidPair.isValid());
    }
}