/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/postSamplingHooks.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Post-sampling hooks - internal API not exposed through settings.
 */
public final class PostSamplingHooks {
    private PostSamplingHooks() {}

    private static final List<PostSamplingHook> postSamplingHooks = new CopyOnWriteArrayList<>();

    /**
     * REPL hook context for both post-sampling and stop hooks.
     */
    public record REPLHookContext(
        List<Map<String, Object>> messages,
        Map<String, String> systemPrompt,
        Map<String, String> userContext,
        Map<String, String> systemContext,
        Map<String, Object> toolUseContext,
        String querySource
    ) {}

    /**
     * Post-sampling hook interface.
     */
    @FunctionalInterface
    public interface PostSamplingHook {
        CompletableFuture<Void> apply(REPLHookContext context);
    }

    /**
     * Register a post-sampling hook that will be called after model sampling completes.
     * This is an internal API not exposed through settings.
     */
    public static void registerPostSamplingHook(PostSamplingHook hook) {
        postSamplingHooks.add(hook);
    }

    /**
     * Clear all registered post-sampling hooks (for testing).
     */
    public static void clearPostSamplingHooks() {
        postSamplingHooks.clear();
    }

    /**
     * Execute all registered post-sampling hooks.
     */
    public static CompletableFuture<Void> executePostSamplingHooks(
            List<Map<String, Object>> messages,
            Map<String, String> systemPrompt,
            Map<String, String> userContext,
            Map<String, String> systemContext,
            Map<String, Object> toolUseContext,
            String querySource) {

        REPLHookContext context = new REPLHookContext(
            messages,
            systemPrompt,
            userContext,
            systemContext,
            toolUseContext,
            querySource
        );

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (PostSamplingHook hook : postSamplingHooks) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    hook.apply(context).join();
                } catch (Exception e) {
                    // Log but don't fail on hook errors
                    logError(e);
                }
                return null;
            }));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Get the count of registered hooks.
     */
    public static int getHookCount() {
        return postSamplingHooks.size();
    }

    private static void logError(Exception e) {
        if (System.getenv("CLAUDE_CODE_DEBUG") != null) {
            System.err.println("[post-sampling-hooks] Hook error: " + e.getMessage());
        }
    }
}