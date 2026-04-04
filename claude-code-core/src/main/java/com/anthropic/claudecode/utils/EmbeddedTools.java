/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code embedded tools utilities
 */
package com.anthropic.claudecode.utils;

/**
 * Utilities for checking embedded search tools in the native binary.
 * When true, find and grep are shadowed by shell functions that invoke
 * the embedded bfs/ugrep binaries.
 */
public final class EmbeddedTools {
    private EmbeddedTools() {}

    /**
     * Check if this build has bfs/ugrep embedded.
     */
    public static boolean hasEmbeddedSearchTools() {
        if (!EnvUtils.isTruthy(System.getenv("EMBEDDED_SEARCH_TOOLS"))) {
            return false;
        }

        String entrypoint = System.getenv("CLAUDE_CODE_ENTRYPOINT");
        return !"sdk-ts".equals(entrypoint) &&
               !"sdk-py".equals(entrypoint) &&
               !"sdk-cli".equals(entrypoint) &&
               !"local-agent".equals(entrypoint);
    }

    /**
     * Get the path to the binary that contains the embedded search tools.
     */
    public static String embeddedSearchToolsBinaryPath() {
        return ProcessHandle.current().info().command().orElse("java");
    }
}