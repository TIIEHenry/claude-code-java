/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/betas.ts
 */
package com.anthropic.claudecode.constants;

import java.util.Set;
import java.util.HashSet;

/**
 * Beta header constants for API features.
 */
public final class Betas {
    private Betas() {}

    // Beta headers
    public static final String CLAUDE_CODE_20250219_BETA_HEADER = "claude-code-20250219";
    public static final String INTERLEAVED_THINKING_BETA_HEADER = "interleaved-thinking-2025-05-14";
    public static final String CONTEXT_1M_BETA_HEADER = "context-1m-2025-08-07";
    public static final String CONTEXT_MANAGEMENT_BETA_HEADER = "context-management-2025-06-27";
    public static final String STRUCTURED_OUTPUTS_BETA_HEADER = "structured-outputs-2025-12-15";
    public static final String WEB_SEARCH_BETA_HEADER = "web-search-2025-03-05";

    // Tool search beta headers differ by provider
    public static final String TOOL_SEARCH_BETA_HEADER_1P = "advanced-tool-use-2025-11-20";
    public static final String TOOL_SEARCH_BETA_HEADER_3P = "tool-search-tool-2025-10-19";

    public static final String EFFORT_BETA_HEADER = "effort-2025-11-24";
    public static final String TASK_BUDGETS_BETA_HEADER = "task-budgets-2026-03-13";
    public static final String PROMPT_CACHING_SCOPE_BETA_HEADER = "prompt-caching-scope-2026-01-05";
    public static final String FAST_MODE_BETA_HEADER = "fast-mode-2026-02-01";
    public static final String REDACT_THINKING_BETA_HEADER = "redact-thinking-2026-02-12";
    public static final String TOKEN_EFFICIENT_TOOLS_BETA_HEADER = "token-efficient-tools-2026-03-28";
    public static final String SUMMARIZE_CONNECTOR_TEXT_BETA_HEADER = "summarize-connector-text-2026-03-13";
    public static final String AFK_MODE_BETA_HEADER = "afk-mode-2026-01-31";
    public static final String CLI_INTERNAL_BETA_HEADER = "cli-internal-2026-02-09";
    public static final String ADVISOR_BETA_HEADER = "advisor-tool-2026-03-01";

    /**
     * Bedrock only supports a limited number of beta headers and only through
     * extraBodyParams. This set maintains the beta strings that should be in
     * Bedrock extraBodyParams *and not* in Bedrock headers.
     */
    public static final Set<String> BEDROCK_EXTRA_PARAMS_HEADERS = Set.of(
        INTERLEAVED_THINKING_BETA_HEADER,
        CONTEXT_1M_BETA_HEADER,
        TOOL_SEARCH_BETA_HEADER_3P
    );

    /**
     * Betas allowed on Vertex countTokens API.
     * Other betas will cause 400 errors.
     */
    public static final Set<String> VERTEX_COUNT_TOKENS_ALLOWED_BETAS = Set.of(
        CLAUDE_CODE_20250219_BETA_HEADER,
        INTERLEAVED_THINKING_BETA_HEADER,
        CONTEXT_MANAGEMENT_BETA_HEADER
    );
}