/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/auth
 */
package com.anthropic.claudecode.services.mcp;

import java.util.*;
import java.util.concurrent.*;
import java.time.*;

/**
 * MCP auth - MCP authentication utilities.
 */
public final class McpAuth {
    private final Map<String, AuthState> authStates = new ConcurrentHashMap<>();

    /**
     * Auth state record.
     */
    public record AuthState(
        String serverName,
        AuthStatus status,
        String token,
        Instant expiresAt,
        String refreshToken,
        Map<String, String> additionalData
    ) {
        public boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }

        public boolean needsRefresh() {
            if (expiresAt == null) return false;
            // Refresh 5 minutes before expiry
            return Instant.now().isAfter(expiresAt.minusSeconds(300));
        }
    }

    /**
     * Auth status enum.
     */
    public enum AuthStatus {
        UNAUTHENTICATED,
        PENDING,
        AUTHENTICATED,
        EXPIRED,
        ERROR
    }

    /**
     * Auth config record.
     */
    public record AuthConfig(
        String serverName,
        String authType,
        String clientId,
        String clientSecret,
        String scope,
        String redirectUri,
        String authUrl,
        String tokenUrl
    ) {
        public static AuthConfig oauth(String serverName, String clientId, String authUrl, String tokenUrl) {
            return new AuthConfig(serverName, "oauth", clientId, null, null, null, authUrl, tokenUrl);
        }

        public static AuthConfig apiKey(String serverName) {
            return new AuthConfig(serverName, "api_key", null, null, null, null, null, null);
        }
    }

    /**
     * Check if authenticated.
     */
    public boolean isAuthenticated(String serverName) {
        AuthState state = authStates.get(serverName);
        if (state == null) return false;

        if (state.isExpired()) {
            authStates.put(serverName, new AuthState(
                serverName, AuthStatus.EXPIRED, null, null, null, null
            ));
            return false;
        }

        return state.status() == AuthStatus.AUTHENTICATED;
    }

    /**
     * Get auth state.
     */
    public AuthState getAuthState(String serverName) {
        return authStates.get(serverName);
    }

    /**
     * Set authenticated.
     */
    public void setAuthenticated(String serverName, String token, Instant expiresAt, String refreshToken) {
        authStates.put(serverName, new AuthState(
            serverName, AuthStatus.AUTHENTICATED, token, expiresAt, refreshToken, null
        ));
    }

    /**
     * Clear auth.
     */
    public void clearAuth(String serverName) {
        authStates.remove(serverName);
    }

    /**
     * Get token.
     */
    public Optional<String> getToken(String serverName) {
        AuthState state = authStates.get(serverName);
        if (state == null || state.isExpired()) {
            return Optional.empty();
        }
        return Optional.ofNullable(state.token());
    }

    /**
     * Refresh token if needed.
     */
    public CompletableFuture<Boolean> refreshTokenIfNeeded(String serverName) {
        AuthState state = authStates.get(serverName);
        if (state == null || !state.needsRefresh()) {
            return CompletableFuture.completedFuture(true);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Load refresh token and call token endpoint
                if (state.refreshToken() == null) {
                    return false;
                }

                // Find auth config for this server
                AuthConfig config = loadAuthConfig(serverName);
                if (config == null || config.tokenUrl() == null) {
                    return false;
                }

                // Call token endpoint with refresh_token grant
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                String formBody = "grant_type=refresh_token&refresh_token=" +
                    java.net.URLEncoder.encode(state.refreshToken(), java.nio.charset.StandardCharsets.UTF_8);

                if (config.clientId() != null) {
                    formBody += "&client_id=" + java.net.URLEncoder.encode(config.clientId(), java.nio.charset.StandardCharsets.UTF_8);
                }

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(config.tokenUrl()))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // Parse token response
                    TokenResponse tokenResp = parseTokenResponse(response.body());
                    if (tokenResp != null) {
                        setAuthenticated(serverName, tokenResp.accessToken, tokenResp.expiresAt, tokenResp.refreshToken);
                        return true;
                    }
                }

                return false;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Token response record.
     */
    private record TokenResponse(String accessToken, Instant expiresAt, String refreshToken) {}

    /**
     * Parse token response JSON.
     */
    private TokenResponse parseTokenResponse(String json) {
        try {
            String accessToken = null;
            String refreshToken = null;
            int expiresIn = 3600;

            int accessIdx = json.indexOf("\"access_token\"");
            if (accessIdx >= 0) {
                int valStart = json.indexOf("\"", accessIdx + 14) + 1;
                int valEnd = json.indexOf("\"", valStart);
                accessToken = json.substring(valStart, valEnd);
            }

            int refreshIdx = json.indexOf("\"refresh_token\"");
            if (refreshIdx >= 0) {
                int valStart = json.indexOf("\"", refreshIdx + 15) + 1;
                int valEnd = json.indexOf("\"", valStart);
                refreshToken = json.substring(valStart, valEnd);
            }

            int expiresIdx = json.indexOf("\"expires_in\"");
            if (expiresIdx >= 0) {
                int valStart = json.indexOf(":", expiresIdx + 12) + 1;
                while (valStart < json.length() && Character.isWhitespace(json.charAt(valStart))) valStart++;
                int valEnd = valStart;
                while (valEnd < json.length() && Character.isDigit(json.charAt(valEnd))) valEnd++;
                expiresIn = Integer.parseInt(json.substring(valStart, valEnd));
            }

            if (accessToken != null) {
                return new TokenResponse(accessToken, Instant.now().plusSeconds(expiresIn), refreshToken);
            }
        } catch (Exception e) {
            // Parse error
        }
        return null;
    }

    /**
     * Load auth config for server.
     */
    private AuthConfig loadAuthConfig(String serverName) {
        try {
            java.nio.file.Path configPath = java.nio.file.Paths.get(
                System.getProperty("user.home"), ".claude", "mcp-auth.json"
            );

            if (!java.nio.file.Files.exists(configPath)) {
                return null;
            }

            String content = java.nio.file.Files.readString(configPath);
            // Find config for this server
            int serverIdx = content.indexOf("\"" + serverName + "\"");
            if (serverIdx < 0) return null;

            // Find config object
            int objStart = content.lastIndexOf("{", serverIdx);
            int objEnd = content.indexOf("}", serverIdx) + 1;

            if (objStart < 0 || objEnd <= objStart) return null;

            String obj = content.substring(objStart, objEnd);

            String clientId = extractJsonValueString(obj, "client_id");
            String authUrl = extractJsonValueString(obj, "auth_url");
            String tokenUrl = extractJsonValueString(obj, "token_url");

            return new AuthConfig(serverName, "oauth", clientId, null, null, null, authUrl, tokenUrl);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractJsonValueString(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int valStart = json.indexOf("\"", idx + key.length() + 2) + 1;
        if (valStart < 1) return null;
        int valEnd = json.indexOf("\"", valStart);
        return json.substring(valStart, valEnd);
    }

    /**
     * Auth result record.
     */
    public record AuthResult(
        boolean success,
        String token,
        String error,
        AuthStatus status
    ) {
        public static AuthResult success(String token) {
            return new AuthResult(true, token, null, AuthStatus.AUTHENTICATED);
        }

        public static AuthResult failure(String error) {
            return new AuthResult(false, null, error, AuthStatus.ERROR);
        }

        public static AuthResult pending() {
            return new AuthResult(false, null, null, AuthStatus.PENDING);
        }
    }

    /**
     * Start auth flow.
     */
    public CompletableFuture<AuthResult> startAuthFlow(AuthConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if ("oauth".equals(config.authType())) {
                    // Build authorization URL
                    String authUrl = buildAuthorizationUrl(config);

                    // Open browser for OAuth
                    openBrowser(authUrl);

                    // Store pending state
                    authStates.put(config.serverName(), new AuthState(
                        config.serverName(), AuthStatus.PENDING, null, null, null,
                        Map.of("auth_url", authUrl)
                    ));

                    return AuthResult.pending();
                } else if ("api_key".equals(config.authType())) {
                    // For API key auth, check environment or prompt user
                    String apiKey = System.getenv(config.serverName().toUpperCase() + "_API_KEY");
                    if (apiKey != null) {
                        setAuthenticated(config.serverName(), apiKey, null, null);
                        return AuthResult.success(apiKey);
                    }

                    return AuthResult.pending();
                }

                return AuthResult.failure("Unsupported auth type: " + config.authType());
            } catch (Exception e) {
                return AuthResult.failure(e.getMessage());
            }
        });
    }

    /**
     * Build OAuth authorization URL.
     */
    private String buildAuthorizationUrl(AuthConfig config) {
        StringBuilder url = new StringBuilder(config.authUrl());
        url.append("?response_type=code");
        url.append("&client_id=").append(java.net.URLEncoder.encode(config.clientId(), java.nio.charset.StandardCharsets.UTF_8));

        if (config.scope() != null) {
            url.append("&scope=").append(java.net.URLEncoder.encode(config.scope(), java.nio.charset.StandardCharsets.UTF_8));
        }

        if (config.redirectUri() != null) {
            url.append("&redirect_uri=").append(java.net.URLEncoder.encode(config.redirectUri(), java.nio.charset.StandardCharsets.UTF_8));
        }

        // Add state for CSRF protection
        String state = UUID.randomUUID().toString();
        url.append("&state=").append(state);

        return url.toString();
    }

    /**
     * Open browser to URL.
     */
    private void openBrowser(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("mac")) {
                pb = new ProcessBuilder("open", url);
            } else if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "start", url);
            } else {
                pb = new ProcessBuilder("xdg-open", url);
            }

            pb.start();
        } catch (Exception e) {
            // Ignore browser open errors
        }
    }

    /**
     * Complete auth flow.
     */
    public AuthResult completeAuthFlow(String serverName, String authCode) {
        try {
            AuthConfig config = loadAuthConfig(serverName);
            if (config == null || config.tokenUrl() == null) {
                return AuthResult.failure("No auth config found for server: " + serverName);
            }

            // Exchange authorization code for token
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            String formBody = "grant_type=authorization_code&code=" +
                java.net.URLEncoder.encode(authCode, java.nio.charset.StandardCharsets.UTF_8);

            if (config.clientId() != null) {
                formBody += "&client_id=" + java.net.URLEncoder.encode(config.clientId(), java.nio.charset.StandardCharsets.UTF_8);
            }

            if (config.redirectUri() != null) {
                formBody += "&redirect_uri=" + java.net.URLEncoder.encode(config.redirectUri(), java.nio.charset.StandardCharsets.UTF_8);
            }

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(config.tokenUrl()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(formBody))
                .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                TokenResponse tokenResp = parseTokenResponse(response.body());
                if (tokenResp != null) {
                    setAuthenticated(serverName, tokenResp.accessToken, tokenResp.expiresAt, tokenResp.refreshToken);
                    return AuthResult.success(tokenResp.accessToken);
                }
            }

            return AuthResult.failure("Token exchange failed: HTTP " + response.statusCode());
        } catch (Exception e) {
            return AuthResult.failure(e.getMessage());
        }
    }

    /**
     * Auth header record.
     */
    public record AuthHeader(String name, String value) {
        public static AuthHeader bearer(String token) {
            return new AuthHeader("Authorization", "Bearer " + token);
        }

        public static AuthHeader apiKey(String key) {
            return new AuthHeader("X-API-Key", key);
        }
    }

    /**
     * Get auth header.
     */
    public Optional<AuthHeader> getAuthHeader(String serverName) {
        return getToken(serverName).map(AuthHeader::bearer);
    }
}