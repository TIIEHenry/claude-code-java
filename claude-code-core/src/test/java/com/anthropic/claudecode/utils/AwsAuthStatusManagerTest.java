/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AwsAuthStatusManager.
 */
class AwsAuthStatusManagerTest {

    @BeforeEach
    void setUp() {
        AwsAuthStatusManager.reset();
    }

    @Test
    @DisplayName("AwsAuthStatusManager getInstance returns singleton")
    void getInstance() {
        AwsAuthStatusManager instance1 = AwsAuthStatusManager.getInstance();
        AwsAuthStatusManager instance2 = AwsAuthStatusManager.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    @DisplayName("AwsAuthStatusManager AwsAuthStatus record")
    void awsAuthStatus() {
        AwsAuthStatusManager.AwsAuthStatus status = new AwsAuthStatusManager.AwsAuthStatus(
            true, List.of("line1", "line2"), "error"
        );
        assertTrue(status.isAuthenticating());
        assertEquals(2, status.output().size());
        assertEquals("error", status.error());
    }

    @Test
    @DisplayName("AwsAuthStatusManager AwsAuthStatus copy")
    void awsAuthStatusCopy() {
        AwsAuthStatusManager.AwsAuthStatus status = new AwsAuthStatusManager.AwsAuthStatus(
            true, List.of("line1"), null
        );
        AwsAuthStatusManager.AwsAuthStatus copy = status.copy();
        assertEquals(status.isAuthenticating(), copy.isAuthenticating());
        assertEquals(status.output(), copy.output());
        assertNotSame(status.output(), copy.output());
    }

    @Test
    @DisplayName("AwsAuthStatusManager getStatus initially not authenticating")
    void getStatusInitiallyNotAuthenticating() {
        AwsAuthStatusManager manager = AwsAuthStatusManager.getInstance();
        AwsAuthStatusManager.AwsAuthStatus status = manager.getStatus();
        assertFalse(status.isAuthenticating());
        assertTrue(status.output().isEmpty());
        assertNull(status.error());
    }

    @Test
    @DisplayName("AwsAuthStatusManager startAuthentication sets authenticating")
    void startAuthentication() {
        AwsAuthStatusManager manager = AwsAuthStatusManager.getInstance();
        manager.startAuthentication();
        assertTrue(manager.isAuthenticating());
    }

    @Test
    @DisplayName("AwsAuthStatusManager addOutput adds line")
    void addOutput() {
        AwsAuthStatusManager manager = AwsAuthStatusManager.getInstance();
        manager.startAuthentication();
        manager.addOutput("test line");
        List<String> output = manager.getOutput();
        assertEquals(1, output.size());
        assertEquals("test line", output.get(0));
    }

    @Test
    @DisplayName("AwsAuthStatusManager setError sets error")
    void setError() {
        AwsAuthStatusManager manager = AwsAuthStatusManager.getInstance();
        manager.setError("test error");
        Optional<String> error = manager.getError();
        assertTrue(error.isPresent());
        assertEquals("test error", error.get());
    }

    @Test
    @DisplayName("AwsAuthStatusManager endAuthentication success clears status")
    void endAuthenticationSuccess() {
        AwsAuthStatusManager manager = AwsAuthStatusManager.getInstance();
        manager.startAuthentication();
        manager.addOutput("test");
        manager.endAuthentication(true);
        
        assertFalse(manager.isAuthenticating());
        assertTrue(manager.getOutput().isEmpty());
    }

    @Test
    @DisplayName("AwsAuthStatusManager endAuthentication failure keeps output")
    void endAuthenticationFailure() {
        AwsAuthStatusManager manager = AwsAuthStatusManager.getInstance();
        manager.startAuthentication();
        manager.addOutput("test");
        manager.endAuthentication(false);
        
        assertFalse(manager.isAuthenticating());
        assertEquals(1, manager.getOutput().size());
    }

    @Test
    @DisplayName("AwsAuthStatusManager isAuthenticating returns state")
    void isAuthenticating() {
        AwsAuthStatusManager manager = AwsAuthStatusManager.getInstance();
        assertFalse(manager.isAuthenticating());
        manager.startAuthentication();
        assertTrue(manager.isAuthenticating());
    }

    @Test
    @DisplayName("AwsAuthStatusManager getOutput returns copy")
    void getOutputReturnsCopy() {
        AwsAuthStatusManager manager = AwsAuthStatusManager.getInstance();
        manager.startAuthentication();
        manager.addOutput("test");
        List<String> output1 = manager.getOutput();
        List<String> output2 = manager.getOutput();
        assertNotSame(output1, output2);
    }

    @Test
    @DisplayName("AwsAuthStatusManager getError returns empty when no error")
    void getErrorEmpty() {
        AwsAuthStatusManager manager = AwsAuthStatusManager.getInstance();
        Optional<String> error = manager.getError();
        assertFalse(error.isPresent());
    }

    @Test
    @DisplayName("AwsAuthStatusManager subscribe receives updates")
    void subscribe() {
        AwsAuthStatusManager manager = AwsAuthStatusManager.getInstance();
        AtomicInteger callCount = new AtomicInteger(0);
        manager.subscribe(callCount::incrementAndGet);
        
        manager.startAuthentication();
        assertTrue(callCount.get() >= 1);
    }

    @Test
    @DisplayName("AwsAuthStatusManager reset clears instance")
    void reset() {
        AwsAuthStatusManager manager1 = AwsAuthStatusManager.getInstance();
        manager1.startAuthentication();
        AwsAuthStatusManager.reset();
        AwsAuthStatusManager manager2 = AwsAuthStatusManager.getInstance();
        assertFalse(manager2.isAuthenticating());
    }
}
