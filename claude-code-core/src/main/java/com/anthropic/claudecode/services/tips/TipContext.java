/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/tips/types.ts
 */
package com.anthropic.claudecode.services.tips;

/**
 * Tip context for determining tip relevance.
 */
public record TipContext(
    String theme,
    java.util.Set<String> bashTools,
    Object readFileState
) {
    public TipContext() {
        this(null, null, null);
    }
}