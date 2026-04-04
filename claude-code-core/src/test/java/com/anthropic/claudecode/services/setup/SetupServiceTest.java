/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.setup;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Tests for SetupService.
 */
@DisplayName("SetupService Tests")
class SetupServiceTest {

    @Test
    @DisplayName("SetupService.SetupOptions creates with defaults")
    void setupOptionsCreatesWithDefaults() {
        SetupService.SetupOptions options = new SetupService.SetupOptions("/tmp");

        assertEquals("/tmp", options.cwd());
        assertEquals(SetupService.PermissionMode.DEFAULT, options.permissionMode());
        assertFalse(options.allowDangerouslySkipPermissions());
        assertFalse(options.worktreeEnabled());
        assertNull(options.worktreeName());
        assertFalse(options.tmuxEnabled());
        assertNull(options.customSessionId());
        assertNull(options.worktreePRNumber());
        assertNull(options.messagingSocketPath());
    }

    @Test
    @DisplayName("SetupService.SetupOptions with all parameters")
    void setupOptionsWithAllParameters() {
        SetupService.SetupOptions options = new SetupService.SetupOptions(
            "/project",
            SetupService.PermissionMode.ACCEPT_EDITS,
            false,
            true,
            "feature-branch",
            true,
            "custom-session",
            123,
            "/tmp/socket"
        );

        assertEquals("/project", options.cwd());
        assertEquals(SetupService.PermissionMode.ACCEPT_EDITS, options.permissionMode());
        assertFalse(options.allowDangerouslySkipPermissions());
        assertTrue(options.worktreeEnabled());
        assertEquals("feature-branch", options.worktreeName());
        assertTrue(options.tmuxEnabled());
        assertEquals("custom-session", options.customSessionId());
        assertEquals(123, options.worktreePRNumber());
        assertEquals("/tmp/socket", options.messagingSocketPath());
    }

    @Test
    @DisplayName("SetupService.PermissionMode has correct values")
    void permissionModeHasCorrectValues() {
        SetupService.PermissionMode[] modes = SetupService.PermissionMode.values();

        assertEquals(5, modes.length);
        assertTrue(Arrays.asList(modes).contains(SetupService.PermissionMode.DEFAULT));
        assertTrue(Arrays.asList(modes).contains(SetupService.PermissionMode.ACCEPT_EDITS));
        assertTrue(Arrays.asList(modes).contains(SetupService.PermissionMode.BYPASS_PERMISSIONS));
        assertTrue(Arrays.asList(modes).contains(SetupService.PermissionMode.PLAN));
        assertTrue(Arrays.asList(modes).contains(SetupService.PermissionMode.AUTO));
    }

    @Test
    @DisplayName("SetupService getCwd returns current directory")
    void getCwdReturnsCurrentDirectory() {
        String cwd = SetupService.getCwd();

        assertNotNull(cwd);
        assertFalse(cwd.isEmpty());
    }
}
