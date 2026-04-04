/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/client.ts
 */
package com.anthropic.claudecode.services.api;

import java.util.*;
import java.util.concurrent.*;

/**
 * API client factory for creating configured clients.
 */
public final class ApiClientFactory {
    private ApiClientFactory() {}

    public static final String CLIENT_REQUEST_ID_HEADER = "x-client-request-id";

    /**
     * API provider types.
     */
    public enum ApiProvider {
        FIRST_PARTY,
        BEDROCK,
        FOUNDRY,
        VERTEX
    }

    /**
     * Client configuration options.
     */
    public record ClientOptions(
        String apiKey,
        int maxRetries,
        String model,
        int timeoutMs,
        ApiProvider provider,
        Map<String, String> customHeaders,
        String sessionId,
        String containerId,
        String remoteSessionId,
        String clientApp,
        boolean additionalProtectionEnabled
    ) {
        public ClientOptions() {
            this(null, 3, null, 600000, ApiProvider.FIRST_PARTY,
                 new HashMap<>(), null, null, null, null, false);
        }
    }

    /**
     * Create an API client with the specified options.
     */
    public static CompletableFuture<ApiClient> createClient(ClientOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            // Build default headers
            Map<String, String> headers = new HashMap<>();
            headers.put("x-app", "cli");
            headers.put("User-Agent", buildUserAgent());
            headers.put("X-Claude-Code-Session-Id", options.sessionId());

            // Add custom headers
            if (options.customHeaders() != null) {
                headers.putAll(options.customHeaders());
            }

            // Add container/session headers
            if (options.containerId() != null) {
                headers.put("x-claude-remote-container-id", options.containerId());
            }
            if (options.remoteSessionId() != null) {
                headers.put("x-claude-remote-session-id", options.remoteSessionId());
            }
            if (options.clientApp() != null) {
                headers.put("x-client-app", options.clientApp());
            }

            // Additional protection header
            if (options.additionalProtectionEnabled()) {
                headers.put("x-anthropic-additional-protection", "true");
            }

            // Build client based on provider
            ApiClientConfig config = new ApiClientConfig(
                options.apiKey(),
                getBaseUrl(options.provider()),
                options.timeoutMs(),
                options.maxRetries(),
                headers
            );

            return switch (options.provider()) {
                case BEDROCK -> createBedrockClient(config, options);
                case FOUNDRY -> createFoundryClient(config, options);
                case VERTEX -> createVertexClient(config, options);
                default -> createFirstPartyClient(config, options);
            };
        });
    }

    private static ApiClient createFirstPartyClient(ApiClientConfig config, ClientOptions options) {
        // Standard Anthropic API client
        return new ApiClient(config);
    }

    private static ApiClient createBedrockClient(ApiClientConfig config, ClientOptions options) {
        // AWS Bedrock client
        // In real implementation, would configure AWS credentials
        String region = getAwsRegion(options.model());
        ApiClientConfig bedrockConfig = new ApiClientConfig(
            config.apiKey(),
            "https://bedrock-runtime." + region + ".amazonaws.com",
            config.timeoutMs(),
            config.maxRetries(),
            config.defaultHeaders()
        );
        return new ApiClient(bedrockConfig);
    }

    private static ApiClient createFoundryClient(ApiClientConfig config, ClientOptions options) {
        // Azure Foundry client
        // In real implementation, would configure Azure AD auth
        String resource = System.getenv("ANTHROPIC_FOUNDRY_RESOURCE");
        String baseUrl = System.getenv("ANTHROPIC_FOUNDRY_BASE_URL");
        if (baseUrl == null && resource != null) {
            baseUrl = "https://" + resource + ".services.ai.azure.com";
        }

        ApiClientConfig foundryConfig = new ApiClientConfig(
            config.apiKey(),
            baseUrl != null ? baseUrl : config.baseUrl(),
            config.timeoutMs(),
            config.maxRetries(),
            config.defaultHeaders()
        );
        return new ApiClient(foundryConfig);
    }

    private static ApiClient createVertexClient(ApiClientConfig config, ClientOptions options) {
        // Google Vertex AI client
        // In real implementation, would configure GCP credentials
        String region = getVertexRegion(options.model());
        String projectId = System.getenv("ANTHROPIC_VERTEX_PROJECT_ID");

        String baseUrl = String.format(
            "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/anthropic/models",
            region, projectId, region
        );

        ApiClientConfig vertexConfig = new ApiClientConfig(
            config.apiKey(),
            baseUrl,
            config.timeoutMs(),
            config.maxRetries(),
            config.defaultHeaders()
        );
        return new ApiClient(vertexConfig);
    }

    private static String getBaseUrl(ApiProvider provider) {
        return switch (provider) {
            case BEDROCK -> "https://bedrock-runtime.us-east-1.amazonaws.com";
            case FOUNDRY -> System.getenv("ANTHROPIC_FOUNDRY_BASE_URL");
            case VERTEX -> "https://us-east5-aiplatform.googleapis.com";
            default -> "https://api.anthropic.com";
        };
    }

    private static String getAwsRegion(String model) {
        // Check for model-specific region override
        if (model != null && model.contains("haiku")) {
            String haikuRegion = System.getenv("ANTHROPIC_SMALL_FAST_MODEL_AWS_REGION");
            if (haikuRegion != null) return haikuRegion;
        }
        String region = System.getenv("AWS_REGION");
        if (region != null) return region;
        region = System.getenv("AWS_DEFAULT_REGION");
        if (region != null) return region;
        return "us-east-1";
    }

    private static String getVertexRegion(String model) {
        // Check for model-specific regions
        if (model != null) {
            String lowerModel = model.toLowerCase();
            if (lowerModel.contains("haiku-4-5") || lowerModel.contains("claude_3_5_haiku")) {
                String region = System.getenv("VERTEX_REGION_CLAUDE_HAIKU_4_5");
                if (region != null) return region;
                region = System.getenv("VERTEX_REGION_CLAUDE_3_5_HAIKU");
                if (region != null) return region;
            }
            if (lowerModel.contains("sonnet-4-5") || lowerModel.contains("claude_3_7_sonnet")) {
                String region = System.getenv("VERTEX_REGION_CLAUDE_3_7_SONNET");
                if (region != null) return region;
                region = System.getenv("VERTEX_REGION_CLAUDE_3_5_SONNET");
                if (region != null) return region;
            }
        }

        // Check global region
        String region = System.getenv("CLOUD_ML_REGION");
        if (region != null) return region;

        return "us-east5";
    }

    private static String buildUserAgent() {
        String version = "1.0.0"; // Would get from version provider
        String platform = System.getProperty("os.name", "unknown");
        String arch = System.getProperty("os.arch", "unknown");
        return String.format("ClaudeCode/%s (%s/%s) Java", version, platform, arch);
    }

    /**
     * Parse custom headers from environment variable.
     */
    public static Map<String, String> parseCustomHeaders(String headersEnv) {
        Map<String, String> headers = new HashMap<>();
        if (headersEnv == null || headersEnv.isEmpty()) {
            return headers;
        }

        // Split by newlines to support multiple headers
        String[] headerStrings = headersEnv.split("\\r?\\n");

        for (String headerString : headerStrings) {
            if (headerString.trim().isEmpty()) continue;

            // Parse header in format "Name: Value"
            int colonIdx = headerString.indexOf(':');
            if (colonIdx == -1) continue;

            String name = headerString.substring(0, colonIdx).trim();
            String value = headerString.substring(colonIdx + 1).trim();

            if (!name.isEmpty()) {
                headers.put(name, value);
            }
        }

        return headers;
    }

    /**
     * Detect API provider from environment.
     */
    public static ApiProvider detectProvider() {
        if (isEnvTruthy("CLAUDE_CODE_USE_BEDROCK")) {
            return ApiProvider.BEDROCK;
        }
        if (isEnvTruthy("CLAUDE_CODE_USE_FOUNDRY")) {
            return ApiProvider.FOUNDRY;
        }
        if (isEnvTruthy("CLAUDE_CODE_USE_VERTEX")) {
            return ApiProvider.VERTEX;
        }
        return ApiProvider.FIRST_PARTY;
    }

    private static boolean isEnvTruthy(String envVar) {
        String value = System.getenv(envVar);
        if (value == null) return false;
        return "true".equalsIgnoreCase(value) ||
               "1".equals(value) ||
               "yes".equalsIgnoreCase(value);
    }
}