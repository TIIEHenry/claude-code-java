/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tools/RemoteTriggerTool/RemoteTriggerTool.ts
 */
package com.anthropic.claudecode.tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.net.http.*;
import java.net.URI;
import com.anthropic.claudecode.*;

/**
 * RemoteTrigger Tool - manage scheduled remote agent triggers.
 */
public final class RemoteTriggerTool extends AbstractTool<RemoteTriggerTool.Input, RemoteTriggerTool.Output, ToolProgressData> {
    public static final String TOOL_NAME = "RemoteTrigger";

    public RemoteTriggerTool() {
        super(TOOL_NAME, "Manage scheduled remote agent triggers");
    }

    /**
     * Input schema.
     */
    public record Input(
        String action,
        String trigger_id,
        Map<String, Object> body
    ) {}

    /**
     * Output schema.
     */
    public record Output(
        int status,
        String json
    ) {}

    private static final String TRIGGERS_BETA = "ccr-triggers-2026-01-30";

    @Override
    public String description() {
        return "Manage scheduled remote agent triggers for automation";
    }

    @Override
    public String searchHint() {
        return "manage scheduled remote agent triggers";
    }

    @Override
    public boolean shouldDefer() {
        return true;
    }

    @Override
    public boolean isConcurrencySafe(Input input) {
        return true;
    }

    @Override
    public boolean isReadOnly(Input input) {
        return "list".equals(input.action()) || "get".equals(input.action());
    }

    @Override
    public boolean isEnabled() {
        // Check feature flags
        return isFeatureEnabled("tengu_surreal_dali") && isPolicyAllowed("allow_remote_sessions");
    }

    @Override
    public Class<Input> inputType() {
        return Input.class;
    }

    @Override
    public Class<Output> outputType() {
        return Output.class;
    }

    @Override
    public ValidationResult validateInput(Input input) {
        if (input.action() == null) {
            return ValidationResult.failure("action is required", 1);
        }

        String action = input.action();
        if (!Arrays.asList("list", "get", "create", "update", "run").contains(action)) {
            return ValidationResult.failure("Invalid action: " + action, 1);
        }

        if (("get".equals(action) || "update".equals(action) || "run".equals(action))
            && (input.trigger_id() == null || input.trigger_id().isEmpty())) {
            return ValidationResult.failure(action + " requires trigger_id", 1);
        }

        if (("create".equals(action) || "update".equals(action)) && input.body() == null) {
            return ValidationResult.failure(action + " requires body", 1);
        }

        return ValidationResult.success();
    }

    @Override
    public CompletableFuture<ToolResult<Output>> call(
        Input input,
        ToolUseContext context,
        CanUseToolFn canUseTool,
        AssistantMessage parent,
        Consumer<ToolProgress<ToolProgressData>> onProgress
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get OAuth tokens
                String accessToken = getAccessToken();
                if (accessToken == null) {
                    throw new RuntimeException(
                        "Not authenticated with a claude.ai account. Run /login and try again."
                    );
                }

                String orgUUID = getOrganizationUUID();
                if (orgUUID == null) {
                    throw new RuntimeException("Unable to resolve organization UUID.");
                }

                String baseUrl = getBaseApiUrl() + "/v1/code/triggers";

                // Build request
                String method;
                String url;
                String body = null;

                switch (input.action()) {
                    case "list":
                        method = "GET";
                        url = baseUrl;
                        break;
                    case "get":
                        method = "GET";
                        url = baseUrl + "/" + input.trigger_id();
                        break;
                    case "create":
                        method = "POST";
                        url = baseUrl;
                        body = toJson(input.body());
                        break;
                    case "update":
                        method = "POST";
                        url = baseUrl + "/" + input.trigger_id();
                        body = toJson(input.body());
                        break;
                    case "run":
                        method = "POST";
                        url = baseUrl + "/" + input.trigger_id() + "/run";
                        body = "{}";
                        break;
                    default:
                        throw new RuntimeException("Unknown action: " + input.action());
                }

                // Make HTTP request
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .header("anthropic-version", "2023-06-01")
                    .header("anthropic-beta", TRIGGERS_BETA)
                    .header("x-organization-uuid", orgUUID)
                    .timeout(java.time.Duration.ofSeconds(20));

                if ("GET".equals(method)) {
                    requestBuilder.GET();
                } else {
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : "{}"));
                }

                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> response = client.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
                );

                return ToolResult.of(new Output(
                    response.statusCode(),
                    response.body()
                ));

            } catch (Exception e) {
                throw new RuntimeException("Failed to execute remote trigger: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public String formatResult(Output output) {
        return "HTTP " + output.status() + "\n" + output.json();
    }

    // Helpers

    private boolean isFeatureEnabled(String feature) {
        String env = System.getenv("CLAUDE_CODE_FEATURE_" + feature.toUpperCase());
        return "true".equalsIgnoreCase(env) || "1".equals(env);
    }

    private boolean isPolicyAllowed(String policy) {
        String env = System.getenv("CLAUDE_CODE_POLICY_" + policy.toUpperCase());
        return "true".equalsIgnoreCase(env) || "1".equals(env);
    }

    private String getAccessToken() {
        return System.getenv("CLAUDE_ACCESS_TOKEN");
    }

    private String getOrganizationUUID() {
        return System.getenv("CLAUDE_ORG_UUID");
    }

    private String getBaseApiUrl() {
        String url = System.getenv("CLAUDE_API_URL");
        return url != null ? url : "https://api.anthropic.com";
    }

    private String toJson(Map<String, Object> map) {
        if (map == null) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof String) {
                sb.append("\"").append(v).append("\"");
            } else {
                sb.append(v);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}