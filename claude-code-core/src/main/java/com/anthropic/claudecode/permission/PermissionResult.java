/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code types/permissions.ts
 */
package com.anthropic.claudecode.permission;

import java.util.*;

/**
 * Permission result - sealed interface for allow/deny/ask decisions.
 * Uses non-generic sealed interface with generic permit implementations.
 */
public sealed interface PermissionResult permits
    PermissionResult.Allow, PermissionResult.Deny, PermissionResult.Ask {

    String behavior();

    /**
     * Allow result with updated input.
     */
    public final class Allow<I> implements PermissionResult {
        private final I updatedInput;
        private final boolean userModified;
        private final String decisionReason;

        public Allow(I updatedInput) {
            this(updatedInput, false, null);
        }

        public Allow(I updatedInput, boolean userModified, String decisionReason) {
            this.updatedInput = updatedInput;
            this.userModified = userModified;
            this.decisionReason = decisionReason;
        }

        public I updatedInput() { return updatedInput; }
        public boolean userModified() { return userModified; }
        public String decisionReason() { return decisionReason; }

        @Override
        public String behavior() { return "allow"; }
    }

    /**
     * Deny result with message.
     */
    public final class Deny implements PermissionResult {
        private final String message;
        private final String decisionReason;

        public Deny(String message) {
            this(message, null);
        }

        public Deny(String message, String decisionReason) {
            this.message = message;
            this.decisionReason = decisionReason;
        }

        public String message() { return message; }
        public String decisionReason() { return decisionReason; }

        @Override
        public String behavior() { return "deny"; }
    }

    /**
     * Ask result - need user confirmation.
     */
    public final class Ask<I> implements PermissionResult {
        private final String message;
        private final I updatedInput;
        private final String decisionReason;

        public Ask(String message) {
            this(message, null, null);
        }

        public Ask(String message, I updatedInput, String decisionReason) {
            this.message = message;
            this.updatedInput = updatedInput;
            this.decisionReason = decisionReason;
        }

        public String message() { return message; }
        public I updatedInput() { return updatedInput; }
        public String decisionReason() { return decisionReason; }

        @Override
        public String behavior() { return "ask"; }
    }

    // Factory methods
    static <I> Allow<I> allow(I input) {
        return new Allow<>(input);
    }

    static <I> Allow<I> allow(I input, boolean userModified) {
        return new Allow<>(input, userModified, null);
    }

    static Deny deny(String message) {
        return new Deny(message);
    }

    static <I> Ask<I> ask(String message) {
        return new Ask<>(message);
    }

    static <I> Ask<I> ask(String message, I updatedInput) {
        return new Ask<>(message, updatedInput, null);
    }
}