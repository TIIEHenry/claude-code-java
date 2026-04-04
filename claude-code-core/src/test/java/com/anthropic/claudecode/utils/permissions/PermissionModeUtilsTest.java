/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.permissions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PermissionModeUtils.
 */
class PermissionModeUtilsTest {

    @Test
    @DisplayName("PermissionModeUtils PermissionMode enum values")
    void permissionModeEnum() {
        PermissionModeUtils.PermissionMode[] modes = PermissionModeUtils.PermissionMode.values();
        assertEquals(7, modes.length);
    }

    @Test
    @DisplayName("PermissionModeUtils PermissionMode getTitle")
    void permissionModeGetTitle() {
        assertEquals("Default", PermissionModeUtils.PermissionMode.DEFAULT.getTitle());
        assertEquals("Plan Mode", PermissionModeUtils.PermissionMode.PLAN.getTitle());
        assertEquals("Accept edits", PermissionModeUtils.PermissionMode.ACCEPT_EDITS.getTitle());
    }

    @Test
    @DisplayName("PermissionModeUtils PermissionMode getShortTitle")
    void permissionModeGetShortTitle() {
        assertEquals("Default", PermissionModeUtils.PermissionMode.DEFAULT.getShortTitle());
        assertEquals("Plan", PermissionModeUtils.PermissionMode.PLAN.getShortTitle());
        assertEquals("Accept", PermissionModeUtils.PermissionMode.ACCEPT_EDITS.getShortTitle());
    }

    @Test
    @DisplayName("PermissionModeUtils PermissionMode getSymbol")
    void permissionModeGetSymbol() {
        assertEquals("", PermissionModeUtils.PermissionMode.DEFAULT.getSymbol());
        assertEquals("⏸", PermissionModeUtils.PermissionMode.PLAN.getSymbol());
    }

    @Test
    @DisplayName("PermissionModeUtils PermissionMode getColor")
    void permissionModeGetColor() {
        assertEquals("text", PermissionModeUtils.PermissionMode.DEFAULT.getColor());
        assertEquals("planMode", PermissionModeUtils.PermissionMode.PLAN.getColor());
    }

    @Test
    @DisplayName("PermissionModeUtils ExternalPermissionMode enum values")
    void externalPermissionModeEnum() {
        PermissionModeUtils.ExternalPermissionMode[] modes = PermissionModeUtils.ExternalPermissionMode.values();
        assertEquals(5, modes.length);
    }

    @Test
    @DisplayName("PermissionModeUtils PERMISSION_MODES list")
    void permissionModesList() {
        List<PermissionModeUtils.PermissionMode> modes = PermissionModeUtils.PERMISSION_MODES;
        assertEquals(7, modes.size());
    }

    @Test
    @DisplayName("PermissionModeUtils EXTERNAL_PERMISSION_MODES list")
    void externalPermissionModesList() {
        List<PermissionModeUtils.ExternalPermissionMode> modes = PermissionModeUtils.EXTERNAL_PERMISSION_MODES;
        assertEquals(5, modes.size());
    }

    @Test
    @DisplayName("PermissionModeUtils isExternalPermissionMode")
    void isExternalPermissionMode() {
        // The method behavior depends on USER_TYPE environment variable
        // For non-ant users, it always returns true
        // For ant users, AUTO and BUBBLE return false
        boolean isAnt = "ant".equals(System.getenv("USER_TYPE"));
        assertTrue(PermissionModeUtils.isExternalPermissionMode(PermissionModeUtils.PermissionMode.DEFAULT));
        assertTrue(PermissionModeUtils.isExternalPermissionMode(PermissionModeUtils.PermissionMode.PLAN));
        assertEquals(isAnt, !PermissionModeUtils.isExternalPermissionMode(PermissionModeUtils.PermissionMode.AUTO));
        assertEquals(isAnt, !PermissionModeUtils.isExternalPermissionMode(PermissionModeUtils.PermissionMode.BUBBLE));
    }

    @Test
    @DisplayName("PermissionModeUtils toExternalPermissionMode")
    void toExternalPermissionMode() {
        assertEquals(PermissionModeUtils.ExternalPermissionMode.DEFAULT,
            PermissionModeUtils.toExternalPermissionMode(PermissionModeUtils.PermissionMode.DEFAULT));
        assertEquals(PermissionModeUtils.ExternalPermissionMode.PLAN,
            PermissionModeUtils.toExternalPermissionMode(PermissionModeUtils.PermissionMode.PLAN));
        assertEquals(PermissionModeUtils.ExternalPermissionMode.ACCEPT_EDITS,
            PermissionModeUtils.toExternalPermissionMode(PermissionModeUtils.PermissionMode.ACCEPT_EDITS));
    }

