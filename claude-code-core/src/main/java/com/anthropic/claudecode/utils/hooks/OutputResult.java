/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code hook output result
 */
package com.anthropic.claudecode.utils.hooks;

/**
 * Output result from hook execution.
 */
public record OutputResult(String stdout, String stderr, String combined) {
    public static OutputResult empty() {
        return new OutputResult("", "", "");
    }
}