/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/oauth
 */
package com.anthropic.claudecode.services.oauth;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * OAuth types - Type definitions for OAuth flow.
 */
public final class OAuthTypes {

    /**
     * OAuth tokens response.
     */
    public record OAuthTokens(
        String accessToken,
        String refreshToken,
        long expiresAt,
        List<String> scopes,
        SubscriptionType subscriptionType,
        RateLimitTier rateLimitTier,
        OAuthProfileResponse profile,
        TokenAccount tokenAccount
    ) {}

    /**
     * Token exchange response from OAuth server.
     */
    public record OAuthTokenExchangeResponse(
        String access_token,
        String refresh_token,
        int expires_in,
        String scope,
        TokenAccountInfo account,
        OrganizationInfo organization
    ) {}

    /**
     * OAuth profile response.
     */
    public record OAuthProfileResponse(
        AccountInfo account,
        OrganizationInfo organization
    ) {}

    /**
     * Token account info.
     */
    public record TokenAccount(
        String uuid,
        String emailAddress,
        String organizationUuid
    ) {}

    /**
     * Account info from profile.
     */
    public record AccountInfo(
        String uuid,
        String email,
        String display_name,
        String created_at
    ) {}

    /**
     * Organization info from profile.
     */
    public record OrganizationInfo(
        String uuid,
        String organization_type,
        String rate_limit_tier,
        Boolean has_extra_usage_enabled,
        String billing_type,
        String subscription_created_at
    ) {}

    /**
     * Token account info from token exchange.
     */
    public record TokenAccountInfo(
        String uuid,
        String email_address
    ) {}

    /**
     * User roles response.
     */
    public record UserRolesResponse(
        String organization_role,
        String workspace_role,
        String organization_name
    ) {}

    /**
     * Subscription type enum.
     */
    public enum SubscriptionType {
        FREE, PRO, MAX, TEAM, ENTERPRISE
    }

    /**
     * Rate limit tier enum.
     */
    public enum RateLimitTier {
        TIER_1, TIER_2, TIER_3, TIER_4, TIER_5, UNLIMITED
    }

    /**
     * Billing type enum.
     */
    public enum BillingType {
        FREE, SUBSCRIPTION, CREDIT_CARD, ENTERPRISE
    }

    /**
     * Parse scopes from space-separated string.
     */
    public static List<String> parseScopes(String scopeString) {
        if (scopeString == null || scopeString.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(scopeString.split(" "));
    }

    /**
     * Check if OAuth token is expired (with 5 minute buffer).
     */
    public static boolean isOAuthTokenExpired(Long expiresAt) {
        if (expiresAt == null) {
            return false;
        }

        long bufferTime = 5 * 60 * 1000; // 5 minutes
        long now = System.currentTimeMillis();
        long expiresWithBuffer = now + bufferTime;
        return expiresWithBuffer >= expiresAt;
    }

    /**
     * Build authorization URL for OAuth flow.
     */
    public static String buildAuthUrl(OAuthConfig config, AuthUrlParams params) {
        String authUrlBase = params.loginWithClaudeAi()
            ? config.getClaudeAiAuthorizeUrl()
            : config.getConsoleAuthorizeUrl();

        StringBuilder url = new StringBuilder(authUrlBase);
        url.append("?code=true");
        url.append("&client_id=").append(URLEncoder.encode(config.getClientId(), StandardCharsets.UTF_8));
        url.append("&response_type=code");

        String redirectUri = params.isManual()
            ? config.getManualRedirectUrl()
            : "http://localhost:" + params.port() + "/callback";
        url.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));

        // Scopes
        List<String> scopes = params.inferenceOnly()
            ? Collections.singletonList(config.getClaudeAiInferenceScope())
            : config.getAllOauthScopes();
        url.append("&scope=").append(URLEncoder.encode(String.join(" ", scopes), StandardCharsets.UTF_8));

        // PKCE
        url.append("&code_challenge=").append(URLEncoder.encode(params.codeChallenge(), StandardCharsets.UTF_8));
        url.append("&code_challenge_method=S256");
        url.append("&state=").append(URLEncoder.encode(params.state(), StandardCharsets.UTF_8));

        // Optional parameters
        if (params.orgUUID() != null) {
            url.append("&orgUUID=").append(URLEncoder.encode(params.orgUUID(), StandardCharsets.UTF_8));
        }
        if (params.loginHint() != null) {
            url.append("&login_hint=").append(URLEncoder.encode(params.loginHint(), StandardCharsets.UTF_8));
        }
        if (params.loginMethod() != null) {
            url.append("&login_method=").append(URLEncoder.encode(params.loginMethod(), StandardCharsets.UTF_8));
        }

        return url.toString();
    }

    /**
     * Auth URL parameters record.
     */
    public record AuthUrlParams(
        String codeChallenge,
        String state,
        int port,
        boolean isManual,
        boolean loginWithClaudeAi,
        boolean inferenceOnly,
        String orgUUID,
        String loginHint,
        String loginMethod
    ) {}
}