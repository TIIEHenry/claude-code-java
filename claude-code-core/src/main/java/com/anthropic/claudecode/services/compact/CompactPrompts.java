/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/compact/prompt.ts
 */
package com.anthropic.claudecode.services.compact;

/**
 * Compaction prompt templates.
 */
public final class CompactPrompts {
    private CompactPrompts() {}

    public static final String COMPACT_SYSTEM_PROMPT = """
You are a conversation summarizer. Your task is to create a concise summary of the conversation history while preserving all important information.

Instructions:
1. Preserve key decisions, conclusions, and outcomes
2. Keep file paths, function names, and code snippets that are still relevant
3. Note any ongoing work or incomplete tasks
4. Maintain the context needed for the assistant to continue helping
5. Be concise but comprehensive

Format your summary as a structured document with these sections:
- Main Tasks: What was accomplished
- Key Decisions: Important choices made
- Current State: Where things stand now
- Important Files: Files that were read or modified
- Pending Work: Things still to be done
""";

    public static final String COMPACT_USER_PROMPT = """
Please summarize the conversation above, preserving all important context needed for continuing the work. Focus on:
- What was discussed and decided
- Files that were read or modified
- Current progress on tasks
- Any errors encountered and how they were resolved
""";

    public static final String PARTIAL_COMPACT_PROMPT = """
Summarize the conversation segments below, keeping only the most important context:

{segments}
""";

    /**
     * Get the compact system prompt.
     */
    public static String getCompactSystemPrompt() {
        return COMPACT_SYSTEM_PROMPT;
    }

    /**
     * Get the compact user prompt.
     */
    public static String getCompactUserPrompt() {
        return COMPACT_USER_PROMPT;
    }

    /**
     * Get partial compact prompt.
     */
    public static String getPartialCompactPrompt(String segments) {
        return PARTIAL_COMPACT_PROMPT.replace("{segments}", segments);
    }
}