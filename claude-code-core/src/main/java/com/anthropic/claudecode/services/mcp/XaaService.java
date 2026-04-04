/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/xaa
 */
package com.anthropic.claudecode.services.mcp;

import java.util.*;
import java.util.concurrent.*;
import java.net.http.*;
import java.net.URI;
import java.net.URL;
import java.time.*;
import java.util.regex.*;

/**
 * Cross-App Access (XAA) service - Enterprise Managed Authorization.
 *
 * Obtains an MCP access token WITHOUT a browser consent screen by chaining:
 *   1. RFC 8693 Token Exchange at the IdP: id_token → ID-JAG
 *   2. RFC 7523 JWT Bearer Grant at the AS: ID-JAG → access_token
 */
public final class XaaService {
    private static final int REQUEST_TIMEOUT_MS = 30000;

    private static final String TOKEN_EXCHANGE_GRANT =
        "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final String JWT_BEARER_GRANT =
        "urn:ietf:params:oauth:grant-type:jwt-bearer";
    private static final String ID_JAG_TOKEN_TYPE =
        "urn:ietf:params:oauth:token-type:id-jag";
    private static final String ID_TOKEN_TYPE =
        "urn:ietf:params:oauth:token-type:id_token";

    // Regex to redact sensitive tokens in logs
    private static final Pattern SENSITIVE_TOKEN_RE = Pattern.compile(
        "\"(access_token|refresh_token|id_token|assertion|subject_token|client_secret)\"\\s*:\\s*\"[^\"]*\""
    );

    private final HttpClient httpClient;

