/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/bootstrap.ts
 */
package com.anthropic.claudecode.services.api;

import java.util.*;
import java.util.concurrent.*;

/**
 * Bootstrap data fetcher for API configuration.
 */
public final class ApiBootstrap {
    private ApiBootstrap() {}

    /**
     * Bootstrap response from API.
     */
    public record BootstrapResponse(
        Map<String, Object> clientData,
        List<ModelOption> additionalModelOptions
    ) {}

    /**
     * Additional model option.
     */
    public record ModelOption(
        String value,      // model ID
        String label,      // display name
        String description
    ) {}

    /**
     * Client data cache.
     */
    private static volatile Map<String, Object> clientDataCache = null;
    private static volatile List<ModelOption> additionalModelOptionsCache = new ArrayList<>();

    /**
     * Fetch bootstrap data from API.
     */
    public static CompletableFuture<BootstrapResponse> fetchBootstrapAPI(ApiClient client) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get API key from environment
                String apiKey = System.getenv("ANTHROPIC_API_KEY");
                if (apiKey == null || apiKey.isEmpty()) {
                    return null;
                }

                String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
                if (baseUrl == null) baseUrl = "https://api.anthropic.com";

                // Make HTTP GET request to bootstrap endpoint
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/v1/api/bootstrap"))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseBootstrapResponse(response.body());
                } else if (response.statusCode() == 404) {
                    // Endpoint not available, return defaults
                    return new BootstrapResponse(
                        Map.of("defaultModel", "claude-sonnet-4-6"),
                        List.of(
                            new ModelOption("claude-sonnet-4-6", "Claude Sonnet 4.6", "Best balance of speed and intelligence"),
                            new ModelOption("claude-opus-4-6", "Claude Opus 4.6", "Most capable model for complex tasks"),
                            new ModelOption("claude-haiku-4-5-20251001", "Claude Haiku 4.5", "Fast and efficient for simple tasks")
                        )
                    );
                }
            } catch (Exception e) {
                // Log error and return defaults
            }

            // Return default bootstrap data
            return new BootstrapResponse(
                Map.of("defaultModel", "claude-sonnet-4-6"),
                List.of(
                    new ModelOption("claude-sonnet-4-6", "Claude Sonnet 4.6", "Best balance of speed and intelligence"),
                    new ModelOption("claude-opus-4-6", "Claude Opus 4.6", "Most capable model for complex tasks"),
                    new ModelOption("claude-haiku-4-5-20251001", "Claude Haiku 4.5", "Fast and efficient for simple tasks")
                )
            );
        });
    }

    private static BootstrapResponse parseBootstrapResponse(String json) {
        Map<String, Object> clientData = new HashMap<>();
        List<ModelOption> modelOptions = new ArrayList<>();

        try {
            // Parse client_data
            int clientDataIdx = json.indexOf("\"client_data\"");
            if (clientDataIdx >= 0) {
                int objStart = json.indexOf("{", clientDataIdx);
                int objEnd = findJsonObjEnd(json, objStart);
                if (objStart >= 0 && objEnd > objStart) {
                    // Parse key-value pairs
                    String obj = json.substring(objStart, objEnd);
                    clientData.put("raw", obj);
                }
            }

            // Parse additional_model_options
            int modelsIdx = json.indexOf("\"additional_model_options\"");
            if (modelsIdx >= 0) {
                int arrStart = json.indexOf("[", modelsIdx);
                int arrEnd = findJsonArrEnd(json, arrStart);
                if (arrStart >= 0 && arrEnd > arrStart) {
                    String arr = json.substring(arrStart + 1, arrEnd);

                    int i = 0;
                    while (i < arr.length()) {
                        int objStart = arr.indexOf("{", i);
                        if (objStart < 0) break;

                        int depth = 1;
                        int objEnd = objStart + 1;
                        while (objEnd < arr.length() && depth > 0) {
                            char c = arr.charAt(objEnd);
                            if (c == '{') depth++;
                            else if (c == '}') depth--;
                            objEnd++;
                        }

                        String obj = arr.substring(objStart, objEnd);
                        String value = extractJsonValueString(obj, "value");
                        String label = extractJsonValueString(obj, "label");
                        String description = extractJsonValueString(obj, "description");

                        if (value != null) {
                            modelOptions.add(new ModelOption(value, label, description));
                        }

                        i = objEnd;
                    }
                }
            }
        } catch (Exception e) {
            // Return defaults on parse error
        }

        if (modelOptions.isEmpty()) {
            modelOptions = List.of(
                new ModelOption("claude-sonnet-4-6", "Claude Sonnet 4.6", "Best balance of speed and intelligence"),
                new ModelOption("claude-opus-4-6", "Claude Opus 4.6", "Most capable model for complex tasks"),
                new ModelOption("claude-haiku-4-5-20251001", "Claude Haiku 4.5", "Fast and efficient for simple tasks")
            );
        }

        return new BootstrapResponse(clientData, modelOptions);
    }

    private static int findJsonObjEnd(String json, int start) {
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

    private static int findJsonArrEnd(String json, int start) {
        int depth = 1;
        int pos = start + 1;
        while (pos < json.length() && depth > 0) {
            char c = json.charAt(pos);
            if (c == '[') depth++;
            else if (c == ']') depth--;
            pos++;
        }
        return pos;
    }

    private static String extractJsonValueString(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int valStart = json.indexOf("\"", idx + key.length() + 3) + 1;
        if (valStart < 1) return null;
        int valEnd = json.indexOf("\"", valStart);
        return json.substring(valStart, valEnd);
    }

    /**
     * Fetch bootstrap data and persist to cache.
     */
    public static CompletableFuture<Void> fetchBootstrapData(ApiClient client) {
        return fetchBootstrapAPI(client).thenAccept(response -> {
            if (response == null) return;

            // Check if data changed
            boolean changed = !Objects.equals(clientDataCache, response.clientData()) ||
                              !Objects.equals(additionalModelOptionsCache, response.additionalModelOptions());

            if (changed) {
                clientDataCache = response.clientData();
                additionalModelOptionsCache = new ArrayList<>(response.additionalModelOptions());
                // In real implementation, would persist to config file
            }
        });
    }

    /**
     * Get cached client data.
     */
    public static Map<String, Object> getClientDataCache() {
        return clientDataCache;
    }

    /**
     * Get cached additional model options.
     */
    public static List<ModelOption> getAdditionalModelOptionsCache() {
        return new ArrayList<>(additionalModelOptionsCache);
    }

    /**
     * Clear cache.
     */
    public static void clearCache() {
        clientDataCache = null;
        additionalModelOptionsCache = new ArrayList<>();
    }
}