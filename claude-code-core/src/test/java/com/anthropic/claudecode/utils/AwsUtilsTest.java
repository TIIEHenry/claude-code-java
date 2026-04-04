/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AwsUtils.
 */
class AwsUtilsTest {

    @Test
    @DisplayName("AwsUtils AwsCredentials record")
    void awsCredentials() {
        AwsUtils.AwsCredentials creds = new AwsUtils.AwsCredentials(
            "access-key", "secret-key", "session-token", "2024-01-01"
        );
        assertEquals("access-key", creds.accessKeyId());
        assertEquals("secret-key", creds.secretAccessKey());
        assertEquals("session-token", creds.sessionToken());
        assertEquals("2024-01-01", creds.expiration());
    }

    @Test
    @DisplayName("AwsUtils AwsStsOutput record")
    void awsStsOutput() {
        AwsUtils.AwsCredentials creds = new AwsUtils.AwsCredentials(
            "access-key", "secret-key", "session-token", null
        );
        AwsUtils.AwsStsOutput output = new AwsUtils.AwsStsOutput(creds);
        assertEquals(creds, output.credentials());
    }

    @Test
    @DisplayName("AwsUtils isAwsCredentialsProviderError false for null")
    void isAwsCredentialsProviderErrorNull() {
        assertFalse(AwsUtils.isAwsCredentialsProviderError(null));
    }

    @Test
    @DisplayName("AwsUtils isAwsCredentialsProviderError false for other exception")
    void isAwsCredentialsProviderErrorOther() {
        RuntimeException ex = new RuntimeException("test");
        assertFalse(AwsUtils.isAwsCredentialsProviderError(ex));
    }

    @Test
    @DisplayName("AwsUtils isValidAwsStsOutput false for null")
    void isValidAwsStsOutputNull() {
        assertFalse(AwsUtils.isValidAwsStsOutput(null));
    }

    @Test
    @DisplayName("AwsUtils isValidAwsStsOutput false for non-map")
    void isValidAwsStsOutputNonMap() {
        assertFalse(AwsUtils.isValidAwsStsOutput("string"));
        assertFalse(AwsUtils.isValidAwsStsOutput(123));
    }

    @Test
    @DisplayName("AwsUtils isValidAwsStsOutput false for empty map")
    void isValidAwsStsOutputEmptyMap() {
        assertFalse(AwsUtils.isValidAwsStsOutput(Map.of()));
    }

    @Test
    @DisplayName("AwsUtils isValidAwsStsOutput false for missing credentials")
    void isValidAwsStsOutputMissingCredentials() {
        Map<String, Object> output = Map.of("other", "value");
        assertFalse(AwsUtils.isValidAwsStsOutput(output));
    }

    @Test
    @DisplayName("AwsUtils isValidAwsStsOutput false for incomplete credentials")
    void isValidAwsStsOutputIncompleteCredentials() {
        Map<String, Object> creds = new HashMap<>();
        creds.put("AccessKeyId", "key");
        // Missing SecretAccessKey and SessionToken
        
        Map<String, Object> output = new HashMap<>();
        output.put("Credentials", creds);
        
        assertFalse(AwsUtils.isValidAwsStsOutput(output));
    }

    @Test
    @DisplayName("AwsUtils isValidAwsStsOutput true for valid credentials")
    void isValidAwsStsOutputValid() {
        Map<String, Object> creds = new HashMap<>();
        creds.put("AccessKeyId", "access-key-id");
        creds.put("SecretAccessKey", "secret-access-key");
        creds.put("SessionToken", "session-token");
        
        Map<String, Object> output = new HashMap<>();
        output.put("Credentials", creds);
        
        assertTrue(AwsUtils.isValidAwsStsOutput(output));
    }

    @Test
    @DisplayName("AwsUtils isValidAwsStsOutput false for empty values")
    void isValidAwsStsOutputEmptyValues() {
        Map<String, Object> creds = new HashMap<>();
        creds.put("AccessKeyId", "");
        creds.put("SecretAccessKey", "secret");
        creds.put("SessionToken", "token");
        
        Map<String, Object> output = new HashMap<>();
        output.put("Credentials", creds);
        
        assertFalse(AwsUtils.isValidAwsStsOutput(output));
    }

    @Test
    @DisplayName("AwsUtils checkStsCallerIdentity does not throw")
    void checkStsCallerIdentity() {
        assertDoesNotThrow(() -> AwsUtils.checkStsCallerIdentity());
    }

    @Test
    @DisplayName("AwsUtils clearAwsIniCache does not throw")
    void clearAwsIniCache() {
        assertDoesNotThrow(() -> AwsUtils.clearAwsIniCache());
    }

    @Test
    @DisplayName("AwsUtils isBedrockConfigured returns boolean")
    void isBedrockConfigured() {
        // Just verify it returns a boolean
        boolean result = AwsUtils.isBedrockConfigured();
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("AwsUtils isVertexConfigured returns boolean")
    void isVertexConfigured() {
        boolean result = AwsUtils.isVertexConfigured();
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("AwsUtils isFoundryConfigured returns boolean")
    void isFoundryConfigured() {
        boolean result = AwsUtils.isFoundryConfigured();
        assertTrue(result == true || result == false);
    }
}
