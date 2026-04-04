/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/extractMemories/prompts
 */
package com.anthropic.claudecode.services.extractmemories;

import java.util.*;

/**
 * Extract memories prompts - Prompts for memory extraction.
 */
public final class ExtractMemoriesPrompts {

    /**
     * Build extract auto-only prompt.
     */
    public static String buildExtractAutoOnlyPrompt(List<String> existingMemories) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a memory extraction assistant. Analyze the conversation and extract durable memories.\n\n");

        sb.append("## Task\n");
        sb.append("Extract important information from the conversation that should be remembered for future sessions.\n\n");

        sb.append("## Guidelines\n");
        sb.append("- Extract facts about the user's preferences, goals, and context\n");
        sb.append("- Extract technical decisions and their rationale\n");
        sb.append("- Extract project-specific knowledge and patterns\n");
        sb.append("- Ignore temporary state, errors, and ephemeral details\n");
        sb.append("- Each memory should be a single, concise statement\n\n");

        if (!existingMemories.isEmpty()) {
            sb.append("## Existing Memories\n");
            sb.append("Avoid duplicating these existing memories:\n");
            for (String memory : existingMemories) {
                sb.append("- ").append(memory).append("\n");
            }
            sb.append("\n");
        }

        sb.append("## Output Format\n");
        sb.append("Output each extracted memory on a new line, prefixed with '- '.\n");
        sb.append("Example:\n");
        sb.append("- User prefers using TypeScript over JavaScript\n");
        sb.append("- Project uses React with Next.js framework\n");

        return sb.toString();
    }

    /**
     * Build extract combined prompt (auto + session memories).
     */
    public static String buildExtractCombinedPrompt(
        List<String> autoMemories,
        List<String> sessionMemories
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a memory extraction assistant. Analyze the conversation and extract durable memories.\n\n");

        sb.append("## Task\n");
        sb.append("Extract memories from the conversation, considering both persistent (auto) and session-specific memories.\n\n");

        sb.append("## Memory Types\n");
        sb.append("- AUTO memories: Persistent across all sessions (preferences, context, decisions)\n");
        sb.append("- SESSION memories: Specific to this session (current task, temporary state)\n\n");

        if (!autoMemories.isEmpty()) {
            sb.append("## Existing Auto Memories\n");
            for (String memory : autoMemories) {
                sb.append("[AUTO] ").append(memory).append("\n");
            }
            sb.append("\n");
        }

        if (!sessionMemories.isEmpty()) {
            sb.append("## Existing Session Memories\n");
            for (String memory : sessionMemories) {
                sb.append("[SESSION] ").append(memory).append("\n");
            }
            sb.append("\n");
        }

        sb.append("## Output Format\n");
        sb.append("For each extracted memory, specify the type:\n");
        sb.append("- [AUTO] <memory> - for persistent memories\n");
        sb.append("- [SESSION] <memory> - for session-specific memories\n\n");

        sb.append("## Guidelines\n");
        sb.append("- Prefer AUTO for stable, reusable information\n");
        sb.append("- Use SESSION for task-specific, temporary information\n");
        sb.append("- Avoid duplicating existing memories\n");
        sb.append("- Keep memories concise and actionable\n");

        return sb.toString();
    }

    /**
     * Build consolidation prompt for memory merging.
     */
    public static String buildConsolidationPrompt(List<String> memories) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a memory consolidation assistant. Merge and deduplicate memories.\n\n");

        sb.append("## Task\n");
        sb.append("Consolidate the following memories by:\n");
        sb.append("1. Removing duplicates\n");
        sb.append("2. Merging related memories\n");
        sb.append("3. Updating outdated information\n");
        sb.append("4. Prioritizing recent information\n\n");

        sb.append("## Input Memories\n");
        for (String memory : memories) {
            sb.append("- ").append(memory).append("\n");
        }

        sb.append("\n## Output Format\n");
        sb.append("Output the consolidated memories, one per line.\n");

        return sb.toString();
    }

    /**
     * Parse extracted memories from model output.
     */
    public static List<String> parseExtractedMemories(String output) {
        List<String> memories = new ArrayList<>();

        if (output == null || output.isEmpty()) {
            return memories;
        }

        for (String line : output.split("\n")) {
            line = line.trim();
            if (line.startsWith("- ")) {
                memories.add(line.substring(2));
            } else if (line.startsWith("[AUTO] ")) {
                memories.add(line.substring(7));
            } else if (line.startsWith("[SESSION] ")) {
                memories.add(line.substring(10));
            } else if (!line.isEmpty()) {
                memories.add(line);
            }
        }

        return memories;
    }
}