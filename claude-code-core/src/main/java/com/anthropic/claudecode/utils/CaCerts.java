/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code CA certificates utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * CA certificates loading for TLS connections.
 *
 * Handles custom CA certificate configuration:
 * - System CA certificates
 * - Extra CA certificates from file
 * - Bundled root certificates
 */
public final class CaCerts {
    private CaCerts() {}

    // Cached certificates
    private static volatile List<X509Certificate> cachedCertificates = null;
    private static volatile boolean cacheValid = false;

    // Options
    private static volatile boolean useSystemCA = false;
    private static volatile String extraCertsPath = null;

    /**
     * Configure CA certificate options.
     */
    public static void configure(boolean useSystemCA, String extraCertsPath) {
        CaCerts.useSystemCA = useSystemCA;
        CaCerts.extraCertsPath = extraCertsPath;
        invalidateCache();
    }

    /**
     * Check if system CA is being used.
     */
    public static boolean isUseSystemCA() {
        return useSystemCA;
    }

    /**
     * Get the extra certificates path.
     */
    public static String getExtraCertsPath() {
        return extraCertsPath;
    }

    /**
     * Get CA certificates for TLS connections.
     * Returns null when no custom CA configuration is needed.
     */
    public static List<X509Certificate> getCACertificates() {
        if (cacheValid && cachedCertificates != null) {
            return cachedCertificates;
        }

        // If neither is set, return null (use runtime defaults)
        if (!useSystemCA && extraCertsPath == null) {
            return null;
        }

        List<X509Certificate> certs = new ArrayList<>();

        if (useSystemCA) {
            // Load system CA store
            List<X509Certificate> systemCAs = loadSystemCACertificates();
            if (systemCAs != null && !systemCAs.isEmpty()) {
                certs.addAll(systemCAs);
            } else {
                // Fall back to bundled root certs
                certs.addAll(getBundledRootCertificates());
            }
        } else {
            // Must include bundled root certs as base
            certs.addAll(getBundledRootCertificates());
        }

        // Append extra certs from file
        if (extraCertsPath != null) {
            try {
                List<X509Certificate> extraCerts = loadCertificatesFromFile(extraCertsPath);
                certs.addAll(extraCerts);
            } catch (Exception e) {
                // Log error but continue
            }
        }

        cachedCertificates = certs.isEmpty() ? null : certs;
        cacheValid = true;
        return cachedCertificates;
    }

    /**
     * Clear the CA certificates cache.
     */
    public static void clearCACertsCache() {
        invalidateCache();
    }

    private static void invalidateCache() {
        cacheValid = false;
        cachedCertificates = null;
    }

    /**
     * Load system CA certificates.
     * In Java, this uses the default trust store.
     */
    private static List<X509Certificate> loadSystemCACertificates() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            String trustStorePath = System.getProperty("javax.net.ssl.trustStore");
            String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword", "");

            if (trustStorePath != null) {
                Path path = Paths.get(trustStorePath);
                if (Files.exists(path)) {
                    trustStore.load(Files.newInputStream(path), trustStorePassword.toCharArray());
                    return extractCertificates(trustStore);
                }
            }

            // Try default trust store locations
            String javaHome = System.getProperty("java.home");
            Path defaultTrustStore = Paths.get(javaHome, "lib", "security", "cacerts");
            if (Files.exists(defaultTrustStore)) {
                trustStore.load(Files.newInputStream(defaultTrustStore), "changeit".toCharArray());
                return extractCertificates(trustStore);
            }

        } catch (Exception e) {
            // Fall back to bundled
        }
        return null;
    }

    /**
     * Extract certificates from a KeyStore.
     */
    private static List<X509Certificate> extractCertificates(KeyStore keyStore) throws KeyStoreException {
        List<X509Certificate> certs = new ArrayList<>();
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isCertificateEntry(alias)) {
                java.security.cert.Certificate cert = keyStore.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    certs.add((X509Certificate) cert);
                }
            }
        }
        return certs;
    }

    /**
     * Load certificates from a PEM file.
     */
    private static List<X509Certificate> loadCertificatesFromFile(String path) throws Exception {
        String content = Files.readString(Paths.get(path), StandardCharsets.UTF_8);
        return parsePEMCertificates(content);
    }

    /**
     * Parse PEM-encoded certificates.
     */
    private static List<X509Certificate> parsePEMCertificates(String pemContent) throws Exception {
        List<X509Certificate> certs = new ArrayList<>();
        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        // Parse certificates from PEM format
        ByteArrayInputStream bais = new ByteArrayInputStream(pemContent.getBytes(StandardCharsets.UTF_8));
        while (bais.available() > 0) {
            try {
                java.security.cert.Certificate cert = factory.generateCertificate(bais);
                if (cert instanceof X509Certificate) {
                    certs.add((X509Certificate) cert);
                }
            } catch (Exception e) {
                // End of stream or invalid cert
                break;
            }
        }

        return certs;
    }

    /**
     * Get bundled root certificates.
     * These are the Mozilla root certificates bundled with the JVM.
     */
    private static List<X509Certificate> getBundledRootCertificates() {
        return loadSystemCACertificates(); // Java's cacerts already contains Mozilla roots
    }

    /**
     * Get certificate count for testing.
     */
    public static int getCertificateCount() {
        List<X509Certificate> certs = getCACertificates();
        return certs != null ? certs.size() : 0;
    }
}