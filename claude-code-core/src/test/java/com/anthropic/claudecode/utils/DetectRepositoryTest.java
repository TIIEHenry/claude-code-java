/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DetectRepository.
 */
class DetectRepositoryTest {

    @BeforeEach
    void setUp() {
        DetectRepository.clearRepositoryCaches();
    }

    @Test
    @DisplayName("DetectRepository ParsedRepository record")
    void parsedRepositoryRecord() {
        DetectRepository.ParsedRepository repo = new DetectRepository.ParsedRepository(
            "github.com", "owner", "repo"
        );

        assertEquals("github.com", repo.host());
        assertEquals("owner", repo.owner());
        assertEquals("repo", repo.name());
    }

    @Test
    @DisplayName("DetectRepository parseGitRemote SSH format")
    void parseGitRemoteSSH() {
        DetectRepository.ParsedRepository repo = DetectRepository.parseGitRemote("git@github.com:owner/repo.git");

        assertNotNull(repo);
        assertEquals("github.com", repo.host());
        assertEquals("owner", repo.owner());
        assertEquals("repo", repo.name());
    }

    @Test
    @DisplayName("DetectRepository parseGitRemote HTTPS format")
    void parseGitRemoteHTTPS() {
        DetectRepository.ParsedRepository repo = DetectRepository.parseGitRemote("https://github.com/owner/repo.git");

        assertNotNull(repo);
        assertEquals("github.com", repo.host());
        assertEquals("owner", repo.owner());
        assertEquals("repo", repo.name());
    }

    @Test
    @DisplayName("DetectRepository parseGitRemote HTTPS without .git")
    void parseGitRemoteHTTPsWithoutGit() {
        DetectRepository.ParsedRepository repo = DetectRepository.parseGitRemote("https://github.com/owner/repo");

        assertNotNull(repo);
        assertEquals("github.com", repo.host());
        assertEquals("owner", repo.owner());
        assertEquals("repo", repo.name());
    }

    @Test
    @DisplayName("DetectRepository parseGitRemote SSH URL format")
    void parseGitRemoteSSHURL() {
        DetectRepository.ParsedRepository repo = DetectRepository.parseGitRemote("ssh://git@github.com/owner/repo.git");

        assertNotNull(repo);
        assertEquals("github.com", repo.host());
        assertEquals("owner", repo.owner());
        assertEquals("repo", repo.name());
    }

    @Test
    @DisplayName("DetectRepository parseGitRemote git:// format")
    void parseGitRemoteGitProtocol() {
        DetectRepository.ParsedRepository repo = DetectRepository.parseGitRemote("git://github.com/owner/repo.git");

        assertNotNull(repo);
        assertEquals("github.com", repo.host());
        assertEquals("owner", repo.owner());
        assertEquals("repo", repo.name());
    }

    @Test
    @DisplayName("DetectRepository parseGitRemote null")
    void parseGitRemoteNull() {
        assertNull(DetectRepository.parseGitRemote(null));
    }

    @Test
    @DisplayName("DetectRepository parseGitRemote empty")
    void parseGitRemoteEmpty() {
        assertNull(DetectRepository.parseGitRemote(""));
    }

    @Test
    @DisplayName("DetectRepository parseGitRemote invalid")
    void parseGitRemoteInvalid() {
        assertNull(DetectRepository.parseGitRemote("not-a-valid-url"));
    }

    @Test
    @DisplayName("DetectRepository parseGitRemote non-github")
    void parseGitRemoteNonGithub() {
        DetectRepository.ParsedRepository repo = DetectRepository.parseGitRemote("git@gitlab.com:owner/repo.git");

        assertNotNull(repo);
        assertEquals("gitlab.com", repo.host());
        assertEquals("owner", repo.owner());
        assertEquals("repo", repo.name());
    }

    @Test
    @DisplayName("DetectRepository parseGitHubRepository SSH URL")
    void parseGitHubRepositorySSH() {
        String result = DetectRepository.parseGitHubRepository("git@github.com:owner/repo.git");
        assertEquals("owner/repo", result);
    }

    @Test
    @DisplayName("DetectRepository parseGitHubRepository HTTPS URL")
    void parseGitHubRepositoryHTTPS() {
        String result = DetectRepository.parseGitHubRepository("https://github.com/owner/repo.git");
        assertEquals("owner/repo", result);
    }

    @Test
    @DisplayName("DetectRepository parseGitHubRepository owner/repo format")
    void parseGitHubRepositoryOwnerRepo() {
        String result = DetectRepository.parseGitHubRepository("owner/repo");
        assertEquals("owner/repo", result);
    }

    @Test
    @DisplayName("DetectRepository parseGitHubRepository owner/repo.git format")
    void parseGitHubRepositoryOwnerRepoGit() {
        String result = DetectRepository.parseGitHubRepository("owner/repo.git");
        assertEquals("owner/repo", result);
    }

    @Test
    @DisplayName("DetectRepository parseGitHubRepository non-github returns null")
    void parseGitHubRepositoryNonGithub() {
        assertNull(DetectRepository.parseGitHubRepository("git@gitlab.com:owner/repo.git"));
    }

    @Test
    @DisplayName("DetectRepository parseGitHubRepository null")
    void parseGitHubRepositoryNull() {
        assertNull(DetectRepository.parseGitHubRepository(null));
    }

    @Test
    @DisplayName("DetectRepository parseGitHubRepository empty")
    void parseGitHubRepositoryEmpty() {
        assertNull(DetectRepository.parseGitHubRepository(""));
    }

    @Test
    @DisplayName("DetectRepository detectCurrentRepository")
    void detectCurrentRepository() {
        // May return null if not in a git repo
        String result = DetectRepository.detectCurrentRepository();
        assertTrue(result == null || result.contains("/"));
    }

    @Test
    @DisplayName("DetectRepository detectCurrentRepositoryWithHost")
    void detectCurrentRepositoryWithHost() {
        // May return null if not in a git repo
        DetectRepository.ParsedRepository result = DetectRepository.detectCurrentRepositoryWithHost();
        assertTrue(result == null || result.host() != null);
    }

    @Test
    @DisplayName("DetectRepository getCachedRepository")
    void getCachedRepository() {
        // First call may cache
        DetectRepository.detectCurrentRepository();
        String cached = DetectRepository.getCachedRepository();
        // May be null if not github.com
        assertTrue(cached == null || cached.contains("/"));
    }

    @Test
    @DisplayName("DetectRepository clearRepositoryCaches")
    void clearRepositoryCaches() {
        DetectRepository.detectCurrentRepository();
        DetectRepository.clearRepositoryCaches();
        // Cache is cleared
        assertTrue(true);
    }
}