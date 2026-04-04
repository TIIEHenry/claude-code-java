/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/errorIds.ts
 */
package com.anthropic.claudecode.constants;

/**
 * Error IDs for tracking error sources in production.
 * These IDs are obfuscated identifiers that help us trace
 * which logError() call generated an error.
 *
 * ADDING A NEW ERROR TYPE:
 * 1. Add a const based on Next ID.
 * 2. Increment Next ID.
 * Next ID: 346
 */
public final class ErrorIds {
    private ErrorIds() {}

    // Error IDs
    public static final int E_TOOL_USE_SUMMARY_GENERATION_FAILED = 344;
}