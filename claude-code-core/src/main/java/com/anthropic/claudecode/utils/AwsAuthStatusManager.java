/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code AWS auth status manager
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Singleton manager for cloud-provider authentication status.
 * Communicates auth refresh state between auth utilities and UI components.
 * Used by AWS Bedrock, GCP Vertex, and other cloud auth refresh flows.
 */
public final class AwsAuthStatusManager {
    private static volatile AwsAuthStatusManager instance = null;

    private volatile AwsAuthStatus status = new AwsAuthStatus(false, new ArrayList<>(), null);
    private final SignalNew changed = new SignalNew();

    /**
     * AWS auth status record.
     */
    public record AwsAuthStatus(
            boolean isAuthenticating,
            List<String> output,
            String error
    ) {
        public AwsAuthStatus copy() {
            return new AwsAuthStatus(isAuthenticating, new ArrayList<>(output), error);
        }
    }

    private AwsAuthStatusManager() {}

    /**
     * Get singleton instance.
     */
    public static AwsAuthStatusManager getInstance() {
        if (instance == null) {
            synchronized (AwsAuthStatusManager.class) {
                if (instance == null) {
                    instance = new AwsAuthStatusManager();
                }
            }
        }
        return instance;
    }

    /**
     * Get current status.
     */
    public AwsAuthStatus getStatus() {
        return status.copy();
    }

    /**
     * Start authentication.
     */
    public void startAuthentication() {
        status = new AwsAuthStatus(true, new ArrayList<>(), null);
        changed.emit(status.copy());
    }

    /**
     * Add output line.
     */
    public void addOutput(String line) {
        List<String> newOutput = new ArrayList<>(status.output());
        newOutput.add(line);
        status = new AwsAuthStatus(status.isAuthenticating(), newOutput, status.error());
        changed.emit(status.copy());
    }

    /**
     * Set error.
     */
    public void setError(String error) {
        status = new AwsAuthStatus(status.isAuthenticating(), status.output(), error);
        changed.emit(status.copy());
    }

    /**
     * End authentication.
     */
    public void endAuthentication(boolean success) {
        if (success) {
            // Clear status completely on success
            status = new AwsAuthStatus(false, new ArrayList<>(), null);
        } else {
            // Keep output visible on failure
            status = new AwsAuthStatus(false, status.output(), status.error());
        }
        changed.emit(status.copy());
    }

    /**
     * Subscribe to status changes.
     */
    public void subscribe(Runnable callback) {
        changed.subscribe(callback);
    }

    /**
     * Reset for testing.
     */
    public static void reset() {
        if (instance != null) {
            instance.changed.clear();
            instance = null;
        }
    }

    /**
     * Check if currently authenticating.
     */
    public boolean isAuthenticating() {
        return status.isAuthenticating();
    }

    /**
     * Get output lines.
     */
    public List<String> getOutput() {
        return new ArrayList<>(status.output());
    }

    /**
     * Get error if present.
     */
    public Optional<String> getError() {
        return Optional.ofNullable(status.error());
    }
}