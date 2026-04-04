/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.bridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BridgeTypes.
 */
class BridgeTypesTest {

    @Test
    @DisplayName("BridgeTypes DEFAULT_SESSION_TIMEOUT_MS")
    void defaultSessionTimeoutMs() {
        assertEquals(24 * 60 * 60 * 1000, BridgeTypes.DEFAULT_SESSION_TIMEOUT_MS);
    }

    @Test
    @DisplayName("BridgeTypes BRIDGE_LOGIN_INSTRUCTION not null")
    void bridgeLoginInstruction() {
        assertNotNull(BridgeTypes.BRIDGE_LOGIN_INSTRUCTION);
        assertTrue(BridgeTypes.BRIDGE_LOGIN_INSTRUCTION.contains("claude.ai"));
    }

    @Test
    @DisplayName("BridgeTypes BRIDGE_LOGIN_ERROR not null")
    void bridgeLoginError() {
        assertNotNull(BridgeTypes.BRIDGE_LOGIN_ERROR);
        assertTrue(BridgeTypes.BRIDGE_LOGIN_ERROR.contains("logged in"));
    }

    @Test
    @DisplayName("BridgeTypes REMOTE_CONTROL_DISCONNECTED_MSG")
    void remoteControlDisconnectedMsg() {
        assertEquals("Remote Control disconnected.", BridgeTypes.REMOTE_CONTROL_DISCONNECTED_MSG);
    }

    @Test
    @DisplayName("BridgeTypes WorkData record")
    void workDataRecord() {
        BridgeTypes.WorkData data = new BridgeTypes.WorkData("session", "id-123");

        assertEquals("session", data.type());
        assertEquals("id-123", data.id());
    }

    @Test
    @DisplayName("BridgeTypes WorkResponse record")
    void workResponseRecord() {
        BridgeTypes.WorkData workData = new BridgeTypes.WorkData("session", "id");
        BridgeTypes.WorkResponse response = new BridgeTypes.WorkResponse(
            "work-id",
            "work",
            "env-123",
            "running",
            workData,
            "secret-base64",
            "2024-01-01T00:00:00Z"
        );

        assertEquals("work-id", response.id());
        assertEquals("work", response.type());
        assertEquals("env-123", response.environmentId());
        assertEquals("running", response.state());
    }

    @Test
    @DisplayName("BridgeTypes WorkSecret record")
    void workSecretRecord() {
        BridgeTypes.WorkSecret secret = new BridgeTypes.WorkSecret(
            1,
            "ingress-token",
            "https://api.example.com",
            List.of(),
            List.of(),
            Map.of(),
            null,
            Map.of(),
            false
        );

        assertEquals(1, secret.version());
        assertEquals("ingress-token", secret.sessionIngressToken());
        assertEquals("https://api.example.com", secret.apiBaseUrl());
    }

    @Test
    @DisplayName("BridgeTypes Source record")
    void sourceRecord() {
        BridgeTypes.GitInfo gitInfo = new BridgeTypes.GitInfo("github", "repo", "main", "token");
        BridgeTypes.Source source = new BridgeTypes.Source("git", gitInfo);

        assertEquals("git", source.type());
        assertNotNull(source.gitInfo());
    }

    @Test
    @DisplayName("BridgeTypes GitInfo record")
    void gitInfoRecord() {
        BridgeTypes.GitInfo gitInfo = new BridgeTypes.GitInfo("github", "owner/repo", "main", "token123");

        assertEquals("github", gitInfo.type());
        assertEquals("owner/repo", gitInfo.repo());
        assertEquals("main", gitInfo.ref());
        assertEquals("token123", gitInfo.token());
    }

    @Test
    @DisplayName("BridgeTypes Auth record")
    void authRecord() {
        BridgeTypes.Auth auth = new BridgeTypes.Auth("bearer", "token-value");

        assertEquals("bearer", auth.type());
        assertEquals("token-value", auth.token());
    }

    @Test
    @DisplayName("BridgeTypes SessionDoneStatus enum")
    void sessionDoneStatusEnum() {
        BridgeTypes.SessionDoneStatus[] values = BridgeTypes.SessionDoneStatus.values();
        assertEquals(3, values.length);
        assertEquals(BridgeTypes.SessionDoneStatus.COMPLETED, BridgeTypes.SessionDoneStatus.valueOf("COMPLETED"));
        assertEquals(BridgeTypes.SessionDoneStatus.FAILED, BridgeTypes.SessionDoneStatus.valueOf("FAILED"));
        assertEquals(BridgeTypes.SessionDoneStatus.INTERRUPTED, BridgeTypes.SessionDoneStatus.valueOf("INTERRUPTED"));
    }

