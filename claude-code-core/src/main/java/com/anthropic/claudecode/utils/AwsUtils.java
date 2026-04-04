/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code AWS utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * AWS credential utilities.
 */
public final class AwsUtils {
    private AwsUtils() {}

    /**
     * AWS short-term credentials.
     */
    public record AwsCredentials(
            String accessKeyId,
            String secretAccessKey,
            String sessionToken,
            String expiration
    ) {}

    /**
     * AWS STS output.
     */
    public record AwsStsOutput(AwsCredentials credentials) {}

    /**
     * Check if error is a credentials provider error.
     */
    public static boolean isAwsCredentialsProviderError(Throwable err) {
        return err != null && "CredentialsProviderError".equals(err.getClass().getSimpleName());
    }

    /**
     * Validate AWS STS output.
     */
    public static boolean isValidAwsStsOutput(Object obj) {
        if (!(obj instanceof Map)) return false;

        Map<?, ?> output = (Map<?, ?>) obj;
        Object credentials = output.get("Credentials");

        if (!(credentials instanceof Map)) return false;

        Map<?, ?> creds = (Map<?, ?>) credentials;

        String accessKeyId = creds.get("AccessKeyId") instanceof String ? (String) creds.get("AccessKeyId") : null;
        String secretAccessKey = creds.get("SecretAccessKey") instanceof String ? (String) creds.get("SecretAccessKey") : null;
        String sessionToken = creds.get("SessionToken") instanceof String ? (String) creds.get("SessionToken") : null;

        return accessKeyId != null && !accessKeyId.isEmpty() &&
               secretAccessKey != null && !secretAccessKey.isEmpty() &&
               sessionToken != null && !sessionToken.isEmpty();
    }

    /**
     * Check STS caller identity (placeholder).
     */
    public static void checkStsCallerIdentity() {
        // Would integrate with AWS SDK in real implementation
    }

    /**
     * Clear AWS credential provider cache.
     */
    public static void clearAwsIniCache() {
        // Would integrate with AWS SDK in real implementation
    }

    /**
     * Check if AWS Bedrock is configured.
     */
    public static boolean isBedrockConfigured() {
        String useBedrock = System.getenv("CLAUDE_CODE_USE_BEDROCK");
        return "true".equalsIgnoreCase(useBedrock) || "1".equals(useBedrock);
    }

    /**
     * Check if Google Vertex AI is configured.
     */
    public static boolean isVertexConfigured() {
        String useVertex = System.getenv("CLAUDE_CODE_USE_VERTEX");
        return "true".equalsIgnoreCase(useVertex) || "1".equals(useVertex);
    }

    /**
     * Check if Azure Foundry is configured.
     */
    public static boolean isFoundryConfigured() {
        String useFoundry = System.getenv("CLAUDE_CODE_USE_FOUNDRY");
        return "true".equalsIgnoreCase(useFoundry) || "1".equals(useFoundry);
    }
}