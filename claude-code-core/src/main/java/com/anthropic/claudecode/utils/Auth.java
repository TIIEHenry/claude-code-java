/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/auth
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Auth - Authentication utilities.
 */
public final class Auth {
    private static volatile AuthStatus currentStatus = AuthStatus.UNAUTHENTICATED;
    private static volatile String authToken = null;
    private static volatile String refreshToken = null;
    private static volatile long tokenExpiry = 0;

    /**
     * Auth status enum.
     */
    public enum AuthStatus {
        UNAUTHENTICATED,
        AUTHENTICATING,
        AUTHENTICATED,
        EXPIRED,
        ERROR
    }

    /**
     * Check if authenticated.
     */
    public static boolean isAuthenticated() {
        if (currentStatus == AuthStatus.AUTHENTICATED) {
            // Check expiry
            if (tokenExpiry > 0 && System.currentTimeMillis() > tokenExpiry) {
                currentStatus = AuthStatus.EXPIRED;
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Get current status.
     */
    public static AuthStatus getStatus() {
        return currentStatus;
    }

    /**
     * Get auth token.
     */
    public static String getAuthToken() {
        return authToken;
    }

    /**
     * Set auth token.
     */
    public static void setAuthToken(String token, long expiryMs) {
        authToken = token;
        tokenExpiry = expiryMs;
        currentStatus = AuthStatus.AUTHENTICATED;
    }

    /**
     * Clear auth.
     */
    public static void clearAuth() {
        authToken = null;
        refreshToken = null;
        tokenExpiry = 0;
        currentStatus = AuthStatus.UNAUTHENTICATED;
    }

    /**
     * Refresh token if needed.
     */
    public static CompletableFuture<Boolean> refreshTokenIfNeeded() {
        if (currentStatus != AuthStatus.EXPIRED || refreshToken == null) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            currentStatus = AuthStatus.AUTHENTICATING;
            try {
                // Load OAuth config
                String tokenUrl = System.getenv("ANTHROPIC_TOKEN_URL");
                if (tokenUrl == null) {
                    tokenUrl = "https://api.claude.ai/oauth/token";
                }

                // Call refresh endpoint
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                String formBody = "grant_type=refresh_token&refresh_token=" +
                    java.net.URLEncoder.encode(refreshToken, java.nio.charset.StandardCharsets.UTF_8);

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // Parse response
                    String newToken = extractAccessToken(response.body());
                    int expiresIn = extractExpiresIn(response.body());

                    if (newToken != null) {
                        authToken = newToken;
                        tokenExpiry = System.currentTimeMillis() + (expiresIn * 1000L);
                        currentStatus = AuthStatus.AUTHENTICATED;
                        return true;
                    }
                }

                currentStatus = AuthStatus.ERROR;
                return false;
            } catch (Exception e) {
                currentStatus = AuthStatus.ERROR;
                return false;
            }
        });
    }

    /**
     * Extract access token from JSON response.
     */
    private static String extractAccessToken(String json) {
        int idx = json.indexOf("\"access_token\"");
        if (idx < 0) return null;
        int valStart = json.indexOf("\"", idx + 14) + 1;
        int valEnd = json.indexOf("\"", valStart);
        return json.substring(valStart, valEnd);
    }

    /**
     * Extract expires_in from JSON response.
     */
    private static int extractExpiresIn(String json) {
        int idx = json.indexOf("\"expires_in\"");
        if (idx < 0) return 3600; // Default 1 hour
        int valStart = json.indexOf(":", idx + 12) + 1;
        while (valStart < json.length() && Character.isWhitespace(json.charAt(valStart))) valStart++;
        int valEnd = valStart;
        while (valEnd < json.length() && Character.isDigit(json.charAt(valEnd))) valEnd++;
        return Integer.parseInt(json.substring(valStart, valEnd));
    }

    /**
     * Auth info record.
     */
    public record AuthInfo(
        String userId,
        String email,
        String organizationId,
        long authenticatedAt,
        long expiresAt
    ) {
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }

        public long getRemainingTime() {
            return expiresAt - System.currentTimeMillis();
        }
    }

    /**
     * OAuth config record.
     */
    public record OAuthConfig(
        String clientId,
        String redirectUri,
        String scope,
        String authorizeUrl,
        String tokenUrl
    ) {}
}