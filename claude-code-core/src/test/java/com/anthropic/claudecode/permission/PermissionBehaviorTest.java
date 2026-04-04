/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.permission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PermissionBehavior.
 */
class PermissionBehaviorTest {

    @Test
    @DisplayName("PermissionBehavior enum has all expected values")
    void permissionBehaviorHasAllValues() {
        PermissionBehavior[] behaviors = PermissionBehavior.values();
        assertEquals(3, behaviors.length);
    }

    @Test
    @DisplayName("PermissionBehavior getValue returns correct string")
    void permissionBehaviorGetValueWorks() {
        assertEquals("allow", PermissionBehavior.ALLOW.getValue());
        assertEquals("deny", PermissionBehavior.DENY.getValue());
        assertEquals("ask", PermissionBehavior.ASK.getValue());
    }

    @Test
    @DisplayName("PermissionBehavior values are ordered correctly")
    void permissionBehaviorOrder() {
        PermissionBehavior[] behaviors = PermissionBehavior.values();
        assertEquals(PermissionBehavior.ALLOW, behaviors[0]);
        assertEquals(PermissionBehavior.DENY, behaviors[1]);
        assertEquals(PermissionBehavior.ASK, behaviors[2]);
    }
}