    @Test
    @DisplayName("BridgeTypes SessionActivityType enum")
    void sessionActivityTypeEnum() {
        BridgeTypes.SessionActivityType[] values = BridgeTypes.SessionActivityType.values();
        assertEquals(4, values.length);
        assertEquals(BridgeTypes.SessionActivityType.TOOL_START, BridgeTypes.SessionActivityType.valueOf("TOOL_START"));
        assertEquals(BridgeTypes.SessionActivityType.TEXT, BridgeTypes.SessionActivityType.valueOf("TEXT"));
        assertEquals(BridgeTypes.SessionActivityType.RESULT, BridgeTypes.SessionActivityType.valueOf("RESULT"));
        assertEquals(BridgeTypes.SessionActivityType.ERROR, BridgeTypes.SessionActivityType.valueOf("ERROR"));
    }

    @Test
    @DisplayName("BridgeTypes SessionActivity record")
    void sessionActivityRecord() {
        BridgeTypes.SessionActivity activity = new BridgeTypes.SessionActivity(
            BridgeTypes.SessionActivityType.TEXT,
            "Processing...",
            System.currentTimeMillis()
        );

        assertEquals(BridgeTypes.SessionActivityType.TEXT, activity.type());
        assertEquals("Processing...", activity.summary());
    }

    @Test
    @DisplayName("BridgeTypes SpawnMode enum")
    void spawnModeEnum() {
        BridgeTypes.SpawnMode[] values = BridgeTypes.SpawnMode.values();
        assertEquals(3, values.length);
        assertEquals(BridgeTypes.SpawnMode.SINGLE_SESSION, BridgeTypes.SpawnMode.valueOf("SINGLE_SESSION"));
        assertEquals(BridgeTypes.SpawnMode.WORKTREE, BridgeTypes.SpawnMode.valueOf("WORKTREE"));
        assertEquals(BridgeTypes.SpawnMode.SAME_DIR, BridgeTypes.SpawnMode.valueOf("SAME_DIR"));
    }

    @Test
    @DisplayName("BridgeTypes BridgeWorkerType enum")
    void bridgeWorkerTypeEnum() {
        BridgeTypes.BridgeWorkerType[] values = BridgeTypes.BridgeWorkerType.values();
        assertEquals(2, values.length);
        assertEquals("claude_code", BridgeTypes.BridgeWorkerType.CLAUDE_CODE.getValue());
        assertEquals("claude_code_assistant", BridgeTypes.BridgeWorkerType.CLAUDE_CODE_ASSISTANT.getValue());
    }

    @Test
    @DisplayName("BridgeTypes BridgeWorkerType fromValue")
    void bridgeWorkerTypeFromValue() {
        BridgeTypes.BridgeWorkerType type = BridgeTypes.BridgeWorkerType.fromValue("claude_code");
        assertEquals(BridgeTypes.BridgeWorkerType.CLAUDE_CODE, type);

        BridgeTypes.BridgeWorkerType invalid = BridgeTypes.BridgeWorkerType.fromValue("invalid");
        assertNull(invalid);
    }

    @Test
    @DisplayName("BridgeTypes BridgeConfig builder")
    void bridgeConfigBuilder() {
        BridgeTypes.BridgeConfig config = BridgeTypes.BridgeConfig.builder()
            .dir("/project")
            .machineName("test-machine")
            .branch("main")
            .maxSessions(5)
            .verbose(true)
            .build();

        assertEquals("/project", config.dir());
        assertEquals("test-machine", config.machineName());
        assertEquals("main", config.branch());
        assertEquals(5, config.maxSessions());
        assertTrue(config.verbose());
    }

    @Test
    @DisplayName("BridgeTypes BridgeConfig default values")
    void bridgeConfigDefaultValues() {
        BridgeTypes.BridgeConfig config = BridgeTypes.BridgeConfig.builder().build();

        assertEquals(1, config.maxSessions());
        assertFalse(config.verbose());
        assertFalse(config.sandbox());
        assertEquals(BridgeTypes.SpawnMode.SINGLE_SESSION, config.spawnMode());
        assertEquals(BridgeTypes.DEFAULT_SESSION_TIMEOUT_MS, config.sessionTimeoutMs());
    }

    @Test
    @DisplayName("BridgeTypes PermissionResponseEvent record")
    void permissionResponseEventRecord() {
        BridgeTypes.PermissionResponseEvent.Response response =
            new BridgeTypes.PermissionResponseEvent.Response("success", "req-123", Map.of("allowed", true));
        BridgeTypes.PermissionResponseEvent event =
            new BridgeTypes.PermissionResponseEvent("control_response", response);

        assertEquals("control_response", event.type());
        assertEquals("success", event.response().subtype());
        assertEquals("req-123", event.response().requestId());
    }

    @Test
    @DisplayName("BridgeTypes SessionSpawnOpts record")
    void sessionSpawnOptsRecord() {
        BridgeTypes.SessionSpawnOpts opts = new BridgeTypes.SessionSpawnOpts(
            "session-123",
            "https://sdk.example.com",
            "access-token",
            false,
            1,
            null
        );

        assertEquals("session-123", opts.sessionId());
        assertEquals("https://sdk.example.com", opts.sdkUrl());
        assertFalse(opts.useCcrV2());
    }
}