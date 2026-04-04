/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/analytics/metadata.ts
 */
package com.anthropic.claudecode.services.analytics;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Analytics metadata types and utilities.
 */
public final class AnalyticsMetadata {
    private AnalyticsMetadata() {}

    // Limits for truncation
    private static final int TOOL_INPUT_STRING_TRUNCATE_AT = 512;
    private static final int TOOL_INPUT_STRING_TRUNCATE_TO = 128;
    private static final int TOOL_INPUT_MAX_JSON_CHARS = 4 * 1024;
    private static final int TOOL_INPUT_MAX_COLLECTION_ITEMS = 20;
    private static final int TOOL_INPUT_MAX_DEPTH = 2;
    private static final int MAX_FILE_EXTENSION_LENGTH = 10;

    /**
     * Sanitizes tool names for analytics logging to avoid PII exposure.
     */
    public static String sanitizeToolNameForAnalytics(String toolName) {
        if (toolName.startsWith("mcp__")) {
            return "mcp_tool";
        }
        return toolName;
    }

    /**
     * Check if detailed tool name logging is enabled for OTLP events.
     */
    public static boolean isToolDetailsLoggingEnabled() {
        return isEnvTruthy(System.getenv("OTEL_LOG_TOOL_DETAILS"));
    }

    /**
     * Extract MCP server and tool names from a full MCP tool name.
     */
    public static McpToolDetails extractMcpToolDetails(String toolName) {
        if (!toolName.startsWith("mcp__")) {
            return null;
        }

        String[] parts = toolName.split("__");
        if (parts.length < 3) {
            return null;
        }

        String serverName = parts[1];
        String mcpToolName = String.join("__", Arrays.copyOfRange(parts, 2, parts.length));

        if (serverName.isEmpty() || mcpToolName.isEmpty()) {
            return null;
        }

        return new McpToolDetails(serverName, mcpToolName);
    }

    /**
     * Extracts and sanitizes a file extension for analytics logging.
     */
    public static String getFileExtensionForAnalytics(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        int lastDot = filePath.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filePath.length() - 1) {
            return null;
        }

        String extension = filePath.substring(lastDot + 1).toLowerCase();
        if (extension.length() > MAX_FILE_EXTENSION_LENGTH) {
            return "other";
        }

        return extension;
    }

    /**
     * Truncate tool input value for telemetry.
     */
    public static Object truncateToolInputValue(Object value, int depth) {
        if (value instanceof String) {
            String str = (String) value;
            if (str.length() > TOOL_INPUT_STRING_TRUNCATE_AT) {
                return str.substring(0, TOOL_INPUT_STRING_TRUNCATE_TO) + "…[" + str.length() + " chars]";
            }
            return str;
        }

        if (value instanceof Number || value instanceof Boolean || value == null) {
            return value;
        }

        if (depth >= TOOL_INPUT_MAX_DEPTH) {
            return "<nested>";
        }

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<Object> result = new ArrayList<>();
            int count = Math.min(list.size(), TOOL_INPUT_MAX_COLLECTION_ITEMS);
            for (int i = 0; i < count; i++) {
                result.add(truncateToolInputValue(list.get(i), depth + 1));
            }
            if (list.size() > TOOL_INPUT_MAX_COLLECTION_ITEMS) {
                result.add("…[" + list.size() + " items]");
            }
            return result;
        }

        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            Map<String, Object> result = new LinkedHashMap<>();
            int count = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (count >= TOOL_INPUT_MAX_COLLECTION_ITEMS) break;
                String key = String.valueOf(entry.getKey());
                if (!key.startsWith("_")) {
                    result.put(key, truncateToolInputValue(entry.getValue(), depth + 1));
                    count++;
                }
            }
            if (map.size() > TOOL_INPUT_MAX_COLLECTION_ITEMS) {
                result.put("…", map.size() + " keys");
            }
            return result;
        }

        return String.valueOf(value);
    }

    /**
     * Extract tool input for telemetry.
     */
    public static String extractToolInputForTelemetry(Object input) {
        if (!isToolDetailsLoggingEnabled()) {
            return null;
        }
        Object truncated = truncateToolInputValue(input, 0);
        String json = String.valueOf(truncated);
        if (json.length() > TOOL_INPUT_MAX_JSON_CHARS) {
            json = json.substring(0, TOOL_INPUT_MAX_JSON_CHARS) + "…[truncated]";
        }
        return json;
    }

    // Helper
    private static boolean isEnvTruthy(String value) {
        return value != null && ("1".equals(value) || "true".equalsIgnoreCase(value));
    }

    /**
     * Log an analytics event.
     */
    public static void logEvent(String eventName, Map<String, String> data) {
        // Simple implementation - could be extended to send to analytics service
        System.out.println("[Analytics] " + eventName + ": " + data);
    }

    /**
     * Log an analytics event with object values.
     */
    public static void logEvent(String eventName, Map<String, ?> data, boolean withObjects) {
        System.out.println("[Analytics] " + eventName + ": " + data);
    }

    /**
     * MCP tool details record.
     */
    public record McpToolDetails(String serverName, String toolName) {}

    /**
     * Environment context metadata.
     */
    public record EnvContext(
        String platform,
        String platformRaw,
        String arch,
        String nodeVersion,
        String terminal,
        String packageManagers,
        String runtimes,
        boolean isRunningWithBun,
        boolean isCi,
        boolean isClaubbit,
        boolean isClaudeCodeRemote,
        boolean isLocalAgentMode,
        boolean isConductor,
        String remoteEnvironmentType,
        String coworkerType,
        String claudeCodeContainerId,
        String claudeCodeRemoteSessionId,
        String tags,
        boolean isGithubAction,
        boolean isClaudeCodeAction,
        boolean isClaudeAiAuth,
        String version,
        String versionBase,
        String buildTime,
        String deploymentEnvironment
    ) {}

    /**
     * Process metrics.
     */
    public record ProcessMetrics(
        long uptime,
        long rss,
        long heapTotal,
        long heapUsed,
        long external,
        long arrayBuffers,
        long cpuUser,
        long cpuSystem,
        double cpuPercent
    ) {}

    /**
     * Core event metadata.
     */
    public record EventMetadata(
        String model,
        String sessionId,
        String userType,
        String betas,
        EnvContext envContext,
        String entrypoint,
        String agentSdkVersion,
        boolean isInteractive,
        String clientType,
        ProcessMetrics processMetrics,
        String sweBenchRunId,
        String sweBenchInstanceId,
        String sweBenchTaskId,
        String agentId,
        String parentSessionId,
        String agentType,
        String teamName,
        String subscriptionType,
        String repoRemoteHash
    ) {}
}