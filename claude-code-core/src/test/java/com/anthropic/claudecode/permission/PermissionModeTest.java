/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.permission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PermissionMode.
 */
class PermissionModeTest {

    @Test
    @DisplayName("PermissionMode enum has all expected values")
    void permissionModeHasAllValues() {
        PermissionMode[] modes = PermissionMode.values();
        assertEquals(7, modes.length);
    }

    @Test
    @DisplayName("PermissionMode getValue returns correct string")
    void permissionModeGetValueWorks() {
        assertEquals("default", PermissionMode.DEFAULT.getValue());
        assertEquals("acceptEdits", PermissionMode.ACCEPT_EDITS.getValue());
        assertEquals("bypassPermissions", PermissionMode.BYPASS_PERMISSIONS.getValue());
        assertEquals("dontAsk", PermissionMode.DONT_ASK.getValue());
        assertEquals("plan", PermissionMode.PLAN.getValue());
        assertEquals("auto", PermissionMode.AUTO.getValue());
        assertEquals("bubble", PermissionMode.BUBBLE.getValue());
    }

    @Test
    @DisplayName("PermissionMode fromString parses correctly")
    void permissionModeFromStringWorks() {
        assertEquals(PermissionMode.DEFAULT, PermissionMode.fromString("default"));
        assertEquals(PermissionMode.ACCEPT_EDITS, PermissionMode.fromString("acceptEdits"));
        assertEquals(PermissionMode.BYPASS_PERMISSIONS, PermissionMode.fromString("bypassPermissions"));
        assertEquals(PermissionMode.AUTO, PermissionMode.fromString("auto"));
    }

    @Test
    @DisplayName("PermissionMode fromString returns DEFAULT for unknown")
    void permissionModeFromStringUnknown() {
        assertEquals(PermissionMode.DEFAULT, PermissionMode.fromString("unknown"));
        assertEquals(PermissionMode.DEFAULT, PermissionMode.fromString(null));
        assertEquals(PermissionMode.DEFAULT, PermissionMode.fromString(""));
    }
}