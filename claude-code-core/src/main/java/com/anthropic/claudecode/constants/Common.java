/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/common.ts
 */
package com.anthropic.claudecode.constants;

/**
 * Common constants.
 */
public final class Common {
    private Common() {}

    // App info
    public static final String APP_NAME = "Claude Code";
    public static final String APP_VERSION = "1.0.0";
    public static final String COMPANY_NAME = "Anthropic";

    // Defaults
    public static final int DEFAULT_MAX_TOKENS = 4096;
    public static final int DEFAULT_TIMEOUT_MS = 120000;
    public static final int MAX_TIMEOUT_MS = 600000;
    public static final int DEFAULT_MAX_TURNS = 100;
    public static final int DEFAULT_THREAD_POOL_SIZE = 4;

    // Limits
    public static final int MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    public static final int MAX_LINE_LENGTH = 2000;
    public static final int MAX_OUTPUT_LINES = 1000;
    public static final int MAX_GLOB_RESULTS = 1000;
    public static final int MAX_GREP_RESULTS = 100;

    // Timeouts
    public static final long CONNECT_TIMEOUT_MS = 30000;
    public static final long READ_TIMEOUT_MS = 120000;
    public static final long WRITE_TIMEOUT_MS = 120000;

    // Paths
    public static final String CLAUDE_DIR = ".claude";
    public static final String CONFIG_FILE = "config.json";
    public static final String MEMORY_FILE = "CLAUDE.md";
    public static final String SKILLS_DIR = "skills";

    // Encoding
    public static final String DEFAULT_CHARSET = "UTF-8";
    public static final String LINE_SEPARATOR = System.lineSeparator();
}