/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code subprocess environment utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Utilities for managing subprocess environments.
 *
 * Strips sensitive secrets from subprocess environments when running inside
 * GitHub Actions to prevent prompt-injection attacks.
 */
public final class SubprocessEnv {
    private SubprocessEnv() {}

    /**
     * Environment variables to strip from subprocess environments when running
     * inside GitHub Actions. This prevents prompt-injection attacks from
     * exfiltrating secrets via shell expansion.
     */
    private static final Set<String> GHA_SUBPROCESS_SCRUB = Set.of(
            // Anthropic auth — claude re-reads these per-request
            "ANTHROPIC_API_KEY",
            "CLAUDE_CODE_OAUTH_TOKEN",
            "ANTHROPIC_AUTH_TOKEN",
            "ANTHROPIC_FOUNDRY_API_KEY",
            "ANTHROPIC_CUSTOM_HEADERS",

            // OTLP exporter headers
            "OTEL_EXPORTER_OTLP_HEADERS",
            "OTEL_EXPORTER_OTLP_LOGS_HEADERS",
            "OTEL_EXPORTER_OTLP_METRICS_HEADERS",
            "OTEL_EXPORTER_OTLP_TRACES_HEADERS",

            // Cloud provider creds
            "AWS_SECRET_ACCESS_KEY",
            "AWS_SESSION_TOKEN",
            "AWS_BEARER_TOKEN_BEDROCK",
            "GOOGLE_APPLICATION_CREDENTIALS",
            "AZURE_CLIENT_SECRET",
            "AZURE_CLIENT_CERTIFICATE_PATH",

            // GitHub Actions OIDC
            "ACTIONS_ID_TOKEN_REQUEST_TOKEN",
            "ACTIONS_ID_TOKEN_REQUEST_URL",

            // GitHub Actions artifact/cache API
            "ACTIONS_RUNTIME_TOKEN",
            "ACTIONS_RUNTIME_URL",

            // claude-code-action-specific duplicates
            "ALL_INPUTS",
            "OVERRIDE_GITHUB_TOKEN",
            "DEFAULT_WORKFLOW_TOKEN",
            "SSH_SIGNING_KEY"
    );

    // Upstream proxy env function
    private static volatile java.util.function.Supplier<Map<String, String>> upstreamProxyEnvFn = null;

    /**
     * Register the upstream proxy env function.
     */
    public static void registerUpstreamProxyEnvFn(java.util.function.Supplier<Map<String, String>> fn) {
        upstreamProxyEnvFn = fn;
    }

    /**
     * Returns a copy of the environment with sensitive secrets stripped,
     * for use when spawning subprocesses (Bash tool, shell snapshot, MCP stdio).
     *
     * Gated on CLAUDE_CODE_SUBPROCESS_ENV_SCRUB.
     */
    public static Map<String, String> subprocessEnv() {
        Map<String, String> result = new HashMap<>(System.getenv());

        // Add proxy env if available
        if (upstreamProxyEnvFn != null) {
            Map<String, String> proxyEnv = upstreamProxyEnvFn.get();
            if (proxyEnv != null) {
                result.putAll(proxyEnv);
            }
        }

        // Check if scrubbing is enabled
        if (!EnvUtils.isTruthy(System.getenv("CLAUDE_CODE_SUBPROCESS_ENV_SCRUB"))) {
            return result;
        }

        // Scrub sensitive variables
        for (String key : GHA_SUBPROCESS_SCRUB) {
            result.remove(key);
            // GitHub Actions auto-creates INPUT_<NAME> for `with:` inputs
            result.remove("INPUT_" + key);
        }

        return result;
    }

    /**
     * Get environment for Bash tool execution.
     */
    public static Map<String, String> bashEnv() {
        return subprocessEnv();
    }

    /**
     * Get environment for MCP server execution.
     */
    public static Map<String, String> mcpServerEnv() {
        return subprocessEnv();
    }

    /**
     * Get environment for LSP server execution.
     */
    public static Map<String, String> lspServerEnv() {
        return subprocessEnv();
    }

    /**
     * Get environment for hook execution.
     */
    public static Map<String, String> hookEnv() {
        return subprocessEnv();
    }

    /**
     * Check if subprocess env scrubbing is enabled.
     */
    public static boolean isScrubbingEnabled() {
        return EnvUtils.isTruthy(System.getenv("CLAUDE_CODE_SUBPROCESS_ENV_SCRUB"));
    }

    /**
     * Add custom variables to scrub.
     */
    public static Set<String> getScrubList() {
        return new HashSet<>(GHA_SUBPROCESS_SCRUB);
    }

    /**
     * Create a process builder with scrubbed environment.
     */
    public static ProcessBuilder createProcessBuilder(String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().clear();
        pb.environment().putAll(subprocessEnv());
        return pb;
    }

    /**
     * Create a process builder with custom environment.
     */
    public static ProcessBuilder createProcessBuilder(Map<String, String> extraEnv, String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        Map<String, String> env = subprocessEnv();
        if (extraEnv != null) {
            env.putAll(extraEnv);
        }
        pb.environment().clear();
        pb.environment().putAll(env);
        return pb;
    }
}