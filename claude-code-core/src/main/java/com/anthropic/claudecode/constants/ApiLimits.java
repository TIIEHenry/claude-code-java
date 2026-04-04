/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/apiLimits.ts
 */
package com.anthropic.claudecode.constants;

/**
 * API limits and constraints.
 *
 * These constants define server-side limits enforced by the Anthropic API.
 */
public final class ApiLimits {
    private ApiLimits() {}

    // =============================================================================
    // IMAGE LIMITS
    // =============================================================================

    /**
     * Maximum base64-encoded image size (API enforced).
     */
    public static final int API_IMAGE_MAX_BASE64_SIZE = 5 * 1024 * 1024; // 5 MB

    /**
     * Target raw image size to stay under base64 limit after encoding.
     */
    public static final int IMAGE_TARGET_RAW_SIZE = (API_IMAGE_MAX_BASE64_SIZE * 3) / 4; // 3.75 MB

    /**
     * Client-side maximum width for image resizing.
     */
    public static final int IMAGE_MAX_WIDTH = 2000;

    /**
     * Client-side maximum height for image resizing.
     */
    public static final int IMAGE_MAX_HEIGHT = 2000;

    // =============================================================================
    // PDF LIMITS
    // =============================================================================

    /**
     * Maximum raw PDF file size that fits within the API request limit.
     */
    public static final int PDF_TARGET_RAW_SIZE = 20 * 1024 * 1024; // 20 MB

    /**
     * Maximum number of pages in a PDF accepted by the API.
     */
    public static final int API_PDF_MAX_PAGES = 100;

    /**
     * Size threshold above which PDFs are extracted into page images.
     */
    public static final int PDF_EXTRACT_SIZE_THRESHOLD = 3 * 1024 * 1024; // 3 MB

    /**
     * Maximum PDF file size for the page extraction path.
     */
    public static final int PDF_MAX_EXTRACT_SIZE = 100 * 1024 * 1024; // 100 MB

    /**
     * Max pages the Read tool will extract in a single call.
     */
    public static final int PDF_MAX_PAGES_PER_READ = 20;

    /**
     * PDFs with more pages than this get the reference treatment on @ mention.
     */
    public static final int PDF_AT_MENTION_INLINE_THRESHOLD = 10;

    // =============================================================================
    // MEDIA LIMITS
    // =============================================================================

    /**
     * Maximum number of media items (images + PDFs) allowed per API request.
     */
    public static final int API_MAX_MEDIA_PER_REQUEST = 100;

    // =============================================================================
    // TOKEN LIMITS PER MODEL
    // =============================================================================
    public static final int CLAUDE_3_5_SONNET_MAX_TOKENS = 8192;
    public static final int CLAUDE_3_5_HAIKU_MAX_TOKENS = 8192;
    public static final int CLAUDE_3_OPUS_MAX_TOKENS = 4096;
    public static final int CLAUDE_SONNET_4_6_MAX_TOKENS = 16384;
    public static final int CLAUDE_OPUS_4_6_MAX_TOKENS = 16384;

    // Default model
    public static final String DEFAULT_MODEL = "claude-sonnet-4-6";

    // Rate limits
    public static final int MAX_REQUESTS_PER_MINUTE = 60;
    public static final int MAX_TOKENS_PER_MINUTE = 100000;

    // Request limits
    public static final int MAX_MESSAGE_LENGTH = 100000;
    public static final int MAX_SYSTEM_PROMPT_LENGTH = 50000;
    public static final int MAX_TOOL_COUNT = 64;
    public static final int MAX_TOOL_NAME_LENGTH = 64;

    // Retry settings
    public static final int MAX_RETRIES = 3;
    public static final long RETRY_DELAY_MS = 1000;
    public static final long MAX_RETRY_DELAY_MS = 30000;

    // Cache settings
    public static final boolean CACHE_ENABLED_DEFAULT = true;
    public static final long CACHE_TTL_MS = 3600000; // 1 hour
}