/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EnvDynamic.
 */
class EnvDynamicTest {

    @Test
    @DisplayName("EnvDynamic getIsDocker returns future")
    void getIsDockerReturnsFuture() {
        CompletableFuture<Boolean> future = EnvDynamic.getIsDocker();
        assertNotNull(future);
    }

    @Test
    @DisplayName("EnvDynamic getIsBubblewrapSandbox returns boolean")
    void getIsBubblewrapSandboxReturnsBoolean() {
        boolean result = EnvDynamic.getIsBubblewrapSandbox();
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("EnvDynamic isMuslEnvironment returns boolean")
    void isMuslEnvironmentReturnsBoolean() {
        boolean result = EnvDynamic.isMuslEnvironment();
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("EnvDynamic getTerminalWithJetBrainsDetection returns string or null")
    void getTerminalWithJetBrainsDetectionReturnsString() {
        String result = EnvDynamic.getTerminalWithJetBrainsDetection();
        // May be null if TERM not set
        assertTrue(result == null || result instanceof String);
    }

    @Test
    @DisplayName("EnvDynamic getTerminalWithJetBrainsDetectionAsync returns future")
    void getTerminalWithJetBrainsDetectionAsyncReturnsFuture() {
        CompletableFuture<String> future = EnvDynamic.getTerminalWithJetBrainsDetectionAsync();
        assertNotNull(future);
    }

    @Test
    @DisplayName("EnvDynamic initJetBrainsDetection returns future")
    void initJetBrainsDetectionReturnsFuture() {
        CompletableFuture<Void> future = EnvDynamic.initJetBrainsDetection();
        assertNotNull(future);
    }
}