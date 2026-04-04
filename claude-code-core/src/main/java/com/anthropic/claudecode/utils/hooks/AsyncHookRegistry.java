/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/AsyncHookRegistry.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.util.*;
import java.util.concurrent.*;

/**
 * Registry for managing pending async hooks.
 */
public final class AsyncHookRegistry {
    private AsyncHookRegistry() {}

    private static final ConcurrentHashMap<String, PendingAsyncHook> pendingHooks =
        new ConcurrentHashMap<>();

    /**
     * Pending async hook state.
     */
    public record PendingAsyncHook(
        String processId,
        String hookId,
        String hookName,
        String hookEvent,
        String toolName,
        String pluginId,
        long startTime,
        int timeout,
        String command,
        boolean responseAttachmentSent,
        ShellCommandState shellCommand,
        Runnable stopProgressInterval
    ) {}

    /**
     * Shell command state placeholder.
     */
    public record ShellCommandState(
        String status,
        CompletableFuture<ShellResult> result,
        TaskOutput taskOutput,
        Runnable cleanup,
        Runnable kill
    ) {}

    /**
     * Shell result placeholder.
     */
    public record ShellResult(int code, String stdout, String stderr) {}

    /**
     * Task output placeholder.
     */
    public interface TaskOutput {
        String getStdout();
        String getStderr();
    }

    /**
     * Sync hook JSON output placeholder.
     */
    public record SyncHookJSONOutput(
        Boolean blockTool,
        String blockReason,
        String modifiedToolInputJSON,
        Map<String, Object> additionalFields
    ) {}

    /**
     * Async hook JSON output placeholder.
     */
    public record AsyncHookJSONOutput(
        Boolean async,
        Integer asyncTimeout,
        Map<String, Object> additionalFields
    ) {}

    /**
     * Register a pending async hook.
     */
    public static void registerPendingAsyncHook(
            String processId,
            String hookId,
            AsyncHookJSONOutput asyncResponse,
            String hookName,
            String hookEvent,
            String command,
            ShellCommandState shellCommand,
            String toolName,
            String pluginId) {

        int timeout = asyncResponse.asyncTimeout() != null ? asyncResponse.asyncTimeout() : 15000;
        logForDebugging("Hooks: Registering async hook " + processId + " (" + hookName +
            ") with timeout " + timeout + "ms");

        Runnable stopProgressInterval = HookEvents.startHookProgressInterval(
            hookId, hookName, hookEvent,
            () -> {
                PendingAsyncHook hook = pendingHooks.get(processId);
                if (hook == null || hook.shellCommand() == null) {
                    return new OutputResult("", "", "");
                }
                TaskOutput taskOutput = hook.shellCommand().taskOutput();
                if (taskOutput == null) {
                    return new OutputResult("", "", "");
                }
                String stdout = taskOutput.getStdout();
                String stderr = taskOutput.getStderr();
                return new OutputResult(stdout, stderr, stdout + stderr);
            });

        pendingHooks.put(processId, new PendingAsyncHook(
            processId,
            hookId,
            hookName,
            hookEvent,
            toolName,
            pluginId,
            System.currentTimeMillis(),
            timeout,
            command,
            false,
            shellCommand,
            stopProgressInterval
        ));
    }

    /**
     * Output result for progress interval.
     */
    public record OutputResult(String stdout, String stderr, String output) {}

    /**
     * Get all pending async hooks.
     */
    public static List<PendingAsyncHook> getPendingAsyncHooks() {
        return pendingHooks.values().stream()
            .filter(hook -> !hook.responseAttachmentSent())
            .toList();
    }

