/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GitHubApp constants.
 */
class GitHubAppTest {

    @Test
    @DisplayName("GitHubApp PR_TITLE")
    void prTitle() {
        assertEquals("Add Claude Code GitHub Workflow", GitHubApp.PR_TITLE);
    }

    @Test
    @DisplayName("GitHubApp GITHUB_ACTION_SETUP_DOCS_URL")
    void githubActionSetupDocsUrl() {
        assertEquals("https://github.com/anthropics/claude-code-action/blob/main/docs/setup.md", GitHubApp.GITHUB_ACTION_SETUP_DOCS_URL);
    }

    @Test
    @DisplayName("GitHubApp WORKFLOW_CONTENT not null")
    void workflowContentNotNull() {
        assertNotNull(GitHubApp.WORKFLOW_CONTENT);
    }

    @Test
    @DisplayName("GitHubApp WORKFLOW_CONTENT contains expected sections")
    void workflowContentContainsExpected() {
        assertTrue(GitHubApp.WORKFLOW_CONTENT.contains("name: Claude Code"));
        assertTrue(GitHubApp.WORKFLOW_CONTENT.contains("jobs:"));
        assertTrue(GitHubApp.WORKFLOW_CONTENT.contains("claude:"));
    }

    @Test
    @DisplayName("GitHubApp WORKFLOW_CONTENT contains triggers")
    void workflowContentContainsTriggers() {
        assertTrue(GitHubApp.WORKFLOW_CONTENT.contains("issue_comment:"));
        assertTrue(GitHubApp.WORKFLOW_CONTENT.contains("pull_request_review_comment:"));
        assertTrue(GitHubApp.WORKFLOW_CONTENT.contains("issues:"));
    }

    @Test
    @DisplayName("GitHubApp PR_BODY not null")
    void prBodyNotNull() {
        assertNotNull(GitHubApp.PR_BODY);
    }

    @Test
    @DisplayName("GitHubApp PR_BODY contains expected content")
    void prBodyContainsExpected() {
        assertTrue(GitHubApp.PR_BODY.contains("Claude Code"));
        assertTrue(GitHubApp.PR_BODY.contains("@claude"));
    }

    @Test
    @DisplayName("GitHubApp CODE_REVIEW_PLUGIN_WORKFLOW_CONTENT not null")
    void codeReviewPluginWorkflowContentNotNull() {
        assertNotNull(GitHubApp.CODE_REVIEW_PLUGIN_WORKFLOW_CONTENT);
    }

    @Test
    @DisplayName("GitHubApp CODE_REVIEW_PLUGIN_WORKFLOW_CONTENT contains expected")
    void codeReviewPluginWorkflowContentContainsExpected() {
        assertTrue(GitHubApp.CODE_REVIEW_PLUGIN_WORKFLOW_CONTENT.contains("name: Claude Code Review"));
        assertTrue(GitHubApp.CODE_REVIEW_PLUGIN_WORKFLOW_CONTENT.contains("pull_request:"));
    }

    @Test
    @DisplayName("GitHubApp all constants are non-empty strings")
    void allConstantsNonEmpty() {
        assertFalse(GitHubApp.PR_TITLE.isEmpty());
        assertFalse(GitHubApp.GITHUB_ACTION_SETUP_DOCS_URL.isEmpty());
        assertFalse(GitHubApp.WORKFLOW_CONTENT.isEmpty());
        assertFalse(GitHubApp.PR_BODY.isEmpty());
        assertFalse(GitHubApp.CODE_REVIEW_PLUGIN_WORKFLOW_CONTENT.isEmpty());
    }
}