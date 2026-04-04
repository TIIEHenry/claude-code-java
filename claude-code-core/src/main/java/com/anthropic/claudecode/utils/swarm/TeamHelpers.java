/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code team helpers for swarm
 */
package com.anthropic.claudecode.utils.swarm;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

import com.anthropic.claudecode.utils.EnvUtils;

/**
 * Team file management utilities for swarm coordination.
 */
public final class TeamHelpers {
    private TeamHelpers() {}

    private static final Pattern SLUG_PATTERN = Pattern.compile("[^a-zA-Z0-9-]");

    /**
     * Sanitize name for use in paths and identifiers.
     */
    public static String sanitizeName(String name) {
        return SLUG_PATTERN.matcher(name.toLowerCase()).replaceAll("-");
    }

    /**
     * Sanitize agent name (remove @ to prevent ambiguity).
     */
    public static String sanitizeAgentName(String name) {
        return name.replace("@", "-");
    }

    /**
     * Get team directory path.
     */
    public static Path getTeamDir(String teamName) {
        return Paths.get(EnvUtils.getTeamsDir()).resolve(sanitizeName(teamName));
    }

    /**
     * Get team file path.
     */
    public static Path getTeamFilePath(String teamName) {
        return getTeamDir(teamName).resolve("config.json");
    }

    /**
     * Check if team exists.
     */
    public static boolean teamExists(String teamName) {
        return Files.exists(getTeamFilePath(teamName));
    }

    /**
     * Team member record.
     */
    public record TeamMember(
            String agentId,
            String name,
            String agentType,
            String model,
            String prompt,
            String color,
            boolean planModeRequired,
            Instant joinedAt,
            String tmuxPaneId,
            String cwd,
            String worktreePath,
            String sessionId,
            List<String> subscriptions,
            String backendType,
            Boolean isActive,
            String mode
    ) {}

    /**
     * Team allowed path record.
     */
    public record TeamAllowedPath(
            String path,
            String toolName,
            String addedBy,
            Instant addedAt
    ) {}

    /**
     * Team file record.
     */
    public record TeamFile(
            String name,
            String description,
            Instant createdAt,
            String leadAgentId,
            String leadSessionId,
            List<String> hiddenPaneIds,
            List<TeamAllowedPath> teamAllowedPaths,
            List<TeamMember> members
    ) {
        public static TeamFile create(String name, String description, String leadAgentId) {
            return new TeamFile(
                    name,
                    description,
                    Instant.now(),
                    leadAgentId,
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
    }
}