    /**
     * Finalize a hook with outcome.
     */
    private static void finalizeHook(
            PendingAsyncHook hook,
            int exitCode,
            String outcome) {

        hook.stopProgressInterval().run();
        TaskOutput taskOutput = hook.shellCommand() != null ? hook.shellCommand().taskOutput() : null;
        String stdout = taskOutput != null ? taskOutput.getStdout() : "";
        String stderr = taskOutput != null ? taskOutput.getStderr() : "";

        if (hook.shellCommand() != null && hook.shellCommand().cleanup() != null) {
            hook.shellCommand().cleanup().run();
        }

        HookEvents.emitHookResponse(
            hook.hookId(),
            hook.hookName(),
            hook.hookEvent(),
            stdout + stderr,
            stdout,
            stderr,
            exitCode,
            outcome
        );
    }

    /**
     * Hook response result.
     */
    public record HookResponse(
        String processId,
        SyncHookJSONOutput response,
        String hookName,
        String hookEvent,
        String toolName,
        String pluginId,
        String stdout,
        String stderr,
        Integer exitCode
    ) {}

    /**
     * Check for async hook responses.
     */
    public static CompletableFuture<List<HookResponse>> checkForAsyncHookResponses() {
        List<HookResponse> responses = new ArrayList<>();
        int pendingCount = pendingHooks.size();
        logForDebugging("Hooks: Found " + pendingCount + " total hooks in registry");

        List<PendingAsyncHook> hooks = new ArrayList<>(pendingHooks.values());

        for (PendingAsyncHook hook : hooks) {
            try {
                TaskOutput taskOutput = hook.shellCommand() != null ? hook.shellCommand().taskOutput() : null;
                String stdout = taskOutput != null ? taskOutput.getStdout() : "";
                String stderr = hook.shellCommand() != null && hook.shellCommand().taskOutput() != null
                    ? hook.shellCommand().taskOutput().getStderr() : "";

                logForDebugging("Hooks: Checking hook " + hook.processId() + " (" + hook.hookName() +
                    ") - attachmentSent: " + hook.responseAttachmentSent() + ", stdout length: " + stdout.length());

                if (hook.shellCommand() == null) {
                    logForDebugging("Hooks: Hook " + hook.processId() +
                        " has no shell command, removing from registry");
                    hook.stopProgressInterval().run();
                    pendingHooks.remove(hook.processId());
                    continue;
                }

                logForDebugging("Hooks: Hook shell status " + hook.shellCommand().status());

                if ("killed".equals(hook.shellCommand().status())) {
                    logForDebugging("Hooks: Hook " + hook.processId() + " is " +
                        hook.shellCommand().status() + ", removing from registry");
                    hook.stopProgressInterval().run();
                    if (hook.shellCommand().cleanup() != null) {
                        hook.shellCommand().cleanup().run();
                    }
                    pendingHooks.remove(hook.processId());
                    continue;
                }

                if (!"completed".equals(hook.shellCommand().status())) {
                    continue;
                }

                if (hook.responseAttachmentSent() || stdout.trim().isEmpty()) {
                    logForDebugging("Hooks: Skipping hook " + hook.processId() +
                        " - already delivered/sent or no stdout");
                    hook.stopProgressInterval().run();
                    pendingHooks.remove(hook.processId());
                    continue;
                }

                String[] lines = stdout.split("\n");
                logForDebugging("Hooks: Processing " + lines.length +
                    " lines of stdout for " + hook.processId());

                int exitCode = 0;
                if (hook.shellCommand().result() != null) {
                    try {
                        ShellResult result = hook.shellCommand().result().getNow(null);
                        if (result != null) {
                            exitCode = result.code();
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }

                SyncHookJSONOutput response = new SyncHookJSONOutput(null, null, null, new HashMap<>());
                for (String line : lines) {
                    if (line.trim().startsWith("{")) {
                        logForDebugging("Hooks: Found JSON line: " +
                            line.trim().substring(0, Math.min(100, line.trim().length())) + "...");
                        try {
                            org.json.JSONObject parsed = new org.json.JSONObject(line.trim());
                            if (!parsed.has("async")) {
                                logForDebugging("Hooks: Found sync response from " +
                                    hook.processId() + ": " + parsed.toString());
                                response = parseSyncResponse(parsed);
                                break;
                            }
                        } catch (Exception e) {
                            logForDebugging("Hooks: Failed to parse JSON from " +
                                hook.processId() + ": " + line.trim());
                        }
                    }
                }

                pendingHooks.put(hook.processId(), new PendingAsyncHook(
                    hook.processId(),
                    hook.hookId(),
                    hook.hookName(),
                    hook.hookEvent(),
                    hook.toolName(),
                    hook.pluginId(),
                    hook.startTime(),
                    hook.timeout(),
                    hook.command(),
                    true,
                    hook.shellCommand(),
                    hook.stopProgressInterval()
                ));

                finalizeHook(hook, exitCode, exitCode == 0 ? "success" : "error");
                pendingHooks.remove(hook.processId());

                responses.add(new HookResponse(
                    hook.processId(),
                    response,
                    hook.hookName(),
                    hook.hookEvent(),
                    hook.toolName(),
                    hook.pluginId(),
                    stdout,
                    stderr,
                    exitCode
                ));
            } catch (Exception e) {
                logForDebugging("Hooks: checkForAsyncHookResponses callback rejected: " + e.getMessage());
            }
        }

        logForDebugging("Hooks: checkForNewResponses returning " + responses.size() + " responses");
        return CompletableFuture.completedFuture(responses);
    }

    private static SyncHookJSONOutput parseSyncResponse(org.json.JSONObject obj) {
        Boolean blockTool = obj.has("blockTool") ? obj.optBoolean("blockTool") : null;
        String blockReason = obj.optString("blockReason", null);
        String modifiedToolInputJSON = obj.optString("modifiedToolInputJSON", null);

        Map<String, Object> additional = new HashMap<>();
        for (String key : obj.keySet()) {
            if (!key.equals("blockTool") && !key.equals("blockReason") &&
                !key.equals("modifiedToolInputJSON")) {
                additional.put(key, obj.opt(key));
            }
        }

        return new SyncHookJSONOutput(blockTool, blockReason, modifiedToolInputJSON, additional);
    }

    /**
     * Remove delivered async hooks.
     */
    public static void removeDeliveredAsyncHooks(List<String> processIds) {
        for (String processId : processIds) {
            PendingAsyncHook hook = pendingHooks.get(processId);
            if (hook != null && hook.responseAttachmentSent()) {
                logForDebugging("Hooks: Removing delivered hook " + processId);
                hook.stopProgressInterval().run();
                pendingHooks.remove(processId);
            }
        }
    }

    /**
     * Finalize all pending async hooks.
     */
    public static CompletableFuture<Void> finalizePendingAsyncHooks() {
        List<PendingAsyncHook> hooks = new ArrayList<>(pendingHooks.values());
        for (PendingAsyncHook hook : hooks) {
            try {
                if (hook.shellCommand() != null && "completed".equals(hook.shellCommand().status())) {
                    ShellResult result = hook.shellCommand().result() != null
                        ? hook.shellCommand().result().getNow(new ShellResult(0, "", ""))
                        : new ShellResult(0, "", "");
                    finalizeHook(hook, result.code(), result.code() == 0 ? "success" : "error");
                } else {
                    if (hook.shellCommand() != null &&
                        !"killed".equals(hook.shellCommand().status()) &&
                        hook.shellCommand().kill() != null) {
                        hook.shellCommand().kill().run();
                    }
                    finalizeHook(hook, 1, "cancelled");
                }
            } catch (Exception e) {
                // Ignore errors during cleanup
            }
        }
        pendingHooks.clear();
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Clear all async hooks (for testing).
     */
    public static void clearAllAsyncHooks() {
        for (PendingAsyncHook hook : pendingHooks.values()) {
            hook.stopProgressInterval().run();
        }
        pendingHooks.clear();
    }

    private static void logForDebugging(String message) {
        if (System.getenv("CLAUDE_CODE_DEBUG") != null) {
            System.err.println("[hooks] " + message);
        }
    }
}