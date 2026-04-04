/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/logging.ts
 */
package com.anthropic.claudecode.services.api;

import java.util.*;
import com.anthropic.claudecode.services.analytics.*;

/**
 * API logging service for query, error, and success events.
 */
public final class ApiLoggingService {
    private ApiLoggingService() {}

    /**
     * Global cache strategy.
     */
    public enum GlobalCacheStrategy {
        TOOL_BASED,
        SYSTEM_PROMPT,
        NONE
    }

    /**
     * Known gateway types.
     */
    public enum KnownGateway {
        LITELM,
        HELICONE,
        PORTKEY,
        CLOUDFLARE_AI_GATEWAY,
        KONG,
        BRAINTRUST,
        DATABRICKS
    }

    /**
     * API query parameters.
     */
    public record ApiQueryParams(
        String model,
        int messagesLength,
        double temperature,
        List<String> betas,
        String permissionMode,
        String querySource,
        QueryChainTracking queryTracking,
        String thinkingType,
        String effortValue,
        Boolean fastMode,
        String previousRequestId
    ) {}

    /**
     * API error parameters.
     */
    public record ApiErrorParams(
        Throwable error,
        String model,
        int messageCount,
        Integer messageTokens,
        long durationMs,
        long durationMsIncludingRetries,
        int attempt,
        String requestId,
        String clientRequestId,
        Boolean didFallBackToNonStreaming,
        String promptCategory,
        Map<String, String> headers,
        QueryChainTracking queryTracking,
        String querySource,
        Boolean fastMode,
        String previousRequestId
    ) {}

    /**
     * API success parameters.
     */
    public record ApiSuccessParams(
        String model,
        String preNormalizedModel,
        int messageCount,
        int messageTokens,
        Usage usage,
        long durationMs,
        long durationMsIncludingRetries,
        int attempt,
        Long ttftMs,
        String requestId,
        String stopReason,
        double costUSD,
        boolean didFallBackToNonStreaming,
        String querySource,
        KnownGateway gateway,
        QueryChainTracking queryTracking,
        String permissionMode,
        GlobalCacheStrategy globalCacheStrategy,
        Integer textContentLength,
        Integer thinkingContentLength,
        Map<String, Integer> toolUseContentLengths,
        Integer connectorTextBlockCount,
        Boolean fastMode,
        String previousRequestId,
        List<String> betas
    ) {}

    /**
     * Query chain tracking.
     */
    public record QueryChainTracking(
        String chainId,
        int depth
    ) {}

    /**
     * Usage data.
     */
    public record Usage(
        int inputTokens,
        int outputTokens,
        Integer cacheReadInputTokens,
        Integer cacheCreationInputTokens
    ) {}

    /**
     * Log API query event.
     */
    public static void logApiQuery(ApiQueryParams params) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("model", params.model());
        metadata.put("messagesLength", params.messagesLength());
        metadata.put("temperature", params.temperature());
        metadata.put("provider", getApiProviderForStatsig());
        metadata.put("buildAgeMins", getBuildAgeMinutes());

        if (params.betas() != null && !params.betas().isEmpty()) {
            metadata.put("betas", String.join(",", params.betas()));
        }
        if (params.permissionMode() != null) {
            metadata.put("permissionMode", params.permissionMode());
        }
        if (params.querySource() != null) {
            metadata.put("querySource", params.querySource());
        }
        if (params.queryTracking() != null) {
            metadata.put("queryChainId", params.queryTracking().chainId());
            metadata.put("queryDepth", params.queryTracking().depth());
        }
        if (params.thinkingType() != null) {
            metadata.put("thinkingType", params.thinkingType());
        }
        if (params.effortValue() != null) {
            metadata.put("effortValue", params.effortValue());
        }
        if (params.fastMode() != null) {
            metadata.put("fastMode", params.fastMode());
        }
        if (params.previousRequestId() != null) {
            metadata.put("previousRequestId", params.previousRequestId());
        }

        // Add environment metadata
        addAnthropicEnvMetadata(metadata);

