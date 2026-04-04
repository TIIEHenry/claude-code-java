/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/messages.ts
 */
package com.anthropic.claudecode.constants;

/**
 * Message constants.
 */
public final class Messages {
    private Messages() {}

    // Common messages
    public static final String NO_CONTENT_MESSAGE = "(no content)";

    // Error messages
    public static final String ERROR_FILE_NOT_FOUND = "File not found: %s";
    public static final String ERROR_FILE_TOO_LARGE = "File too large: %s (max %d bytes)";
    public static final String ERROR_PERMISSION_DENIED = "Permission denied: %s";
    public static final String ERROR_TIMEOUT = "Operation timed out after %d ms";
    public static final String ERROR_INVALID_INPUT = "Invalid input: %s";
    public static final String ERROR_NO_HANDLER = "No handler found for: %s";
    public static final String ERROR_PARSE_FAILED = "Failed to parse: %s";
    public static final String ERROR_API_FAILED = "API request failed: %s";
    public static final String ERROR_RATE_LIMITED = "Rate limited. Please try again later.";
    public static final String ERROR_OVERLOADED = "Service temporarily overloaded. Please try again.";

    // Warning messages
    public static final String WARN_LARGE_OUTPUT = "Output truncated. Showing last %d lines.";
    public static final String WARN_DEPRECATED = "This feature is deprecated: %s";
    public static final String WARN_UNSAFE_COMMAND = "Potentially unsafe command detected: %s";

    // Info messages
    public static final String INFO_PROCESSING = "Processing...";
    public static final String INFO_COMPLETE = "Complete.";
    public static final String INFO_CANCELLED = "Operation cancelled.";
    public static final String INFO_NO_RESULTS = "No results found.";
    public static final String INFO_SAVED = "Saved to: %s";
    public static final String INFO_COPIED = "Copied to clipboard.";

    // Prompt messages
    public static final String PROMPT_CONFIRM = "Are you sure? (y/n)";
    public static final String PROMPT_CONTINUE = "Press Enter to continue...";
    public static final String PROMPT_INPUT = "Enter value: ";
    public static final String PROMPT_SELECT = "Select an option: ";

    // Tool messages
    public static final String TOOL_READ = "Reading: %s";
    public static final String TOOL_WRITE = "Writing: %s";
    public static final String TOOL_EXECUTE = "Executing: %s";
    public static final String TOOL_SEARCH = "Searching: %s";

    // Format strings
    public static final String FORMAT_DURATION = "%dm %ds";
    public static final String FORMAT_TOKENS = "%.1fK tokens";
    public static final String FORMAT_COST = "$%.4f";
    public static final String FORMAT_FILE_SIZE = "%.1f %s";
}