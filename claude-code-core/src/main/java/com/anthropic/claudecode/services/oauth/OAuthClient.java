/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/oauth/OAuthClient
 */
package com.anthropic.claudecode.services.oauth;

import java.net.http.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.time.Duration;

/**
 * OAuth client - Handles OAuth token exchange and profile fetching.
 */
public final class OAuthClient {
    private final OAuthConfig config;
    private final HttpClient httpClient;

    public OAuthClient() {
        this.config = OAuthConfig.getInstance();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    }

    /**
     * Exchange authorization code for tokens.
     */
    public CompletableFuture<OAuthTypes.OAuthTokenExchangeResponse> exchangeCodeForTokens(
        String authorizationCode,
        String state,
        String codeVerifier,
        int port,
        boolean useManualRedirect,
        Integer expiresIn
    ) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("grant_type", "authorization_code");
        requestBody.put("code", authorizationCode);
        requestBody.put("redirect_uri", useManualRedirect
            ? config.getManualRedirectUrl()
            : "http://localhost:" + port + "/callback");
        requestBody.put("client_id", config.getClientId());
        requestBody.put("code_verifier", codeVerifier);
        requestBody.put("state", state);

        if (expiresIn != null) {
            requestBody.put("expires_in", expiresIn);
        }

        return sendTokenRequest(requestBody);
    }

    /**
     * Refresh OAuth token.
     */
    public CompletableFuture<OAuthTypes.OAuthTokens> refreshOAuthToken(
        String refreshToken,
        List<String> requestedScopes
    ) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("grant_type", "refresh_token");
        requestBody.put("refresh_token", refreshToken);
        requestBody.put("client_id", config.getClientId());

        List<String> scopes = requestedScopes != null && !requestedScopes.isEmpty()
            ? requestedScopes
            : config.getClaudeAiOauthScopes();
        requestBody.put("scope", String.join(" ", scopes));

        return sendTokenRequest(requestBody)
            .thenCompose(response -> fetchProfileInfo(response.access_token())
                .thenApply(profile -> formatTokens(response, profile)));
    }

    /**
     * Fetch profile info from OAuth server.
     */
    public CompletableFuture<ProfileInfo> fetchProfileInfo(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.getProfileUrl()))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    throw new OAuthException("Failed to fetch profile: " + response.statusCode());
                }
                return parseProfileResponse(response.body());
            });
    }

    /**
     * Fetch and store user roles.
     */
    public CompletableFuture<OAuthTypes.UserRolesResponse> fetchUserRoles(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.getRolesUrl()))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    throw new OAuthException("Failed to fetch roles: " + response.statusCode());
                }
                return parseRolesResponse(response.body());
            });
    }

    /**
     * Create and store API key.
     */
    public CompletableFuture<String> createApiKey(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.getApiKeyUrl()))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(""))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    throw new OAuthException("Failed to create API key: " + response.statusCode());
                }
                // Parse response and extract raw_key
                return parseApiKeyResponse(response.body());
            });
    }

    private CompletableFuture<OAuthTypes.OAuthTokenExchangeResponse> sendTokenRequest(Map<String, Object> requestBody) {
        String jsonBody = toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.getTokenUrl()))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() == 401) {
                    throw new OAuthException("Authentication failed: Invalid authorization code");
                }
                if (response.statusCode() != 200) {
                    throw new OAuthException("Token exchange failed: " + response.statusCode());
                }
                return parseTokenResponse(response.body());
            });
    }

    // JSON parsing methods (simplified - would use Jackson/Gson in production)
    private OAuthTypes.OAuthTokenExchangeResponse parseTokenResponse(String json) {
        // Parse JSON and create response object
        String accessToken = extractJsonString(json, "access_token");
        String refreshToken = extractJsonString(json, "refresh_token");
        int expiresIn = extractJsonInt(json, "expires_in");
        String scope = extractJsonString(json, "scope");

        // Parse account info
        OAuthTypes.TokenAccountInfo account = null;
        int accountIdx = json.indexOf("\"account\"");
        if (accountIdx >= 0) {
            int objStart = json.indexOf("{", accountIdx);
            int objEnd = findJsonObjEnd(json, objStart);
            if (objStart >= 0 && objEnd > objStart) {
                String accountJson = json.substring(objStart, objEnd);
                String uuid = extractJsonString(accountJson, "uuid");
                String email = extractJsonString(accountJson, "email_address");
                account = new OAuthTypes.TokenAccountInfo(uuid, email);
            }
        }

        // Parse organization info
        OAuthTypes.OrganizationInfo organization = null;
        int orgIdx = json.indexOf("\"organization\"");
        if (orgIdx >= 0) {
            int objStart = json.indexOf("{", orgIdx);
            int objEnd = findJsonObjEnd(json, objStart);
            if (objStart >= 0 && objEnd > objStart) {
                String orgJson = json.substring(objStart, objEnd);
                organization = parseOrganizationInfo(orgJson);
            }
        }

        return new OAuthTypes.OAuthTokenExchangeResponse(accessToken, refreshToken, expiresIn, scope, account, organization);
    }

    private ProfileInfo parseProfileResponse(String json) {
        // Parse profile info
        OAuthTypes.AccountInfo account = null;
        int accountIdx = json.indexOf("\"account\"");
        if (accountIdx >= 0) {
            int objStart = json.indexOf("{", accountIdx);
            int objEnd = findJsonObjEnd(json, objStart);
            if (objStart >= 0 && objEnd > objStart) {
                String accountJson = json.substring(objStart, objEnd);
                String uuid = extractJsonString(accountJson, "uuid");
                String email = extractJsonString(accountJson, "email");
                String displayName = extractJsonString(accountJson, "display_name");
                String createdAt = extractJsonString(accountJson, "created_at");
                account = new OAuthTypes.AccountInfo(uuid, email, displayName, createdAt);
            }
        }

        OAuthTypes.OrganizationInfo organization = null;
        int orgIdx = json.indexOf("\"organization\"");
        if (orgIdx >= 0) {
            int objStart = json.indexOf("{", orgIdx);
            int objEnd = findJsonObjEnd(json, objStart);
            if (objStart >= 0 && objEnd > objStart) {
                String orgJson = json.substring(objStart, objEnd);
                organization = parseOrganizationInfo(orgJson);
            }
        }

        OAuthTypes.OAuthProfileResponse profile = new OAuthTypes.OAuthProfileResponse(account, organization);

        // Determine subscription type from organization
        OAuthTypes.SubscriptionType subscriptionType = OAuthTypes.SubscriptionType.FREE;
        OAuthTypes.RateLimitTier rateLimitTier = OAuthTypes.RateLimitTier.TIER_1;
        String displayName = account != null ? account.display_name() : "";
        boolean hasExtraUsage = false;
        OAuthTypes.BillingType billingType = OAuthTypes.BillingType.FREE;

        if (organization != null) {
            String tierStr = organization.rate_limit_tier();
            if (tierStr != null) {
                rateLimitTier = parseRateLimitTier(tierStr);
            }
            if (organization.has_extra_usage_enabled() != null) {
                hasExtraUsage = organization.has_extra_usage_enabled();
            }
            String billingStr = organization.billing_type();
            if (billingStr != null) {
                billingType = parseBillingType(billingStr);
            }
            String orgType = organization.organization_type();
            if (orgType != null) {
                subscriptionType = parseSubscriptionType(orgType);
            }
        }

        return new ProfileInfo(subscriptionType, rateLimitTier, displayName, hasExtraUsage, billingType, profile);
    }

    private OAuthTypes.UserRolesResponse parseRolesResponse(String json) {
        String orgRole = extractJsonString(json, "organization_role");
        String workspaceRole = extractJsonString(json, "workspace_role");
        String orgName = extractJsonString(json, "organization_name");
        return new OAuthTypes.UserRolesResponse(orgRole, workspaceRole, orgName);
    }

    private String parseApiKeyResponse(String json) {
        // Extract raw_key from API key response
        return extractJsonString(json, "raw_key");
    }

    private OAuthTypes.OrganizationInfo parseOrganizationInfo(String json) {
        String uuid = extractJsonString(json, "uuid");
        String orgType = extractJsonString(json, "organization_type");
        String rateLimitTier = extractJsonString(json, "rate_limit_tier");
        Boolean hasExtraUsage = extractJsonBoolean(json, "has_extra_usage_enabled");
        String billingType = extractJsonString(json, "billing_type");
        String subscriptionCreatedAt = extractJsonString(json, "subscription_created_at");
        return new OAuthTypes.OrganizationInfo(uuid, orgType, rateLimitTier, hasExtraUsage, billingType, subscriptionCreatedAt);
    }

    private String extractJsonString(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int valStart = json.indexOf("\"", idx + key.length() + 3) + 1;
        if (valStart < 1) return null;
        int valEnd = json.indexOf("\"", valStart);
        return json.substring(valStart, valEnd);
    }

    private int extractJsonInt(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return 0;
        int valStart = json.indexOf(":", idx + key.length() + 2) + 1;
        while (valStart < json.length() && Character.isWhitespace(json.charAt(valStart))) valStart++;
        int valEnd = valStart;
        while (valEnd < json.length() && Character.isDigit(json.charAt(valEnd))) valEnd++;
        try {
            return Integer.parseInt(json.substring(valStart, valEnd));
        } catch (Exception e) {
            return 0;
        }
    }

    private Boolean extractJsonBoolean(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int valStart = json.indexOf(":", idx + key.length() + 2) + 1;
        while (valStart < json.length() && Character.isWhitespace(json.charAt(valStart))) valStart++;
        if (json.substring(valStart).startsWith("true")) return true;
        if (json.substring(valStart).startsWith("false")) return false;
        return null;
    }

    private int findJsonObjEnd(String json, int start) {
        int depth = 1;
        int pos = start + 1;
        while (pos < json.length() && depth > 0) {
            char c = json.charAt(pos);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            pos++;
        }
        return pos;
    }

    private OAuthTypes.RateLimitTier parseRateLimitTier(String tier) {
        if (tier == null) return OAuthTypes.RateLimitTier.TIER_1;
        switch (tier.toLowerCase()) {
            case "tier_1": return OAuthTypes.RateLimitTier.TIER_1;
            case "tier_2": return OAuthTypes.RateLimitTier.TIER_2;
            case "tier_3": return OAuthTypes.RateLimitTier.TIER_3;
            case "tier_4": return OAuthTypes.RateLimitTier.TIER_4;
            case "tier_5": return OAuthTypes.RateLimitTier.TIER_5;
            case "unlimited": return OAuthTypes.RateLimitTier.UNLIMITED;
            default: return OAuthTypes.RateLimitTier.TIER_1;
        }
    }

    private OAuthTypes.BillingType parseBillingType(String billing) {
        if (billing == null) return OAuthTypes.BillingType.FREE;
        switch (billing.toLowerCase()) {
            case "free": return OAuthTypes.BillingType.FREE;
            case "subscription": return OAuthTypes.BillingType.SUBSCRIPTION;
            case "credit_card": return OAuthTypes.BillingType.CREDIT_CARD;
            case "enterprise": return OAuthTypes.BillingType.ENTERPRISE;
            default: return OAuthTypes.BillingType.FREE;
        }
    }

    private OAuthTypes.SubscriptionType parseSubscriptionType(String type) {
        if (type == null) return OAuthTypes.SubscriptionType.FREE;
        switch (type.toLowerCase()) {
            case "free": return OAuthTypes.SubscriptionType.FREE;
            case "pro": return OAuthTypes.SubscriptionType.PRO;
            case "max": return OAuthTypes.SubscriptionType.MAX;
            case "team": return OAuthTypes.SubscriptionType.TEAM;
            case "enterprise": return OAuthTypes.SubscriptionType.ENTERPRISE;
            default: return OAuthTypes.SubscriptionType.FREE;
        }
    }

    private OAuthTypes.OAuthTokens formatTokens(OAuthTypes.OAuthTokenExchangeResponse response, ProfileInfo profile) {
        return new OAuthTypes.OAuthTokens(
            response.access_token(),
            response.refresh_token(),
            System.currentTimeMillis() + response.expires_in() * 1000,
            OAuthTypes.parseScopes(response.scope()),
            profile.subscriptionType,
            profile.rateLimitTier,
            profile.rawProfile,
            response.account() != null
                ? new OAuthTypes.TokenAccount(response.account().uuid(), response.account().email_address(), null)
                : null
        );
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Number) {
                sb.append(value);
            } else {
                sb.append("\"").append(value).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Profile info record.
     */
    public record ProfileInfo(
        OAuthTypes.SubscriptionType subscriptionType,
        OAuthTypes.RateLimitTier rateLimitTier,
        String displayName,
        boolean hasExtraUsageEnabled,
        OAuthTypes.BillingType billingType,
        OAuthTypes.OAuthProfileResponse rawProfile
    ) {}

    /**
     * OAuth exception.
     */
    public static class OAuthException extends RuntimeException {
        public OAuthException(String message) {
            super(message);
        }
    }
}