        DatadogAnalytics.logEvent("tengu_api_query", metadata);
    }

    /**
     * Log API error event.
     */
    public static void logApiError(ApiErrorParams params) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("model", params.model());
        metadata.put("error", getErrorMessage(params.error()));
        metadata.put("status", getStatusCode(params.error()));
        metadata.put("errorType", classifyApiError(params.error()));
        metadata.put("messageCount", params.messageCount());
        metadata.put("messageTokens", params.messageTokens());
        metadata.put("durationMs", params.durationMs());
        metadata.put("durationMsIncludingRetries", params.durationMsIncludingRetries());
        metadata.put("attempt", params.attempt());
        metadata.put("provider", getApiProviderForStatsig());
        metadata.put("requestId", params.requestId());
        metadata.put("clientRequestId", params.clientRequestId());
        metadata.put("didFallBackToNonStreaming", params.didFallBackToNonStreaming());

        if (params.promptCategory() != null) {
            metadata.put("promptCategory", params.promptCategory());
        }

        KnownGateway gateway = detectGateway(params.headers());
        if (gateway != null) {
            metadata.put("gateway", gateway);
        }

        if (params.queryTracking() != null) {
            metadata.put("queryChainId", params.queryTracking().chainId());
            metadata.put("queryDepth", params.queryTracking().depth());
        }

        if (params.querySource() != null) {
            metadata.put("querySource", params.querySource());
        }

        if (params.fastMode() != null) {
            metadata.put("fastMode", params.fastMode());
        }

        if (params.previousRequestId() != null) {
            metadata.put("previousRequestId", params.previousRequestId());
        }

        addAnthropicEnvMetadata(metadata);

        DatadogAnalytics.logEvent("tengu_api_error", metadata);
    }

    /**
     * Log API success event.
     */
    public static void logApiSuccess(ApiSuccessParams params) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("model", params.model());

        if (!params.model().equals(params.preNormalizedModel())) {
            metadata.put("preNormalizedModel", params.preNormalizedModel());
        }

        if (params.betas() != null && !params.betas().isEmpty()) {
            metadata.put("betas", String.join(",", params.betas()));
        }

        metadata.put("messageCount", params.messageCount());
        metadata.put("messageTokens", params.messageTokens());
        metadata.put("inputTokens", params.usage().inputTokens());
        metadata.put("outputTokens", params.usage().outputTokens());
        metadata.put("cachedInputTokens", params.usage().cacheReadInputTokens() != null ?
            params.usage().cacheReadInputTokens() : 0);
        metadata.put("uncachedInputTokens", params.usage().cacheCreationInputTokens() != null ?
            params.usage().cacheCreationInputTokens() : 0);
        metadata.put("durationMs", params.durationMs());
        metadata.put("durationMsIncludingRetries", params.durationMsIncludingRetries());
        metadata.put("attempt", params.attempt());
        metadata.put("ttftMs", params.ttftMs());
        metadata.put("buildAgeMins", getBuildAgeMinutes());
        metadata.put("provider", getApiProviderForStatsig());
        metadata.put("requestId", params.requestId());
        metadata.put("stop_reason", params.stopReason());
        metadata.put("costUSD", params.costUSD());
        metadata.put("didFallBackToNonStreaming", params.didFallBackToNonStreaming());
        metadata.put("querySource", params.querySource());

        if (params.gateway() != null) {
            metadata.put("gateway", params.gateway());
        }

        if (params.queryTracking() != null) {
            metadata.put("queryChainId", params.queryTracking().chainId());
            metadata.put("queryDepth", params.queryTracking().depth());
        }

        if (params.permissionMode() != null) {
            metadata.put("permissionMode", params.permissionMode());
        }

        if (params.globalCacheStrategy() != null) {
            metadata.put("globalCacheStrategy", params.globalCacheStrategy());
        }

        if (params.textContentLength() != null) {
            metadata.put("textContentLength", params.textContentLength());
        }

        if (params.thinkingContentLength() != null) {
            metadata.put("thinkingContentLength", params.thinkingContentLength());
        }

        if (params.toolUseContentLengths() != null) {
            metadata.put("toolUseContentLengths", params.toolUseContentLengths().toString());
        }

        if (params.fastMode() != null) {
            metadata.put("fastMode", params.fastMode());
        }

        if (params.previousRequestId() != null) {
            metadata.put("previousRequestId", params.previousRequestId());
        }

        addAnthropicEnvMetadata(metadata);

        DatadogAnalytics.logEvent("tengu_api_success", metadata);
    }

    // Private helpers

    private static String getErrorMessage(Throwable error) {
        return error != null ? error.getMessage() : "Unknown error";
    }

    private static String getStatusCode(Throwable error) {
        if (error instanceof ApiException) {
            return String.valueOf(((ApiException) error).getStatusCode());
        }
        return null;
    }

    private static String classifyApiError(Throwable error) {
        if (error instanceof ApiException) {
            ApiException apiError = (ApiException) error;
            int status = apiError.getStatusCode();

            if (status == 401) return "authentication_error";
            if (status == 403) return "permission_error";
            if (status == 404) return "not_found_error";
            if (status == 429) return "rate_limit_error";
            if (status == 500) return "server_error";
            if (status == 529) return "overloaded_error";
            if (status >= 500) return "server_error";
            if (status >= 400) return "client_error";
        }
        if (error instanceof java.net.ConnectException) return "connection_error";
        if (error instanceof java.net.SocketTimeoutException) return "timeout_error";
        return "unknown_error";
    }

    private static KnownGateway detectGateway(Map<String, String> headers) {
        if (headers == null) return null;

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.startsWith("x-litellm-")) return KnownGateway.LITELM;
            if (key.startsWith("helicone-")) return KnownGateway.HELICONE;
            if (key.startsWith("x-portkey-")) return KnownGateway.PORTKEY;
            if (key.startsWith("cf-aig-")) return KnownGateway.CLOUDFLARE_AI_GATEWAY;
            if (key.startsWith("x-kong-")) return KnownGateway.KONG;
            if (key.startsWith("x-bt-")) return KnownGateway.BRAINTRUST;
        }

        // Check URL for Databricks
        String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
        if (baseUrl != null) {
            try {
                String host = new java.net.URL(baseUrl).getHost().toLowerCase();
                if (host.contains("databricks.com")) return KnownGateway.DATABRICKS;
            } catch (Exception ignored) {}
        }

        return null;
    }

    private static String getApiProviderForStatsig() {
        if (isEnvTruthy(System.getenv("CLAUDE_CODE_USE_BEDROCK"))) return "bedrock";
        if (isEnvTruthy(System.getenv("CLAUDE_CODE_USE_VERTEX"))) return "vertex";
        if (isEnvTruthy(System.getenv("CLAUDE_CODE_USE_FOUNDRY"))) return "foundry";
        return "anthropic";
    }

    private static Integer getBuildAgeMinutes() {
        String buildTime = System.getenv("CLAUDE_CODE_BUILD_TIME");
        if (buildTime == null) return null;
        try {
            // Parse ISO timestamp and calculate age
            return null; // Simplified
        } catch (Exception e) {
            return null;
        }
    }

    private static void addAnthropicEnvMetadata(Map<String, Object> metadata) {
        String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
        if (baseUrl != null && !baseUrl.isEmpty()) {
            metadata.put("baseUrl", baseUrl);
        }

        String envModel = System.getenv("ANTHROPIC_MODEL");
        if (envModel != null && !envModel.isEmpty()) {
            metadata.put("envModel", envModel);
        }

        String envSmallFastModel = System.getenv("ANTHROPIC_SMALL_FAST_MODEL");
        if (envSmallFastModel != null && !envSmallFastModel.isEmpty()) {
            metadata.put("envSmallFastModel", envSmallFastModel);
        }
    }

    private static boolean isEnvTruthy(String value) {
        return value != null && ("1".equals(value) || "true".equalsIgnoreCase(value));
    }
}