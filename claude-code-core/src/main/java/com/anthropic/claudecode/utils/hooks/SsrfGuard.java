/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/ssrfGuard.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * SSRF guard for HTTP hooks.
 *
 * Blocks private, link-local, and other non-routable address ranges to prevent
 * project-configured HTTP hooks from reaching cloud metadata endpoints
 * (169.254.169.254) or internal infrastructure.
 *
 * Loopback (127.0.0.0/8, ::1) is intentionally ALLOWED — local dev policy
 * servers are a primary HTTP hook use case.
 */
public final class SsrfGuard {
    private SsrfGuard() {}

    /**
     * Returns true if the address is in a range that HTTP hooks should not reach.
     *
     * Blocked IPv4:
     *   0.0.0.0/8        "this" network
     *   10.0.0.0/8       private
     *   100.64.0.0/10    shared address space / CGNAT
     *   169.254.0.0/16   link-local (cloud metadata)
     *   172.16.0.0/12    private
     *   192.168.0.0/16   private
     *
     * Blocked IPv6:
     *   ::               unspecified
     *   fc00::/7         unique local
     *   fe80::/10        link-local
     *   ::ffff:<v4>      mapped IPv4 in a blocked range
     *
     * Allowed (returns false):
     *   127.0.0.0/8      loopback (local dev hooks)
     *   ::1              loopback
     */
    public static boolean isBlockedAddress(String address) {
        try {
            InetAddress inetAddress = InetAddress.getByName(address);

            if (inetAddress instanceof Inet4Address v4) {
                return isBlockedV4(v4);
            } else if (inetAddress instanceof Inet6Address v6) {
                return isBlockedV6(v6);
            }

            return false;
        } catch (Exception e) {
            // Not a valid IP — let DNS handle it
            return false;
        }
    }

    private static boolean isBlockedV4(Inet4Address addr) {
        byte[] bytes = addr.getAddress();

        int a = bytes[0] & 0xFF;
        int b = bytes[1] & 0xFF;

        // Loopback explicitly allowed
        if (a == 127) return false;

        // 0.0.0.0/8
        if (a == 0) return true;

        // 10.0.0.0/8
        if (a == 10) return true;

        // 169.254.0.0/16 — link-local, cloud metadata
        if (a == 169 && b == 254) return true;

        // 172.16.0.0/12
        if (a == 172 && b >= 16 && b <= 31) return true;

        // 100.64.0.0/10 — shared address space (CGNAT)
        if (a == 100 && b >= 64 && b <= 127) return true;

        // 192.168.0.0/16
        if (a == 192 && b == 168) return true;

        return false;
    }

    private static boolean isBlockedV6(Inet6Address addr) {
        byte[] bytes = addr.getAddress();

        // ::1 loopback explicitly allowed
        if (isLoopbackV6(bytes)) return false;

        // :: unspecified
        if (isUnspecifiedV6(bytes)) return true;

        // IPv4-mapped IPv6 (::ffff:X:Y)
        if (isIpv4MappedV6(bytes)) {
            // Extract the embedded IPv4 and check it
            byte[] v4Bytes = new byte[4];
            v4Bytes[0] = bytes[12];
            v4Bytes[1] = bytes[13];
            v4Bytes[2] = bytes[14];
            v4Bytes[3] = bytes[15];

            try {
                Inet4Address v4Addr = (Inet4Address) InetAddress.getByAddress(v4Bytes);
                return isBlockedV4(v4Addr);
            } catch (Exception e) {
                return false;
            }
        }

        // fc00::/7 — unique local addresses
        int firstByte = bytes[0] & 0xFF;
        if ((firstByte & 0xFE) == 0xFC) return true;

        // fe80::/10 — link-local
        if ((firstByte & 0xFF) >= 0xFE && (firstByte & 0xC0) == 0x80) {
            return true;
        }

        return false;
    }

    private static boolean isLoopbackV6(byte[] bytes) {
        // ::1 = all zeros except last byte is 1
        for (int i = 0; i < 15; i++) {
            if (bytes[i] != 0) return false;
        }
        return bytes[15] == 1;
    }

    private static boolean isUnspecifiedV6(byte[] bytes) {
        // :: = all zeros
        for (byte b : bytes) {
            if (b != 0) return false;
        }
        return true;
    }

    private static boolean isIpv4MappedV6(byte[] bytes) {
        // IPv4-mapped: first 80 bits zero, next 16 bits ffff
        // 0:0:0:0:0:ffff:X:Y
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) return false;
        }
        return bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF;
    }

    /**
     * DNS lookup with SSRF guard.
     * Validates resolved addresses before returning.
     */
    public static CompletableFuture<InetAddress[]> guardedLookup(String hostname) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // If hostname is already an IP literal, validate it directly
                InetAddress directAddr = InetAddress.getByName(hostname);
                if (directAddr.getHostAddress().equals(hostname)) {
                    if (isBlockedAddress(hostname)) {
                        throw new SsrfException(hostname, hostname);
                    }
                    return new InetAddress[] { directAddr };
                }

                // DNS lookup
                InetAddress[] addresses = InetAddress.getAllByName(hostname);

                for (InetAddress addr : addresses) {
                    if (isBlockedAddress(addr.getHostAddress())) {
                        throw new SsrfException(hostname, addr.getHostAddress());
                    }
                }

                return addresses;
            } catch (SsrfException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("DNS lookup failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * SSRF exception.
     */
    public static class SsrfException extends RuntimeException {
        private final String hostname;
        private final String address;

        public SsrfException(String hostname, String address) {
            super("HTTP hook blocked: " + hostname + " resolves to " + address +
                  " (private/link-local address). Loopback (127.0.0.1, ::1) is allowed for local dev.");
            this.hostname = hostname;
            this.address = address;
        }

        public String getHostname() { return hostname; }
        public String getAddress() { return address; }
    }

    /**
     * Check if a URL is safe to access via HTTP hook.
     */
    public static boolean isUrlSafe(String urlString) {
        try {
            URL url = new URL(urlString);
            String host = url.getHost();

            // Check if host is an IP literal
            InetAddress addr = InetAddress.getByName(host);
            if (addr.getHostAddress().equals(host)) {
                return !isBlockedAddress(host);
            }

            // For hostnames, we need to resolve first
            // This is a synchronous check - use guardedLookup for async
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress a : addresses) {
                if (isBlockedAddress(a.getHostAddress())) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            // If we can't parse or resolve, let it proceed
            // The actual request will fail anyway
            return true;
        }
    }
}