    @Test
    @DisplayName("PermissionModeUtils permissionModeFromString")
    void permissionModeFromString() {
        assertEquals(PermissionModeUtils.PermissionMode.PLAN,
            PermissionModeUtils.permissionModeFromString("PLAN"));
        assertEquals(PermissionModeUtils.PermissionMode.ACCEPT_EDITS,
            PermissionModeUtils.permissionModeFromString("accept-edits"));
        assertEquals(PermissionModeUtils.PermissionMode.DEFAULT,
            PermissionModeUtils.permissionModeFromString("invalid"));
        assertEquals(PermissionModeUtils.PermissionMode.DEFAULT,
            PermissionModeUtils.permissionModeFromString(null));
        assertEquals(PermissionModeUtils.PermissionMode.DEFAULT,
            PermissionModeUtils.permissionModeFromString(""));
    }

    @Test
    @DisplayName("PermissionModeUtils permissionModeTitle")
    void permissionModeTitle() {
        assertEquals("Default", PermissionModeUtils.permissionModeTitle(PermissionModeUtils.PermissionMode.DEFAULT));
        assertEquals("Plan Mode", PermissionModeUtils.permissionModeTitle(PermissionModeUtils.PermissionMode.PLAN));
    }

    @Test
    @DisplayName("PermissionModeUtils permissionModeShortTitle")
    void permissionModeShortTitle() {
        assertEquals("Default", PermissionModeUtils.permissionModeShortTitle(PermissionModeUtils.PermissionMode.DEFAULT));
        assertEquals("Plan", PermissionModeUtils.permissionModeShortTitle(PermissionModeUtils.PermissionMode.PLAN));
    }

    @Test
    @DisplayName("PermissionModeUtils permissionModeSymbol")
    void permissionModeSymbol() {
        assertEquals("⏸", PermissionModeUtils.permissionModeSymbol(PermissionModeUtils.PermissionMode.PLAN));
    }

    @Test
    @DisplayName("PermissionModeUtils getModeColor")
    void getModeColor() {
        assertEquals("text", PermissionModeUtils.getModeColor(PermissionModeUtils.PermissionMode.DEFAULT));
        assertEquals("planMode", PermissionModeUtils.getModeColor(PermissionModeUtils.PermissionMode.PLAN));
    }

    @Test
    @DisplayName("PermissionModeUtils isDefaultMode")
    void isDefaultMode() {
        assertTrue(PermissionModeUtils.isDefaultMode(PermissionModeUtils.PermissionMode.DEFAULT));
        assertTrue(PermissionModeUtils.isDefaultMode(null));
        assertFalse(PermissionModeUtils.isDefaultMode(PermissionModeUtils.PermissionMode.PLAN));
    }

    @Test
    @DisplayName("PermissionModeUtils isAutoAcceptMode")
    void isAutoAcceptMode() {
        assertTrue(PermissionModeUtils.isAutoAcceptMode(PermissionModeUtils.PermissionMode.ACCEPT_EDITS));
        assertTrue(PermissionModeUtils.isAutoAcceptMode(PermissionModeUtils.PermissionMode.BYPASS_PERMISSIONS));
        assertTrue(PermissionModeUtils.isAutoAcceptMode(PermissionModeUtils.PermissionMode.DONT_ASK));
        assertFalse(PermissionModeUtils.isAutoAcceptMode(PermissionModeUtils.PermissionMode.DEFAULT));
        assertFalse(PermissionModeUtils.isAutoAcceptMode(PermissionModeUtils.PermissionMode.PLAN));
    }

    @Test
    @DisplayName("PermissionModeUtils isBypassMode")
    void isBypassMode() {
        assertTrue(PermissionModeUtils.isBypassMode(PermissionModeUtils.PermissionMode.BYPASS_PERMISSIONS));
        assertFalse(PermissionModeUtils.isBypassMode(PermissionModeUtils.PermissionMode.DEFAULT));
        assertFalse(PermissionModeUtils.isBypassMode(PermissionModeUtils.PermissionMode.ACCEPT_EDITS));
    }

    @Test
    @DisplayName("PermissionModeUtils isPlanMode")
    void isPlanMode() {
        assertTrue(PermissionModeUtils.isPlanMode(PermissionModeUtils.PermissionMode.PLAN));
        assertFalse(PermissionModeUtils.isPlanMode(PermissionModeUtils.PermissionMode.DEFAULT));
    }
}