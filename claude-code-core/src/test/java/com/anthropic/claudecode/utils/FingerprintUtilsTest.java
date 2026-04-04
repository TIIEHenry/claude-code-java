/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FingerprintUtils.
 */
class FingerprintUtilsTest {

    @Test
    @DisplayName("FingerprintUtils FINGERPRINT_SALT constant")
    void fingerprintSaltConstant() {
        assertEquals("59cf53e54c78", FingerprintUtils.FINGERPRINT_SALT);
    }

    @Test
    @DisplayName("FingerprintUtils computeFingerprint returns 3 chars")
    void computeFingerprintReturns3Chars() {
        String fingerprint = FingerprintUtils.computeFingerprint("Hello world test message", "1.0.0");
        assertEquals(3, fingerprint.length());
    }

    @Test
    @DisplayName("FingerprintUtils computeFingerprint consistent")
    void computeFingerprintConsistent() {
        String fp1 = FingerprintUtils.computeFingerprint("Test message", "1.0.0");
        String fp2 = FingerprintUtils.computeFingerprint("Test message", "1.0.0");
        assertEquals(fp1, fp2);
    }

    @Test
    @DisplayName("FingerprintUtils computeFingerprint different messages")
    void computeFingerprintDifferentMessages() {
        String fp1 = FingerprintUtils.computeFingerprint("First message", "1.0.0");
        String fp2 = FingerprintUtils.computeFingerprint("Second message", "1.0.0");
        // May or may not be different, but should both be 3 chars
        assertEquals(3, fp1.length());
        assertEquals(3, fp2.length());
    }

    @Test
    @DisplayName("FingerprintUtils computeFingerprint different versions")
    void computeFingerprintDifferentVersions() {
        String fp1 = FingerprintUtils.computeFingerprint("Test message", "1.0.0");
        String fp2 = FingerprintUtils.computeFingerprint("Test message", "2.0.0");
        // Different versions should produce different fingerprints
        assertNotEquals(fp1, fp2);
    }

    @Test
    @DisplayName("FingerprintUtils computeFingerprint short message")
    void computeFingerprintShortMessage() {
        String fingerprint = FingerprintUtils.computeFingerprint("Hi", "1.0.0");
        assertEquals(3, fingerprint.length());
    }

    @Test
    @DisplayName("FingerprintUtils computeFingerprint empty message")
    void computeFingerprintEmptyMessage() {
        String fingerprint = FingerprintUtils.computeFingerprint("", "1.0.0");
        assertEquals(3, fingerprint.length());
    }

    @Test
    @DisplayName("FingerprintUtils extractFirstMessageText string content")
    void extractFirstMessageTextString() {
        List<Map<String, Object>> messages = List.of(
            Map.of("type", "user", "content", "Hello world")
        );
        String text = FingerprintUtils.extractFirstMessageText(messages);
        assertEquals("Hello world", text);
    }

    @Test
    @DisplayName("FingerprintUtils extractFirstMessageText list content")
    void extractFirstMessageTextList() {
        Map<String, Object> textBlock = new HashMap<>();
        textBlock.put("type", "text");
        textBlock.put("text", "List content");

        List<Map<String, Object>> messages = List.of(
            Map.of("type", "user", "content", List.of(textBlock))
        );
        String text = FingerprintUtils.extractFirstMessageText(messages);
        assertEquals("List content", text);
    }

    @Test
    @DisplayName("FingerprintUtils extractFirstMessageText assistant message skipped")
    void extractFirstMessageTextAssistantSkipped() {
        List<Map<String, Object>> messages = List.of(
            Map.of("type", "assistant", "content", "Response"),
            Map.of("type", "user", "content", "User message")
        );
        String text = FingerprintUtils.extractFirstMessageText(messages);
        assertEquals("User message", text);
    }

    @Test
    @DisplayName("FingerprintUtils extractFirstMessageText empty list")
    void extractFirstMessageTextEmpty() {
        String text = FingerprintUtils.extractFirstMessageText(List.of());
        assertEquals("", text);
    }

    @Test
    @DisplayName("FingerprintUtils computeFingerprintFromMessages")
    void computeFingerprintFromMessages() {
        List<Map<String, Object>> messages = List.of(
            Map.of("type", "user", "content", "Test message for fingerprint")
        );
        String fingerprint = FingerprintUtils.computeFingerprintFromMessages(messages, "1.0.0");
        assertEquals(3, fingerprint.length());
    }
}