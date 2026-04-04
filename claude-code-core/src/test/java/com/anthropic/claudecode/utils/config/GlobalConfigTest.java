/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for GlobalConfig.
 */
@DisplayName("GlobalConfig Tests")
class GlobalConfigTest {

    @Test
    @DisplayName("GlobalConfig createDefault returns valid config")
    void createDefaultReturnsValidConfig() {
        GlobalConfig config = GlobalConfig.createDefault();

        assertNotNull(config);
        assertNull(config.model());
        assertEquals("default", config.theme());
        assertEquals("auto", config.preferredNotifChannel());
        assertTrue(config.enabledMcpServers().isEmpty());
        assertTrue(config.disabledMcpServers().isEmpty());
        assertFalse(config.hasTrustDialogAccepted());
        assertFalse(config.hasCompletedOnboarding());
        assertEquals(0, config.onboardingSeenCount());
        assertEquals(ReleaseChannel.STABLE, config.releaseChannel());
        assertEquals(InstallMethod.UNKNOWN, config.installMethod());
        assertNull(config.lastUsedVersion());
        assertNull(config.lastUpdateTime());
        assertTrue(config.autoUpdate());
        assertNull(config.apiKey());
        assertNull(config.oauthToken());
        assertNull(config.advisorModel());
        assertTrue(config.customSettings().isEmpty());
    }

    @Test
    @DisplayName("GlobalConfig record works correctly")
    void globalConfigRecordWorksCorrectly() {
        GlobalConfig config = new GlobalConfig(
            "claude-opus-4-6",
            "dark",
            "terminal",
            List.of("server1"),
            List.of("server2"),
            true,
            true,
            5,
            ReleaseChannel.LATEST,
            InstallMethod.GLOBAL,
            "1.0.0",
            System.currentTimeMillis(),
            false,
            "test-key",
            "test-token",
            "claude-sonnet-4-6",
            Map.of("custom", "value")
        );

        assertEquals("claude-opus-4-6", config.model());
        assertEquals("dark", config.theme());
        assertEquals("terminal", config.preferredNotifChannel());
        assertEquals(1, config.enabledMcpServers().size());
        assertEquals(1, config.disabledMcpServers().size());
        assertTrue(config.hasTrustDialogAccepted());
        assertTrue(config.hasCompletedOnboarding());
        assertEquals(5, config.onboardingSeenCount());
        assertEquals(ReleaseChannel.LATEST, config.releaseChannel());
        assertEquals(InstallMethod.GLOBAL, config.installMethod());
        assertEquals("1.0.0", config.lastUsedVersion());
        assertNotNull(config.lastUpdateTime());
        assertFalse(config.autoUpdate());
        assertEquals("test-key", config.apiKey());
        assertEquals("test-token", config.oauthToken());
        assertEquals("claude-sonnet-4-6", config.advisorModel());
        assertEquals(1, config.customSettings().size());
    }

    @Test
    @DisplayName("GlobalConfig withDefaults merges with defaults")
    void withDefaultsMergesWithDefaults() {
        GlobalConfig partial = new GlobalConfig(
            null, // model
            null, // theme
            null, // preferredNotifChannel
            null, // enabledMcpServers
            null, // disabledMcpServers
            null, // hasTrustDialogAccepted
            null, // hasCompletedOnboarding
            null, // onboardingSeenCount
            null, // releaseChannel
            null, // installMethod
            null,
            null,
            null, // autoUpdate
            "test-key",
            null,
            null,
            null
        );

        GlobalConfig merged = partial.withDefaults();

        // From defaults
        assertEquals("default", merged.theme());
        assertEquals("auto", merged.preferredNotifChannel());
        assertEquals(ReleaseChannel.STABLE, merged.releaseChannel());
        assertTrue(merged.autoUpdate());

        // From partial (only apiKey was set)
        assertEquals("test-key", merged.apiKey());
    }

    @Test
    @DisplayName("GlobalConfig withDefaults preserves non-null values")
    void withDefaultsPreservesNonNullValues() {
        GlobalConfig config = new GlobalConfig(
            "claude-opus-4-6",
            "dark",
            "terminal",
            List.of("server"),
            List.of(),
            true,
            false,
            10,
            ReleaseChannel.LATEST,
            InstallMethod.NATIVE,
            null,
            null,
            false,
            null,
            null,
            null,
            null
        );

        GlobalConfig merged = config.withDefaults();

        // All non-null values preserved
        assertEquals("claude-opus-4-6", merged.model());
        assertEquals("dark", merged.theme());
        assertEquals("terminal", merged.preferredNotifChannel());
        assertTrue(merged.hasTrustDialogAccepted());
        assertEquals(10, merged.onboardingSeenCount());
        assertEquals(ReleaseChannel.LATEST, merged.releaseChannel());
        assertFalse(merged.autoUpdate());
    }

    @Test
    @DisplayName("HistoryEntry of factory method works correctly")
    void historyEntryOfFactoryMethodWorksCorrectly() {
        HistoryEntry entry = HistoryEntry.of("test display");

        assertEquals("test display", entry.display());
        assertTrue(entry.pastedContents().isEmpty());
    }

    @Test
    @DisplayName("HistoryEntry record works correctly")
    void historyEntryRecordWorksCorrectly() {
        Map<Integer, PastedContent> pasted = new HashMap<>();
        pasted.put(1, PastedContent.text(1, "text content"));

        HistoryEntry entry = new HistoryEntry("display", pasted);

        assertEquals("display", entry.display());
        assertEquals(1, entry.pastedContents().size());
    }

    @Test
    @DisplayName("PastedContent text factory method works correctly")
    void pastedContentTextFactoryMethodWorksCorrectly() {
        PastedContent content = PastedContent.text(1, "text content");

        assertEquals(1, content.id());
        assertEquals("text", content.type());
        assertEquals("text content", content.content());
        assertNull(content.mediaType());
        assertNull(content.filename());
    }

    @Test
    @DisplayName("PastedContent image factory method works correctly")
    void pastedContentImageFactoryMethodWorksCorrectly() {
        PastedContent content = PastedContent.image(2, "base64data", "image/png", "test.png");

        assertEquals(2, content.id());
        assertEquals("image", content.type());
        assertEquals("base64data", content.content());
        assertEquals("image/png", content.mediaType());
        assertEquals("test.png", content.filename());
    }

    @Test
    @DisplayName("PastedContent record works correctly")
    void pastedContentRecordWorksCorrectly() {
        ImageDimensions dims = new ImageDimensions(100, 200, 200, 400);
        PastedContent content = new PastedContent(
            3, "image", "data", "image/jpeg", "photo.jpg", dims, "/path/to/file"
        );

        assertEquals(3, content.id());
        assertEquals("image", content.type());
        assertEquals("data", content.content());
        assertEquals("image/jpeg", content.mediaType());
        assertEquals("photo.jpg", content.filename());
        assertNotNull(content.dimensions());
        assertEquals("/path/to/file", content.sourcePath());
    }

    @Test
    @DisplayName("ImageDimensions record works correctly")
    void imageDimensionsRecordWorksCorrectly() {
        ImageDimensions dims = new ImageDimensions(100, 200, 200, 400);

        assertEquals(100, dims.width());
        assertEquals(200, dims.height());
        assertEquals(200, dims.originalWidth());
        assertEquals(400, dims.originalHeight());
    }
}