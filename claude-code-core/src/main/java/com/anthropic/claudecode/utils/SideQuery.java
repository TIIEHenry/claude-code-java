/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code side query utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Lightweight API wrapper for side queries outside the main conversation loop.
 */
public final class SideQuery {
    private SideQuery() {}

    /**
     * Side query options.
     */
    public record SideQueryOptions(
            String model,
            String system,
            List<Map<String, Object>> messages,
            List<Map<String, Object>> tools,
            Map<String, Object> toolChoice,
            String outputFormat,
            int maxTokens,
            int maxRetries,
            CompletableFuture<Void> signal,
            boolean skipSystemPromptPrefix,
            Double temperature,
            Integer thinking,
            List<String> stopSequences,
            String querySource
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String model;
            private String system;
            private List<Map<String, Object>> messages = new ArrayList<>();
            private List<Map<String, Object>> tools;
            private Map<String, Object> toolChoice;
            private String outputFormat;
            private int maxTokens = 1024;
            private int maxRetries = 2;
            private CompletableFuture<Void> signal;
            private boolean skipSystemPromptPrefix;
            private Double temperature;
            private Integer thinking;
            private List<String> stopSequences;
            private String querySource;

            public Builder model(String model) { this.model = model; return this; }
            public Builder system(String system) { this.system = system; return this; }
            public Builder messages(List<Map<String, Object>> messages) { this.messages = messages; return this; }
            public Builder addMessage(Map<String, Object> message) { this.messages.add(message); return this; }
            public Builder tools(List<Map<String, Object>> tools) { this.tools = tools; return this; }
            public Builder toolChoice(Map<String, Object> toolChoice) { this.toolChoice = toolChoice; return this; }
            public Builder outputFormat(String format) { this.outputFormat = format; return this; }
            public Builder maxTokens(int tokens) { this.maxTokens = tokens; return this; }
            public Builder maxRetries(int retries) { this.maxRetries = retries; return this; }
            public Builder signal(CompletableFuture<Void> signal) { this.signal = signal; return this; }
            public Builder skipSystemPromptPrefix(boolean skip) { this.skipSystemPromptPrefix = skip; return this; }
            public Builder temperature(Double temp) { this.temperature = temp; return this; }
            public Builder thinking(Integer budget) { this.thinking = budget; return this; }
            public Builder stopSequences(List<String> sequences) { this.stopSequences = sequences; return this; }
            public Builder querySource(String source) { this.querySource = source; return this; }

            public SideQueryOptions build() {
                return new SideQueryOptions(
                        model, system, messages, tools, toolChoice, outputFormat,
                        maxTokens, maxRetries, signal, skipSystemPromptPrefix,
                        temperature, thinking, stopSequences, querySource
                );
            }
        }
    }

    /**
     * Execute a side query.
     */
    public static CompletableFuture<SideQueryResult> execute(SideQueryOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get API key
                String apiKey = System.getenv("ANTHROPIC_API_KEY");
                if (apiKey == null || apiKey.isEmpty()) {
                    return new SideQueryResult(
                        List.of(Map.of("type", "text", "text", "Error: No API key configured")),
                        new Usage(0, 0, 0, 0),
                        null,
                        options.model
                    );
                }

                String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
                if (baseUrl == null) baseUrl = "https://api.anthropic.com";

                // Build request JSON
                StringBuilder jsonBody = new StringBuilder();
                jsonBody.append("{");
                jsonBody.append("\"model\":\"").append(options.model).append("\",");

                if (options.maxTokens > 0) {
                    jsonBody.append("\"max_tokens\":").append(options.maxTokens).append(",");
                } else {
                    jsonBody.append("\"max_tokens\":1024,");
                }

                if (options.system != null && !options.system.isEmpty()) {
                    jsonBody.append("\"system\":\"").append(escapeJson(options.system)).append("\",");
                }

                jsonBody.append("\"messages\":[");
                if (options.messages != null) {
                    boolean first = true;
                    for (Map<String, Object> msg : options.messages) {
                        if (!first) jsonBody.append(",");
                        first = false;
                        jsonBody.append("{\"role\":\"").append(msg.get("role")).append("\",");
                        jsonBody.append("\"content\":\"").append(escapeJson(String.valueOf(msg.get("content")))).append("\"}");
                    }
                }
                jsonBody.append("]");

                if (options.temperature != null) {
                    jsonBody.append(",\"temperature\":").append(options.temperature);
                }

                jsonBody.append("}");

                // Make HTTP request
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/v1/messages"))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                    .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseResponse(response.body(), options.model);
                } else {
                    return new SideQueryResult(
                        List.of(Map.of("type", "text", "text", "Error: HTTP " + response.statusCode())),
                        new Usage(0, 0, 0, 0),
                        null,
                        options.model
                    );
                }
            } catch (Exception e) {
                return new SideQueryResult(
                    List.of(Map.of("type", "text", "text", "Error: " + e.getMessage())),
                    new Usage(0, 0, 0, 0),
                    null,
                    options.model
                );
            }
        });
    }

    private static SideQueryResult parseResponse(String json, String model) {
        List<Map<String, Object>> content = new ArrayList<>();
        int inputTokens = 0;
        int outputTokens = 0;
        String requestId = null;

        try {
            // Parse usage
            int usageIdx = json.indexOf("\"usage\"");
            if (usageIdx >= 0) {
                int objStart = json.indexOf("{", usageIdx);
                int objEnd = json.indexOf("}", objStart);
                if (objStart >= 0 && objEnd > objStart) {
                    String usageObj = json.substring(objStart, objEnd + 1);
                    inputTokens = extractJsonInt(usageObj, "input_tokens");
                    outputTokens = extractJsonInt(usageObj, "output_tokens");
                }
            }

            // Parse request ID
            requestId = extractJsonValueString(json, "id");

            // Parse content
            int contentIdx = json.indexOf("\"content\"");
            if (contentIdx >= 0) {
                int arrStart = json.indexOf("[", contentIdx);
                int arrEnd = json.indexOf("]", arrStart);
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
                        String type = extractJsonValueString(obj, "type");

                        if ("text".equals(type)) {
                            String text = extractJsonValueString(obj, "text");
                            content.add(Map.of("type", "text", "text", text != null ? text : ""));
                        }

                        i = objEnd;
                    }
                }
            }
        } catch (Exception e) {
            // Return minimal result on parse error
        }

        return new SideQueryResult(content, new Usage(inputTokens, outputTokens, 0, 0), requestId, model);
    }

    private static String extractJsonValueString(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int valStart = json.indexOf("\"", idx + key.length() + 3) + 1;
        if (valStart < 1) return null;
        int valEnd = json.indexOf("\"", valStart);
        return json.substring(valStart, valEnd);
    }

    private static int extractJsonInt(String json, String key) {
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

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Side query result.
     */
    public record SideQueryResult(
            List<Map<String, Object>> content,
            Usage usage,
            String requestId,
            String model
    ) {}

    /**
     * Usage statistics.
     */
    public record Usage(
            int inputTokens,
            int outputTokens,
            int cachedInputTokens,
            int cacheCreationTokens
    ) {}
}