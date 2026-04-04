/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GitUtils.
 */
class GitUtilsTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        GitUtils.clearCache();
    }

    @Test
    @DisplayName("GitUtils findGitRoot null path")
    void findGitRootNull() {
        assertNull(GitUtils.findGitRoot(null));
    }

    @Test
    @DisplayName("GitUtils findGitRoot empty path")
    void findGitRootEmpty() {
        assertNull(GitUtils.findGitRoot(""));
    }

    @Test
    @DisplayName("GitUtils findGitRoot non-git directory")
    void findGitRootNonGit() {
        assertNull(GitUtils.findGitRoot(tempDir.toString()));
    }

    @Test
    @DisplayName("GitUtils findGitRoot finds root")
    void findGitRootFindsRoot() throws Exception {
        Path gitDir = tempDir.resolve(".git");
        Files.createDirectories(gitDir);

        String result = GitUtils.findGitRoot(tempDir.toString());
        assertEquals(tempDir.toAbsolutePath().normalize().toString(), result);
    }

    @Test
    @DisplayName("GitUtils findGitRoot nested directory")
    void findGitRootNested() throws Exception {
        Path gitDir = tempDir.resolve(".git");
        Files.createDirectories(gitDir);

        Path nested = tempDir.resolve("a").resolve("b").resolve("c");
        Files.createDirectories(nested);

        String result = GitUtils.findGitRoot(nested.toString());
        assertEquals(tempDir.toAbsolutePath().normalize().toString(), result);
    }

    @Test
    @DisplayName("GitUtils findGitRoot caches result")
    void findGitRootCaches() throws Exception {
        Path gitDir = tempDir.resolve(".git");
        Files.createDirectories(gitDir);

        String result1 = GitUtils.findGitRoot(tempDir.toString());
        String result2 = GitUtils.findGitRoot(tempDir.toString());

        assertEquals(result1, result2);
    }

    @Test
    @DisplayName("GitUtils isGitRepo false for non-git")
    void isGitRepoFalse() {
        assertFalse(GitUtils.isGitRepo(tempDir.toString()));
    }

    @Test
    @DisplayName("GitUtils isGitRepo true for git")
    void isGitRepoTrue() throws Exception {
        Path gitDir = tempDir.resolve(".git");
        Files.createDirectories(gitDir);

        assertTrue(GitUtils.isGitRepo(tempDir.toString()));
    }

    @Test
    @DisplayName("GitUtils isInGitRepo alias")
    void isInGitRepoAlias() throws Exception {
        Path gitDir = tempDir.resolve(".git");
        Files.createDirectories(gitDir);

        assertTrue(GitUtils.isInGitRepo(tempDir.toString()));
    }

    @Test
    @DisplayName("GitUtils getCurrentBranch null path")
    void getCurrentBranchNull() {
        assertNull(GitUtils.getCurrentBranch(null));
    }

    @Test
    @DisplayName("GitUtils getCurrentBranch non-git")
    void getCurrentBranchNonGit() {
        assertNull(GitUtils.getCurrentBranch(tempDir.toString()));
    }

    @Test
    @DisplayName("GitUtils getRemoteUrl null path")
    void getRemoteUrlNull() {
        assertNull(GitUtils.getRemoteUrl(null));
    }

    @Test
    @DisplayName("GitUtils getRemoteUrl non-git")
    void getRemoteUrlNonGit() {
        assertNull(GitUtils.getRemoteUrl(tempDir.toString()));
    }

    @Test
    @DisplayName("GitUtils getRemoteUrl with remote")
    void getRemoteUrlWithRemote() {
        assertNull(GitUtils.getRemoteUrl(tempDir.toString(), "upstream"));
    }

    @Test
    @DisplayName("GitUtils getRepoRemoteHash non-git")
    void getRepoRemoteHashNonGit() {
        assertNull(GitUtils.getRepoRemoteHash(tempDir.toString()));
    }

    @Test
    @DisplayName("GitUtils getDefaultBranch null path")
    void getDefaultBranchNull() {
        assertNull(GitUtils.getDefaultBranch(null));
    }

    @Test
    @DisplayName("GitUtils getDefaultBranch non-git")
    void getDefaultBranchNonGit() {
        assertNull(GitUtils.getDefaultBranch(tempDir.toString()));
    }

    @Test
    @DisplayName("GitUtils getDefaultBranch with main")
    void getDefaultBranchMain() throws Exception {
        Path gitDir = tempDir.resolve(".git");
        Path refsHeads = gitDir.resolve("refs").resolve("heads");
        Files.createDirectories(refsHeads);
        Files.writeString(refsHeads.resolve("main"), "");

        assertEquals("main", GitUtils.getDefaultBranch(tempDir.toString()));
    }

    @Test
    @DisplayName("GitUtils getDefaultBranch with master")
    void getDefaultBranchMaster() throws Exception {
        Path gitDir = tempDir.resolve(".git");
        Path refsHeads = gitDir.resolve("refs").resolve("heads");
        Files.createDirectories(refsHeads);
        Files.writeString(refsHeads.resolve("master"), "");

        assertEquals("master", GitUtils.getDefaultBranch(tempDir.toString()));
    }

    @Test
    @DisplayName("GitUtils clearCache")
    void clearCache() {
        GitUtils.clearCache();
        // Should not throw
        assertTrue(true);
    }

    @Test
    @DisplayName("GitUtils findCanonicalGitRoot non-git")
    void findCanonicalGitRootNonGit() {
        assertNull(GitUtils.findCanonicalGitRoot(tempDir.toString()));
    }

    @Test
    @DisplayName("GitUtils findCanonicalGitRoot git repo")
    void findCanonicalGitRootGit() throws Exception {
        Path gitDir = tempDir.resolve(".git");
        Files.createDirectories(gitDir);

        String result = GitUtils.findCanonicalGitRoot(tempDir.toString());
        assertEquals(tempDir.toAbsolutePath().normalize().toString(), result);
    }
}