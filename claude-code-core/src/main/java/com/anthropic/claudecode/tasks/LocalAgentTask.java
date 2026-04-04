/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code LocalAgentTask
 */
package com.anthropic.claudecode.tasks;

import java.util.concurrent.*;

/**
 * Local agent task - runs an agent locally.
 */
public final class LocalAgentTask implements Task {
    private static final LocalAgentTask INSTANCE = new LocalAgentTask();

    private LocalAgentTask() {}

    public static LocalAgentTask getInstance() {
        return INSTANCE;
    }

    @Override
    public TaskType getType() {
        return TaskType.LOCAL_AGENT;
    }

    @Override
    public String getName() {
        return "LocalAgent";
    }

    @Override
    public String getDescription() {
        return "Run an agent locally";
    }

    @Override
    public CompletableFuture<TaskResult> execute(TaskContext context) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            try {
                String agentName = (String) context.getProperty("agentName");
                String prompt = (String) context.getProperty("prompt");

                if (agentName == null || agentName.isEmpty()) {
                    return TaskResult.failure("No agent name specified");
                }

                // Execute local agent via API call
                String apiKey = System.getenv("ANTHROPIC_API_KEY");
                if (apiKey == null || apiKey.isEmpty()) {
                    return TaskResult.failure("No API key configured for local agent", System.currentTimeMillis() - start);
                }

                String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
                if (baseUrl == null) baseUrl = "https://api.anthropic.com";

                // Build request JSON
                String model = (String) context.getPropertyOrDefault("model", "claude-haiku-4-5-20251001");
                int maxTokens = (Integer) context.getPropertyOrDefault("maxTokens", 4096);

                StringBuilder jsonBody = new StringBuilder();
                jsonBody.append("{\"model\":\"").append(model).append("\",");
                jsonBody.append("\"max_tokens\":").append(maxTokens).append(",");
                jsonBody.append("\"messages\":[{\"role\":\"user\",\"content\":\"");
                jsonBody.append(escapeJson(prompt)).append("\"}]}");

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

                long duration = System.currentTimeMillis() - start;

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String resultContent = extractContentFromResponse(response.body());
                    return TaskResult.success("Agent " + agentName + " completed: " + resultContent, duration);
                } else {
                    return TaskResult.failure("Agent failed: HTTP " + response.statusCode(), duration);
                }
            } catch (Exception e) {
                return TaskResult.failure(e.getMessage(), System.currentTimeMillis() - start);
            }
        });
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractContentFromResponse(String json) {
        int contentIdx = json.indexOf("\"content\"");
        if (contentIdx < 0) return json;

        int arrStart = json.indexOf("[", contentIdx);
        if (arrStart < 0) return json;

        int textIdx = json.indexOf("\"text\":", arrStart);
        if (textIdx < 0) return json;

        int valStart = json.indexOf("\"", textIdx + 7) + 1;
        int valEnd = json.indexOf("\"", valStart);

        if (valStart > 0 && valEnd > valStart) {
            return json.substring(valStart, valEnd);
        }
        return json;
    }
}