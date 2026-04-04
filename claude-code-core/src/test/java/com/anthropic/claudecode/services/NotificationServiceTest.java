/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NotificationService.
 */
class NotificationServiceTest {

    @BeforeEach
    void setUp() {
        NotificationService.setPreferredChannel("auto");
        NotificationService.setDetectedTerminal(NotificationService.TerminalType.UNKNOWN);
    }

    @Test
    @DisplayName("NotificationService TerminalType enum")
    void terminalTypeEnum() {
        NotificationService.TerminalType[] types = NotificationService.TerminalType.values();
        assertEquals(7, types.length);
        assertEquals("Apple_Terminal", NotificationService.TerminalType.APPLE_TERMINAL.getId());
        assertEquals("iTerm.app", NotificationService.TerminalType.ITERM2.getId());
        assertEquals("kitty", NotificationService.TerminalType.KITTY.getId());
    }

    @Test
    @DisplayName("NotificationService TerminalType fromEnv")
    void terminalTypeFromEnv() {
        assertEquals(NotificationService.TerminalType.ITERM2,
            NotificationService.TerminalType.fromEnv("iTerm.app"));
        assertEquals(NotificationService.TerminalType.KITTY,
            NotificationService.TerminalType.fromEnv("kitty"));
        assertEquals(NotificationService.TerminalType.UNKNOWN,
            NotificationService.TerminalType.fromEnv(null));
        assertEquals(NotificationService.TerminalType.UNKNOWN,
            NotificationService.TerminalType.fromEnv("unknown_term"));
    }

    @Test
    @DisplayName("NotificationService NotificationChannel enum")
    void notificationChannelEnum() {
        NotificationService.NotificationChannel[] channels = NotificationService.NotificationChannel.values();
        assertEquals(7, channels.length);
        assertEquals("auto", NotificationService.NotificationChannel.AUTO.getId());
        assertEquals("iterm2", NotificationService.NotificationChannel.ITERM2.getId());
    }

    @Test
    @DisplayName("NotificationService NotificationChannel fromId")
    void notificationChannelFromId() {
        assertEquals(NotificationService.NotificationChannel.ITERM2,
            NotificationService.NotificationChannel.fromId("iterm2"));
        assertEquals(NotificationService.NotificationChannel.AUTO,
            NotificationService.NotificationChannel.fromId("unknown"));
    }

    @Test
    @DisplayName("NotificationService NotificationOptions of")
    void notificationOptionsOf() {
        NotificationService.NotificationOptions options = NotificationService.NotificationOptions.of("Test message");

        assertEquals("Test message", options.message());
        assertEquals("Claude Code", options.title());
        assertEquals("default", options.notificationType());
    }

    @Test
    @DisplayName("NotificationService NotificationOptions withTitle")
    void notificationOptionsWithTitle() {
        NotificationService.NotificationOptions options = NotificationService.NotificationOptions.withTitle(
            "Test message", "Custom Title"
        );

        assertEquals("Test message", options.message());
        assertEquals("Custom Title", options.title());
    }

    @Test
    @DisplayName("NotificationService setPreferredChannel and getPreferredChannel")
    void setGetPreferredChannel() {
        NotificationService.setPreferredChannel("iterm2");
        assertEquals("iterm2", NotificationService.getPreferredChannel());

        NotificationService.setPreferredChannel(null);
        assertEquals("auto", NotificationService.getPreferredChannel());
    }

    @Test
    @DisplayName("NotificationService setDetectedTerminal and getDetectedTerminal")
    void setGetDetectedTerminal() {
        NotificationService.setDetectedTerminal(NotificationService.TerminalType.ITERM2);
        assertEquals(NotificationService.TerminalType.ITERM2, NotificationService.getDetectedTerminal());
    }

    @Test
    @DisplayName("NotificationService detectTerminal returns TerminalType")
    void detectTerminal() {
        NotificationService.TerminalType terminal = NotificationService.detectTerminal();
        assertNotNull(terminal);
    }

    @Test
    @DisplayName("NotificationService sendNotification returns channel")
    void sendNotification() throws Exception {
        NotificationService.NotificationOptions options = NotificationService.NotificationOptions.of("Test");

        String result = NotificationService.sendNotification(options).get();
        assertNotNull(result);
    }

    @Test
    @DisplayName("NotificationService ringBell does not throw")
    void ringBell() {
        NotificationService.ringBell();
    }
}