/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/status
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Status command - Show Claude Code status including version, model, account.
 */
public final class StatusCommand implements Command {
    @Override
    public String name() {
        return "status";
    }

    @Override
    public String description() {
        return "Show Claude Code status including version, model, account, API connectivity, and tool statuses";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supportsNonInteractive() {
        return true;
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Claude Code Status\n");
        sb.append("==================\n\n");

        // Version
        sb.append("Version: ").append(getVersion()).append("\n\n");

        // Model
        sb.append("Model: ").append(context.getCurrentModel()).append("\n\n");

        // Account status
        sb.append("Account: ");
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        String authToken = System.getenv("ANTHROPIC_AUTH_TOKEN");
        if (apiKey != null) {
            sb.append("API Key configured (").append(apiKey.length()).append(" chars)\n");
        } else if (authToken != null) {
            sb.append("OAuth token configured\n");
        } else {
            sb.append("Not authenticated - run /login\n");
        }

        sb.append("\n");

        // API Provider
        sb.append("API Provider: ").append(getApiProvider()).append("\n\n");

        // API Connectivity
        sb.append("API Connectivity: ");
        if (checkApiConnectivity()) {
            sb.append("OK\n");
        } else {
            sb.append("FAILED - Check network/proxy settings\n");
        }

        sb.append("\n");

        // Session info
        sb.append("Session:\n");
        sb.append("  Messages: ").append(context.getMessageCount()).append("\n");
        sb.append("  Tokens Used: ").append(context.getTotalInputTokens() + context.getTotalOutputTokens()).append("\n");

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }

    private String getVersion() {
        return "1.0.0-java"; // Would read from manifest or version file
    }

    private String getApiProvider() {
        if ("true".equalsIgnoreCase(System.getenv("CLAUDE_CODE_USE_BEDROCK"))) {
            return "AWS Bedrock";
        }
        if ("true".equalsIgnoreCase(System.getenv("CLAUDE_CODE_USE_VERTEX"))) {
            return "Google Vertex AI";
        }
        if ("true".equalsIgnoreCase(System.getenv("CLAUDE_CODE_USE_FOUNDRY"))) {
            return "Azure Foundry";
        }
        return "Anthropic (First Party)";
    }

    private boolean checkApiConnectivity() {
        // Actually ping API with a minimal request
        try {
            String apiKey = System.getenv("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                return false;
            }

            String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
            if (baseUrl == null) baseUrl = "https://api.anthropic.com";

            // Make a minimal API request to check connectivity
            String requestBody = "{\"model\":\"claude-haiku-4-5-20251001\",\"max_tokens\":1,\"messages\":[{\"role\":\"user\",\"content\":\"ping\"}]}";

            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + "/v1/messages"))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            // Any 2xx or 4xx (rate limit, etc) response means API is reachable
            return response.statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }
}