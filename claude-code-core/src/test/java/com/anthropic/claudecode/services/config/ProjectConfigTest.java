/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ProjectConfig.
 */
class ProjectConfigTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("ProjectConfig constructor initializes correctly")
    void constructor() {
        ProjectConfig config = new ProjectConfig(tempDir);

        assertEquals(tempDir, config.getProjectPath());
        assertTrue(config.isLoaded());
    }

    @Test
    @DisplayName("ProjectConfig getProjectName returns directory name by default")
    void getProjectNameDefault() {
        ProjectConfig config = new ProjectConfig(tempDir);

        String name = config.getProjectName();
        assertNotNull(name);
        assertEquals(tempDir.getFileName().toString(), name);
    }

    @Test
    @DisplayName("ProjectConfig setProjectName stores name")
    void setProjectName() {
        ProjectConfig config = new ProjectConfig(tempDir);

        config.setProjectName("MyProject");

        assertEquals("MyProject", config.getProjectName());
    }

    @Test
    @DisplayName("ProjectConfig getDescription returns empty by default")
    void getDescriptionDefault() {
        ProjectConfig config = new ProjectConfig(tempDir);

        assertEquals("", config.getDescription());
    }

    @Test
    @DisplayName("ProjectConfig setDescription stores description")
    void setDescription() {
        ProjectConfig config = new ProjectConfig(tempDir);

        config.setDescription("Test project description");

        assertEquals("Test project description", config.getDescription());
    }

    @Test
    @DisplayName("ProjectConfig getLanguage detects java from pom.xml")
    void getLanguageJava() throws Exception {
        // Create pom.xml
        java.nio.file.Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");

        ProjectConfig config = new ProjectConfig(tempDir);

        assertEquals("java", config.getLanguage());
    }

    @Test
    @DisplayName("ProjectConfig getLanguage detects javascript from package.json")
    void getLanguageJavascript() throws Exception {
        // Create package.json
        java.nio.file.Files.writeString(tempDir.resolve("package.json"), "{}");

        ProjectConfig config = new ProjectConfig(tempDir);

        assertEquals("javascript", config.getLanguage());
    }

    @Test
    @DisplayName("ProjectConfig getLanguage detects python from requirements.txt")
    void getLanguagePython() throws Exception {
        // Create requirements.txt
        java.nio.file.Files.writeString(tempDir.resolve("requirements.txt"), "requests");

        ProjectConfig config = new ProjectConfig(tempDir);

        assertEquals("python", config.getLanguage());
    }

    @Test
    @DisplayName("ProjectConfig getLanguage returns unknown without markers")
    void getLanguageUnknown() {
        ProjectConfig config = new ProjectConfig(tempDir);

        assertEquals("unknown", config.getLanguage());
    }

    @Test
    @DisplayName("ProjectConfig setLanguage overrides detection")
    void setLanguage() throws Exception {
        // Create pom.xml (would detect java)
        java.nio.file.Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");

        ProjectConfig config = new ProjectConfig(tempDir);
        config.setLanguage("kotlin");

        assertEquals("kotlin", config.getLanguage());
    }

    @Test
    @DisplayName("ProjectConfig getBuildCommand returns empty by default")
    void getBuildCommandDefault() {
        ProjectConfig config = new ProjectConfig(tempDir);

        assertEquals("", config.getBuildCommand());
    }

    @Test
    @DisplayName("ProjectConfig setBuildCommand stores command")
    void setBuildCommand() {
        ProjectConfig config = new ProjectConfig(tempDir);

        config.setBuildCommand("mvn clean install");

        assertEquals("mvn clean install", config.getBuildCommand());
    }

    @Test
    @DisplayName("ProjectConfig getTestCommand returns empty by default")
    void getTestCommandDefault() {
        ProjectConfig config = new ProjectConfig(tempDir);

        assertEquals("", config.getTestCommand());
    }

    @Test
    @DisplayName("ProjectConfig setTestCommand stores command")
    void setTestCommand() {
        ProjectConfig config = new ProjectConfig(tempDir);

        config.setTestCommand("mvn test");

        assertEquals("mvn test", config.getTestCommand());
    }

    @Test
    @DisplayName("ProjectConfig getRunCommand returns empty by default")
    void getRunCommandDefault() {
        ProjectConfig config = new ProjectConfig(tempDir);

        assertEquals("", config.getRunCommand());
    }

    @Test
    @DisplayName("ProjectConfig setRunCommand stores command")
    void setRunCommand() {
        ProjectConfig config = new ProjectConfig(tempDir);

        config.setRunCommand("java -jar app.jar");

        assertEquals("java -jar app.jar", config.getRunCommand());
    }

    @Test
    @DisplayName("ProjectConfig getExcludedDirs returns default list")
    void getExcludedDirsDefault() {
        ProjectConfig config = new ProjectConfig(tempDir);

        var dirs = config.getExcludedDirs();

        assertTrue(dirs.contains("node_modules"));
        assertTrue(dirs.contains(".git"));
        assertTrue(dirs.contains("target"));
        assertTrue(dirs.contains("build"));
    }

    @Test
    @DisplayName("ProjectConfig setExcludedDirs stores list")
    void setExcludedDirs() {
        ProjectConfig config = new ProjectConfig(tempDir);

        config.setExcludedDirs(java.util.List.of("custom", "excludes"));

        var dirs = config.getExcludedDirs();
        assertTrue(dirs.contains("custom"));
        assertTrue(dirs.contains("excludes"));
    }

    @Test
    @DisplayName("ProjectConfig getIncludedDirs returns default list")
    void getIncludedDirsDefault() {
        ProjectConfig config = new ProjectConfig(tempDir);

        var dirs = config.getIncludedDirs();

        assertTrue(dirs.contains("src"));
        assertTrue(dirs.contains("lib"));
        assertTrue(dirs.contains("app"));
    }

    @Test
    @DisplayName("ProjectConfig setIncludedDirs stores list")
    void setIncludedDirs() {
        ProjectConfig config = new ProjectConfig(tempDir);

        config.setIncludedDirs(java.util.List.of("source", "main"));

        var dirs = config.getIncludedDirs();
        assertTrue(dirs.contains("source"));
        assertTrue(dirs.contains("main"));
    }

    @Test
    @DisplayName("ProjectConfig getMaxFileSize returns default")
    void getMaxFileSizeDefault() {
        ProjectConfig config = new ProjectConfig(tempDir);

        assertEquals(1000000, config.getMaxFileSize());
    }

    @Test
    @DisplayName("ProjectConfig setMaxFileSize stores value")
    void setMaxFileSize() {
        ProjectConfig config = new ProjectConfig(tempDir);

        config.setMaxFileSize(500000);

        assertEquals(500000, config.getMaxFileSize());
    }

    @Test
    @DisplayName("ProjectConfig isIndexingEnabled returns true by default")
    void isIndexingEnabledDefault() {
        ProjectConfig config = new ProjectConfig(tempDir);

        assertTrue(config.isIndexingEnabled());
    }

    @Test
    @DisplayName("ProjectConfig setIndexingEnabled stores value")
    void setIndexingEnabled() {
        ProjectConfig config = new ProjectConfig(tempDir);

        config.setIndexingEnabled(false);

        assertFalse(config.isIndexingEnabled());
    }

    @Test
    @DisplayName("ProjectConfig isTestsEnabled returns true by default")
    void isTestsEnabledDefault() {
        ProjectConfig config = new ProjectConfig(tempDir);

        assertTrue(config.isTestsEnabled());
    }

    @Test
    @DisplayName("ProjectConfig setTestsEnabled stores value")
    void setTestsEnabled() {
        ProjectConfig config = new ProjectConfig(tempDir);

        config.setTestsEnabled(false);

        assertFalse(config.isTestsEnabled());
    }

    @Test
    @DisplayName("ProjectConfig ProjectInfo record")
    void projectInfoRecord() {
        ProjectConfig config = new ProjectConfig(tempDir);
        config.setProjectName("TestProject");
        config.setDescription("Description");
        config.setLanguage("java");

        ProjectConfig.ProjectInfo info = config.getProjectInfo();

        assertEquals("TestProject", info.name());
        assertEquals("Description", info.description());
        assertEquals("java", info.language());
        assertEquals(tempDir.toString(), info.path());
    }

    @Test
    @DisplayName("ProjectConfig ProjectInfo formatSummary")
    void projectInfoFormatSummary() {
        ProjectConfig.ProjectInfo info = new ProjectConfig.ProjectInfo(
            "MyProject", "A test project", "java", "/path/to/project",
            java.util.List.of("target"), java.util.List.of("src")
        );

        String summary = info.formatSummary();

        assertTrue(summary.contains("MyProject"));
        assertTrue(summary.contains("java"));
        assertTrue(summary.contains("/path/to/project"));
    }
}