/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/autoDream/consolidationPrompt
 */
package com.anthropic.claudecode.services.autodream;

import java.util.*;

/**
 * Consolidation prompt - Prompts for memory consolidation.
 */
public final class ConsolidationPrompt {

    /**
     * Build consolidation prompt for session memory merging.
     */
    public static String buildPrompt(List<String> sessionContents) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a memory consolidation assistant. Analyze the following sessions and extract durable memories.\n\n");

        sb.append("## Task\n");
        sb.append("Consolidate information from multiple sessions into persistent memories.\n\n");

        sb.append("## Guidelines\n");
        sb.append("1. Extract facts that should persist across sessions\n");
        sb.append("2. Merge duplicate information\n");
        sb.append("3. Update outdated information with newer data\n");
        sb.append("4. Prioritize user preferences and project context\n");
        sb.append("5. Ignore temporary debugging state and errors\n\n");

        sb.append("## Sessions\n");
        int sessionNum = 1;
        for (String content : sessionContents) {
            sb.append("\n### Session ").append(sessionNum++).append("\n");
            sb.append(content.length() > 1000 ? content.substring(0, 1000) + "..." : content);
            sb.append("\n");
        }

        sb.append("\n## Output Format\n");
        sb.append("Output each consolidated memory on a new line:\n");
        sb.append("- <memory statement>\n\n");

        sb.append("Example:\n");
        sb.append("- User prefers functional programming style\n");
        sb.append("- Project uses PostgreSQL database with Prisma ORM\n");
        sb.append("- API authentication uses JWT tokens with 1-hour expiry\n");

        return sb.toString();
    }

    /**
     * Build consolidation prompt with existing memories.
     */
    public static String buildPromptWithExisting(
        List<String> sessionContents,
        List<String> existingMemories
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a memory consolidation assistant. Update and extend existing memories with new session information.\n\n");

        sb.append("## Existing Memories\n");
        if (existingMemories.isEmpty()) {
            sb.append("(No existing memories)\n");
        } else {
            for (String memory : existingMemories) {
                sb.append("- ").append(memory).append("\n");
            }
        }

        sb.append("\n## New Session Content\n");
        for (String content : sessionContents) {
            sb.append(content.length() > 500 ? content.substring(0, 500) + "..." : content);
            sb.append("\n---\n");
        }

        sb.append("\n## Task\n");
        sb.append("1. Keep existing memories that are still accurate\n");
        sb.append("2. Update memories that have new information\n");
        sb.append("3. Add new memories for new information\n");
        sb.append("4. Remove memories that are now outdated\n\n");

        sb.append("## Output Format\n");
        sb.append("Output the complete updated memory list:\n");
        sb.append("- <memory statement>\n");

        return sb.toString();
    }

    /**
     * Parse consolidation result.
     */
    public static List<String> parseConsolidationResult(String output) {
        List<String> memories = new ArrayList<>();

        if (output == null || output.isEmpty()) {
            return memories;
        }

        for (String line : output.split("\n")) {
            line = line.trim();
            if (line.startsWith("- ")) {
                memories.add(line.substring(2));
            } else if (!line.isEmpty() && !line.startsWith("#")) {
                memories.add(line);
            }
        }

        return memories;
    }

    /**
     * Validate consolidated memories.
     */
    public static List<String> validateMemories(List<String> memories) {
        List<String> valid = new ArrayList<>();

        for (String memory : memories) {
            if (memory == null || memory.isEmpty()) {
                continue;
            }

            // Remove leading hyphens
            String cleaned = memory.startsWith("- ") ? memory.substring(2) : memory;

            // Skip too short memories
            if (cleaned.length() < 10) {
                continue;
            }

            // Skip memories that look like errors or temp state
            if (cleaned.toLowerCase().contains("error:") ||
                cleaned.toLowerCase().contains("failed") ||
                cleaned.contains("temporary")) {
                continue;
            }

            valid.add(cleaned);
        }

        return valid;
    }
}