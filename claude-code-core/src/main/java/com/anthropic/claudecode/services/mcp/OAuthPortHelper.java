/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/oauthPort
 */
package com.anthropic.claudecode.services.mcp;

import java.util.concurrent.CompletableFuture;

import java.util.*;
import java.net.*;
import java.io.IOException;
import java.util.random.RandomGenerator;

/**
 * OAuth redirect port helpers.
 *
 * RFC 8252 Section 7.3 (OAuth for Native Apps): loopback redirect URIs match
 * any port as long as the path matches.
 */
public final class OAuthPortHelper {
    // Windows dynamic port range 49152-65535 is reserved
    private static final PortRange REDIRECT_PORT_RANGE = getRedirectPortRange();
    private static final int REDIRECT_PORT_FALLBACK = 3118;

    /**
     * Port range record.
     */
    public record PortRange(int min, int max) {}

    /**
     * Build redirect URI on localhost with given port.
     */
    public static String buildRedirectUri(int port) {
        return "http://localhost:" + port + "/callback";
    }

    /**
     * Build redirect URI with fallback port.
     */
    public static String buildRedirectUri() {
        return buildRedirectUri(REDIRECT_PORT_FALLBACK);
    }

    /**
     * Get configured MCP OAuth callback port.
     */
    public static int getMcpOAuthCallbackPort() {
        String portStr = System.getenv("MCP_OAUTH_CALLBACK_PORT");
        if (portStr != null && !portStr.isEmpty()) {
            try {
                int port = Integer.parseInt(portStr);
                return port > 0 ? port : -1;
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Find available port in the specified range.
     * Uses random selection for better security.
     */
    public static int findAvailablePort() {
        // First, try the configured port if specified
        int configuredPort = getMcpOAuthCallbackPort();
        if (configuredPort > 0) {
            if (isPortAvailable(configuredPort)) {
                return configuredPort;
            }
        }

        int min = REDIRECT_PORT_RANGE.min();
        int max = REDIRECT_PORT_RANGE.max();
        int range = max - min + 1;
        int maxAttempts = Math.min(range, 100);

        RandomGenerator rng = RandomGenerator.getDefault();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int port = min + rng.nextInt(range);

            if (isPortAvailable(port)) {
                return port;
            }
        }

        // Try fallback port
        if (isPortAvailable(REDIRECT_PORT_FALLBACK)) {
            return REDIRECT_PORT_FALLBACK;
        }

        throw new RuntimeException("No available ports for OAuth redirect");
    }

    /**
     * Find available port asynchronously.
     */
    public static CompletableFuture<Integer> findAvailablePortAsync() {
        return CompletableFuture.supplyAsync(OAuthPortHelper::findAvailablePort);
    }

    /**
     * Check if port is available.
     */
    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get redirect port range based on platform.
     */
    private static PortRange getRedirectPortRange() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new PortRange(39152, 49151);
        }
        return new PortRange(49152, 65535);
    }
}