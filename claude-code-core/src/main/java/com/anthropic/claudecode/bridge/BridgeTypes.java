/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code bridge/types
 */
package com.anthropic.claudecode.bridge;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Bridge types - Protocol types for bridge communication.
 */
public final class BridgeTypes {
    /** Default per-session timeout (24 hours). */
    public static final long DEFAULT_SESSION_TIMEOUT_MS = 24 * 60 * 60 * 1000;

    /** Login guidance for bridge auth errors. */
    public static final String BRIDGE_LOGIN_INSTRUCTION =
        "Remote Control is only available with claude.ai subscriptions. " +
        "Please use `/login` to sign in with your claude.ai account.";

    /** Full error when remote-control is run without auth. */
    public static final String BRIDGE_LOGIN_ERROR =
        "Error: You must be logged in to use Remote Control.\n\n" +
        BRIDGE_LOGIN_INSTRUCTION;

    /** Shown when Remote Control is disconnected. */
    public static final String REMOTE_CONTROL_DISCONNECTED_MSG = "Remote Control disconnected.";

    /**
     * Work data record.
     */
    public record WorkData(
        String type,  // "session" or "healthcheck"
        String id
    ) {}

    /**
     * Work response record.
     */
    public record WorkResponse(
        String id,
        String type,  // "work"
        String environmentId,
        String state,
        WorkData data,
        String secret,  // base64url-encoded JSON
        String createdAt
    ) {}

    /**
     * Work secret record (decoded from base64).
     */
    public record WorkSecret(
        int version,
        String sessionIngressToken,
        String apiBaseUrl,
        List<Source> sources,
        List<Auth> auth,
        Map<String, String> claudeCodeArgs,
        Object mcpConfig,
        Map<String, String> environmentVariables,
        Boolean useCodeSessions
    ) {}

    /**
     * Source record for work secret.
     */
    public record Source(
        String type,
        GitInfo gitInfo
    ) {}

    /**
     * Git info record.
     */
    public record GitInfo(
        String type,
        String repo,
        String ref,
        String token
    ) {}

    /**
     * Auth record for work secret.
     */
    public record Auth(
        String type,
        String token
    ) {}

    /**
     * Session done status enum.
     */
    public enum SessionDoneStatus {
        COMPLETED,
        FAILED,
        INTERRUPTED
    }

    /**
     * Session activity type enum.
     */
    public enum SessionActivityType {
        TOOL_START,
        TEXT,
        RESULT,
        ERROR
    }

    /**
     * Session activity record.
     */
    public record SessionActivity(
        SessionActivityType type,
        String summary,
        long timestamp
    ) {}

    /**
     * Spawn mode enum.
     */
    public enum SpawnMode {
        SINGLE_SESSION,
        WORKTREE,
        SAME_DIR
    }

    /**
     * Bridge worker type enum.
     */
    public enum BridgeWorkerType {
        CLAUDE_CODE("claude_code"),
        CLAUDE_CODE_ASSISTANT("claude_code_assistant");

        private final String value;

        BridgeWorkerType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static BridgeWorkerType fromValue(String value) {
            for (BridgeWorkerType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * Bridge config record.
     */
    public record BridgeConfig(
        String dir,
        String machineName,
        String branch,
        String gitRepoUrl,
        int maxSessions,
        SpawnMode spawnMode,
        boolean verbose,
        boolean sandbox,
        String bridgeId,
        String workerType,
        String environmentId,
        String reuseEnvironmentId,
        String apiBaseUrl,
        String sessionIngressUrl,
        String debugFile,
        Long sessionTimeoutMs
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String dir;
            private String machineName;
            private String branch;
            private String gitRepoUrl;
            private int maxSessions = 1;
            private SpawnMode spawnMode = SpawnMode.SINGLE_SESSION;
            private boolean verbose = false;
            private boolean sandbox = false;
            private String bridgeId;
            private String workerType = "claude_code";
            private String environmentId;
            private String reuseEnvironmentId;
            private String apiBaseUrl;
            private String sessionIngressUrl;
            private String debugFile;
            private Long sessionTimeoutMs = DEFAULT_SESSION_TIMEOUT_MS;

            public Builder dir(String dir) { this.dir = dir; return this; }
            public Builder machineName(String machineName) { this.machineName = machineName; return this; }
            public Builder branch(String branch) { this.branch = branch; return this; }
            public Builder gitRepoUrl(String gitRepoUrl) { this.gitRepoUrl = gitRepoUrl; return this; }
            public Builder maxSessions(int maxSessions) { this.maxSessions = maxSessions; return this; }
            public Builder spawnMode(SpawnMode spawnMode) { this.spawnMode = spawnMode; return this; }
            public Builder verbose(boolean verbose) { this.verbose = verbose; return this; }
            public Builder sandbox(boolean sandbox) { this.sandbox = sandbox; return this; }
            public Builder bridgeId(String bridgeId) { this.bridgeId = bridgeId; return this; }
            public Builder workerType(String workerType) { this.workerType = workerType; return this; }
            public Builder environmentId(String environmentId) { this.environmentId = environmentId; return this; }
            public Builder reuseEnvironmentId(String reuseEnvironmentId) { this.reuseEnvironmentId = reuseEnvironmentId; return this; }
            public Builder apiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; return this; }
            public Builder sessionIngressUrl(String sessionIngressUrl) { this.sessionIngressUrl = sessionIngressUrl; return this; }
            public Builder debugFile(String debugFile) { this.debugFile = debugFile; return this; }
            public Builder sessionTimeoutMs(Long sessionTimeoutMs) { this.sessionTimeoutMs = sessionTimeoutMs; return this; }

            public BridgeConfig build() {
                return new BridgeConfig(
                    dir, machineName, branch, gitRepoUrl,
                    maxSessions, spawnMode, verbose, sandbox,
                    bridgeId, workerType, environmentId,
                    reuseEnvironmentId, apiBaseUrl, sessionIngressUrl,
                    debugFile, sessionTimeoutMs
                );
            }
        }
    }

    /**
     * Permission response event.
     */
    public record PermissionResponseEvent(
        String type,  // "control_response"
        Response response
    ) {
        public record Response(
            String subtype,  // "success"
            String requestId,
            Map<String, Object> response
        ) {}
    }

    /**
     * Session handle interface.
     */
    public interface SessionHandle {
        String getSessionId();
        CompletableFuture<SessionDoneStatus> done();
        void kill();
        void forceKill();
        List<SessionActivity> getActivities();
        SessionActivity getCurrentActivity();
        String getAccessToken();
        List<String> getLastStderr();
        void writeStdin(String data);
        void updateAccessToken(String token);
    }

    /**
     * Session spawn options.
     */
    public record SessionSpawnOpts(
        String sessionId,
        String sdkUrl,
        String accessToken,
        boolean useCcrV2,
        Integer workerEpoch,
        java.util.function.Consumer<String> onFirstUserMessage
    ) {}
}