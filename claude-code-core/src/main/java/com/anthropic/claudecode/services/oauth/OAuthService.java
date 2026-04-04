/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/oauth
 */
package com.anthropic.claudecode.services.oauth;

import java.util.*;
import java.util.concurrent.*;
import java.security.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * OAuth service - OAuth authentication service.
 */
public final class OAuthService {
    private final OAuthConfig config;
    private volatile OAuthState currentState;
    private final CompletableFuture<String> authFuture = new CompletableFuture<>();

    /**
     * OAuth config record.
     */
    public record OAuthConfig(
        String clientId,
        String redirectUri,
        String authUrl,
        String tokenUrl,
        List<String> scopes,
        boolean usePKCE
    ) {
        public static OAuthConfig forAnthropic() {
            return new OAuthConfig(
                "claude-code-cli",
                "http://localhost:8080/callback",
                "https://auth.anthropic.com/oauth/authorize",
                "https://auth.anthropic.com/oauth/token",
                List.of("api:read", "api:write"),
                true
            );
        }
    }

    /**
     * OAuth state record.
     */
    public record OAuthState(
        String state,
        String codeVerifier,
        String codeChallenge,
        String authorizationUrl,
        OAuthStatus status,
        long createdAt
    ) {
        public static OAuthState pending(String state, String verifier, String challenge, String url) {
            return new OAuthState(state, verifier, challenge, url, OAuthStatus.PENDING, System.currentTimeMillis());
        }

        public static OAuthState completed() {
            return new OAuthState(null, null, null, null, OAuthStatus.COMPLETED, System.currentTimeMillis());
        }

        public static OAuthState failed(String error) {
            return new OAuthState(null, null, null, error, OAuthStatus.FAILED, System.currentTimeMillis());
        }
    }

    /**
     * OAuth status enum.
     */
    public enum OAuthStatus {
        PENDING,
        AUTHORIZING,
        EXCHANGING,
        COMPLETED,
        FAILED,
        TIMEOUT
    }

    /**
     * Create OAuth service.
     */
    public OAuthService(OAuthConfig config) {
        this.config = config;
        this.currentState = null;
    }

    /**
     * Start OAuth flow.
     */
    public CompletableFuture<String> startAuthFlow() {
        try {
            // Generate state and PKCE values
            String state = generateRandomString(32);
            String codeVerifier = generateRandomString(64);
            String codeChallenge = generateCodeChallenge(codeVerifier);

            // Build authorization URL
            String authUrl = buildAuthorizationUrl(state, codeChallenge);

            // Create pending state
            currentState = OAuthState.pending(state, codeVerifier, codeChallenge, authUrl);

            // Start callback server
            startCallbackServer();

            return authFuture;
        } catch (Exception e) {
            authFuture.completeExceptionally(e);
            return authFuture;
        }
    }

    /**
     * Build authorization URL.
     */
    private String buildAuthorizationUrl(String state, String codeChallenge) {
        StringBuilder url = new StringBuilder(config.authUrl());
        url.append("?client_id=").append(URLEncoder.encode(config.clientId(), StandardCharsets.UTF_8));
        url.append("&redirect_uri=").append(URLEncoder.encode(config.redirectUri(), StandardCharsets.UTF_8));
        url.append("&response_type=code");
        url.append("&scope=").append(URLEncoder.encode(String.join(" ", config.scopes()), StandardCharsets.UTF_8));
        url.append("&state=").append(state);

        if (config.usePKCE()) {
            url.append("&code_challenge=").append(codeChallenge);
            url.append("&code_challenge_method=S256");
        }

        return url.toString();
    }

