/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.hooks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SsrfGuard.
 */
class SsrfGuardTest {

    @Test
    @DisplayName("SsrfGuard isBlockedAddress blocks 0.0.0.0")
    void blocksZeroNetwork() {
        assertTrue(SsrfGuard.isBlockedAddress("0.0.0.0"));
        assertTrue(SsrfGuard.isBlockedAddress("0.0.0.1"));
        assertTrue(SsrfGuard.isBlockedAddress("0.255.255.255"));
    }

    @Test
    @DisplayName("SsrfGuard isBlockedAddress blocks 10.x.x.x private")
    void blocksTenNetwork() {
        assertTrue(SsrfGuard.isBlockedAddress("10.0.0.1"));
        assertTrue(SsrfGuard.isBlockedAddress("10.255.255.255"));
    }

    @Test
    @DisplayName("SsrfGuard isBlockedAddress blocks 169.254.x.x link-local")
    void blocksLinkLocal() {
        assertTrue(SsrfGuard.isBlockedAddress("169.254.0.1"));
        assertTrue(SsrfGuard.isBlockedAddress("169.254.169.254")); // AWS metadata
        assertTrue(SsrfGuard.isBlockedAddress("169.254.255.255"));
    }

    @Test
    @DisplayName("SsrfGuard isBlockedAddress blocks 172.16-31.x.x private")
    void blocks172Network() {
        assertTrue(SsrfGuard.isBlockedAddress("172.16.0.1"));
        assertTrue(SsrfGuard.isBlockedAddress("172.20.0.1"));
        assertTrue(SsrfGuard.isBlockedAddress("172.31.255.255"));
    }

    @Test
    @DisplayName("SsrfGuard isBlockedAddress allows 172.32.x.x")
    void allows172OutsideRange() {
        assertFalse(SsrfGuard.isBlockedAddress("172.32.0.1"));
        assertFalse(SsrfGuard.isBlockedAddress("172.0.0.1"));
        assertFalse(SsrfGuard.isBlockedAddress("172.255.0.1"));
    }

    @Test
    @DisplayName("SsrfGuard isBlockedAddress blocks 192.168.x.x private")
    void blocks192Network() {
        assertTrue(SsrfGuard.isBlockedAddress("192.168.0.1"));
        assertTrue(SsrfGuard.isBlockedAddress("192.168.1.1"));
        assertTrue(SsrfGuard.isBlockedAddress("192.168.255.255"));
    }

    @Test
    @DisplayName("SsrfGuard isBlockedAddress allows 192.x.x.x outside 168")
    void allows192Outside168() {
        assertFalse(SsrfGuard.isBlockedAddress("192.0.0.1"));
        assertFalse(SsrfGuard.isBlockedAddress("192.167.0.1"));
        assertFalse(SsrfGuard.isBlockedAddress("192.169.0.1"));
    }

    @Test
    @DisplayName("SsrfGuard isBlockedAddress allows 127.x.x.x loopback")
    void allowsLoopback() {
        assertFalse(SsrfGuard.isBlockedAddress("127.0.0.1"));
        assertFalse(SsrfGuard.isBlockedAddress("127.0.0.1")); // localhost
        assertFalse(SsrfGuard.isBlockedAddress("127.255.255.255"));
    }

    @Test
    @DisplayName("SsrfGuard isBlockedAddress blocks 100.64-127.x.x CGNAT")
    void blocksCgnat() {
        assertTrue(SsrfGuard.isBlockedAddress("100.64.0.1"));
        assertTrue(SsrfGuard.isBlockedAddress("100.100.0.1"));
        assertTrue(SsrfGuard.isBlockedAddress("100.127.255.255"));
    }

    @Test
    @DisplayName("SsrfGuard isBlockedAddress allows 100.x.x.x outside CGNAT")
    void allows100OutsideCgnat() {
        assertFalse(SsrfGuard.isBlockedAddress("100.0.0.1"));
        assertFalse(SsrfGuard.isBlockedAddress("100.63.0.1"));
        assertFalse(SsrfGuard.isBlockedAddress("100.128.0.1"));
    }

    @Test
    @DisplayName("SsrfGuard isBlockedAddress allows public IPs")
    void allowsPublicIps() {
        assertFalse(SsrfGuard.isBlockedAddress("8.8.8.8"));
        assertFalse(SsrfGuard.isBlockedAddress("1.1.1.1"));
        assertFalse(SsrfGuard.isBlockedAddress("208.67.222.222"));
    }

