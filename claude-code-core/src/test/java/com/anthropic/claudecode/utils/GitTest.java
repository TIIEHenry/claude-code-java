/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Git.
 */
class GitTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Git gitExe returns git")
    void gitExe() {
        assertEquals("git", Git.gitExe());
    }

    @Test
    @DisplayName("Git findGitRoot null for non-git dir")
    void findGitRootNonGit() {
        Path result = Git.findGitRoot(tempDir);
        assertNull(result);
    }

    @Test
    @DisplayName("Git findGitRoot finds git root")
    void findGitRootFindsRoot() throws Exception {
        // Create .git directory
        Path gitDir = tempDir.resolve(".git");
        Files.createDirectories(gitDir);

        Path subDir = tempDir.resolve("subdir").resolve("nested");
        Files.createDirectories(subDir);

        Path result = Git.findGitRoot(subDir);
        assertEquals(tempDir.toAbsolutePath().normalize(), result);
    }

    @Test
    @DisplayName("Git getRemoteUrlForDir null for non-git dir")
    void getRemoteUrlForDirNonGit() {
        assertNull(Git.getRemoteUrlForDir(tempDir.toString()));
    }

    @Test
    @DisplayName("Git isInGitRepo false for non-git dir")
    void isInGitRepoNonGit() {
        assertFalse(Git.isInGitRepo(tempDir));
    }

    @Test
    @DisplayName("Git isInGitRepo true for git dir")
    void isInGitRepoGit() throws Exception {
        Path gitDir = tempDir.resolve(".git");
        Files.createDirectories(gitDir);

        assertTrue(Git.isInGitRepo(tempDir));
    }

    @Test
    @DisplayName("Git getIsGit returns boolean")
    void getIsGit() throws Exception {
        CompletableFuture<Boolean> future = Git.getIsGit();
        Boolean result = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("Git getBranch returns string or null")
    void getBranch() throws Exception {
        CompletableFuture<String> future = Git.getBranch();
        String result = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
        // May be null if not in a git repo
        assertTrue(result == null || result.length() > 0);
    }

    @Test
    @DisplayName("Git getDefaultBranch returns string")
    void getDefaultBranch() throws Exception {
        CompletableFuture<String> future = Git.getDefaultBranch();
        String result = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertNotNull(result);
        assertTrue(result.equals("main") || result.equals("master"));
    }

    @Test
    @DisplayName("Git getCurrentBranch returns string or null")
    void getCurrentBranch() throws Exception {
        CompletableFuture<String> future = Git.getCurrentBranch(tempDir);
        String result = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
        // May be null if not a git repo
        assertTrue(result == null || result.length() > 0);
    }
}