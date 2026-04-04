/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/toolLimits.ts
 */
package com.anthropic.claudecode.constants;

/**
 * Tool-specific limits.
 */
public final class ToolLimits {
    private ToolLimits() {}

    // Bash tool
    public static final int BASH_MAX_OUTPUT_CHARS = 30000;
    public static final int BASH_DEFAULT_TIMEOUT_MS = 120000;
    public static final int BASH_MAX_TIMEOUT_MS = 600000;

    // File read tool
    public static final int FILE_READ_MAX_LINES = 2000;
    public static final int FILE_READ_MAX_BYTES = 10 * 1024 * 1024; // 10MB
    public static final int FILE_READ_DEFAULT_LIMIT = 2000;

    // File write tool
    public static final int FILE_WRITE_MAX_BYTES = 10 * 1024 * 1024; // 10MB

    // Glob tool
    public static final int GLOB_MAX_RESULTS = 1000;

    // Grep tool
    public static final int GREP_MAX_RESULTS = 100;
    public static final int GREP_MAX_CONTEXT_LINES = 10;

    // Web fetch
    public static final int WEB_FETCH_MAX_BYTES = 5 * 1024 * 1024; // 5MB
    public static final int WEB_FETCH_TIMEOUT_MS = 30000;

    // Web search
    public static final int WEB_SEARCH_MAX_RESULTS = 10;

    // Agent tool
    public static final int AGENT_MAX_TURNS = 50;
    public static final int AGENT_MAX_NESTING_DEPTH = 3;

    // Ask user question
    public static final int ASK_QUESTION_MAX_OPTIONS = 4;

    // Image
    public static final int IMAGE_MAX_DIMENSION = 768;
    public static final int IMAGE_MAX_BYTES = 5 * 1024 * 1024; // 5MB
    public static final String[] IMAGE_SUPPORTED_TYPES = {"image/png", "image/jpeg", "image/gif", "image/webp"};

    // Notebook
    public static final int NOTEBOOK_MAX_CELL_SIZE = 100000;

    // Task output
    public static final int TASK_OUTPUT_MAX_LINES = 1000;
}