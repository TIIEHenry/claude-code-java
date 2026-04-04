/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/init.ts
 */
package com.anthropic.claudecode.commands;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Init command - Initialize a new CLAUDE.md file with codebase documentation.
 */
public final class InitCommand implements Command {
    @Override
    public String name() {
        return "init";
    }

    @Override
    public String description() {
        return "Initialize a new CLAUDE.md file with codebase documentation";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supportsNonInteractive() {
        return false;
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        Path projectRoot = context.workingDirectory();
        Path claudeMdPath = projectRoot.resolve("CLAUDE.md");

        if (claudeMdPath.toFile().exists()) {
            return CompletableFuture.completedFuture(
                CommandResult.success("CLAUDE.md already exists.")
            );
        }

        try {
            String content = generateClaudeMdContent(projectRoot.toString());
            Files.writeString(claudeMdPath, content);
            return CompletableFuture.completedFuture(
                CommandResult.success("Created CLAUDE.md at " + claudeMdPath)
            );
        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                CommandResult.failure("Failed to create CLAUDE.md: " + e.getMessage())
            );
        }
    }

    private String generateClaudeMdContent(String projectRoot) {
        StringBuilder sb = new StringBuilder();
        sb.append("# CLAUDE.md\n\n");
        sb.append("This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.\n\n");

        String projectType = detectProjectType(projectRoot);
        if (projectType != null) {
            sb.append("## Project Type\n\n");
            sb.append("This is a ").append(projectType).append(" project.\n\n");
        }

        sb.append("## Development Commands\n\n");
        sb.append(getBuildCommands(projectType));
        sb.append("\n## Architecture\n\n");
        sb.append("[Describe the high-level architecture here]\n");

        return sb.toString();
    }

    private String detectProjectType(String projectRoot) {
        Path root = Path.of(projectRoot);
        if (root.resolve("pom.xml").toFile().exists()) return "Maven Java";
        if (root.resolve("build.gradle").toFile().exists()) return "Gradle Java";
        if (root.resolve("package.json").toFile().exists()) return "Node.js";
        if (root.resolve("Cargo.toml").toFile().exists()) return "Rust";
        if (root.resolve("go.mod").toFile().exists()) return "Go";
        if (root.resolve("pyproject.toml").toFile().exists()) return "Python";
        return null;
    }

    private String getBuildCommands(String projectType) {
        if (projectType == null) return "- Build: [specify]\n- Test: [specify]\n";
        return switch (projectType) {
            case "Maven Java" -> "- Build: mvn compile\n- Test: mvn test\n";
            case "Gradle Java" -> "- Build: gradle build\n- Test: gradle test\n";
            case "Node.js" -> "- Install: npm install\n- Build: npm run build\n- Test: npm test\n";
            case "Rust" -> "- Build: cargo build\n- Test: cargo test\n";
            case "Go" -> "- Build: go build ./...\n- Test: go test ./...\n";
            case "Python" -> "- Test: pytest\n";
            default -> "- Build: [specify]\n- Test: [specify]\n";
        };
    }
}