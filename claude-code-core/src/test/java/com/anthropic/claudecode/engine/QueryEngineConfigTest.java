/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.engine;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;

import com.anthropic.claudecode.*;
import com.anthropic.claudecode.permission.PermissionResult;
import com.anthropic.claudecode.permission.PermissionMode;

/**
 * Tests for QueryEngineConfig.
 */
@DisplayName("QueryEngineConfig Tests")
class QueryEngineConfigTest {

    @Test
    @DisplayName("QueryEngineConfig builder creates default config")
    void builderCreatesDefaultConfig() {
        QueryEngineConfig config = QueryEngineConfig.builder().build();

        assertNotNull(config);
        assertEquals(System.getProperty("user.dir"), config.cwd());
        assertTrue(config.tools().isEmpty());
        assertTrue(config.commands().isEmpty());
        assertTrue(config.mcpClients().isEmpty());
        assertFalse(config.verbose());
        assertFalse(config.debug());
        assertFalse(config.isNonInteractiveSession());
        assertEquals(PermissionMode.DEFAULT, config.permissionMode());
    }

    @Test
    @DisplayName("QueryEngineConfig builder with all parameters")
    void builderWithAllParameters() {
        CanUseToolFn canUseTool = (tool, input, context, assistant, id) ->
            CompletableFuture.completedFuture(PermissionResult.allow(input));

        QueryEngineConfig config = QueryEngineConfig.builder()
            .cwd("/custom")
            .tools(Collections.emptyList())
            .commands(List.of("cmd1"))
            .mcpClients(List.of("client1"))
            .agents(List.of("agent1"))
            .canUseTool(canUseTool)
            .initialMessages(List.of("msg1"))
            .customSystemPrompt("custom prompt")
            .appendSystemPrompt("append prompt")
            .userSpecifiedModel("claude-opus-4-6")
            .fallbackModel("claude-sonnet-4-6")
            .apiKey("test-key")
            .model("glm-5")
            .systemPrompt("system prompt")
            .maxTurns(10)
            .maxBudgetUsd(5.0)
            .verbose(true)
            .debug(true)
            .isNonInteractiveSession(true)
            .permissionMode(PermissionMode.ACCEPT_EDITS)
            .build();

        assertEquals("/custom", config.cwd());
        assertTrue(config.tools().isEmpty());
        assertEquals(1, config.commands().size());
        assertEquals(1, config.mcpClients().size());
        assertEquals(1, config.agents().size());
        assertEquals("custom prompt", config.customSystemPrompt());
        assertEquals("append prompt", config.appendSystemPrompt());
        assertEquals("claude-opus-4-6", config.userSpecifiedModel());
        assertEquals("claude-sonnet-4-6", config.fallbackModel());
        assertEquals("test-key", config.apiKey());
        assertEquals("glm-5", config.model());
        assertEquals("system prompt", config.systemPrompt());
        assertEquals(10, config.maxTurns());
        assertEquals(5.0, config.maxBudgetUsd());
        assertTrue(config.verbose());
        assertTrue(config.debug());
        assertTrue(config.isNonInteractiveSession());
        assertEquals(PermissionMode.ACCEPT_EDITS, config.permissionMode());
    }

    @Test
    @DisplayName("QueryEngineConfig canUseTool default allows all")
    void canUseToolDefaultAllowsAll() {
        QueryEngineConfig config = QueryEngineConfig.builder().build();

        // Default canUseTool should allow all
        CompletableFuture<PermissionResult> future = config.canUseTool()
            .apply(null, "input", null, null, "tool-id");

        PermissionResult result = future.join();
        assertTrue(result instanceof PermissionResult.Allow);
        assertEquals("allow", result.behavior());
    }

    @Test
    @DisplayName("QueryEngineConfig cwd defaults to user.dir")
    void cwdDefaultsToUserDir() {
        QueryEngineConfig config = QueryEngineConfig.builder().build();

        assertEquals(System.getProperty("user.dir"), config.cwd());
    }

    @Test
    @DisplayName("QueryEngineConfig builder allows overriding individual fields")
    void builderAllowsOverridingIndividualFields() {
        QueryEngineConfig config1 = QueryEngineConfig.builder()
            .cwd("/first")
            .verbose(true)
            .build();

        QueryEngineConfig config2 = QueryEngineConfig.builder()
            .cwd("/second")
            .verbose(true)
            .build();

        assertEquals("/first", config1.cwd());
        assertEquals("/second", config2.cwd());
    }

    @Test
    @DisplayName("QueryEngineConfig model defaults to glm-5")
    void modelDefaultsToGlm5() {
        QueryEngineConfig config = QueryEngineConfig.builder().build();

        assertEquals("glm-5", config.model());
    }
}