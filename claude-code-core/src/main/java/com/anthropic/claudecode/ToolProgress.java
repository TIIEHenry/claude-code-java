/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode;

/**
 * Tool progress container.
 */
public record ToolProgress<P extends ToolProgressData>(
        String toolUseId,
        P data
) {
    public static <P extends ToolProgressData> ToolProgress<P> of(String toolUseId, P data) {
        return new ToolProgress<>(toolUseId, data);
    }
}