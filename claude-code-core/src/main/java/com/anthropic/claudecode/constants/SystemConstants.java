/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/system.ts
 */
package com.anthropic.claudecode.constants;

import java.util.Set;

/**
 * System constants for CLI sysprompt prefixes.
 */
public final class SystemConstants {
    private SystemConstants() {}

    // CLI sysprompt prefixes
    public static final String DEFAULT_PREFIX = "You are Claude Code, Anthropic's official CLI for Claude.";
    public static final String AGENT_SDK_CLAUDE_CODE_PRESET_PREFIX =
        "You are Claude Code, Anthropic's official CLI for Claude, running within the Claude Agent SDK.";
    public static final String AGENT_SDK_PREFIX =
        "You are a Claude agent, built on Anthropic's Claude Agent SDK.";

    // All possible CLI sysprompt prefix values
    public static final Set<String> CLI_SYSPROMPT_PREFIXES = Set.of(
        DEFAULT_PREFIX,
        AGENT_SDK_CLAUDE_CODE_PRESET_PREFIX,
        AGENT_SDK_PREFIX
    );

    /**
     * Get CLI sysprompt prefix based on context.
     */
    public static String getCLISyspromptPrefix(
        String apiProvider,
        boolean isNonInteractive,
        boolean hasAppendSystemPrompt
    ) {
        if ("vertex".equals(apiProvider)) {
            return DEFAULT_PREFIX;
        }

        if (isNonInteractive) {
            if (hasAppendSystemPrompt) {
                return AGENT_SDK_CLAUDE_CODE_PRESET_PREFIX;
            }
            return AGENT_SDK_PREFIX;
        }
        return DEFAULT_PREFIX;
    }

    /**
     * Get attribution header for API requests.
     */
    public static String getAttributionHeader(
        String version,
        String fingerprint,
        String entrypoint,
        String workload,
        boolean nativeClientAttestation
    ) {
        String fullVersion = version + "." + fingerprint;
        String cch = nativeClientAttestation ? " cch=00000;" : "";
        String workloadPair = workload != null ? " cc_workload=" + workload + ";" : "";

        return "x-anthropic-billing-header: cc_version=" + fullVersion +
               "; cc_entrypoint=" + entrypoint + ";" + cch + workloadPair;
    }
}