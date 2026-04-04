/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code types/permissions.ts
 */
package com.anthropic.claudecode.permission;

import java.util.*;

/**
 * Permission mode enumeration.
 */
public enum PermissionMode {
    DEFAULT("default"),
    ACCEPT_EDITS("acceptEdits"),
    BYPASS_PERMISSIONS("bypassPermissions"),
    DONT_ASK("dontAsk"),
    PLAN("plan"),
    AUTO("auto"),
    BUBBLE("bubble");

    private final String value;

    PermissionMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PermissionMode fromString(String value) {
        for (PermissionMode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        return DEFAULT;
    }
}