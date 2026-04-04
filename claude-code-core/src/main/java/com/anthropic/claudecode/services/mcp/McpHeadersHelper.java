/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/headersHelper
 */
package com.anthropic.claudecode.services.mcp;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;

/**
 * MCP headers helper - Get dynamic headers for MCP servers.
 *
 * Executes a headersHelper script to obtain dynamic headers for MCP authentication.
 */
public final class McpHeadersHelper {
    private final int timeoutMs = 10000;

    /**
     * Get headers from helper script.
     */
    public CompletableFuture<Map<String, String>> getMcpHeadersFromHelper(
        String serverName,
        McpServerConfig config
    ) {
        if (config.headersHelper() == null || config.headersHelper().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // Security check for project/local settings
        if (config.scope() != null &&
            ("project".equals(config.scope()) || "local".equals(config.scope())) &&
            !isNonInteractiveSession()) {

            if (!checkHasTrustDialogAccepted()) {
                logError("MCP headersHelper invoked before trust check");
                return CompletableFuture.completedFuture(null);
            }
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(config.headersHelper());
                pb.redirectErrorStream(true);

                // Pass server context
                Map<String, String> env = pb.environment();
                env.put("CLAUDE_CODE_MCP_SERVER_NAME", serverName);
                env.put("CLAUDE_CODE_MCP_SERVER_URL", config.url());

                Process process = pb.start();

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line);
                    }
                }

                boolean completed = process.waitFor(timeoutMs / 1000, TimeUnit.SECONDS);
                if (!completed) {
                    process.destroyForcibly();
                    throw new RuntimeException("headersHelper timed out");
                }

                if (process.exitValue() != 0) {
                    throw new RuntimeException(
                        "headersHelper returned non-zero exit code: " + process.exitValue());
                }

                String result = output.toString().trim();
                return parseHeaders(result, serverName);
            } catch (Exception e) {
                logError("Error getting headers from headersHelper: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Get combined headers (static + dynamic).
     */
    public CompletableFuture<Map<String, String>> getMcpServerHeaders(
        String serverName,
        McpServerConfig config
    ) {
        Map<String, String> staticHeaders = config.headers() != null
            ? config.headers()
            : Collections.emptyMap();

        return getMcpHeadersFromHelper(serverName, config)
            .thenApply(dynamicHeaders -> {
                if (dynamicHeaders == null) {
                    return staticHeaders;
                }

                // Dynamic headers override static headers
                Map<String, String> combined = new HashMap<>(staticHeaders);
                combined.putAll(dynamicHeaders);
                return combined;
            });
    }

    /**
     * Parse headers JSON response.
     */
    private Map<String, String> parseHeaders(String json, String serverName) {
        if (json == null || json.isEmpty()) {
            throw new RuntimeException(
                "headersHelper for '" + serverName + "' did not return a valid value");
        }

        // Simplified JSON parsing
        Map<String, String> headers = new HashMap<>();

        try {
            // Parse JSON object
            if (json.startsWith("{") && json.endsWith("}")) {
                String content = json.substring(1, json.length() - 1);
                String[] pairs = content.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":");
                    if (kv.length == 2) {
                        String key = kv[0].trim().replace("\"", "");
                        String value = kv[1].trim().replace("\"", "");
                        headers.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(
                "headersHelper must return JSON object with string key-value pairs");
        }

        return headers;
    }

    // Helper methods
    private boolean isNonInteractiveSession() {
        String mode = System.getenv("CLAUDE_CODE_SESSION_MODE");
        return "non-interactive".equals(mode);
    }

    private boolean checkHasTrustDialogAccepted() {
        String trusted = System.getenv("CLAUDE_CODE_PROJECT_TRUSTED");
        return "true".equals(trusted);
    }

    private void logError(String message) {
        System.err.println("[MCP headersHelper] " + message);
    }

    /**
     * MCP server config record.
     */
    public record McpServerConfig(
        String url,
        String headersHelper,
        Map<String, String> headers,
        String scope
    ) {}
}