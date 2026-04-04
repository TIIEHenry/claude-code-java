/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/apiQueryHookHelper.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * API query hook helper for creating LLM-based hooks.
 */
public final class ApiQueryHookHelper {
    private ApiQueryHookHelper() {}

    /**
     * API query hook context.
     */
    public record ApiQueryHookContext(
        List<Map<String, Object>> messages,
        Map<String, String> systemPrompt,
        Map<String, String> userContext,
        Map<String, String> systemContext,
        Map<String, Object> toolUseContext,
        String querySource,
        Integer queryMessageCount
    ) {}

    /**
     * API query hook configuration.
     */
    public record ApiQueryHookConfig<TResult>(
        String name,
        Predicate<ApiQueryHookContext> shouldRun,
        Function<ApiQueryHookContext, List<Map<String, Object>>> buildMessages,
        String systemPromptOverride,
        boolean useTools,
        BiFunction<String, ApiQueryHookContext, TResult> parseResponse,
        BiConsumer<ApiQueryResult, ApiQueryHookContext> logResult,
        Function<ApiQueryHookContext, String> getModel
    ) {}

    /**
     * API query result sealed interface.
     */
    public sealed interface ApiQueryResult permits Success, Error {}

    public record Success<TResult>(
        String type,
        String queryName,
        TResult result,
        String messageId,
        String model,
        String uuid
    ) implements ApiQueryResult {
        public Success(String queryName, TResult result, String messageId, String model, String uuid) {
            this("success", queryName, result, messageId, model, uuid);
        }
    }

    public record Error<TResult>(
        String type,
        String queryName,
        Exception error,
        String uuid
    ) implements ApiQueryResult {
        public Error(String queryName, Exception error, String uuid) {
            this("error", queryName, error, uuid);
        }
    }

    /**
     * Create an API query hook from configuration.
     */
    public static <TResult> PostSamplingHooks.PostSamplingHook createApiQueryHook(
            ApiQueryHookConfig<TResult> config) {

        return context -> CompletableFuture.runAsync(() -> {
            try {
                ApiQueryHookContext apiContext = new ApiQueryHookContext(
                    context.messages(),
                    context.systemPrompt(),
                    context.userContext(),
                    context.systemContext(),
                    context.toolUseContext(),
                    context.querySource(),
                    null
                );

                boolean shouldRun = config.shouldRun().test(apiContext);
                if (!shouldRun) {
                    return;
                }

                String uuid = UUID.randomUUID().toString();

                // Build messages
                List<Map<String, Object>> messages = config.buildMessages().apply(apiContext);
                apiContext = new ApiQueryHookContext(
                    apiContext.messages(),
                    apiContext.systemPrompt(),
                    apiContext.userContext(),
                    apiContext.systemContext(),
                    apiContext.toolUseContext(),
                    apiContext.querySource(),
                    messages.size()
                );

                // Get system prompt
                Map<String, String> systemPrompt = config.systemPromptOverride() != null
                    ? Map.of("content", config.systemPromptOverride())
                    : apiContext.systemPrompt();

                // Get tools
                List<Object> tools = config.useTools()
                    ? getToolsFromContext(apiContext.toolUseContext())
                    : new ArrayList<>();

                // Get model
                String model = config.getModel().apply(apiContext);

                // Make API call (placeholder)
                String responseContent = queryModel(messages, systemPrompt, model, tools);
                String messageId = UUID.randomUUID().toString();

                try {
                    TResult result = config.parseResponse().apply(responseContent.trim(), apiContext);
                    config.logResult().accept(
                        new Success<>(config.name(), result, messageId, model, uuid),
                        apiContext
                    );
                } catch (Exception e) {
                    config.logResult().accept(
                        new Error<>(config.name(), e, uuid),
                        apiContext
                    );
                }
            } catch (Exception e) {
                logError(e);
            }
        });
    }

    /**
     * Query the model with messages and system prompt.
     */
    private static String queryModel(
            List<Map<String, Object>> messages,
            Map<String, String> systemPrompt,
            String model,
            List<Object> tools) {
        try {
            // Get API key
            String apiKey = System.getenv("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                return "";
            }

            String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
            if (baseUrl == null) baseUrl = "https://api.anthropic.com";

            // Build request JSON
            StringBuilder sb = new StringBuilder();
            sb.append("{\"model\":\"").append(model).append("\",");
            sb.append("\"max_tokens\":1024,");

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                String sysContent = systemPrompt.getOrDefault("content", "");
                sb.append("\"system\":\"").append(escapeJson(sysContent)).append("\",");
            }

            sb.append("\"messages\":[");
            for (int i = 0; i < messages.size(); i++) {
                if (i > 0) sb.append(",");
                Map<String, Object> msg = messages.get(i);
                sb.append("{\"role\":\"").append(msg.get("role")).append("\",");
                sb.append("\"content\":\"").append(escapeJson(String.valueOf(msg.get("content")))).append("\"}");
            }
            sb.append("]");
            sb.append("}");

            // Make HTTP request
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + "/v1/messages"))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(sb.toString()))
                .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Extract content from response
                return extractContent(response.body());
            }
        } catch (Exception e) {
            // Return empty on error
        }
        return "";
    }

    /**
     * Extract content from API response.
     */
    private static String extractContent(String json) {
        int contentIdx = json.indexOf("\"content\":");
        if (contentIdx < 0) return "";

        int arrStart = json.indexOf("[", contentIdx);
        if (arrStart < 0) return "";

        // Find first text block
        int textIdx = json.indexOf("\"text\":", arrStart);
        if (textIdx < 0) return "";

        int valStart = json.indexOf("\"", textIdx + 7) + 1;
        int valEnd = json.indexOf("\"", valStart);

        if (valStart > 0 && valEnd > valStart) {
            return unescapeJson(json.substring(valStart, valEnd));
        }
        return "";
    }

    /**
     * Get tools from context.
     */
    private static List<Object> getToolsFromContext(Map<String, Object> toolUseContext) {
        List<Object> tools = new ArrayList<>();
        if (toolUseContext == null) return tools;

        // Try to extract tools from options
        Object options = toolUseContext.get("options");
        if (options instanceof Map) {
            Object toolsList = ((Map<?, ?>) options).get("tools");
            if (toolsList instanceof List) {
                tools.addAll((List<?>) toolsList);
            }
        }

        return tools;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private static void logError(Exception e) {
        if (System.getenv("CLAUDE_CODE_DEBUG") != null) {
            System.err.println("[api-query-hook] Error: " + e.getMessage());
        }
    }
}