    /**
     * Create XAA service.
     */
    public XaaService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(REQUEST_TIMEOUT_MS))
            .build();
    }

    /**
     * XAA token exchange error.
     */
    public static final class XaaTokenExchangeError extends RuntimeException {
        private final boolean shouldClearIdToken;

        public XaaTokenExchangeError(String message, boolean shouldClearIdToken) {
            super(message);
            this.shouldClearIdToken = shouldClearIdToken;
        }

        public boolean shouldClearIdToken() {
            return shouldClearIdToken;
        }
    }

    /**
     * Protected resource metadata.
     */
    public record ProtectedResourceMetadata(
        String resource,
        List<String> authorizationServers
    ) {}

    /**
     * Authorization server metadata.
     */
    public record AuthorizationServerMetadata(
        String issuer,
        String tokenEndpoint,
        List<String> grantTypesSupported,
        List<String> tokenEndpointAuthMethodsSupported
    ) {}

    /**
     * JWT auth grant result.
     */
    public record JwtAuthGrantResult(
        String jwtAuthGrant,
        Integer expiresIn,
        String scope
    ) {}

    /**
     * XAA token result.
     */
    public record XaaTokenResult(
        String accessToken,
        String tokenType,
        Integer expiresIn,
        String scope,
        String refreshToken
    ) {}

    /**
     * XAA full result with AS URL.
     */
    public record XaaResult(
        String accessToken,
        String tokenType,
        Integer expiresIn,
        String scope,
        String refreshToken,
        String authorizationServerUrl
    ) {}

    /**
     * XAA config.
     */
    public record XaaConfig(
        String clientId,
        String clientSecret,
        String idpClientId,
        String idpClientSecret,
        String idpIdToken,
        String idpTokenEndpoint
    ) {}

    /**
     * Discover protected resource metadata.
     */
    public CompletableFuture<ProtectedResourceMetadata> discoverProtectedResource(
        String serverUrl
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // In production, this would fetch from well-known endpoint
                // Simplified: assume basic structure
                String prmUrl = serverUrl + "/.well-known/oauth-protected-resource";

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(prmUrl))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("PRM discovery failed: HTTP " + response.statusCode());
                }

                return parseProtectedResourceMetadata(response.body(), serverUrl);
            } catch (Exception e) {
                throw new RuntimeException("XAA: PRM discovery failed: " + e.getMessage());
            }
        });
    }

    /**
     * Discover authorization server metadata.
     */
    public CompletableFuture<AuthorizationServerMetadata> discoverAuthorizationServer(
        String asUrl
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String asMetaUrl = asUrl + "/.well-known/oauth-authorization-server";

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(asMetaUrl))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("AS metadata discovery failed");
                }

                AuthorizationServerMetadata meta = parseAuthorizationServerMetadata(response.body(), asUrl);

                // Validate HTTPS
                if (!meta.tokenEndpoint().startsWith("https://")) {
                    throw new RuntimeException("XAA: refusing non-HTTPS token endpoint: " + meta.tokenEndpoint());
                }

                return meta;
            } catch (Exception e) {
                throw new RuntimeException("XAA: AS metadata discovery failed: " + e.getMessage());
            }
        });
    }

    /**
     * Request JWT authorization grant (RFC 8693 Token Exchange).
     */
    public CompletableFuture<JwtAuthGrantResult> requestJwtAuthorizationGrant(
        String tokenEndpoint,
        String audience,
        String resource,
        String idToken,
        String clientId,
        String clientSecret,
        String scope
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> params = new LinkedHashMap<>();
                params.put("grant_type", TOKEN_EXCHANGE_GRANT);
                params.put("requested_token_type", ID_JAG_TOKEN_TYPE);
                params.put("audience", audience);
                params.put("resource", resource);
                params.put("subject_token", idToken);
                params.put("subject_token_type", ID_TOKEN_TYPE);
                params.put("client_id", clientId);

                if (clientSecret != null) {
                    params.put("client_secret", clientSecret);
                }
                if (scope != null) {
                    params.put("scope", scope);
                }

                String body = buildFormBody(params);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenEndpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400 && response.statusCode() < 500) {
                    String redacted = redactTokens(response.body());
                    throw new XaaTokenExchangeError(
                        "XAA: token exchange failed: HTTP " + response.statusCode() + ": " + redacted,
                        true
                    );
                }

                if (response.statusCode() >= 500) {
                    throw new XaaTokenExchangeError(
                        "XAA: token exchange failed: HTTP " + response.statusCode(),
                        false
                    );
                }

                return parseTokenExchangeResponse(response.body());
            } catch (XaaTokenExchangeError e) {
                throw e;
            } catch (Exception e) {
                throw new XaaTokenExchangeError("XAA: token exchange error: " + e.getMessage(), false);
            }
        });
    }

    /**
     * Exchange JWT auth grant for access token (RFC 7523 JWT Bearer).
     */
    public CompletableFuture<XaaTokenResult> exchangeJwtAuthGrant(
        String tokenEndpoint,
        String assertion,
        String clientId,
        String clientSecret,
        String authMethod,
        String scope
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> params = new LinkedHashMap<>();
                params.put("grant_type", JWT_BEARER_GRANT);
                params.put("assertion", assertion);

                if (scope != null) {
                    params.put("scope", scope);
                }

                String body = buildFormBody(params);

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(tokenEndpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded");

                // Auth method: client_secret_basic or client_secret_post
                if ("client_secret_basic".equals(authMethod)) {
                    String basicAuth = Base64.getEncoder().encodeToString(
                        (clientId + ":" + clientSecret).getBytes()
                    );
                    requestBuilder.header("Authorization", "Basic " + basicAuth);
                } else {
                    params.put("client_id", clientId);
                    params.put("client_secret", clientSecret);
                    body = buildFormBody(params);
                }

                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));

                HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

                if (!(response.statusCode() >= 200 && response.statusCode() < 300)) {
                    String redacted = redactTokens(response.body());
                    throw new RuntimeException(
                        "XAA: jwt-bearer grant failed: HTTP " + response.statusCode() + ": " + redacted
                    );
                }

                return parseJwtBearerResponse(response.body());
            } catch (Exception e) {
                throw new RuntimeException("XAA: jwt-bearer exchange error: " + e.getMessage());
            }
        });
    }

    /**
     * Perform full XAA flow.
     */
    public CompletableFuture<XaaResult> performCrossAppAccess(
        String serverUrl,
        XaaConfig config,
        String serverName
    ) {
        return discoverProtectedResource(serverUrl)
            .thenCompose(prm -> {
                // Try each AS
                for (String asUrl : prm.authorizationServers()) {
                    try {
                        return discoverAuthorizationServer(asUrl)
                            .thenCompose(asMeta -> {
                                // Check grant types
                                if (asMeta.grantTypesSupported() != null &&
                                    !asMeta.grantTypesSupported().contains(JWT_BEARER_GRANT)) {
                                    throw new RuntimeException("AS does not support jwt-bearer");
                                }

                                // Determine auth method
                                final String authMethod;
                                if (asMeta.tokenEndpointAuthMethodsSupported() != null) {
                                    if (!asMeta.tokenEndpointAuthMethodsSupported().contains("client_secret_basic") &&
                                        asMeta.tokenEndpointAuthMethodsSupported().contains("client_secret_post")) {
                                        authMethod = "client_secret_post";
                                    } else {
                                        authMethod = "client_secret_basic";
                                    }
                                } else {
                                    authMethod = "client_secret_basic";
                                }

                                // Token exchange at IdP
                                return requestJwtAuthorizationGrant(
                                    config.idpTokenEndpoint(),
                                    asMeta.issuer(),
                                    prm.resource(),
                                    config.idpIdToken(),
                                    config.idpClientId(),
                                    config.idpClientSecret(),
                                    null
                                ).thenCompose(jag ->
                                    // JWT bearer at AS
                                    exchangeJwtAuthGrant(
                                        asMeta.tokenEndpoint(),
                                        jag.jwtAuthGrant(),
                                        config.clientId(),
                                        config.clientSecret(),
                                        authMethod,
                                        null
                                    ).thenApply(tokens -> new XaaResult(
                                        tokens.accessToken(),
                                        tokens.tokenType(),
                                        tokens.expiresIn(),
                                        tokens.scope(),
                                        tokens.refreshToken(),
                                        asMeta.issuer()
                                    ))
                                );
                            });
                    } catch (Exception e) {
                        continue;
                    }
                }
                throw new RuntimeException("XAA: no authorization server supports jwt-bearer");
            });
    }

    // Helper methods
    private String buildFormBody(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private String redactTokens(String raw) {
        return SENSITIVE_TOKEN_RE.matcher(raw).replaceAll("\"$1\":\"[REDACTED]\"");
    }

    private String normalizeUrl(String url) {
        try {
            return new URL(url).toString().replaceAll("/$", "");
        } catch (Exception e) {
            return url.replaceAll("/$", "");
        }
    }

    private ProtectedResourceMetadata parseProtectedResourceMetadata(String json, String serverUrl) {
        // Simplified parsing
        return new ProtectedResourceMetadata(serverUrl, List.of("https://auth.example.com"));
    }

    private AuthorizationServerMetadata parseAuthorizationServerMetadata(String json, String asUrl) {
        // Simplified parsing
        return new AuthorizationServerMetadata(
            asUrl,
            asUrl + "/token",
            List.of(JWT_BEARER_GRANT),
            List.of("client_secret_basic", "client_secret_post")
        );
    }

    private JwtAuthGrantResult parseTokenExchangeResponse(String json) {
        // Simplified parsing - would use Jackson in production
        return new JwtAuthGrantResult("mock-jag-token", 3600, null);
    }

    private XaaTokenResult parseJwtBearerResponse(String json) {
        // Simplified parsing
        return new XaaTokenResult("mock-access-token", "Bearer", 3600, null, null);
    }
}