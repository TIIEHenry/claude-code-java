/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Fingerprint.
 */
class FingerprintTest {

    @Test
    @DisplayName("Fingerprint computeFingerprint returns 16 char hex")
    void computeFingerprint() {
        String fp = Fingerprint.computeFingerprint("test message");

        assertNotNull(fp);
        assertEquals(16, fp.length());
        assertTrue(fp.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("Fingerprint computeFingerprint with version returns 16 char hex")
    void computeFingerprintWithVersion() {
        String fp = Fingerprint.computeFingerprint("test message", "1.0.0");

        assertNotNull(fp);
        assertEquals(16, fp.length());
    }

    @Test
    @DisplayName("Fingerprint same input produces same fingerprint")
    void computeFingerprintConsistent() {
        String fp1 = Fingerprint.computeFingerprint("test message", "1.0.0");
        String fp2 = Fingerprint.computeFingerprint("test message", "1.0.0");

        assertEquals(fp1, fp2);
    }

    @Test
    @DisplayName("Fingerprint different input produces different fingerprint")
    void computeFingerprintDifferent() {
        String fp1 = Fingerprint.computeFingerprint("message 1", "1.0.0");
        String fp2 = Fingerprint.computeFingerprint("message 2", "1.0.0");

        assertNotEquals(fp1, fp2);
    }

    @Test
    @DisplayName("Fingerprint computeDeviceFingerprint returns 32 char hex")
    void computeDeviceFingerprint() {
        String fp = Fingerprint.computeDeviceFingerprint();

        assertNotNull(fp);
        assertEquals(32, fp.length());
        assertTrue(fp.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("Fingerprint computeDeviceFingerprint is consistent")
    void computeDeviceFingerprintConsistent() {
        String fp1 = Fingerprint.computeDeviceFingerprint();
        String fp2 = Fingerprint.computeDeviceFingerprint();

        assertEquals(fp1, fp2);
    }

    @Test
    @DisplayName("Fingerprint computeSessionFingerprint returns 16 char hex")
    void computeSessionFingerprint() {
        String fp = Fingerprint.computeSessionFingerprint("session-123");

        assertNotNull(fp);
        assertEquals(16, fp.length());
        assertTrue(fp.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("Fingerprint computeSessionFingerprint different for different sessions")
    void computeSessionFingerprintDifferent() throws InterruptedException {
        String fp1 = Fingerprint.computeSessionFingerprint("session-1");
        Thread.sleep(10); // Ensure different timestamp
        String fp2 = Fingerprint.computeSessionFingerprint("session-2");

        assertNotEquals(fp1, fp2);
    }

    @Test
    @DisplayName("Fingerprint getVersion returns version")
    void getVersion() {
        String version = Fingerprint.getVersion();

        assertNotNull(version);
        assertEquals("1.0.0", version);
    }
}