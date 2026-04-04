/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/config/projectConfig
 */
package com.anthropic.claudecode.services.config;

import java.util.*;
import java.nio.file.*;

/**
 * Project config - Project-specific configuration.
 */
public final class ProjectConfig {
    private final Path projectPath;
    private final ConfigService projectConfig;
    private volatile boolean loaded = false;

    /**
     * Project config keys.
     */
    public static final class Keys {
        public static final String PROJECT_NAME = "project.name";
        public static final String PROJECT_DESCRIPTION = "project.description";
        public static final String PROJECT_LANGUAGE = "project.language";
        public static final String BUILD_COMMAND = "project.buildCommand";
        public static final String TEST_COMMAND = "project.testCommand";
        public static final String RUN_COMMAND = "project.runCommand";
        public static final String GIT_IGNORE_PATTERNS = "project.gitIgnorePatterns";
        public static final String INCLUDED_DIRS = "project.includedDirs";
        public static final String EXCLUDED_DIRS = "project.excludedDirs";
        public static final String MAX_FILE_SIZE = "project.maxFileSize";
        public static final String ENABLE_INDEXING = "project.enableIndexing";
        public static final String ENABLE_TESTS = "project.enableTests";
        public static final String WORKSPACE_ROOTS = "project.workspaceRoots";
    }

    /**
     * Create project config.
     */
    public ProjectConfig(Path projectPath) {
        this.projectPath = projectPath;
        this.projectConfig = new ConfigService(
            projectPath.resolve(".claude").resolve("project.conf")
        );
        this.loaded = true;
    }

    /**
     * Get project path.
     */
    public Path getProjectPath() {
        return projectPath;
    }

    /**
     * Get project name.
     */
    public String getProjectName() {
        String name = projectConfig.getString(Keys.PROJECT_NAME, null);
        if (name == null) {
            name = projectPath.getFileName().toString();
        }
        return name;
    }

    /**
     * Set project name.
     */
    public void setProjectName(String name) {
        projectConfig.set(Keys.PROJECT_NAME, name);
    }

    /**
     * Get project description.
     */
    public String getDescription() {
        return projectConfig.getString(Keys.PROJECT_DESCRIPTION, "");
    }

    /**
     * Set description.
     */
    public void setDescription(String description) {
        projectConfig.set(Keys.PROJECT_DESCRIPTION, description);
    }

    /**
     * Get primary language.
     */
    public String getLanguage() {
        return projectConfig.getString(Keys.PROJECT_LANGUAGE, detectLanguage());
    }

    /**
     * Detect language from project files.
     */
    private String detectLanguage() {
        // Check common project markers
        if (Files.exists(projectPath.resolve("pom.xml"))) return "java";
        if (Files.exists(projectPath.resolve("build.gradle"))) return "java";
        if (Files.exists(projectPath.resolve("package.json"))) return "javascript";
        if (Files.exists(projectPath.resolve("requirements.txt"))) return "python";
        if (Files.exists(projectPath.resolve("Cargo.toml"))) return "rust";
        if (Files.exists(projectPath.resolve("go.mod"))) return "go";
        return "unknown";
    }

    /**
     * Set language.
     */
    public void setLanguage(String language) {
        projectConfig.set(Keys.PROJECT_LANGUAGE, language);
    }

    /**
     * Get build command.
     */
    public String getBuildCommand() {
        return projectConfig.getString(Keys.BUILD_COMMAND, "");
    }

    /**
     * Set build command.
     */
    public void setBuildCommand(String command) {
        projectConfig.set(Keys.BUILD_COMMAND, command);
    }

    /**
     * Get test command.
     */
    public String getTestCommand() {
        return projectConfig.getString(Keys.TEST_COMMAND, "");
    }

    /**
     * Set test command.
     */
    public void setTestCommand(String command) {
        projectConfig.set(Keys.TEST_COMMAND, command);
    }

    /**
     * Get run command.
     */
    public String getRunCommand() {
        return projectConfig.getString(Keys.RUN_COMMAND, "");
    }

    /**
     * Set run command.
     */
    public void setRunCommand(String command) {
        projectConfig.set(Keys.RUN_COMMAND, command);
    }

    /**
     * Get excluded directories.
     */
    public List<String> getExcludedDirs() {
        String dirs = projectConfig.getString(Keys.EXCLUDED_DIRS, "node_modules,.git,target,build");
        return Arrays.asList(dirs.split(","));
    }

    /**
     * Set excluded directories.
     */
    public void setExcludedDirs(List<String> dirs) {
        projectConfig.set(Keys.EXCLUDED_DIRS, String.join(",", dirs));
    }

    /**
     * Get included directories.
     */
    public List<String> getIncludedDirs() {
        String dirs = projectConfig.getString(Keys.INCLUDED_DIRS, "src,lib,app");
        return Arrays.asList(dirs.split(","));
    }

    /**
     * Set included directories.
     */
    public void setIncludedDirs(List<String> dirs) {
        projectConfig.set(Keys.INCLUDED_DIRS, String.join(",", dirs));
    }

    /**
     * Get max file size.
     */
    public int getMaxFileSize() {
        return projectConfig.getInt(Keys.MAX_FILE_SIZE, 1000000); // 1MB
    }

    /**
     * Set max file size.
     */
    public void setMaxFileSize(int maxSize) {
        projectConfig.set(Keys.MAX_FILE_SIZE, maxSize);
    }

    /**
     * Check indexing enabled.
     */
    public boolean isIndexingEnabled() {
        return projectConfig.getBoolean(Keys.ENABLE_INDEXING, true);
    }

    /**
     * Set indexing enabled.
     */
    public void setIndexingEnabled(boolean enabled) {
        projectConfig.set(Keys.ENABLE_INDEXING, enabled);
    }

    /**
     * Check tests enabled.
     */
    public boolean isTestsEnabled() {
        return projectConfig.getBoolean(Keys.ENABLE_TESTS, true);
    }

    /**
     * Set tests enabled.
     */
    public void setTestsEnabled(boolean enabled) {
        projectConfig.set(Keys.ENABLE_TESTS, enabled);
    }

    /**
     * Project info record.
     */
    public record ProjectInfo(
        String name,
        String description,
        String language,
        String path,
        List<String> excludedDirs,
        List<String> includedDirs
    ) {
        public String formatSummary() {
            return String.format(
                "%s [%s] at %s",
                name,
                language,
                path
            );
        }
    }

    /**
     * Get project info.
     */
    public ProjectInfo getProjectInfo() {
        return new ProjectInfo(
            getProjectName(),
            getDescription(),
            getLanguage(),
            projectPath.toString(),
            getExcludedDirs(),
            getIncludedDirs()
        );
    }

    /**
     * Save config.
     */
    public void save() {
        projectConfig.saveConfig();
    }

    /**
     * Check if loaded.
     */
    public boolean isLoaded() {
        return loaded;
    }
}