    /**
     * Generate random string.
     */
    private String generateRandomString(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generate code challenge.
     */
    private String generateCodeChallenge(String verifier) throws Exception {
        byte[] bytes = verifier.getBytes("UTF-8");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    /**
     * Handle callback.
     */
    public void handleCallback(String code, String state) {
        if (currentState == null || !currentState.state().equals(state)) {
            currentState = OAuthState.failed("Invalid state");
            authFuture.completeExceptionally(new OAuthException("Invalid state"));
            return;
        }

        try {
            // Exchange code for token
            currentState = new OAuthState(
                currentState.state(),
                currentState.codeVerifier(),
                currentState.codeChallenge(),
                currentState.authorizationUrl(),
                OAuthStatus.EXCHANGING,
                System.currentTimeMillis()
            );

            String token = exchangeCodeForToken(code);

            currentState = OAuthState.completed();
            authFuture.complete(token);
        } catch (Exception e) {
            currentState = OAuthState.failed(e.getMessage());
            authFuture.completeExceptionally(e);
        }
    }

    /**
     * Exchange code for token.
     */
    private String exchangeCodeForToken(String code) throws Exception {
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();

        // Build form body for token exchange
        StringBuilder formBody = new StringBuilder();
        formBody.append("grant_type=authorization_code");
        formBody.append("&code=").append(URLEncoder.encode(code, StandardCharsets.UTF_8));
        formBody.append("&client_id=").append(URLEncoder.encode(config.clientId(), StandardCharsets.UTF_8));
        formBody.append("&redirect_uri=").append(URLEncoder.encode(config.redirectUri(), StandardCharsets.UTF_8));

        if (config.usePKCE() && currentState.codeVerifier() != null) {
            formBody.append("&code_verifier=").append(URLEncoder.encode(currentState.codeVerifier(), StandardCharsets.UTF_8));
        }

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(config.tokenUrl()))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(formBody.toString()))
            .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Parse access_token from response
            return parseAccessToken(response.body());
        }

        throw new OAuthException("Token exchange failed: HTTP " + response.statusCode());
    }

    /**
     * Parse access token from JSON response.
     */
    private String parseAccessToken(String json) {
        int accessIdx = json.indexOf("\"access_token\"");
        if (accessIdx < 0) return null;

        int valStart = json.indexOf("\"", accessIdx + 14) + 1;
        int valEnd = json.indexOf("\"", valStart);

        if (valStart > 0 && valEnd > valStart) {
            return json.substring(valStart, valEnd);
        }
        return null;
    }

    /**
     * Start callback server.
     */
    private void startCallbackServer() {
        // Parse port from redirect URI
        int port = parsePortFromRedirectUri();

        // Start simple HTTP server to receive OAuth callback
        CompletableFuture.runAsync(() -> {
            try {
                java.net.ServerSocket serverSocket = new java.net.ServerSocket(port);
                serverSocket.setSoTimeout(60000); // 60 second timeout

                java.net.Socket client = serverSocket.accept();
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(client.getInputStream())
                );

                // Read HTTP request
                String line = reader.readLine();
                if (line != null && line.startsWith("GET")) {
                    // Parse query string
                    String[] parts = line.split(" ");
                    if (parts.length >= 2) {
                        String path = parts[1];
                        int queryIdx = path.indexOf('?');
                        if (queryIdx >= 0) {
                            String query = path.substring(queryIdx + 1);
                            String code = null;
                            String state = null;

                            for (String param : query.split("&")) {
                                String[] kv = param.split("=");
                                if (kv.length == 2) {
                                    String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                                    String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                                    if ("code".equals(key)) code = value;
                                    if ("state".equals(key)) state = value;
                                }
                            }

                            if (code != null && state != null) {
                                handleCallback(code, state);
                            }
                        }
                    }
                }

                // Send response
                String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n" +
                    "<html><body><h1>Authentication successful!</h1>" +
                    "<p>You can close this window now.</p></body></html>";
                client.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
                client.getOutputStream().flush();
                client.close();
                serverSocket.close();
            } catch (Exception e) {
                // Timeout or error - auth flow may have been cancelled
                if (!authFuture.isDone()) {
                    currentState = OAuthState.failed(e.getMessage());
                    authFuture.completeExceptionally(new OAuthException(e.getMessage()));
                }
            }
        });
    }

    /**
     * Parse port from redirect URI.
     */
    private int parsePortFromRedirectUri() {
        try {
            URI uri = new URI(config.redirectUri());
            if (uri.getPort() > 0) {
                return uri.getPort();
            }
        } catch (Exception e) {
            // Use default port
        }
        return 8080;
    }

    /**
     * Get current state.
     */
    public OAuthState getCurrentState() {
        return currentState;
    }

    /**
     * Cancel auth flow.
     */
    public void cancel() {
        currentState = OAuthState.failed("Cancelled");
        authFuture.cancel(true);
    }

    /**
     * OAuth exception.
     */
    public static class OAuthException extends Exception {
        public OAuthException(String message) {
            super(message);
        }
    }

    /**
     * Token info record.
     */
    public record TokenInfo(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        List<String> scope
    ) {
        public boolean isExpired() {
            return expiresIn <= 0;
        }

        public long expiresAt() {
            return System.currentTimeMillis() + expiresIn * 1000;
        }
    }
}