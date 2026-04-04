/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/execAgentHook.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.nio.file.*;

/**
 * Execute an agent-based hook using a multi-turn LLM query.
 */
public final class ExecAgentHook {
    private ExecAgentHook() {}

    private static final int DEFAULT_AGENT_HOOK_TIMEOUT_MS = 60000;
    private static final int MAX_AGENT_TURNS = 50;

    /**
     * Agent hook configuration.
     */
    public record AgentHook(
        String type,
        String prompt,
        Integer timeout,
        String model
    ) {}

    /**
     * Hook result.
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
     * Structured output result.
     */
    public record StructuredOutputResult(boolean ok, String reason) {}

    /**
     * Execute an agent-based hook.
     */
    public static CompletableFuture<HookResult> execAgentHook(
            AgentHook hook,
            String hookName,
            String hookEvent,
            String jsonInput,
            CompletableFuture<Void> signal,
            Map<String, Object> toolUseContext,
            String toolUseID,
            List<Map<String, Object>> messages,
            String agentName) {

        String effectiveToolUseID = toolUseID != null ? toolUseID
            : "hook-" + UUID.randomUUID().toString();

        long hookStartTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Replace $ARGUMENTS with the JSON input
                String processedPrompt = HookHelpers.addArgumentsToPrompt(hook.prompt(), jsonInput);
                logForDebugging("Hooks: Processing agent hook with prompt: " + processedPrompt);

                // Create user message
                Map<String, Object> userMessage = createUserMessage(processedPrompt);
                List<Map<String, Object>> agentMessages = new ArrayList<>();
                agentMessages.add(userMessage);

                logForDebugging("Hooks: Starting agent query with " + agentMessages.size() + " messages");

                // Setup timeout
                int hookTimeoutMs = hook.timeout() != null ? hook.timeout() * 1000 : DEFAULT_AGENT_HOOK_TIMEOUT_MS;

                // Create abort controller with timeout
                CompletableFuture<Void> abortSignal = signal != null
                    ? CompletableFuture.anyOf(signal,
                        CompletableFuture.runAsync(() -> {}, CompletableFuture.delayedExecutor(
                            hookTimeoutMs, TimeUnit.MILLISECONDS))).thenApply(o -> null)
                    : CompletableFuture.runAsync(() -> {}, CompletableFuture.delayedExecutor(
                        hookTimeoutMs, TimeUnit.MILLISECONDS));

                try {
                    // Build system prompt
                    String transcriptPath = getTranscriptPath(toolUseContext);
                    String systemPrompt = "You are verifying a stop condition in Claude Code. " +
                        "Your task is to verify that the agent completed the given plan. " +
                        "The conversation transcript is available at: " + transcriptPath + "\n" +
                        "You can read this file to analyze the conversation history if needed.\n\n" +
                        "Use the available tools to inspect the codebase and verify the condition.\n" +
                        "Use as few steps as possible - be efficient and direct.\n\n" +
                        "When done, return your result using the structured_output tool with:\n" +
                        "- ok: true if the condition is met\n" +
                        "- ok: false with reason if the condition is not met";

                    String model = hook.model() != null ? hook.model() : getSmallFastModel();
                    String hookAgentId = "hook-agent-" + UUID.randomUUID().toString();

                    // Execute multi-turn query (placeholder)
                    StructuredOutputResult structuredOutputResult = null;
                    int turnCount = 0;
                    boolean hitMaxTurns = false;

                    // Simulate agent execution
                    while (turnCount < MAX_AGENT_TURNS && structuredOutputResult == null) {
                        turnCount++;

                        // Check for abort
                        if (abortSignal.isDone()) {
                            hitMaxTurns = turnCount >= MAX_AGENT_TURNS;
                            break;
                        }

                        // Placeholder: in real implementation, would call query()
                        // and check for structured_output attachment
                    }

                    // Check if we got a result
                    if (structuredOutputResult == null) {
                        if (hitMaxTurns) {
                            logForDebugging("Hooks: Agent hook did not complete within " +
                                MAX_AGENT_TURNS + " turns");
                            return new HookResult(hook, "cancelled");
                        }

                        logForDebugging("Hooks: Agent hook did not return structured output");
                        return new HookResult(hook, "cancelled");
                    }

                    // Return result based on structured output
                    if (!structuredOutputResult.ok()) {
                        logForDebugging("Hooks: Agent hook condition was not met: " +
                            structuredOutputResult.reason());
                        return new HookResult(hook, "blocking",
                            null,
                            new BlockingError(
                                "Agent hook condition was not met: " + structuredOutputResult.reason(),
                                hook.prompt()),
                            true,
                            structuredOutputResult.reason());
                    }

                    // Condition was met
                    logForDebugging("Hooks: Agent hook condition was met");
                    long durationMs = System.currentTimeMillis() - hookStartTime;

                    return new HookResult(hook, "success",
                        createHookSuccessMessage(hookName, effectiveToolUseID, hookEvent),
                        null, false, null);

                } catch (Exception e) {
                    if (abortSignal.isDone()) {
                        return new HookResult(hook, "cancelled");
                    }
                    throw e;
                }
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                logForDebugging("Hooks: Agent hook error: " + errorMsg);
                return new HookResult(hook, "non_blocking_error",
                    createNonBlockingErrorMessage(hookName, effectiveToolUseID, hookEvent, errorMsg),
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
            String hookName, String toolUseID, String hookEvent, String error) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "attachment");
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("type", "hook_non_blocking_error");
        attachment.put("hookName", hookName);
        attachment.put("toolUseID", toolUseID);
        attachment.put("hookEvent", hookEvent);
        attachment.put("stderr", "Error executing agent hook: " + error);
        attachment.put("stdout", "");
        attachment.put("exitCode", 1);
        msg.put("attachment", attachment);
        return msg;
    }

    private static String getTranscriptPath(Map<String, Object> toolUseContext) {
        // Get transcript path from session storage
        String home = System.getProperty("user.home");
        String sessionId = System.getenv("CLAUDE_CODE_SESSION_ID");

        if (sessionId != null && !sessionId.isEmpty()) {
            return Paths.get(home, ".claude", "sessions", sessionId, "transcript.json").toString();
        }

        // Fall back to current session
        return Paths.get(home, ".claude", "current-session", "transcript.json").toString();
    }

    private static String getSmallFastModel() {
        // Check environment override
        String model = System.getenv("CLAUDE_CODE_SMALL_FAST_MODEL");
        if (model != null && !model.isEmpty()) {
            return model;
        }
        return "claude-haiku-4-5";
    }

    private static void logForDebugging(String message) {
        if (System.getenv("CLAUDE_CODE_DEBUG") != null) {
            System.err.println("[agent-hook] " + message);
        }
    }
}