    @Test
    @DisplayName("SsrfGuard isBlockedAddress IPv6 loopback ::1 allowed")
    void allowsIpv6Loopback() {
        assertFalse(SsrfGuard.isBlockedAddress("::1"));
    }

    @Test
    @DisplayName("SsrfGuard isBlockedAddress IPv6 unspecified :: blocked")
    void blocksIpv6Unspecified() {
        assertTrue(SsrfGuard.isBlockedAddress("::"));
        assertTrue(SsrfGuard.isBlockedAddress("0:0:0:0:0:0:0:0"));
    }

    @Test
    @DisplayName("SsrfGuard isBlockedAddress IPv6 unique local fc00::/7 blocked")
    void blocksIpv6UniqueLocal() {
        assertTrue(SsrfGuard.isBlockedAddress("fc00::1"));
        assertTrue(SsrfGuard.isBlockedAddress("fd00::1"));
        assertTrue(SsrfGuard.isBlockedAddress("fcff::1"));
    }

    @Test
    @DisplayName("SsrfGuard isBlockedAddress IPv4-mapped IPv6")
    void blocksIpv4MappedIpv6() {
        // ::ffff:10.0.0.1 should be blocked (10.x is private)
        assertTrue(SsrfGuard.isBlockedAddress("::ffff:10.0.0.1"));
        // ::ffff:127.0.0.1 should be allowed (loopback)
        assertFalse(SsrfGuard.isBlockedAddress("::ffff:127.0.0.1"));
    }

    @Test
    @DisplayName("SsrfGuard isBlockedAddress IPv6 public allowed")
    void allowsIpv6Public() {
        assertFalse(SsrfGuard.isBlockedAddress("2001:4860:4860::8888")); // Google DNS
    }

    @Test
    @DisplayName("SsrfGuard isBlockedAddress invalid IP returns false")
    void invalidIpReturnsFalse() {
        assertFalse(SsrfGuard.isBlockedAddress("not-an-ip"));
        assertFalse(SsrfGuard.isBlockedAddress("999.999.999.999"));
    }

    @Test
    @DisplayName("SsrfGuard SsrfException properties")
    void ssrfExceptionProperties() {
        SsrfGuard.SsrfException ex = new SsrfGuard.SsrfException("example.com", "10.0.0.1");

        assertEquals("example.com", ex.getHostname());
        assertEquals("10.0.0.1", ex.getAddress());
        assertTrue(ex.getMessage().contains("example.com"));
        assertTrue(ex.getMessage().contains("10.0.0.1"));
    }

    @Test
    @DisplayName("SsrfGuard isUrlSafe allows public URLs")
    void isUrlSafePublic() {
        assertTrue(SsrfGuard.isUrlSafe("http://example.com/path"));
        assertTrue(SsrfGuard.isUrlSafe("https://api.anthropic.com/v1"));
    }

    @Test
    @DisplayName("SsrfGuard isUrlSafe allows localhost")
    void isUrlSafeLocalhost() {
        assertTrue(SsrfGuard.isUrlSafe("http://127.0.0.1:8080/hook"));
        assertTrue(SsrfGuard.isUrlSafe("http://localhost:3000/api"));
    }

    @Test
    @DisplayName("SsrfGuard isUrlSafe blocks private IPs")
    void isUrlSafeBlocksPrivate() {
        assertFalse(SsrfGuard.isUrlSafe("http://10.0.0.1/internal"));
        assertFalse(SsrfGuard.isUrlSafe("http://192.168.1.1/admin"));
        assertFalse(SsrfGuard.isUrlSafe("http://169.254.169.254/metadata"));
    }

    @Test
    @DisplayName("SsrfGuard isUrlSafe invalid URL returns true")
    void isUrlSafeInvalid() {
        // Invalid URLs return true (let the actual request fail)
        assertTrue(SsrfGuard.isUrlSafe("not-a-url"));
    }

    @Test
    @DisplayName("SsrfGuard guardedLookup returns addresses for valid hostname")
    void guardedLookupValid() throws Exception {
        InetAddress[] addresses = SsrfGuard.guardedLookup("localhost").get();

        assertTrue(addresses.length > 0);
    }
}