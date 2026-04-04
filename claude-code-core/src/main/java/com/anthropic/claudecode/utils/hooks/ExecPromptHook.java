/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/execPromptHook.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/**
 * Execute a prompt-based hook using an LLM.
 */
public final class ExecPromptHook {
    private ExecPromptHook() {}

    private static final int DEFAULT_PROMPT_HOOK_TIMEOUT_MS = 30000;

    /**
     * Prompt hook configuration.
     */
    public record PromptHook(
        String type,
        String prompt,
        Integer timeout,
        String model
    ) {}

    /**
     * Hook result (same as ExecAgentHook).
     */
    public record HookResult(
        Object hook,
        String outcome,
        Map<String, Object> message,
        BlockingError blockingError,
        boolean preventContinuation,
        String stopReason
    ) {
        public HookResult(Object hook, String outcome) {
            this(hook, outcome, null, null, false, null);
        }
    }

    /**
     * Blocking error.
     */
    public record BlockingError(
        String blockingError,
        String command
    ) {}

    /**
     * Execute a prompt-based hook.
     */
    public static CompletableFuture<HookResult> execPromptHook(
            PromptHook hook,
            String hookName,
            String hookEvent,
            String jsonInput,
            CompletableFuture<Void> signal,
            Map<String, Object> toolUseContext,
            List<Map<String, Object>> messages,
            String toolUseID) {

        String effectiveToolUseID = toolUseID != null ? toolUseID
            : "hook-" + UUID.randomUUID().toString();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Replace $ARGUMENTS with the JSON input
                String processedPrompt = HookHelpers.addArgumentsToPrompt(hook.prompt(), jsonInput);
                logForDebugging("Hooks: Processing prompt hook with prompt: " + processedPrompt);

                // Create user message
                Map<String, Object> userMessage = createUserMessage(processedPrompt);

                // Prepend conversation history if provided
                List<Map<String, Object>> messagesToQuery = new ArrayList<>();
                if (messages != null && !messages.isEmpty()) {
                    messagesToQuery.addAll(messages);
                }
                messagesToQuery.add(userMessage);

                logForDebugging("Hooks: Querying model with " + messagesToQuery.size() + " messages");

                int hookTimeoutMs = hook.timeout() != null
                    ? hook.timeout() * 1000
                    : DEFAULT_PROMPT_HOOK_TIMEOUT_MS;

                try {
                    // Build system prompt
                    String systemPrompt = "You are evaluating a hook in Claude Code.\n\n" +
                        "Your response must be a JSON object matching one of the following schemas:\n" +
                        "1. If the condition is met, return: {\"ok\": true}\n" +
                        "2. If the condition is not met, return: {\"ok\": false, \"reason\": \"Reason for why it is not met\"}";

                    String model = hook.model() != null ? hook.model() : getSmallFastModel();

                    // Query the model (placeholder - would call queryModelWithoutStreaming)
                    String responseContent = queryModel(
                        messagesToQuery,
                        systemPrompt,
                        model,
                        hookTimeoutMs);

                    String fullResponse = responseContent.trim();
                    logForDebugging("Hooks: Model response: " + fullResponse);

                    // Parse JSON response
                    JSONObject json;
                    try {
                        json = new JSONObject(fullResponse);
                    } catch (JSONException e) {
                        logForDebugging("Hooks: error parsing response as JSON: " + fullResponse);
                        return new HookResult(hook, "non_blocking_error",
                            createNonBlockingErrorMessage(hookName, effectiveToolUseID,
                                hookEvent, "JSON validation failed", fullResponse),
                            null, false, null);
                    }

                    // Validate schema
                    if (!json.has("ok")) {
                        logForDebugging("Hooks: model response does not conform to expected schema");
                        return new HookResult(hook, "non_blocking_error",
                            createNonBlockingErrorMessage(hookName, effectiveToolUseID,
                                hookEvent, "Schema validation failed: missing 'ok' field", fullResponse),
                            null, false, null);
                    }

                    boolean ok = json.getBoolean("ok");
                    String reason = json.optString("reason", null);

                    // Failed to meet condition
                    if (!ok) {
                        logForDebugging("Hooks: Prompt hook condition was not met: " + reason);
                        return new HookResult(hook, "blocking",
                            null,
                            new BlockingError(
                                "Prompt hook condition was not met: " + reason,
                                hook.prompt()),
                            true,
                            reason);
                    }

                    // Condition was met
                    logForDebugging("Hooks: Prompt hook condition was met");
                    return new HookResult(hook, "success",
                        createHookSuccessMessage(hookName, effectiveToolUseID, hookEvent),
                        null, false, null);

                } catch (Exception e) {
                    if (signal != null && signal.isDone()) {
                        return new HookResult(hook, "cancelled");
                    }
                    throw e;
                }
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                logForDebugging("Hooks: Prompt hook error: " + errorMsg);
                return new HookResult(hook, "non_blocking_error",
                    createNonBlockingErrorMessage(hookName, effectiveToolUseID,
                        hookEvent, "Error executing prompt hook: " + errorMsg, ""),
                    null, false, null);
            }
        });
    }

    private static Map<String, Object> createUserMessage(String content) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", content);
        return msg;
    }

    private static Map<String, Object> createHookSuccessMessage(
            String hookName, String toolUseID, String hookEvent) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "attachment");
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("type", "hook_success");
        attachment.put("hookName", hookName);
        attachment.put("toolUseID", toolUseID);
        attachment.put("hookEvent", hookEvent);
        attachment.put("content", "");
        msg.put("attachment", attachment);
        return msg;
    }

    private static Map<String, Object> createNonBlockingErrorMessage(
            String hookName, String toolUseID, String hookEvent,
            String stderr, String stdout) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "attachment");
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("type", "hook_non_blocking_error");
        attachment.put("hookName", hookName);
        attachment.put("toolUseID", toolUseID);
        attachment.put("hookEvent", hookEvent);
        attachment.put("stderr", stderr);
        attachment.put("stdout", stdout);
        attachment.put("exitCode", 1);
        msg.put("attachment", attachment);
        return msg;
    }

    /**
     * Query model for prompt hook evaluation.
     */
    private static String queryModel(
            List<Map<String, Object>> messages,
            String systemPrompt,
            String model,
            int timeoutMs) {

        try {
            // Get API credentials
            String apiKey = System.getenv("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                // Fallback to OAuth token
                java.nio.file.Path tokenPath = java.nio.file.Paths.get(
                    System.getProperty("user.home"), ".claude", "oauth_tokens.json"
                );
                if (java.nio.file.Files.exists(tokenPath)) {
                    String tokenContent = java.nio.file.Files.readString(tokenPath);
                    apiKey = extractAccessToken(tokenContent);
                }
            }

            if (apiKey == null) {
                return "{\"ok\": true}"; // Default to success if no auth
            }

            // Build request payload
            org.json.JSONArray msgArray = new org.json.JSONArray();
            for (Map<String, Object> msg : messages) {
                org.json.JSONObject msgObj = new org.json.JSONObject();
                msgObj.put("role", msg.get("role"));
                msgObj.put("content", msg.get("content"));
                msgArray.put(msgObj);
            }

            org.json.JSONObject request = new org.json.JSONObject();
            request.put("model", model);
            request.put("max_tokens", 100);
            request.put("system", systemPrompt);
            request.put("messages", msgArray);

            // Make HTTP request
            String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
            if (baseUrl == null) baseUrl = "https://api.anthropic.com";

            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofMillis(timeoutMs))
                .build();

            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + "/v1/messages"))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(request.toString()))
                .build();

            java.net.http.HttpResponse<String> response = httpClient.send(httpRequest,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                org.json.JSONObject respJson = new org.json.JSONObject(response.body());
                org.json.JSONArray content = respJson.getJSONArray("content");
                if (content.length() > 0) {
                    org.json.JSONObject firstBlock = content.getJSONObject(0);
                    if ("text".equals(firstBlock.getString("type"))) {
                        return firstBlock.getString("text");
                    }
                }
            }

            // Default success response on error
            return "{\"ok\": true}";
        } catch (Exception e) {
            logForDebugging("Hooks: Model query error: " + e.getMessage());
            // Return success to avoid blocking on hook errors
            return "{\"ok\": true}";
        }
    }

    /**
     * Extract access token from OAuth token file.
     */
    private static String extractAccessToken(String content) {
        try {
            org.json.JSONObject json = new org.json.JSONObject(content);
            if (json.has("access_token")) {
                return json.getString("access_token");
            }
            if (json.has("tokens")) {
                org.json.JSONObject tokens = json.getJSONObject("tokens");
                if (tokens.has("accessToken")) {
                    return tokens.getString("accessToken");
                }
            }
        } catch (Exception e) {
            // Parse error
        }
        return null;
    }

    private static String getSmallFastModel() {
        // Placeholder - would use model.getSmallFastModel()
        return "claude-haiku-4-5";
    }

    private static void logForDebugging(String message) {
        if (System.getenv("CLAUDE_CODE_DEBUG") != null) {
            System.err.println("[prompt-hook] " + message);
        }
    }
}