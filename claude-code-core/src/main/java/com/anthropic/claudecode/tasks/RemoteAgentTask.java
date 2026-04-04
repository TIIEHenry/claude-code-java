/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code RemoteAgentTask
 */
package com.anthropic.claudecode.tasks;

import java.util.concurrent.*;

/**
 * Remote agent task - runs an agent on a remote server.
 */
public final class RemoteAgentTask implements Task {
    private static final RemoteAgentTask INSTANCE = new RemoteAgentTask();

    private RemoteAgentTask() {}

    public static RemoteAgentTask getInstance() {
        return INSTANCE;
    }

    @Override
    public TaskType getType() {
        return TaskType.REMOTE_AGENT;
    }

    @Override
    public String getName() {
        return "RemoteAgent";
    }

    @Override
    public String getDescription() {
        return "Run an agent on a remote server";
    }

    @Override
    public CompletableFuture<TaskResult> execute(TaskContext context) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            try {
                String agentName = (String) context.getProperty("agentName");
                String serverUrl = (String) context.getProperty("serverUrl");
                String prompt = (String) context.getProperty("prompt");

                if (agentName == null || agentName.isEmpty()) {
                    return TaskResult.failure("No agent name specified");
                }

                if (serverUrl == null || serverUrl.isEmpty()) {
                    return TaskResult.failure("No server URL specified");
                }

                // Execute remote agent via HTTP API call
                String apiKey = System.getenv("ANTHROPIC_API_KEY");
                if (apiKey == null || apiKey.isEmpty()) {
                    return TaskResult.failure("No API key configured for remote agent", System.currentTimeMillis() - start);
                }

                // Build request JSON
                String model = (String) context.getPropertyOrDefault("model", "claude-haiku-4-5-20251001");
                int maxTokens = (Integer) context.getPropertyOrDefault("maxTokens", 4096);

                StringBuilder jsonBody = new StringBuilder();
                jsonBody.append("{\"model\":\"").append(model).append("\",");
                jsonBody.append("\"max_tokens\":").append(maxTokens).append(",");
                jsonBody.append("\"messages\":[{\"role\":\"user\",\"content\":\"");
                jsonBody.append(escapeJson(prompt)).append("\"}]}");

                // Make HTTP request to remote server
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(serverUrl + "/v1/messages"))
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
                    return TaskResult.success("Remote agent " + agentName + " completed: " + resultContent, duration);
                } else {
                    return TaskResult.failure("Remote agent failed: " + response.statusCode(), duration);
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