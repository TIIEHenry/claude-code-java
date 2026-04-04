/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/skills
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Skills command - Manage and browse available skills.
 */
public final class SkillsCommand implements Command {
    @Override
    public String name() {
        return "skills";
    }

    @Override
    public String description() {
        return "Browse and manage available skills";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supportsNonInteractive() {
        return true;
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        if (args == null || args.trim().isEmpty()) {
            return CompletableFuture.completedFuture(listSkills());
        }

        String[] parts = args.trim().split("\\s+");
        String action = parts[0].toLowerCase();

        return CompletableFuture.completedFuture(switch (action) {
            case "list", "ls" -> listSkills();
            case "search", "find" -> {
                if (parts.length < 2) {
                    yield CommandResult.failure("Usage: skills search <query>");
                }
                yield searchSkills(String.join(" ", Arrays.copyOfRange(parts, 1, parts.length)));
            }
            case "info", "show" -> {
                if (parts.length < 2) {
                    yield CommandResult.failure("Usage: skills info <skill-name>");
                }
                yield showSkillInfo(parts[1]);
            }
            default -> listSkills();
        });
    }

    private CommandResult listSkills() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available Skills\n");
        sb.append("================\n\n");

        sb.append("No skills available.\n\n");
        sb.append("Skills are predefined prompts that help with common tasks.\n");
        sb.append("Configure skills in your CLAUDE.md or .claude/settings.json.\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult searchSkills(String query) {
        StringBuilder sb = new StringBuilder();
        sb.append("Search Results: ").append(query).append("\n");
        sb.append("=================================\n\n");

        sb.append("No matching skills found.\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult showSkillInfo(String skillName) {
        return CommandResult.failure("Skill not found: " + skillName);
    }
}