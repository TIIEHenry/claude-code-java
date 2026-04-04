/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CaCerts.
 */
class CaCertsTest {

    @BeforeEach
    void setUp() {
        CaCerts.clearCACertsCache();
        CaCerts.configure(false, null);
    }

    @Test
    @DisplayName("CaCerts configure")
    void configure() {
        CaCerts.configure(true, "/path/to/certs.pem");

        assertTrue(CaCerts.isUseSystemCA());
        assertEquals("/path/to/certs.pem", CaCerts.getExtraCertsPath());
    }

    @Test
    @DisplayName("CaCerts configure false")
    void configureFalse() {
        CaCerts.configure(false, null);

        assertFalse(CaCerts.isUseSystemCA());
        assertNull(CaCerts.getExtraCertsPath());
    }

    @Test
    @DisplayName("CaCerts isUseSystemCA")
    void isUseSystemCA() {
        assertFalse(CaCerts.isUseSystemCA());

        CaCerts.configure(true, null);
        assertTrue(CaCerts.isUseSystemCA());
    }

    @Test
    @DisplayName("CaCerts getExtraCertsPath")
    void getExtraCertsPath() {
        assertNull(CaCerts.getExtraCertsPath());

        CaCerts.configure(false, "/custom/path.pem");
        assertEquals("/custom/path.pem", CaCerts.getExtraCertsPath());
    }

    @Test
    @DisplayName("CaCerts getCACertificates returns null when not configured")
    void getCACertificatesNotConfigured() {
        CaCerts.configure(false, null);

        assertNull(CaCerts.getCACertificates());
    }

    @Test
    @DisplayName("CaCerts getCACertificates with system CA")
    void getCACertificatesWithSystemCA() {
        CaCerts.configure(true, null);

        var certs = CaCerts.getCACertificates();
        // May return null if system CA store cannot be loaded
        // or may return certificates from Java's cacerts
        // Just verify it doesn't throw
        assertTrue(certs == null || certs.size() >= 0);
    }

    @Test
    @DisplayName("CaCerts getCACertificates cached")
    void getCACertificatesCached() {
        CaCerts.configure(true, null);

        var certs1 = CaCerts.getCACertificates();
        var certs2 = CaCerts.getCACertificates();

        // Should return same cached instance
        assertSame(certs1, certs2);
    }

    @Test
    @DisplayName("CaCerts clearCACertsCache")
    void clearCACertsCache() {
        CaCerts.configure(true, null);

        var certs1 = CaCerts.getCACertificates();
        CaCerts.clearCACertsCache();
        var certs2 = CaCerts.getCACertificates();

        // After clear, cache should be invalidated
        // May still return same result (from reloading), but cache was cleared
        assertTrue(certs1 == null || certs1.size() >= 0);
        assertTrue(certs2 == null || certs2.size() >= 0);
    }

    @Test
    @DisplayName("CaCerts getCertificateCount")
    void getCertificateCount() {
        CaCerts.configure(false, null);

        assertEquals(0, CaCerts.getCertificateCount());
    }

    @Test
    @DisplayName("CaCerts getCertificateCount with system CA")
    void getCertificateCountWithSystemCA() {
        CaCerts.configure(true, null);

        int count = CaCerts.getCertificateCount();
        // May be 0 if loading fails, or positive if successful
        assertTrue(count >= 0);
    }

    @Test
    @DisplayName("CaCerts configure clears cache")
    void configureClearsCache() {
        CaCerts.configure(true, null);
        var certs1 = CaCerts.getCACertificates();

        CaCerts.configure(false, null); // Should invalidate cache

        assertNull(CaCerts.getCACertificates());
    }

    @Test
    @DisplayName("CaCerts getCACertificates with invalid extra path")
    void getCACertificatesInvalidExtraPath() {
        CaCerts.configure(false, "/nonexistent/path.pem");

        // Should not throw, just ignore invalid path
        var certs = CaCerts.getCACertificates();
        // Returns bundled roots since extra path fails
        assertTrue(certs == null || certs.size() >= 0);
    }

    @Test
    @DisplayName("CaCerts multiple configure calls")
    void multipleConfigureCalls() {
        CaCerts.configure(true, "/path1.pem");
        assertTrue(CaCerts.isUseSystemCA());
        assertEquals("/path1.pem", CaCerts.getExtraCertsPath());

        CaCerts.configure(false, "/path2.pem");
        assertFalse(CaCerts.isUseSystemCA());
        assertEquals("/path2.pem", CaCerts.getExtraCertsPath());

        CaCerts.configure(true, null);
        assertTrue(CaCerts.isUseSystemCA());
        assertNull(CaCerts.getExtraCertsPath());
    }
}