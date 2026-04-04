/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code types/permissions.ts
 */
package com.anthropic.claudecode.permission;

import java.util.*;

/**
 * Permission behavior enumeration.
 */
public enum PermissionBehavior {
    ALLOW("allow"),
    DENY("deny"),
    ASK("ask");

    private final String value;

    PermissionBehavior(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}