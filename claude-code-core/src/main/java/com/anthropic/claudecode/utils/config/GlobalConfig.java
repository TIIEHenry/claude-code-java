/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/config.ts global config
 */
package com.anthropic.claudecode.utils.config;

import java.util.*;
import java.util.concurrent.*;

/**
 * Global configuration.
 */
public record GlobalConfig(
    String model,
    String theme,
    String preferredNotifChannel,
    List<String> enabledMcpServers,
    List<String> disabledMcpServers,
    Boolean hasTrustDialogAccepted,
    Boolean hasCompletedOnboarding,
    Integer onboardingSeenCount,
    ReleaseChannel releaseChannel,
    InstallMethod installMethod,
    String lastUsedVersion,
    Long lastUpdateTime,
    Boolean autoUpdate,
    String apiKey,
    String oauthToken,
    String advisorModel,
    Map<String, Object> customSettings
) {
    /**
     * Create default global config.
     */
    public static GlobalConfig createDefault() {
        return new GlobalConfig(
            null, // model - use default
            "default", // theme
            "auto", // preferredNotifChannel
            List.of(), // enabledMcpServers
            List.of(), // disabledMcpServers
            false, // hasTrustDialogAccepted
            false, // hasCompletedOnboarding
            0, // onboardingSeenCount
            ReleaseChannel.STABLE,
            InstallMethod.UNKNOWN,
            null, // lastUsedVersion
            null, // lastUpdateTime
            true, // autoUpdate
            null, // apiKey
            null, // oauthToken
            null, // advisorModel
            Map.of() // customSettings
        );
    }

    /**
     * Merge with defaults.
     */
    public GlobalConfig withDefaults() {
        GlobalConfig defaults = createDefault();
        return new GlobalConfig(
            model != null ? model : defaults.model,
            theme != null ? theme : defaults.theme,
            preferredNotifChannel != null ? preferredNotifChannel : defaults.preferredNotifChannel,
            enabledMcpServers != null ? enabledMcpServers : defaults.enabledMcpServers,
            disabledMcpServers != null ? disabledMcpServers : defaults.disabledMcpServers,
            hasTrustDialogAccepted != null ? hasTrustDialogAccepted : defaults.hasTrustDialogAccepted,
            hasCompletedOnboarding != null ? hasCompletedOnboarding : defaults.hasCompletedOnboarding,
            onboardingSeenCount != null ? onboardingSeenCount : defaults.onboardingSeenCount,
            releaseChannel != null ? releaseChannel : defaults.releaseChannel,
            installMethod != null ? installMethod : defaults.installMethod,
            lastUsedVersion,
            lastUpdateTime,
            autoUpdate != null ? autoUpdate : defaults.autoUpdate,
            apiKey,
            oauthToken,
            advisorModel,
            customSettings != null ? customSettings : defaults.customSettings
        );
    }
}

/**
 * History entry for serialized history.
 */
record HistoryEntry(
    String display,
    Map<Integer, PastedContent> pastedContents
) {
    public static HistoryEntry of(String display) {
        return new HistoryEntry(display, Map.of());
    }
}

/**
 * Pasted content entry.
 */
record PastedContent(
    int id,
    String type,
    String content,
    String mediaType,
    String filename,
    ImageDimensions dimensions,
    String sourcePath
) {
    public static PastedContent text(int id, String content) {
        return new PastedContent(id, "text", content, null, null, null, null);
    }

    public static PastedContent image(int id, String content, String mediaType, String filename) {
        return new PastedContent(id, "image", content, mediaType, filename, null, null);
    }
}

/**
 * Image dimensions.
 */
record ImageDimensions(
    int width,
    int height,
    int originalWidth,
    int originalHeight
) {}