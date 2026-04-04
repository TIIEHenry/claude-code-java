/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code API preconnect utilities
 */
package com.anthropic.claudecode.utils;

import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.concurrent.*;

/**
 * Preconnect to the Anthropic API to overlap TCP+TLS handshake with startup.
 *
 * The TCP+TLS handshake is ~100-200ms that normally blocks inside the first
 * API call. Kicking a fire-and-forget request during init lets the handshake
 * happen in parallel with other setup work.
 */
public final class ApiPreconnect {
    private ApiPreconnect() {}

    private static volatile boolean fired = false;

    /**
     * Preconnect to Anthropic API.
     * Fires a HEAD request to warm up the connection pool.
     */
    public static void preconnectAnthropicApi() {
        if (fired) return;
        fired = true;

        // Skip if using a cloud provider - different endpoint + auth
        if (AwsUtils.isBedrockConfigured() ||
                AwsUtils.isVertexConfigured() ||
                AwsUtils.isFoundryConfigured()) {
            return;
        }

        // Skip if proxy/mTLS/unix - custom dispatcher won't reuse pool
        if (System.getenv("HTTPS_PROXY") != null ||
                System.getenv("https_proxy") != null ||
                System.getenv("HTTP_PROXY") != null ||
                System.getenv("http_proxy") != null ||
                System.getenv("ANTHROPIC_UNIX_SOCKET") != null ||
                System.getenv("CLAUDE_CODE_CLIENT_CERT") != null ||
                System.getenv("CLAUDE_CODE_CLIENT_KEY") != null) {
            return;
        }

        // Get base URL
        String baseUrlEnv = System.getenv("ANTHROPIC_BASE_URL");
        final String baseUrl = baseUrlEnv != null ? baseUrlEnv : "https://api.anthropic.com";

        // Fire and forget HEAD request with 10s timeout
        CompletableFuture.runAsync(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl))
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .timeout(Duration.ofSeconds(10))
                        .build();

                client.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                // Ignore - the real request will handshake fresh if needed
            }
        });
    }

    /**
     * Reset for testing.
     */
    public static void reset() {
        fired = false;
    }

    /**
     * Check if preconnect was fired.
     */
    public static boolean wasFired() {
        return fired;
    }
}